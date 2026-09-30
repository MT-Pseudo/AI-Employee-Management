package com.company.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "attendance",
        uniqueConstraints = @UniqueConstraint(name = "uk_attendance_emp_date", columnNames = {"employee_id", "work_date"}),
        indexes = @Index(name = "idx_attendance_date", columnList = "work_date"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Attendance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attendance_employee"))
    private Employee employee;

    @Column(name = "work_date", nullable = false) private LocalDate workDate;
    @Column(name = "check_in") private LocalTime checkIn;
    @Column(name = "check_out") private LocalTime checkOut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private AttendanceStatus status;

    @Column(name = "hours_worked") private Double hoursWorked;
    @Column(length = 255) private String remarks;

    public void computeHours() {
        if (checkIn != null && checkOut != null) {
            long minutes = Duration.between(checkIn, checkOut).toMinutes();
            this.hoursWorked = minutes <= 0 ? 0.0 : Math.round((minutes / 60.0) * 100.0) / 100.0;
        }
    }
}