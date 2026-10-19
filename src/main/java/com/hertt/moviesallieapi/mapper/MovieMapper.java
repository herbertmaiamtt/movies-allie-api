package com.hertt.moviesallieapi.mapper;

import com.hertt.moviesallieapi.dto.response.MovieDetailsResponse;
import com.hertt.moviesallieapi.dto.response.MovieSummaryResponse;
import com.hertt.moviesallieapi.dto.tmdb.TmdbMovieDetails;
import com.hertt.moviesallieapi.dto.tmdb.TmdbMovieResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MovieMapper {

    @Mapping(target = "id",          source = "result.id")
    @Mapping(target = "title",       source = "result.title")
    @Mapping(target = "overview",    source = "result.overview")
    @Mapping(target = "releaseDate", source = "result.releaseDate")
    @Mapping(target = "releaseYear", source = "releaseYear")
    @Mapping(target = "posterUrl",   source = "posterUrl")
    MovieSummaryResponse toSummaryResponse(TmdbMovieResult result, Integer releaseYear, String posterUrl);

    @Mapping(target = "id",          source = "details.id")
    @Mapping(target = "title",       source = "details.title")
    @Mapping(target = "overview",    source = "details.overview")
    @Mapping(target = "releaseDate", source = "details.releaseDate")
    @Mapping(target = "releaseYear", source = "releaseYear")
    @Mapping(target = "director",    source = "director")
    @Mapping(target = "mainActors",  source = "mainActors")
    @Mapping(target = "posterUrl",   source = "posterUrl")
    @Mapping(target = "runtime",     source = "details.runtime")
    MovieDetailsResponse toDetailsResponse(TmdbMovieDetails details,
                                          Integer releaseYear,
                                          String director,
                                          String mainActors,
                                          String posterUrl);

}
