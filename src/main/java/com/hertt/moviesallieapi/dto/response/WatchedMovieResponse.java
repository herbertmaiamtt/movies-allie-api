package com.hertt.moviesallieapi.dto.response;

import java.time.LocalDateTime;

public record WatchedMovieResponse(

    Long id,
    Long tmdbId,
    String title,
    Integer releaseYear,
    String director,
    String overview,
    String posterUrl,
    String mainActors,
    String comment,
    LocalDateTime watchedAt

) { }
