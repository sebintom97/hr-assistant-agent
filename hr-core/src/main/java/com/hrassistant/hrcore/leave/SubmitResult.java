package com.hrassistant.hrcore.leave;

import java.util.List;

/**
 * Response of a successful submit. A team clash is a WARNING, not an error: the request is still
 * created, and the manager decides. The agent can mention these to the user.
 */
public record SubmitResult(LeaveRequestView request, List<TeamAbsence> teamClashes) {
}
