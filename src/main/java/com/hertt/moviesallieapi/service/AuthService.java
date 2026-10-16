package com.hertt.moviesallieapi.service;

import com.hertt.moviesallieapi.dto.request.LoginRequest;
import com.hertt.moviesallieapi.dto.request.RegisterRequest;
import com.hertt.moviesallieapi.dto.response.AuthResponse;
import com.hertt.moviesallieapi.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

}
