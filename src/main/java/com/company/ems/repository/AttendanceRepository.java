package com.company.ems.repository;

import com.company.ems.entity.Attendance;
import com.company.ems.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);
    List<Attendance> findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(Long employeeId, LocalDate from, LocalDate to);
    List<Attendance> findByWorkDate(LocalDate workDate);
    long countByEmployeeIdAndStatusAndWorkDateBetween(Long employeeId, AttendanceStatus status, LocalDate from, LocalDate to);
    long countByWorkDateAndStatus(LocalDate workDate, AttendanceStatus status);
    List<Attendance> findByWorkDateBetween(LocalDate from, LocalDate to);
}