-- Enable Write-Ahead Logging for PostgreSQL
-- This is required for Change Data Capture (CDC) with Debezium
-- WAL allows tracking of all database changes for replication and streaming

ALTER SYSTEM SET wal_level = logical;
ALTER SYSTEM SET max_replication_slots = 4;
ALTER SYSTEM SET max_wal_senders = 4;

-- Note: Restart PostgreSQL after this migration for changes to take effect
-- In Docker: docker-compose restart postgres
