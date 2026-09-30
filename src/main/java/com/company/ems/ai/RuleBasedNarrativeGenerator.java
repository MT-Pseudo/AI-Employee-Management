package com.company.ems.ai;

import java.util.List;

public class RuleBasedNarrativeGenerator implements NarrativeGenerator {

    @Override public String name() { return "rule-based-v1"; }

    @Override
    public String employeeNarrative(EmployeeMetrics m, List<String> insights, List<String> recommendations) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "%s has been with the organisation for %d month(s), currently serving as %s in the %s team. ",
                m.employeeName(), m.tenureMonths(),
                safe(m.designation(), "an unassigned role"), safe(m.department(), "unassigned")));
        sb.append(String.format(
                "Over the last 30 days the attendance rate was %.0f%% with a late-arrival rate of %.0f%%, "
                + "and %d approved leave day(s) were taken in the last 90 days. ",
                m.attendanceRate()*100, m.lateRate()*100, m.approvedLeaveDaysLast90()));
        if (m.averageRating() != null)
            sb.append(String.format("Average performance rating stands at %.2f/5 and the recent trend is %s. ",
                    m.averageRating(), m.ratingTrend().toLowerCase()));
        else
            sb.append("No performance reviews have been recorded yet. ");
        sb.append(String.format("The composite attrition-risk score is %d/100, placing this employee in the %s risk band. ",
                m.attritionRiskScore(), m.riskBand()));
        if (!insights.isEmpty())
            sb.append("Key observations: ").append(String.join(" ", insights)).append(" ");
        if (!recommendations.isEmpty())
            sb.append("Recommended next steps: ").append(String.join(" ", recommendations));
        return sb.toString().trim();
    }

    @Override
    public String organizationNarrative(OrgMetrics m, List<String> insights) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("As of today the organisation has %d employee record(s), of which %d are active. ",
                m.headcount(), m.activeEmployees()));
        sb.append(String.format("Overall attendance across tracked working days is %.1f%%. ",
                m.overallAttendanceRate()*100));
        sb.append(String.format("%d employee(s) were marked absent today and %d arrived late. ",
                m.absentToday(), m.lateToday()));
        if (m.pendingLeaveRequests() > 0)
            sb.append(String.format("There are %d leave request(s) awaiting a decision. ", m.pendingLeaveRequests()));
        else
            sb.append("There are no pending leave requests. ");
        if (m.attendanceRateByDepartment() != null && !m.attendanceRateByDepartment().isEmpty()) {
            String worst = m.attendanceRateByDepartment().entrySet().stream()
                    .min(java.util.Map.Entry.comparingByValue()).map(java.util.Map.Entry::getKey).orElse(null);
            String best = m.attendanceRateByDepartment().entrySet().stream()
                    .max(java.util.Map.Entry.comparingByValue()).map(java.util.Map.Entry::getKey).orElse(null);
            if (worst != null && best != null && !worst.equals(best))
                sb.append(String.format("%s shows the strongest attendance discipline while %s lags behind. ", best, worst));
        }
        if (!insights.isEmpty())
            sb.append("Notable findings: ").append(String.join(" ", insights));
        return sb.toString().trim();
    }

    private String safe(String v, String fb) { return (v == null || v.isBlank()) ? fb : v; }
}