package com.company.ems.ai;
import java.util.List;
public record EmployeeMetrics(
        Long employeeId, String employeeName, String department, String designation,
        int tenureMonths, double attendanceRate, double lateRate,
        int approvedLeaveDaysLast90, Double averageRating, String ratingTrend,
        List<Integer> recentRatings, int attritionRiskScore, String riskBand) {}