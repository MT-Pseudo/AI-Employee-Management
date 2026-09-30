package com.company.ems.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceScheduler {

    private final AttendanceService attendanceService;

    @Scheduled(cron = "${app.attendance.absent-sweep-cron:0 0 21 * * MON-FRI}")
    public void sweepAbsentees() {
        LocalDate today = LocalDate.now();
        int marked = attendanceService.markAbsenteesFor(today);
        log.info("Attendance sweep for {} completed. Marked {} absent.", today, marked);
    }
}