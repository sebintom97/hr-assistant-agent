# HR Assistant Agent (prototype)

An AI HR assistant built on **synthetic company data**. It answers HR questions and takes actions like leave requests. It respects **role permissions** and keeps a **human in the loop** for approvals.

The core rule: **the AI decides, code enforces.** Permissions, approvals and the audit trail live in a deterministic Java service. The AI agent can only call that service's API with the end user's own token.

> Status: **Phase 0 (domain and data)**. The database schema and demo company are ready. The leave API, security, approval workflow and AI agent come next.

## Try it

Requirements: Docker (with Compose).

```bash
cp .env.example .env        # set a local database password, and change ports if 5432/8080 are taken
docker compose up --build
```

This starts PostgreSQL 18 and `hr-core`. On startup, hr-core runs the Flyway migrations and loads the demo company, so you get the same data as everyone else.

Explore the data:

```bash
docker compose exec postgres psql -U hr -d hr
```

```sql
-- who is waiting on an approval, and for how long
SELECT e.first_name, e.last_name, r.start_date, r.end_date, r.working_days, now() - r.created_at AS waiting
FROM leave_request r JOIN employee e ON e.id = r.employee_id
WHERE r.status = 'PENDING' ORDER BY r.created_at;
```

Reset to a fresh demo (the seed's dates are relative to the day it loads):

```bash
docker compose down -v && docker compose up --build
```

## The demo company

**Acme Analytics Ltd** (Dublin, 30 people, 3 teams) plus a small second company, **Brightwave Labs**, to prove tenant isolation. Everyone is fictional; emails use the reserved `.example` domain.

| Person | Role | Why they're interesting |
|---|---|---|
| Liam O'Connor | Employee, Customer Success | The clean demo user: full balance, no requests |
| Sarah Murphy | Employee, Customer Success | Has leave history. Liam must **not** be able to see it |
| Niamh Kelly | Manager, Customer Success | Approves Liam's and Sarah's leave; has her **own** pending request she can't approve |
| Tom Keane | Manager, Engineering | **On holiday today**, so his team's requests are stuck |
| Priya Sharma | Employee, Engineering | Request pending 6 days, should be **escalated** to HR |
| Conor Brennan | Employee, Engineering | Request pending 3 days, should get a **reminder** |
| Emma Fitzgerald, Ciarán Doherty | Employees, Engineering | Both off next week: a **team clash** warning |
| Fionn Gallagher | Employee, Engineering | Joined 3 weeks ago, only **2 days** of balance |
| Aoife Byrne | HR admin | Receives escalations, sees the whole company |
| Declan Walsh | Manager (CEO) | Approves the managers' leave |
| Sarah Fischer | Employee, **Brightwave** | Another "Sarah" in another company: must never appear for Acme users |

Dates are generated relative to the current week, so these scenarios hold whenever you run it. Irish public holidays are seeded for **2026–2030**; after that, the seed needs new holiday rows.

## Architecture

```
Browser ──▶ agent-service (Python, FastAPI) ──▶ LLM API          [Phase 4]
                 │  tools = HTTP calls carrying the USER'S JWT
                 ▼
Browser ──▶ hr-core (Java 21, Spring Boot 3.5) ──▶ PostgreSQL 18
                 roles, leave rules, approvals, audit log, watcher
```

- **hr-core** is the system of record: correct, transactional, and the only thing that touches the database.
- **agent-service** is the intelligence layer: it can be wrong, so it has no database credentials and can only do what the logged-in user is allowed to do.
- The schema lives in [`hr-core/src/main/resources/db/migration`](hr-core/src/main/resources/db/migration); each migration starts with a comment explaining its design choices.

## Development

```bash
cd hr-core
./mvnw test          # runs against a real Postgres 18 via Testcontainers (Docker must be running)
```
