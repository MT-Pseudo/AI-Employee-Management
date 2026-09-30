package com.company.ems.dto;

import com.company.ems.entity.PerformanceReview;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class PerformanceDtos {
    private PerformanceDtos() {}

    public record ReviewRequest(
            @NotNull Long employeeId,
            @NotNull LocalDate reviewDate,
            @NotNull @Min(1) @Max(5) Integer rating,
            @Min(0) @Max(100) Integer goalsMetPercent,
            String strengths, String improvements, String reviewerName) {}

    public record ReviewResponse(
            Long id, Long employeeId, String employeeName, LocalDate reviewDate,
            Integer rating, Integer goalsMetPercent,
            String strengths, String improvements, String reviewerName) {
        public static ReviewResponse from(PerformanceReview r) {
            return new ReviewResponse(
                    r.getId(), r.getEmployee().getId(), r.getEmployee().getFullName(),
                    r.getReviewDate(), r.getRating(), r.getGoalsMetPercent(),
                    r.getStrengths(), r.getImprovements(), r.getReviewerName());
        }
    }
}