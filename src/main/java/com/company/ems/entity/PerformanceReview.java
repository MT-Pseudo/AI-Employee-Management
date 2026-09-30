package com.company.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "performance_reviews", indexes = @Index(name = "idx_review_employee", columnList = "employee_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PerformanceReview {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, foreignKey = @ForeignKey(name = "fk_review_employee"))
    private Employee employee;

    @Column(name = "review_date", nullable = false) private LocalDate reviewDate;
    @Column(nullable = false) private Integer rating;
    @Column(name = "goals_met_percent") private Integer goalsMetPercent;
    @Column(length = 1000) private String strengths;
    @Column(length = 1000) private String improvements;
    @Column(name = "reviewer_name", length = 120) private String reviewerName;
}