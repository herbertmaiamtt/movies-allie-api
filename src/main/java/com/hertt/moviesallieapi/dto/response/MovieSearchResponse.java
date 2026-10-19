package com.hertt.moviesallieapi.dto.response;

import java.util.List;

public record MovieSearchResponse(

    Integer page,
    List<MovieSummaryResponse> results,
    Integer totalPages,
    Integer totalResults

) { }
