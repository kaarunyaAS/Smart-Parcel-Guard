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
