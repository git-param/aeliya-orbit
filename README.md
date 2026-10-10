# AELIYA Orbit

**Aeliya’s internal organization portal.**

AELIYA Orbit brings employee information, attendance, leave requests,
payroll workflows, and workplace communication into one application.

Built for approximately 60 employees today, with planned growth to
200–500 employees.

> **Status:** Planning and initial development.
> The features below describe the intended scope, not completed functionality.

## Purpose

- Give employees a central place to access information and submit requests.
- Simplify daily workflows for HR, operations, and team managers.
- Reduce manual attendance processing and payroll calculations.
- Protect sensitive information through clearly defined permissions.
- Support company-server deployment and future cloud migration.

## Planned Features

### Employee and Team Management

- Employee profiles and employment details.
- Departments, teams, and reporting relationships.
- Joining dates, employment status, and work anniversaries.
- Employee onboarding and offboarding.
- Access based on assigned roles and responsibilities.

### Leave Management

- Leave applications, approvals, and rejections.
- Configurable leave types and policies.
- Leave balances and request history.
- Approval routing based on organizational structure.
- Email notifications for request updates.
- Protection against duplicate approvals and balance deductions.

### Attendance Management

- Daily punch-in and punch-out Excel uploads by HR.
- Validation of employee identifiers, dates, and punch records.
- Duplicate file and record detection.
- Missing-punch and attendance exception tracking.
- Support for shift rules and overnight attendance.
- Attendance corrections with an audit history.
- Daily and monthly attendance summaries.

### Payroll Workflows

- Salary structures and effective-dated salary changes.
- Calculation previews using approved company policies.
- Attendance and unpaid-leave adjustments.
- Explicit decimal precision and rounding rules.
- Payroll review, approval, and period locking.
- Payslip generation and private employee access.
- Historical calculation inputs and results.

Payroll calculations must be verified against approved HR and accounts
examples before production use.

### Complaints and Requests

- Complaint submission and status tracking.
- Restricted access to confidential complaints.
- Assignment and routing to authorized personnel.
- Resolution history and relevant notifications.

### Organization Dashboard

- Role-specific employee and management views.
- Leave balances and pending approvals.
- Attendance summaries and exceptions.
- Holiday calendar.
- Birthdays and work anniversaries, subject to company policy.
- Organization announcements.

### Notifications

- Email delivery through the organization’s SMTP service.
- Leave, attendance, and workflow notifications.
- Background processing with tracked delivery attempts and retries.

## Technology Stack

| Layer | Planned Technology |
|---|---|
| Frontend | React, TypeScript, Vite |
| UI components | Material UI with a custom theme |
| Routing | React Router |
| API data management | TanStack Query |
| Backend | Java, Spring Boot, Spring MVC |
| Authentication and authorization | Spring Security |
| Database | PostgreSQL |
| Database access | Spring Data JPA / Hibernate |
| Schema migrations | Flyway |
| Excel processing | Apache POI |
| Email | Spring Mail |
| Background processing | Database-backed jobs with bounded workers |
| Deployment | Docker Compose, Nginx, HTTPS |

Compatible runtime and dependency versions will be pinned during setup.

## Architecture

HR Orbit will use a modular monolith: one backend application with
separate modules for each business area.

Planned modules:

- Identity and access
- Employees and organization structure
- Leave
- Attendance
- Payroll
- Complaints
- Announcements and events
- Notifications
- Audit history

The React frontend will communicate with the backend through REST APIs.
PostgreSQL will store structured application data.

Attendance imports, payroll processing, and email delivery will run as
background jobs with visible status and controlled resource usage.

Uploaded spreadsheets, attachments, and payslips will remain private.
Access will require backend authorization.

## Access Control

Permissions will combine an action with an allowed scope, such as
viewing personal attendance or approving leave for an assigned team.

| Role | Intended Responsibilities |
|---|---|
| Employee | Access own information and submit permitted requests |
| Team manager | View authorized team information and handle assigned approvals |
| HR | Manage employee records and authorized HR workflows |
| Operations manager | Manage authorized operational workflows |
| Payroll approver | Review and approve assigned payroll periods |
| System administrator | Manage technical configuration and accounts |

A person may hold multiple roles.

Salary access, confidential complaint access, payroll approval, and
technical administration will be separate permissions.

