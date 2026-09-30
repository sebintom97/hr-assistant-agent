package com.hrassistant.hrcore.leave;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Every query filters by tenantId (rule 5). */
interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    Optional<LeaveRequest> findByTenantIdAndId(UUID tenantId, UUID id);

    List<LeaveRequest> findByTenantIdAndEmployeeIdOrderByStartDateDesc(UUID tenantId, UUID employeeId);

    List<LeaveRequest> findByTenantIdAndStatusAndEmployeeIdInOrderByCreatedAtAsc(
            UUID tenantId, LeaveStatus status, Collection<UUID> employeeIds);

    /** Sum of working days, e.g. all APPROVED ANNUAL leave starting in 2026. The heart of the derived balance. */
    @Query("""
            SELECT COALESCE(SUM(r.workingDays), 0) FROM LeaveRequest r
            WHERE r.tenantId = :tenantId AND r.employeeId = :employeeId
              AND r.status = :status AND r.leaveType = :leaveType
              AND r.startDate BETWEEN :from AND :to""")
    BigDecimal sumWorkingDays(@Param("tenantId") UUID tenantId, @Param("employeeId") UUID employeeId,
                              @Param("status") LeaveStatus status, @Param("leaveType") LeaveType leaveType,
                              @Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Two date ranges overlap when each one starts before the other ends:
     * start1 <= end2 AND end1 >= start2. (Same rule as the database's EXCLUDE constraint.)
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM LeaveRequest r
            WHERE r.tenantId = :tenantId AND r.employeeId = :employeeId
              AND r.status IN :statuses
              AND r.startDate <= :endDate AND r.endDate >= :startDate""")
    boolean existsOverlapping(@Param("tenantId") UUID tenantId, @Param("employeeId") UUID employeeId,
                              @Param("statuses") Collection<LeaveStatus> statuses,
                              @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT r FROM LeaveRequest r
            WHERE r.tenantId = :tenantId AND r.employeeId IN :employeeIds
              AND r.status = :status
              AND r.startDate <= :to AND r.endDate >= :from
            ORDER BY r.startDate""")
    List<LeaveRequest> findOverlappingForEmployees(@Param("tenantId") UUID tenantId,
                                                   @Param("employeeIds") Collection<UUID> employeeIds,
                                                   @Param("status") LeaveStatus status,
                                                   @Param("from") LocalDate from, @Param("to") LocalDate to);
}
