$ErrorActionPreference = "Stop"

$envFile = Join-Path (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $name, $value = $line.Split("=", 2)
            $name = $name.Trim()
            if (-not (Test-Path "Env:$name")) {
                Set-Item -Path "Env:$name" -Value $value.Trim()
            }
        }
    }
}

$adminUser = $env:MYSQL_ADMIN_USER
$adminPassword = $env:MYSQL_ADMIN_PASSWORD
$appUser = if ($env:SPG_DB_USERNAME) { $env:SPG_DB_USERNAME } else { "spg_app" }
$appPassword = $env:SPG_DB_PASSWORD

if (-not $adminUser -or -not $adminPassword -or -not $appPassword) {
    Write-Error "Set MYSQL_ADMIN_USER, MYSQL_ADMIN_PASSWORD, and SPG_DB_PASSWORD in this terminal session before running database setup. Do not paste those values into chat or source files."
    exit 1
}

if ($appUser -notmatch "^[A-Za-z0-9_]+$") {
    Write-Error "SPG_DB_USERNAME may contain only letters, numbers, and underscores."
    exit 1
}

$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if (-not (Test-Path $mysql)) {
    Write-Error "MySQL client was not found at the expected MySQL Server 8.0 location."
    exit 1
}

$defaultsFile = Join-Path ([System.IO.Path]::GetTempPath()) ("spg-mysql-" + [guid]::NewGuid().ToString("N") + ".cnf")
$sqlFile = Join-Path ([System.IO.Path]::GetTempPath()) ("spg-mysql-" + [guid]::NewGuid().ToString("N") + ".sql")
$quotedPassword = $appPassword.Replace("\", "\\").Replace("'", "''")
$quotedAdminPassword = $adminPassword.Replace('"', '\"')

try {
    @(
        "[client]"
        "user=$adminUser"
        "password=""$quotedAdminPassword"""
        "host=127.0.0.1"
        "port=3306"
        "protocol=TCP"
    ) | Set-Content -Path $defaultsFile -Encoding Ascii

    @"
CREATE DATABASE IF NOT EXISTS smart_parcel_guard
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$appUser'@'localhost' IDENTIFIED BY '$quotedPassword';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
    ON smart_parcel_guard.* TO '$appUser'@'localhost';
FLUSH PRIVILEGES;
"@ | Set-Content -Path $sqlFile -Encoding Ascii

    & $mysql --defaults-extra-file="$defaultsFile" --batch --silent -e "source $($sqlFile.Replace('\','/'))"
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    Write-Output "Database setup finished for smart_parcel_guard."
}
finally {
    if (Test-Path $defaultsFile) {
        Remove-Item -Path $defaultsFile -Force
    }
    if (Test-Path $sqlFile) {
        Remove-Item -Path $sqlFile -Force
    }
}
