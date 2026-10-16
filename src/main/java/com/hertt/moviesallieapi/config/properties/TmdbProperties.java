package com.hertt.moviesallieapi.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated // entender pq o uso d
@ConfigurationProperties(prefix = "tmdb")
public record TmdbProperties(

    @NotBlank(message = "A URL base do TMDB é obrigatória.")
    String baseUrl,

    @NotBlank(message = "O token do TMDB é obrigatório.")
    String token,

    @NotBlank(message = "O tamanho do pôster é obrigatório.")
    String posterSize

) { }
