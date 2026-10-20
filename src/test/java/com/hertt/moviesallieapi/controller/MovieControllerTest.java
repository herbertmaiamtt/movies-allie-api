package com.hertt.moviesallieapi.controller;

import com.hertt.moviesallieapi.dto.response.MovieDetailsResponse;
import com.hertt.moviesallieapi.dto.response.MovieSearchResponse;
import com.hertt.moviesallieapi.dto.response.MovieSummaryResponse;
import com.hertt.moviesallieapi.exception.TmdbErrorKind;
import com.hertt.moviesallieapi.exception.TmdbIntegrationException;
import com.hertt.moviesallieapi.service.MovieService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Testes de Integração - MovieController")
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovieService movieService;

    @Test
    @DisplayName("GET /api/movies/search - Deve retornar 200 com lista de filmes sem autenticação")
    void shouldReturn200OnSearchWithoutAuthentication() throws Exception {
        MovieSummaryResponse movie = new MovieSummaryResponse(
                603L,
                "The Matrix",
                "Visão geral do filme Matrix",
                "1999-03-30",
                1999,
                "https://image.tmdb.org/t/p/w500/matrix.jpg"
        );
        MovieSearchResponse response = new MovieSearchResponse(1, List.of(movie), 1, 1);

        when(movieService.search("Matrix")).thenReturn(response);

        mockMvc.perform(get("/api/movies/search")
                        .param("query", "Matrix")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.totalResults", is(1)))
                .andExpect(jsonPath("$.results", hasSize(1)))
                .andExpect(jsonPath("$.results[0].id", is(603)))
                .andExpect(jsonPath("$.results[0].title", is("The Matrix")))
                .andExpect(jsonPath("$.results[0].releaseYear", is(1999)))
                .andExpect(jsonPath("$.results[0].posterUrl", is("https://image.tmdb.org/t/p/w500/matrix.jpg")));
    }

    @Test
    @DisplayName("GET /api/movies/search - Deve retornar 400 quando query for vazia")
    void shouldReturn400WhenQueryIsBlank() throws Exception {
        when(movieService.search("   "))
                .thenThrow(new IllegalArgumentException("O termo de busca não pode ser vazio."));

        mockMvc.perform(get("/api/movies/search")
                        .param("query", "   ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("O termo de busca não pode ser vazio.")));
    }

    @Test
    @DisplayName("GET /api/movies/search - Deve retornar 400 quando parâmetro query estiver ausente")
    void shouldReturn400WhenQueryParamIsMissing() throws Exception {
        mockMvc.perform(get("/api/movies/search")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("O parâmetro obrigatório 'query' não foi informado.")));
    }

    @Test
    @DisplayName("GET /api/movies/{id} - Deve retornar 200 com detalhes do filme")
    void shouldReturn200OnGetMovieDetails() throws Exception {
        MovieDetailsResponse response = new MovieDetailsResponse(
                603L,
                "The Matrix",
                "Sinopse do filme Matrix",
                "1999-03-31",
                1999,
                "Lana Wachowski",
                "Keanu Reeves, Laurence Fishburne",
                "https://image.tmdb.org/t/p/w500/matrix.jpg",
                136
        );

        when(movieService.getDetails(603L)).thenReturn(response);

        mockMvc.perform(get("/api/movies/603")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(603)))
                .andExpect(jsonPath("$.title", is("The Matrix")))
                .andExpect(jsonPath("$.director", is("Lana Wachowski")))
                .andExpect(jsonPath("$.mainActors", is("Keanu Reeves, Laurence Fishburne")))
                .andExpect(jsonPath("$.releaseYear", is(1999)))
                .andExpect(jsonPath("$.posterUrl", is("https://image.tmdb.org/t/p/w500/matrix.jpg")))
                .andExpect(jsonPath("$.runtime", is(136)));
    }

    @Test
    @DisplayName("GET /api/movies/{id} - Deve retornar 404 quando filme não for encontrado no TMDB")
    void shouldReturn404WhenMovieNotFoundOnTmdb() throws Exception {
        when(movieService.getDetails(999999L))
                .thenThrow(new TmdbIntegrationException(
                        TmdbErrorKind.NOT_FOUND,
                        "Filme não encontrado no TMDB: 999999"
                ));

        mockMvc.perform(get("/api/movies/999999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Filme não encontrado no TMDB: 999999")));
    }

    @Test
    @DisplayName("GET /api/movies/{id} - Deve retornar 502 quando ocorrer falha na integração com o TMDB")
    void shouldReturn502OnUpstreamTmdbError() throws Exception {
        when(movieService.getDetails(123L))
                .thenThrow(new TmdbIntegrationException(
                        TmdbErrorKind.UPSTREAM_ERROR,
                        "O TMDB está indisponível no momento."
                ));

        mockMvc.perform(get("/api/movies/123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status", is(502)))
                .andExpect(jsonPath("$.message", is("O TMDB está indisponível no momento.")));
    }

    @Test
    @DisplayName("GET /api/movies/search - Deve retornar 429 quando limite de requisições do TMDB for atingido")
    void shouldReturn429WhenTmdbRateLimitExceeded() throws Exception {
        when(movieService.search("Matrix"))
                .thenThrow(new TmdbIntegrationException(
                        TmdbErrorKind.RATE_LIMITED,
                        "Limite de requisições ao TMDB excedido. Tente novamente em instantes."
                ));

        mockMvc.perform(get("/api/movies/search")
                        .param("query", "Matrix")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status", is(429)))
                .andExpect(jsonPath("$.message", is("Limite de requisições ao TMDB excedido. Tente novamente em instantes.")));
    }

    @Test
    @DisplayName("GET /api/movies/{id} - Deve retornar 400 quando id for texto não numérico")
    void shouldReturn400WhenIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/api/movies/invalido")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }
}
