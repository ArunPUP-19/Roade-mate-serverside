-- V3: Create ride_request table
CREATE TABLE ride_request (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id           UUID NOT NULL DEFAULT RANDOM_UUID(),
    passenger_id        BIGINT NOT NULL,
    passenger_name      VARCHAR(100) NOT NULL,
    passenger_rating    DOUBLE NOT NULL DEFAULT 5.0,
    starting_location   VARCHAR(500) NOT NULL,
    destination         VARCHAR(500) NOT NULL,
    request_date        VARCHAR(20) NOT NULL,
    preferred_time      VARCHAR(20) DEFAULT 'Flexible',
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP,
    version             INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_request_public_id UNIQUE (public_id),
    CONSTRAINT fk_request_passenger FOREIGN KEY (passenger_id) REFERENCES app_user(id)
);

CREATE INDEX idx_request_passenger_id ON ride_request(passenger_id);
CREATE INDEX idx_request_status ON ride_request(status, request_date);
