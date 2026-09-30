package com.hrassistant.hrcore.leave;

import static com.hrassistant.hrcore.SeedIds.AISLING;
import static com.hrassistant.hrcore.SeedIds.AISLING_PENDING_REQUEST;
import static com.hrassistant.hrcore.SeedIds.BRIGHTWAVE_SARAH;
import static com.hrassistant.hrcore.SeedIds.EMMA;
import static com.hrassistant.hrcore.SeedIds.FIONN;
import static com.hrassistant.hrcore.SeedIds.HANNAH;
import static com.hrassistant.hrcore.SeedIds.LIAM;
import static com.hrassistant.hrcore.SeedIds.SARAH;
import static com.hrassistant.hrcore.SeedIds.d;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrassistant.hrcore.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
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
 * The leave API end to end: HTTP request -> controller -> service -> real Postgres (with the demo seed).
 * @Transactional: each test's changes are rolled back afterwards, so tests don't affect each other.
 *
 * Dates use SeedIds.d(n) = this week's Monday + n, the same anchor the seed uses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Import(TestcontainersConfiguration.class)
@Transactional
class LeaveApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManager entityManager;

    @Nested
    class WhoIsCalling {

        @Test
        void missingHeaderIs401() throws Exception {
            mvc.perform(get("/api/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }

        @Test
        void unknownEmployeeIs401() throws Exception {
            mvc.perform(get("/api/me").header("X-Employee-Id", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void malformedIdIs401() throws Exception {
            mvc.perform(get("/api/me").header("X-Employee-Id", "not-a-uuid"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void meReturnsProfileWithTeamAndManager() throws Exception {
            as(LIAM, get("/api/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Liam O'Connor"))
                    .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                    .andExpect(jsonPath("$.teamName").value("Customer Success"))
                    .andExpect(jsonPath("$.managerName").value("Niamh Kelly"));
        }

        @Test
        void everyResponseCarriesARequestId() throws Exception {
            as(LIAM, get("/api/me"))
                    .andExpect(header().exists("X-Request-Id"));
        }
    }

    @Nested
    class Balance {

        @Test
        void newJoinerHasTwoDaysAvailable() throws Exception {
            as(FIONN, get("/api/me/leave-balance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.allowance").value(2.0))
                    .andExpect(jsonPath("$.available").value(2.0));
        }

        @Test
        void pendingRequestsReduceAvailableButNotRemaining() throws Exception {
            // Aisling has one 3-day pending request
            as(AISLING, get("/api/me/leave-balance").param("year", String.valueOf(d(15).getYear())))
                    .andExpect(jsonPath("$.pending").value(3.0))
                    .andExpect(jsonPath("$.remaining").value(25.0))
                    .andExpect(jsonPath("$.available").value(22.0));
        }
    }

    @Nested
    class Submit {

        @Test
        void validRequestIsCreatedAsPendingAndAudited() throws Exception {
            String json = as(LIAM, submit(d(21), d(25)).header("X-Request-Id", "test-trace-123"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", startsWith("/api/leave-requests/")))
                    .andExpect(jsonPath("$.request.status").value("PENDING"))
                    .andExpect(jsonPath("$.request.employeeId").value(LIAM.toString()))
                    .andReturn().getResponse().getContentAsString();
            UUID id = UUID.fromString(JsonPath.read(json, "$.request.id"));

            entityManager.flush();   // push pending inserts to the DB so plain JDBC can see them
            String requestId = jdbc.queryForObject(
                    "SELECT request_id FROM audit_log WHERE action = 'LEAVE_REQUEST_SUBMITTED' AND entity_id = ?",
                    String.class, id);
            assertThat(requestId).isEqualTo("test-trace-123");
        }

        @Test
        void teamClashIsAWarningNotAnError() throws Exception {
            // Emma and Ciarán are off next week, and Tom's holiday runs until next Friday
            as(HANNAH, submit(d(7), d(11)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.teamClashes[*].employeeName",
                            containsInAnyOrder("Tom Keane", "Emma Fitzgerald", "Ciarán Doherty")));
        }

        @Test
        void notEnoughBalanceIs422() throws Exception {
            // Fionn has 2 days; a full week needs at least 4 even with a bank holiday
            as(FIONN, submit(d(14), d(18)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));
        }

        @Test
        void overlappingMyOwnLeaveIs409() throws Exception {
            // Emma already has approved leave d(7)..d(11)
            as(EMMA, submit(d(8), d(8)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("OVERLAPPING_REQUEST"));
        }

        @Test
        void startInThePastIs422() throws Exception {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            as(LIAM, submit(yesterday, yesterday.plusDays(3)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("START_DATE_IN_PAST"));
        }

        @Test
        void endBeforeStartIs422() throws Exception {
            as(LIAM, submit(d(24), d(22)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("END_BEFORE_START"));
        }

        @Test
        void weekendOnlyIs422() throws Exception {
            as(LIAM, submit(d(26), d(27)))   // Saturday and Sunday
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("NO_WORKING_DAYS"));
        }

        @Test
        void missingFieldsAre400() throws Exception {
            as(LIAM, post("/api/leave-requests").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Reading {

        @Test
        void myRequestsListsOnlyMine() throws Exception {
            as(SARAH, get("/api/me/leave-requests"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[*].employeeId", not(hasItem(LIAM.toString()))));
        }

        @Test
        void teamCalendarShowsApprovedLeaveOnly() throws Exception {
            as(LIAM, get("/api/me/team-calendar").param("from", d(0).toString()).param("to", d(35).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].employeeName", hasItem("Sarah Murphy")))      // approved d(28)
                    .andExpect(jsonPath("$[*].employeeName", hasItem("Patrick Hughes")))    // approved d(9)
                    .andExpect(jsonPath("$[*].employeeName", not(hasItem("Aisling Ryan")))); // only pending
        }

        @Test
        void anotherTenantsRequestIsNotFound() throws Exception {
            // Brightwave's Sarah asks for an Acme request: 404, as if it doesn't exist
            as(BRIGHTWAVE_SARAH, get("/api/leave-requests/" + AISLING_PENDING_REQUEST))
                    .andExpect(status().isNotFound());
        }
    }

    // --- helpers ---------------------------------------------------------------------------------

    private ResultActions as(UUID employeeId, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("X-Employee-Id", employeeId));
    }

    private static MockHttpServletRequestBuilder submit(LocalDate start, LocalDate end) {
        return post("/api/leave-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"startDate": "%s", "endDate": "%s", "leaveType": "ANNUAL", "reason": "Test"}"""
                        .formatted(start, end));
    }
}
