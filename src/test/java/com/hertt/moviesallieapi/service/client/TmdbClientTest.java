package com.hertt.moviesallieapi.service.client;

import com.hertt.moviesallieapi.dto.tmdb.TmdbCastMember;
import com.hertt.moviesallieapi.dto.tmdb.TmdbCredits;
import com.hertt.moviesallieapi.dto.tmdb.TmdbCrewMember;
import com.hertt.moviesallieapi.dto.tmdb.TmdbMovieDetails;
import com.hertt.moviesallieapi.dto.tmdb.TmdbMovieResult;
import com.hertt.moviesallieapi.dto.tmdb.TmdbSearchResponse;
import com.hertt.moviesallieapi.exception.TmdbErrorKind;
import com.hertt.moviesallieapi.exception.TmdbIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - TmdbClient")
class TmdbClientTest {

    @Mock
    private RestClient tmdbRestClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private TmdbClient tmdbClient;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        lenient().when(tmdbRestClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
    }

    @Nested
    @DisplayName("getMovieDetails")
    class GetMovieDetailsTests {

        @Test
        @DisplayName("Deve retornar detalhes do filme com sucesso quando TMDB responder 200")
        void shouldReturnMovieDetailsSuccessfully() {
            TmdbCredits credits = new TmdbCredits(
                    List.of(new TmdbCastMember(1L, "Keanu Reeves", "Neo", 0)),
                    List.of(new TmdbCrewMember(2L, "Lana Wachowski", "Director", "Directing"))
            );
            TmdbMovieDetails expectedDetails = new TmdbMovieDetails(
                    603L,
                    "The Matrix",
                    "Visão geral do Matrix",
                    "1999-03-31",
                    "/matrix.jpg",
                    136,
                    credits
            );

            when(responseSpec.body(TmdbMovieDetails.class)).thenReturn(expectedDetails);

            TmdbMovieDetails result = tmdbClient.getMovieDetails(603L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(603L);
            assertThat(result.title()).isEqualTo("The Matrix");
            assertThat(result.runtime()).isEqualTo(136);
            assertThat(result.credits().cast()).hasSize(1);
            assertThat(result.credits().crew()).hasSize(1);
            verify(responseSpec).body(TmdbMovieDetails.class);
        }

        @Test
        @DisplayName("Deve lançar TmdbIntegrationException com UPSTREAM_ERROR quando TMDB retornar corpo nulo")
        void shouldThrowUpstreamErrorWhenBodyIsNull() {
            when(responseSpec.body(TmdbMovieDetails.class)).thenReturn(null);

            assertThatThrownBy(() -> tmdbClient.getMovieDetails(603L))
                    .isInstanceOf(TmdbIntegrationException.class)
                    .satisfies(ex -> {
                        TmdbIntegrationException tie = (TmdbIntegrationException) ex;
                        assertThat(tie.getKind()).isEqualTo(TmdbErrorKind.UPSTREAM_ERROR);
                        assertThat(tie.getMessage()).isEqualTo("O TMDB devolveu uma resposta vazia.");
                    });
        }

        @Test
        @DisplayName("Deve lançar TmdbIntegrationException com NOT_FOUND quando TMDB responder 404")
        void shouldThrowNotFoundWhenTmdbReturns404() {
            HttpClientErrorException notFoundException = HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND,
                    "Not Found",
                    HttpHeaders.EMPTY,
                    new byte[0],
                    StandardCharsets.UTF_8
            );
            when(responseSpec.body(TmdbMovieDetails.class)).thenThrow(notFoundException);

            assertThatThrownBy(() -> tmdbClient.getMovieDetails(999999L))
                    .isInstanceOf(TmdbIntegrationException.class)
                    .satisfies(ex -> {
                        TmdbIntegrationException tie = (TmdbIntegrationException) ex;
                        assertThat(tie.getKind()).isEqualTo(TmdbErrorKind.NOT_FOUND);
                        assertThat(tie.getMessage()).isEqualTo("Filme não encontrado no TMDB: 999999");
                    });
        }

        @Test
        @DisplayName("Deve lançar TmdbIntegrationException com RATE_LIMITED quando TMDB responder 429")
        void shouldThrowRateLimitedWhenTmdbReturns429() {
            HttpClientErrorException tooManyRequests = HttpClientErrorException.create(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too Many Requests",
                    HttpHeaders.EMPTY,
                    new byte[0],
                    StandardCharsets.UTF_8
            );
            when(responseSpec.body(TmdbMovieDetails.class)).thenThrow(tooManyRequests);

            assertThatThrownBy(() -> tmdbClient.getMovieDetails(603L))
                    .isInstanceOf(TmdbIntegrationException.class)
                    .satisfies(ex -> {
                        TmdbIntegrationException tie = (TmdbIntegrationException) ex;
                        assertThat(tie.getKind()).isEqualTo(TmdbErrorKind.RATE_LIMITED);
                        assertThat(tie.getMessage()).isEqualTo("Limite de requisições ao TMDB excedido. Tente novamente em instantes.");
                    });
        }

        @Test
        @DisplayName("Deve lançar TmdbIntegrationException com UPSTREAM_ERROR para outros erros 4xx (ex: 401)")
        void shouldThrowUpstreamErrorWhenTmdbReturnsOther4xx() {
            HttpClientErrorException unauthorized = HttpClientErrorException.create(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    HttpHeaders.EMPTY,
                    new byte[0],
                    StandardCharsets.UTF_8
            );
            when(responseSpec.body(TmdbMovieDetails.class)).thenThrow(unauthorized);

            assertThatThrownBy(() -> tmdbClient.getMovieDetails(603L))
                    .isInstanceOf(TmdbIntegrationException.class)
                    .satisfies(ex -> {
                        TmdbIntegrationException tie = (TmdbIntegrationException) ex;
                        assertThat(tie.getKind()).isEqualTo(TmdbErrorKind.UPSTREAM_ERROR);
                        assertThat(tie.getMessage()).contains("O TMDB rejeitou a requisição (HTTP 401).");
                    });
        }

