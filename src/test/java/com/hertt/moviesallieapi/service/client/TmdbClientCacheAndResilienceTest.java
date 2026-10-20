package com.hertt.moviesallieapi.service.client;

import com.hertt.moviesallieapi.dto.tmdb.TmdbMovieDetails;
import com.hertt.moviesallieapi.dto.tmdb.TmdbMovieResult;
import com.hertt.moviesallieapi.dto.tmdb.TmdbSearchResponse;
import com.hertt.moviesallieapi.exception.TmdbErrorKind;
import com.hertt.moviesallieapi.exception.TmdbIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@DisplayName("Testes de Integração - TmdbClient (Cache em Memória e Resiliência 429/Timeout)")
class TmdbClientCacheAndResilienceTest {

    @Autowired
    private TmdbClient tmdbClient;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private RestClient tmdbRestClient;

    private RestClient.RequestHeadersUriSpec<?> uriSpec;
    private RestClient.RequestHeadersSpec<?> headersSpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });

        uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        headersSpec = mock(RestClient.RequestHeadersSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(tmdbRestClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) uriSpec);
        when(uriSpec.uri(any(Function.class))).thenReturn((RestClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("Deve armazenar em cache o resultado de searchMovies na região 'tmdb-search'")
    void shouldCacheSearchMoviesResult() {
        TmdbSearchResponse expected = new TmdbSearchResponse(
                1,
                List.of(new TmdbMovieResult(603L, "The Matrix", "Sinopse", "1999-03-30", "/matrix.jpg")),
                1,
                1
        );
        when(responseSpec.body(TmdbSearchResponse.class)).thenReturn(expected);

        TmdbSearchResponse firstCall = tmdbClient.searchMovies("Matrix");
        TmdbSearchResponse secondCall = tmdbClient.searchMovies("Matrix");

        assertThat(firstCall).isEqualTo(expected);
        assertThat(secondCall).isEqualTo(expected);

        // O RestClient deve ter sido chamado apenas na primeira vez; a segunda vem do cache
        verify(tmdbRestClient, times(1)).get();

        Cache searchCache = cacheManager.getCache("tmdb-search");
        assertThat(searchCache).isNotNull();
        assertThat(searchCache.get("Matrix", TmdbSearchResponse.class)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Deve armazenar em cache o resultado de getMovieDetails na região 'tmdb-movie'")
    void shouldCacheGetMovieDetailsResult() {
        TmdbMovieDetails expected = new TmdbMovieDetails(
                603L,
                "The Matrix",
                "Sinopse",
                "1999-03-30",
                "/matrix.jpg",
                136,
                null
        );
        when(responseSpec.body(TmdbMovieDetails.class)).thenReturn(expected);

        TmdbMovieDetails firstCall = tmdbClient.getMovieDetails(603L);
        TmdbMovieDetails secondCall = tmdbClient.getMovieDetails(603L);

        assertThat(firstCall).isEqualTo(expected);
        assertThat(secondCall).isEqualTo(expected);

        // O RestClient deve ter sido chamado apenas na primeira vez; a segunda vem do cache
        verify(tmdbRestClient, times(1)).get();

        Cache movieCache = cacheManager.getCache("tmdb-movie");
        assertThat(movieCache).isNotNull();
        assertThat(movieCache.get(603L, TmdbMovieDetails.class)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Deve traduzir HTTP 429 TooManyRequests para TmdbIntegrationException com RATE_LIMITED")
    void shouldTranslateHttp429ToRateLimitedException() {
        HttpClientErrorException tooManyRequests = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );
        when(responseSpec.body(TmdbSearchResponse.class)).thenThrow(tooManyRequests);

        assertThatThrownBy(() -> tmdbClient.searchMovies("Avatar"))
                .isInstanceOf(TmdbIntegrationException.class)
                .satisfies(ex -> {
                    TmdbIntegrationException tmdbEx = (TmdbIntegrationException) ex;
                    assertThat(tmdbEx.getKind()).isEqualTo(TmdbErrorKind.RATE_LIMITED);
                    assertThat(tmdbEx.getMessage()).isEqualTo(
                            "Limite de requisições ao TMDB excedido. Tente novamente em instantes."
                    );
                });
    }

    @Test
    @DisplayName("Deve traduzir ResourceAccessException (timeout/rede) para TmdbIntegrationException com UPSTREAM_ERROR")
    void shouldTranslateTimeoutToUpstreamErrorException() {
        when(responseSpec.body(TmdbMovieDetails.class))
                .thenThrow(new ResourceAccessException("Read timed out"));

        assertThatThrownBy(() -> tmdbClient.getMovieDetails(550L))
                .isInstanceOf(TmdbIntegrationException.class)
                .satisfies(ex -> {
                    TmdbIntegrationException tmdbEx = (TmdbIntegrationException) ex;
                    assertThat(tmdbEx.getKind()).isEqualTo(TmdbErrorKind.UPSTREAM_ERROR);
                    assertThat(tmdbEx.getMessage()).isEqualTo("Não foi possível contatar o TMDB.");
                });
    }
}
