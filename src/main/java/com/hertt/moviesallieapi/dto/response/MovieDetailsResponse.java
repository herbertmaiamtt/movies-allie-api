package com.hertt.moviesallieapi.dto.response;

public record MovieDetailsResponse(

    Long id,
    String title,
    String overview,
    String releaseDate,
    Integer releaseYear,
    String director,
    String mainActors,
    String posterUrl,
    Integer runtime

) { }
