package com.hrassistant.hrcore.leave;

import static com.hrassistant.hrcore.SeedIds.LIAM;
import static com.hrassistant.hrcore.SeedIds.d;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.hrassistant.hrcore.TestcontainersConfiguration;
import com.hrassistant.hrcore.audit.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * THE Phase 1 lesson: a transaction is all or nothing.
 *
 * LeaveService.submit does two writes: INSERT leave_request, then INSERT audit_log. We make the
 * second one fail and check the first one was rolled back too. Without @Transactional on submit,
 * Liam would end up with a leave request that has no audit trail.
 *
 * Deliberately NOT @Transactional (unlike the other API tests): the test must see what was really
 * committed to the database, not what's visible inside a test transaction.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Import(TestcontainersConfiguration.class)
class SubmitTransactionTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoSpyBean   // the real AuditService, except where we tell it to misbehave
    AuditService audit;

    @Test
    void ifTheAuditWriteFailsTheLeaveRequestIsRolledBack() {
        // Stub the real object behind Spring's transaction proxy (the proxy would demand a transaction just to stub)
        AuditService target = AopTestUtils.getUltimateTargetObject(audit);
        doThrow(new IllegalStateException("audit database on fire"))
                .when(target).record(any(), any(), any(), eq("LEAVE_REQUEST_SUBMITTED"), any(), any(), any());

        assertThatThrownBy(() -> mvc.perform(post("/api/leave-requests")
                .header("X-Employee-Id", LIAM)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"startDate": "%s", "endDate": "%s"}""".formatted(d(21), d(25)))))
                .hasRootCauseMessage("audit database on fire");

        Long liamsRequests = jdbc.queryForObject(
                "SELECT count(*) FROM leave_request WHERE employee_id = ?", Long.class, LIAM);
        assertThat(liamsRequests).as("the INSERT into leave_request must have been rolled back").isZero();
    }
}
