package com.hrassistant.hrcore.leave;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "public_holiday")
class PublicHoliday {

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "holiday_date")
    private LocalDate holidayDate;

    private String name;

    protected PublicHoliday() {
        // for JPA
    }

    LocalDate getHolidayDate() {
        return holidayDate;
    }
}
