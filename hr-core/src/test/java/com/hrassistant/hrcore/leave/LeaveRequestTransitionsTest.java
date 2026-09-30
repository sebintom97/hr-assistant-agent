package com.hrassistant.hrcore.leave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrassistant.hrcore.common.ApiException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The specification for the state machine in LeaveRequest. Sebin implements approve/reject/cancel
 * until every test here is green. Plain unit tests: no Spring, no database.
 *
 *            submit
 *   (none) ─────────▶ PENDING ──approve──▶ APPROVED ──cancel (before start)──▶ CANCELLED
 *                        │ ├───reject───▶ REJECTED
 *                        │ └───cancel───▶ CANCELLED
 */
class LeaveRequestTransitionsTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID EMPLOYEE = UUID.randomUUID();
    private static final UUID MANAGER = UUID.randomUUID();

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);
    private static final LocalDate START = TODAY.plusDays(10);
    private static final Instant NOW = Instant.parse("2026-10-01T09:00:00Z");
    private static final Instant LATER = NOW.plusSeconds(3600);

    private static LeaveRequest pending() {
        return LeaveRequest.submit(TENANT, EMPLOYEE, LeaveType.ANNUAL, START, START.plusDays(4),
                new BigDecimal("5.0"), "Holiday", NOW);
    }

    @Nested
    class Approve {

        @Test
        void pendingBecomesApprovedAndRecordsTheDecision() {
            LeaveRequest request = pending();

            request.approve(MANAGER, "Enjoy!", LATER);

            assertThat(request.getStatus()).isEqualTo(LeaveStatus.APPROVED);
            assertThat(request.getDecidedById()).isEqualTo(MANAGER);
            assertThat(request.getDecidedAt()).isEqualTo(LATER);
            assertThat(request.getDecisionNote()).isEqualTo("Enjoy!");
            assertThat(request.getUpdatedAt()).isEqualTo(LATER);
        }

        @Test
        void noteIsOptional() {
            LeaveRequest request = pending();

            request.approve(MANAGER, null, LATER);

            assertThat(request.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        }

        @Test
        void nobodyApprovesTheirOwnRequest() {
            LeaveRequest request = pending();

            assertRefused(() -> request.approve(EMPLOYEE, null, LATER), "SELF_DECISION");
            assertThat(request.getStatus()).isEqualTo(LeaveStatus.PENDING);
        }

        @Test
        void approvedCannotBeApprovedAgain() {
            LeaveRequest request = pending();
            request.approve(MANAGER, null, LATER);

            assertRefused(() -> request.approve(MANAGER, null, LATER), "INVALID_STATE_TRANSITION");
        }

        @Test
        void rejectedCannotBeApproved() {
            LeaveRequest request = pending();
            request.reject(MANAGER, "Busy week", LATER);

            assertRefused(() -> request.approve(MANAGER, null, LATER), "INVALID_STATE_TRANSITION");
            assertThat(request.getStatus()).isEqualTo(LeaveStatus.REJECTED);
        }

        @Test
        void cancelledCannotBeApproved() {
            LeaveRequest request = pending();
            request.cancel(TODAY, LATER);

            assertRefused(() -> request.approve(MANAGER, null, LATER), "INVALID_STATE_TRANSITION");
        }
    }

    @Nested
    class Reject {

        @Test
        void pendingBecomesRejectedWithTheReason() {
            LeaveRequest request = pending();

            request.reject(MANAGER, "Release week", LATER);

            assertThat(request.getStatus()).isEqualTo(LeaveStatus.REJECTED);
            assertThat(request.getDecidedById()).isEqualTo(MANAGER);
            assertThat(request.getDecidedAt()).isEqualTo(LATER);
            assertThat(request.getDecisionNote()).isEqualTo("Release week");
        }

        @Test
        void aReasonIsRequired() {
            LeaveRequest request = pending();

            assertRefused(() -> request.reject(MANAGER, null, LATER), "REJECTION_REASON_REQUIRED");
            assertRefused(() -> request.reject(MANAGER, "   ", LATER), "REJECTION_REASON_REQUIRED");
            assertThat(request.getStatus()).isEqualTo(LeaveStatus.PENDING);
        }

        @Test
        void nobodyRejectsTheirOwnRequest() {
            LeaveRequest request = pending();

            assertRefused(() -> request.reject(EMPLOYEE, "No", LATER), "SELF_DECISION");
        }

        @Test
        void approvedCannotBeRejected() {
            LeaveRequest request = pending();
            request.approve(MANAGER, null, LATER);

            assertRefused(() -> request.reject(MANAGER, "Changed my mind", LATER), "INVALID_STATE_TRANSITION");
            assertThat(request.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        }
    }

    @Nested
    class Cancel {

        @Test
        void pendingBecomesCancelled() {
            LeaveRequest request = pending();

            request.cancel(TODAY, LATER);

            assertThat(request.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
            assertThat(request.getCancelledAt()).isEqualTo(LATER);
            assertThat(request.getUpdatedAt()).isEqualTo(LATER);
        }

        @Test
        void approvedCanBeCancelledBeforeItStarts() {
            LeaveRequest request = pending();
            request.approve(MANAGER, null, LATER);

            request.cancel(START.minusDays(1), LATER);

            assertThat(request.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
            assertThat(request.getDecidedById()).as("the original decision is kept for the record").isEqualTo(MANAGER);
        }

        @Test
        void approvedCannotBeCancelledOnceItHasStarted() {
            LeaveRequest request = pending();
            request.approve(MANAGER, null, LATER);

            assertRefused(() -> request.cancel(START, LATER), "LEAVE_ALREADY_STARTED");
            assertThat(request.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        }

        @Test
        void rejectedCannotBeCancelled() {
            LeaveRequest request = pending();
            request.reject(MANAGER, "No", LATER);

            assertRefused(() -> request.cancel(TODAY, LATER), "INVALID_STATE_TRANSITION");
        }

        @Test
        void cancelledCannotBeCancelledAgain() {
            LeaveRequest request = pending();
            request.cancel(TODAY, LATER);

            assertRefused(() -> request.cancel(TODAY, LATER), "INVALID_STATE_TRANSITION");
        }
    }

    private static void assertRefused(ThrowingCallable action, String expectedCode) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(expectedCode));
    }
}
