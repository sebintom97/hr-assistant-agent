package com.hrassistant.hrcore.approval;

import com.hrassistant.hrcore.audit.ActorType;
import com.hrassistant.hrcore.audit.AuditService;
import com.hrassistant.hrcore.common.CurrentUser;
import com.hrassistant.hrcore.employee.Employee;
import com.hrassistant.hrcore.employee.EmployeeService;
import com.hrassistant.hrcore.leave.LeaveRequest;
import com.hrassistant.hrcore.leave.LeaveRequestView;
import com.hrassistant.hrcore.leave.LeaveService;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The manager's side: inbox, approve, reject. Always a human action (rule 7); the agent never calls these.
 * Uses the leave module only through LeaveService, never its repository.
 */
@Service
public class ApprovalService {

    private final LeaveService leave;
    private final EmployeeService employees;
    private final AuditService audit;
    private final Clock clock;

    ApprovalService(LeaveService leave, EmployeeService employees, AuditService audit, Clock clock) {
        this.leave = leave;
        this.employees = employees;
        this.audit = audit;
        this.clock = clock;
    }

    /** Pending requests from my direct reports, oldest first (the one waiting longest is on top). */
    @Transactional(readOnly = true)
    public List<PendingApproval> inbox(CurrentUser me) {
        Map<UUID, Employee> reports = new HashMap<>();
        employees.directReports(me.tenantId(), me.employeeId()).forEach(e -> reports.put(e.getId(), e));
        return leave.pendingFor(me.tenantId(), reports.keySet()).stream()
                .map(r -> new PendingApproval(
                        reports.get(r.getEmployeeId()).fullName(),
                        Duration.between(r.getCreatedAt(), clock.instant()).toHours(),
                        LeaveRequestView.of(r)))
                .toList();
    }

    /**
     * One transaction: load, change status, write audit. If anything fails, nothing is saved.
     * No explicit save(): the entity is managed, so Hibernate writes the change on commit, with the
     * @Version check that stops two managers deciding at the same time.
     */
    @Transactional
    public LeaveRequestView approve(CurrentUser me, UUID requestId, String note) {
        LeaveRequest request = leave.getForDecision(me.tenantId(), requestId);
        // TODO(Sebin, Phase 2): permission check: only the employee's direct manager or an HR admin.
        request.approve(me.employeeId(), note, clock.instant());
        audit.record(me.tenantId(), ActorType.USER, me.employeeId(), "LEAVE_REQUEST_APPROVED",
                "LEAVE_REQUEST", request.getId(), note == null ? Map.of() : Map.of("note", note));
        return LeaveRequestView.of(request);
    }

    @Transactional
    public LeaveRequestView reject(CurrentUser me, UUID requestId, String note) {
        LeaveRequest request = leave.getForDecision(me.tenantId(), requestId);
        // TODO(Sebin, Phase 2): permission check: only the employee's direct manager or an HR admin.
        request.reject(me.employeeId(), note, clock.instant());
        audit.record(me.tenantId(), ActorType.USER, me.employeeId(), "LEAVE_REQUEST_REJECTED",
                "LEAVE_REQUEST", request.getId(), Map.of("note", note));
        return LeaveRequestView.of(request);
    }
}
