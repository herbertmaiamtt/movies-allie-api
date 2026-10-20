package com.hertt.moviesallieapi.controller;

import com.hertt.moviesallieapi.dto.response.MovieDetailsResponse;
import com.hertt.moviesallieapi.dto.response.MovieSearchResponse;
import com.hertt.moviesallieapi.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Tag(
        name = "Catálogo de Filmes",
        description = "Endpoints públicos para busca e consulta de detalhes de filmes integrados com a API do TMDB."
)
public class MovieController {

    private final MovieService movieService;

    @GetMapping("/search")
    @Operation(
            summary = "Busca filmes no catálogo do TMDB",
            description = """
                    Pesquisa filmes na API do TMDB pelo título informado em língua portuguesa (pt-BR).
                    Os resultados retornam dados essenciais do filme com a URL completa do pôster montada.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Termo de busca ausente ou inválido."),
            @ApiResponse(responseCode = "429", description = "Limite de requisições ao TMDB excedido."),
            @ApiResponse(responseCode = "502", description = "Falha de comunicação ou indisponibilidade da API do TMDB.")
    })
    public ResponseEntity<MovieSearchResponse> search(
            @Parameter(description = "Título ou trecho do título a ser pesquisado.", required = true)
            @RequestParam("query") String query
    ) {
        MovieSearchResponse response = movieService.search(query);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Busca detalhes de um filme pelo ID do TMDB",
            description = """
                    Recupera os detalhes completos de um filme no catálogo do TMDB,
                    incluindo sinopse, diretor, elenco principal, ano de lançamento e URL completa do pôster.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalhes do filme recuperados com sucesso."),
            @ApiResponse(responseCode = "400", description = "ID do filme informado inválido."),
            @ApiResponse(responseCode = "404", description = "Filme não encontrado no TMDB."),
            @ApiResponse(responseCode = "429", description = "Limite de requisições ao TMDB excedido."),
            @ApiResponse(responseCode = "502", description = "Falha de comunicação ou indisponibilidade da API do TMDB.")
    })
    public ResponseEntity<MovieDetailsResponse> getDetails(
            @Parameter(description = "ID do filme no TMDB.", required = true)
            @PathVariable("id") Long id
    ) {
        MovieDetailsResponse response = movieService.getDetails(id);
        return ResponseEntity.ok(response);
    }
}
