// file: src/main/java/com/hertt/moviesallieapi/exception/GlobalExceptionHandler.java
package com.hertt.moviesallieapi.exception;

import com.hertt.moviesallieapi.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================================================================
    // 400 — Bad Request
    // ==================================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();

        log.warn("Falha de validação em {}: {} campo(s) inválido(s).",
                request.getRequestURI(), fieldErrors.size());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "Um ou mais campos estão inválidos.",
                        request.getRequestURI(),
                        fieldErrors
                )
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String expected = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "valor válido";

        String message = "O parâmetro '%s' deve ser do tipo %s."
                .formatted(ex.getName(), expected);

        log.warn("Tipo inválido em {}: {}", request.getRequestURI(), message);

        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        log.warn("Corpo da requisição ilegível em {}: {}",
                request.getRequestURI(), ex.getMessage());

        return build(
                HttpStatus.BAD_REQUEST,
                "O corpo da requisição está ausente ou malformado.",
                request
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        String message = "O parâmetro obrigatório '%s' não foi informado."
                .formatted(ex.getParameterName());

        log.warn("Parâmetro ausente em {}: {}", request.getRequestURI(), message);

        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    // ==================================================================
    // 401 — Unauthorized
    // ==================================================================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        log.warn("Tentativa de login falhou em {}: {}", request.getRequestURI(), ex.getMessage());

        return build(
                HttpStatus.UNAUTHORIZED,
                "Email ou senha inválidos.",
                request
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        log.warn("Falha de autenticação em {}: {}", request.getRequestURI(), ex.getMessage());

        return build(
                HttpStatus.UNAUTHORIZED,
                "Falha na autenticação.",
                request
        );
    }

    // ==================================================================
    // 404 — Not Found
    // ==================================================================

    @ExceptionHandler(WatchedMovieNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWatchedMovieNotFound(
            WatchedMovieNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Recurso não encontrado em {}: {}", request.getRequestURI(), ex.getMessage());

        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // ==================================================================
    // 409 — Conflict
    // ==================================================================

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        log.warn("Conflito de email em {}: {}", request.getRequestURI(), ex.getMessage());

        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUsernameAlreadyExists(
            UsernameAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        log.warn("Conflito de username em {}: {}", request.getRequestURI(), ex.getMessage());

        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(WatchedMovieAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleWatchedMovieAlreadyExists(
            WatchedMovieAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        log.warn("Conflito de filme assistido em {}: {}", request.getRequestURI(), ex.getMessage());

        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

// ==================================================================
// 502 — Bad Gateway (integração com o TMDB)
// ==================================================================

    @ExceptionHandler(TmdbIntegrationException.class)
    public ResponseEntity<ErrorResponse> handleTmdbIntegration(
            TmdbIntegrationException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = switch (ex.getKind()) {
            case NOT_FOUND      -> HttpStatus.NOT_FOUND;
            case UPSTREAM_ERROR -> HttpStatus.BAD_GATEWAY;
        };

        if (status.is4xxClientError()) {
            log.warn("TMDB retornou erro de cliente em {}: {}",
                    request.getRequestURI(), ex.getMessage());
        } else {
            log.error("Falha de integração com o TMDB em {}: {}",
                    request.getRequestURI(), ex.getMessage(), ex);
        }

        return build(status, ex.getMessage(), request);
    }

    // ==================================================================
    // 500 — Internal Server Error
    // ==================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Erro inesperado em {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tente novamente mais tarde.",
                request
        );
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private ResponseEntity<ErrorResponse> build(HttpStatus status,
                                                String message,
                                                HttpServletRequest request) {
        return ResponseEntity.status(status).body(
                ErrorResponse.of(
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        request.getRequestURI()
                )
        );
    }

    private ErrorResponse.FieldError toFieldError(FieldError fe) {
        return new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage());
    }
}