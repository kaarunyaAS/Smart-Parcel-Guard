# Smart Parcel Guard roadmap

The system is a Java 21 Spring Boot backend with a MySQL database. The operator and administrator interface is a separate web frontend. ESP32 firmware is developed separately in Arduino C/C++ and is not run on the application server.

The original seven-phase plan is consolidated into five phases. Phases 1, 2, and 3 are complete. Phase 4 is in progress; Phase 5 has not been started.

| Phase | Scope | Status |
|---|---|---|
| 1 | Platform and database | Completed |
| 2 | Identity, parcels, and devices | Completed |
| 3 | Hardware telemetry and evidence ingestion | Completed |
| 4 | Intelligence, alerts, and the complete frontend | In progress |
| 5 | Hardware integration, final testing, and delivery | Pending |

## Phase 1 — Completed

Platform foundation:

- Maven project on Java 21 and Spring Boot.
- MySQL database `smart_parcel_guard` and the `spg_app` account, with credentials supplied from the environment.
- Flyway `V1__init_schema.sql`.
- JPA entities for users, parcels, devices, telemetry, evidence, alerts, risk, commands, notifications, and audit data.
- Hibernate schema validation.

## Phase 2 — Completed

Account and operations APIs:

- User registration and login.
- JWT access tokens, validation, expiry, and logout revocation.
- BCrypt password hashing.
- `ADMIN` and `OPERATOR` authorization.
- User management.
- Parcel registration, retrieval, update, status changes, and status history.
- Device registration, one-time device token display, SHA-256 token storage, and parcel assignment.
- Request validation, consistent API errors, and Swagger/OpenAPI.
- Flyway `V2__auth_and_device_enabled.sql`.

## Phase 3 — Completed

Telemetry and evidence ingestion. Flyway `V3__telemetry_evidence.sql` introduced the Phase 3 schema; the current database has since advanced to schema version 4.

- GPS ingestion and Leaflet-compatible location history.
- MPU6050 acceleration readings, with impact detection separated from ordinary vibration.
- Reed-switch open and closed state, including unauthorized-opening detection.
- Device-token authentication. Raw tokens are not stored or returned again.
- JPEG and PNG evidence upload for inside and outside cameras, stored as files with metadata in MySQL.
- Device heartbeat and online/offline status from a configurable timeout.
- Callers must declare telemetry as `REAL` or `SIMULATED`.
- Automated tests for Phases 1–3: 25 tests, 0 failures.

Phase 3 does not score risk, send alerts, or provide the dashboard. No physical device is connected.

## Phase 4 — In Progress

This phase merges the original risk-analysis phase and the original frontend phase. Existing Phase 4 source is being continued; do not replace completed modules or start Phase 5.

Implemented so far:

- Java risk scoring, suspicious-event correlation, configurable database-backed weights/thresholds, and LOW/MEDIUM/HIGH/CRITICAL classification.
- Periodic risk recalculation for active device assignments so stale heartbeats can produce offline-risk alerts without waiting for another telemetry request.
- ADMIN-only `GET` and `PUT /api/admin/risk-settings` endpoints and a settings UI, using the existing `security_settings` table with range and cross-field validation.
- Persisted security events, risk history, automatic HIGH/CRITICAL alerts, and alert read/acknowledge/resolve APIs.
- Unresolved-alert deduplication by parcel/event type.
- Admin/operator dashboard APIs backed by stored parcel, device, risk, and alert data.
- JWT-authenticated WebSocket updates with operator parcel-visibility checks.
- Login, dashboard, parcel registration/tracking, Leaflet map, sensor chart, evidence gallery, event timeline, and alert workflows.
- Admin user creation/editing/status controls and device registration/editing/assignment controls using the existing APIs.
- Request validation failures return structured field/message details; malformed JSON gets a separate generic 400 response, without echoing submitted values.
- Flyway V4 risk/alert migration; the local MySQL test database is at schema version 4.
- Backend regression and Phase 4 API tests, including registration validation and malformed-JSON responses, persisted stale-heartbeat risk/alert verification, invalid/expired/disabled WebSocket credential rejection, and authorized parcel-only update delivery.

Verification and remaining blockers:

