CREATE TABLE channel_credential
(
    id BIGINT PRIMARY KEY,
    channel_code       VARCHAR(64)    NOT NULL,
    channel_name       VARCHAR(128)   NOT NULL,
    secret_ciphertext  VARBINARY(512) NOT NULL,
    secret_iv          BINARY(12)     NOT NULL,
    encryption_key_id  VARCHAR(32)    NOT NULL,
    secret_version     BIGINT         NOT NULL,
    status             TINYINT        NOT NULL DEFAULT 1,
    last_rotated_at    DATETIME       NOT NULL,
    created_by_user_id BIGINT         NOT NULL,
    updated_by_user_id BIGINT         NOT NULL,
    deleted            TINYINT        NOT NULL DEFAULT 0,
    create_time        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_channel_credential_code (channel_code),
    KEY idx_channel_credential_status (status, deleted, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE channel_data_scope
(
    id BIGINT PRIMARY KEY,
    channel_id         BIGINT       NOT NULL,
    scope_type         VARCHAR(32)  NOT NULL,
    scope_value        VARCHAR(128) NOT NULL,
    status             TINYINT      NOT NULL DEFAULT 1,
    version            BIGINT       NOT NULL DEFAULT 1,
    created_by_user_id BIGINT       NOT NULL,
    updated_by_user_id BIGINT       NOT NULL,
    deleted            TINYINT      NOT NULL DEFAULT 0,
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_channel_data_scope (channel_id, scope_type, scope_value),
    KEY idx_channel_data_scope_query (channel_id, scope_type, status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
