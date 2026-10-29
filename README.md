# Movies Allie API 🎬

API RESTful stateless desenvolvida em **Java 21** com **Spring Boot 3.4.1** para catálogo, busca e registro de filmes assistidos, integrada com o **The Movie Database (TMDB)** e autenticação baseada em **JSON Web Tokens (JWT)**.

---

## 🚀 Tecnologias Utilizadas

- **Linguagem**: Java 21 (LTS)
- **Framework**: Spring Boot 3.4.1 (Web, Security, Data JPA, Validation, Cache)
- **Segurança**: Spring Security 6 + JJWT 0.12.6 (HMAC-SHA256) + BCrypt
- **Banco de Dados**:
  - **Desenvolvimento**: SQLite com HikariCP
  - **Produção / Testes**: PostgreSQL com Flyway Cross-Dialect
- **Migrações de Banco**: Flyway 10
- **Integração Externa**: TMDB API v3 via `RestClient` do Spring 6 com cache em memória
- **Mapeamento & Boilerplate**: MapStruct 1.6.3 + Lombok
- **Documentação Interativa**: SpringDoc OpenAPI 2.8.5 (Swagger UI)
- **Testes**: JUnit 5, AssertJ, Mockito, MockMvc, Testcontainers (PostgreSQL 16)

---

## 📋 Pré-requisitos

- **Java JDK 21** instalado (`java -version`)
- **Git** instalado
- **Token de Acesso do TMDB** (gratuito)

---

## 🔑 Como Obter o Token do TMDB

1. Crie uma conta gratuita no [The Movie Database (TMDB)](https://www.themoviedb.org/signup).
2. Acesse as configurações da sua conta: **Configurações > API** ([link direto](https://www.themoviedb.org/settings/api)).
3. Solicite uma chave de API para desenvolvedor.
4. Copie o **Token de Leitura da API (v4 auth)** / *API Read Access Token*.

---

## 💻 Passo a Passo para Execução Local (Localhost)

### 1. Clonar o Repositório
```bash
git clone https://github.com/herbertmaiamtt/movies-allie-api.git
cd movies-allie-api
```

### 2. Configurar as Variáveis de Ambiente
O projeto inclui um modelo pronto [`.env.example`](.env.example). Você pode exportar a variável diretamente no terminal:

```bash
export TMDB_TOKEN="seu_token_de_leitura_do_tmdb_aqui"
```

> **Nota sobre o JWT no perfil local (`dev`)**:  
> O arquivo `application-dev.properties` já possui uma chave segura configurada por padrão (`JWT_SECRET`). Portanto, para rodar localmente no seu computador, **você só precisa definir a variável `TMDB_TOKEN`**.

### 3. Iniciar a Aplicação
Execute o Maven Wrapper na raiz do projeto:

```bash
./mvnw spring-boot:run
```

A aplicação inicializará na porta padrão **8080**:
- O banco de dados SQLite será criado automaticamente em `./data/dev.db`.
- As migrações do Flyway (`V1` e `V2`) criarão as tabelas e índices necessários no primeiro boot.
- O console exibirá: `Started MoviesAllieApiApplication in X.XXX seconds`.

---

## 📖 Documentação Interativa (Swagger UI)

Com o servidor em execução, acesse a documentação interativa e teste todos os endpoints diretamente pelo navegador:

📍 **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
📍 **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Como se autenticar no Swagger UI:
1. Cadastre um usuário em `POST /api/auth/register` ou faça login em `POST /api/auth/login`.
2. Copie o token JWT retornado no campo `token`.
3. No topo da página do Swagger, clique no botão verde **Authorize**.
4. Cole o token no campo de valor e clique em **Authorize**.
5. Todos os endpoints protegidos de `/api/watched-movies` estarão prontos para teste!

---

## 🛣️ Principais Endpoints da API

### Autenticação (Público)
| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Cria uma nova conta com email, username e senha |
| `POST` | `/api/auth/login` | Autentica as credenciais e devolve o token JWT |

### Catálogo de Filmes (Público)
| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `GET` | `/api/movies/search?query={titulo}` | Busca filmes paginados no catálogo do TMDB em português |
| `GET` | `/api/movies/{id}` | Retorna detalhes completos do filme (sinopse, diretor, elenco principal) |

### Filmes Assistidos (Protegido - Exige Token JWT)
| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `POST` | `/api/watched-movies` | Adiciona um filme à lista de assistidos com comentário opcional |
| `GET` | `/api/watched-movies` | Lista todos os filmes assistidos do usuário autenticado |
| `GET` | `/api/watched-movies/search?title={termo}` | Busca filmes assistidos por título na coleção pessoal |
| `GET` | `/api/watched-movies/count` | Retorna o total de filmes assistidos pelo usuário |
| `GET` | `/api/watched-movies/{id}` | Busca um filme assistido específico por ID |
| `PATCH` | `/api/watched-movies/{id}/comment` | Atualiza o comentário pessoal de um filme assistido |
| `DELETE` | `/api/watched-movies/{id}` | Remove um filme da lista de assistidos |

---

## 🧪 Executando os Testes Automatizados

O projeto conta com **95 testes automatizados** cobrindo testes unitários de serviços, resiliência do cliente TMDB, integração de controllers com Spring Security e testes de banco de dados com Testcontainers:

```bash
./mvnw test
```

---

## 📚 Documentação Técnica de Arquitetura

Para detalhes profundos das decisões de design, modelo de dados e diretrizes de desenvolvimento, consulte a pasta [`src/docs/`](src/docs):

- [`architecture.md`](src/docs/architecture.md): Visão geral da arquitetura em camadas e stack.
- [`decisions.md`](src/docs/decisions.md): Registros de Decisão de Arquitetura (ADRs 001 a 012).
- [`entities.md`](src/docs/entities.md): Mapeamento relacional, campos e constraints das entidades.
- [`security.md`](src/docs/security.md): Fluxo de autenticação stateless e ciclo do filtro JWT.
- [`tmdb-integration.md`](src/docs/tmdb-integration.md): Especificação da integração, cache e limites de uso da API do TMDB.

---

## ⚖️ Atribuição Legal

Este produto utiliza a API do The Movie Database (TMDB), mas não é endossado ou certificado pelo TMDB.  
*This product uses the TMDB API but is not endorsed or certified by TMDB.*