        @Test
        @DisplayName("Deve lançar TmdbIntegrationException com UPSTREAM_ERROR quando TMDB responder 5xx")
        void shouldThrowUpstreamErrorWhenTmdbReturns5xx() {
            HttpServerErrorException serverError = HttpServerErrorException.create(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error",
                    HttpHeaders.EMPTY,
                    new byte[0],
                    StandardCharsets.UTF_8
            );
            when(responseSpec.body(TmdbMovieDetails.class)).thenThrow(serverError);

            assertThatThrownBy(() -> tmdbClient.getMovieDetails(603L))
                    .isInstanceOf(TmdbIntegrationException.class)
                    .satisfies(ex -> {
                        TmdbIntegrationException tie = (TmdbIntegrationException) ex;
                        assertThat(tie.getKind()).isEqualTo(TmdbErrorKind.UPSTREAM_ERROR);
                        assertThat(tie.getMessage()).isEqualTo("O TMDB está indisponível no momento.");
                    });
        }

        @Test
        @DisplayName("Deve lançar TmdbIntegrationException com UPSTREAM_ERROR em caso de timeout de rede (ResourceAccessException)")
        void shouldThrowUpstreamErrorWhenResourceAccessExceptionOccurs() {
            ResourceAccessException timeoutException = new ResourceAccessException(
                    "Connection timed out",
                    new SocketTimeoutException("timeout")
            );
            when(responseSpec.body(TmdbMovieDetails.class)).thenThrow(timeoutException);

            assertThatThrownBy(() -> tmdbClient.getMovieDetails(603L))
                    .isInstanceOf(TmdbIntegrationException.class)
                    .satisfies(ex -> {
                        TmdbIntegrationException tie = (TmdbIntegrationException) ex;
                        assertThat(tie.getKind()).isEqualTo(TmdbErrorKind.UPSTREAM_ERROR);
                        assertThat(tie.getMessage()).isEqualTo("Não foi possível contatar o TMDB.");
                    });
        }

        @Test
        @DisplayName("Deve lançar TmdbIntegrationException com UPSTREAM_ERROR em caso de erro genérico do RestClient (RestClientException)")
        void shouldThrowUpstreamErrorWhenGenericRestClientExceptionOccurs() {
            RestClientException restClientException = new RestClientException("Deserialization failure");
            when(responseSpec.body(TmdbMovieDetails.class)).thenThrow(restClientException);

            assertThatThrownBy(() -> tmdbClient.getMovieDetails(603L))
                    .isInstanceOf(TmdbIntegrationException.class)
                    .satisfies(ex -> {
                        TmdbIntegrationException tie = (TmdbIntegrationException) ex;
                        assertThat(tie.getKind()).isEqualTo(TmdbErrorKind.UPSTREAM_ERROR);
                        assertThat(tie.getMessage()).isEqualTo("Erro inesperado na comunicação com o TMDB.");
                    });
        }
    }

    @Nested
    @DisplayName("searchMovies")
    class SearchMoviesTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Deve lançar IllegalArgumentException quando o termo de busca for nulo ou em branco")
        void shouldThrowIllegalArgumentExceptionWhenQueryIsBlank(String invalidQuery) {
            assertThatThrownBy(() -> tmdbClient.searchMovies(invalidQuery))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("O termo de busca não pode ser vazio.");
        }

        @Test
        @DisplayName("Deve retornar resultados da busca com sucesso")
        void shouldReturnSearchResultsSuccessfully() {
            TmdbMovieResult movieResult = new TmdbMovieResult(
                    603L,
                    "The Matrix",
                    "Visão geral do Matrix",
                    "1999-03-31",
                    "/matrix.jpg"
            );
            TmdbSearchResponse searchResponse = new TmdbSearchResponse(1, List.of(movieResult), 1, 1);

            when(responseSpec.body(TmdbSearchResponse.class)).thenReturn(searchResponse);

            TmdbSearchResponse result = tmdbClient.searchMovies("Matrix");

            assertThat(result).isNotNull();
            assertThat(result.page()).isEqualTo(1);
            assertThat(result.results()).hasSize(1);
            assertThat(result.results().get(0).title()).isEqualTo("The Matrix");
        }

        @Test
        @DisplayName("Deve lançar TmdbIntegrationException com NOT_FOUND quando busca responder 404")
        void shouldThrowNotFoundOnSearch404() {
            HttpClientErrorException notFoundException = HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND,
                    "Not Found",
                    HttpHeaders.EMPTY,
                    new byte[0],
                    StandardCharsets.UTF_8
            );
            when(responseSpec.body(TmdbSearchResponse.class)).thenThrow(notFoundException);

            assertThatThrownBy(() -> tmdbClient.searchMovies("Inexistente"))
                    .isInstanceOf(TmdbIntegrationException.class)
                    .satisfies(ex -> {
                        TmdbIntegrationException tie = (TmdbIntegrationException) ex;
                        assertThat(tie.getKind()).isEqualTo(TmdbErrorKind.NOT_FOUND);
                        assertThat(tie.getMessage()).isEqualTo("Recurso não encontrado no TMDB para a busca: Inexistente");
                    });
        }
    }
}
