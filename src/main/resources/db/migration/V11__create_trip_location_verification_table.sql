-- V11: Create trip_location_verification table for location-based trip completion
CREATE TABLE trip_location_verification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    verified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loc_trip FOREIGN KEY (trip_id) REFERENCES trip(id) ON DELETE CASCADE,
    CONSTRAINT fk_loc_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT uk_loc_trip_user UNIQUE (trip_id, user_id)
);

CREATE INDEX idx_loc_trip ON trip_location_verification(trip_id);
