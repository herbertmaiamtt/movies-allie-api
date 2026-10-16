package com.hertt.moviesallieapi.service;

import com.hertt.moviesallieapi.dto.request.AddWatchedMovieRequest;
import com.hertt.moviesallieapi.dto.request.UpdateCommentRequest;
import com.hertt.moviesallieapi.dto.response.WatchedMovieResponse;

import java.util.List;

public interface WatchedMovieService {

    WatchedMovieResponse add(Long userId, AddWatchedMovieRequest request);
    List<WatchedMovieResponse> listAll(Long userId);
    List<WatchedMovieResponse> searchByTitle(Long userId, String title);
    WatchedMovieResponse findById(Long userId, Long watchedMovieId);
    WatchedMovieResponse updateComment(Long userId, Long watchedMovieId, UpdateCommentRequest request);
    void remove(Long userId, Long watchedMovieId);
    Long count(Long userId);

}
