// file: src/main/java/com/hertt/moviesallieapi/service/impl/AuthServiceImpl.java
package com.hertt.moviesallieapi.service.impl;

import com.hertt.moviesallieapi.dto.request.LoginRequest;
import com.hertt.moviesallieapi.dto.request.RegisterRequest;
import com.hertt.moviesallieapi.dto.response.AuthResponse;
import com.hertt.moviesallieapi.dto.response.UserResponse;
import com.hertt.moviesallieapi.entity.User;
import com.hertt.moviesallieapi.exception.EmailAlreadyExistsException;
import com.hertt.moviesallieapi.exception.UsernameAlreadyExistsException;
import com.hertt.moviesallieapi.mapper.UserMapper;
import com.hertt.moviesallieapi.repository.UserRepository;
import com.hertt.moviesallieapi.security.JwtProperties;
import com.hertt.moviesallieapi.security.JwtService;
import com.hertt.moviesallieapi.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;

    // ==================== Cadastro ====================

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(
                    "Já existe um usuário com o email: " + request.email());
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(
                    "Já existe um usuário com o username: " + request.username());
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();

        User saved = userRepository.save(user);

        return userMapper.toResponse(saved);
    }

    // ==================== Login ====================

    @Override
    public AuthResponse login(LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = (User) authentication.getPrincipal();

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, jwtProperties.expiration());
    }
}