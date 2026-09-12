CREATE TABLE trip_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trip_message_trip FOREIGN KEY (trip_id) REFERENCES trip(id) ON DELETE CASCADE,
    CONSTRAINT fk_trip_message_sender FOREIGN KEY (sender_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX idx_trip_message_trip_id ON trip_message(trip_id);
