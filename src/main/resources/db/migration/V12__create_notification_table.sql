-- Notification table for trip-related notifications
CREATE TABLE notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    recipient_id BIGINT NOT NULL,
    sender_id BIGINT,
    trip_id BIGINT,
    participant_public_id UUID,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_recipient FOREIGN KEY (recipient_id) REFERENCES app_user(id),
    CONSTRAINT fk_notif_sender FOREIGN KEY (sender_id) REFERENCES app_user(id),
    CONSTRAINT fk_notif_trip FOREIGN KEY (trip_id) REFERENCES trip(id)
);

CREATE INDEX idx_notif_recipient ON notification(recipient_id);
CREATE INDEX idx_notif_recipient_unread ON notification(recipient_id, is_read);
