-- V1: tenants, teams, employees.
--
-- Conventions used in every table:
--   * id uuid DEFAULT uuidv7()   time-ordered UUIDs (Postgres 18+): new rows land at the end of the
--                                 index like a sequence would, instead of random v4 scattering pages.
--   * tenant_id NOT NULL          every row belongs to exactly one company (rule 5).
--   * UNIQUE (tenant_id, id)      lets other tables point here with a composite foreign key
--                                 (tenant_id, x_id), so the DATABASE rejects a reference that
--                                 crosses tenants, even if a query in the code forgets to filter.
--   * timestamptz                 always store instants with a time zone.

CREATE TABLE tenant (
    id           uuid        PRIMARY KEY DEFAULT uuidv7(),
    slug         text        NOT NULL UNIQUE,
    name         text        NOT NULL,
    country_code char(2)     NOT NULL,           -- ISO 3166, drives which public holidays apply
    created_at   timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE team (
    id         uuid        PRIMARY KEY DEFAULT uuidv7(),
    tenant_id  uuid        NOT NULL REFERENCES tenant (id),
    name       text        NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, id),
    UNIQUE (tenant_id, name)
);

CREATE TABLE employee (
    id         uuid        PRIMARY KEY DEFAULT uuidv7(),
    tenant_id  uuid        NOT NULL REFERENCES tenant (id),
    team_id    uuid,
    manager_id uuid,                               -- direct manager; NULL for the top of the tree
    email      text        NOT NULL,
    first_name text        NOT NULL,
    last_name  text        NOT NULL,
    job_title  text        NOT NULL,
    role       text        NOT NULL,
    hire_date  date        NOT NULL,
    active     boolean     NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    UNIQUE (tenant_id, id),
    CONSTRAINT employee_role_valid     CHECK (role IN ('EMPLOYEE', 'MANAGER', 'HR_ADMIN')),
    CONSTRAINT employee_not_own_manager CHECK (manager_id <> id),
    CONSTRAINT employee_team_same_tenant
        FOREIGN KEY (tenant_id, team_id) REFERENCES team (tenant_id, id),
    CONSTRAINT employee_manager_same_tenant
        FOREIGN KEY (tenant_id, manager_id) REFERENCES employee (tenant_id, id)
);

-- Email is unique per tenant (case insensitive). How login finds the tenant is a Phase 2 decision.
CREATE UNIQUE INDEX employee_tenant_email_uq ON employee (tenant_id, lower(email));
-- "Who are my direct reports?" is the most common permission lookup.
CREATE INDEX employee_tenant_manager_idx ON employee (tenant_id, manager_id);
CREATE INDEX employee_tenant_team_idx    ON employee (tenant_id, team_id);
