package com.company.ems.service;

import com.company.ems.dto.AttendanceDtos.*;
import com.company.ems.entity.*;
import com.company.ems.exception.*;
import com.company.ems.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeService employeeService;

    @Value("${app.attendance.work-start:09:30}")
    private String workStartRaw;

    @Transactional
    public AttendanceResponse checkIn(Long employeeId, String remarks) {
        LocalDate today = LocalDate.now();
        Attendance a = attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, today)
                .orElseGet(() -> Attendance.builder()
                        .employee(employeeService.getEntity(employeeId))
                        .workDate(today).build());
        if (a.getCheckIn() != null)
            throw new BusinessException("Already checked in today at " + a.getCheckIn());
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        a.setCheckIn(now);
        a.setStatus(now.isAfter(LocalTime.parse(workStartRaw)) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);
        if (remarks != null && !remarks.isBlank()) a.setRemarks(remarks);
        a.computeHours();
        return AttendanceResponse.from(attendanceRepository.save(a));
    }

    @Transactional
    public AttendanceResponse checkOut(Long employeeId) {
        LocalDate today = LocalDate.now();
        Attendance a = attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, today)
                .orElseThrow(() -> new BusinessException("You have not checked in today"));
        if (a.getCheckOut() != null)
            throw new BusinessException("Already checked out today at " + a.getCheckOut());
        a.setCheckOut(LocalTime.now().withSecond(0).withNano(0));
        a.computeHours();
        if (a.getHoursWorked() != null && a.getHoursWorked() < 4.0)
            a.setStatus(AttendanceStatus.HALF_DAY);
        return AttendanceResponse.from(attendanceRepository.save(a));
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> history(Long employeeId, LocalDate from, LocalDate to) {
        return attendanceRepository.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(employeeId, from, to)
                .stream().map(AttendanceResponse::from).toList();
    }

    @Transactional
    public AttendanceResponse upsert(ManualAttendanceRequest r) {
        Employee e = employeeService.getEntity(r.employeeId());
        LocalDate date = r.workDate() != null ? r.workDate() : LocalDate.now();
        Attendance a = attendanceRepository.findByEmployeeIdAndWorkDate(e.getId(), date)
                .orElseGet(() -> Attendance.builder().employee(e).workDate(date).build());
        a.setCheckIn(r.checkIn()); a.setCheckOut(r.checkOut());
        a.setStatus(r.status() != null ? r.status() : AttendanceStatus.PRESENT);
        a.setRemarks(r.remarks());
        a.computeHours();
        return AttendanceResponse.from(attendanceRepository.save(a));
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> forDate(LocalDate date) {
        return attendanceRepository.findByWorkDate(date).stream().map(AttendanceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AttendanceSummary summary(Long employeeId, LocalDate from, LocalDate to) {
        employeeService.getEntity(employeeId);
        List<Attendance> recs = attendanceRepository
                .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(employeeId, from, to);
        long present = count(recs, AttendanceStatus.PRESENT);
        long late = count(recs, AttendanceStatus.LATE);
        long absent = count(recs, AttendanceStatus.ABSENT);
        long leave = count(recs, AttendanceStatus.LEAVE);
        double hours = recs.stream().filter(r -> r.getHoursWorked() != null)
                .mapToDouble(Attendance::getHoursWorked).sum();
        long workingDays = countWorkingDays(from, to);
        double rate = workingDays == 0 ? 0.0
                : Math.round(((present + late) * 10000.0 / workingDays)) / 100.0;
        return new AttendanceSummary(employeeId, from, to, workingDays,
                present, late, absent, leave, rate, Math.round(hours * 100.0) / 100.0);
    }

    @Transactional
    public void markLeaveRange(Employee employee, LocalDate from, LocalDate to) {
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (isWeekend(d)) continue;
            final LocalDate cur = d;
            Attendance a = attendanceRepository.findByEmployeeIdAndWorkDate(employee.getId(), cur)
                    .orElseGet(() -> Attendance.builder().employee(employee).workDate(cur).build());
            a.setStatus(AttendanceStatus.LEAVE);
            a.setRemarks("Approved leave");
            attendanceRepository.save(a);
        }
    }

    @Transactional
    public int markAbsenteesFor(LocalDate date) {
        if (isWeekend(date)) return 0;
        List<Employee> active = employeeService
                .list(null, org.springframework.data.domain.Pageable.unpaged())
                .stream().map(r -> employeeService.getEntity(r.id()))
                .filter(e -> e.getStatus() == EmployeeStatus.ACTIVE || e.getStatus() == EmployeeStatus.PROBATION)
                .toList();
        int marked = 0;
        for (Employee e : active) {
            if (attendanceRepository.findByEmployeeIdAndWorkDate(e.getId(), date).isPresent()) continue;
            attendanceRepository.save(Attendance.builder()
                    .employee(e).workDate(date)
                    .status(AttendanceStatus.ABSENT)
                    .remarks("Auto-marked absent (no check-in)").build());
            marked++;
        }
        return marked;
    }

    public static boolean isWeekend(LocalDate d) {
        DayOfWeek day = d.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    public static long countWorkingDays(LocalDate from, LocalDate to) {
        long c = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1))
            if (!isWeekend(d)) c++;
        return c;
    }

    private long count(List<Attendance> recs, AttendanceStatus s) {
        return recs.stream().filter(r -> r.getStatus() == s).count();
    }
}