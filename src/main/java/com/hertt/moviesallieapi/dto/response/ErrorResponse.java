// file: src/main/java/com/hertt/moviesallieapi/dto/response/ErrorResponse.java
package com.hertt.moviesallieapi.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;


@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Corpo padronizado de erro devolvido pela API.")
public record ErrorResponse(

        @Schema(
                description = "Momento (UTC) em que o erro ocorreu.",
                example = "2025-01-15T14:30:00Z"
        )
        Instant timestamp,

        @Schema(description = "Código HTTP do erro.", example = "404")
        int status,

        @Schema(
                description = "Nome curto do motivo HTTP.",
                example = "Not Found"
        )
        String error,

        @Schema(
                description = "Mensagem legível descrevendo o erro.",
                example = "Filme assistido não encontrado: 42"
        )
        String message,

        @Schema(
                description = "Caminho da requisição que gerou o erro.",
                example = "/api/watched-movies/42"
        )
        String path,

        @Schema(
                description = "Detalhes por campo. Presente apenas em erros de validação (HTTP 400)."
        )
        List<FieldError> fieldErrors

) {

    @Schema(description = "Erro de validação associado a um campo.")
    public record FieldError(

            @Schema(description = "Nome do campo inválido.", example = "email")
            String field,

            @Schema(description = "Mensagem de erro do campo.", example = "O email deve ser válido.")
            String message

    ) { }

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }

    public static ErrorResponse of(int status,
                                   String error,
                                   String message,
                                   String path,
                                   List<FieldError> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, error, message, path, fieldErrors);
    }
}