-- Smart Parcel Guard database setup for MySQL 8.0/8.4.
-- Schema and initial security_settings are copied in Flyway order from V1 through V4.
-- Run only against an empty smart_parcel_guard database; this script is not a migration or rerunnable.
-- No database users, credentials, JWT secrets, or operational parcel/device records are created.
CREATE DATABASE IF NOT EXISTS smart_parcel_guard
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE smart_parcel_guard;

-- Begin Flyway migration V1__init_schema.sql
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(80) NOT NULL,
    email VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE parcels (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parcel_code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(2000) NULL,
    category VARCHAR(80) NOT NULL,
    declared_value DECIMAL(12, 2) NOT NULL,
    sender_name VARCHAR(160) NOT NULL,
    sender_phone VARCHAR(30) NULL,
    sender_address VARCHAR(500) NULL,
    receiver_name VARCHAR(160) NOT NULL,
    receiver_phone VARCHAR(30) NULL,
    receiver_address VARCHAR(500) NULL,
    pickup_address VARCHAR(500) NOT NULL,
    pickup_latitude DECIMAL(9, 6) NULL,
    pickup_longitude DECIMAL(9, 6) NULL,
    destination_address VARCHAR(500) NOT NULL,
    destination_latitude DECIMAL(9, 6) NULL,
    destination_longitude DECIMAL(9, 6) NULL,
    expected_delivery_date DATE NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(40) NOT NULL,
    monitoring_state VARCHAR(20) NOT NULL,
    operator_id BIGINT NULL,
    created_by_id BIGINT NULL,
    latest_latitude DECIMAL(9, 6) NULL,
    latest_longitude DECIMAL(9, 6) NULL,
    latest_speed_mps DECIMAL(8, 2) NULL,
    latest_location_at DATETIME(6) NULL,
    latest_risk_score INT NULL,
    latest_risk_level VARCHAR(20) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_parcels_parcel_code UNIQUE (parcel_code),
    CONSTRAINT fk_parcels_operator FOREIGN KEY (operator_id) REFERENCES users (id),
    CONSTRAINT fk_parcels_created_by FOREIGN KEY (created_by_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE devices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_code VARCHAR(64) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    firmware_version VARCHAR(40) NULL,
    status VARCHAR(20) NOT NULL,
    last_seen_at DATETIME(6) NULL,
    battery_percent INT NULL,
    gps_status VARCHAR(20) NOT NULL,
    sensor_status VARCHAR(20) NOT NULL,
    camera_status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_devices_device_code UNIQUE (device_code),
    INDEX idx_devices_last_seen (last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE device_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_id BIGINT NOT NULL,
    parcel_id BIGINT NOT NULL,
    assigned_at DATETIME(6) NOT NULL,
    released_at DATETIME(6) NULL,
    active_device_key BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_device_assignments_active UNIQUE (active_device_key),
    CONSTRAINT fk_device_assignments_device FOREIGN KEY (device_id) REFERENCES devices (id),
    CONSTRAINT fk_device_assignments_parcel FOREIGN KEY (parcel_id) REFERENCES parcels (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE geofences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parcel_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    geofence_type VARCHAR(30) NOT NULL,
    boundary_json LONGTEXT NOT NULL,
    deviation_tolerance_meters DECIMAL(8, 2) NULL,
    active BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_geofences_parcel FOREIGN KEY (parcel_id) REFERENCES parcels (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE parcel_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parcel_id BIGINT NOT NULL,
    from_status VARCHAR(40) NULL,
    to_status VARCHAR(40) NOT NULL,
    changed_by_id BIGINT NULL,
    note VARCHAR(500) NULL,
    changed_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_parcel_status_history_parcel_time (parcel_id, changed_at),
    CONSTRAINT fk_parcel_status_history_parcel FOREIGN KEY (parcel_id) REFERENCES parcels (id),
    CONSTRAINT fk_parcel_status_history_user FOREIGN KEY (changed_by_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE location_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parcel_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    latitude DECIMAL(9, 6) NOT NULL,
    longitude DECIMAL(9, 6) NOT NULL,
    accuracy_meters DECIMAL(8, 2) NULL,
    speed_mps DECIMAL(8, 2) NULL,
    recorded_at DATETIME(6) NOT NULL,
    data_source VARCHAR(20) NOT NULL,
    gps_signal_status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_location_history_parcel_time (parcel_id, recorded_at),
    CONSTRAINT fk_location_history_parcel FOREIGN KEY (parcel_id) REFERENCES parcels (id),
    CONSTRAINT fk_location_history_device FOREIGN KEY (device_id) REFERENCES devices (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sensor_readings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parcel_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    acceleration_x DECIMAL(10, 4) NOT NULL,
    acceleration_y DECIMAL(10, 4) NOT NULL,
    acceleration_z DECIMAL(10, 4) NOT NULL,
    gyroscope_x DECIMAL(10, 4) NOT NULL,
    gyroscope_y DECIMAL(10, 4) NOT NULL,
    gyroscope_z DECIMAL(10, 4) NOT NULL,
    magnitude DECIMAL(10, 4) NOT NULL,
    movement_status VARCHAR(40) NOT NULL,
    recorded_at DATETIME(6) NOT NULL,
    data_source VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_sensor_readings_parcel_time (parcel_id, recorded_at),
    CONSTRAINT fk_sensor_readings_parcel FOREIGN KEY (parcel_id) REFERENCES parcels (id),
    CONSTRAINT fk_sensor_readings_device FOREIGN KEY (device_id) REFERENCES devices (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE security_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parcel_id BIGINT NOT NULL,
    device_id BIGINT NULL,
    event_type VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    latitude DECIMAL(9, 6) NULL,
    longitude DECIMAL(9, 6) NULL,
    recorded_at DATETIME(6) NOT NULL,
    risk_points INT NOT NULL,
    dedupe_key VARCHAR(160) NULL,
    data_source VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_security_events_dedupe UNIQUE (parcel_id, dedupe_key),
    INDEX idx_security_events_parcel_time (parcel_id, recorded_at),
    CONSTRAINT fk_security_events_parcel FOREIGN KEY (parcel_id) REFERENCES parcels (id),
    CONSTRAINT fk_security_events_device FOREIGN KEY (device_id) REFERENCES devices (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tamper_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parcel_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    latitude DECIMAL(9, 6) NULL,
    longitude DECIMAL(9, 6) NULL,
    recorded_at DATETIME(6) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    during_maintenance BIT(1) NOT NULL,
    security_event_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_tamper_events_parcel_time (parcel_id, recorded_at),
    CONSTRAINT fk_tamper_events_parcel FOREIGN KEY (parcel_id) REFERENCES parcels (id),
    CONSTRAINT fk_tamper_events_device FOREIGN KEY (device_id) REFERENCES devices (id),
    CONSTRAINT fk_tamper_events_security_event FOREIGN KEY (security_event_id) REFERENCES security_events (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE risk_analysis (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parcel_id BIGINT NOT NULL,
    score INT NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    reasons LONGTEXT NOT NULL,
    correlation_applied BIT(1) NOT NULL,
    calculated_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_risk_analysis_parcel_time (parcel_id, calculated_at),
    CONSTRAINT fk_risk_analysis_parcel FOREIGN KEY (parcel_id) REFERENCES parcels (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE alerts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parcel_id BIGINT NOT NULL,
    device_id BIGINT NULL,
    security_event_id BIGINT NULL,
    alert_type VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    latitude DECIMAL(9, 6) NULL,
    longitude DECIMAL(9, 6) NULL,
    risk_score INT NULL,
    status VARCHAR(20) NOT NULL,
    triggered_at DATETIME(6) NOT NULL,
    acknowledged_by_id BIGINT NULL,
    acknowledged_at DATETIME(6) NULL,
    resolved_by_id BIGINT NULL,
    resolved_at DATETIME(6) NULL,
    investigation_notes VARCHAR(2000) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_alerts_parcel_time (parcel_id, triggered_at),
    INDEX idx_alerts_status (status),
    CONSTRAINT fk_alerts_parcel FOREIGN KEY (parcel_id) REFERENCES parcels (id),
    CONSTRAINT fk_alerts_device FOREIGN KEY (device_id) REFERENCES devices (id),
    CONSTRAINT fk_alerts_security_event FOREIGN KEY (security_event_id) REFERENCES security_events (id),
    CONSTRAINT fk_alerts_acknowledged_by FOREIGN KEY (acknowledged_by_id) REFERENCES users (id),
    CONSTRAINT fk_alerts_resolved_by FOREIGN KEY (resolved_by_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE device_commands (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_id BIGINT NOT NULL,
    parcel_id BIGINT NULL,
    command_type VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payload LONGTEXT NULL,
    requested_by_id BIGINT NULL,
    requested_at DATETIME(6) NOT NULL,
    deadline_at DATETIME(6) NULL,
    delivered_at DATETIME(6) NULL,
    executed_at DATETIME(6) NULL,
    failure_reason VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_device_commands_device_status (device_id, status),
    CONSTRAINT fk_device_commands_device FOREIGN KEY (device_id) REFERENCES devices (id),
    CONSTRAINT fk_device_commands_parcel FOREIGN KEY (parcel_id) REFERENCES parcels (id),
    CONSTRAINT fk_device_commands_requested_by FOREIGN KEY (requested_by_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE command_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    command_id BIGINT NOT NULL,
    from_status VARCHAR(20) NULL,
    to_status VARCHAR(20) NOT NULL,
    note VARCHAR(500) NULL,
    changed_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_command_history_command FOREIGN KEY (command_id) REFERENCES device_commands (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE camera_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parcel_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    security_event_id BIGINT NULL,
    command_id BIGINT NULL,
    camera_role VARCHAR(20) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(80) NULL,
    byte_size BIGINT NULL,
    checksum_sha256 VARCHAR(64) NULL,
    captured_at DATETIME(6) NOT NULL,
    data_source VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_camera_images_parcel_time (parcel_id, captured_at),
    CONSTRAINT fk_camera_images_parcel FOREIGN KEY (parcel_id) REFERENCES parcels (id),
    CONSTRAINT fk_camera_images_device FOREIGN KEY (device_id) REFERENCES devices (id),
    CONSTRAINT fk_camera_images_security_event FOREIGN KEY (security_event_id) REFERENCES security_events (id),
    CONSTRAINT fk_camera_images_command FOREIGN KEY (command_id) REFERENCES device_commands (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    alert_id BIGINT NOT NULL,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    destination VARCHAR(255) NULL,
    sent_at DATETIME(6) NULL,
    error_message VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_alert FOREIGN KEY (alert_id) REFERENCES alerts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_id BIGINT NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id BIGINT NULL,
    detail VARCHAR(2000) NULL,
    ip_address VARCHAR(64) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_audit_logs_entity (entity_type, entity_id),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE security_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    setting_key VARCHAR(120) NOT NULL,
    setting_value VARCHAR(500) NOT NULL,
    description VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_security_settings_key UNIQUE (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO security_settings (setting_key, setting_value, description, created_at, updated_at) VALUES
    ('risk.weight.unusual_vibration', '10', 'Points for an unusual vibration event', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('risk.weight.route_deviation', '20', 'Points for leaving the permitted route or geofence', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('risk.weight.strong_impact', '25', 'Points for a strong impact', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('risk.weight.unauthorized_opening', '50', 'Points for an opening outside a maintenance window', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('risk.weight.correlation_bonus', '15', 'Extra points when different suspicious events occur inside the correlation window', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('risk.max_score', '100', 'Upper bound of the parcel risk score', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('risk.correlation_window_seconds', '600', 'Window used to correlate different event types', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('risk.dedupe_window_seconds', '300', 'Window that suppresses a repeated identical event', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('impact.threshold_g', '2.5', 'Acceleration magnitude, in g, required before an impact is recorded', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('impact.min_duration_ms', '50', 'Minimum impact duration in milliseconds', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('vibration.threshold_g', '1.3', 'Acceleration magnitude, in g, that counts as unusual vibration', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('stop.max_movement_meters', '15', 'Movement below this distance can count as a stop', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('stop.min_duration_seconds', '600', 'How long a stop must last before it is unusual', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('route.deviation_meters', '100', 'Default distance outside a route corridor that counts as deviation', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('device.offline_after_seconds', '180', 'Heartbeat age after which a device is offline', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('device.low_battery_percent', '20', 'Battery percentage that raises a low-battery alert', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
-- End Flyway migration V1__init_schema.sql

-- Begin Flyway migration V2__auth_and_device_enabled.sql
ALTER TABLE devices
    ADD COLUMN enabled BIT(1) NOT NULL DEFAULT 1 AFTER camera_status;

CREATE TABLE revoked_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token_id VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_revoked_tokens_token_id UNIQUE (token_id),
    INDEX idx_revoked_tokens_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- End Flyway migration V2__auth_and_device_enabled.sql

-- Begin Flyway migration V3__telemetry_evidence.sql
ALTER TABLE location_history
    ADD COLUMN altitude_meters DECIMAL(8, 2) NULL AFTER longitude;

ALTER TABLE sensor_readings
    ADD COLUMN reed_switch VARCHAR(20) NOT NULL DEFAULT 'CLOSED' AFTER magnitude,
    ADD COLUMN impact_detected BIT(1) NOT NULL DEFAULT 0 AFTER movement_status,
    ADD COLUMN unauthorized_opening BIT(1) NOT NULL DEFAULT 0 AFTER impact_detected;

CREATE UNIQUE INDEX uk_devices_token_hash ON devices (token_hash);
CREATE INDEX idx_location_history_device_time ON location_history (device_id, recorded_at);
CREATE INDEX idx_sensor_readings_device_time ON sensor_readings (device_id, recorded_at);
CREATE INDEX idx_camera_images_device_time ON camera_images (device_id, captured_at);
-- End Flyway migration V3__telemetry_evidence.sql

-- Begin Flyway migration V4__risk_alerts.sql
ALTER TABLE alerts
    ADD COLUMN read_at DATETIME(6) NULL AFTER status,
    ADD COLUMN dedupe_key VARCHAR(160) NULL AFTER investigation_notes;

CREATE UNIQUE INDEX uk_alerts_parcel_dedupe ON alerts (parcel_id, dedupe_key);
CREATE INDEX idx_alerts_read_at ON alerts (read_at);

INSERT INTO security_settings (setting_key, setting_value, description, created_at, updated_at) VALUES
    ('risk.weight.device_offline', '15', 'Points when an assigned device stays offline past the timeout', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('risk.vibration.repeat_count', '5', 'Vibration readings inside the correlation window required before vibration adds risk', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('risk.level.medium', '25', 'Minimum score for MEDIUM risk', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('risk.level.high', '50', 'Minimum score for HIGH risk', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('risk.level.critical', '75', 'Minimum score for CRITICAL risk', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('gps.anomaly.minimum_jump_meters', '100', 'Movement below this distance is treated as ordinary GPS fluctuation', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('gps.anomaly.maximum_plausible_speed_mps', '55', 'Implied speed above this, after the jump minimum, is a GPS anomaly', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
-- End Flyway migration V4__risk_alerts.sql

