-- Synthetic demo data. Loaded only with the "demo" Spring profile (docker-compose enables it).
-- Everyone here is fictional; all emails use the reserved .example domain.
--
-- Repeatable migration (R__): Flyway runs it after all V__ migrations, and again whenever this file
-- changes. It first wipes the two demo tenants, so the result is always exactly what's written here.
--
-- Three tricks keep it working on any machine, on any date:
--   pg_temp.sid(key)   deterministic UUID from a readable key: the same ids everywhere, so docs,
--                      tests and demo scripts can refer to "sarah.murphy" by id.
--   pg_temp.d(n)       Monday of the current week + n days. d(7) = next Monday, d(-28) = four
--                      Mondays ago. Scenarios like "pending for 6 days" stay true whenever you run it.
--   working_days()     weekdays minus public holidays, the same rule hr-core will apply.
--
-- Every seeded request spans at least 3 weekdays, so a bank holiday can never make it 0 days.
--
-- Scenarios (Acme Analytics):
--   * Tom Keane (Engineering manager) is ON HOLIDAY today (d-7 .. d+11).
--       - Priya's request: pending 6 days, already reminded 3x -> watcher should ESCALATE to HR.
--       - Conor's request: pending 3 days, never reminded -> watcher should REMIND.
--   * Emma and Ciarán are both off next week -> team clash WARNING for anyone asking for next week.
--   * Niamh Kelly (Customer Success manager) has her OWN pending request -> only her manager
--     (Declan) or HR can approve it; she can't approve herself.
--   * Aisling's request: pending 1 day -> fresh item in Niamh's inbox for the demo.
--   * Liam O'Connor: no requests, full balance -> the clean demo employee.
--   * Sarah Murphy: approved, historical and cancelled requests -> "Show me Sarah's leave" must be
--     refused for Liam, allowed for Niamh and HR.
--   * Fionn Gallagher: joined 3 weeks ago, pro-rated allowance of 2 days -> "not enough balance".
--   * Rejected, cancelled-after-approval and sick leave history for realism.
-- Second tenant (Brightwave Labs): a few employees, including ANOTHER "Sarah", to prove tenant isolation.

CREATE OR REPLACE FUNCTION pg_temp.sid(key text) RETURNS uuid
    LANGUAGE sql IMMUTABLE AS $$ SELECT md5(key)::uuid $$;

CREATE OR REPLACE FUNCTION pg_temp.d(offset_days integer) RETURNS date
    LANGUAGE sql STABLE AS $$ SELECT date_trunc('week', current_date)::date + offset_days $$;

CREATE OR REPLACE FUNCTION pg_temp.working_days(t uuid, s date, e date) RETURNS numeric
    LANGUAGE sql STABLE AS $$
    SELECT count(*)::numeric
    FROM generate_series(s, e, interval '1 day') AS g(day)
    WHERE extract(isodow FROM g.day) < 6
      AND NOT EXISTS (SELECT 1 FROM public_holiday h WHERE h.tenant_id = t AND h.holiday_date = g.day::date)
$$;

-- ---------------------------------------------------------------------------------------------
-- Reset the demo tenants (children first because of foreign keys)
-- ---------------------------------------------------------------------------------------------
DELETE FROM audit_log       WHERE tenant_id IN (pg_temp.sid('tenant:acme'), pg_temp.sid('tenant:brightwave'));
DELETE FROM leave_request   WHERE tenant_id IN (pg_temp.sid('tenant:acme'), pg_temp.sid('tenant:brightwave'));
DELETE FROM leave_allowance WHERE tenant_id IN (pg_temp.sid('tenant:acme'), pg_temp.sid('tenant:brightwave'));
DELETE FROM public_holiday  WHERE tenant_id IN (pg_temp.sid('tenant:acme'), pg_temp.sid('tenant:brightwave'));
DELETE FROM employee        WHERE tenant_id IN (pg_temp.sid('tenant:acme'), pg_temp.sid('tenant:brightwave'));
DELETE FROM team            WHERE tenant_id IN (pg_temp.sid('tenant:acme'), pg_temp.sid('tenant:brightwave'));
DELETE FROM tenant          WHERE id        IN (pg_temp.sid('tenant:acme'), pg_temp.sid('tenant:brightwave'));

