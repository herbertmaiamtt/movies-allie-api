package com.hertt.moviesallieapi.repository;

import com.hertt.moviesallieapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

/*
* Busca por um usuário pelo email.
* Usado pelo CustomUserDetailsService durante o login.
* */
Optional<User> findByEmail(String email);

/*
* Busca um usuário pelo username.
* Útil para exibição de perfis públicos.
* */
Optional<User> findByUsername(String username);

/*
* Verifica se já existe um usuário com o email informado.
* Usado no cadastro para evitar duplicatas.
* */
boolean existsByEmail(String email);

/*
* Verifica se já existe um usuário com o username informado.
* Usado no cadastro para evitar duplicatas.
* */
boolean existsByUsername(String username);

}
