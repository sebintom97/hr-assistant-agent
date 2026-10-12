"""The ONLY way the agent talks to hr-core: HTTP, acting as one user."""

import httpx

from agent_service.config import settings


class HrCoreError(Exception):
    """hr-core answered with an error (4xx/5xx). The message is safe to show to the model."""

    def __init__(self, status: int, message: str):
        super().__init__(f"hr-core returned {status}: {message}")
        self.status = status
        self.message = message


# ONE shared connection pool for the whole program (like HikariCP between hr-core and Postgres).
_http = httpx.Client(
    base_url=settings.hr_core_url,
    timeout=settings.hr_core_timeout_seconds,
    transport=httpx.HTTPTransport(retries=2),  # retry failed CONNECTIONS only (safe for any request)
)


class HrCoreClient:
    """Calls hr-core as ONE user. Create one per conversation."""

    def __init__(self, employee_id: str):
        self.employee_id = employee_id

    def get(self, path: str, params: dict | None = None):
        response = _http.get(path, params=params, headers={"X-Employee-Id":self.employee_id})
        if response.status_code>=400:
            raise HrCoreError(response.status_code, _error_message(response))
        return response.json()


def _error_message(response: httpx.Response) -> str:
    """hr-core sends RFC 7807 problem details: {"code": ..., "detail": ...}. Fall back to the raw text."""
    try:
        return response.json().get("detail") or response.text
    except ValueError:  # the body wasn't JSON
        return response.text