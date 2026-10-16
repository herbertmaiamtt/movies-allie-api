package com.hertt.moviesallieapi.mapper;

import com.hertt.moviesallieapi.dto.response.UserResponse;
import com.hertt.moviesallieapi.entity.User;
import org.mapstruct.Mapper;

/*
* Converte a entidade User em DTOs de resposta. Todos os campos
* do UserResponse têm o mesmo nome na entidade, então o MapStruct
* resolve o mapeamento automaticamente - sem necessidade de @Mapping
* explícito. Repare que o campo password da entidade não é mapeado
* porque o UserResponse não tem esse campo. Isso é intencional: a senha
* nunca sai da camada de servíço.
* */
@Mapper(config = MoviesAllieMapperConfig.class)
public interface UserMapper {

    UserResponse toResponse(User user);

}
