package com.hertt.moviesallieapi.exception;

public class WatchedMovieNotFoundException extends RuntimeException{

    public WatchedMovieNotFoundException(String message){
        super(message);
    }

}
