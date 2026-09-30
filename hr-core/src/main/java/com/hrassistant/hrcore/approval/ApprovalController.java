package com.hrassistant.hrcore.approval;

import com.hrassistant.hrcore.common.CurrentUser;
import com.hrassistant.hrcore.leave.LeaveRequestView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Approvals")
class ApprovalController {

    private final ApprovalService approvals;

    ApprovalController(ApprovalService approvals) {
        this.approvals = approvals;
    }

    @GetMapping("/api/approvals/pending")
    @Operation(summary = "My inbox: pending requests from my direct reports, oldest first")
    List<PendingApproval> inbox(CurrentUser me) {
        return approvals.inbox(me);
    }

    @PostMapping("/api/leave-requests/{id}/approve")
    @Operation(summary = "Approve a pending request (optional note)")
    LeaveRequestView approve(CurrentUser me, @PathVariable UUID id,
                             @Valid @RequestBody(required = false) DecisionRequest body) {
        return approvals.approve(me, id, body == null ? null : body.note());
    }

    @PostMapping("/api/leave-requests/{id}/reject")
    @Operation(summary = "Reject a pending request (note with the reason is required)")
    LeaveRequestView reject(CurrentUser me, @PathVariable UUID id,
                            @Valid @RequestBody(required = false) DecisionRequest body) {
        return approvals.reject(me, id, body == null ? null : body.note());
    }
}
