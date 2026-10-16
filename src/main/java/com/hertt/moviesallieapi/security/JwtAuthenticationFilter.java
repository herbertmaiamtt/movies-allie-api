package com.hertt.moviesallieapi.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal (
            @NonNull
            HttpServletRequest request,
            @NonNull
            HttpServletResponse response,
            @NonNull
            FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // 1. Sem header ou formato inválido: segue o fluxo sem autenticar.
        //    O SecurityConfig decidirá se a rota exige autenticação.
        if(authHeader == null || !authHeader.startsWith(BEARER_PREFIX)){
            filterChain.doFilter(request, response);

            return;
        }

        final String jwt = authHeader.substring(BEARER_PREFIX.length());

        try{
            final String userEmail = jwtService.extractUsername(jwt);

            // 2. Só autentica se ainda não houver autenticação no contexto.
            //    Isso evita sobrescrever uma autenticação já feita (ex: por outro filtro).
            if(userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null){
                // 3. Carrega o usuário do banco para validar contra o token
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if(jwtService.isTokenValid(jwt, userDetails)){
                    // 4. Cria o token de autenticação e o coloca no contexto.
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null, // credenciais (senha) não são necessárias aqui
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch(JwtException | IllegalArgumentException ex){
            // 5. Token mal formado, expirado ou com assinatura inválida.
            // Não autentica. O SecurityConfig decidirá se a rota exige autenticação.
            SecurityContextHolder.clearContext();
        }

        // 6. Continua  a cadeia de filtros.
        filterChain.doFilter(request, response);
    }

}



























