# Arquitetura — Movies Allie

## 1. Visão Geral

O Movies Allie é uma aplicação web para registrar filmes assistidos.
O usuário pesquisa um filme, marca o filme como assistido e escreve um comentário.

A aplicação tem duas partes:

- **Backend**: API REST em Java com Spring Boot.
- **Frontend**: aplicação web em Next.js (planejado).

As duas partes se comunicam por HTTP com JSON.

## 2. Estrutura do Backend

O backend segue o padrão em camadas. Cada camada tem uma responsabilidade única.

| Camada | Pacote | Responsabilidade |
| :--- | :--- | :--- |
| Controller | `controller` | Recebe requisições HTTP e devolve respostas. |
| Service | `service` | Contém as regras de negócio. |
| Repository | `repository` | Acessa o banco de dados. |
| Entity | `entity` | Representa as tabelas do banco. |
| DTO | `dto` | Transporta dados entre as camadas. |
| Security | `security` | Autentica e autoriza requisições. |
| Config | `config` | Define os beans da aplicação. |
| Exception | `exception` | Trata os erros da aplicação. |
| Mapper | `mapper` | Converte entidades em DTOs. |

O fluxo de uma requisição é:

1. O Controller recebe a requisição.
2. O Controller chama o Service.
3. O Service usa o Repository para acessar o banco.
4. O Service devolve um DTO para o Controller.
5. O Controller devolve uma resposta JSON.

## 3. Perfis de Execução

A aplicação usa dois perfis do Spring Boot.

| Perfil | Banco de dados | Uso |
| :--- | :--- | :--- |
| `dev` | SQLite | Desenvolvimento local e testes. |
| `prod` | PostgreSQL | Produção. |

O perfil `dev` é o padrão. O arquivo `application.properties` define
`spring.profiles.active=dev`.

O perfil `prod` lê as credenciais do banco de variáveis de ambiente.

## 4. Stack de Tecnologias

| Área | Tecnologia | Versão |
| :--- | :--- | :--- |
| Linguagem | Java | 21 |
| Framework | Spring Boot | 3.4.1 |
| Build | Maven | — |
| Banco (dev) | SQLite | — |
| Banco (prod) | PostgreSQL | — |
| Segurança | Spring Security + JWT | JJWT 0.12.6 |
| Documentação | SpringDoc OpenAPI | 2.8.x |
| API externa | TMDB | v3 |

## 5. Comunicação com a API do TMDB

O backend consulta a API do TMDB para obter dados de filmes.
O backend nunca expõe o token do TMDB para o frontend.

O fluxo é:

1. O frontend chama o endpoint `/api/movies/search`.
2. O backend chama a API do TMDB.
3. O backend devolve a resposta para o frontend.

## 6. Segurança

A aplicação é **stateless**. Ou seja, o servidor não guarda sessão.

Cada requisição protegida envia um token JWT no cabeçalho `Authorization`.
O backend valida o token antes de processar a requisição.

## 7. Deploy (Planejado)

| Componente | Serviço sugerido |
| :--- | :--- |
| Backend | Railway, Render ou Fly.io |
| Frontend | Vercel |
| Banco (prod) | Neon ou Supabase |

## 8. Referências

- `docs/entities.md` — Modelo de dados.
- `docs/security.md` — Autenticação e autorização.
- `docs/tmdb-integration.md` — Integração com a API do TMDB.
- `docs/decisions.md` — Registros de decisão de arquitetura.