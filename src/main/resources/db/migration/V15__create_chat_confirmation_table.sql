-- V15: Two-way chat confirmation table for the 3-step trip confirmation workflow
-- Tracks independent creator/requester accept actions in the trip chat
CREATE TABLE chat_confirmation (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id                 BIGINT NOT NULL,
    participant_id          BIGINT NOT NULL,
    creator_confirmed       BOOLEAN NOT NULL DEFAULT FALSE,
    creator_confirmed_at    TIMESTAMP,
    requester_confirmed     BOOLEAN NOT NULL DEFAULT FALSE,
    requester_confirmed_at  TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_chat_confirm_participant UNIQUE (trip_id, participant_id),
    CONSTRAINT fk_chat_confirm_trip FOREIGN KEY (trip_id) REFERENCES trip(id),
    CONSTRAINT fk_chat_confirm_participant FOREIGN KEY (participant_id) REFERENCES trip_participant(id)
);

CREATE INDEX idx_chat_confirm_trip ON chat_confirmation(trip_id);

-- Migrate existing CONFIRMED participants to FULLY_CONFIRMED
UPDATE trip_participant SET confirmation_status = 'FULLY_CONFIRMED'
    WHERE confirmation_status = 'CONFIRMED' AND role = 'PASSENGER';
