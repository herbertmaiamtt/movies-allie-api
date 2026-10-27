package com.hertt.moviesallieapi.migration;

import com.hertt.moviesallieapi.entity.User;
import com.hertt.moviesallieapi.entity.WatchedMovie;
import com.hertt.moviesallieapi.repository.UserRepository;
import com.hertt.moviesallieapi.repository.WatchedMovieRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("Testes de Integração - Migrações Flyway e Banco PostgreSQL com Testcontainers")
@EnabledIf(value = "isDockerAvailable", disabledReason = "Docker daemon não está acessível no ambiente local")
class PostgreSqlTestcontainersIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

    static {
        if (isDockerAvailable()) {
            POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("movies_allie_test")
                    .withUsername("test")
                    .withPassword("test");
            POSTGRES_CONTAINER.start();
        } else {
            POSTGRES_CONTAINER = null;
        }
    }

    static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (POSTGRES_CONTAINER != null && POSTGRES_CONTAINER.isRunning()) {
            registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
            registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
            registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
            registry.add("spring.flyway.enabled", () -> "true");
            registry.add("spring.flyway.placeholders.id_type", () -> "BIGSERIAL PRIMARY KEY");
        }
    }

    @Autowired
    private Flyway flyway;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WatchedMovieRepository watchedMovieRepository;

    @Test
    @DisplayName("Deve validar que as migrações V1 e V2 foram aplicadas com sucesso pelo Flyway")
    void shouldValidateFlywayMigrationsApplied() {
        assertThat(flyway.info().applied()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(flyway.info().applied()[0].getScript()).isEqualTo("V1__create_users_table.sql");
        assertThat(flyway.info().applied()[1].getScript()).isEqualTo("V2__create_watched_movies_table.sql");
    }

    @Test
    @DisplayName("Deve validar a constraint de unicidade composta uk_watched_movies_user_tmdb no PostgreSQL")
    void shouldEnforceCompositeUniqueConstraintOnWatchedMovies() {
        User user = User.builder()
                .username("postgres_user")
                .email("pg_user@test.com")
                .password("hash123")
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.saveAndFlush(user);

        WatchedMovie movie1 = WatchedMovie.builder()
                .tmdbId(603L)
                .title("The Matrix")
                .releaseYear(1999)
                .director("Lana Wachowski")
                .watchedAt(LocalDateTime.now())
                .user(user)
                .build();
        watchedMovieRepository.saveAndFlush(movie1);

        WatchedMovie duplicateMovie = WatchedMovie.builder()
                .tmdbId(603L)
                .title("The Matrix Duplicate")
                .releaseYear(1999)
                .director("Lana Wachowski")
                .watchedAt(LocalDateTime.now())
                .user(user)
                .build();

        assertThatThrownBy(() -> watchedMovieRepository.saveAndFlush(duplicateMovie))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Deve validar exclusão em cascata (ON DELETE CASCADE) de filmes ao remover usuário")
    void shouldCascadeDeleteWatchedMoviesWhenUserIsDeleted() {
        User user = User.builder()
                .username("cascade_user")
                .email("cascade@test.com")
                .password("hash123")
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.saveAndFlush(user);

        WatchedMovie movie = WatchedMovie.builder()
                .tmdbId(550L)
                .title("Fight Club")
                .releaseYear(1999)
                .director("David Fincher")
                .watchedAt(LocalDateTime.now())
                .user(user)
                .build();
        movie = watchedMovieRepository.saveAndFlush(movie);
        Long movieId = movie.getId();

        userRepository.delete(user);
        userRepository.flush();

        Optional<WatchedMovie> foundMovie = watchedMovieRepository.findById(movieId);
        assertThat(foundMovie).isEmpty();
    }
}
