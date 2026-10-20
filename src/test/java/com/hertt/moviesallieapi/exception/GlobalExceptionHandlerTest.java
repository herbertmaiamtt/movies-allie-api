package com.hertt.moviesallieapi.exception;

import com.hertt.moviesallieapi.dto.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.jpa.JpaSystemException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes de Unidade - GlobalExceptionHandler (Concorrência 409 e TMDB 429)")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("POST", "/api/watched-movies");
    }

    @Test
    @DisplayName("Deve traduzir DataIntegrityViolationException de uk_watched_movies_user_tmdb para HTTP 409 Conflict")
    void shouldTranslateWatchedMovieUniqueConstraintTo409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uk_watched_movies_user_tmdb\""
        );

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo("Filme já marcado como assistido.");
        assertThat(response.getBody().path()).isEqualTo("/api/watched-movies");
    }

    @Test
    @DisplayName("Deve traduzir DataIntegrityViolationException de uk_users_email para HTTP 409 Conflict")
    void shouldTranslateEmailUniqueConstraintTo409() {
        MockHttpServletRequest registerRequest = new MockHttpServletRequest("POST", "/api/auth/register");
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uk_users_email\""
        );

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, registerRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo("Este email já está cadastrado.");
    }

    @Test
    @DisplayName("Deve traduzir DataIntegrityViolationException de uk_users_username para HTTP 409 Conflict")
    void shouldTranslateUsernameUniqueConstraintTo409() {
        MockHttpServletRequest registerRequest = new MockHttpServletRequest("POST", "/api/auth/register");
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uk_users_username\""
        );

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, registerRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo("Este username já está em uso.");
    }

    @Test
    @DisplayName("Deve traduzir JpaSystemException do SQLite com SQLITE_CONSTRAINT_UNIQUE para HTTP 409 Conflict")
    void shouldTranslateSqliteUniqueJpaSystemExceptionTo409() {
        JpaSystemException ex = new JpaSystemException(
                new RuntimeException("[SQLITE_CONSTRAINT_UNIQUE] A UNIQUE constraint failed (UNIQUE constraint failed: watched_movies.user_id, watched_movies.tmdb_id)")
        );

        ResponseEntity<ErrorResponse> response = handler.handleJpaSystemException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo("Filme já marcado como assistido.");
    }

    @Test
    @DisplayName("Deve traduzir TmdbIntegrationException com RATE_LIMITED para HTTP 429 Too Many Requests")
    void shouldTranslateTmdbRateLimitedTo429() {
        MockHttpServletRequest searchRequest = new MockHttpServletRequest("GET", "/api/movies/search");
        TmdbIntegrationException ex = new TmdbIntegrationException(
                TmdbErrorKind.RATE_LIMITED,
                "Limite de requisições ao TMDB excedido. Tente novamente em instantes."
        );

        ResponseEntity<ErrorResponse> response = handler.handleTmdbIntegration(ex, searchRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(429);
        assertThat(response.getBody().message()).isEqualTo("Limite de requisições ao TMDB excedido. Tente novamente em instantes.");
    }
}
