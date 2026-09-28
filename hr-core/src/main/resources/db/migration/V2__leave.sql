-- V2: public holidays, yearly leave allowance, leave requests.
--
-- Balance is DERIVED, not stored (one source of truth, nothing to drift out of sync):
--   remaining(employee, year) = leave_allowance.days
--                               - SUM(leave_request.working_days) WHERE status = 'APPROVED'
--                                 AND leave_type = 'ANNUAL' AND start_date in that year
-- So approving or cancelling a request only changes its status; there is no counter to keep in sync.

-- btree_gist lets an EXCLUDE constraint mix "=" on uuids with "&&" (overlaps) on date ranges.
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Holidays are per tenant (not global) so a company can add its own closure days.
-- Weekend holidays are stored on their real date; Irish "substitute day" rules are out of scope.
CREATE TABLE public_holiday (
    id           uuid PRIMARY KEY DEFAULT uuidv7(),
    tenant_id    uuid NOT NULL REFERENCES tenant (id),
    holiday_date date NOT NULL,
    name         text NOT NULL,
    UNIQUE (tenant_id, holiday_date)
);

CREATE TABLE leave_allowance (
    id          uuid         PRIMARY KEY DEFAULT uuidv7(),
    tenant_id   uuid         NOT NULL REFERENCES tenant (id),
    employee_id uuid         NOT NULL,
    year        integer      NOT NULL,
    days        numeric(4,1) NOT NULL,             -- numeric, not float: 0.5 days must stay exactly 0.5
    created_at  timestamptz  NOT NULL DEFAULT now(),

    UNIQUE (tenant_id, employee_id, year),
    CONSTRAINT leave_allowance_days_valid CHECK (days >= 0),
    CONSTRAINT leave_allowance_employee_same_tenant
        FOREIGN KEY (tenant_id, employee_id) REFERENCES employee (tenant_id, id)
);

CREATE TABLE leave_request (
    id               uuid         PRIMARY KEY DEFAULT uuidv7(),
    tenant_id        uuid         NOT NULL REFERENCES tenant (id),
    employee_id      uuid         NOT NULL,
    leave_type       text         NOT NULL DEFAULT 'ANNUAL',
    start_date       date         NOT NULL,
    end_date         date         NOT NULL,        -- inclusive
    working_days     numeric(4,1) NOT NULL,        -- calculated by hr-core at submit time, then frozen
    reason           text,
    status           text         NOT NULL DEFAULT 'PENDING',

    -- decision (approve / reject); kept even if an approved request is later cancelled
    decided_by_id    uuid,
    decided_at       timestamptz,
    decision_note    text,
    cancelled_at     timestamptz,

    -- watcher bookkeeping (Phase 5)
    reminder_count   integer      NOT NULL DEFAULT 0,
    last_reminded_at timestamptz,
    escalated_at     timestamptz,

    version          bigint       NOT NULL DEFAULT 0,   -- JPA @Version: optimistic locking so two approvers can't both win
    created_at       timestamptz  NOT NULL DEFAULT now(),
    updated_at       timestamptz  NOT NULL DEFAULT now(),

    UNIQUE (tenant_id, id),
    CONSTRAINT leave_request_type_valid   CHECK (leave_type IN ('ANNUAL', 'SICK', 'UNPAID')),
    CONSTRAINT leave_request_status_valid CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT leave_request_dates_valid  CHECK (end_date >= start_date),
    CONSTRAINT leave_request_days_valid   CHECK (working_days > 0),
    CONSTRAINT leave_request_reminders_valid CHECK (reminder_count >= 0),

    -- An approved/rejected request must say who decided and when.
    CONSTRAINT leave_request_decision_recorded CHECK (
        status NOT IN ('APPROVED', 'REJECTED') OR (decided_by_id IS NOT NULL AND decided_at IS NOT NULL)
    ),
    -- Backstop for "nobody approves their own request". The real check lives in the service layer;
    -- this makes a bug there fail loudly instead of silently corrupting data.
    CONSTRAINT leave_request_not_self_decided CHECK (decided_by_id IS NULL OR decided_by_id <> employee_id),
    CONSTRAINT leave_request_cancel_recorded CHECK ((status = 'CANCELLED') = (cancelled_at IS NOT NULL)),

    -- An employee can't hold two live (PENDING or APPROVED) requests covering the same day.
    CONSTRAINT leave_request_no_overlap EXCLUDE USING gist (
        tenant_id   WITH =,
        employee_id WITH =,
        daterange(start_date, end_date, '[]') WITH &&
    ) WHERE (status IN ('PENDING', 'APPROVED')),

    CONSTRAINT leave_request_employee_same_tenant
        FOREIGN KEY (tenant_id, employee_id) REFERENCES employee (tenant_id, id),
    CONSTRAINT leave_request_decider_same_tenant
        FOREIGN KEY (tenant_id, decided_by_id) REFERENCES employee (tenant_id, id)
);

-- Watcher: "PENDING and older than 48h". Partial index = only pending rows are indexed.
CREATE INDEX leave_request_pending_idx  ON leave_request (tenant_id, created_at) WHERE status = 'PENDING';
-- "My requests" and balance calculation.
CREATE INDEX leave_request_employee_idx ON leave_request (tenant_id, employee_id, start_date);
-- Team calendar: who is off between two dates.
CREATE INDEX leave_request_dates_idx    ON leave_request (tenant_id, start_date, end_date);
