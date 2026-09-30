package com.company.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = @Index(name = "idx_user_username", columnList = "username", unique = true))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60) private String username;
    @Column(nullable = false, length = 100) private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private Role role;

    @Column(name = "employee_id") private Long employeeId;

    @Column(nullable = false) @Builder.Default private boolean enabled = true;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;

    @PrePersist void onCreate() { this.createdAt = LocalDateTime.now(); }
}