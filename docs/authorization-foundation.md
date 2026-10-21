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

Current flow: username/password login checks the database password hash; the response returns a signed, one-day bearer JWT; React stores it in local storage. The backend accepts `Authorization: Bearer ...` and reloads account status and grants on each request, but the current React code has no authenticated API client and does **not** attach the token to requests after login. Logout only removes the browser copy. The server cannot revoke one stolen token without rotating the global key, deactivating the account, or adding token/session state. Local-storage tokens are readable by injected JavaScript, and password changes currently do not invalidate existing JWTs. This flow conflicts with `docs/architecture.md`, which specifies server-side sessions with HttpOnly, Secure cookies.

**Recommendation:** adopt the documented server-side session-cookie model in a separate authentication migration before broad HR/payroll rollout. That enables server-side session revocation and keeps credentials out of JavaScript, but requires CSRF protection, cookie SameSite/Secure settings, session storage and expiry policy, and careful cross-origin deployment. Until then, keep short JWT lifetime, rotate the exposed key, apply a strong content-security policy, and avoid treating frontend local-storage role values as authorization. This review does not migrate authentication.

### Verification and remaining limits

Automated checks added: MVC route tests for anonymous access, wrong role, explicit Super Admin access, public login method, and deny-by-default; unit tests for current grant composition, revocation with the same token, inactive-account rejection, unauthorized grant mutation, Super Admin target 404 parity, bootstrap behavior, and JWT secret validation. These tests use mocked repositories/JDBC and do not verify PostgreSQL or Flyway execution. The existing `OrbitApplicationTests.contextLoads` also needs configured database credentials.

Maven `test-compile` passed offline. Maven Surefire execution could not start: its cached plugin lacked `SurefireReportParameters`, and Maven Central DNS was unavailable. The MVC tests were executed through Spring `TestContextManager` with the compiled classes, and unit tests through direct Java entry points with the local Mockito agent; both paths passed. These direct runs do not replace a normal `mvn test` in CI. For database-backed verification, provide a disposable PostgreSQL instance plus `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET`, then run the full Maven suite and migration/HTTP integration tests. Super Admin isolation for future employee endpoints, exports, search, pagination, and indirect relationships remains unverified because those endpoints do not exist. The production deployment's credential rotation, database privileges, and browser security headers were not observable from this repository.

## Infrastructure decision proposal (21 October 2026; documentation only)

### Authentication lifecycle and proposed direction

| Concern | Current implementation | Proposed server-side session |
| --- | --- | --- |
| Login | `POST /api/auth/login` authenticates username/password with Spring Security and BCrypt; returns JWT and a role string | Authenticate as now, rotate the session ID, store only an opaque session ID in an HttpOnly cookie; obtain identity/permissions from `/api/me` |
| Credential lifetime | `JWT_EXPIRATION_MS`, default `86400000` ms (24 hours); token contains subject, issue time, expiry and signature, no trusted grants | Server-enforced idle and absolute session timeouts, explicit invalidation on logout and account deactivation |
| Request authentication | `JwtAuthenticationFilter` reads Bearer token, validates signature/expiry, reloads account and permissions from PostgreSQL | Spring Security resolves the server-side session; account status and grants still need fresh or explicitly invalidated authorization state |
| Frontend | Login stores token and role in `localStorage`; `App.tsx` checks only token presence; dashboard/sidebar display/filter using local role | Browser sends cookie automatically; UI calls `/api/me` for authoritative state and treats 401 as signed out |
| Logout/revocation | Dashboard/TopNavbar delete local values only; a copied token remains usable until expiry unless account is deactivated or signing key changes. Revoked grants take effect on the next request. Password changes do not invalidate existing tokens. | Invalidate the server session and clear its cookie; invalidate all sessions for compromised or deactivated accounts; define session renewal and concurrent-session policy |

**Recommendation for review:** use server-side sessions with HttpOnly, Secure cookies, as already specified in `docs/architecture.md`, before sensitive HR and salary flows are added. This supplies individual server-side revocation and avoids exposing the session credential to JavaScript. A session store is needed for multiple backend instances (or documented sticky-session behavior); choose its ownership and retention before implementation. Keep the current modular monolith and database-backed permission evaluation. Do not treat the local-storage role as an authorization claim.

