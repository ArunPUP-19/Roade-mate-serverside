-- V2: Create trip table
CREATE TABLE trip (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id           UUID NOT NULL DEFAULT RANDOM_UUID(),
    driver_id           BIGINT NOT NULL,
    driver_name         VARCHAR(100) NOT NULL,
    driver_rating       DOUBLE NOT NULL DEFAULT 5.0,
    starting_location   VARCHAR(500) NOT NULL,
    destination         VARCHAR(500) NOT NULL,
    trip_date           VARCHAR(20) NOT NULL,
    trip_time           VARCHAR(20) NOT NULL,
    seats_available     INTEGER NOT NULL,
    total_seats         INTEGER NOT NULL,
    estimated_cost      INTEGER NOT NULL DEFAULT 0,
    vehicle             VARCHAR(200),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP,
    version             INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_trip_public_id UNIQUE (public_id),
    CONSTRAINT fk_trip_driver FOREIGN KEY (driver_id) REFERENCES app_user(id)
);

CREATE INDEX idx_trip_driver_id ON trip(driver_id);
CREATE INDEX idx_trip_status ON trip(status, trip_date);
