package com.company.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests", indexes = {
        @Index(name = "idx_leave_employee", columnList = "employee_id"),
        @Index(name = "idx_leave_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, foreignKey = @ForeignKey(name = "fk_leave_employee"))
    private Employee employee;

    @Column(name = "from_date", nullable = false) private LocalDate fromDate;
    @Column(name = "to_date", nullable = false) private LocalDate toDate;
    @Column(name = "working_days", nullable = false) private int workingDays;
    @Column(nullable = false, length = 500) private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) @Builder.Default private LeaveStatus status = LeaveStatus.PENDING;

    @Column(name = "applied_on", nullable = false) private LocalDate appliedOn;
    @Column(name = "decided_by") private Long decidedBy;
    @Column(name = "decided_on") private LocalDateTime decidedOn;
    @Column(name = "decision_note", length = 500) private String decisionNote;
}