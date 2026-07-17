# GEMINI_V3.md

# WFO-Tracker Final Architecture Specification (Version 3.0)

> NOTE: This is the finalized, production-grade architecture specification reflecting the complete Phase 5 implementation.

## Core Stack
- **Backend:** Java 21, Spring Boot 4.x
- **Security:** Spring Security (BCryptPasswordEncoder, multi-role authority support)
- **MVC & Data:** Spring MVC, Spring Data JPA, Hibernate (validation mode only)
- **Database:** MySQL 8 (schema: `wfotracker`), H2 (for in-memory test profiles)
- **Migrations:** Flyway Schema Migrations
- **Frontend UI:** Thymeleaf, Bootstrap 5 (fully responsive CSS)
- **Exports:** Apache POI (XLSX, CSV streams with standard UTF-8 BOM encoding)
- **DevOps:** Docker, Docker Compose (configured for standard HTTP Port 80 deployment)

---

## Core Engineering Principles
- **SOLID Principles:** Strict enforcement of Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, and Dependency Inversion.
- **Clean Architecture & Separation of Concerns:** Core Business Logic is strictly isolated within Service layers. Presentation shifts are handled at the controller/model boundary.
- **Domain-Driven Directory Structure:** Clean package separation by features (`admin`, `auth`, `employee`, `manager`, `compliance`, `common`).
- **Defensive Programming:** Fail-fast parameter validations, constructor-only dependency injection, explicit database transactions, and custom global exception mapping.
- **Immutable DTOs:** Record structures for request parameters (`AddEmployeeRequest`, `EditEmployeeRequest`, `MonthlyConfigRequest`).
- **Dry & Kiss:** No duplicate logic, clear abstractions, zero field injections, and no circular dependencies.

---

## Identity & Role Model
- **One Person = One Corporate Account:** Represented by a single row in the `users` table.
- **DAS ID (Username):** Globally unique, alphanumeric, length <= 10. Acts as the primary credential.
- **Multi-Role Support:** A single user can possess multiple roles (`ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_EMPLOYEE`).
- **Authentication:** Spring Security Form Login with a mandatory **Role Selection Dropdown** supporting context-specific redirects.
- **Default Passwords:**
- Admin: `admin@123` (forced to change on first login).
- Managers/Employees: `firstname + surnameInitial + @123` (forced to change on first login, state tracked via `password_changed = false`).

---

## Admin Module
The system administrator manages teams and manager accounts.
- **Dashboard:** Interactive accordion view of all corporate teams. Clicking a team expands a nested table of managers and active employee rosters.
- **Create Team:** Registers team names and assigns managers. Creates manager login profiles automatically.
- **Deactivate Team:** Marks `team.active = false`. Strikes out the team in the UI, and immediately blocks login for the associated manager and all active team members (no data deletion).
- **Permanent Team Deletion:** Admin can permanently delete a deactivated team. Leverages database-level `ON DELETE CASCADE` constraints to completely purge team associations (`team_manager`, `employee_membership`, `groups`) while **preserving** the underlying manager and employee user rows in the `users` table.
- **Reset Manager Password:** Restores a manager's credentials to the default format and sets `password_changed = false`.
- **Export Data (XLSX/CSV):** Exports compliance statistics across all teams.

---

## Manager Module
Managers manage employees and configurations under their designated team.
- ** Roster Management:**
- **Add Employee:** Adds a member to their team. If the employee user account already exists in the `users` table (corporate user), it creates a new active membership, reactivates the account, and restores login access.
- **Edit Employee:** Modifies name, username, or group association.
- **Deactivate Employee:** Marks membership as `active = false` (strike-through style). Deactivated users are immediately blocked from logging in.
- **Delete Membership:** Permanently deletes a deactivated employee membership join-row, leaving the base user account and their historical attendance records completely intact.
- **Reset Employee Password:** Resets password back to default format and sets `password_changed = false`.
- **Group Management:** Organizes team members into custom groups. Includes an automatic "DEFAULT" group fallback.
- **Monthly Configuration:** Managers configure exceptions per employee, per month:
- Leaves
- Public Holidays
- Exception Days
- Manual Check-ins (Bulk override count)
- **Exports:**
- **Team Dashboard Export (XLSX/CSV):** Downloads the currently filtered team compliance list (including group/month/year selections).
- **Employee List Export (XLSX/CSV):** Downloads the complete employee list showing names, DAS IDs, groups, and status.

---

