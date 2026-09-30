package com.company.ems.dto;

import com.company.ems.entity.Employee;
import com.company.ems.entity.EmployeeStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class EmployeeDtos {
    private EmployeeDtos() {}

    public record EmployeeRequest(
            @NotBlank @Size(max=60) String firstName,
            @NotBlank @Size(max=60) String lastName,
            @NotBlank @Email @Size(max=120) String email,
            @Size(max=20) String phone,
            @Size(max=60) String department,
            @Size(max=60) String designation,
            @PositiveOrZero BigDecimal salary,
            LocalDate joiningDate,
            EmployeeStatus status,
            Long managerId) {}

    public record EmployeeResponse(
            Long id, String firstName, String lastName, String fullName,
            String email, String phone, String department, String designation,
            BigDecimal salary, LocalDate joiningDate, EmployeeStatus status, Long managerId) {

        public static EmployeeResponse from(Employee e) {
            return new EmployeeResponse(
                    e.getId(), e.getFirstName(), e.getLastName(), e.getFullName(),
                    e.getEmail(), e.getPhone(), e.getDepartment(), e.getDesignation(),
                    e.getSalary(), e.getJoiningDate(), e.getStatus(), e.getManagerId());
        }
    }
}