-- ---------------------------------------------------------------------------------------------
-- Tenants and teams
-- ---------------------------------------------------------------------------------------------
INSERT INTO tenant (id, slug, name, country_code) VALUES
    (pg_temp.sid('tenant:acme'),       'acme',       'Acme Analytics Ltd', 'IE'),
    (pg_temp.sid('tenant:brightwave'), 'brightwave', 'Brightwave Labs',    'IE');

INSERT INTO team (id, tenant_id, name) VALUES
    (pg_temp.sid('acme:team:engineering'),      pg_temp.sid('tenant:acme'),       'Engineering'),
    (pg_temp.sid('acme:team:customer-success'), pg_temp.sid('tenant:acme'),       'Customer Success'),
    (pg_temp.sid('acme:team:people'),           pg_temp.sid('tenant:acme'),       'People'),
    (pg_temp.sid('brightwave:team:product'),    pg_temp.sid('tenant:brightwave'), 'Product');

-- ---------------------------------------------------------------------------------------------
-- Irish public holidays 2026-2030, for both tenants.
-- EXPIRES: after 2030 the seed needs new rows here (working days would silently include holidays).
-- ---------------------------------------------------------------------------------------------
INSERT INTO public_holiday (tenant_id, holiday_date, name)
SELECT t.id, h.holiday_date, h.name
FROM tenant t
CROSS JOIN (VALUES
        (DATE '2026-01-01', 'New Year''s Day'),
        (DATE '2026-02-02', 'St Brigid''s Day'),
        (DATE '2026-03-17', 'St Patrick''s Day'),
        (DATE '2026-04-06', 'Easter Monday'),
        (DATE '2026-05-04', 'May Bank Holiday'),
        (DATE '2026-06-01', 'June Bank Holiday'),
        (DATE '2026-08-03', 'August Bank Holiday'),
        (DATE '2026-10-26', 'October Bank Holiday'),
        (DATE '2026-12-25', 'Christmas Day'),
        (DATE '2026-12-26', 'St Stephen''s Day'),
        (DATE '2027-01-01', 'New Year''s Day'),
        (DATE '2027-02-01', 'St Brigid''s Day'),
        (DATE '2027-03-17', 'St Patrick''s Day'),
        (DATE '2027-03-29', 'Easter Monday'),
        (DATE '2027-05-03', 'May Bank Holiday'),
        (DATE '2027-06-07', 'June Bank Holiday'),
        (DATE '2027-08-02', 'August Bank Holiday'),
        (DATE '2027-10-25', 'October Bank Holiday'),
        (DATE '2027-12-25', 'Christmas Day'),
        (DATE '2027-12-26', 'St Stephen''s Day'),
        (DATE '2028-01-01', 'New Year''s Day'),
        (DATE '2028-02-07', 'St Brigid''s Day'),
        (DATE '2028-03-17', 'St Patrick''s Day'),
        (DATE '2028-04-17', 'Easter Monday'),
        (DATE '2028-05-01', 'May Bank Holiday'),
        (DATE '2028-06-05', 'June Bank Holiday'),
        (DATE '2028-08-07', 'August Bank Holiday'),
        (DATE '2028-10-30', 'October Bank Holiday'),
        (DATE '2028-12-25', 'Christmas Day'),
        (DATE '2028-12-26', 'St Stephen''s Day'),
        (DATE '2029-01-01', 'New Year''s Day'),
        (DATE '2029-02-05', 'St Brigid''s Day'),
        (DATE '2029-03-17', 'St Patrick''s Day'),
        (DATE '2029-04-02', 'Easter Monday'),
        (DATE '2029-05-07', 'May Bank Holiday'),
        (DATE '2029-06-04', 'June Bank Holiday'),
        (DATE '2029-08-06', 'August Bank Holiday'),
        (DATE '2029-10-29', 'October Bank Holiday'),
        (DATE '2029-12-25', 'Christmas Day'),
        (DATE '2029-12-26', 'St Stephen''s Day'),
        (DATE '2030-01-01', 'New Year''s Day'),
        (DATE '2030-02-01', 'St Brigid''s Day'),
        (DATE '2030-03-17', 'St Patrick''s Day'),
        (DATE '2030-04-22', 'Easter Monday'),
        (DATE '2030-05-06', 'May Bank Holiday'),
        (DATE '2030-06-03', 'June Bank Holiday'),
        (DATE '2030-08-05', 'August Bank Holiday'),
        (DATE '2030-10-28', 'October Bank Holiday'),
        (DATE '2030-12-25', 'Christmas Day'),
        (DATE '2030-12-26', 'St Stephen''s Day')
) AS h(holiday_date, name)
WHERE t.id IN (pg_temp.sid('tenant:acme'), pg_temp.sid('tenant:brightwave'));

