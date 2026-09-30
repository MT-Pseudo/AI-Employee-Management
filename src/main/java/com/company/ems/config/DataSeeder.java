package com.company.ems.config;

import com.company.ems.entity.*;
import com.company.ems.repository.*;
import com.company.ems.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final PerformanceReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random(42);

    @Override
    public void run(String... args) {
        if (employeeRepository.count() > 0) {
            log.info("Database already seeded - skipping.");
            return;
        }
        log.info("Seeding demo data...");

        Employee alice = employeeRepository.save(Employee.builder()
                .firstName("Alice").lastName("Sharma")
                .email("alice.sharma@company.com").phone("+91-9000000001")
                .department("Engineering").designation("Senior Engineer")
                .salary(new BigDecimal("1450000.00"))
                .joiningDate(LocalDate.now().minusMonths(30))
                .status(EmployeeStatus.ACTIVE).build());

        Employee bob = employeeRepository.save(Employee.builder()
                .firstName("Bob").lastName("Mehta")
                .email("bob.mehta@company.com").phone("+91-9000000002")
                .department("Engineering").designation("Software Engineer")
                .salary(new BigDecimal("950000.00"))
                .joiningDate(LocalDate.now().minusMonths(5))
                .status(EmployeeStatus.ACTIVE)
                .managerId(alice.getId()).build());

        Employee carol = employeeRepository.save(Employee.builder()
                .firstName("Carol").lastName("Nair")
                .email("carol.nair@company.com").phone("+91-9000000003")
                .department("Human Resources").designation("HR Business Partner")
                .salary(new BigDecimal("1100000.00"))
                .joiningDate(LocalDate.now().minusMonths(48))
                .status(EmployeeStatus.ACTIVE).build());

        Employee dave = employeeRepository.save(Employee.builder()
                .firstName("Dave").lastName("Iyer")
                .email("dave.iyer@company.com").phone("+91-9000000004")
                .department("Sales").designation("Account Executive")
                .salary(new BigDecimal("800000.00"))
                .joiningDate(LocalDate.now().minusMonths(18))
                .status(EmployeeStatus.PROBATION)
                .managerId(alice.getId()).build());

        Employee erin = employeeRepository.save(Employee.builder()
                .firstName("Erin").lastName("Khan")
                .email("erin.khan@company.com").phone("+91-9000000005")
                .department("Sales").designation("Sales Lead")
                .salary(new BigDecimal("1300000.00"))
                .joiningDate(LocalDate.now().minusMonths(60))
                .status(EmployeeStatus.ACTIVE).build());

        List<Employee> employees = List.of(alice, bob, carol, dave, erin);

        createUser("admin",   "admin123",   Role.ADMIN,    null);
        createUser("hr",      "hr123",      Role.HR,       carol.getId());
        createUser("manager", "manager123", Role.MANAGER,  alice.getId());
        createUser("bob",     "bob123",     Role.EMPLOYEE, bob.getId());
        createUser("dave",    "dave123",    Role.EMPLOYEE, dave.getId());

        LocalDate today = LocalDate.now();
        for (Employee e : employees) {
            for (int i = 44; i >= 1; i--) {
                LocalDate date = today.minusDays(i);
                if (AttendanceService.isWeekend(date)) continue;
                boolean isDave = e.getId().equals(dave.getId());
                int roll = random.nextInt(100);
                AttendanceStatus status;
                LocalTime in = null, out = null;
                if (isDave && roll < 20) {
                    status = AttendanceStatus.ABSENT;
                } else {
                    boolean late = isDave ? roll < 45 : roll < 12;
                    in = late ? LocalTime.of(9, 45 + random.nextInt(15))
                              : LocalTime.of(9, random.nextInt(25));
                    out = LocalTime.of(18, 15 + random.nextInt(45));
                    status = late ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;
                }
                Attendance a = Attendance.builder()
                        .employee(e).workDate(date).checkIn(in).checkOut(out)
                        .status(status)
                        .remarks(status == AttendanceStatus.ABSENT ? "Auto-marked absent" : null)
                        .build();
                a.computeHours();
                attendanceRepository.save(a);
            }
        }

        seedReviews(alice, List.of(5, 4, 5), LocalDate.now().minusMonths(20));
        seedReviews(bob,   List.of(4, 3, 4), LocalDate.now().minusMonths(18));
        seedReviews(carol, List.of(4, 4, 5), LocalDate.now().minusMonths(20));
        seedReviews(dave,  List.of(2, 3, 3), LocalDate.now().minusMonths(16));
        seedReviews(erin,  List.of(5, 5, 4), LocalDate.now().minusMonths(20));

        log.info("Seeding complete. {} employees created.", employees.size());
        log.info("Logins -> admin/admin123 | hr/hr123 | manager/manager123 | bob/bob123 | dave/dave123");
    }

    private void createUser(String username, String rawPassword, Role role, Long employeeId) {
        userRepository.save(User.builder()
                .username(username).password(passwordEncoder.encode(rawPassword))
                .role(role).employeeId(employeeId).enabled(true).build());
    }

    private void seedReviews(Employee e, List<Integer> ratings, LocalDate start) {
        for (int i = 0; i < ratings.size(); i++) {
            reviewRepository.save(PerformanceReview.builder()
                    .employee(e).reviewDate(start.plusMonths(6L * i))
                    .rating(ratings.get(i)).goalsMetPercent(60 + ratings.get(i) * 8)
                    .strengths("Consistent delivery on assigned goals.")
                    .improvements("Increase cross-team collaboration.")
                    .reviewerName("HR Review Board").build());
        }
    }
}