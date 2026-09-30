cd C:\your_file_location

$readme = @'

# AI Employee Management System

A full-stack employee management system built with Spring Boot, MySQL, and vanilla JavaScript. Features role-based access control, automated attendance tracking, leave workflow, performance reviews, and an AI-assisted reporting module that generates attrition-risk assessments and natural-language HR briefings.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.x-blue)
![License](https://img.shields.io/badge/license-MIT-green)

---

## Table of Contents

1. [Features](#features)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Prerequisites](#prerequisites)
5. [Setup](#setup)
6. [Running the App](#running-the-app)
7. [How to Use the App](#how-to-use-the-app)
8. [Demo Accounts](#demo-accounts)
9. [API Reference](#api-reference)
10. [How the AI Module Works](#how-the-ai-module-works)
11. [Configuration](#configuration)
12. [Troubleshooting](#troubleshooting)
13. [License](#license)
14. [Author](#author)

---

## Features

### Employee Management

- Full CRUD on employee records (add, edit, terminate, delete)
- Search by name, email, or department
- Self-referencing manager hierarchy
- Employee status lifecycle: ACTIVE, PROBATION, ON_LEAVE, TERMINATED

### Attendance & Leave

- Employee self-service check-in / check-out
- Automatic late-arrival detection based on configurable work-start time
- Nightly scheduler auto-marks no-shows as absent
- Leave workflow: apply -> approve/reject -> auto-updates attendance for the approved range
- 30-day attendance history with per-employee summaries

### Performance

- Rating (1-5), goals-met %, strengths, improvements, reviewer
- Multiple reviews per employee with historical trend detection

### AI Reporting Module

- Per-employee attrition risk score (0-100) with LOW / MEDIUM / HIGH banding
- Natural-language HR briefing generated from real data
- Organisation-wide report: headcount, attendance by department, top performers, high-risk employees
- Two interchangeable narrative engines:
  - Rule-based (default, zero external dependencies, deterministic)
  - OpenAI (drop-in via config; falls back to rule-based on any API failure)

### Security

- JWT authentication (HS256, 24h expiry)
- Four roles: ADMIN, HR, MANAGER, EMPLOYEE
- Method-level authorization via @PreAuthorize with self-or-privileged checks
- BCrypt password hashing

### Web UI

- Single-page dashboard served as static files from Spring Boot
- Login screen with demo-account quick reference
- Role-aware navigation (tabs hide based on permissions)
- Modal-based forms for CRUD operations
- CSS animations, reduce-motion support

---

## Tech Stack

| Layer       | Technology                               |
| ----------- | ---------------------------------------- |
| Language    | Java 17                                  |
| Framework   | Spring Boot 3.2.5                        |
| Web         | Spring MVC, REST                         |
| Persistence | Spring Data JPA, Hibernate 6.4           |
| Database    | MySQL 8.x                                |
| Security    | Spring Security 6, JJWT 0.11.5           |
| Validation  | Jakarta Bean Validation                  |
| Frontend    | HTML5, CSS3, vanilla JavaScript (ES2020) |
| Build       | Maven                                    |
| Boilerplate | Lombok                                   |

---

## Project Structure

    src/main/java/com/company/ems/
    |-- EmsApplication.java              Application entry point
    |-- config/
    |   |-- SecurityConfig.java          Filter chain, CORS, password encoder
    |   |-- AiConfig.java                Selects narrative engine
    |   `-- DataSeeder.java              Demo data bootstrap
    |-- security/
    |   |-- JwtService.java              Token generation & validation
    |   |-- JwtAuthFilter.java           Bearer-token request filter
    |   `-- CustomUserDetailsService.java
    |-- entity/                          JPA entities
    |-- repository/                      Spring Data repositories
    |-- dto/                             Request/response records
    |-- service/
    |   |-- AuthService, EmployeeService, AttendanceService
    |   |-- LeaveService, PerformanceService
    |   |-- AttendanceScheduler           Nightly absent sweep
    |   `-- SecurityUtils                 Current user / role helpers
    |-- ai/
    |   |-- AiInsightService              Metric collection + risk scoring
    |   |-- EmployeeMetrics, OrgMetrics   Computed data carriers
    |   |-- NarrativeGenerator            Strategy interface
    |   |-- RuleBasedNarrativeGenerator   Deterministic engine
    |   `-- OpenAiNarrativeGenerator      LLM engine with fallback
    |-- controller/                       REST endpoints
    `-- exception/                        Global exception handler

    src/main/resources/
    |-- application.yml
    `-- static/
        |-- index.html                    Login page
        |-- dashboard.html                Main app
        |-- app.js                        Frontend logic
        `-- style.css

---

## Prerequisites

- Java 17 or later
- Maven 3.8+
- MySQL 8.x running locally

Verify your setup:

    java -version
    mvn -version
    mysql --version

---

## Setup

### 1. Clone the repository

    git clone https://github.com/<your-username>/ai-employee-management.git
    cd ai-employee-management

### 2. Create the database (optional)

The JDBC URL uses createDatabaseIfNotExist=true, so the schema is created automatically. To create it manually:

    CREATE DATABASE ems_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

### 3. Set environment variables

Windows (PowerShell):

    $env:DB_USERNAME = "root"
    $env:DB_PASSWORD = "your_mysql_password"

macOS / Linux:

    export DB_USERNAME=root
    export DB_PASSWORD=your_mysql_password

Optional:

    export JWT_SECRET="a-long-random-secret-at-least-32-bytes"
    export OPENAI_API_KEY="sk-..."   # only if switching to the OpenAI engine

### 4. Build

    mvn clean package -DskipTests

---

## Running the App

    java -jar target/ai-employee-management-1.0.0.jar

The app starts on http://localhost:8080.

On first run, five demo employees, users, attendance history, and performance reviews are seeded automatically. Subsequent runs skip seeding.

Open your browser:

    http://localhost:8080/

To stop the app press Ctrl+C in the terminal.

To run on a different port:

    java -jar target/ai-employee-management-1.0.0.jar --server.port=8081

For development with auto-compile on file save:

    mvn spring-boot:run

---

## How to Use the App

Open http://localhost:8080/ in a browser. You will see the login page. Use any of the demo accounts listed below.

### As an Administrator (admin / admin123)

You have every feature available:

1. Employees tab
   - Click "+ Add Employee" to create a new employee.
   - Fill in the form (name, email, department, designation, salary, joining date) and click Save.
   - Click "Edit" on any row to modify that employee.
   - Click "Terminate" to change status to TERMINATED (record kept for audit).
   - Click "Delete" to permanently remove the record.
   - Use the search box to filter by name, email, or department.

2. Attendance tab
   - Shows today status with Check in / Check out buttons.
   - Table below lists your last 30 days of attendance.

3. Leaves tab
   - Apply for leave by picking dates and entering a reason.
   - "My requests" shows your own history.
   - "Pending approvals" shows every employee request waiting for a decision.
   - Click Approve or Reject. If approved, attendance for those dates is automatically set to LEAVE.

4. Performance tab
   - Click "+ Add Review".
   - Pick an employee from the dropdown, set rating (1-5), goals-met %, and optional feedback.
   - Table lists all reviews with reviewer and date.

5. AI Reports tab
   - "My AI briefing" auto-loads your own report: risk score, band, narrative, insights, recommendations.
   - "Organisation briefing" generates a company-wide report: headcount, attendance rate, top performers, high-risk employees.

6. Users tab
   - Click "+ Create Login" to create a login for any employee.
   - Pick username, password, role, and link to an employee record.
   - The new user can immediately sign in.

### As an HR user (hr / hr123)

Same as Administrator minus Delete on employees and minus the Users tab.

### As a Manager (manager / manager123)

- Read-only employee list.
- Can approve or reject leave requests.
- Can see their own AI briefing.
- Cannot create, edit, or delete employees.

### As an Employee (bob / bob123)

- No Employees, Performance, or Users tabs.
- Can check in / check out.
- Can apply for leave and see own leave status.
- Can view own AI briefing.
- Can view own performance reviews (via API endpoint /api/performance/reviews/me).

### Walkthrough: end-to-end test in 5 minutes

1. Sign in as hr / hr123.
2. Employees tab -> "+ Add Employee" -> fill "Nina Verma", nina@company.com, Engineering, QA Engineer, 780000, today date -> Save.
3. Nina appears in the list. Click "Edit" on her row, change salary to 800000, Save.
4. Users tab -> "+ Create Login" -> username nina, password nina123, role EMPLOYEE, link to Nina's record -> Create.
5. Sign out. Sign in as nina / nina123.
6. Notice the reduced navigation (no Employees, Performance, or Users tabs).
7. Attendance tab -> Check in. Then Check out. Both succeed.
8. Leaves tab -> apply for leave for next week -> Submit.
9. Sign out. Sign in as hr / hr123.
10. Leaves tab -> find Nina request -> Approve.
11. AI Reports tab -> click "Generate" for the Organisation briefing.
12. The report includes Nina in the headcount and shows attendance by department.

That single flow exercises authentication, authorization, CRUD, the scheduler, leave workflow, and the AI module.

### Quick API test with curl

    # 1. Login and capture token
    TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
      -H "Content-Type: application/json" \
      -d '{"username":"hr","password":"hr123"}' | jq -r .token)

    # 2. List employees
    curl -s -H "Authorization: Bearer $TOKEN" \
      "http://localhost:8080/api/employees?page=0&size=5"

    # 3. Add an employee
    curl -s -X POST -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"firstName":"Nina","lastName":"Verma","email":"nina@company.com","department":"Engineering"}' \
      http://localhost:8080/api/employees

    # 4. AI report for employee 2
    curl -s -H "Authorization: Bearer $TOKEN" \
      http://localhost:8080/api/reports/ai/employee/2

On Windows PowerShell, use Invoke-RestMethod instead:

    $login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login `
        -ContentType "application/json" -Body '{"username":"hr","password":"hr123"}'
    $headers = @{ Authorization = "Bearer $($login.token)" }
    Invoke-RestMethod -Uri "http://localhost:8080/api/employees?page=0&size=5" -Headers $headers

---

## Demo Accounts

| Username | Password   | Role     | Access                                            |
| -------- | ---------- | -------- | ------------------------------------------------- |
| admin    | admin123   | ADMIN    | Everything, plus user management and delete       |
| hr       | hr123      | HR       | Employee CRUD, leave approvals, org AI report     |
| manager  | manager123 | MANAGER  | Read employees, approve leaves, own AI report     |
| bob      | bob123     | EMPLOYEE | Self-service only                                 |
| dave     | dave123    | EMPLOYEE | Self-service (low-attendance profile for AI demo) |

Note: Demo credentials exist for evaluation only. Replace or remove DataSeeder before any real deployment.

---

## API Reference

All /api/\*\* endpoints except /api/auth/login require:

    Authorization: Bearer <jwt>

### Authentication

| Method | Endpoint           | Role   | Description    |
| ------ | ------------------ | ------ | -------------- |
| POST   | /api/auth/login    | Public | Get JWT        |
| POST   | /api/auth/register | ADMIN  | Create a user  |
| GET    | /api/auth/users    | ADMIN  | List all users |

### Employees

| Method | Endpoint                      | Role               |
| ------ | ----------------------------- | ------------------ |
| POST   | /api/employees                | ADMIN, HR          |
| GET    | /api/employees?q=&page=&size= | ADMIN, HR, MANAGER |
| GET    | /api/employees/{id}           | Self or privileged |
| PUT    | /api/employees/{id}           | ADMIN, HR          |
| PATCH  | /api/employees/{id}/terminate | ADMIN, HR          |
| DELETE | /api/employees/{id}           | ADMIN              |

### Attendance

| Method | Endpoint                              | Role               |
| ------ | ------------------------------------- | ------------------ |
| POST   | /api/attendance/check-in              | Any authenticated  |
| POST   | /api/attendance/check-out             | Any authenticated  |
| GET    | /api/attendance/me?from=&to=          | Self               |
| GET    | /api/attendance/me/summary?from=&to=  | Self               |
| POST   | /api/attendance/manual                | ADMIN, HR          |
| GET    | /api/attendance/employee/{id}/summary | ADMIN, HR, MANAGER |
| POST   | /api/attendance/sweep/{date}          | ADMIN, HR          |

### Leaves

| Method | Endpoint                 | Role               |
| ------ | ------------------------ | ------------------ |
| POST   | /api/leaves/apply        | Self               |
| GET    | /api/leaves/me           | Self               |
| GET    | /api/leaves/pending      | ADMIN, HR, MANAGER |
| POST   | /api/leaves/{id}/approve | ADMIN, HR, MANAGER |
| POST   | /api/leaves/{id}/reject  | ADMIN, HR, MANAGER |
| POST   | /api/leaves/{id}/cancel  | Self or privileged |

### Performance

| Method | Endpoint                               | Role               |
| ------ | -------------------------------------- | ------------------ |
| POST   | /api/performance/reviews               | ADMIN, HR, MANAGER |
| GET    | /api/performance/reviews               | ADMIN, HR, MANAGER |
| GET    | /api/performance/reviews/me            | Self               |
| GET    | /api/performance/reviews/employee/{id} | ADMIN, HR, MANAGER |

### AI Reports

| Method | Endpoint                      | Role               |
| ------ | ----------------------------- | ------------------ |
| GET    | /api/reports/ai/me            | Self               |
| GET    | /api/reports/ai/employee/{id} | Self or privileged |
| GET    | /api/reports/ai/organization  | ADMIN, HR          |

---

## How the AI Module Works

### Attrition risk scoring

Each employee risk score is a weighted sum of observable signals:

| Signal                         | Points |
| ------------------------------ | ------ |
| Attendance rate < 85%          | +25    |
| Attendance rate 85-92%         | +12    |
| Late-arrival rate > 20%        | +18    |
| Late-arrival rate 10-20%       | +9     |
| Approved leave > 15 days (90d) | +18    |
| Approved leave 8-15 days (90d) | +9     |
| Average rating < 3.0           | +22    |
| Average rating 3.0-3.8         | +10    |
| Declining performance trend    | +15    |
| Improving performance trend    | -8     |
| Tenure < 6 months              | +10    |
| Tenure > 48 months             | +6     |
| Status = PROBATION             | +8     |

Final score is capped at 0-100. Bands: LOW (< 30), MEDIUM (30-59), HIGH (>= 60).

### Narrative generation

The NarrativeGenerator interface has two implementations:

- RuleBasedNarrativeGenerator (default) - deterministic, no network calls, produces a briefing from the computed metrics.
- OpenAiNarrativeGenerator - wraps the rule-based engine as a fallback, calls OpenAI chat completions API when app.ai.provider=openai and an API key is configured. If the call fails or times out, it silently falls back rather than failing the request.

### Enabling the LLM engine

In application.yml:

    app:
      ai:
        provider: openai
        openai:
          api-key: ${OPENAI_API_KEY}
          model: gpt-4o-mini

Set the OPENAI_API_KEY environment variable before starting the app.

---

## Configuration

Key settings in src/main/resources/application.yml:

    spring:
      datasource:
        url: jdbc:mysql://localhost:3306/ems_db?createDatabaseIfNotExist=true&...
        username: ${DB_USERNAME:root}
        password: ${DB_PASSWORD:root}
      jpa:
        hibernate:
          ddl-auto: update

    app:
      jwt:
        secret: ${JWT_SECRET:change-this-in-production}
        expiration-ms: 86400000        # 24 hours
      attendance:
        work-start: "09:30"
        absent-sweep-cron: "0 0 21 * * MON-FRI"

---

## Troubleshooting

| Symptom                      | Fix                                                                                                      |
| ---------------------------- | -------------------------------------------------------------------------------------------------------- |
| Access denied for user root  | Wrong DB_PASSWORD - verify MySQL root password                                                           |
| Port 8080 was already in use | Kill the other process, or run with --server.port=8081                                                   |
| Communications link failure  | MySQL service is stopped - start it via net start MySQL84 (Windows) or brew services start mysql (macOS) |
| Browser shows unstyled page  | Hard refresh: Ctrl+F5                                                                                    |
| BUILD FAILURE                | Check java -version reports 17 or higher                                                                 |
| Seeding skipped unexpectedly | Emptied data already exists. To reset: DROP DATABASE ems_db; then restart                                |

---

## Author

MT

- GitHub: https://github.com/MT

---

## Acknowledgements

- Spring Boot - https://spring.io/projects/spring-boot
- JJWT - https://github.com/jwtk/jjwt
- MySQL - https://www.mysql.com/
- OpenAI - https://openai.com/ (optional narrative engine)
  '@

$enc = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "README.md"), $readme, $enc)

Write-Host ""
Write-Host "README.md updated:" -ForegroundColor Green
Get-Item README.md | Select-Object Name, Length
