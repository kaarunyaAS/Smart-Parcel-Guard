# Smart Parcel Guard

Smart Parcel Guard is a Java 21 / Spring Boot parcel-security application with a
separate browser dashboard. Firmware and physical ESP32 integration are
separate and are not required to run the software demo.

## Requirements

- JDK 21
- Maven 3.6.3 or newer
- MySQL 8.0 or 8.4
- VS Code with Live Server (for the frontend)

The frontend is configured for the backend at `http://localhost:8081` and
Live Server origins `http://localhost:5500` or `http://127.0.0.1:5500`.

## Local setup

1. Copy `.env.example` to `.env` in the project root and set local-only MySQL
   administrator/application credentials and a randomly generated JWT secret
   of at least 32 bytes. Use unquoted values (or matching single/double quotes)
   and keep `.env` private; it is git-ignored.
2. From PowerShell at the project root, create the application database and
   account:

   ```powershell
   .\backend\scripts\setup-database.ps1
   ```

   The script reads credentials from `.env` or the current process environment
   and locates `mysql.exe` on `PATH` or in the standard MySQL 8.0/8.4 folders.
3. In the same PowerShell session, load the application settings without
   printing them, then start the backend on the port expected by the frontend:

   ```powershell
   foreach ($line in Get-Content .env) {
       $line = $line.Trim()
       if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
           $name, $value = $line.Split("=", 2)
           $value = $value.Trim()
           if ($value.Length -ge 2 -and
               (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'")))) {
               $value = $value.Substring(1, $value.Length - 2)
           }
           [Environment]::SetEnvironmentVariable($name.Trim(), $value, "Process")
       }
   }
   Set-Location backend
   mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
   ```

   Flyway is enabled by default for a fresh local database and applies the
   schema migrations. Hibernate validates the resulting schema.
4. In VS Code, open `frontend/index.html` with Live Server and use the local
   address on port 5500.
5. Create the first ADMIN through `POST /api/auth/register` using a local
   trusted REST client. The first registered account is assigned ADMIN; later
   public registrations are assigned OPERATOR. There is no registration page.
   Never commit or share the registration password.

## Verification

Run backend tests from `backend` after loading `.env` into the process as
above:

```powershell
mvn test "-Dspring.flyway.enabled=false"
```

The VS Code Java test configuration loads the project `.env` and disables
Flyway for tests. The current verified suite has 46 passing tests, including
API persistence, authorization, CORS, validation, risk/alert behavior, and
authenticated WebSocket event delivery.

## Current readiness

Phase 4 remains in progress; Phase 5 has not started. The latest Java 21
backend artifact, deployment backup, test evidence, and remaining QA blockers
are recorded in [ROADMAP.md](./ROADMAP.md). No physical GPS, sensor, camera, or
cellular hardware result is claimed.
