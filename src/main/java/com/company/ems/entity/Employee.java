package com.company.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees", indexes = {
        @Index(name = "idx_emp_email", columnList = "email"),
        @Index(name = "idx_emp_department", columnList = "department"),
        @Index(name = "idx_emp_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(length = 20) private String phone;
    @Column(length = 60) private String department;
    @Column(length = 60) private String designation;
    @Column(precision = 12, scale = 2) private BigDecimal salary;
    @Column(name = "joining_date") private LocalDate joiningDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @Column(name = "manager_id") private Long managerId;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @PrePersist void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) this.status = EmployeeStatus.ACTIVE;
    }
    @PreUpdate void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    @Transient public String getFullName() { return firstName + " " + lastName; }
}