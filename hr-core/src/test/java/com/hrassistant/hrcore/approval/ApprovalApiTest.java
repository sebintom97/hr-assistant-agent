package com.hrassistant.hrcore.approval;

import static com.hrassistant.hrcore.SeedIds.AISLING;
import static com.hrassistant.hrcore.SeedIds.AISLING_PENDING_REQUEST;
import static com.hrassistant.hrcore.SeedIds.DECLAN;
import static com.hrassistant.hrcore.SeedIds.NIAMH;
import static com.hrassistant.hrcore.SeedIds.NIAMH_OWN_REQUEST;
import static com.hrassistant.hrcore.SeedIds.d;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrassistant.hrcore.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * The manager's side over HTTP. The Inbox tests pass today; the Decisions tests go green once
 * Sebin implements LeaveRequest.approve/reject/cancel.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Import(TestcontainersConfiguration.class)
@Transactional
class ApprovalApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManager entityManager;

    @Nested
    class Inbox {

        @Test
        void managerSeesTheirReportsPendingRequests() throws Exception {
            as(NIAMH, get("/api/approvals/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].employeeName").value("Aisling Ryan"))
                    .andExpect(jsonPath("$[0].request.id").value(AISLING_PENDING_REQUEST.toString()));
        }

        @Test
        void aManagersOwnRequestGoesToTheirManager() throws Exception {
            // Niamh's own request is not in her inbox, it's in Declan's
            as(DECLAN, get("/api/approvals/pending"))
                    .andExpect(jsonPath("$[*].request.id").value(hasItem(NIAMH_OWN_REQUEST.toString())));
        }

        @Test
        void employeeWithoutReportsHasAnEmptyInbox() throws Exception {
            as(AISLING, get("/api/approvals/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    class Decisions {

        @Test
        void approveMovesRequestToApprovedAndAudits() throws Exception {
            as(NIAMH, decide(AISLING_PENDING_REQUEST, "approve", "Have fun"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.decidedById").value(NIAMH.toString()));

            assertThat(auditCount("LEAVE_REQUEST_APPROVED", AISLING_PENDING_REQUEST)).isEqualTo(1);
        }

        @Test
        void approvingMovesDaysFromPendingToApprovedInTheBalance() throws Exception {
            String year = String.valueOf(d(15).getYear());
            as(NIAMH, decide(AISLING_PENDING_REQUEST, "approve", null)).andExpect(status().isOk());

            as(AISLING, get("/api/me/leave-balance").param("year", year))
                    .andExpect(jsonPath("$.pending").value(0.0))
                    .andExpect(jsonPath("$.remaining").value(22.0))
                    .andExpect(jsonPath("$.available").value(22.0));
        }

        @Test
        void rejectWithoutAReasonIs422() throws Exception {
            as(NIAMH, decide(AISLING_PENDING_REQUEST, "reject", null))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("REJECTION_REASON_REQUIRED"));
        }

        @Test
        void rejectWithAReason() throws Exception {
            as(NIAMH, decide(AISLING_PENDING_REQUEST, "reject", "Quarter end, sorry"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.decisionNote").value("Quarter end, sorry"));
        }

        @Test
        void decidingTwiceIs409() throws Exception {
            as(NIAMH, decide(AISLING_PENDING_REQUEST, "approve", null)).andExpect(status().isOk());

            as(NIAMH, decide(AISLING_PENDING_REQUEST, "reject", "Changed my mind"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
        }

        @Test
        void approvingYourOwnRequestIs422() throws Exception {
            as(NIAMH, decide(NIAMH_OWN_REQUEST, "approve", null))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("SELF_DECISION"));
        }

        @Test
        void employeeCancelsTheirPendingRequest() throws Exception {
            as(AISLING, post("/api/leave-requests/" + AISLING_PENDING_REQUEST + "/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));

            assertThat(auditCount("LEAVE_REQUEST_CANCELLED", AISLING_PENDING_REQUEST)).isEqualTo(1);
        }
    }

    // --- helpers ---------------------------------------------------------------------------------

    private ResultActions as(UUID employeeId, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("X-Employee-Id", employeeId));
    }

    private static MockHttpServletRequestBuilder decide(UUID requestId, String action, String note) {
        return post("/api/leave-requests/" + requestId + "/" + action)
                .contentType(MediaType.APPLICATION_JSON)
                .content(note == null ? "{}" : "{\"note\": \"%s\"}".formatted(note));
    }

    private long auditCount(String action, UUID entityId) {
        entityManager.flush();
        Long count = jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE action = ? AND entity_id = ?",
                Long.class, action, entityId);
        return count == null ? 0 : count;
    }
}
