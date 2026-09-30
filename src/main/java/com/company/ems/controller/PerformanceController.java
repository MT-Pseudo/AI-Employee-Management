package com.company.ems.controller;

import com.company.ems.dto.PerformanceDtos.*;
import com.company.ems.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;
    private final SecurityUtils securityUtils;

    @PostMapping("/reviews")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<ReviewResponse> create(@Valid @RequestBody ReviewRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(performanceService.create(r));
    }

    @GetMapping("/reviews")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<ReviewResponse>> all() {
        return ResponseEntity.ok(performanceService.all());
    }

    @GetMapping("/reviews/me")
    public ResponseEntity<List<ReviewResponse>> myReviews() {
        return ResponseEntity.ok(performanceService.forEmployee(securityUtils.currentEmployeeId()));
    }

    @GetMapping("/reviews/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<ReviewResponse>> forEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(performanceService.forEmployee(employeeId));
    }

    @DeleteMapping("/reviews/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        performanceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}