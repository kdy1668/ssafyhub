ALTER TABLE deployments
    MODIFY COLUMN backend_artifact_id BIGINT NULL,
    MODIFY COLUMN runtime_version VARCHAR(20) NULL,
    MODIFY COLUMN tech_stack VARCHAR(30) NULL,
    MODIFY COLUMN internal_port INT NULL;
