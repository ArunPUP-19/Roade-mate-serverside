-- V4: Insert seed data (matching the existing Express.js in-memory data)

-- Seed users (password for all: password123)
INSERT INTO app_user (id, email, password_hash, display_name, role, rating) VALUES
(1, 'arun@routemate.com', '$2a$10$8OVMSOJUZyVX8F3tv9CMBeK0kbzll.N0smyNpAoDpeXnmTQHqHw4.', 'Arun Kumar', 'USER', 4.9),
(2, 'priya@routemate.com', '$2a$10$8OVMSOJUZyVX8F3tv9CMBeK0kbzll.N0smyNpAoDpeXnmTQHqHw4.', 'Priya Sharma', 'USER', 4.8),
(3, 'suresh@routemate.com', '$2a$10$8OVMSOJUZyVX8F3tv9CMBeK0kbzll.N0smyNpAoDpeXnmTQHqHw4.', 'Suresh V', 'USER', 4.7),
(4, 'default@routemate.com', '$2a$10$8OVMSOJUZyVX8F3tv9CMBeK0kbzll.N0smyNpAoDpeXnmTQHqHw4.', 'You (Driver)', 'USER', 5.0);

-- Seed trips (matching Express server's in-memory data)
INSERT INTO trip (id, driver_id, driver_name, driver_rating, starting_location, destination, trip_date, trip_time, seats_available, total_seats, estimated_cost, vehicle) VALUES
(1, 1, 'Arun Kumar', 4.9, 'Coimbatore, Tamil Nadu, India', 'Tiruppur, Tamil Nadu, India', '2026-08-14', '08:00', 3, 4, 200, 'Hyundai i20 (TN 37 AB 1234)'),
(2, 2, 'Priya Sharma', 4.8, 'Gandhipuram, Coimbatore', 'Tiruppur Bus Stand', '2026-08-14', '09:30', 2, 4, 150, 'Honda City (TN 38 XY 9876)');

-- Seed ride requests
INSERT INTO ride_request (id, passenger_id, passenger_name, passenger_rating, starting_location, destination, request_date, preferred_time) VALUES
(1, 3, 'Suresh V', 4.7, 'Singanallur, Coimbatore', 'Tiruppur', '2026-08-14', '08:30');
