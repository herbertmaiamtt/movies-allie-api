package com.hertt.moviesallieapi.service;

import com.hertt.moviesallieapi.dto.response.MovieDetailsResponse;
import com.hertt.moviesallieapi.dto.response.MovieSearchResponse;

public interface MovieService {

    MovieSearchResponse search(String query);

    MovieDetailsResponse getDetails(Long tmdbId);

}
