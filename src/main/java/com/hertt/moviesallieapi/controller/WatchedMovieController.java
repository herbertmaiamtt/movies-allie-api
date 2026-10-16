package com.hertt.moviesallieapi.controller;

import com.hertt.moviesallieapi.dto.request.AddWatchedMovieRequest;
import com.hertt.moviesallieapi.dto.request.UpdateCommentRequest;
import com.hertt.moviesallieapi.dto.response.WatchedMovieResponse;
import com.hertt.moviesallieapi.entity.User;
import com.hertt.moviesallieapi.service.WatchedMovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watched-movies")
@RequiredArgsConstructor
@Tag(
        name = "Filmes Assistidos",
        description = "Endpoints protegidos para gerenciar os filmes assistidos do usuário atenticado."
)
@SecurityRequirement(name = "bearerAuth")
public class WatchedMovieController {

    private final WatchedMovieService watchedMovieService;

    // Criar

    @PostMapping
    @Operation(
            summary = "Marca um filme como assistido",
            description = """
                    Adiciona um filme à lista de assistidos do usuário autenticado.
                    Regras:
                    - O `tmdvId`é obrigatório e deve ser positivo.
                    - O usuário não pode marcar o mesmo filme duas vezes.
                    - Os dados do filme são obtidos na API do TMDB no momento da criação.
                    - O comentário é opcional, com no máximo 2000 caracteres.    
                """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Filme marcado como assistido."),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos."),
        @ApiResponse(responseCode = "401", description = "Token ausente e ou inválido."),
        @ApiResponse(responseCode = "409", description = "Filme já marcado como asistido.")
    })
    public ResponseEntity<WatchedMovieResponse> add(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody AddWatchedMovieRequest request
            ){
        WatchedMovieResponse response =
                watchedMovieService.add(currentUser.getId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Listar

    @GetMapping
    @Operation(
            summary = "Lista os filmes assistidos",
            description = "Devolve todos os filmes assistidos do usuário autenticado, do mais antigo para o mais novo."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista devolvida com sucesso."),
            @ApiResponse(responseCode = "401", description = "Token ausente e ou inválido.")
    })
    public ResponseEntity<List<WatchedMovieResponse>> listAll(
            @AuthenticationPrincipal User currentUser
    ){

        return ResponseEntity.ok(watchedMovieService.listAll(currentUser.getId()));
    }

    // Busca por título

    @GetMapping("/search")
    @Operation(
            summary = "Busca filmes assistidos por título",
            description = "Busca, na lista do usuário autenticado, filmes cujo título contenha o trecho informado. A busca é case-insensitive."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Parâmetro 'title' ausente ou inválido."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.")
    })
    public ResponseEntity<List<WatchedMovieResponse>> searchByTitle(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "Trecho do título a ser pesquisado.",
            required = true)
            @RequestParam("title") String title
    ) {

        return ResponseEntity.ok(
                watchedMovieService.searchByTitle(currentUser.getId(), title)
        );
    }

    // Contar

    @GetMapping("/count")
    @Operation(
            summary = "Conta os filmes assistidos",
            description = "Devolve o total de filmes assistidos do usuário autenticado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contagem devolvida com sucesso."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.")
    })
    public ResponseEntity<Long> count(
            @AuthenticationPrincipal User currentUser
    ){
        return ResponseEntity.ok(watchedMovieService.count(currentUser.getId()));
    }

    // Buscar por ID

    @GetMapping("/{id}")
    @Operation(
            summary = "Busca um filme assistido por ID",
            description = "Devolve um filme assistido específico do usuário autenticado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filme encontrado."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido."),
            @ApiResponse(responseCode = "404", description = "Filme assistido não encontrado para este usuário.")
    })
    public ResponseEntity<WatchedMovieResponse> findById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id
    ){

        return ResponseEntity.ok(
                watchedMovieService.findById(currentUser.getId(), id)
        );
    }

    // Atualizar comentário

    @PatchMapping("/{id}/comment")
    @Operation(
            summary = "Atualiza o comentário de um filme assistido",
            description = "Atualiza apenas o campo `comment` de um filme assistido. O comentário pode ser vazio ou nulo."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comentário atualizado."),
            @ApiResponse(responseCode = "400", description = "Comentário excede 2000 caracteres."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido."),
            @ApiResponse(responseCode = "404", description = "Filme assistido não encontrado para este usuário.")
    })
    public ResponseEntity<WatchedMovieResponse> updateComment(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommentRequest request
    ) {

        return ResponseEntity.ok(
                watchedMovieService.updateComment(currentUser.getId(), id, request)
        );
    }

    // Remover

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Remove um filme assistido",
            description = "Remove um filme assistido do usuário autenticado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Filme removido com sucesso."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido."),
            @ApiResponse(responseCode = "404", description = "Filme assistido não encontrado para este usuário.")
    })
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id
    ){
        watchedMovieService.remove(currentUser.getId(), id);

        return ResponseEntity.noContent().build();
    }

}




















