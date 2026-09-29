package com.hrassistant.hrcore.leave;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Plain unit test: no Spring, no database, runs in milliseconds. */
class WorkingDayCalculatorTest {

    // Week of Monday 26 Oct 2026, which is the October bank holiday.
    private static final LocalDate MON = LocalDate.of(2026, 10, 26);
    private static final LocalDate FRI = LocalDate.of(2026, 10, 30);

    @Test
    void fullWeekIsFiveDays() {
        assertThat(WorkingDayCalculator.count(MON, FRI, Set.of())).isEqualTo(5);
    }

    @Test
    void weekendsDontCount() {
        // Friday to next Monday = 2 working days
        assertThat(WorkingDayCalculator.count(FRI, FRI.plusDays(3), Set.of())).isEqualTo(2);
    }

    @Test
    void publicHolidaysDontCount() {
        assertThat(WorkingDayCalculator.count(MON, FRI, Set.of(MON))).isEqualTo(4);
    }

    @Test
    void singleDayCountsOnce() {
        assertThat(WorkingDayCalculator.count(FRI, FRI, Set.of())).isEqualTo(1);
    }

    @Test
    void weekendOnlyIsZero() {
        assertThat(WorkingDayCalculator.count(FRI.plusDays(1), FRI.plusDays(2), Set.of())).isZero();
    }
}
