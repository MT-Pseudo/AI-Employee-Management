package com.company.ems.ai;

import com.company.ems.dto.AiDtos.*;
import com.company.ems.entity.*;
import com.company.ems.repository.*;
import com.company.ems.service.AttendanceService;
import com.company.ems.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiInsightService {

    private static final int WINDOW_DAYS = 30;
    private static final int LEAVE_WINDOW_DAYS = 90;

    private final EmployeeService employeeService;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRepository;
    private final PerformanceReviewRepository reviewRepository;
    private final NarrativeGenerator narrativeGenerator;

    @Transactional(readOnly = true)
    public EmployeeAiReport analyzeEmployee(Long employeeId) {
        Employee e = employeeService.getEntity(employeeId);
        EmployeeMetrics m = buildMetrics(e);
        List<String> insights = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        deriveEmployeeInsights(m, insights, recommendations);
        String narrative = narrativeGenerator.employeeNarrative(m, insights, recommendations);
        return new EmployeeAiReport(
                e.getId(), e.getFullName(), e.getDepartment(), e.getDesignation(),
                m.tenureMonths(), round(m.attendanceRate()*100, 2), round(m.lateRate()*100, 2),
                m.approvedLeaveDaysLast90(), m.averageRating(), m.ratingTrend(),
                m.attritionRiskScore(), m.riskBand(),
                insights, recommendations, narrative, narrativeGenerator.name());
    }

    private EmployeeMetrics buildMetrics(Employee employee) {
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(WINDOW_DAYS - 1L);
        List<Attendance> recs = attendanceRepository
                .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(employee.getId(), windowStart, today);
        long workingDays = AttendanceService.countWorkingDays(windowStart, today);
        long attended = recs.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT
                || a.getStatus() == AttendanceStatus.LATE
                || a.getStatus() == AttendanceStatus.HALF_DAY).count();
        long lateDays = recs.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        double attendanceRate = workingDays == 0 ? 1.0 : Math.min(1.0, (double) attended / workingDays);
        double lateRate = attended == 0 ? 0.0 : (double) lateDays / attended;

        Long leaveDays = leaveRepository.sumApprovedLeaveDaysSince(employee.getId(),
                today.minusDays(LEAVE_WINDOW_DAYS));
        int approvedLeave = leaveDays == null ? 0 : leaveDays.intValue();

        List<PerformanceReview> reviews = reviewRepository.findByEmployeeIdOrderByReviewDateDesc(employee.getId());
        Double avgRating = reviews.isEmpty() ? null
                : reviews.stream().mapToInt(PerformanceReview::getRating).average().orElse(0);
        List<Integer> recent = reviews.stream().limit(3).map(PerformanceReview::getRating).toList();
        String trend = computeTrend(recent);
        int tenure = employee.getJoiningDate() == null ? 0
                : (int) ChronoUnit.MONTHS.between(employee.getJoiningDate(), today);
        int risk = computeAttritionRisk(attendanceRate, lateRate, approvedLeave, avgRating, trend, tenure, employee.getStatus());
        return new EmployeeMetrics(employee.getId(), employee.getFullName(),
                employee.getDepartment(), employee.getDesignation(), tenure,
                attendanceRate, lateRate, approvedLeave,
                avgRating == null ? null : round(avgRating, 2), trend, recent,
                risk, riskBand(risk));
    }

    private int computeAttritionRisk(double attendanceRate, double lateRate, int leaveDays,
                                     Double avgRating, String trend, int tenure, EmployeeStatus status) {
        int s = 0;
        if (attendanceRate < 0.85) s += 25; else if (attendanceRate < 0.92) s += 12;
        if (lateRate > 0.20) s += 18; else if (lateRate > 0.10) s += 9;
        if (leaveDays > 15) s += 18; else if (leaveDays > 8) s += 9;
        if (avgRating != null) {
            if (avgRating < 3.0) s += 22; else if (avgRating < 3.8) s += 10;
        }
        switch (trend) {
            case "DECLINING" -> s += 15;
            case "IMPROVING" -> s -= 8;
            default -> { }
        }
        if (tenure < 6) s += 10;
        if (tenure > 48) s += 6;
        if (status == EmployeeStatus.PROBATION) s += 8;
        if (status == EmployeeStatus.ON_LEAVE) s += 5;
        return Math.max(0, Math.min(100, s));
    }

    private String riskBand(int score) {
        if (score >= 60) return "HIGH";
        if (score >= 30) return "MEDIUM";
        return "LOW";
    }

    private String computeTrend(List<Integer> recent) {
        if (recent.size() < 2) return "UNKNOWN";
        int newest = recent.get(0), oldest = recent.get(recent.size()-1);
        if (newest > oldest) return "IMPROVING";
        if (newest < oldest) return "DECLINING";
        return "STABLE";
    }

    private void deriveEmployeeInsights(EmployeeMetrics m, List<String> ins, List<String> rec) {
        if (m.attendanceRate() < 0.85) {
            ins.add(String.format("Attendance has dropped to %.0f%% over the last 30 days.", m.attendanceRate()*100));
            rec.add("Schedule a welfare check-in.");
        } else if (m.attendanceRate() >= 0.97) ins.add("Attendance is excellent.");
        if (m.lateRate() > 0.15) {
            ins.add(String.format("Late arrivals are %.0f%% of attended days.", m.lateRate()*100));
            rec.add("Discuss flexible start times.");
        }
        if (m.approvedLeaveDaysLast90() > 12) {
            ins.add(String.format("%d approved leave days taken in last 90 days.", m.approvedLeaveDaysLast90()));
            rec.add("Review workload and burnout indicators.");
        }
        if ("DECLINING".equals(m.ratingTrend())) {
            ins.add("Performance ratings are trending downward.");
            rec.add("Agree a 30-60-90 day improvement plan.");
        } else if ("IMPROVING".equals(m.ratingTrend())) {
            ins.add("Performance ratings are improving.");
            rec.add("Consider stretch assignments.");
        }
        if (m.averageRating() != null && m.averageRating() >= 4.5) {
            ins.add(String.format("Sustained high performance (%.2f/5).", m.averageRating()));
            rec.add("Include in succession planning.");
        }
        if (m.tenureMonths() < 6) {
            ins.add("Tenure under 6 months - early-attrition risk.");
            rec.add("Complete onboarding milestones and 90-day check-in.");
        }
        if (m.attritionRiskScore() >= 60) rec.add("Escalate to HR for a retention conversation.");
    }

    @Transactional(readOnly = true)
    public OrganizationAiReport analyzeOrganization() {
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(WINDOW_DAYS - 1L);
        List<Employee> employees = employeeRepository.findAll();
        long headcount = employees.size();
        long active = employees.stream().filter(e -> e.getStatus() == EmployeeStatus.ACTIVE
                || e.getStatus() == EmployeeStatus.PROBATION).count();
        Map<String, Long> headcountByDept = new TreeMap<>();
        Map<String, Double> attendanceByDept = new TreeMap<>();
        double totalRate = 0; int deptsWithData = 0;
        for (Employee e : employees) {
            String dept = (e.getDepartment() == null || e.getDepartment().isBlank()) ? "Unassigned" : e.getDepartment();
            headcountByDept.merge(dept, 1L, Long::sum);
        }
        for (String dept : headcountByDept.keySet()) {
            List<Employee> list = employees.stream()
                    .filter(e -> dept.equalsIgnoreCase(e.getDepartment() == null || e.getDepartment().isBlank()
                            ? "Unassigned" : e.getDepartment())).toList();
            double sum = 0; int counted = 0;
            for (Employee e : list) {
                List<Attendance> recs = attendanceRepository
                        .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(e.getId(), windowStart, today);
                long wd = AttendanceService.countWorkingDays(windowStart, today);
                long att = recs.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT
                        || a.getStatus() == AttendanceStatus.LATE
                        || a.getStatus() == AttendanceStatus.HALF_DAY).count();
                if (wd > 0) { sum += Math.min(1.0, (double) att / wd); counted++; }
            }
            double r = counted == 0 ? 0.0 : sum / counted;
            attendanceByDept.put(dept, round(r*100, 2));
            if (counted > 0) { totalRate += r; deptsWithData++; }
        }
        double overall = deptsWithData == 0 ? 0.0 : totalRate / deptsWithData;
        long absentToday = attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.ABSENT);
        long lateToday = attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.LATE);
        long pending = leaveRepository.countByStatus(LeaveStatus.PENDING);
        List<String> top = new ArrayList<>();
        List<String> highRisk = new ArrayList<>();
        for (Employee e : employees) {
            Double avg = reviewRepository.averageRating(e.getId());
            if (avg != null && avg >= 4.5)
                top.add(String.format("%s (%s) - %.2f/5", e.getFullName(), safeDept(e.getDepartment()), avg));
        }
        List<String> insights = new ArrayList<>();
        attendanceByDept.entrySet().stream().min(Map.Entry.comparingByValue()).ifPresent(entry -> {
            if (entry.getValue() < 85.0)
                insights.add(String.format("%s has the lowest attendance rate at %.1f%%.",
                        entry.getKey(), entry.getValue()));
        });
        if (pending > 5) insights.add(String.format("%d leave requests are pending.", pending));
        if (absentToday > 0) insights.add(String.format("%d employee(s) are absent today.", absentToday));
        if (top.isEmpty()) insights.add("No employees currently meet the top-performer threshold (>=4.5).");
        for (Employee e : employees) {
            if (e.getStatus() == EmployeeStatus.TERMINATED) continue;
            EmployeeMetrics m = buildMetrics(e);
            if (m.attritionRiskScore() >= 60)
                highRisk.add(String.format("%s (%s) - risk %d/100",
                        e.getFullName(), safeDept(e.getDepartment()), m.attritionRiskScore()));
        }
        OrgMetrics org = new OrgMetrics(headcount, active, pending, overall,
                headcountByDept, attendanceByDept, absentToday, lateToday);
        String narrative = narrativeGenerator.organizationNarrative(org, insights);
        return new OrganizationAiReport(today, headcount, active, pending,
                round(overall*100, 2), headcountByDept, attendanceByDept,
                top, highRisk, insights, narrative, narrativeGenerator.name());
    }

    private String safeDept(String d) { return (d == null || d.isBlank()) ? "Unassigned" : d; }
    private static double round(double v, int p) {
        double f = Math.pow(10, p);
        return Math.round(v * f) / f;
    }
}