-- ---------------------------------------------------------------------------------------------
-- Employees. key = "<tenant>:<first>.<last>", used for the id and for references below.
-- One statement, so the manager_id self reference is checked once all rows exist.
-- ---------------------------------------------------------------------------------------------
INSERT INTO employee (id, tenant_id, team_id, manager_id, email, first_name, last_name, job_title, role, hire_date)
SELECT pg_temp.sid(e.tenant || ':' || e.key),
       pg_temp.sid('tenant:' || e.tenant),
       pg_temp.sid(e.tenant || ':team:' || e.team),
       pg_temp.sid(e.tenant || ':' || e.manager),
       e.key || '@' || CASE e.tenant WHEN 'acme' THEN 'acme-analytics.example' ELSE 'brightwave.example' END,
       e.first_name, e.last_name, e.job_title, e.role,
       current_date - e.tenure_days
FROM (VALUES
    -- tenant, key,                team,               manager,         first,     last,         title,                        role,       tenure (days)
    ('acme', 'declan.walsh',       NULL::text,         NULL::text,      'Declan',  'Walsh',      'Chief Executive Officer',    'MANAGER',  3650),

    ('acme', 'tom.keane',          'engineering',      'declan.walsh',  'Tom',     'Keane',      'Engineering Manager',        'MANAGER',  2200),
    ('acme', 'priya.sharma',       'engineering',      'tom.keane',     'Priya',   'Sharma',     'Senior Backend Engineer',    'EMPLOYEE', 1500),
    ('acme', 'conor.brennan',      'engineering',      'tom.keane',     'Conor',   'Brennan',    'Backend Engineer',           'EMPLOYEE',  700),
    ('acme', 'emma.fitzgerald',    'engineering',      'tom.keane',     'Emma',    'Fitzgerald', 'Frontend Engineer',          'EMPLOYEE',  900),
    ('acme', 'ciaran.doherty',     'engineering',      'tom.keane',     'Ciarán',  'Doherty',    'Staff Engineer',             'EMPLOYEE', 2600),
    ('acme', 'fionn.gallagher',    'engineering',      'tom.keane',     'Fionn',   'Gallagher',  'Graduate Engineer',          'EMPLOYEE',   21),
    ('acme', 'mateusz.nowak',      'engineering',      'tom.keane',     'Mateusz', 'Nowak',      'Platform Engineer',          'EMPLOYEE', 1100),
    ('acme', 'sofia.rossi',        'engineering',      'tom.keane',     'Sofia',   'Rossi',      'Data Engineer',              'EMPLOYEE',  800),
    ('acme', 'david.okafor',       'engineering',      'tom.keane',     'David',   'Okafor',     'Backend Engineer',           'EMPLOYEE',  450),
    ('acme', 'hannah.quinn',       'engineering',      'tom.keane',     'Hannah',  'Quinn',      'QA Engineer',                'EMPLOYEE', 1300),
    ('acme', 'oisin.murray',       'engineering',      'tom.keane',     'Oisín',   'Murray',     'Site Reliability Engineer',  'EMPLOYEE', 1000),
    ('acme', 'lucas.silva',        'engineering',      'tom.keane',     'Lucas',   'Silva',      'Frontend Engineer',          'EMPLOYEE',  380),
    ('acme', 'rachel.moran',       'engineering',      'tom.keane',     'Rachel',  'Moran',      'Engineering Designer',       'EMPLOYEE',  620),
    ('acme', 'kevin.nolan',        'engineering',      'tom.keane',     'Kevin',   'Nolan',      'Security Engineer',          'EMPLOYEE', 1750),
    ('acme', 'yuki.tanaka',        'engineering',      'tom.keane',     'Yuki',    'Tanaka',     'Machine Learning Engineer',  'EMPLOYEE',  540),
    ('acme', 'sean.daly',          'engineering',      'tom.keane',     'Seán',    'Daly',       'Backend Engineer',           'EMPLOYEE',  260),

    ('acme', 'niamh.kelly',        'customer-success', 'declan.walsh',  'Niamh',   'Kelly',      'Head of Customer Success',   'MANAGER',  1900),
    ('acme', 'sarah.murphy',       'customer-success', 'niamh.kelly',   'Sarah',   'Murphy',     'Customer Success Manager',   'EMPLOYEE', 1200),
    ('acme', 'liam.oconnor',       'customer-success', 'niamh.kelly',   'Liam',    'O''Connor',  'Customer Success Manager',   'EMPLOYEE',  650),
    ('acme', 'aisling.ryan',       'customer-success', 'niamh.kelly',   'Aisling', 'Ryan',       'Account Manager',            'EMPLOYEE',  980),
    ('acme', 'patrick.hughes',     'customer-success', 'niamh.kelly',   'Patrick', 'Hughes',     'Support Engineer',           'EMPLOYEE', 1400),
    ('acme', 'chloe.martin',       'customer-success', 'niamh.kelly',   'Chloe',   'Martin',     'Onboarding Specialist',      'EMPLOYEE',  300),
    ('acme', 'ahmed.khan',         'customer-success', 'niamh.kelly',   'Ahmed',   'Khan',       'Support Engineer',           'EMPLOYEE',  720),
    ('acme', 'orla.kavanagh',      'customer-success', 'niamh.kelly',   'Orla',    'Kavanagh',   'Account Manager',            'EMPLOYEE', 1600),
    ('acme', 'eoin.power',         'customer-success', 'niamh.kelly',   'Eoin',    'Power',      'Solutions Consultant',       'EMPLOYEE',  510),
    ('acme', 'julia.weber',        'customer-success', 'niamh.kelly',   'Julia',   'Weber',      'Customer Success Manager',   'EMPLOYEE',  190),

    ('acme', 'aoife.byrne',        'people',           'declan.walsh',  'Aoife',   'Byrne',      'Head of People',             'HR_ADMIN', 2900),
    ('acme', 'grainne.lynch',      'people',           'aoife.byrne',   'Gráinne', 'Lynch',      'People Partner',             'HR_ADMIN', 1050),
    ('acme', 'ronan.doyle',        'people',           'aoife.byrne',   'Ronan',   'Doyle',      'Talent Acquisition Partner', 'EMPLOYEE',  430),

    ('brightwave', 'maya.chen',    'product',          NULL,            'Maya',    'Chen',       'Founder & CEO',              'HR_ADMIN', 1400),
    ('brightwave', 'tom.becker',   'product',          'maya.chen',     'Tom',     'Becker',     'Engineering Lead',           'MANAGER',  1000),
    ('brightwave', 'sarah.fischer','product',          'tom.becker',    'Sarah',   'Fischer',    'Product Designer',           'EMPLOYEE',  600),
    ('brightwave', 'ben.carter',   'product',          'tom.becker',    'Ben',     'Carter',     'Full Stack Engineer',        'EMPLOYEE',  350)
) AS e(tenant, key, team, manager, first_name, last_name, job_title, role, tenure_days);

