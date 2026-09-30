package com.company.ems.controller;

import com.company.ems.dto.LeaveDtos.*;
import com.company.ems.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    private final SecurityUtils securityUtils;

    @PostMapping("/apply")
    public ResponseEntity<LeaveResponse> apply(@Valid @RequestBody ApplyLeaveRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leaveService.apply(securityUtils.currentEmployeeId(), r));
    }

    @GetMapping("/me")
    public ResponseEntity<List<LeaveResponse>> myLeaves() {
        return ResponseEntity.ok(leaveService.forEmployee(securityUtils.currentEmployeeId()));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<LeaveResponse>> forEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveService.forEmployee(employeeId));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<LeaveResponse>> pending() {
        return ResponseEntity.ok(leaveService.pending());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<LeaveResponse> approve(@PathVariable Long id,
                                                 @RequestBody(required = false) DecisionRequest r) {
        return ResponseEntity.ok(leaveService.approve(id, r));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<LeaveResponse> reject(@PathVariable Long id,
                                                @RequestBody(required = false) DecisionRequest r) {
        return ResponseEntity.ok(leaveService.reject(id, r));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<LeaveResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(leaveService.cancel(id));
    }
}