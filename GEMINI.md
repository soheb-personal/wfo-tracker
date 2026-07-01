# GEMINI.md

# Project Name

**WFO-Tracker**

A web-based internal **Work From Office Compliance Tracker** application for organizations/teams to track employee office attendance compliance based on company policy (50% mandatory office attendance).

---

# 1. Objective

Build an internal web application where:

* Admin manages teams and managers
* Managers manage employees under their teams
* Employees track office attendance by check-in/check-out
* Compliance percentage is calculated monthly based on attendance policy

The application will be deployed as a **Dockerized Spring Boot application** with **MySQL database**.

---

# 2. Technology Stack

Backend + UI Monolithic Architecture

## Backend

* Java 21
* Spring Boot 4.x (latest stable)
* Spring MVC
* Spring Data JPA
* Spring Security
* Flyway Migration
* Maven

## Frontend

* Thymeleaf
* Bootstrap 5
* HTML/CSS/JavaScript

## Database

* MySQL 8+

Schema name:

```text
wfotracker
```

## DevOps

* Docker
* Docker Compose
* Git

---

# 3. User Roles

System has 3 roles.

```text
ADMIN
MANAGER
EMPLOYEE
```

---

# 4. Authentication Rules

Use Spring Security Form Login.

Password stored in DB **as plain text for now** (No encryption currently).

## Admin User

Create default user on startup.

Username

```text
admin
```

Password

Generate random complex 10-character password.

Example:

```text
A9#Lm2@Qx7
```

Store in DB.

Role

```text
ADMIN
```

Admin password does NOT require change on first login.

---

## Manager Login

Username format

```text
firstname
```

Default password format

```text
firstname + surnameInitial + @123
```

Example

```text
Name = Rahul Sharma

Username = rahul

Password = rahuls@123
```

Manager MUST change password at first login.

---

## Employee Login

Username format

```text
firstname
```

Password format

```text
firstname + surnameInitial + @123
```

Example

```text
Amit Khan

Username = amit

Password = amitk@123
```

Employee MUST change password at first login.

---

# 5. Global Rules

Never hard delete records.

Use soft delete only.

Use field

```text
active = true/false
```

Deactivated users cannot login.

---

# 6. Admin Features

Admin manages Teams and Managers.

---

## Create Team

Admin creates:

* Team Name
* Manager Name

When manager created

Generate:

```text
username = firstname
password = firstname + surnameInitial + @123
```

Store manager.

---

## Admin Can View

Admin can ONLY see:

* Team Name
* Manager Name
* Employees under manager

Example

```text
Payments Team

Manager:
Rahul Sharma

Employees:
Amit
Karan
Soheb
```

---

## Admin Can Edit

Allowed fields

* Team Name
* Manager Name
* Manager Username

Admin CANNOT edit employee.

---

## Reset Manager Password

Admin sees button

```text
Reset Password
```

Resets manager password back to default rule

```text
firstname + surnameInitial + @123
```

Sets

```text
password_changed = false
```

---

## Deactivate Team

Admin can deactivate team.

NEVER DELETE.

When team deactivated:

```text
team.active = false
manager.active = false
employees.active = false
```

All become unable to login.

---

# 7. Manager Features

Manager manages only employees assigned to his team.

---

## Add Employee

Fields

* Employee Name

Generate automatically

```text
username = firstname

password = firstname + surnameInitial + @123
```

---

## Manager Can Edit Employee

Can edit:

* Name
* Username

---

## Manager Can Deactivate Employee

No hard delete.

Employee status

```text
active = false
```

List should show

```text
Deactivated
```

---

## Reset Employee Password

Manager can click

```text
Reset Password
```

Password reset to default format.

Set

```text
password_changed = false
```

---

# 8. Attendance Compliance Logic

Policy

```text
50% Work From Office Mandatory
```

Monthly calculation.

Formula

```text
Required Office Days =

(Total Working Days
 - Leaves
 - Public Holidays
 - Exception Days)

* 50%
```

Always use ceiling value.

Example

```text
Working days = 23

Leaves = 1

Public Holiday = 1

Effective = 21

Required = ceil(21/2)

Required = 11
```

---

# 9. Manager Attendance Controls

Manager can specify for each employee.

For every month.

Fields

* Leaves
* Public Holidays
* Exception Days
* Manual Checkin

Example

```text
Employee = Amit

Month = July

Leaves = 2

Public Holiday = 1

Exception = 2
```

System recalculates.

---

# 10. Manager Dashboard

Manager can view current and previous month.

Dashboard should show.

For every employee.

```text
Employee Name

Required Office Days

Actual Office Days Visited

Remaining Office Days

Compliance %
```

Example

```text
Amit

Required = 11

Visited = 5

Compliance = 45%
```

---

## Progress Bar

Bootstrap progress bar.

Example

```text
45%
████████░░░░░░
```

---

## Manager Can View Employee Details

View

* Attendance dates
* Check-in time
* Check-out time
* Total hours spent in office

Current month and previous months.

---

# 11. Employee Features

Employee can login.

Must change password first login.

---

## Employee Dashboard

Current month.

Show

```text
Working Days

Leave Days

Public Holidays

Exception Days

Required Office Days

Days Visited

Remaining Days

Compliance %
```

---

## Check In

Button

```text
CHECK IN
```

Store current timestamp.

Only once per day.

---

## Check Out

Button

```text
CHECK OUT
```

Store timestamp.

Calculate

```text
hours_spent
```

Cannot checkout without check-in.

---

## Employee Can View History

Current month and previous month.

View

* Office visit dates
* Check-in time
* Check-out time
* Hours spent

---

# 12. UI Pages

---

## Authentication

```text
/login
/change-password
```

Pages

```text
login.html
change-password.html
```