- On 2026-11-07, the latest backend source was built in an isolated directory outside `backend/target` using Java 21. The complete backend suite passed 45 tests (0 failures, 0 errors, 0 skipped) before deployment. The executable JAR is `deployments/phase4-cors-build-20261107/target/smart-parcel-guard-0.1.0.jar` (71,430,108 bytes; SHA-256 `31BA329C62501840D6C66BE0ECA764D9EA2A8582E0AFF07DEABEE6D9CE4F61CE`), with the Spring Boot launcher, application and updated security classes, and 94 bundled dependencies verified. The Maven package compiled current production and test sources; tests were skipped during packaging because the full suite had already passed immediately beforehand.
- The initial 45-test deployment on port 8081 (PID 21392, SHA-256 `31BA329C62501840D6C66BE0ECA764D9EA2A8582E0AFF07DEABEE6D9CE4F61CE`) was superseded by the later 46-test build documented in the current verification update below. Port 8080 and the staged diagnostic instance on 8082 were preserved.
- Maven `package` built the latest backend source from an isolated copy at `recovery-build-20261107`, outside the active `backend/target` directory, using JDK 21 at `C:\Users\ELCOT\.jdks\jdk-21.0.13+9`. The verified executable artifact is `recovery-build-20261107/target/smart-parcel-guard-0.1.0.jar` (71,428,457 bytes; modified 2026-11-07 16:14). Its manifest declares `org.springframework.boot.loader.launch.JarLauncher` and `com.smartparcelguard.SmartParcelGuardApplication`; it contains the application class and 94 bundled dependency JARs. This is a separate recovery artifact and has not replaced the existing target JAR or either running process.
- The isolated recovery-artifact Maven package build passed before the registration diagnostic tests were added: 38 tests, 0 failures, 0 errors, and 0 skipped.
- The complete backend regression suite was rerun on 2026-11-07 in a fresh isolated copy outside `backend/target`: 43 tests, 0 failures, 0 errors, and 0 skipped. This includes five registration diagnostic tests for invalid username, invalid email, short password, missing fields, and malformed JSON; they verify submitted values are not echoed and `AuthService` is not invoked. The suite itself did not submit any registration requests.
- The original diagnostic JAR remains staged on port 8082 at `deployments/phase4-diag-20261107-1642/smart-parcel-guard-0.1.0.jar` (71,429,505 bytes; SHA-256 `D34ABD4BB8C3FD49CD4DD8D1E52BDC83D91C848AD1C73ED219D74FEC910D4D62`). Port 8082 reports health `UP`, Flyway is disabled, and Hibernate is `validate`. Its scheduler still has the earlier maximum 32-bit millisecond delay (~24.9 days); stop it before that delay expires or obtain approval for an explicit scheduler-disable change. On port 8081, scheduling auto-configuration is disabled and the annotated risk task's initial and repeat delays are set to `Long.MAX_VALUE` milliseconds. Because the application uses `@EnableScheduling`, this defers execution for an effectively indefinite staging interval but is not a permanent source-level scheduler disable.
- On 2026-11-07, the single invalid-only registration request was sent to the staged 8082 instance using PowerShell `Invoke-RestMethod` with a UTF-8 byte-array JSON body. It returned HTTP 400 with `Validation failed` and a structured `username` error (`may contain letters, numbers, dots, underscores, and hyphens`). The response did not echo any submitted values. No retry was made. The read-only post-request user count remained zero. PIDs on ports 8080, 8081, and 8082 remained unchanged.
- Frontend checks pass: `node --check` succeeded for the single JavaScript file; the single HTML file has 42 IDs, all unique.
- Read-only Flyway history shows successful V1, V2, V3, and V4; no migration was applied during deployment. A read-only check after deployment reported schema version 4, one user, and one ADMIN. No user or other database record was modified as part of deployment.
- Diagnostic-change audit against the pre-diagnostic isolated source found exactly four backend source/test changes: `ApiResponse.java`, new `FieldValidationError.java`, `GlobalExceptionHandler.java`, and new `AuthRegistrationValidationTest.java`. `RegisterRequest.java` is unchanged, and no migration or other backend source files changed. The roadmap is also updated with this audit. The project directory has no Git metadata, so this comparison uses the preserved pre-diagnostic isolated source rather than a Git diff.
- The existing `backend/target/smart-parcel-guard-0.1.0.jar` remains unchanged at 243,414 bytes (modified 2026-11-07 15:27). It contains the application class but no Spring Boot launcher or bundled dependencies. The 8080 process still uses the legacy application and remains healthy; do not overwrite its JAR or restart it. The latest verified executable JAR is running on port 8081; the earlier diagnostic JAR remains on port 8082.
- The registration service assigns ADMIN to the first registered user and OPERATOR to later registrations; `/api/auth/register` is public, but the login page has no registration UI. The database setup script creates the database/application account only and does not seed an initial user. The latest read-only aggregate check found one user and one ADMIN, consistent with the owner's report of successful first-ADMIN registration. No credentials, password hashes, or tokens were read or exposed.
- 2026-11-07 dashboard QA follow-up: browser verification covered ADMIN dashboard, parcel search/details, existing TEST parcel's empty GPS/sensor/evidence states and timeline, Alerts/Devices/Users/Risk Settings page loads, logout and authenticated refresh. One additional synthetic QA parcel (ID 142, `SPG-P-20261107-W9Y4UQ`) was created through the UI and moved from REGISTERED to READY_FOR_PICKUP. Read-only MySQL checks confirmed its parcel row and both status-history entries; existing TEST parcel 134 remains unchanged. Final counts were two parcels and three parcel-status-history rows. No user or device was created.
- The database currently has one enabled ADMIN and no OPERATOR. The proposed QA OPERATOR was never submitted, so OPERATOR browser workflows and live backend role-denial requests remain blocked. Device and alert actions were not testable because their lists are empty; no device token or new user was created. Risk Settings loaded successfully, but saving global settings was intentionally not attempted. GPS, sensor and evidence UI empty states were observed; populated-data workflows remain untested.
- 2026-11-07 source QA fixes: `LiveUpdateConfig` now sets HTTP 401 before rejecting an unauthenticated/invalid WebSocket handshake; missing, malformed, expired and disabled credentials are covered, while valid authenticated parcel-update delivery remains covered. `ParcelRequest` converts blank optional sender/receiver phone strings to `null` while preserving rejection of invalid nonblank values. The dashboard API test now checks the parcel-count increase against its initial database baseline instead of assuming an empty shared database.
- Full backend test suite after these fixes: 46 tests, 0 failures, 0 errors, 0 skipped, Java 21. Frontend `node --check` passed; HTML parsing reported no duplicate IDs, misnested tags, or unclosed tags. Read-only Flyway history remains at version 4. No migrations were applied.
- The previous isolated readiness JAR at `deployments/phase4-readiness-20261107/smart-parcel-guard-0.1.0.jar` was superseded by the later verified build. That earlier artifact was not deployed.
- Physical ESP32/GPS/sensor/camera/SIM7600 integration remains unverified. Phase 4 remains in progress; Phase 5 remains pending.

