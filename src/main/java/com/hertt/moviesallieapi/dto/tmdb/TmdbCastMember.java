package com.hertt.moviesallieapi.dto.tmdb;

public record TmdbCastMember(

    Long id,
    String name,
    String character,
    Integer order

) { }
