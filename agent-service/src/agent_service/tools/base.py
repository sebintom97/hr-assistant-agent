"""What a tool IS: one dish card on the agent's menu."""

from dataclasses import dataclass
from typing import Callable

from agent_service.clients.hr_core import HrCoreClient


class ToolError(Exception):
    """The tool could not run (unknown tool, not allowed, bad input). The message is shown to Claude."""


@dataclass(frozen=True)
class Tool:
    name: str                       # what Claude orders, e.g. "get_team_calendar"
    description: str                # what it does and WHEN to use it (Claude reads this)
    input_schema: dict              # the options Claude must give (e.g. from_date, to_date)
    tier: str                       # "READ" (just looks) or "DRAFT" (proposes a change)
    managers_only: bool             # True = employees never see this tool
    handler: Callable[[HrCoreClient, dict], object]   # the recipe: (who is calling, Claude's options) -> result

    def definition(self) -> dict:
        """The part of the card Claude sees. Never the handler, tier or managers_only."""
        return {
            "name": self.name,
            "description": self.description,
            "input_schema": self.input_schema,
             }