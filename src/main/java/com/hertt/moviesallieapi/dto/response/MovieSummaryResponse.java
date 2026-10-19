package com.hertt.moviesallieapi.dto.response;

public record MovieSummaryResponse(

    Long id,
    String title,
    String overview,
    String releaseDate,
    Integer releaseYear,
    String posterUrl

) { }
