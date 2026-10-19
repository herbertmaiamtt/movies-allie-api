package com.hertt.moviesallieapi.migration;

import com.hertt.moviesallieapi.entity.User;
import com.hertt.moviesallieapi.entity.WatchedMovie;
import com.hertt.moviesallieapi.repository.UserRepository;
import com.hertt.moviesallieapi.repository.WatchedMovieRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("Testes de Integração - Migrações Flyway e Constraints do Banco")
class FlywayMigrationIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WatchedMovieRepository watchedMovieRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void cleanUp() {
        watchedMovieRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve ter aplicado as migrações V1 e V2 com sucesso")
    void shouldApplyV1AndV2MigrationsSuccessfully() {
        MigrationInfo[] applied = flyway.info().applied();

        assertThat(applied).hasSize(2);
        assertThat(applied[0].getVersion().getVersion()).isEqualTo("1");
        assertThat(applied[0].getDescription()).isEqualTo("create users table");
        assertThat(applied[0].getState()).isEqualTo(MigrationState.SUCCESS);

        assertThat(applied[1].getVersion().getVersion()).isEqualTo("2");
        assertThat(applied[1].getDescription()).isEqualTo("create watched movies table");
        assertThat(applied[1].getState()).isEqualTo(MigrationState.SUCCESS);
    }

    @Test
    @DisplayName("Deve rejeitar email duplicado pela constraint uk_users_email")
    void shouldEnforceUniqueEmailConstraint() {
        User user1 = User.builder()
                .username("user_one")
                .email("duplicado@email.com")
                .password("hash123")
                .build();
        userRepository.saveAndFlush(user1);

        User user2 = User.builder()
                .username("user_two")
                .email("duplicado@email.com")
                .password("hash456")
                .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("users.email");
    }

    @Test
    @DisplayName("Deve rejeitar username duplicado pela constraint uk_users_username")
    void shouldEnforceUniqueUsernameConstraint() {
        User user1 = User.builder()
                .username("mesmo_username")
                .email("um@email.com")
                .password("hash123")
                .build();
        userRepository.saveAndFlush(user1);

        User user2 = User.builder()
                .username("mesmo_username")
                .email("dois@email.com")
                .password("hash456")
                .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("users.username");
    }

    @Test
    @DisplayName("Deve rejeitar o mesmo filme para o mesmo usuário pela constraint composta uk_watched_movies_user_tmdb")
    void shouldEnforceUniqueUserTmdbConstraint() {
        User user = userRepository.saveAndFlush(User.builder()
                .username("cinefilo")
                .email("cinefilo@email.com")
                .password("hash123")
                .build());

        WatchedMovie firstEntry = WatchedMovie.builder()
                .tmdbId(603L)
                .title("The Matrix")
                .releaseYear(1999)
                .user(user)
                .build();
        watchedMovieRepository.saveAndFlush(firstEntry);

        WatchedMovie duplicateEntry = WatchedMovie.builder()
                .tmdbId(603L)
                .title("The Matrix")
                .releaseYear(1999)
                .user(user)
                .build();

        assertThatThrownBy(() -> watchedMovieRepository.saveAndFlush(duplicateEntry))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("watched_movies.user_id, watched_movies.tmdb_id");
    }

    @Test
    @DisplayName("Deve permitir que dois usuários diferentes marquem o mesmo tmdb_id como assistido")
    void shouldAllowDifferentUsersToWatchSameMovie() {
        User user1 = userRepository.saveAndFlush(User.builder()
                .username("usuario1")
                .email("u1@email.com")
                .password("hash1")
                .build());

        User user2 = userRepository.saveAndFlush(User.builder()
                .username("usuario2")
                .email("u2@email.com")
                .password("hash2")
                .build());

        WatchedMovie movieUser1 = WatchedMovie.builder()
                .tmdbId(550L)
                .title("Fight Club")
                .user(user1)
                .build();

        WatchedMovie movieUser2 = WatchedMovie.builder()
                .tmdbId(550L)
                .title("Fight Club")
                .user(user2)
                .build();

        watchedMovieRepository.saveAndFlush(movieUser1);
        watchedMovieRepository.saveAndFlush(movieUser2);

        assertThat(watchedMovieRepository.countByUserId(user1.getId())).isEqualTo(1L);
        assertThat(watchedMovieRepository.countByUserId(user2.getId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("Deve remover os filmes assistidos em cascata no banco (ON DELETE CASCADE) ao deletar usuário")
    void shouldCascadeDeleteWatchedMoviesWhenUserIsDeletedInDatabase() {
        User user = userRepository.saveAndFlush(User.builder()
                .username("cascade_user")
                .email("cascade@email.com")
                .password("hash")
                .build());

        watchedMovieRepository.saveAndFlush(WatchedMovie.builder()
                .tmdbId(157336L)
                .title("Interstellar")
                .user(user)
                .build());

        assertThat(watchedMovieRepository.countByUserId(user.getId())).isEqualTo(1L);

        // Executa DELETE direto via SQL dentro de transação para validar o ON DELETE CASCADE do banco
        transactionTemplate.executeWithoutResult(
                status -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", user.getId())
        );

        assertThat(watchedMovieRepository.countByUserId(user.getId())).isZero();
    }

    @Test
    @DisplayName("Deve ter criado os índices idx_watched_movies_user e idx_watched_movies_user_watched_at")
    void shouldHaveCreatedExpectedIndexes() throws Exception {
        Set<String> indexNames = new HashSet<>();

        try (Connection connection = dataSource.getConnection();
             ResultSet rs = connection.getMetaData().getIndexInfo(null, null, "watched_movies", false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName != null) {
                    indexNames.add(indexName.toLowerCase());
                }
            }
        }

        assertThat(indexNames).contains(
                "idx_watched_movies_user",
                "idx_watched_movies_user_watched_at"
        );
    }
}
