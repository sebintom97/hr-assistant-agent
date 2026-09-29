package com.hrassistant.hrcore.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Relationships (team, manager) are plain UUID columns, not @ManyToOne. Deliberate:
 * no lazy loading surprises, no accidental N+1 queries, and other modules only ever see ids.
 * Read only for now; creating and editing employees is not part of the prototype yet.
 */
@Entity
@Table(name = "employee")
public class Employee {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "manager_id")
    private UUID managerId;

    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "job_title")
    private String jobTitle;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    private boolean active;

    protected Employee() {
        // for JPA
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public UUID getManagerId() {
        return managerId;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public Role getRole() {
        return role;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public boolean isActive() {
        return active;
    }
}
