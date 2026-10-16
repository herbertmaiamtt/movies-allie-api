# Integração com o TMDB — Movies Allie

## 1. Visão Geral

O backend consulta a API do TMDB para obter dados de filmes.
TMDB significa The Movie Database.

O backend nunca expõe o token do TMDB para o frontend.
O token fica apenas no backend, em uma variável de ambiente.

## 2. Autenticação

O TMDB aceita dois métodos de autenticação:

1. Chave de API como parâmetro de consulta.
2. Token Bearer no cabeçalho `Authorization`.

A aplicação usa o método **Bearer Token**.

O token vem da variável de ambiente `TMDB_TOKEN`.
O arquivo `application.properties` lê esta variável.

## 3. Configuração

O `RestClientConfig` cria o bean `RestClient` com as seguintes definições:

| Definição | Valor |
| :--- | :--- |
| URL base | `https://api.themoviedb.org/3` |
| Cabeçalho `Authorization` | `Bearer <token>` |
| Cabeçalho `Accept` | `application/json` |

As propriedades vêm do arquivo `TmdbProperties`.

## 4. Endpoints Consumidos

### 4.1. Busca de Filmes

| Item | Valor |
| :--- | :--- |
| Método | GET |
| Rota | `/search/movie` |
| Parâmetro | `query` (título do filme) |
| Parâmetro | `language` (fixo em `pt-BR`) |

A resposta contém uma lista de resultados.
Cada resultado tem: `id`, `title`, `overview`, `release_date`, `poster_path`.

### 4.2. Detalhes do Filme

| Item | Valor |
| :--- | :--- |
| Método | GET |
| Rota | `/movie/{id}` |
| Parâmetro | `append_to_response=credits` |
| Parâmetro | `language` (fixo em `pt-BR`) |

O parâmetro `append_to_response=credits` traz o elenco e a equipe na
mesma resposta. Assim, o backend faz apenas uma requisição.

A resposta contém os dados do filme e um objeto `credits`.

## 5. DTOs

O backend usa DTOs para representar as respostas do TMDB.

| DTO | Descrição |
| :--- | :--- |
| TmdbSearchResponse | Resposta da busca. Contém uma lista de filmes. |
| TmdbMovieResult | Um filme na lista de resultados. |
| TmdbMovieDetails | Detalhes completos de um filme. |
| TmdbCredits | Objeto com `cast` (elenco) e `crew` (equipe). |
| TmdbCastMember | Um ator do elenco. |
| TmdbCrewMember | Um membro da equipe. |

Os DTOs usam `@JsonProperty` para mapear campos em snake_case.
Exemplo: o campo `release_date` do JSON vira `releaseDate` no DTO.

## 6. Montagem da URL do Pôster

O TMDB devolve apenas o caminho relativo do pôster.
Exemplo: `/IfB9hy4JH1eH6HEfIgIGORXi5h.jpg`.

O backend monta a URL completa com três partes:

1. URL base: `https://image.tmdb.org/t/p/`.
2. Tamanho: `w500` (definido em `tmdb.poster-size`).
3. Caminho: o valor de `poster_path`.

A URL final é salva no campo `posterUrl` do `WatchedMovieResponse`.

## 7. Tratamento de Erros

O `TmdbClient` trata dois tipos de erro.

| Tipo de erro | `TmdbErrorKind` | HTTP devolvido ao cliente |
| :--- | :--- | :--- |
| Erro 4xx (cliente) | `NOT_FOUND` | 404 Not Found |
| Erro 5xx ou timeout (upstream) | `UPSTREAM_ERROR` | 502 Bad Gateway |

O `TmdbClient` escolhe o `kind` ao lançar `TmdbIntegrationException`.
O `GlobalExceptionHandler` traduz o `kind` para o status HTTP — a mensagem
da exceção é usada apenas como texto de resposta, nunca para decidir o status.

A exceção `TmdbIntegrationException` é capturada pelo `GlobalExceptionHandler`.

## 8. Limites de Uso

O TMDB não tem um limite fixo de requisições por segundo.
O TMDB aplica um limite superior para evitar uso abusivo.

Para o escopo do Movies Allie, o limite não é atingido em condições normais.

Boas práticas aplicadas:

- Cache de buscas frequentes (planejado).
- Tratamento de erros 429 (Too Many Requests).

## 9. Atribuição

A API do TMDB é gratuita para uso não comercial.
A aplicação deve atribuir o TMDB como fonte dos dados e imagens.

## 10. Referências

- `docs/architecture.md` — Arquitetura geral.
- `docs/decisions.md` — Decisões sobre a integração.
- Documentação oficial: `https://developer.themoviedb.org/docs`.