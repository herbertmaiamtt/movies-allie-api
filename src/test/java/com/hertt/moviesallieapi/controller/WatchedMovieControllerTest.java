package com.hertt.moviesallieapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hertt.moviesallieapi.dto.request.AddWatchedMovieRequest;
import com.hertt.moviesallieapi.dto.request.UpdateCommentRequest;
import com.hertt.moviesallieapi.dto.response.WatchedMovieResponse;
import com.hertt.moviesallieapi.entity.User;
import com.hertt.moviesallieapi.exception.WatchedMovieAlreadyExistsException;
import com.hertt.moviesallieapi.exception.WatchedMovieNotFoundException;
import com.hertt.moviesallieapi.service.WatchedMovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Testes de Integração - WatchedMovieController")
class WatchedMovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WatchedMovieService watchedMovieService;

    private User currentUser;
    private WatchedMovieResponse sampleResponse;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L)
                .username("herbert")
                .email("herbert@email.com")
                .password("encoded_pass")
                .build();

        sampleResponse = new WatchedMovieResponse(
                10L,
                603L,
                "The Matrix",
                1999,
                "Lana Wachowski",
                "Sinopse do Matrix",
                "https://image.tmdb.org/t/p/w500/matrix.jpg",
                "Keanu Reeves, Laurence Fishburne",
                "Comentário inicial",
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/watched-movies")
    class AddWatchedMovieTests {

        @Test
        @DisplayName("Deve retornar 201 quando o filme for adicionado com sucesso pelo usuário autenticado")
        void shouldReturn201WhenMovieAddedSuccessfully() throws Exception {
            AddWatchedMovieRequest request = new AddWatchedMovieRequest(603L, "Excelente filme sci-fi");

            when(watchedMovieService.add(eq(1L), any(AddWatchedMovieRequest.class))).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/watched-movies")
                            .with(user(currentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(10)))
                    .andExpect(jsonPath("$.tmdbId", is(603)))
                    .andExpect(jsonPath("$.title", is("The Matrix")))
                    .andExpect(jsonPath("$.director", is("Lana Wachowski")));

            verify(watchedMovieService).add(eq(1L), any(AddWatchedMovieRequest.class));
        }

        @Test
        @DisplayName("Deve retornar 400 quando o payload for inválido (tmdbId nulo ou não positivo)")
        void shouldReturn400WhenPayloadIsInvalid() throws Exception {
            AddWatchedMovieRequest invalidRequest = new AddWatchedMovieRequest(-5L, "Comentário");

            mockMvc.perform(post("/api/watched-movies")
                            .with(user(currentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Um ou mais campos estão inválidos.")));
        }

        @Test
        @DisplayName("Deve retornar 409 quando o filme já estiver marcado como assistido")
        void shouldReturn409WhenMovieAlreadyWatched() throws Exception {
            AddWatchedMovieRequest request = new AddWatchedMovieRequest(603L, "Excelente filme sci-fi");

            when(watchedMovieService.add(eq(1L), any(AddWatchedMovieRequest.class)))
                    .thenThrow(new WatchedMovieAlreadyExistsException("Filme já marcado como assistido."));

            mockMvc.perform(post("/api/watched-movies")
                            .with(user(currentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message", is("Filme já marcado como assistido.")));
        }

        @Test
        @DisplayName("Deve retornar 403 quando não autenticado")
        void shouldReturn403WhenUnauthenticated() throws Exception {
            AddWatchedMovieRequest request = new AddWatchedMovieRequest(603L, "Comentário");

            mockMvc.perform(post("/api/watched-movies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/watched-movies")
    class ListAllTests {

        @Test
        @DisplayName("Deve retornar 200 com lista de filmes assistidos do usuário autenticado")
        void shouldReturn200WithListOfWatchedMovies() throws Exception {
            when(watchedMovieService.listAll(1L)).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/watched-movies")
                            .with(user(currentUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(10)))
                    .andExpect(jsonPath("$[0].title", is("The Matrix")));

            verify(watchedMovieService).listAll(1L);
        }

        @Test
        @DisplayName("Deve retornar 403 quando não autenticado")
        void shouldReturn403WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/watched-movies"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/watched-movies/search")
    class SearchByTitleTests {

        @Test
        @DisplayName("Deve retornar 200 com filmes correspondentes ao título")
        void shouldReturn200WithMatchingMovies() throws Exception {
            when(watchedMovieService.searchByTitle(1L, "Matrix")).thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/watched-movies/search")
                            .with(user(currentUser))
                            .param("title", "Matrix"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].title", is("The Matrix")));

            verify(watchedMovieService).searchByTitle(1L, "Matrix");
        }

        @Test
        @DisplayName("Deve retornar 400 quando o parâmetro obrigatório title estiver ausente")
        void shouldReturn400WhenTitleParamIsMissing() throws Exception {
            mockMvc.perform(get("/api/watched-movies/search")
                            .with(user(currentUser)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("O parâmetro obrigatório 'title' não foi informado.")));
        }

        @Test
        @DisplayName("Deve retornar 403 quando não autenticado")
        void shouldReturn403WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/watched-movies/search")
                            .param("title", "Matrix"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/watched-movies/count")
    class CountTests {

        @Test
        @DisplayName("Deve retornar 200 com a contagem total de filmes assistidos")
        void shouldReturn200WithCount() throws Exception {
            when(watchedMovieService.count(1L)).thenReturn(42L);

            mockMvc.perform(get("/api/watched-movies/count")
                            .with(user(currentUser)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("42"));

            verify(watchedMovieService).count(1L);
        }

        @Test
        @DisplayName("Deve retornar 403 quando não autenticado")
        void shouldReturn403WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/watched-movies/count"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/watched-movies/{id}")
    class FindByIdTests {

        @Test
        @DisplayName("Deve retornar 200 com os dados do filme quando encontrado")
        void shouldReturn200WhenMovieFound() throws Exception {
            when(watchedMovieService.findById(1L, 10L)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/watched-movies/10")
                            .with(user(currentUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(10)))
                    .andExpect(jsonPath("$.title", is("The Matrix")));

            verify(watchedMovieService).findById(1L, 10L);
        }

        @Test
        @DisplayName("Deve retornar 404 quando o filme assistido não for encontrado")
        void shouldReturn404WhenNotFound() throws Exception {
            when(watchedMovieService.findById(1L, 999L))
                    .thenThrow(new WatchedMovieNotFoundException("Filme assistido com ID 999 não encontrado para o usuário atual."));

            mockMvc.perform(get("/api/watched-movies/999")
                            .with(user(currentUser)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Filme assistido com ID 999 não encontrado para o usuário atual.")));
        }

        @Test
        @DisplayName("Deve retornar 403 quando não autenticado")
        void shouldReturn403WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/watched-movies/10"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /api/watched-movies/{id}/comment")
    class UpdateCommentTests {

        @Test
        @DisplayName("Deve retornar 200 com o comentário atualizado")
        void shouldReturn200WhenCommentUpdated() throws Exception {
            UpdateCommentRequest request = new UpdateCommentRequest("Comentário revisado");
            WatchedMovieResponse updatedResponse = new WatchedMovieResponse(
                    10L, 603L, "The Matrix", 1999, "Lana Wachowski",
                    "Sinopse do Matrix", "https://image.tmdb.org/t/p/w500/matrix.jpg",
                    "Keanu Reeves, Laurence Fishburne", "Comentário revisado", LocalDateTime.now()
            );

            when(watchedMovieService.updateComment(eq(1L), eq(10L), any(UpdateCommentRequest.class)))
                    .thenReturn(updatedResponse);

            mockMvc.perform(patch("/api/watched-movies/10/comment")
                            .with(user(currentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comment", is("Comentário revisado")));

            verify(watchedMovieService).updateComment(eq(1L), eq(10L), any(UpdateCommentRequest.class));
        }

        @Test
        @DisplayName("Deve retornar 400 quando o comentário exceder o limite de 2000 caracteres")
        void shouldReturn400WhenCommentTooLong() throws Exception {
            String longComment = "a".repeat(2001);
            UpdateCommentRequest request = new UpdateCommentRequest(longComment);

            mockMvc.perform(patch("/api/watched-movies/10/comment")
                            .with(user(currentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Um ou mais campos estão inválidos.")));
        }

        @Test
        @DisplayName("Deve retornar 404 ao tentar atualizar comentário de filme inexistente")
        void shouldReturn404WhenMovieNotFoundOnUpdate() throws Exception {
            UpdateCommentRequest request = new UpdateCommentRequest("Novo comentário");

            when(watchedMovieService.updateComment(eq(1L), eq(999L), any(UpdateCommentRequest.class)))
                    .thenThrow(new WatchedMovieNotFoundException("Filme assistido com ID 999 não encontrado para o usuário atual."));

            mockMvc.perform(patch("/api/watched-movies/999/comment")
                            .with(user(currentUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));
        }

        @Test
        @DisplayName("Deve retornar 403 quando não autenticado")
        void shouldReturn403WhenUnauthenticated() throws Exception {
            UpdateCommentRequest request = new UpdateCommentRequest("Novo comentário");

            mockMvc.perform(patch("/api/watched-movies/10/comment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/watched-movies/{id}")
    class RemoveTests {

        @Test
        @DisplayName("Deve retornar 204 No Content quando removido com sucesso")
        void shouldReturn204WhenRemovedSuccessfully() throws Exception {
            doNothing().when(watchedMovieService).remove(1L, 10L);

            mockMvc.perform(delete("/api/watched-movies/10")
                            .with(user(currentUser)))
                    .andExpect(status().isNoContent());

            verify(watchedMovieService).remove(1L, 10L);
        }

        @Test
        @DisplayName("Deve retornar 404 quando o filme a ser removido não for encontrado")
        void shouldReturn404WhenNotFoundOnDelete() throws Exception {
            doThrow(new WatchedMovieNotFoundException("Filme assistido com ID 999 não encontrado para o usuário atual."))
                    .when(watchedMovieService).remove(1L, 999L);

            mockMvc.perform(delete("/api/watched-movies/999")
                            .with(user(currentUser)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));
        }

        @Test
        @DisplayName("Deve retornar 403 quando não autenticado")
        void shouldReturn403WhenUnauthenticated() throws Exception {
            mockMvc.perform(delete("/api/watched-movies/10"))
                    .andExpect(status().isForbidden());
        }
    }
}
