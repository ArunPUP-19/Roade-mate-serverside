-- V5: Reset H2 identity counters after seed data inserts.
-- Flyway V4 inserted rows with explicit IDs, so the auto-increment
-- sequences are out of sync. This bumps them past the seeded values.

ALTER TABLE app_user ALTER COLUMN id RESTART WITH 100;
ALTER TABLE trip ALTER COLUMN id RESTART WITH 100;
ALTER TABLE ride_request ALTER COLUMN id RESTART WITH 100;
