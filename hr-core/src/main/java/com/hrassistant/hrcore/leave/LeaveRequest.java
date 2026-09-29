package com.hrassistant.hrcore.leave;

import com.hrassistant.hrcore.common.Ids;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A leave request, and the ONLY place its status may change.
 *
 * There are no setters on purpose. The status moves through the methods below (submit, approve,
 * reject, cancel), so every rule about "which change is allowed from which state" lives in one class
 * and can be unit tested without Spring or a database (see LeaveRequestTransitionsTest).
 */
@Entity
@Table(name = "leave_request")
public class LeaveRequest {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type")
    private LeaveType leaveType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "working_days")
    private BigDecimal workingDays;

    private String reason;

    @Enumerated(EnumType.STRING)
    private LeaveStatus status;

    @Column(name = "decided_by_id")
    private UUID decidedById;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_note")
    private String decisionNote;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "reminder_count")
    private int reminderCount;

    @Column(name = "last_reminded_at")
    private Instant lastRemindedAt;

    @Column(name = "escalated_at")
    private Instant escalatedAt;

    /**
     * Optimistic locking. Hibernate adds "AND version = ?" to every UPDATE and bumps it. If two
     * managers approve/reject the same request at the same moment, the second UPDATE matches 0 rows
     * and fails (-> 409 CONCURRENT_UPDATE) instead of silently overwriting the first decision.
     * A Long (not long) so a brand new entity has version null, which tells Spring Data it's new.
     */
    @Version
    private Long version;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected LeaveRequest() {
        // for JPA
    }

    /**
     * (none) -> PENDING. Only creates the object; the checks (balance, overlap, dates) happen in
     * LeaveService.submit before this is called, because they need the database.
     */
    static LeaveRequest submit(UUID tenantId, UUID employeeId, LeaveType leaveType, LocalDate startDate,
                               LocalDate endDate, BigDecimal workingDays, String reason, Instant now) {
        LeaveRequest request = new LeaveRequest();
        request.id = Ids.newId();
        request.tenantId = tenantId;
        request.employeeId = employeeId;
        request.leaveType = leaveType;
        request.startDate = startDate;
        request.endDate = endDate;
        request.workingDays = workingDays;
        request.reason = reason;
        request.status = LeaveStatus.PENDING;
        request.createdAt = now;
        request.updatedAt = now;
        return request;
    }

    // ---------------------------------------------------------------------------------------------
    // State transitions: TODO(Sebin). The tests in LeaveRequestTransitionsTest define the behaviour.
    //
    // Rules:
    //   approve: only from PENDING. Sets status, decidedById, decidedAt, decisionNote, updatedAt.
    //   reject:  only from PENDING. A non-blank note (the reason) is required. Sets the same fields.
    //   cancel:  PENDING -> CANCELLED, or APPROVED -> CANCELLED only if today is before startDate.
    //            Sets cancelledAt and updatedAt. (The balance "restores" itself: it's derived, ADR 004.)
    //   Nobody may approve or reject their own request (deciderId == employeeId).
    //
    // Which exception to throw (ApiException in the common package):
    //   transition not allowed from the current status  -> new ApiException.Conflict("INVALID_STATE_TRANSITION", ...)
    //   deciding your own request                        -> new ApiException.BusinessRule("SELF_DECISION", ...)
    //   reject without a note                            -> new ApiException.BusinessRule("REJECTION_REASON_REQUIRED", ...)
    //   cancel approved leave that has already started   -> new ApiException.BusinessRule("LEAVE_ALREADY_STARTED", ...)
    //
    // Permission rules (is the decider this employee's manager or HR?) do NOT belong here. They need
    // other data and are Phase 2 (ApprovalService). This class only guards its own state.
    // ---------------------------------------------------------------------------------------------

    public void approve(UUID deciderId, String note, Instant now) {
        // TODO(Sebin)
        throw new UnsupportedOperationException("TODO(Sebin): implement approve");
    }

    public void reject(UUID deciderId, String note, Instant now) {
        // TODO(Sebin)
        throw new UnsupportedOperationException("TODO(Sebin): implement reject");
    }

    public void cancel(LocalDate today, Instant now) {
        // TODO(Sebin)
        throw new UnsupportedOperationException("TODO(Sebin): implement cancel");
    }

    // ---------------------------------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getWorkingDays() {
        return workingDays;
    }

    public String getReason() {
        return reason;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public UUID getDecidedById() {
        return decidedById;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public int getReminderCount() {
        return reminderCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
