package com.hertt.moviesallieapi.dto.response;

public record AuthResponse(

        String token,
        String tokenType,
        long expiresIn

) {

    public AuthResponse(String token, long expiresIn){
        this(token, "Bearer", expiresIn);
    }

}
