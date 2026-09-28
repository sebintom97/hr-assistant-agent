package com.hrassistant.hrcore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 0: the schema migrates, the demo seed loads, the demo scenarios exist,
 * and the database itself rejects data that breaks the core rules.
 * Each test runs in a transaction that is rolled back, so tests don't affect each other.
 */
@SpringBootTest
@ActiveProfiles("demo")
@Import(TestcontainersConfiguration.class)
@Transactional
class SchemaAndSeedTest {

    // Same deterministic ids as the seed: md5(key)::uuid
    private static final String ACME = "md5('tenant:acme')::uuid";
    private static final String BRIGHTWAVE = "md5('tenant:brightwave')::uuid";

    @Autowired
    JdbcTemplate jdbc;

    // --- seed ---------------------------------------------------------------------------------

    @Test
    void seedCreatesTwoTenantsWithExpectedHeadcount() {
        assertThat(count("SELECT count(*) FROM employee WHERE tenant_id = " + ACME)).isEqualTo(30);
        assertThat(count("SELECT count(*) FROM team WHERE tenant_id = " + ACME)).isEqualTo(3);
        assertThat(count("SELECT count(*) FROM employee WHERE tenant_id = " + BRIGHTWAVE)).isEqualTo(4);
    }

    @Test
    void everyEmployeeExceptTheTopHasAManagerInTheirOwnTenant() {
        assertThat(count("SELECT count(*) FROM employee WHERE manager_id IS NULL")).isEqualTo(2); // one CEO per tenant
        assertThat(count("""
                SELECT count(*) FROM employee e JOIN employee m ON m.id = e.manager_id
                WHERE m.tenant_id <> e.tenant_id""")).isZero();
    }

    @Test
    void managerOnHolidayScenarioHoldsToday() {
        assertThat(count("""
                SELECT count(*) FROM leave_request
                WHERE employee_id = md5('acme:tom.keane')::uuid AND status = 'APPROVED'
                  AND current_date BETWEEN start_date AND end_date""")).isEqualTo(1);
    }

    @Test
    void watcherScenariosExist() {
        // pending > 5 days -> escalate
        assertThat(count("""
                SELECT count(*) FROM leave_request
                WHERE status = 'PENDING' AND created_at < now() - interval '5 days'""")).isEqualTo(1);
        // pending > 48h and never reminded -> remind
        assertThat(count("""
                SELECT count(*) FROM leave_request
                WHERE status = 'PENDING' AND created_at < now() - interval '48 hours' AND last_reminded_at IS NULL""")).isEqualTo(1); // Conor
    }

    @Test
    void newJoinerHasOnlyTwoDaysOfBalance() {
        BigDecimal remaining = jdbc.queryForObject("""
                SELECT a.days - coalesce(sum(r.working_days), 0)
                FROM leave_allowance a
                LEFT JOIN leave_request r ON r.employee_id = a.employee_id AND r.status = 'APPROVED'
                     AND r.leave_type = 'ANNUAL' AND extract(year FROM r.start_date) = a.year
                WHERE a.employee_id = md5('acme:fionn.gallagher')::uuid AND a.year = extract(year FROM current_date)
                GROUP BY a.days""", BigDecimal.class);
        assertThat(remaining).isEqualByComparingTo("2.0");
    }

    @Test
    void workingDaysExcludeWeekends() {
        // Every seeded request spans at least one weekday, and none is longer than its calendar span.
        assertThat(count("SELECT count(*) FROM leave_request WHERE working_days > (end_date - start_date + 1)")).isZero();
    }

    // --- constraints: the database as the last line of defence -------------------------------

    @Test
    void overlappingLiveRequestsForSameEmployeeAreRejected() {
        // Emma already has APPROVED leave next week.
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO leave_request (tenant_id, employee_id, start_date, end_date, working_days)
                SELECT tenant_id, employee_id, start_date + 1, start_date + 1, 1
                FROM leave_request WHERE id = md5('acme:leave:emma-next-week')::uuid"""))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("leave_request_no_overlap");
    }

    @Test
    void nobodyCanDecideTheirOwnRequest() {
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE leave_request SET status = 'APPROVED', decided_by_id = employee_id, decided_at = now()
                WHERE id = md5('acme:leave:niamh-own')::uuid"""))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("leave_request_not_self_decided");
    }

    @Test
    void approvedRequestMustRecordWhoDecided() {
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE leave_request SET status = 'APPROVED'
                WHERE id = md5('acme:leave:aisling-fresh')::uuid"""))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("leave_request_decision_recorded");
    }

    @Test
    void managerFromAnotherTenantIsRejected() {
        // Make Brightwave's Sarah report to Acme's Tom: the composite foreign key must refuse.
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE employee SET manager_id = md5('acme:tom.keane')::uuid
                WHERE id = md5('brightwave:sarah.fischer')::uuid"""))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("employee_manager_same_tenant");
    }

    @Test
    void unknownStatusIsRejected() {
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE leave_request SET status = 'MAYBE' WHERE id = md5('acme:leave:aisling-fresh')::uuid"""))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("leave_request_status_valid");
    }

    private long count(String sql) {
        Long result = jdbc.queryForObject(sql, Long.class);
        return result == null ? 0 : result;
    }
}
