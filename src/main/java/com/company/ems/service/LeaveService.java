package com.company.ems.service;

import com.company.ems.dto.LeaveDtos.*;
import com.company.ems.entity.*;
import com.company.ems.exception.*;
import com.company.ems.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRepository;
    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final SecurityUtils securityUtils;

    @Transactional
    public LeaveResponse apply(Long employeeId, ApplyLeaveRequest r) {
        if (r.toDate().isBefore(r.fromDate()))
            throw new BusinessException("End date cannot be before start date");
        if (r.fromDate().isBefore(LocalDate.now().minusDays(1)))
            throw new BusinessException("Cannot apply for past dates");
        Employee e = employeeService.getEntity(employeeId);
        if (leaveRepository.countOverlapping(employeeId, r.fromDate(), r.toDate()) > 0)
            throw new BusinessException("Overlapping leave request already exists");
        long workingDays = AttendanceService.countWorkingDays(r.fromDate(), r.toDate());
        if (workingDays == 0) throw new BusinessException("Range contains no working days");
        LeaveRequest l = LeaveRequest.builder()
                .employee(e).fromDate(r.fromDate()).toDate(r.toDate())
                .workingDays((int) workingDays).reason(r.reason())
                .status(LeaveStatus.PENDING).appliedOn(LocalDate.now()).build();
        return LeaveResponse.from(leaveRepository.save(l));
    }

    @Transactional
    public LeaveResponse approve(Long id, DecisionRequest r) {
        LeaveRequest l = getEntity(id);
        if (l.getStatus() != LeaveStatus.PENDING)
            throw new BusinessException("Only PENDING can be approved (current: " + l.getStatus() + ")");
        l.setStatus(LeaveStatus.APPROVED);
        l.setDecidedBy(securityUtils.currentUser().getId());
        l.setDecidedOn(LocalDateTime.now());
        l.setDecisionNote(r == null ? null : r.note());
        attendanceService.markLeaveRange(l.getEmployee(), l.getFromDate(), l.getToDate());
        return LeaveResponse.from(leaveRepository.save(l));
    }

    @Transactional
    public LeaveResponse reject(Long id, DecisionRequest r) {
        LeaveRequest l = getEntity(id);
        if (l.getStatus() != LeaveStatus.PENDING)
            throw new BusinessException("Only PENDING can be rejected (current: " + l.getStatus() + ")");
        l.setStatus(LeaveStatus.REJECTED);
        l.setDecidedBy(securityUtils.currentUser().getId());
        l.setDecidedOn(LocalDateTime.now());
        l.setDecisionNote(r == null ? null : r.note());
        return LeaveResponse.from(leaveRepository.save(l));
    }

    @Transactional
    public LeaveResponse cancel(Long id) {
        LeaveRequest l = getEntity(id);
        securityUtils.requireSelfOrPrivileged(l.getEmployee().getId());
        if (l.getStatus() == LeaveStatus.REJECTED || l.getStatus() == LeaveStatus.CANCELLED)
            throw new BusinessException("Request is already " + l.getStatus());
        l.setStatus(LeaveStatus.CANCELLED);
        return LeaveResponse.from(leaveRepository.save(l));
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> forEmployee(Long employeeId) {
        employeeService.getEntity(employeeId);
        return leaveRepository.findByEmployeeIdOrderByAppliedOnDesc(employeeId)
                .stream().map(LeaveResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> pending() {
        return leaveRepository.findByStatusOrderByAppliedOnAsc(LeaveStatus.PENDING)
                .stream().map(LeaveResponse::from).toList();
    }

    private LeaveRequest getEntity(Long id) {
        return leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found: " + id));
    }
}