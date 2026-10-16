package com.hertt.moviesallieapi.controller;

import com.hertt.moviesallieapi.dto.request.LoginRequest;
import com.hertt.moviesallieapi.dto.request.RegisterRequest;
import com.hertt.moviesallieapi.dto.response.AuthResponse;
import com.hertt.moviesallieapi.dto.response.UserResponse;
import com.hertt.moviesallieapi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(
        name = "Autenticação",
        description = "Endpoints públicos para cadastro e login de usuário."
)
public class AuthController {

    private final AuthService authService;

    // Cadastro

    @PostMapping("/register")
    @Operation(
            summary = "Cadastra um novo usuário",
            description = """
               Cria um novo usuário no sistema.
               
               Regras:
               - O email deve ser único.
               - O username deve ser único.
               - A senha é criptografada com BCrypt antes de ser salva.
               - A senha nunca é devolvida na resposta. 
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos."),
            @ApiResponse(responseCode = "409", description = "Email ou username já cadastrado.")
    })
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ){
        UserResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Login

    @PostMapping("/login")
    @Operation(
            summary = "Autentica um usuário",
            description = """
                Valida as credenciais do usuário e devolve um token JWT.
                O token deve ser enviado nas próximas requisições protegidas
                no cabeçalho `Authorization: Bearer <token>`.    
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "Credeenciais inválidas.")
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ){
        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }
}





















