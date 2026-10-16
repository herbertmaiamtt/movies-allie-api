package com.hertt.moviesallieapi.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
/*
* Configuração compartilhada por todos os mappers do projeto.
* Centralizar a configuração evita repitir as mesmas anotações
* em cada mapper e garante que mudanças de política (ex.: trocar
* a estratégia de erro) sejam aplicadas em um único ponto.
* */
@MapperConfig(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface MoviesAllieMapperConfig {
}
