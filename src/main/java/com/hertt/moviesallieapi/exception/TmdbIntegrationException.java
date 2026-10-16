package com.hertt.moviesallieapi.exception;

import lombok.Getter;

@Getter
public class TmdbIntegrationException extends RuntimeException {

    private final TmdbErrorKind kind;

    public TmdbIntegrationException(TmdbErrorKind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public TmdbIntegrationException(TmdbErrorKind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public TmdbErrorKind getKind() {
        return kind;
    }
}