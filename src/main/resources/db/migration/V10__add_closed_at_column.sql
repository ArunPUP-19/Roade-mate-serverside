-- V10: Add closed_at column for formal trip closure state
ALTER TABLE trip ADD COLUMN closed_at TIMESTAMP;
