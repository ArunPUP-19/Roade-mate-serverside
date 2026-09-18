-- V13: Cancellation cooldown — tracks per-user, per-trip 30-minute re-request restriction
CREATE TABLE cancellation_cooldown (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    trip_id         BIGINT NOT NULL,
    cancelled_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cooldown_until  TIMESTAMP NOT NULL,
    CONSTRAINT fk_cooldown_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_cooldown_trip FOREIGN KEY (trip_id) REFERENCES trip(id)
);

CREATE INDEX idx_cooldown_user_trip ON cancellation_cooldown(user_id, trip_id);
CREATE INDEX idx_cooldown_until ON cancellation_cooldown(cooldown_until);
