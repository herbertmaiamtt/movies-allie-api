package com.hertt.moviesallieapi.service.client;

import com.hertt.moviesallieapi.dto.tmdb.TmdbMovieDetails;
import com.hertt.moviesallieapi.dto.tmdb.TmdbSearchResponse;
import com.hertt.moviesallieapi.exception.TmdbErrorKind;
import com.hertt.moviesallieapi.exception.TmdbIntegrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbClient {

    private static final String LANGUAGE = "pt-BR";
    private static final String APPEND_CREDITS = "credits";

    private final RestClient tmdbRestClient;

    // ==================== Detalhes do filme ====================

    public TmdbMovieDetails getMovieDetails(Long tmdbId) {
        log.debug("TMDB: buscando detalhes do filme {}", tmdbId);

        return execute(
                () -> tmdbRestClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/movie/{id}")
                                .queryParam("language", LANGUAGE)
                                .queryParam("append_to_response", APPEND_CREDITS)
                                .build(tmdbId))
                        .retrieve()
                        .body(TmdbMovieDetails.class),
                "Filme não encontrado no TMDB: " + tmdbId
        );
    }

    // ==================== Busca por título ====================

    public TmdbSearchResponse searchMovies(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("O termo de busca não pode ser vazio.");
        }

        log.debug("TMDB: buscando filmes com query='{}'", query);

        return execute(
                () -> tmdbRestClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/search/movie")
                                .queryParam("query", query)
                                .queryParam("language", LANGUAGE)
                                .build())
                        .retrieve()
                        .body(TmdbSearchResponse.class),
                "Recurso não encontrado no TMDB para a busca: " + query
        );
    }

    // ==================== Execução e tradução de erros ====================

    private <T> T execute(Supplier<T> call, String notFoundMessage) {
        try {
            T result = call.get();

            if (result == null) {
                // RestClient devolve null quando o corpo está vazio e o tipo não é void.
                throw new TmdbIntegrationException(
                        TmdbErrorKind.UPSTREAM_ERROR,
                        "O TMDB devolveu uma resposta vazia."
                );
            }

            return result;

        } catch (HttpClientErrorException.NotFound ex) {
            // 404 é um erro esperado do cliente — sem stack trace no log.
            log.warn("TMDB respondeu 404 para a requisição.");
            throw new TmdbIntegrationException(
                    TmdbErrorKind.NOT_FOUND,
                    notFoundMessage
            );

        } catch (HttpClientErrorException ex) {
            // Outros 4xx (400, 401, 403, 429...) indicam problema do lado do cliente
            // ou da configuração (ex.: token inválido). Não vazamos o corpo do erro.
            log.warn("TMDB respondeu erro de cliente (HTTP {}).",
                    ex.getStatusCode().value());
            throw new TmdbIntegrationException(
                    TmdbErrorKind.UPSTREAM_ERROR,
                    "O TMDB rejeitou a requisição (HTTP "
                            + ex.getStatusCode().value() + ").",
                    ex
            );

        } catch (HttpServerErrorException ex) {
            // 5xx é falha do TMDB. Preservamos a causa para investigação.
            log.warn("TMDB respondeu erro de servidor (HTTP {}).",
                    ex.getStatusCode().value());
            throw new TmdbIntegrationException(
                    TmdbErrorKind.UPSTREAM_ERROR,
                    "O TMDB está indisponível no momento.",
                    ex
            );

        } catch (ResourceAccessException ex) {
            // Timeout, DNS, conexão recusada — o TMDB não foi alcançado.
            log.warn("Falha de rede ao contatar o TMDB.");
            throw new TmdbIntegrationException(
                    TmdbErrorKind.UPSTREAM_ERROR,
                    "Não foi possível contatar o TMDB.",
                    ex
            );

        } catch (RestClientException ex) {
            // Catch-all da família RestClient — inclui erros de decodificação.
            log.warn("Erro inesperado do RestClient ao chamar o TMDB.");
            throw new TmdbIntegrationException(
                    TmdbErrorKind.UPSTREAM_ERROR,
                    "Erro inesperado na comunicação com o TMDB.",
                    ex
            );
        }
    }
}