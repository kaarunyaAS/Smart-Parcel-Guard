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
