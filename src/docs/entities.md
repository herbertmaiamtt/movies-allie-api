# Entidades — Movies Allie

## 1. Visão Geral

O banco de dados tem duas entidades:

- **User**: representa o usuário da aplicação.
- **WatchedMovie**: representa um filme que um usuário assistiu.

Cada usuário pode ter muitos filmes assistidos.
Cada filme assistido pertence a um único usuário.

## 2. Entidade User

A entidade `User` mapeia a tabela `users`.

### 2.1. Campos

| Campo | Tipo Java | Coluna | Restrições |
| :--- | :--- | :--- | :--- |
| id | Long | id | Chave primária. |
| username | String | username | Único. Não nulo. Máximo 50 caracteres. |
| email | String | email | Único. Não nulo. Máximo 100 caracteres. |
| password | String | password | Não nulo. Contém o hash BCrypt. |
| createdAt | LocalDateTime | created_at | Não nulo. Não atualizável. |
| updatedAt | LocalDateTime | updated_at | — |
| watchedMovies | List<WatchedMovie> | — | Relação LAZY. |

### 2.2. Regras

- O login é feito pelo campo `email`.
- O método `getUsername()` devolve o email. Este método vem da
  interface `UserDetails` do Spring Security.
- A senha é sempre criptografada com BCrypt antes de salvar.
- A entidade implementa a interface `UserDetails`.

### 2.3. Callbacks JPA

| Callback | Ação |
| :--- | :--- |
| `@PrePersist` | Define `createdAt` e `updatedAt`. |
| `@PreUpdate` | Atualiza `updatedAt`. |

### 2.4. Métodos Auxiliares

- `addWatchedMovie(WatchedMovie)`: adiciona um filme à lista e define o dono.
- `removeWatchedMovie(WatchedMovie)`: remove um filme da lista e limpa o dono.

## 3. Entidade WatchedMovie

A entidade `WatchedMovie` mapeia a tabela `watched_movies`.

### 3.1. Campos

| Campo | Tipo Java | Coluna | Restrições |
| :--- | :--- | :--- | :--- |
| id | Long | id | Chave primária. |
| tmdbId | Long | tmdb_id | Não nulo. ID do filme no TMDB. |
| title | String | title | Não nulo. Máximo 255 caracteres. |
| releaseYear | Integer | release_year | — |
| director | String | director | Máximo 150 caracteres. |
| overview | String | overview | Tipo TEXT. |
| posterPath | String | poster_path | Máximo 250 caracteres. |
| mainActors | String | main_actors | Tipo TEXT. Nomes separados por vírgula. |
| comment | String | comment | Tipo TEXT. Comentário do usuário. |
| watchedAt | LocalDateTime | watched_at | Não nulo. Não atualizável. |
| user | User | user_id | Chave estrangeira. Não nulo. Relação LAZY. |

### 3.2. Regras

- Cada par `(user_id, tmdb_id)` é único. O banco garante esta regra.
- `watchedAt` é definido no momento da inserção. O valor nunca muda.
- A ordenação padrão é do mais antigo para o mais novo.

### 3.3. Índices

| Nome | Colunas | Finalidade |
| :--- | :--- | :--- |
| `idx_watched_movies_user` | user_id | Acelera consultas por usuário. |
| `idx_watched_movies_user_watched_at` | user_id, watched_at | Acelera a consulta principal. |

### 3.4. Callbacks JPA

| Callback | Ação |
| :--- | :--- |
| `@PrePersist` | Define `watchedAt`. |

### 3.5. Métodos Auxiliares

- `attachTo(User)`: define o dono do registro.
- `detachFromUser()`: remove o dono do registro.

## 4. Relacionamento

O relacionamento entre `User` e `WatchedMovie` é do tipo um-para-muitos.

| Lado | Anotação | Fetch |
| :--- | :--- | :--- |
| User | `@OneToMany` | LAZY |
| WatchedMovie | `@ManyToOne` | LAZY |

As duas relações usam `FetchType.LAZY`. Ou seja, o Hibernate só carrega
a relação quando o código acessa a relação.

O lado `User` tem `cascade = CascadeType.ALL` e `orphanRemoval = true`.
Assim, ao deletar um usuário, o banco deleta todos os filmes dele.

## 5. Convenções

- Nomes de tabela usam snake_case no plural.
- Nomes de coluna usam snake_case.
- Toda entidade tem uma chave primária `id` do tipo `Long`.
- Toda entidade usa `@EqualsAndHashCode(of = "id")`.
- Nenhuma entidade usa `@Data` do Lombok.

## 6. Referências

- `docs/architecture.md` — Arquitetura geral.
- `docs/decisions.md` — Decisões sobre as entidades.