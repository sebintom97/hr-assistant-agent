"""The menu: every tool the agent has, which ones each user may see, and how to run one by name."""

import json

from agent_service.clients.hr_core import HrCoreClient
from agent_service.tools.base import Tool, ToolError
from agent_service.tools.approvals import PENDING_APPROVALS
from agent_service.tools.employee import MY_PROFILE
from agent_service.tools.leave import LEAVE_BALANCE, MY_LEAVE_REQUESTS, TEAM_CALENDAR

# 1. ALL THE CARDS, in one list
TOOLS: list[Tool] = [MY_PROFILE, LEAVE_BALANCE, MY_LEAVE_REQUESTS, TEAM_CALENDAR, PENDING_APPROVALS]

# A quick way to find a card by its name: {"get_leave_balance": LEAVE_BALANCE, ...}
_BY_NAME = {tool.name: tool for tool in TOOLS}

MANAGER_ROLES = ("MANAGER", "HR_ADMIN")


# 2. WHICH CARDS MAY THIS USER SEE? (the fronts, sent to Claude with every request)
def tools_for(role: str) -> list[dict]:
    is_manager = role in MANAGER_ROLES
    allowed = []
    for tool in TOOLS:
        if not tool.managers_only or is_manager:
            allowed.append(tool.definition())
    return allowed


# 3. CLAUDE ORDERED A TOOL BY NAME: find it, check it, cook it
def run_tool(name: str, args: dict, hr: HrCoreClient, role: str) -> str:
    tool = _BY_NAME.get(name)
    if tool is None:
        raise ToolError(f"Unknown tool: {name}")
    if tool.managers_only and role not in MANAGER_ROLES:
        raise ToolError(f"{name} is only available to managers")
    result = tool.handler(hr, args)
    return json.dumps(result)


# Tool NAMES for the evals (evals/cases.py imports these, so a new tool is covered automatically)
TOOL_NAMES = [t.name for t in TOOLS]
READ_TOOL_NAMES = [t.name for t in TOOLS if t.tier == "READ" and not t.managers_only]
DRAFT_TOOL_NAMES = [t.name for t in TOOLS if t.tier == "DRAFT"]
MANAGER_TOOL_NAMES = [t.name for t in TOOLS if t.managers_only]
