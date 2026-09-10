-- V1: Create app_user table
CREATE TABLE app_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id       UUID NOT NULL DEFAULT RANDOM_UUID(),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',
    rating          DOUBLE NOT NULL DEFAULT 5.0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP,
    version         INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT uk_user_public_id UNIQUE (public_id)
);

CREATE INDEX idx_user_email ON app_user(email);
