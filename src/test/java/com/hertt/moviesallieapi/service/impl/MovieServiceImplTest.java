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
import com.hertt.moviesallieapi.service.PosterUrlBuilder;
import com.hertt.moviesallieapi.service.client.TmdbClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Unidade - MovieServiceImpl")
class MovieServiceImplTest {

    @Mock
    private TmdbClient tmdbClient;

    @Mock
    private MovieMapper movieMapper;

    @Mock
    private PosterUrlBuilder posterUrlBuilder;

    @InjectMocks
    private MovieServiceImpl movieService;

    @Nested
    @DisplayName("Busca de filmes (search)")
    class SearchTests {

        @Test
        @DisplayName("Deve buscar filmes com sucesso e retornar MovieSearchResponse enriquecido")
        void shouldSearchMoviesSuccessfully() {
            String query = "Matrix";
            TmdbMovieResult movieResult = new TmdbMovieResult(
                    603L,
                    "The Matrix",
                    "Um programador descobre a realidade...",
                    "1999-03-30",
                    "/matrix.jpg"
            );
            TmdbSearchResponse tmdbResponse = new TmdbSearchResponse(
                    1,
                    List.of(movieResult),
                    1,
                    1
            );
            MovieSummaryResponse summaryResponse = new MovieSummaryResponse(
                    603L,
                    "The Matrix",
                    "Um programador descobre a realidade...",
                    "1999-03-30",
                    1999,
                    "https://image.tmdb.org/t/p/w500/matrix.jpg"
            );

            when(tmdbClient.searchMovies("Matrix")).thenReturn(tmdbResponse);
            when(posterUrlBuilder.build("/matrix.jpg")).thenReturn("https://image.tmdb.org/t/p/w500/matrix.jpg");
            when(movieMapper.toSummaryResponse(eq(movieResult), eq(1999), eq("https://image.tmdb.org/t/p/w500/matrix.jpg")))
                    .thenReturn(summaryResponse);

            MovieSearchResponse response = movieService.search(query);

            assertThat(response).isNotNull();
            assertThat(response.page()).isEqualTo(1);
            assertThat(response.totalPages()).isEqualTo(1);
            assertThat(response.totalResults()).isEqualTo(1);
            assertThat(response.results()).hasSize(1);
            assertThat(response.results().get(0)).isEqualTo(summaryResponse);

            verify(tmdbClient).searchMovies("Matrix");
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException quando query for nula ou em branco")
        void shouldThrowExceptionWhenQueryIsBlank() {
            assertThatThrownBy(() -> movieService.search(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("O termo de busca não pode ser vazio.");

            assertThatThrownBy(() -> movieService.search("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("O termo de busca não pode ser vazio.");
        }

        @Test
        @DisplayName("Deve retornar lista vazia se results for nulo na resposta do TMDB")
        void shouldHandleNullResultsFromTmdb() {
            TmdbSearchResponse tmdbResponse = new TmdbSearchResponse(1, null, 0, 0);
            when(tmdbClient.searchMovies("Inexistente")).thenReturn(tmdbResponse);

            MovieSearchResponse response = movieService.search("Inexistente");

            assertThat(response.results()).isEmpty();
            assertThat(response.totalResults()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Detalhes de filme (getDetails)")
    class GetDetailsTests {

        @Test
        @DisplayName("Deve retornar detalhes completos do filme com diretor e atores formatados")
        void shouldReturnMovieDetailsSuccessfully() {
            Long tmdbId = 603L;
            TmdbCredits credits = new TmdbCredits(
                    List.of(
                            new TmdbCastMember(1L, "Keanu Reeves", "Neo", 0),
                            new TmdbCastMember(2L, "Laurence Fishburne", "Morpheus", 1),
                            new TmdbCastMember(3L, "Carrie-Anne Moss", "Trinity", 2)
                    ),
                    List.of(
                            new TmdbCrewMember(10L, "Lana Wachowski", "Director", "Directing"),
                            new TmdbCrewMember(11L, "Joel Silver", "Producer", "Production")
                    )
            );
            TmdbMovieDetails details = new TmdbMovieDetails(
                    tmdbId,
                    "The Matrix",
                    "Sinopse da Matrix",
                    "1999-03-31",
                    "/matrix.jpg",
                    136,
                    credits
            );
            MovieDetailsResponse expectedResponse = new MovieDetailsResponse(
                    tmdbId,
                    "The Matrix",
                    "Sinopse da Matrix",
                    "1999-03-31",
                    1999,
                    "Lana Wachowski",
                    "Keanu Reeves, Laurence Fishburne, Carrie-Anne Moss",
                    "https://image.tmdb.org/t/p/w500/matrix.jpg",
                    136
            );

            when(tmdbClient.getMovieDetails(tmdbId)).thenReturn(details);
            when(posterUrlBuilder.build("/matrix.jpg")).thenReturn("https://image.tmdb.org/t/p/w500/matrix.jpg");
            when(movieMapper.toDetailsResponse(
                    eq(details),
                    eq(1999),
                    eq("Lana Wachowski"),
                    eq("Keanu Reeves, Laurence Fishburne, Carrie-Anne Moss"),
                    eq("https://image.tmdb.org/t/p/w500/matrix.jpg")
            )).thenReturn(expectedResponse);

            MovieDetailsResponse response = movieService.getDetails(tmdbId);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(tmdbId);
            assertThat(response.director()).isEqualTo("Lana Wachowski");
            assertThat(response.mainActors()).isEqualTo("Keanu Reeves, Laurence Fishburne, Carrie-Anne Moss");
            assertThat(response.releaseYear()).isEqualTo(1999);
            assertThat(response.posterUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/matrix.jpg");
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException quando tmdbId for nulo ou menor ou igual a zero")
        void shouldThrowExceptionWhenIdIsInvalid() {
            assertThatThrownBy(() -> movieService.getDetails(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("O ID do filme deve ser informado e positivo.");

            assertThatThrownBy(() -> movieService.getDetails(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("O ID do filme deve ser informado e positivo.");

            assertThatThrownBy(() -> movieService.getDetails(-5L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("O ID do filme deve ser informado e positivo.");
        }
    }
}
