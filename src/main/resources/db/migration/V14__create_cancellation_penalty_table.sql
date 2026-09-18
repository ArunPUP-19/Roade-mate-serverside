-- V14: Cancellation penalty table + user account lock columns
CREATE TABLE cancellation_penalty (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id           UUID NOT NULL DEFAULT RANDOM_UUID(),
    trip_id             BIGINT NOT NULL,
    penalized_user_id   BIGINT NOT NULL,
    other_party_id      BIGINT NOT NULL,
    penalty_amount      INTEGER NOT NULL,
    split_amount        INTEGER NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    cancelled_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at         TIMESTAMP,
    paid_at             TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_penalty_public_id UNIQUE (public_id),
    CONSTRAINT fk_penalty_trip FOREIGN KEY (trip_id) REFERENCES trip(id),
    CONSTRAINT fk_penalty_user FOREIGN KEY (penalized_user_id) REFERENCES app_user(id),
    CONSTRAINT fk_penalty_other FOREIGN KEY (other_party_id) REFERENCES app_user(id)
);

CREATE INDEX idx_penalty_user ON cancellation_penalty(penalized_user_id, status);

-- Add account lock columns to user table
ALTER TABLE app_user ADD COLUMN penalty_locked BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE app_user ADD COLUMN penalty_locked_at TIMESTAMP;
