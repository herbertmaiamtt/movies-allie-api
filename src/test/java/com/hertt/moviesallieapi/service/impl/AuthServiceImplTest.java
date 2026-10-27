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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Unidade - AuthServiceImpl")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    @Nested
    @DisplayName("Cadastro (register)")
    class RegisterTests {

        @Test
        @DisplayName("Deve cadastrar novo usuário com sucesso criptografando a senha")
        void shouldRegisterNewUserSuccessfully() {
            RegisterRequest request = new RegisterRequest("herbert", "herbert@email.com", "senha123");
            User savedUser = User.builder()
                    .id(1L)
                    .username("herbert")
                    .email("herbert@email.com")
                    .password("hash_bcrypt")
                    .createdAt(LocalDateTime.now())
                    .build();
            UserResponse expectedResponse = new UserResponse(1L, "herbert", "herbert@email.com");

            when(userRepository.existsByEmail("herbert@email.com")).thenReturn(false);
            when(userRepository.existsByUsername("herbert")).thenReturn(false);
            when(passwordEncoder.encode("senha123")).thenReturn("hash_bcrypt");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(userMapper.toResponse(savedUser)).thenReturn(expectedResponse);

            UserResponse response = authService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.username()).isEqualTo("herbert");
            assertThat(response.email()).isEqualTo("herbert@email.com");

            verify(userRepository).save(any(User.class));
            verify(passwordEncoder).encode("senha123");
        }

        @Test
        @DisplayName("Deve lançar EmailAlreadyExistsException quando email já existir")
        void shouldThrowExceptionWhenEmailExists() {
            RegisterRequest request = new RegisterRequest("herbert", "duplicado@email.com", "senha123");
            when(userRepository.existsByEmail("duplicado@email.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining("Já existe um usuário com o email: duplicado@email.com");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar UsernameAlreadyExistsException quando username já existir")
        void shouldThrowExceptionWhenUsernameExists() {
            RegisterRequest request = new RegisterRequest("duplicado", "novo@email.com", "senha123");
            when(userRepository.existsByEmail("novo@email.com")).thenReturn(false);
            when(userRepository.existsByUsername("duplicado")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(UsernameAlreadyExistsException.class)
                    .hasMessageContaining("Já existe um usuário com o username: duplicado");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("Deve autenticar usuário e retornar token JWT e expiração")
        void shouldLoginSuccessfullyAndReturnJwt() {
            LoginRequest request = new LoginRequest("herbert@email.com", "senha123");
            User user = User.builder().id(1L).email("herbert@email.com").username("herbert").build();
            Authentication authentication = mock(Authentication.class);

            when(authentication.getPrincipal()).thenReturn(user);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
            when(jwtService.generateToken(user)).thenReturn("token_jwt_valido");
            when(jwtProperties.expiration()).thenReturn(86400000L);

            AuthResponse response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo("token_jwt_valido");
            assertThat(response.expiresIn()).isEqualTo(86400000L);

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtService).generateToken(user);
        }

        @Test
        @DisplayName("Deve propagar BadCredentialsException quando credenciais forem inválidas")
        void shouldThrowBadCredentialsWhenAuthenticationFails() {
            LoginRequest request = new LoginRequest("herbert@email.com", "senha_errada");
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);

            verify(jwtService, never()).generateToken(any());
        }
    }
}
