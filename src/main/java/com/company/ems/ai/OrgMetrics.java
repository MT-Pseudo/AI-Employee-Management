package com.company.ems.ai;
import java.util.Map;
public record OrgMetrics(
        long headcount, long activeEmployees, long pendingLeaveRequests, double overallAttendanceRate,
        Map<String, Long> headcountByDepartment, Map<String, Double> attendanceRateByDepartment,
        long absentToday, long lateToday) {}