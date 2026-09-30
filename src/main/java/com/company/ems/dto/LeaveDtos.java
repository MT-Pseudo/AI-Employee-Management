package com.company.ems.dto;

import com.company.ems.entity.LeaveRequest;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class LeaveDtos {
    private LeaveDtos() {}

    public record ApplyLeaveRequest(
            @NotNull LocalDate fromDate,
            @NotNull LocalDate toDate,
            @NotBlank @Size(max=500) String reason) {}

    public record DecisionRequest(@Size(max=500) String note) {}

    public record LeaveResponse(
            Long id, Long employeeId, String employeeName,
            LocalDate fromDate, LocalDate toDate, int workingDays, String reason,
            String status, LocalDate appliedOn, Long decidedBy,
            LocalDateTime decidedOn, String decisionNote) {
        public static LeaveResponse from(LeaveRequest l) {
            return new LeaveResponse(
                    l.getId(), l.getEmployee().getId(), l.getEmployee().getFullName(),
                    l.getFromDate(), l.getToDate(), l.getWorkingDays(),
                    l.getReason(), l.getStatus().name(), l.getAppliedOn(),
                    l.getDecidedBy(), l.getDecidedOn(), l.getDecisionNote());
        }
    }
}