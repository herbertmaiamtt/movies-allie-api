package com.hertt.moviesallieapi.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AddWatchedMovieRequest(

    @NotNull(message = "O ID do filme no TMDB é obrigatório.")
    @Positive(message = "O ID do filme no TMDB deve ser positivo.")
    Long tmdbId,

    @Size(max = 2000, message = "O comentário deve ter no máximo 2000 caracteres.")
    String comment

) { }
