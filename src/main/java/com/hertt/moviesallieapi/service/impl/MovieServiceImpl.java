package com.hertt.moviesallieapi.service.impl;

import com.hertt.moviesallieapi.dto.response.MovieDetailsResponse;
import com.hertt.moviesallieapi.dto.response.MovieSearchResponse;
import com.hertt.moviesallieapi.dto.response.MovieSummaryResponse;
import com.hertt.moviesallieapi.dto.tmdb.TmdbCastMember;
import com.hertt.moviesallieapi.dto.tmdb.TmdbCredits;
import com.hertt.moviesallieapi.dto.tmdb.TmdbCrewMember;
import com.hertt.moviesallieapi.dto.tmdb.TmdbMovieDetails;
import com.hertt.moviesallieapi.dto.tmdb.TmdbMovieResult;
import com.hertt.moviesallieapi.dto.tmdb.TmdbSearchResponse;
import com.hertt.moviesallieapi.mapper.MovieMapper;
import com.hertt.moviesallieapi.service.MovieService;
import com.hertt.moviesallieapi.service.PosterUrlBuilder;
import com.hertt.moviesallieapi.service.client.TmdbClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private static final int MAIN_ACTORS_LIMIT = 5;
    private static final String DIRECTOR_JOB = "Director";

    private final TmdbClient tmdbClient;
    private final MovieMapper movieMapper;
    private final PosterUrlBuilder posterUrlBuilder;

    @Override
    public MovieSearchResponse search(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("O termo de busca não pode ser vazio.");
        }

        TmdbSearchResponse tmdbResponse = tmdbClient.searchMovies(query.trim());

        List<MovieSummaryResponse> items = tmdbResponse.results() == null
                ? Collections.emptyList()
                : tmdbResponse.results().stream()
                .map(this::toSummaryResponse)
                .toList();

        return new MovieSearchResponse(
                tmdbResponse.page(),
                items,
                tmdbResponse.totalPages(),
                tmdbResponse.totalResults()
        );
    }

    @Override
    public MovieDetailsResponse getDetails(Long tmdbId) {
        if (tmdbId == null || tmdbId <= 0) {
            throw new IllegalArgumentException("O ID do filme deve ser informado e positivo.");
        }

        TmdbMovieDetails details = tmdbClient.getMovieDetails(tmdbId);

        Integer releaseYear = extractReleaseYear(details.releaseDate());
        String director = extractDirector(details.credits());
        String mainActors = extractMainActors(details.credits());
        String posterUrl = posterUrlBuilder.build(details.posterPath());

        return movieMapper.toDetailsResponse(
                details,
                releaseYear,
                director,
                mainActors,
                posterUrl
        );
    }

    private MovieSummaryResponse toSummaryResponse(TmdbMovieResult result) {
        Integer releaseYear = extractReleaseYear(result.releaseDate());
        String posterUrl = posterUrlBuilder.build(result.posterPath());

        return movieMapper.toSummaryResponse(result, releaseYear, posterUrl);
    }

    private Integer extractReleaseYear(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) {
            return null;
        }

        try {
            return Integer.parseInt(releaseDate.substring(0, 4));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String extractDirector(TmdbCredits credits) {
        if (credits == null || credits.crew() == null) {
            return null;
        }

        return credits.crew().stream()
                .filter(member -> DIRECTOR_JOB.equalsIgnoreCase(member.job()))
                .map(TmdbCrewMember::name)
                .findFirst()
                .orElse(null);
    }

    private String extractMainActors(TmdbCredits credits) {
        if (credits == null || credits.cast() == null) {
            return null;
        }

        return credits.cast().stream()
                .sorted(Comparator.comparing(
                        TmdbCastMember::order,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAIN_ACTORS_LIMIT)
                .map(TmdbCastMember::name)
                .collect(Collectors.joining(", "));
    }
}