-- ---------------------------------------------------------------------------------------------
-- Allowances: last, this and next year (so the seed also works across a new year).
-- 25 days standard, 30 for the CEO, and Fionn's pro-rated 2 days as a new joiner.
-- ---------------------------------------------------------------------------------------------
INSERT INTO leave_allowance (tenant_id, employee_id, year, days)
SELECT e.tenant_id, e.id, y.year,
       CASE
           WHEN e.id = pg_temp.sid('acme:fionn.gallagher') AND y.year = extract(year FROM current_date) THEN 2.0
           WHEN e.id = pg_temp.sid('acme:fionn.gallagher') AND y.year < extract(year FROM current_date) THEN 0.0
           WHEN e.id = pg_temp.sid('acme:declan.walsh') THEN 30.0
           ELSE 25.0
       END
FROM employee e
CROSS JOIN generate_series(extract(year FROM current_date)::int - 1, extract(year FROM current_date)::int + 1) AS y(year)
WHERE e.tenant_id IN (pg_temp.sid('tenant:acme'), pg_temp.sid('tenant:brightwave'));

-- ---------------------------------------------------------------------------------------------
-- Leave requests. Offsets are days from this week's Monday (d(0)); "ago" values are intervals before now().
-- ---------------------------------------------------------------------------------------------
INSERT INTO leave_request (id, tenant_id, employee_id, leave_type, start_date, end_date, working_days, reason, status,
                           decided_by_id, decided_at, decision_note, cancelled_at,
                           reminder_count, last_reminded_at, created_at, updated_at)