The current bearer-token approach avoids cookie-based CSRF because browsers do not attach its Authorization header automatically. It still exposes a bearer credential to XSS and lacks individual token revocation. HttpOnly cookies reduce credential theft through JavaScript but do not stop injected JavaScript from making authenticated same-origin requests; continue output escaping, dependency review, and a restrictive content-security policy. Cookie authentication requires CSRF protection on login and all state-changing routes, including logout, plus session fixation protection and careful cookie `SameSite` policy. `SameSite` alone is not a substitute for CSRF tokens or origin checks.

Current development origins are Vite at `http://localhost:5173` (also allowed: `http://localhost:3000`) and Spring Boot at `http://localhost:8080`; login uses a hardcoded backend URL. CORS presently allows only those origins and the `Authorization`/`Content-Type` headers, without `allowCredentials`. For cookie sessions, prefer one HTTPS application origin via reverse proxy so frontend and `/api` share a site. If separate origins are required, explicitly configure allowed production origins, credentialed CORS, `credentials: 'include'`, CSRF token delivery/header, and cookie `SameSite`/domain/`Secure` settings. Never use wildcard origin with credentials. Define a local HTTPS or controlled development-cookie profile. Keep `Secure` cookies and TLS mandatory in production.

**Migration plan to review before coding:** agree production origin topology and session store; define idle/absolute timeouts and concurrent-session policy; design CSRF token/bootstrap and error behavior; add backend session login/logout and `/api/me` integration behind a deliberate cutover; replace local-storage route checks with server-verified state; test login, CSRF denial, logout, revocation, inactive account, cross-origin behavior, and expired sessions; rotate the previously exposed JWT key and retire bearer acceptance only after clients cut over. No authentication mechanism was changed in this task.

### Credential remediation and deployment checklist

- Current tracked `application.properties` reads `JWT_SECRET`, `JWT_EXPIRATION_MS`, `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` from environment. `JwtUtil` rejects signing keys shorter than 32 UTF-8 bytes and nonpositive expiry. `DatabaseSeeder` reads `BOOTSTRAP_SUPER_ADMIN_USERNAME` and `BOOTSTRAP_SUPER_ADMIN_PASSWORD` directly from the process environment only during startup. No unauthenticated bootstrap HTTP endpoint exists. The checked-in seed password and JWT key were removed from current source but remain in the three historical commits listed above. A scan of tracked working files found no literal JWT key or datasource password; a bootstrap-password pattern in `DatabaseSeederTest` is a test fixture, not a deployment credential.
- Provision secrets through the deployment secret manager into the backend process environment, with access limited to the service/operator. Do not put values in Git, Docker image layers, `application.yml`, Maven command arguments, shell history, or logs. The ignored local `application.yml` exists in this checkout and carries datasource/Flyway settings; inspect or remove it in each test/deployment environment. A clean checkout will not contain it.
- Rotate any deployed JWT signing key that matches a historical value. Deploy the new key to all backend instances together; existing JWTs will fail signature validation and users must sign in again. Confirm clients handle 401. Do not print the old or new value in tickets/logs.
- Rotate the bootstrap Super Admin's password through an authorized account-recovery or controlled operator procedure that updates its BCrypt hash and leaves an audit record. The seeder intentionally will **not** replace an existing account's password. Verify login with the new credential, then remove both bootstrap environment variables. If a previously committed datasource credential was ever used (including an ignored local file copied to deployment), rotate that DB principal too and update its secret-manager entry.
- Review exposure of clones, CI logs, build artifacts, and deployed environments; decide separately whether Git history rewrite is warranted. History rewrite and live secret changes require explicit authorization and coordinated rollout. Neither was performed here.

### Disposable PostgreSQL verification recipe

**Prerequisites:** a newly created, empty, disposable PostgreSQL database and a dedicated role with schema creation/migration rights. Record the database host/name before running anything; never point these steps at an existing HR or production database. PostgreSQL client tools are installed locally, but no disposable database or authorized credentials were supplied for this review. The repository has no automated PostgreSQL/Testcontainers setup or database-backed HTTP tests yet.

Supply these variables through the environment or a secret manager (values omitted):

