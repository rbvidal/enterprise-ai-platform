-- V6__ingestion_sequence.sql
-- Add sequence number to ingestion jobs (idempotent)

CREATE SEQUENCE IF NOT EXISTS ingestion_job_seq START 1;

ALTER TABLE document_ingestion_jobs ADD COLUMN IF NOT EXISTS sequence_number INT;
