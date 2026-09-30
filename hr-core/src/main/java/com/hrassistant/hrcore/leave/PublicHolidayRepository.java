package com.hrassistant.hrcore.leave;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PublicHolidayRepository extends JpaRepository<PublicHoliday, UUID> {

    List<PublicHoliday> findByTenantIdAndHolidayDateBetween(UUID tenantId, LocalDate from, LocalDate to);
}
