CREATE TABLE IF NOT EXISTS messages
(
    id   VARCHAR(36) PRIMARY KEY NOT NULL,
    text VARCHAR(255)            NOT NULL
);
CREATE TABLE IF NOT EXISTS orchestrations
(
    id                 VARCHAR(36) PRIMARY KEY NOT NULL,
    name               VARCHAR(255)            NOT NULL,
    input              VARCHAR(6000)           NOT NULL,
    state              VARCHAR(255)            NOT NULL,
    result             VARCHAR(6000)           NULL,
    creation_timestamp TIMESTAMP               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_timestamp  TIMESTAMP               NULL
);
CREATE TABLE IF NOT EXISTS orchestration_steps
(
    id                 VARCHAR(36) PRIMARY KEY NOT NULL,
    orchestration_id   VARCHAR(36)             NOT NULL,
    name               VARCHAR(255)            NOT NULL,
    state              VARCHAR(255)            NOT NULL,
    result             VARCHAR(6000)           NOT NULL,
    creation_timestamp TIMESTAMP               NOT NULL DEFAULT CURRENT_TIMESTAMP
);
