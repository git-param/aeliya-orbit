# Authorization foundation: audit and proposal

## Current code audit (21 October 2026)

- Backend is a modular Spring Boot application with login, users, JWT security, and a health endpoint. Flyway V1 and V2 create only `system_status` and `users`. There is no employee, organization, attendance, leave, payroll, or audit module yet.
- The `users.role` string holds the account role. There is no designation, department, level, reporting relation, or permission model. No employee records can yet be scoped to an owner or manager.
- Previously every authenticated account could call every non-login endpoint. The frontend dashboard is mock data; its role-filtered navigation is not an authorization boundary.
- The application currently uses bearer JWTs stored in browser local storage. `docs/architecture.md` describes server-side sessions and secure cookies; this is an unresolved architecture mismatch. This phase preserves JWT login. Moving to secure cookies is a separate authentication migration.
- The source previously contained database, JWT, and bootstrap credentials. Rotate any credentials that were used outside a local development environment. The new configuration requires environment-supplied secrets.
- The source declares Spring Boot 4.1.1 / Java 21 and React 19, although the request says Java 17 / React 18. Version migration is out of scope for this authorization phase.

## Permission matrix for the foundation

`Self` means the account owner, and `Reports` means employees in the reporting tree. These scopes require employee records and are **not granted by this phase**. `Grant` means an explicit database grant to a role or user. Super Admin has all catalog permissions, independent of grants, and cannot be edited through grant APIs.

| Operation | Super Admin | Admin initial | USER initial | Future scoped rule |
| --- | --- | --- | --- | --- |
| Manage grants | Yes | No | No | Super Admin only initially |
| View/create/update/deactivate employees | Yes | Grant | No | HR/Operations Manager may receive grants; visibility and target checks still required |
| Manage departments/holidays/attendance | Yes | Grant | No | Explicit grants plus record scope |
| Approve leave | Yes | Grant | No | Assigned approver and reporting checks |
| View/manage payroll | Yes | No | No | Explicit grant; own salary needs a separate self endpoint |
| View own account and health | Yes | Yes | Yes | Authenticated account |

Initial Admin grants: `EMPLOYEE_VIEW`, `EMPLOYEE_CREATE`, `EMPLOYEE_UPDATE`, `EMPLOYEE_DEACTIVATE`, `DEPARTMENT_MANAGE`, `HOLIDAY_MANAGE`, `ATTENDANCE_MANAGE`, `LEAVE_APPROVE`. They authorize *capabilities* for future services, not unrestricted employee data. USER receives no role grants. HR/Operations Manager remain USER accounts and may later get explicit user grants. Existing accounts keep their roles.

## Data model proposal

- **This phase:** `users` keeps account identity and base role; `active` permits immediate login revocation. `permissions` is the action catalog. `role_permission_grants` and `user_permission_grants` support admin configuration. `permission_change_audit` records grant changes. No employee-related table is created yet.
- **Next phase:** `employees` links optionally and uniquely to `users`, has a unique manually assigned employee ID, private profile fields, active status, reporting manager FK to employees, and references to `departments`, `designations`, and `levels`. Administrative accounts need no employee row. Keep account role separate from these organization fields. Use dedicated public/private DTO projections.
- **Later phases:** salary revisions as immutable effective-dated records; attendance and corrections as auditable records; leave balances as transactions; breaks as start/end events; config rows with effective dates and archival. All rows link to employee IDs and actors as appropriate. Do not create these tables until policies and endpoint scopes are settled.

## Implementation sequence and invariants

1. Add the permission catalog and grant storage with Flyway. Resolve authorities from the database on every authenticated request, not JWT role claims. Super Admin is the only initial grant editor. Grant changes are audited.
2. Build employee and organization modules with query-level filtering. A permission alone never skips ownership, reporting, account-invisibility, or field-level checks. Admin and USER endpoints must exclude Super Admin accounts.
3. Add separate payroll and salary endpoints with explicit permissions and self-only views.
4. Add attendance, leave, break, and dashboard APIs when policy choices in `docs/open-questions.md` are answered. Replace mock UI data only when real, scoped endpoints exist.

