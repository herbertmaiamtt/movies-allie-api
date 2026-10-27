package com.hertt.moviesallieapi.service.impl;

import com.hertt.moviesallieapi.dto.request.AddWatchedMovieRequest;
import com.hertt.moviesallieapi.dto.request.UpdateCommentRequest;
import com.hertt.moviesallieapi.dto.response.WatchedMovieResponse;
import com.hertt.moviesallieapi.dto.tmdb.TmdbCastMember;
import com.hertt.moviesallieapi.dto.tmdb.TmdbCredits;
import com.hertt.moviesallieapi.dto.tmdb.TmdbCrewMember;
import com.hertt.moviesallieapi.dto.tmdb.TmdbMovieDetails;
import com.hertt.moviesallieapi.entity.User;
import com.hertt.moviesallieapi.entity.WatchedMovie;
import com.hertt.moviesallieapi.exception.WatchedMovieAlreadyExistsException;
import com.hertt.moviesallieapi.exception.WatchedMovieNotFoundException;
import com.hertt.moviesallieapi.mapper.WatchedMovieMapper;
import com.hertt.moviesallieapi.repository.UserRepository;
import com.hertt.moviesallieapi.repository.WatchedMovieRepository;
import com.hertt.moviesallieapi.service.PosterUrlBuilder;
import com.hertt.moviesallieapi.service.client.TmdbClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Unidade - WatchedMovieServiceImpl")
class WatchedMovieServiceImplTest {

    @Mock
    private WatchedMovieRepository watchedMovieRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TmdbClient tmdbClient;

    @Mock
    private WatchedMovieMapper watchedMovieMapper;

    @Mock
    private PosterUrlBuilder posterUrlBuilder;

    @InjectMocks
    private WatchedMovieServiceImpl watchedMovieService;

    @Nested
    @DisplayName("Adicionar filme assistido (add)")
    class AddTests {

        @Test
        @DisplayName("Deve adicionar filme assistido enriquecendo com dados do TMDB com sucesso")
        void shouldAddWatchedMovieSuccessfully() {
            Long userId = 1L;
            AddWatchedMovieRequest request = new AddWatchedMovieRequest(603L, "Excelente filme!");

            TmdbCredits credits = new TmdbCredits(
                    List.of(
                            new TmdbCastMember(1L, "Keanu Reeves", "Neo", 0),
                            new TmdbCastMember(2L, "Laurence Fishburne", "Morpheus", 1),
                            new TmdbCastMember(3L, "Carrie-Anne Moss", "Trinity", 2),
                            new TmdbCastMember(4L, "Hugo Weaving", "Agent Smith", 3),
                            new TmdbCastMember(5L, "Joe Pantoliano", "Cypher", 4),
                            new TmdbCastMember(6L, "Marcus Chong", "Tank", 5) // deve ser limitado a 5
                    ),
                    List.of(
                            new TmdbCrewMember(10L, "Lana Wachowski", "Director", "Directing"),
                            new TmdbCrewMember(11L, "Joel Silver", "Producer", "Production")
                    )
            );
            TmdbMovieDetails details = new TmdbMovieDetails(
                    603L, "The Matrix", "Sinopse", "1999-03-31", "/matrix.jpg", 136, credits
            );
            User userProxy = User.builder().id(userId).build();
            WatchedMovie savedMovie = WatchedMovie.builder()
                    .id(10L)
                    .tmdbId(603L)
                    .title("The Matrix")
                    .releaseYear(1999)
                    .director("Lana Wachowski")
                    .mainActors("Keanu Reeves, Laurence Fishburne, Carrie-Anne Moss, Hugo Weaving, Joe Pantoliano")
                    .overview("Sinopse")
                    .posterPath("/matrix.jpg")
                    .comment("Excelente filme!")
                    .user(userProxy)
                    .watchedAt(LocalDateTime.now())
                    .build();

            WatchedMovieResponse expectedResponse = new WatchedMovieResponse(
                    10L, 603L, "The Matrix", 1999, "Lana Wachowski", "Sinopse",
                    "https://image.tmdb.org/t/p/w500/matrix.jpg",
                    "Keanu Reeves, Laurence Fishburne, Carrie-Anne Moss, Hugo Weaving, Joe Pantoliano",
                    "Excelente filme!", savedMovie.getWatchedAt()
            );

            when(watchedMovieRepository.existsByUserIdAndTmdbId(userId, 603L)).thenReturn(false);
            when(tmdbClient.getMovieDetails(603L)).thenReturn(details);
            when(userRepository.getReferenceById(userId)).thenReturn(userProxy);
            when(watchedMovieRepository.save(any(WatchedMovie.class))).thenReturn(savedMovie);
            when(posterUrlBuilder.build("/matrix.jpg")).thenReturn("https://image.tmdb.org/t/p/w500/matrix.jpg");
            when(watchedMovieMapper.toResponse(eq(savedMovie), eq("https://image.tmdb.org/t/p/w500/matrix.jpg")))
                    .thenReturn(expectedResponse);

            WatchedMovieResponse response = watchedMovieService.add(userId, request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.title()).isEqualTo("The Matrix");
            assertThat(response.director()).isEqualTo("Lana Wachowski");
            assertThat(response.mainActors()).doesNotContain("Marcus Chong"); // limitado aos top 5
            assertThat(response.comment()).isEqualTo("Excelente filme!");

            verify(watchedMovieRepository).save(any(WatchedMovie.class));
        }