---

## Admin Pages

```text
/admin/dashboard
/admin/team/create
/admin/team/list
/admin/team/edit/{id}
/admin/team/deactivate/{id}
/admin/manager/reset-password/{id}
```

Templates

```text
admin-dashboard.html
team-form.html
team-list.html
```

---

## Manager Pages

```text
/manager/dashboard
/manager/employee/add
/manager/employee/list
/manager/employee/edit/{id}
/manager/employee/reset-password/{id}
/manager/employee/configure/{id}
```

Templates

```text
manager-dashboard.html
employee-form.html
employee-list.html
employee-config.html
```

---

## Employee Pages

```text
/employee/dashboard
/employee/checkin
/employee/checkout
/employee/history
```

Templates

```text
employee-dashboard.html
attendance-history.html
```

---

# 13. Database Design

Use Flyway.

All tables created automatically during startup.

Location

```text
src/main/resources/db/migration
```

---

## team

```sql
CREATE TABLE team (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  team_name VARCHAR(100) NOT NULL,
  active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## users

Single table for all users.

```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,

  full_name VARCHAR(100) NOT NULL,

  username VARCHAR(50) UNIQUE NOT NULL,

  password VARCHAR(100) NOT NULL,

  role VARCHAR(20) NOT NULL,

  password_changed BOOLEAN DEFAULT FALSE,

  active BOOLEAN DEFAULT TRUE,

  team_id BIGINT,

  manager_id BIGINT,

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  FOREIGN KEY(team_id) REFERENCES team(id)
);
```

Rules

```text
Admin → no team

Manager → team assigned

Employee → team assigned + manager assigned
```

---

## monthly_configuration

Stores manager-defined exceptions.

```sql
CREATE TABLE monthly_configuration (

  id BIGINT PRIMARY KEY AUTO_INCREMENT,

  employee_id BIGINT NOT NULL,

  month INT NOT NULL,

  year INT NOT NULL,

  working_days INT NOT NULL,

  leaves INT DEFAULT 0,

  public_holidays INT DEFAULT 0,

  exception_days INT DEFAULT 0,

  required_office_days INT DEFAULT 0,

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  FOREIGN KEY(employee_id) REFERENCES users(id)
);
```

---

## attendance

```sql
CREATE TABLE attendance (

  id BIGINT PRIMARY KEY AUTO_INCREMENT,

  employee_id BIGINT NOT NULL,

  office_date DATE NOT NULL,

  check_in TIMESTAMP,

  check_out TIMESTAMP,

  hours_spent DECIMAL(5,2),

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  FOREIGN KEY(employee_id) REFERENCES users(id)
);
```

---

# 14. Flyway Migration Files

Folder

```text
src/main/resources/db/migration
```

Files

```text
V1__create_team_table.sql

V2__create_users_table.sql

V3__create_monthly_configuration.sql

V4__create_attendance_table.sql

V5__insert_admin_user.sql
```

---

# 15. Project Structure

```text
wfotracker

src/main/java/com/wfotracker

controller
service
repository
entity
dto
mapper
security
config
exception

src/main/resources

templates
static/css
static/js
db/migration
application.yml
```

---

# 16. Spring Security Rules

```text
ADMIN → /admin/**

MANAGER → /manager/**

EMPLOYEE → /employee/**
```

Unauthorized redirect to login.

If

```text
password_changed = false
```

Redirect

```text
/change-password
```

Mandatory change before using application.

---

# 17. Services Required

```text
AuthenticationService

TeamService

ManagerService

EmployeeService

AttendanceService

ComplianceService

MonthlyConfigurationService
```

---

# 18. Compliance Calculation Logic

```java
public int calculateRequiredDays(
        int workingDays,
        int leaves,
        int publicHoliday,
        int exceptionDays){

    int effectiveDays =
            workingDays
            - leaves
            - publicHoliday
            - exceptionDays;

    return (int) Math.ceil(
            effectiveDays * 0.50
    );
}
```

---

# 19. Dockerfile

```dockerfile
FROM eclipse-temurin:23

COPY target/wfotracker.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]
```

---

# 20. Docker Compose

```yaml
version: '3.8'

services:

  mysql:

    image: mysql:8

    container_name: wfotracker-db

    environment:

      MYSQL_ROOT_PASSWORD: root

      MYSQL_DATABASE: wfotracker

    ports:

      - "3306:3306"

    volumes:

      - mysql-data:/var/lib/mysql

  app:

    build: .

    container_name: wfotracker-app

    depends_on:

      - mysql

    ports:

      - "8080:8080"

    environment:

      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/wfotracker

      SPRING_DATASOURCE_USERNAME: root

      SPRING_DATASOURCE_PASSWORD: root

volumes:

  mysql-data:
```

---

# 21. application.yml

```yaml
spring:

  datasource:
    url: jdbc:mysql://localhost:3306/wfotracker
    username: root
    password: root

  jpa:
    hibernate:
      ddl-auto: validate

  flyway:
    enabled: true
    baseline-on-migrate: true
```

---

# 22. Development Order

Phase 1

```text
Project setup
Flyway
Security
Login
Password change
```

Phase 2

```text
Admin module
Team creation
Manager creation
Deactivate team
```

Phase 3

```text
Employee management
Reset password
Deactivate employee
```

Phase 4

```text
Attendance checkin checkout
History
```

Phase 5

```text
Compliance calculation
Dashboard
Progress bar
```

Phase 6

```text
Docker deployment
Testing
```

---

# Final Architecture

```text
Browser

↓

Thymeleaf UI

↓

Spring MVC Controllers

↓

Service Layer

↓

JPA Repository Layer

↓

MySQL Database

↓

Flyway Migration

↓

Docker Deployment
```
