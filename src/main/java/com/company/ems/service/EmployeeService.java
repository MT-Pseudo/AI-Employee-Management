package com.company.ems.service;

import com.company.ems.dto.EmployeeDtos.*;
import com.company.ems.entity.*;
import com.company.ems.exception.*;
import com.company.ems.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Transactional
    public EmployeeResponse create(EmployeeRequest r) {
        if (employeeRepository.existsByEmailIgnoreCase(r.email()))
            throw new BusinessException("Employee with email already exists: " + r.email());
        Employee e = Employee.builder()
                .firstName(r.firstName()).lastName(r.lastName())
                .email(r.email().toLowerCase()).phone(r.phone())
                .department(r.department()).designation(r.designation())
                .salary(r.salary())
                .joiningDate(r.joiningDate() != null ? r.joiningDate() : LocalDate.now())
                .status(r.status() != null ? r.status() : EmployeeStatus.ACTIVE)
                .managerId(r.managerId()).build();
        return EmployeeResponse.from(employeeRepository.save(e));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest r) {
        Employee e = getEntity(id);
        if (!e.getEmail().equalsIgnoreCase(r.email()) && employeeRepository.existsByEmailIgnoreCase(r.email()))
            throw new BusinessException("Email already in use: " + r.email());
        e.setFirstName(r.firstName()); e.setLastName(r.lastName());
        e.setEmail(r.email().toLowerCase()); e.setPhone(r.phone());
        e.setDepartment(r.department()); e.setDesignation(r.designation());
        e.setSalary(r.salary());
        if (r.joiningDate() != null) e.setJoiningDate(r.joiningDate());
        if (r.status() != null) e.setStatus(r.status());
        e.setManagerId(r.managerId());
        return EmployeeResponse.from(employeeRepository.save(e));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(Long id) { return EmployeeResponse.from(getEntity(id)); }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(String q, Pageable p) {
        Page<Employee> page = (q == null || q.isBlank())
                ? employeeRepository.findAll(p)
                : employeeRepository.search(q.trim(), p);
        return page.map(EmployeeResponse::from);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> byDepartment(String dept) {
        return employeeRepository.findByDepartmentIgnoreCase(dept).stream()
                .map(EmployeeResponse::from).toList();
    }

    @Transactional
    public void terminate(Long id) {
        Employee e = getEntity(id);
        e.setStatus(EmployeeStatus.TERMINATED);
        employeeRepository.save(e);
    }

    @Transactional
    public void delete(Long id) { employeeRepository.delete(getEntity(id)); }

    @Transactional(readOnly = true)
    public Employee getEntity(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }
}