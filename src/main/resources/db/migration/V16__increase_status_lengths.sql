-- V16: Increase lengths of status columns to accommodate longer enum values
ALTER TABLE trip_participant ALTER COLUMN confirmation_status VARCHAR(50);
ALTER TABLE trip ALTER COLUMN status VARCHAR(50);
