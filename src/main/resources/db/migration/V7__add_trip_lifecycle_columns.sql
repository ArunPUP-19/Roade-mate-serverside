-- V7: Extend trip table for full lifecycle tracking
-- Add timestamps for lock/start/complete transitions + data lifecycle
ALTER TABLE trip ADD COLUMN locked_at TIMESTAMP;
ALTER TABLE trip ADD COLUMN started_at TIMESTAMP;
ALTER TABLE trip ADD COLUMN completed_at TIMESTAMP;
ALTER TABLE trip ADD COLUMN data_delete_at TIMESTAMP;
