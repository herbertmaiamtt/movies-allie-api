package com.hertt.moviesallieapi.exception;

public class WatchedMovieAlreadyExistsException extends RuntimeException{

    public WatchedMovieAlreadyExistsException(String message){
        super(message);
    }

}
