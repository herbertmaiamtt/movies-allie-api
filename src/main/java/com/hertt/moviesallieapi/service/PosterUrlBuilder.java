package com.hertt.moviesallieapi.service;

import com.hertt.moviesallieapi.config.properties.TmdbProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PosterUrlBuilder {

    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/";
    private final TmdbProperties tmdbProperties;

    public String build(String posterPath){
        if(posterPath == null || posterPath.isBlank()) return null;

        return IMAGE_BASE_URL + tmdbProperties.posterSize() + posterPath;
    }

}
