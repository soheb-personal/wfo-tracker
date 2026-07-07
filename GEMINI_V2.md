# GEMINI_V2.md

# WFO-Tracker Final Architecture Specification

> NOTE: This is the finalized architecture specification for production-grade implementation.

## Core Stack
- Java 21
- Spring Boot 4.x
- Spring Security (BCrypt)
- Spring MVC
- Spring Data JPA
- Flyway
- Thymeleaf
- Bootstrap 5
- MySQL (schema: wfotracker)
- Docker / Docker Compose
- Apache POI (CSV/XLSX export)

## Core Principles
- SOLID Principles
- Clean Architecture
- Feature-based package structure
- Constructor Injection only
- No duplicate logic
- Production-grade maintainable code

## Identity Model
- One person = one permanent user account
- DAS ID globally unique and acts as username
- One user may have multiple roles:
	- ADMIN
	- MANAGER
	- EMPLOYEE

## Authentication
Login fields:
- Role Dropdown (mandatory)
- Username (For admin, it is admin & for manager/employee role, it is DAS ID)
- Password

Role determines dashboard access.

## Password Security
Use BCryptPasswordEncoder.

Rules:
- Never store plaintext password
- Admin password: admin@123
- Manager/Employee default password:
firstname + surnameInitial + @123
- password_changed=false on first creation/reset
- Force password change on first login

## Admin Module
Admin can:
- Create Team
- Create/Edit Manager by providing DAS ID & Full name
- DAS ID: mandatory, alphanumeric only, length <= 10
- Reset Manager password
- Deactivate Team
- Export all teams data (CSV/XLSX)

Dashboard:
- Accordion view
- Click team -> expand employees list

Team Deactivation:
- team.active = false
- Team shown strike-through
- Manager + employees of team cannot login through that team
- No data deletion

Manager Rules:
- One manager can manage ONLY one team
- Existing manager cannot manage second team

## Manager Module
Manager can:
- Add/Edit Employees by providing DAS ID & Full name
- DAS ID: mandatory, alphanumeric only, length <= 10
- Create Groups
- Configure monthly employee config
- Manual Attendance Entry
- Reset employee password
- Deactivate employee
- Reactivate employee
- Delete deactivated employee membership
- Export visible team data

Employee rules:
- Employee can belong to ONLY one active team
- If employee already assigned elsewhere -> reject

Groups:
- Optional while adding employee
- DEFAULT group if none specified
- Hide DEFAULT if unused

## Employee Membership Lifecycle
Deactivate:
- employee_membership.active=false
- Show strike-through

Reactivate:
- employee_membership.active=true

Delete:
- Only allowed after deactivate
- Delete membership only
- Never delete user or attendance history

Reassignment:
- Reuse same DAS ID user
- Create new membership
- Enable login again

## Attendance
Employee actions:
- Check-in once per day
- Checkout after check-in only

Forgot Checkout:
- Day still counted visited
- check_out=NULL
- UI shows:
Checkout = Forgot
Hours = -

Manual Attendance:
Manager can manually mark office visit if employee forgot both check-in and checkout.

attendance_type:
- NORMAL
- MANUAL_ENTRY

## Monthly Configuration
Manager can configure per employee per month:
- Leaves
- Public Holidays
- Exception Days
- Manual Attendance Entry

## Compliance Logic

Required Office Days =
(Total Working Days - Leaves - Public Holiday - Exception Days) * 50%

Use Math.ceil()

Visited Days =
Normal Attendance + Manual Attendance

Compliance % =
Visited Days / Required Days * 100

## Dashboards

Admin:
- Teams accordion
- Expand to show employees

Manager:
- DAS ID
- Employee Name
- Required Days
- Visited Days
- Remaining Days
- Compliance %
- Group filter

Employee:
- Leaves
- Public Holidays
- Exception Days
- Required Days
- Visited Days
- Remaining Days
- Compliance %
- Attendance History

Attendance History table:
- Date
- Checkin
- Checkout
- Hours

If forgot checkout:
- Checkout = Forgot
- Hours = -

## Export Features (Apache POI)

Admin:
- Export all teams data

Manager:
- Export visible team dashboard data

Employee:
- Export visible monthly attendance history

Formats:
- CSV
- XLSX

## Database Core Tables
- users
- roles
- user_roles
- teams
- team_manager
- groups
- employee_membership
- monthly_configuration
- attendance

Attendance stores:
- employee_id
- team_id

Never delete attendance history.

## Database Management
- Flyway only
- No Hibernate ddl-auto create/update
- Schema = wfotracker

## DevOps
Generate:
- Dockerfile
- docker-compose.yml

Services:
- Spring Boot App
- MySQL

## Testing
Mandatory:
- JUnit 5
- MockMvc
- Service tests
- Security tests

## Final Coding Instructions For Gemini

Read this file as single source of truth.

Generate complete application.

Do NOT:
- Leave TODOs
- Generate placeholders
- Skip modules
- Use poor architecture

Must be:
- Production grade
- Enterprise maintainable
- Clean code
- Robust
