package com.company.ems.controller;

import com.company.ems.dto.AttendanceDtos.*;
import com.company.ems.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final SecurityUtils securityUtils;

    @PostMapping("/check-in")
    public ResponseEntity<AttendanceResponse> checkIn(@RequestParam(required = false) String remarks) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.checkIn(securityUtils.currentEmployeeId(), remarks));
    }

    @PostMapping("/check-out")
    public ResponseEntity<AttendanceResponse> checkOut() {
        return ResponseEntity.ok(attendanceService.checkOut(securityUtils.currentEmployeeId()));
    }

    @GetMapping("/me")
    public ResponseEntity<List<AttendanceResponse>> myHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(attendanceService.history(securityUtils.currentEmployeeId(), from, to));
    }

    @GetMapping("/me/summary")
    public ResponseEntity<AttendanceSummary> mySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(attendanceService.summary(securityUtils.currentEmployeeId(), from, to));
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<AttendanceResponse> upsert(@Valid @RequestBody ManualAttendanceRequest r) {
        return ResponseEntity.ok(attendanceService.upsert(r));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<AttendanceResponse>> employeeHistory(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(attendanceService.history(employeeId, from, to));
    }

    @GetMapping("/employee/{employeeId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<AttendanceSummary> employeeSummary(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(attendanceService.summary(employeeId, from, to));
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<AttendanceResponse>> forDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.forDate(date));
    }

    @PostMapping("/sweep/{date}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Integer> sweep(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.markAbsenteesFor(date));
    }
}