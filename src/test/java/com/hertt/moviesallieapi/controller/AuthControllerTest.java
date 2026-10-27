package com.hertt.moviesallieapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hertt.moviesallieapi.dto.request.LoginRequest;
import com.hertt.moviesallieapi.dto.request.RegisterRequest;
import com.hertt.moviesallieapi.dto.response.AuthResponse;
import com.hertt.moviesallieapi.dto.response.UserResponse;
import com.hertt.moviesallieapi.exception.EmailAlreadyExistsException;
import com.hertt.moviesallieapi.exception.UsernameAlreadyExistsException;
import com.hertt.moviesallieapi.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Testes de Integração - AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Deve retornar 201 com dados do usuário quando cadastro for bem-sucedido")
        void shouldReturn201WhenRegistrationSuccessful() throws Exception {
            RegisterRequest request = new RegisterRequest("herbert", "herbert@email.com", "senha123");
            UserResponse response = new UserResponse(1L, "herbert", "herbert@email.com");

            when(authService.register(any(RegisterRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.username", is("herbert")))
                    .andExpect(jsonPath("$.email", is("herbert@email.com")));

            verify(authService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Deve retornar 400 quando campos obrigatórios estiverem ausentes ou inválidos")
        void shouldReturn400WhenValidationFails() throws Exception {
            RegisterRequest invalidRequest = new RegisterRequest("", "email-invalido", "12");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Um ou mais campos estão inválidos.")));
        }

        @Test
        @DisplayName("Deve retornar 409 quando o email já estiver em uso")
        void shouldReturn409WhenEmailAlreadyExists() throws Exception {
            RegisterRequest request = new RegisterRequest("herbert", "herbert@email.com", "senha123");

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new EmailAlreadyExistsException("Este email já está em uso."));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message", is("Este email já está em uso.")));
        }

        @Test
        @DisplayName("Deve retornar 409 quando o username já estiver em uso")
        void shouldReturn409WhenUsernameAlreadyExists() throws Exception {
            RegisterRequest request = new RegisterRequest("herbert", "herbert@email.com", "senha123");

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new UsernameAlreadyExistsException("Este username já está em uso."));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message", is("Este username já está em uso.")));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Deve retornar 200 com token JWT quando credenciais forem válidas")
        void shouldReturn200WhenLoginSuccessful() throws Exception {
            LoginRequest request = new LoginRequest("herbert@email.com", "senha123");
            AuthResponse response = new AuthResponse("jwt.mock.token", "Bearer", 86400000L);

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", is("jwt.mock.token")))
                    .andExpect(jsonPath("$.tokenType", is("Bearer")))
                    .andExpect(jsonPath("$.expiresIn", is(86400000)));

            verify(authService).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Deve retornar 400 quando requisição de login contiver campos em branco")
        void shouldReturn400WhenLoginFieldsAreBlank() throws Exception {
            LoginRequest invalidRequest = new LoginRequest("", "");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Um ou mais campos estão inválidos.")));
        }

        @Test
        @DisplayName("Deve retornar 401 quando credenciais forem inválidas")
        void shouldReturn401WhenBadCredentials() throws Exception {
            LoginRequest request = new LoginRequest("herbert@email.com", "senha-errada");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Email ou senha inválidos."));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.message", is("Email ou senha inválidos.")));
        }
    }
}
