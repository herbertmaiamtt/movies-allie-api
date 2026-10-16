package com.hertt.moviesallieapi.repository;

import com.hertt.moviesallieapi.entity.WatchedMovie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchedMovieRepository extends JpaRepository<WatchedMovie, Long> {

    /*
    * Lista todos os filmes assistidos de um usuário,
    * do mais antigo para o mais recente.
    * Consulta principal do projeto.
    * */
    List<WatchedMovie> findAllByUserIdOrderByWatchedAtAsc(Long userId);

    /*
    * Busca filmes assistidos de um usuário filtrando pelo título,
    * do mais antigo para o mais recente.
    * Usado pela barra de pesquisa interna.
    * */
    List<WatchedMovie> findAllByUserIdAndTitleContainingIgnoreCaseOrderByWatchedAtAsc(Long userId, String title);

    /*
    * Verifica se um usuário já marcou um determinado filme como assistido.
    * Usado antes de inserir, para evitar duplicatas e dar feedback rápido.
    * */
    boolean existsByUserIdAndTmdbId(Long userId, Long tmdbId);

    /*
    * Conta quantos filmes um usuário já assistiu.
    * Útil para exibir estatísticas no perfil.
    * */
    Long countByUserId(Long userId);

    /*
    * Busca um filme assistido específico, garantindo que pertence ao usuário.
    * Usado em operações de edição/exclusão para evitar acesso indevido.
    * */
    Optional<WatchedMovie> findByIdAndUserId(Long id, Long userId);

    /*
    * Remove um filme assistido específico, garantindo que pertence ao usuário.
    * Retorna a quantidade de registros deletados (0 ou 1).
    * */
    long deleteByIdAndUserId(Long id, Long userId);

}



























