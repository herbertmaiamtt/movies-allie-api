-- ============================================================================
-- V2: Criação da tabela de filmes assistidos (watched_movies) e seus índices
-- Compatível com SQLite (dev) e PostgreSQL (prod) via placeholder ${id_type}
-- ============================================================================

CREATE TABLE watched_movies (
    id           ${id_type},
    tmdb_id      BIGINT       NOT NULL,
    title        VARCHAR(255) NOT NULL,
    release_year INTEGER,
    director     VARCHAR(150),
    overview     TEXT,
    poster_path  VARCHAR(250),
    main_actors  TEXT,
    comment      TEXT,
    watched_at   TIMESTAMP    NOT NULL,
    user_id      BIGINT       NOT NULL,

    CONSTRAINT fk_watched_movies_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_watched_movies_user_tmdb
        UNIQUE (user_id, tmdb_id)
);

CREATE INDEX idx_watched_movies_user
    ON watched_movies (user_id);

CREATE INDEX idx_watched_movies_user_watched_at
    ON watched_movies (user_id, watched_at);
