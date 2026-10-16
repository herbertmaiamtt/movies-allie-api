package com.hertt.moviesallieapi.dto.tmdb;

import java.util.List;

public record TmdbCredits(

    List<TmdbCastMember> cast,
    List<TmdbCrewMember> crew

) { }
