package com.company.ems.repository;

import com.company.ems.entity.LeaveRequest;
import com.company.ems.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeIdOrderByAppliedOnDesc(Long employeeId);
    List<LeaveRequest> findByStatusOrderByAppliedOnAsc(LeaveStatus status);

    @Query("""
            SELECT COUNT(l) FROM LeaveRequest l
            WHERE l.employee.id = :employeeId
              AND l.status IN ('PENDING','APPROVED')
              AND l.fromDate <= :toDate
              AND l.toDate >= :fromDate
            """)
    long countOverlapping(@Param("employeeId") Long employeeId,
                          @Param("fromDate") LocalDate fromDate,
                          @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT COALESCE(SUM(l.workingDays),0) FROM LeaveRequest l
            WHERE l.employee.id = :employeeId AND l.status='APPROVED' AND l.fromDate >= :from
            """)
    Long sumApprovedLeaveDaysSince(@Param("employeeId") Long employeeId, @Param("from") LocalDate from);

    long countByStatus(LeaveStatus status);
}