All protected operations must enforce permissions in the backend.
The final permission matrix will be defined before implementation.

## Security Requirements

- Session-based login using Secure, HttpOnly cookies.
- CSRF protection for authenticated state-changing requests.
- HTTPS in production.
- Secure password hashing when local accounts are used.
- Privileged-account MFA or suitable company SSO integration.
- Backend checks for both operation-level and record-level access.
- File-type validation and upload size limits.
- Private storage for sensitive files.
- Audit history for sensitive changes and approvals.
- Credentials supplied through deployment configuration.
- Restricted database access.
- Backups stored separately, with tested restoration.
- Sensitive information excluded from application logs.

These requirements must be implemented and verified before production use.

## Business Rules and Data Integrity

- Critical workflows must use transactions and database constraints.
- Concurrent approvals must not apply the same action twice.
- Repeated imports must not duplicate attendance records.
- Money calculations must use decimal arithmetic.
- Salary and policy changes must have effective dates.
- Approved payroll must retain its calculation inputs and results.
- Later policy changes must not silently alter approved payroll.
- Schema changes must be delivered through versioned migrations.

Leave rules, shift rules, approval routing, and payroll policies will be
documented with examples before their implementation.

## Data Retention

Retention rules will be defined separately for:

- Attendance records and original spreadsheets.
- Leave history.
- Payroll records and payslips.
- Complaints and attachments.
- Audit records.
- Application logs and temporary files.

Cleanup operations must preview affected records, check dependencies,
and require appropriate authorization.

Historical business records will not be deleted through an unrestricted
“delete everything before this date” operation.

Archiving records does not automatically reduce physical storage usage.

## Interface Direction

The interface will be professional, readable, and consistent.

Planned design principles:

- Warm neutral surfaces, charcoal navigation, and a restrained accent color.
- Clear typography and consistent spacing.
- Responsive forms and navigation.
- Compact tables for administrative workflows.
- Task-focused employee dashboards.
- Clear status labels and useful error messages.
- Accessible contrast, keyboard navigation, and form labels.

## Development Workflow

The product owner defines application behavior and acceptance criteria.
Codex assists with implementation, documentation, refactoring, and tests.

Development will proceed through small, complete workflows.

Each feature should include:

1. Documented behavior and acceptance criteria.
2. Backend business rules and permissions.
3. Frontend interactions, loading states, and error handling.
4. Database migrations where needed.
5. Appropriate automated verification.
6. Product-owner review.

AI-generated code must meet the same verification standards as any
other code in the project.

## Verification Priorities

- Unauthorized access to employee information.
- Leave balance calculations and concurrent approvals.
- Duplicate imports and invalid spreadsheet data.
- Missing punches and overnight shifts.
- Payroll deductions, salary changes, and rounding.
- Payroll approval and locking.
- Confidential complaint access.
- Background-job recovery and retry behavior.
- Backup restoration.

Demo environments will use synthetic or appropriately anonymized data
and a test email destination.

## Initial Roadmap

### Month 1 — Foundation

- Project setup.
- Login and access control.
- Employee records.
- Departments, teams, and reporting relationships.
- Shared interface and navigation.

### Month 2 — Core Workflows

- Leave submission and approvals.
- Attendance upload and validation.
- Holiday calendar.
- Email notifications.

### Month 3 — Demo Completion

- Basic complaint workflows.
- Birthdays and work anniversaries.
- Payroll calculation preview.
- Testing and interface refinement.
- Company-server demo deployment.

The three-month target applies to a focused demo.
Production readiness will be evaluated separately.

## Deployment

Initial deployment will run on a company server using Docker Compose.

The deployment will require:

- Persistent database and file storage.
- Environment-specific configuration.
- HTTPS and restricted network access.
- Health monitoring and log rotation.
- Backups stored outside the application server.
- Documented upgrade, rollback, and restore procedures.

Future cloud deployment will include database and file migration,
configuration updates, and verification.

## Local Development

Setup instructions will be added after the application scaffold,
runtime versions, and environment variables are finalized.

Do not commit:

- Credentials or SMTP passwords.
- Real employee attendance files.
- Salary records or payslips.
- Confidential complaints or attachments.
- Database exports containing personal information.

## Ownership and License

Developed for internal use at Aeliya.

Ownership and licensing terms must be confirmed with the organization.
This README does not grant an open-source license.