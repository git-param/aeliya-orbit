# Architecture Guidelines

- **Frontend:** React, Vite, MUI. Uses TanStack Query for state.
- **Backend:** Spring Boot (Modular Monolith).
- **Auth:** Spring Security with Server-Side Sessions (HttpOnly, Secure cookies).
- **Database:** PostgreSQL accessed via Spring Data JPA. Migrations via Flyway.
- **Background Jobs:** Handled via Spring scheduling for attendance parsing, payroll, and notifications.
- **File Storage:** Local initially (or cloud object storage), decoupled via interface.