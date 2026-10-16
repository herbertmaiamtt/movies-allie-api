package com.hertt.moviesallieapi.service.impl;

import com.hertt.moviesallieapi.dto.request.AddWatchedMovieRequest;
import com.hertt.moviesallieapi.dto.request.UpdateCommentRequest;
import com.hertt.moviesallieapi.dto.response.WatchedMovieResponse;
import com.hertt.moviesallieapi.dto.tmdb.TmdbCastMember;
import com.hertt.moviesallieapi.dto.tmdb.TmdbCredits;
import com.hertt.moviesallieapi.dto.tmdb.TmdbCrewMember;
import com.hertt.moviesallieapi.dto.tmdb.TmdbMovieDetails;
import com.hertt.moviesallieapi.entity.WatchedMovie;
import com.hertt.moviesallieapi.exception.WatchedMovieAlreadyExistsException;
import com.hertt.moviesallieapi.exception.WatchedMovieNotFoundException;
import com.hertt.moviesallieapi.mapper.WatchedMovieMapper;
import com.hertt.moviesallieapi.repository.UserRepository;
import com.hertt.moviesallieapi.repository.WatchedMovieRepository;
import com.hertt.moviesallieapi.service.PosterUrlBuilder;
import com.hertt.moviesallieapi.service.WatchedMovieService;
import com.hertt.moviesallieapi.service.client.TmdbClient;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchedMovieServiceImpl implements WatchedMovieService {

    private static final int MAIN_ACTORS_LIMIT = 5;
    private static final String DIRECTOR_JOB = "Director";

    private final WatchedMovieRepository watchedMovieRepository;
    private final UserRepository userRepository;
    private final TmdbClient tmdbClient;
    private final WatchedMovieMapper watchedMovieMapper;
    private final PosterUrlBuilder posterUrlBuilder;

    // Adicionar
    @Override
    @Transactional
    public WatchedMovieResponse add(Long userId, AddWatchedMovieRequest request){
        // 1. Checagem antecipada de duplicata (otimização - o banco garante via UNIQUE).
        if(watchedMovieRepository.existsByUserIdAndTmdbId(userId, request.tmdbId())){
            throw new WatchedMovieAlreadyExistsException(
                    "Filme já marcado como assistido: tmdbId=" + request.tmdbId()
            );
        }

        // 2. Busca os detalhes completos no TMDB (uma única requisição).
        TmdbMovieDetails details = tmdbClient.getMovieDetails(request.tmdbId());

        // 3. Monta a identidade com os dados enriquecidos do TMDB.
        WatchedMovie movie = WatchedMovie.builder()
                .tmdbId(details.id())
                .title(details.title())
                .releaseYear(extractReleaseYear(details.releaseDate()))
                .director(extractDirector(details.credits()))
                .overview(details.overview())
                .posterPath(details.posterPath())
                .mainActors(extractMainActors(details.credits()))
                .comment(request.comment())
                .build();

        // 4. Associa ao usuário via proxy.
        movie.attachTo(userRepository.getReferenceById(userId));

        // 5. Persiste. watchedAt é definido pelo @PrePersist.
        WatchedMovie saved = watchedMovieRepository.save(movie);

        // 6. Converte para DTO.
        return toResponse(saved);
    }

    // Listar

    @Override
    @Transactional(readOnly = true)
    public List<WatchedMovieResponse> listAll(Long userId){

        return watchedMovieRepository
                .findAllByUserIdOrderByWatchedAtAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Buscar por título

    @Override
    @Transactional(readOnly = true)
    public List<WatchedMovieResponse> searchByTitle(Long userId, String title){

        return watchedMovieRepository
                .findAllByUserIdAndTitleContainingIgnoreCaseOrderByWatchedAtAsc(userId, title)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Busca por ID

    @Override
    @Transactional(readOnly = true)
    public WatchedMovieResponse findById(Long userId, Long watchedMovieId){

        return toResponse(findOwnedMovie(userId, watchedMovieId));
    }

    // Atualizaar comentário

    @Override
    @Transactional
    public WatchedMovieResponse updateComment(Long userId,
                                              Long watchedMovieId,
                                              UpdateCommentRequest request){
        WatchedMovie movie = findOwnedMovie(userId, watchedMovieId);
        movie.setComment(request.comment());

        // Dirty checking: a entidade está gerenciada dentro da transação,
        // então o Hibernate já vai emitir o UPDATE no commit. Não precisa de save().
        return toResponse(movie);
    }

    // Remover

    @Override
    @Transactional
    public void remove(Long userId, Long watchedMovieId){
        long deleted = watchedMovieRepository.deleteByIdAndUserId(watchedMovieId, userId);

        if(deleted == 0){
            throw new WatchedMovieNotFoundException(
                    "Filme assistido não encontrado: " + watchedMovieId
            );
        }
    }

    // Contar

    @Override
    @Transactional(readOnly = true)
    public Long count(Long userId){

        return watchedMovieRepository.countByUserId(userId);
    }

    // Helpers privados

    private WatchedMovie findOwnedMovie(Long userId, Long watchedMovieId){

        return watchedMovieRepository.findByIdAndUserId(watchedMovieId, userId)
                .orElseThrow(() -> new WatchedMovieNotFoundException(
                        "Filme assistido não encontrado: " + watchedMovieId
                ));
    }

    private WatchedMovieResponse toResponse(WatchedMovie entity){
        String posterUrl = posterUrlBuilder.build(entity.getPosterPath());

        return watchedMovieMapper.toResponse(entity, posterUrl);
    }

    private Integer extractReleaseYear(String releaseDate){
        if(releaseDate == null || releaseDate.length() < 4)
            return null;

        try{
            return Integer.parseInt(releaseDate.substring(0, 4));
        } catch(NumberFormatException ex){
            return null;
        }
    }

    private String extractDirector(TmdbCredits credits){
        if(credits == null || credits.crew() == null)
            return null;

        return credits.crew().stream()
                .filter(member -> DIRECTOR_JOB.equalsIgnoreCase(member.job()))
                .map(TmdbCrewMember::name)
                .findFirst()
                .orElse(null);
    }

    private String extractMainActors(TmdbCredits credits){
        if(credits == null || credits.cast() == null)
            return null;

        return credits.cast().stream()
                .sorted(Comparator.comparing(
                        TmdbCastMember::order,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAIN_ACTORS_LIMIT)
                .map(TmdbCastMember::name)
                .collect(java.util.stream.Collectors.joining(", "));
    }

}




















