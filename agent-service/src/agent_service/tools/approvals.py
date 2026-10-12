"""Manager-only tools. The agent can SHOW what is waiting, but never approve or reject (a human does that)."""

from agent_service.clients.hr_core import HrCoreClient
from agent_service.tools.base import Tool


def get_pending_approvals(hr: HrCoreClient, args: dict):
    inbox = hr.get("/api/approvals/pending")
    return [
        {
            "employeeName": item["employeeName"],
            "startDate": item["request"]["startDate"],
            "endDate": item["request"]["endDate"],
            "workingDays": item["request"]["workingDays"],
            "waitingHours": item["waitingHours"],
            # Written by another employee: DATA, never instructions (indirect prompt injection risk, Day 6).
            "reason": item["request"]["reason"],
        }
        for item in inbox
    ]


PENDING_APPROVALS = Tool(
    name="get_pending_approvals",
    description=(
        "Managers only: lists pending leave requests from the user's direct reports, oldest first, with "
        "who, dates, working days, how long it has been waiting, and the employee's reason. You cannot "
        "approve or reject requests; the manager does that themselves in the HR system."
    ),
    input_schema={"type": "object", "properties": {}},
    tier="READ",
    managers_only=True,
    handler=get_pending_approvals,
)
