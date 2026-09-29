package com.hrassistant.hrcore.leave;

import com.hrassistant.hrcore.audit.ActorType;
import com.hrassistant.hrcore.audit.AuditService;
import com.hrassistant.hrcore.common.ApiException;
import com.hrassistant.hrcore.common.CurrentUser;
import com.hrassistant.hrcore.employee.Employee;
import com.hrassistant.hrcore.employee.EmployeeService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The leave module's front door. Every method takes the caller's tenant from CurrentUser, never
 * from the request body, so one company can never reach another's data (rule 5).
 */
@Service
public class LeaveService {

    static final List<LeaveStatus> LIVE_STATUSES = List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);
    private static final long MAX_CALENDAR_DAYS = 93;

    private final LeaveRequestRepository requests;
    private final BalanceService balances;
    private final WorkingDayCalculator workingDays;
    private final EmployeeService employees;
    private final AuditService audit;
    private final Clock clock;

    LeaveService(LeaveRequestRepository requests, BalanceService balances, WorkingDayCalculator workingDays,
                 EmployeeService employees, AuditService audit, Clock clock) {
        this.requests = requests;
        this.balances = balances;
        this.workingDays = workingDays;
        this.employees = employees;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * Validates and creates a PENDING request. The agent will have checked balance and calendar
     * already, but we check AGAIN here: the agent's check is for a helpful conversation, this is the rule.
     *
     * One transaction: the leave_request row and its audit_log row are committed together or not at all.
     */
    @Transactional
    public SubmitResult submit(CurrentUser me, SubmitLeaveRequest body) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = body.startDate();
        LocalDate end = body.endDate();
        LeaveType type = body.leaveTypeOrDefault();

        if (end.isBefore(start)) {
            throw new ApiException.BusinessRule("END_BEFORE_START", "End date must be on or after the start date.");
        }
        if (start.isBefore(today)) {
            throw new ApiException.BusinessRule("START_DATE_IN_PAST", "Leave can't start in the past.");
        }
        BigDecimal days = workingDays.workingDays(me.tenantId(), start, end);
        if (days.signum() == 0) {
            throw new ApiException.BusinessRule("NO_WORKING_DAYS",
                    "Those dates are all weekends or public holidays, so no leave is needed.");
        }
        // Friendly check first; the database EXCLUDE constraint is the backstop for a race between two submits.
        if (requests.existsOverlapping(me.tenantId(), me.employeeId(), LIVE_STATUSES, start, end)) {
            throw new ApiException.Conflict("OVERLAPPING_REQUEST",
                    "You already have a pending or approved request covering some of these days.");
        }
        if (type == LeaveType.ANNUAL) {
            LeaveBalance balance = balances.balance(me.tenantId(), me.employeeId(), start.getYear());
            if (days.compareTo(balance.available()) > 0) {
                throw new ApiException.BusinessRule("INSUFFICIENT_BALANCE",
                        "This request needs %s working days but only %s are available in %d (%s remaining, %s pending)."
                                .formatted(days, balance.available(), balance.year(), balance.remaining(), balance.pending()));
            }
        }

        LeaveRequest request = LeaveRequest.submit(me.tenantId(), me.employeeId(), type, start, end, days,
                body.reason(), clock.instant());
        requests.save(request);
        audit.record(me.tenantId(), ActorType.USER, me.employeeId(), "LEAVE_REQUEST_SUBMITTED",
                "LEAVE_REQUEST", request.getId(), Map.of(
                        "leaveType", type.name(),
                        "startDate", start.toString(),
                        "endDate", end.toString(),
                        "workingDays", days.toPlainString()));

        return new SubmitResult(LeaveRequestView.of(request), teamClashes(me, start, end));
    }

    @Transactional
    public LeaveRequestView cancel(CurrentUser me, UUID requestId) {
        LeaveRequest request = load(me.tenantId(), requestId);
        // TODO(Sebin, Phase 2): permission check: only the employee who owns the request may cancel it.
        LeaveStatus before = request.getStatus();
        request.cancel(LocalDate.now(clock), clock.instant());
        audit.record(me.tenantId(), ActorType.USER, me.employeeId(), "LEAVE_REQUEST_CANCELLED",
                "LEAVE_REQUEST", request.getId(), Map.of("previousStatus", before.name()));
        return LeaveRequestView.of(request);   // no save() needed: the entity is managed, changes flush on commit
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestView> myRequests(CurrentUser me) {
        return requests.findByTenantIdAndEmployeeIdOrderByStartDateDesc(me.tenantId(), me.employeeId()).stream()
                .map(LeaveRequestView::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public LeaveRequestView get(CurrentUser me, UUID requestId) {
        LeaveRequest request = load(me.tenantId(), requestId);
        // TODO(Sebin, Phase 2): row level check: the employee, their manager, or HR admin only.
        return LeaveRequestView.of(request);
    }

    @Transactional(readOnly = true)
    public LeaveBalance myBalance(CurrentUser me, Integer year) {
        int y = year != null ? year : LocalDate.now(clock).getYear();
        return balances.balance(me.tenantId(), me.employeeId(), y);
    }

    /** Approved absences of everyone in my team (including me) between two dates. */
    @Transactional(readOnly = true)
    public List<TeamAbsence> teamCalendar(CurrentUser me, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new ApiException.BusinessRule("END_BEFORE_START", "'to' must be on or after 'from'.");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_CALENDAR_DAYS) {
            throw new ApiException.BusinessRule("RANGE_TOO_LONG", "The calendar range can be at most " + MAX_CALENDAR_DAYS + " days.");
        }
        UUID teamId = employees.get(me.tenantId(), me.employeeId()).getTeamId();
        List<Employee> team = employees.teamMembers(me.tenantId(), teamId);
        return absences(me.tenantId(), team, from, to);
    }

    // --- used by the approval module -------------------------------------------------------------

    /**
     * Loads a request for approve/reject. MANDATORY: the caller's transaction must already be open,
     * so the returned entity stays managed and the caller's changes are saved when it commits.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public LeaveRequest getForDecision(UUID tenantId, UUID requestId) {
        return load(tenantId, requestId);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> pendingFor(UUID tenantId, Collection<UUID> employeeIds) {
        if (employeeIds.isEmpty()) {
            return List.of();
        }
        return requests.findByTenantIdAndStatusAndEmployeeIdInOrderByCreatedAtAsc(tenantId, LeaveStatus.PENDING, employeeIds);
    }

    // ---------------------------------------------------------------------------------------------

    /** Not found and "exists in another tenant" look identical to the caller, on purpose. */
    private LeaveRequest load(UUID tenantId, UUID requestId) {
        return requests.findByTenantIdAndId(tenantId, requestId)
                .orElseThrow(() -> new ApiException.NotFound("Leave request"));
    }

    private List<TeamAbsence> teamClashes(CurrentUser me, LocalDate start, LocalDate end) {
        UUID teamId = employees.get(me.tenantId(), me.employeeId()).getTeamId();
        List<Employee> colleagues = employees.teamMembers(me.tenantId(), teamId).stream()
                .filter(e -> !e.getId().equals(me.employeeId()))
                .toList();
        return absences(me.tenantId(), colleagues, start, end);
    }

    private List<TeamAbsence> absences(UUID tenantId, List<Employee> people, LocalDate from, LocalDate to) {
        if (people.isEmpty()) {
            return List.of();
        }
        Map<UUID, Employee> byId = people.stream().collect(Collectors.toMap(Employee::getId, e -> e));
        return requests.findOverlappingForEmployees(tenantId, byId.keySet(), LeaveStatus.APPROVED, from, to).stream()
                .map(r -> new TeamAbsence(r.getEmployeeId(), byId.get(r.getEmployeeId()).fullName(),
                        r.getStartDate(), r.getEndDate()))
                .toList();
    }
}
