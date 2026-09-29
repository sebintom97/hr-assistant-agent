package com.hrassistant.hrcore.leave;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** "How many days of leave is 10 to 14 November?" Weekdays minus the tenant's public holidays. */
@Component
class WorkingDayCalculator {

    private final PublicHolidayRepository holidays;

    WorkingDayCalculator(PublicHolidayRepository holidays) {
        this.holidays = holidays;
    }

    BigDecimal workingDays(UUID tenantId, LocalDate start, LocalDate end) {
        Set<LocalDate> holidayDates = holidays.findByTenantIdAndHolidayDateBetween(tenantId, start, end).stream()
                .map(PublicHoliday::getHolidayDate)
                .collect(Collectors.toSet());
        return BigDecimal.valueOf(count(start, end, holidayDates)).setScale(1);
    }

    /** The pure rule, separate from the database lookup so it can be unit tested directly. */
    static long count(LocalDate start, LocalDate end, Set<LocalDate> holidays) {
        return start.datesUntil(end.plusDays(1))        // end is inclusive
                .filter(day -> day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY)
                .filter(day -> !holidays.contains(day))
                .count();
    }
}