New routes must be explicitly listed in security configuration and checked in services for target/resource scope. All other routes are denied. The permission management API is deliberately limited to grant configuration and does not create or alter account roles.

## Phase 1 delivery notes

Files changed: security configuration, JWT authentication, user principal loading, bootstrap seeder, account entity, login role selection, application properties; new permission service/controller/DTO, current-user controller, Flyway V3, and this audit.

Migration V3 adds `users.active`, checks account-role values, creates the permission catalog, role and user grants, and an append-only permission change audit table. Existing user rows and passwords are preserved. The initial Admin grants above are seeded by the migration; Super Admin's full catalog access is computed by the service.

Endpoints:

| Method and path | Access | Response / effect |
| --- | --- | --- |
| `POST /api/auth/login` | Public | Existing JWT login; disabled accounts rejected |
| `GET /api/me` | Authenticated | Own ID, username, base role, effective permissions only |
| `GET /api/health` | Authenticated | Existing backend status |
| `GET /api/admin/permissions` | Super Admin | Permission catalog |
| `GET/PUT /api/admin/permissions/roles/{ROLE_ADMIN\|ROLE_USER}` | Super Admin | Read/replace role grants |
| `GET/PUT /api/admin/permissions/users/{id}` | Super Admin | Read/replace direct user grants; Super Admin target hidden |

A PUT body is `{"permissions":["EMPLOYEE_VIEW"]}`. An empty list revokes all explicit grants on that target. Unknown codes are rejected. `PERMISSION_MANAGE` is reserved for Super Admin and cannot be assigned through this initial API. Each changed grant creates an audit row in the same transaction. No audit-reading endpoint exists yet.

All other backend paths are denied until they have an explicit security rule. Permissions are loaded from PostgreSQL for each authenticated request, so grant changes and account deactivation take effect without waiting for JWT expiry. Tokens contain only subject and timing claims; the browser's role value is for display only. Existing issued JWTs remain syntactically valid but now use current database authorities.

Deployment configuration: set `DB_USERNAME`, `DB_PASSWORD`, and a strong `JWT_SECRET`; optionally set `DB_URL` and `JWT_EXPIRATION_MS`. Set `BOOTSTRAP_SUPER_ADMIN_USERNAME` and `BOOTSTRAP_SUPER_ADMIN_PASSWORD` only when creating a new Super Admin account. The seeder never overwrites an existing account. Existing credentials should be rotated because they were previously present in source. There is no migration from browser local-storage tokens to cookies in this phase.

Verification: backend `./mvnw -o -q -Dmaven.repo.local=/private/tmp/aeliya-m2/repository -DskipTests compile` passed. Database migration and HTTP integration were not run because no authorized database credentials were supplied. Frontend was not changed in this phase; its navigation and dashboard remain role-display and mock-data code. Future UI must consume `/api/me` permissions and handle expired sessions.

## Security review (21 October 2026)

### Verified code paths and fixes

