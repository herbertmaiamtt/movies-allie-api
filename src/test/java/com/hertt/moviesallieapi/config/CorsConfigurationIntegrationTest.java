package com.hertt.moviesallieapi.config;

import com.hertt.moviesallieapi.dto.response.MovieSearchResponse;
import com.hertt.moviesallieapi.service.MovieService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Testes de Integração - Configuração de CORS")
class CorsConfigurationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovieService movieService;

    @Test
    @DisplayName("Deve responder à requisição preflight OPTIONS com cabeçalhos CORS adequados para origem permitida")
    void shouldHandlePreflightRequestForAllowedOrigin() throws Exception {
        mockMvc.perform(options("/api/movies/search")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    @DisplayName("Deve incluir cabeçalho Access-Control-Allow-Origin em requisição GET de origem permitida")
    void shouldAllowSimpleRequestFromConfiguredOrigin() throws Exception {
        when(movieService.search("Matrix")).thenReturn(new MovieSearchResponse(1, List.of(), 0, 0));

        mockMvc.perform(get("/api/movies/search")
                        .param("query", "Matrix")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("Não deve incluir cabeçalho Access-Control-Allow-Origin para origem não autorizada")
    void shouldDenyCorsForUnauthorizedOrigin() throws Exception {
        when(movieService.search("Matrix")).thenReturn(new MovieSearchResponse(1, List.of(), 0, 0));

        mockMvc.perform(get("/api/movies/search")
                        .param("query", "Matrix")
                        .header("Origin", "http://untrusted-site.com"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