| Variable | Required | Purpose |
| --- | --- | --- |
| `DB_URL` | Yes for verification | JDBC URL of the disposable database; override the tracked localhost default explicitly |
| `DB_USERNAME`, `DB_PASSWORD` | Yes | Dedicated disposable DB role |
| `JWT_SECRET` | Yes | Fresh random signing key, at least 32 UTF-8 bytes; never reuse the historical key |
| `JWT_EXPIRATION_MS` | Optional | Defaults to 24 hours |
| `SPRING_FLYWAY_BASELINE_ON_MIGRATE` | Set to `false` | Prevent the ignored local `application.yml` (`baseline-on-migrate: true`) from silently baselining a nonempty schema |
| `BOOTSTRAP_SUPER_ADMIN_USERNAME`, `BOOTSTRAP_SUPER_ADMIN_PASSWORD` | Only for first-account verification | Supply both only for first startup, then unset; omit both for ordinary tests |

Use `cd backend && ./mvnw test` after dependency resolution, with the variables above. `OrbitApplicationTests.contextLoads` starts the Spring context, so Flyway runs at startup before JPA validation (`spring.jpa.hibernate.ddl-auto=validate`) and before the `DatabaseSeeder` runner. For a clean database, V1 creates `system_status`, V2 creates `users`, and V3 adds `users.active`, the fixed account-role check, permission catalog/grants, audit table, constraints, indexes, and initial Admin grants. Flyway records successful migrations in `flyway_schema_history`. Confirm read-only that history contains successful V1–V3 rows, the intended tables exist, and there is at most one bootstrap Super Admin; keep actual test credentials out of output. A second startup should apply no migrations and should not create another Super Admin.

Migration compatibility caveats: V1/V2 are not idempotent SQL outside Flyway; never run them manually against an existing schema. V3 requires all preexisting `users.role` values to be one of `ROLE_SUPER_ADMIN`, `ROLE_ADMIN`, or `ROLE_USER`; incompatible rows cause migration failure and need an approved data-cleanup plan. V3 defaults existing users to `active=true`, preserves password hashes, and seeds Admin grants without an account. `baseline-on-migrate: true` can skip required history on a nonempty legacy schema; do not use it as a migration-repair shortcut. The V3 audit table is protected by application routes, while database privileges and retention still require deployment review.

For HTTP integration verification, start the backend against **that same disposable database** only after migrations pass, bootstrap a first Super Admin with temporary environment values, then exercise login, `/api/me`, Super Admin grant read/write, Admin/USER denials, grant revocation using an existing token, inactive-account access, and unknown/Super Admin target 404 parity. Create Admin/USER accounts and change `users.active` only through isolated test fixtures or controlled SQL on the disposable database: no account-creation or deactivation API exists yet. Keep tokens and credentials out of terminal logs. No database-backed HTTP integration test harness currently implements this sequence; add it in a subsequent test task before claiming end-to-end verification.

### Maven test execution status

`./mvnw -o -q -Dmaven.repo.local=/private/tmp/aeliya-m2/repository -DskipTests test-compile` passed in this environment. `./mvnw -o -q -Dmaven.repo.local=/private/tmp/aeliya-m2/repository -Dtest=AuthorizationRoutesTest test` failed **before running tests**: the cached `maven-surefire-plugin:3.5.6` JAR lacks its `org.apache.maven.surefire` dependencies, including the class `SurefireReportParameters`; its POM and dependency artifacts are absent from the local cache. A prior online retry failed because Maven Central DNS was unavailable. The test code is not implicated by that plugin failure.

Once network access or an approved internal Maven mirror is available, use a writable Maven repository and run `cd backend && ./mvnw -U test` (or configure the internal mirror in Maven settings). Maven should fetch the plugin POM and its matching Surefire dependencies; do not change versions or skip tests merely to bypass the cache. Run on the project's Java 21 target and configure database environment for the context test. If Mockito agent attachment is restricted by the runner, configure Mockito as a Java agent in that test environment rather than disabling tests. Check Surefire's per-test reports for actual execution and failures.

Previously executed focused checks: MVC route tests and unit tests passed through direct Java test entry points with Spring `TestContextManager`/Mockito agent; those are narrower than a normal Surefire run. This task reran only offline `test-compile` and the failing offline Surefire command. Flyway, first-account provisioning against PostgreSQL, database-backed authorization behavior, and HTTP integration remain unverified.
