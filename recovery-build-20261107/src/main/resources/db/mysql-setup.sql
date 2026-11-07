-- Creates the application database.
-- The application account is created by backend/scripts/setup-database.ps1,
-- which reads MYSQL_ADMIN_USER, MYSQL_ADMIN_PASSWORD, SPG_DB_USERNAME, and
-- SPG_DB_PASSWORD from the environment or the gitignored project .env file.
-- Do not put a password in this file.

CREATE DATABASE IF NOT EXISTS smart_parcel_guard
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
