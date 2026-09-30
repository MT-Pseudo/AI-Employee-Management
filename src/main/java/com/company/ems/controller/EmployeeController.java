package com.company.ems.controller;

import com.company.ems.dto.EmployeeDtos.*;
import com.company.ems.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(r));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<Page<EmployeeResponse>> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "id") Pageable p) {
        return ResponseEntity.ok(employeeService.list(q, p));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER') or @securityUtils.currentEmployeeId() == #id")
    public ResponseEntity<EmployeeResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<EmployeeResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody EmployeeRequest r) {
        return ResponseEntity.ok(employeeService.update(id, r));
    }

    @GetMapping("/department/{department}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<EmployeeResponse>> byDept(@PathVariable String department) {
        return ResponseEntity.ok(employeeService.byDepartment(department));
    }

    @PatchMapping("/{id}/terminate")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Void> terminate(@PathVariable Long id) {
        employeeService.terminate(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}