        @Test
        @DisplayName("Deve lançar WatchedMovieAlreadyExistsException se o usuário já assistiu ao filme")
        void shouldThrowExceptionWhenMovieAlreadyWatched() {
            Long userId = 1L;
            AddWatchedMovieRequest request = new AddWatchedMovieRequest(603L, "Comentário");
            when(watchedMovieRepository.existsByUserIdAndTmdbId(userId, 603L)).thenReturn(true);

            assertThatThrownBy(() -> watchedMovieService.add(userId, request))
                    .isInstanceOf(WatchedMovieAlreadyExistsException.class)
                    .hasMessageContaining("Filme já marcado como assistido: tmdbId=603");

            verify(tmdbClient, never()).getMovieDetails(any());
            verify(watchedMovieRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Listagem e Consultas")
    class QueryTests {

        @Test
        @DisplayName("Deve listar todos os filmes assistidos do usuário ordenados por data")
        void shouldListAllWatchedMoviesForUser() {
            Long userId = 1L;
            WatchedMovie movie = WatchedMovie.builder().id(1L).tmdbId(603L).posterPath("/p.jpg").build();
            WatchedMovieResponse responseDto = new WatchedMovieResponse(
                    1L, 603L, "Matrix", 1999, "Director", "Overview", "url", "Actors", "Comment", LocalDateTime.now()
            );

            when(watchedMovieRepository.findAllByUserIdOrderByWatchedAtAsc(userId)).thenReturn(List.of(movie));
            when(posterUrlBuilder.build("/p.jpg")).thenReturn("url");
            when(watchedMovieMapper.toResponse(movie, "url")).thenReturn(responseDto);

            List<WatchedMovieResponse> list = watchedMovieService.listAll(userId);

            assertThat(list).hasSize(1);
            assertThat(list.get(0)).isEqualTo(responseDto);
        }

        @Test
        @DisplayName("Deve buscar filmes assistidos pelo título")
        void shouldSearchWatchedMoviesByTitle() {
            Long userId = 1L;
            WatchedMovie movie = WatchedMovie.builder().id(1L).title("Matrix").posterPath("/p.jpg").build();
            WatchedMovieResponse responseDto = new WatchedMovieResponse(
                    1L, 603L, "Matrix", 1999, "Director", "Overview", "url", "Actors", "Comment", LocalDateTime.now()
            );

            when(watchedMovieRepository.findAllByUserIdAndTitleContainingIgnoreCaseOrderByWatchedAtAsc(userId, "mat"))
                    .thenReturn(List.of(movie));
            when(posterUrlBuilder.build("/p.jpg")).thenReturn("url");
            when(watchedMovieMapper.toResponse(movie, "url")).thenReturn(responseDto);

            List<WatchedMovieResponse> list = watchedMovieService.searchByTitle(userId, "mat");

            assertThat(list).hasSize(1);
            assertThat(list.get(0)).isEqualTo(responseDto);
        }

        @Test
        @DisplayName("Deve encontrar filme assistido por ID pertencente ao usuário")
        void shouldFindWatchedMovieByIdWhenOwned() {
            Long userId = 1L;
            Long movieId = 10L;
            WatchedMovie movie = WatchedMovie.builder().id(movieId).posterPath("/p.jpg").build();
            WatchedMovieResponse responseDto = new WatchedMovieResponse(
                    movieId, 603L, "Matrix", 1999, "Director", "Overview", "url", "Actors", "Comment", LocalDateTime.now()
            );

            when(watchedMovieRepository.findByIdAndUserId(movieId, userId)).thenReturn(Optional.of(movie));
            when(posterUrlBuilder.build("/p.jpg")).thenReturn("url");
            when(watchedMovieMapper.toResponse(movie, "url")).thenReturn(responseDto);

            WatchedMovieResponse result = watchedMovieService.findById(userId, movieId);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(movieId);
        }

        @Test
        @DisplayName("Deve lançar WatchedMovieNotFoundException quando filme não for encontrado para o usuário")
        void shouldThrowExceptionWhenMovieNotFoundById() {
            Long userId = 1L;
            Long movieId = 999L;
            when(watchedMovieRepository.findByIdAndUserId(movieId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> watchedMovieService.findById(userId, movieId))
                    .isInstanceOf(WatchedMovieNotFoundException.class)
                    .hasMessageContaining("Filme assistido não encontrado: 999");
        }

        @Test
        @DisplayName("Deve retornar contagem correta de filmes do usuário")
        void shouldReturnMovieCountForUser() {
            Long userId = 1L;
            when(watchedMovieRepository.countByUserId(userId)).thenReturn(5L);

            Long count = watchedMovieService.count(userId);

            assertThat(count).isEqualTo(5L);
            verify(watchedMovieRepository).countByUserId(userId);
        }
    }

    @Nested
    @DisplayName("Atualização e Remoção")
    class MutationTests {

        @Test
        @DisplayName("Deve atualizar comentário do filme assistido pertencente ao usuário")
        void shouldUpdateCommentSuccessfully() {
            Long userId = 1L;
            Long movieId = 10L;
            UpdateCommentRequest request = new UpdateCommentRequest("Comentário atualizado!");
            WatchedMovie movie = WatchedMovie.builder().id(movieId).comment("Comentário antigo").posterPath("/p.jpg").build();
            WatchedMovieResponse responseDto = new WatchedMovieResponse(
                    movieId, 603L, "Matrix", 1999, "Director", "Overview", "url", "Actors", "Comentário atualizado!", LocalDateTime.now()
            );

            when(watchedMovieRepository.findByIdAndUserId(movieId, userId)).thenReturn(Optional.of(movie));
            when(posterUrlBuilder.build("/p.jpg")).thenReturn("url");
            when(watchedMovieMapper.toResponse(movie, "url")).thenReturn(responseDto);

            WatchedMovieResponse result = watchedMovieService.updateComment(userId, movieId, request);

            assertThat(result).isNotNull();
            assertThat(movie.getComment()).isEqualTo("Comentário atualizado!");
        }

        @Test
        @DisplayName("Deve lançar WatchedMovieNotFoundException ao tentar atualizar filme de outro usuário")
        void shouldThrowExceptionWhenUpdatingNonOwnedMovie() {
            Long userId = 1L;
            Long movieId = 10L;
            UpdateCommentRequest request = new UpdateCommentRequest("Novo comentário");
            when(watchedMovieRepository.findByIdAndUserId(movieId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> watchedMovieService.updateComment(userId, movieId, request))
                    .isInstanceOf(WatchedMovieNotFoundException.class);
        }

        @Test
        @DisplayName("Deve remover filme assistido com sucesso")
        void shouldRemoveWatchedMovieSuccessfully() {
            Long userId = 1L;
            Long movieId = 10L;
            when(watchedMovieRepository.deleteByIdAndUserId(movieId, userId)).thenReturn(1L);

            watchedMovieService.remove(userId, movieId);

            verify(watchedMovieRepository).deleteByIdAndUserId(movieId, userId);
        }

        @Test
        @DisplayName("Deve lançar WatchedMovieNotFoundException ao tentar remover filme não pertencente ao usuário")
        void shouldThrowExceptionWhenRemovingNonOwnedMovie() {
            Long userId = 1L;
            Long movieId = 10L;
            when(watchedMovieRepository.deleteByIdAndUserId(movieId, userId)).thenReturn(0L);

            assertThatThrownBy(() -> watchedMovieService.remove(userId, movieId))
                    .isInstanceOf(WatchedMovieNotFoundException.class)
                    .hasMessageContaining("Filme assistido não encontrado: 10");
        }
    }
}