### Current readiness verification — 2026-11-07

- The complete backend suite was rerun from an isolated copy using Java 21: **46 passed, 0 failed, 0 errors, 0 skipped**. Tests completed with Maven `BUILD SUCCESS`, using in-process Surefire (`forkCount=0`) and the cached Mockito agent to stay within available memory; no tests were skipped or weakened. The project `backend/target` directory and its legacy JAR were not used for packaging.
- The current source was packaged outside `backend/target` as `deployments/phase4-verified-20261107/smart-parcel-guard-0.1.0.jar` (71,430,357 bytes; SHA-256 `78FD82DAEB46E316E1538E97E9C9895865F5EB76EB6583A3B98613C72A4F6711`). Its Spring Boot launcher, application start class, and 94 nested dependency JARs were verified.
- Before deployment, the exact 8081 artifact was backed up without replacement to `deployments/backups/smart-parcel-guard-8081-predeploy-20261107-192822.jar` (71,430,108 bytes; SHA-256 `31BA329C62501840D6C66BE0ECA764D9EA2A8582E0AFF07DEABEE6D9CE4F61CE`). Only the identified old 8081 process (PID 21392) was stopped. The new artifact is now running as PID 972 under Java 21.0.13+9. Startup logs confirm Tomcat started on 8081 and `SmartParcelGuardApplication` completed startup. Flyway is disabled for this deployment and Hibernate uses `validate`; scheduling auto-configuration is disabled and the `@Scheduled` risk task is deferred using `Long.MAX_VALUE` delays.
- Post-deployment, ports 8080, 8081 and 8082 reported health `UP` under PIDs 17676, 972 and 19828. Ports 8080 and 8082 were not stopped or restarted. A read-only MySQL check confirmed Flyway version 4 and unchanged aggregates: one user, two parcels, three parcel-history rows, and zero devices, assignments, GPS locations, sensor readings, evidence rows, alerts, and risk-analysis rows.
- Live 8081 security smoke checks returned HTTP 401 for unauthenticated dashboard/users API calls and for a WebSocket handshake without a token. The frontend-origin CORS preflight from `http://localhost:5500` returned HTTP 200 and allowed the origin, expected methods and Authorization header. Automated WebSocket tests cover invalid/missing/expired/disabled credentials and authenticated operator event delivery.
- Frontend `node --check` passed; HTML parsing passed with 42 unique IDs and no misnested/unclosed tags. `setup-database.ps1` now discovers `mysql.exe` on PATH or in standard MySQL 8.0/8.4 install locations; PowerShell syntax and MySQL 8.4 path discovery were verified without executing setup SQL. New [README.md](./README.md) and [.env.example](./.env.example) provide secret-safe laptop setup guidance.
- ADMIN browser workflows could not be resumed in this verification. The active integrated browser is at `http://localhost:5500/frontend/` and still shows the sign-in form after the user was asked to sign in. No login credentials were submitted by the assistant, no QA operator was created, and no new QA records were added in this turn. Parcel/device/user/alert/risk UI workflows, live browser event delivery, and authenticated OPERATOR restrictions remain unverified. Phase 4 remains in progress; Phase 5 remains pending.

