"""Tools about leave: the user's balance, their own requests, and their team's calendar."""

from agent_service.clients.hr_core import HrCoreClient
from agent_service.tools.base import Tool


def get_leave_balance(hr: HrCoreClient, args: dict):
    return hr.get("/api/me/leave-balance")


def get_my_leave_requests(hr: HrCoreClient, args: dict):
    requests = hr.get("/api/me/leave-requests")
    # One small dict per request: only the fields Claude needs (no ids, timestamps or decision details).
    return [
        {
            "leaveType": r["leaveType"],
            "startDate": r["startDate"],
            "endDate": r["endDate"],
            "workingDays": r["workingDays"],
            "status": r["status"],
        }
        for r in requests
    ]


def get_team_calendar(hr: HrCoreClient, args: dict):
    # Translate Claude's option names (from_date/to_date) into hr-core's query parameter names (from/to).
    return hr.get("/api/me/team-calendar", params={"from": args["from_date"], "to": args["to_date"]})


LEAVE_BALANCE = Tool(
    name="get_leave_balance",
    description=(
        "Returns the current user's own annual leave totals for a year: allowance, approved, pending, "
        "remaining and available days (available = what they can still request). Use it whenever the "
        "user asks how much leave they have, and before proposing a leave request. It does not list "
        "individual requests or dates."
    ),
    input_schema={"type": "object", "properties": {}},
    tier="READ",
    managers_only=False,
    handler=get_leave_balance,
)

MY_LEAVE_REQUESTS = Tool(
    name="get_my_leave_requests",
    description=(
        "Lists the current user's OWN leave requests, newest first, with type, start and end date, "
        "working days and status (PENDING, APPROVED, REJECTED or CANCELLED). Use it for questions like "
        "'do I have leave booked?' or 'was my request approved?'. Cannot show anyone else's requests."
    ),
    input_schema={"type": "object", "properties": {}},
    tier="READ",
    managers_only=False,
    handler=get_my_leave_requests,
)

TEAM_CALENDAR = Tool(
    name="get_team_calendar",
    description=(
        "Lists APPROVED absences of people in the current user's team between two dates (at most 93 days "
        "apart): only who is away and when, never reasons or balances. Use it for 'who is off?' questions "
        "and to check for clashes before proposing leave."
    ),
    input_schema={
        "type": "object",
        "properties": {
            "from_date": {"type": "string", "description": "Start date, YYYY-MM-DD"},
            "to_date": {"type": "string", "description": "End date, YYYY-MM-DD"},
        },
        "required": ["from_date", "to_date"],
    },
    tier="READ",
    managers_only=False,
    handler=get_team_calendar,
)
