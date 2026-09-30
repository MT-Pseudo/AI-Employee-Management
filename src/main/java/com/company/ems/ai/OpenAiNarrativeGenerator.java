package com.company.ems.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public class OpenAiNarrativeGenerator implements NarrativeGenerator {

    private final NarrativeGenerator fallback;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiNarrativeGenerator(NarrativeGenerator fallback, String apiKey, String model) {
        this.fallback = fallback;
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder().baseUrl("https://api.openai.com/v1").build();
    }

    @Override public String name() { return "openai:" + model; }

    @Override
    public String employeeNarrative(EmployeeMetrics metrics, List<String> insights, List<String> recommendations) {
        String prompt = """
                You are an HR analytics assistant. Write a concise, factual 4-6 sentence briefing
                for an HR manager about the employee below. Do not invent data.
                Employee: %s (ID %d)
                Department: %s | Designation: %s | Tenure: %d months
                Attendance rate (30d): %.1f%% | Late rate: %.1f%%
                Approved leave (90d): %d
                Avg rating: %s | Trend: %s
                Attrition risk: %d/100 (%s)
                Signals: %s
                Suggested actions: %s
                """.formatted(metrics.employeeName(), metrics.employeeId(),
                metrics.department(), metrics.designation(), metrics.tenureMonths(),
                metrics.attendanceRate()*100, metrics.lateRate()*100,
                metrics.approvedLeaveDaysLast90(),
                metrics.averageRating() == null ? "n/a" : String.format("%.2f", metrics.averageRating()),
                metrics.ratingTrend(), metrics.attritionRiskScore(), metrics.riskBand(),
                String.join("; ", insights), String.join("; ", recommendations));
        return callModel(prompt, () -> fallback.employeeNarrative(metrics, insights, recommendations));
    }

    @Override
    public String organizationNarrative(OrgMetrics metrics, List<String> insights) {
        String prompt = """
                You are an HR analytics assistant. Write a concise 5-7 sentence workforce briefing.
                Headcount: %d (active: %d) | Pending leave: %d
                Overall attendance: %.1f%%
                Absent today: %d | Late today: %d
                Headcount by dept: %s
                Attendance by dept: %s
                Signals: %s
                """.formatted(metrics.headcount(), metrics.activeEmployees(),
                metrics.pendingLeaveRequests(), metrics.overallAttendanceRate()*100,
                metrics.absentToday(), metrics.lateToday(),
                metrics.headcountByDepartment(), metrics.attendanceRateByDepartment(),
                String.join("; ", insights));
        return callModel(prompt, () -> fallback.organizationNarrative(metrics, insights));
    }

    private String callModel(String prompt, Supplier<String> onFailure) {
        if (apiKey == null || apiKey.isBlank()) return onFailure.get();
        try {
            String response = restClient.post().uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(Map.of("model", model, "temperature", 0.2, "max_tokens", 400,
                            "messages", List.of(
                                    Map.of("role","system","content","You write factual, concise HR briefings."),
                                    Map.of("role","user","content", prompt))))
                    .retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) return onFailure.get();
            return content.asText().trim();
        } catch (Exception ex) {
            log.warn("LLM narrative failed, using rule-based fallback: {}", ex.getMessage());
            return onFailure.get();
        }
    }
}