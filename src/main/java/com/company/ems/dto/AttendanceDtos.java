package com.company.ems.dto;

import com.company.ems.entity.Attendance;
import com.company.ems.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public final class AttendanceDtos {
    private AttendanceDtos() {}

    public record AttendanceResponse(
            Long id, Long employeeId, String employeeName, LocalDate workDate,
            LocalTime checkIn, LocalTime checkOut, AttendanceStatus status,
            Double hoursWorked, String remarks) {
        public static AttendanceResponse from(Attendance a) {
            return new AttendanceResponse(
                    a.getId(), a.getEmployee().getId(), a.getEmployee().getFullName(),
                    a.getWorkDate(), a.getCheckIn(), a.getCheckOut(),
                    a.getStatus(), a.getHoursWorked(), a.getRemarks());
        }
    }

    public record ManualAttendanceRequest(
            @NotNull Long employeeId,
            LocalDate workDate,
            LocalTime checkIn,
            LocalTime checkOut,
            AttendanceStatus status,
            String remarks) {}

    public record AttendanceSummary(
            Long employeeId, LocalDate from, LocalDate to,
            long workingDays, long presentDays, long lateDays, long absentDays, long leaveDays,
            double attendanceRate, double totalHoursWorked) {}
}