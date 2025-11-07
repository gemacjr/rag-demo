-- Database migration for document metadata
-- This script creates the document_metadata table to track uploaded documents
-- Note: With spring.jpa.hibernate.ddl-auto=update, this will be created automatically
-- This script is provided for reference and manual execution if needed

CREATE TABLE IF NOT EXISTS document_metadata (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    chunk_count INTEGER NOT NULL,
    uploaded_at TIMESTAMP NOT NULL
);

-- Create index on filename for faster lookups
CREATE INDEX IF NOT EXISTS idx_document_metadata_filename ON document_metadata(filename);

-- Create index on uploaded_at for sorting
CREATE INDEX IF NOT EXISTS idx_document_metadata_uploaded_at ON document_metadata(uploaded_at DESC);