## Employee Module & Attendance Flow
Employees track their attendance and view compliance statistics.
- **Dashboard:** Displays current month statistics cards, progress bars, and an **embedded current month detailed attendance history table** at the bottom of the page.
- **Check-In/Out:**
- Restricts check-ins to once per day based on the calendar date.
- **Subsequent Day Check-In (Forgot Checkout Rule):** If an employee forgets to check out on Day 1, they are **never blocked** from checking in on Day 2.
- **History Page:** Detailed calendar lookup of office visits, check-in/out times, and calculated hours.
- **Exports (XLSX/CSV):** Downloads the employee's filtered monthly history records.

---

## Attendance & Timezone Refinement

### 1. Dynamic, View-Only "Forgot Checkout" Handling
- To prevent database write-locks and avoid complex background schedulers, stale check-outs are resolved dynamically at the presentation layer.
- If an attendance record has `check_out = null` and the `office_date` is strictly before the current day, the UI (both Manager History and Employee History) dynamically renders **"Forgot"** for the checkout time and **"-"** for hours spent.
- This view-only resolution keeps database transactions lightweight, quick, and clean.

### 2. Physical `MANUAL_ENTRY` Attendance Counting
- When a manager manually logs an office visit for an employee who missed both check-in and check-out, the record is stored in the `attendance` table with `attendance_type = 'MANUAL_ENTRY'`.
- The Compliance Engine retrieves visited days using a query that correctly captures both standard check-ins and specific manual override rows:
```sql
SELECT COUNT(a) FROM Attendance a
WHERE a.employee.id = :employeeId
	AND YEAR(a.officeDate) = :year
	AND MONTH(a.officeDate) = :month
	AND ((a.attendanceType = 'NORMAL' AND a.checkIn IS NOT NULL) OR a.attendanceType = 'MANUAL_ENTRY')
```

### 3. Server-Side India Standard Time (IST) Localization
- Core timestamps are saved strictly in **UTC** in the database to maintain audit integrity.
- Localization to **IST (`Asia/Kolkata`, UTC+5:30)** is handled cleanly at the entity/presentation layer right before rendering:
```java
public LocalDateTime getCheckInLocal() {
	if (this.checkIn == null) return null;
	return this.checkIn.atZone(ZoneId.of("UTC"))
			.withZoneSameInstant(ZoneId.of("Asia/Kolkata"))
			.toLocalDateTime();
}
```
- This ensures that browsers, Excel spreadsheets, and CSV exports display aligned local time values across all user sessions.

---

## Core Compliance Logic

$$Required\ Office\ Days = \lceil (Total\ Working\ Days - Leaves - Public\ Holidays - Exception\ Days) \times 0.50 \rceil$$

$$Visited\ Days = Normal\ Attendance\ (check-in\ \neq\ null) + Manual\ Attendance\ (MANUAL\_ENTRY\ records)$$

$$Compliance\ \% = \left(\frac{Visited\ Days}{Required\ Days}\right) \times 100$$

*The calculated Compliance % is displayed as a colored Bootstrap progress bar (Green for >= 100%, Yellow for >= 50%, Red for < 50%).*

---

## Database Schema (Core Tables)
1.  **`teams`:** Registers corporate teams (`id`, `team_name`, `active`).
2.  **`users`:** Central corporate account storage (`id`, `full_name`, `username`, `password`, `password_changed`, `active`).
3.  **`roles` / `user_roles`:** Standard join tables for role management.
4.  **`team_manager`:** Maps one manager to a single team.
5.  **`groups`:** Team subdivisions created by managers.
6.  **`employee_membership`:** Maps employees to teams, managers, and groups (`id`, `employee_id`, `team_id`, `manager_id`, `group_id`, `active`).
7.  **`monthly_configuration`:** Stores manager adjustments (`working_days`, `leaves`, `public_holidays`, `exception_days`, `manual_checkins`).
8.  **`attendance`:** Tracks check-ins, check-outs, and manual entries (`id`, `employee_id`, `team_id`, `office_date`, `check_in`, `check_out`, `hours_spent`, `attendance_type`).

---

## Quality & Test Requirements
- **Strict Verification:** All features must pass automated unit and integration tests.
- **MockMvc Integrations:** Direct validation of web requests, validation error paths, flash messaging, and Excel/CSV download content-types.
- **Mockito Strictness:** Complete elimination of duplicate, redundant, or unnecessary stubbings.
- **Quality Metrics:** Zero Sonar violations (e.g., duplicated string literals) and an overall test code coverage **greater than 85%**.
