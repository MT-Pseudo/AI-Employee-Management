package com.company.ems.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class AiDtos {
    private AiDtos() {}

    public record EmployeeAiReport(
            Long employeeId, String employeeName, String department, String designation,
            int tenureMonths, double attendanceRate, double lateRate,
            int approvedLeaveDaysLast90, Double averageRating, String ratingTrend,
            int attritionRiskScore, String riskBand,
            List<String> insights, List<String> recommendations,
            String narrative, String generatedBy) {}

    public record OrganizationAiReport(
            LocalDate generatedOn, long headcount, long activeEmployees, long pendingLeaveRequests,
            double overallAttendanceRate,
            Map<String, Long> headcountByDepartment,
            Map<String, Double> attendanceRateByDepartment,
            List<String> topPerformers, List<String> highRiskEmployees,
            List<String> insights, String narrative, String generatedBy) {}
}