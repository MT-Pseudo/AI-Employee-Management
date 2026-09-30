package com.company.ems.controller;

import com.company.ems.dto.AiDtos.*;
import com.company.ems.ai.AiInsightService;
import com.company.ems.service.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports/ai")
@RequiredArgsConstructor
public class AiReportController {

    private final AiInsightService aiInsightService;
    private final SecurityUtils securityUtils;

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER') or @securityUtils.currentEmployeeId() == #employeeId")
    public ResponseEntity<EmployeeAiReport> employeeReport(@PathVariable Long employeeId) {
        return ResponseEntity.ok(aiInsightService.analyzeEmployee(employeeId));
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeAiReport> myReport() {
        return ResponseEntity.ok(aiInsightService.analyzeEmployee(securityUtils.currentEmployeeId()));
    }

    @GetMapping("/organization")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<OrganizationAiReport> organizationReport() {
        return ResponseEntity.ok(aiInsightService.analyzeOrganization());
    }
}