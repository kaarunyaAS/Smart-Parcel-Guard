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
