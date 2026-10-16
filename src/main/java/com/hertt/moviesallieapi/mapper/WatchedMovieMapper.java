package com.hertt.moviesallieapi.mapper;

import com.hertt.moviesallieapi.dto.response.WatchedMovieResponse;
import com.hertt.moviesallieapi.entity.WatchedMovie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WatchedMovieMapper {

    @Mapping(target = "id",         source = "entity.id")
    @Mapping(target = "tmdbId",     source = "entity.tmdbId")
    @Mapping(target = "title",      source = "entity.title")
    @Mapping(target = "releaseYear",source = "entity.releaseYear")
    @Mapping(target = "director",   source = "entity.director")
    @Mapping(target = "overview",   source = "entity.overview")
    @Mapping(target = "posterUrl",  source = "posterUrl")
    @Mapping(target = "mainActors", source = "entity.mainActors")
    @Mapping(target = "comment",    source = "entity.comment")
    @Mapping(target = "watchedAt",  source = "entity.watchedAt")
    WatchedMovieResponse toResponse(WatchedMovie entity, String posterUrl);

}
