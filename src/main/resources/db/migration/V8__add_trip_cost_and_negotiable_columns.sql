-- V8: Add cost breakdown and negotiable columns to trip table
ALTER TABLE trip ADD COLUMN total_cost INTEGER;
ALTER TABLE trip ADD COLUMN your_split INTEGER;
ALTER TABLE trip ADD COLUMN negotiable BOOLEAN DEFAULT FALSE;
ALTER TABLE trip ADD COLUMN vehicle_details VARCHAR(500);
