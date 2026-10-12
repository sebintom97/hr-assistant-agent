"""Tools about the current user themselves."""

from agent_service.clients.hr_core import HrCoreClient
from agent_service.tools.base import Tool


def get_my_profile(hr: HrCoreClient, args: dict):
    me = hr.get("/api/me")
    # Only what Claude needs: no ids, email or hire date.
    return {
        "name": me["name"],
        "jobTitle": me["jobTitle"],
        "role": me["role"],
        "teamName": me["teamName"],
        "managerName": me["managerName"],
    }


MY_PROFILE = Tool(
    name="get_my_profile",
    description=(
        "Returns the current user's own profile: name, job title, role (EMPLOYEE, MANAGER or HR_ADMIN), "
        "team and manager's name. Use it for questions like 'who is my manager?' or 'which team am I in?'."
    ),
    input_schema={"type": "object", "properties": {}},
    tier="READ",
    managers_only=False,
    handler=get_my_profile,
)