SELECT pg_temp.sid(r.tenant || ':leave:' || r.key),
       pg_temp.sid('tenant:' || r.tenant),
       pg_temp.sid(r.tenant || ':' || r.employee),
       r.leave_type,
       pg_temp.d(r.start_offset),
       pg_temp.d(r.end_offset),
       pg_temp.working_days(pg_temp.sid('tenant:' || r.tenant), pg_temp.d(r.start_offset), pg_temp.d(r.end_offset)),
       r.reason,
       r.status,
       pg_temp.sid(r.tenant || ':' || r.decided_by),
       now() - r.decided_ago,
       r.decision_note,
       now() - r.cancelled_ago,
       r.reminders,
       now() - r.reminded_ago,
       now() - r.created_ago,
       now() - coalesce(r.cancelled_ago, r.reminded_ago, r.decided_ago, r.created_ago)
FROM (VALUES
    -- tenant, key,              employee,           type,     start, end, status,      created_ago,          decided_by,      decided_ago,         decision_note,                                reason,                          reminders, reminded_ago,       cancelled_ago
    -- Engineering: Tom is away, so two requests are stuck
    ('acme', 'tom-holiday',      'tom.keane',        'ANNUAL',  -7,  11, 'APPROVED',  interval '35 days',  'declan.walsh',  interval '30 days',  NULL::text,                                   'Family trip to Australia',      0,         NULL::interval,     NULL::interval),
    ('acme', 'priya-stuck',      'priya.sharma',     'ANNUAL',  14,  18, 'PENDING',   interval '6 days',   NULL,            NULL,                NULL,                                         'Sister''s wedding in Pune',     3,         interval '20 hours', NULL),
    ('acme', 'priya-sick',       'priya.sharma',     'SICK',   -20, -18, 'APPROVED',  interval '17 days',  'tom.keane',     interval '16 days',  'Get well soon',                              'Flu',                           0,         NULL,               NULL),
    ('acme', 'conor-waiting',    'conor.brennan',    'ANNUAL',  21,  23, 'PENDING',   interval '3 days',   NULL,            NULL,                NULL,                                         'Long weekend in Galway',        0,         NULL,               NULL),
    -- Engineering: Emma and Ciarán overlap next week (team clash warning)
    ('acme', 'emma-next-week',   'emma.fitzgerald',  'ANNUAL',   7,  11, 'APPROVED',  interval '20 days',  'tom.keane',     interval '18 days',  NULL,                                         'Moving house',                  0,         NULL,               NULL),
    ('acme', 'ciaran-next-week', 'ciaran.doherty',   'ANNUAL',   8,  10, 'APPROVED',  interval '15 days',  'tom.keane',     interval '14 days',  NULL,                                         'Conference in Berlin',          0,         NULL,               NULL),
    ('acme', 'ciaran-rejected',  'ciaran.doherty',   'ANNUAL', -28, -26, 'REJECTED',  interval '40 days',  'tom.keane',     interval '38 days',  'Release week, could you pick the week after?', 'Short break',                 0,         NULL,               NULL),
    -- Engineering: history
    ('acme', 'mateusz-past',     'mateusz.nowak',    'ANNUAL', -56, -52, 'APPROVED',  interval '70 days',  'tom.keane',     interval '68 days',  NULL,                                         'Visiting family in Kraków',     0,         NULL,               NULL),
    ('acme', 'sofia-future',     'sofia.rossi',      'ANNUAL',  49,  53, 'APPROVED',  interval '25 days',  'tom.keane',     interval '24 days',  NULL,                                         'Holiday in Sicily',             0,         NULL,               NULL),
    ('acme', 'david-cancelled',  'david.okafor',     'ANNUAL', -14, -12, 'CANCELLED', interval '32 days',  'tom.keane',     interval '30 days',  NULL,                                         'Trip to Lagos',                 0,         NULL,               interval '25 days'),
    ('acme', 'hannah-past',      'hannah.quinn',     'ANNUAL', -35, -31, 'APPROVED',  interval '50 days',  'tom.keane',     interval '49 days',  NULL,                                         NULL,                            0,         NULL,               NULL),
    -- Customer Success
    ('acme', 'niamh-own',        'niamh.kelly',      'ANNUAL',  35,  39, 'PENDING',   interval '30 hours',  NULL,            NULL,                NULL,                                         'School mid-term break',         0,         NULL,               NULL),
    ('acme', 'aisling-fresh',    'aisling.ryan',     'ANNUAL',  15,  17, 'PENDING',   interval '1 day',    NULL,            NULL,                NULL,                                         'Friend''s wedding',             0,         NULL,               NULL),
    ('acme', 'sarah-upcoming',   'sarah.murphy',     'ANNUAL',  28,  32, 'APPROVED',  interval '12 days',  'niamh.kelly',   interval '11 days',  NULL,                                         'Lisbon trip',                   0,         NULL,               NULL),
    ('acme', 'sarah-past',       'sarah.murphy',     'ANNUAL', -49, -45, 'APPROVED',  interval '60 days',  'niamh.kelly',   interval '59 days',  NULL,                                         NULL,                            0,         NULL,               NULL),
    ('acme', 'sarah-withdrawn',  'sarah.murphy',     'ANNUAL',  42,  44, 'CANCELLED', interval '5 days',   NULL,            NULL,                NULL,                                         'Plans changed',                 0,         NULL,               interval '4 days'),
    ('acme', 'patrick-next-week','patrick.hughes',   'ANNUAL',   9,  11, 'APPROVED',  interval '8 days',   'niamh.kelly',   interval '7 days',   NULL,                                         NULL,                            0,         NULL,               NULL),
    ('acme', 'chloe-past',       'chloe.martin',     'ANNUAL', -63, -59, 'APPROVED',  interval '75 days',  'niamh.kelly',   interval '74 days',  NULL,                                         NULL,                            0,         NULL,               NULL),
    -- People
    ('acme', 'ronan-this-week',  'ronan.doyle',      'ANNUAL',   1,   3, 'APPROVED',  interval '21 days',  'aoife.byrne',   interval '20 days',  NULL,                                         NULL,                            0,         NULL,               NULL),
    -- Brightwave (second tenant)
    ('brightwave', 'sarah-pending', 'sarah.fischer', 'ANNUAL',  14,  16, 'PENDING',   interval '30 hours',  NULL,            NULL,                NULL,                                         'Design festival in Copenhagen', 0,         NULL,               NULL),
    ('brightwave', 'ben-next-week', 'ben.carter',    'ANNUAL',   7,   9, 'APPROVED',  interval '10 days',  'tom.becker',    interval '9 days',   NULL,                                         NULL,                            0,         NULL,               NULL)
) AS r(tenant, key, employee, leave_type, start_offset, end_offset, status, created_ago, decided_by, decided_ago,
       decision_note, reason, reminders, reminded_ago, cancelled_ago);
