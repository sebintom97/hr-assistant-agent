package com.hrassistant.hrcore.leave;

import com.hrassistant.hrcore.common.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin on purpose: translate HTTP <-> Java, nothing else. All rules live in LeaveService.
 */
@RestController
@Tag(name = "Leave")
class LeaveController {

    private final LeaveService leave;

    LeaveController(LeaveService leave) {
        this.leave = leave;
    }

    @GetMapping("/api/me/leave-balance")
    @Operation(summary = "My leave balance for a year (default: this year)")
    LeaveBalance myBalance(CurrentUser me, @RequestParam(required = false) Integer year) {
        return leave.myBalance(me, year);
    }

    @GetMapping("/api/me/leave-requests")
    @Operation(summary = "My leave requests, newest first")
    List<LeaveRequestView> myRequests(CurrentUser me) {
        return leave.myRequests(me);
    }

    @GetMapping("/api/me/team-calendar")
    @Operation(summary = "Approved absences in my team between two dates (max 93 days)")
    List<TeamAbsence> teamCalendar(CurrentUser me,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return leave.teamCalendar(me, from, to);
    }

    @PostMapping("/api/leave-requests")
    @Operation(summary = "Request leave for myself",
            description = "Creates a PENDING request. 422 if it breaks a rule (balance, dates), 409 if it overlaps "
                    + "my own leave. Team clashes are returned as warnings; the request is still created.")
    ResponseEntity<SubmitResult> submit(CurrentUser me, @Valid @RequestBody SubmitLeaveRequest body) {
        SubmitResult result = leave.submit(me, body);
        return ResponseEntity
                .created(URI.create("/api/leave-requests/" + result.request().id()))   // 201 + Location header
                .body(result);
    }

    @GetMapping("/api/leave-requests/{id}")
    @Operation(summary = "One leave request")
    LeaveRequestView get(CurrentUser me, @PathVariable UUID id) {
        return leave.get(me, id);
    }

    @PostMapping("/api/leave-requests/{id}/cancel")
    @Operation(summary = "Cancel my own request (pending, or approved before it starts)")
    LeaveRequestView cancel(CurrentUser me, @PathVariable UUID id) {
        return leave.cancel(me, id);
    }
}