### CRUD audit follow-up — 2026-11-07

- Added `backend/src/test/java/com/smartparcelguard/Phase2CrudApiTest.java` with transaction-scoped API tests for ADMIN user creation, listing/detail reads, updates and activation/deactivation; parcel creation, detail/search reads, updates, status-history persistence; and device creation, reads, updates, assignment and unassignment history. The tests also check validation, duplicate user email, role restrictions, and that password hashes and device tokens are not returned. Both added tests passed.
- The complete Java 21 backend suite ran from the isolated `deployments/crud-audit-tests-20261107/backend` copy: **48 passed, 0 failed, 0 errors, 0 skipped**. Maven used in-process Surefire with bounded memory; no tests were skipped or weakened, and the active `backend/target` JAR was not used.
- Existing backend integration tests additionally cover generated alert lifecycle, risk-settings reads/updates, telemetry/evidence persistence, and authenticated WebSocket event delivery. These are API/service integration tests, not proof of the corresponding live dashboard workflows.
- CRUD API implementation/coverage: users support create/read/update and enable/disable; parcels support create/read/search/update/status changes and history; devices support create/read/update and assignment release; alerts are generated by events and support read/acknowledge/resolve; risk settings support read/update; assignments retain released history. Direct delete endpoints are not implemented. Risk settings have no user-facing create/delete operation, and alerts have no manual-create/delete operation.
- Frontend checks passed: `node --check` for the one JavaScript file; HTML structural parsing found 42 unique IDs and no mismatched or unclosed tags. An initial browser check showed the sign-in form and reported frontend CSS/JavaScript request failures (`net::ERR_INSUFFICIENT_RESOURCES`). After the user confirmed the browser was ready, rereading both shared tabs still showed the sign-in form rather than an authenticated dashboard. No credentials were entered and no CRUD action was attempted. ADMIN/OPERATOR browser CRUD, persistence-after-refresh, and actual browser WebSocket delivery therefore remain blocked/unverified.
- Read-only MySQL verification confirmed Flyway V4 and current counts of 2 users (one enabled ADMIN and one enabled OPERATOR), 2 parcels, 1 enabled OFFLINE device, 3 parcel-status-history rows, 0 device assignments, and 0 alerts, location history, sensor readings, camera images, or risk analyses. These user/device counts differ from the previously recorded baseline of one user and zero devices. The audit did not create or alter persistent user/device records; the source of the difference has not been established, so no records were changed. This discrepancy and the absence of browser CRUD verification keep Phase 4 IN PROGRESS; Phase 5 remains PENDING.

The existing backend modules, APIs, migrations, and tests stay in place. The frontend remains under the existing `frontend` directory.

## Phase 5 — Pending

This phase merges the original hardware phase and the original final-testing and delivery phase. It starts only after Phase 4 is accepted.

Firmware, written separately in Arduino C/C++ for ESP32:

- GPS, MPU6050, reed switch, and camera integration.
- SIM7600 4G communication.
- Connection to the Phase 3 device-token telemetry APIs.

Delivery:

- Physical hardware validation.
- End-to-end, security, performance, and reliability testing.
- Defect fixes.
- Deployment configuration.
- Technical documentation and the final project report.

The finished project remains a Java parcel-tracking and security system. The ESP32 firmware stays a separate program.
