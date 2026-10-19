-- ============================================================================
-- V1: Criação da tabela de usuários (users)
-- Compatível com SQLite (dev) e PostgreSQL (prod) via placeholder ${id_type}
-- ============================================================================

CREATE TABLE users (
    id          ${id_type},
    username    VARCHAR(50)  NOT NULL,
    email       VARCHAR(100) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP,

    CONSTRAINT uk_users_email    UNIQUE (email),
    CONSTRAINT uk_users_username UNIQUE (username)
);