- `SecurityConfig` now permits only `POST /api/auth/login` publicly. `GET /api/me` and `GET /api/health` require authentication. Only a current `ROLE_SUPER_ADMIN` principal can use the listed `GET`/`PUT` grant routes. Every other request reaches `denyAll`. The review found the earlier path-only login matcher and broad grant matcher; these are now method and path scoped. Anonymous requests previously returned 403; the configured authentication entry point now returns 401. A wrong role receives 403.
- The JWT filter is now registered only inside Spring Security's filter chain; Spring Boot's second servlet filter registration is disabled.
- `JwtAuthenticationFilter` parses a signed JWT subject, loads the account through `CustomUserDetailsService`, validates the token, and installs *current* authorities. The service rejects `active=false` and combines the base role with current role grants and direct user grants. Revocation is effective on the next request if the database update has committed; an in-flight request retains its already loaded authorities. The JWT no longer supplies trusted role or permission claims. `/api/me` returns only the authenticated account's own ID, username, base role, and effective permissions.
- Super Admin identity comes from `users.role = ROLE_SUPER_ADMIN`, constrained by V3. Only the bootstrap seeder currently creates that role; there is no role-changing API. Grant endpoints reject a Super Admin user ID with the same 404 as an unknown ID. The current API has no account list, search, export, reporting relationship, or audit reader, so those paths cannot currently reveal Super Admin accounts. This is a current-route finding, not a guarantee for future modules.
- Grant mutations check Super Admin again in the service, validate permission codes, forbid assigning `PERMISSION_MANAGE`, and write an audit row in the same transaction as each change. Admin and USER cannot grant themselves permissions through current APIs. No route exposes `permission_change_audit`. The table is append-only by application behavior, not protected against direct modification by a database administrator; database privileges and retention controls are deployment work.
- The bootstrap seeder now requires both environment credentials, takes a PostgreSQL transaction advisory lock, and creates an account only when no Super Admin exists. It fails if the chosen username is already occupied. It does not overwrite an existing password and has no HTTP endpoint. Remove bootstrap environment variables after provisioning; use a deployment secret store rather than checked-in files or command-line arguments. The configured JWT signing key is validated at startup for a minimum 32 UTF-8 bytes and positive expiration.

### Secret-history finding

The current tracked `application.properties` uses environment placeholders, and the current tracked seeder contains no literal password. Git history still contains a JWT signing secret in `application.properties` and a bootstrap password in `DatabaseSeeder.java` in commits `91fe4f5`, `e795692`, and `c4b9266`. Values are intentionally omitted here. Treat both as compromised: rotate the deployed JWT key and bootstrap account password, invalidate old tokens by changing the signing key, and assess whether any copies of the repository need history cleanup. The local `application.yml` is ignored by Git; deployment must not rely on untracked local values.

### JWT and session decision

Current flow: username/password login checks the database password hash; the response returns a signed, one-day bearer JWT; React stores it in local storage and sends it in `Authorization: Bearer ...`; each request reloads account status and grants. Logout only removes the browser copy. The server cannot revoke one stolen token without rotating the global key, deactivating the account, or adding token/session state. Local-storage tokens are readable by injected JavaScript, and password changes currently do not invalidate existing JWTs. This flow conflicts with `docs/architecture.md`, which specifies server-side sessions with HttpOnly, Secure cookies.

**Recommendation:** adopt the documented server-side session-cookie model in a separate authentication migration before broad HR/payroll rollout. That enables server-side session revocation and keeps credentials out of JavaScript, but requires CSRF protection, cookie SameSite/Secure settings, session storage and expiry policy, and careful cross-origin deployment. Until then, keep short JWT lifetime, rotate the exposed key, apply a strong content-security policy, and avoid treating frontend local-storage role values as authorization. This review does not migrate authentication.

### Verification and remaining limits

Automated checks added: MVC route tests for anonymous access, wrong role, explicit Super Admin access, public login method, and deny-by-default; unit tests for current grant composition, revocation with the same token, inactive-account rejection, unauthorized grant mutation, Super Admin target 404 parity, bootstrap behavior, and JWT secret validation. These tests use mocked repositories/JDBC and do not verify PostgreSQL or Flyway execution. The existing `OrbitApplicationTests.contextLoads` also needs configured database credentials.

Maven `test-compile` passed offline. Maven Surefire execution could not start: its cached plugin lacked `SurefireReportParameters`, and Maven Central DNS was unavailable. The MVC tests were executed through Spring `TestContextManager` with the compiled classes, and unit tests through direct Java entry points with the local Mockito agent; both paths passed. These direct runs do not replace a normal `mvn test` in CI. For database-backed verification, provide a disposable PostgreSQL instance plus `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET`, then run the full Maven suite and migration/HTTP integration tests. Super Admin isolation for future employee endpoints, exports, search, pagination, and indirect relationships remains unverified because those endpoints do not exist. The production deployment's credential rotation, database privileges, and browser security headers were not observable from this repository.
