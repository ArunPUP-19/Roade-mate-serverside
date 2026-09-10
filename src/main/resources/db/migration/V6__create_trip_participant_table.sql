-- V6: Create trip_participant table for many-to-many user<->trip relationship
-- Tracks who is on each trip with confirmation workflow
CREATE TABLE trip_participant (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id               UUID NOT NULL DEFAULT RANDOM_UUID(),
    trip_id                 BIGINT NOT NULL,
    user_id                 BIGINT NOT NULL,
    role                    VARCHAR(20) NOT NULL DEFAULT 'PASSENGER',
    confirmation_status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    confirmed_at            TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_participant_public_id UNIQUE (public_id),
    CONSTRAINT uk_trip_user UNIQUE (trip_id, user_id),
    CONSTRAINT fk_participant_trip FOREIGN KEY (trip_id) REFERENCES trip(id),
    CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE INDEX idx_participant_trip ON trip_participant(trip_id);
CREATE INDEX idx_participant_user ON trip_participant(user_id);
CREATE INDEX idx_participant_status ON trip_participant(confirmation_status);
