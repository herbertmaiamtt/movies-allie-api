# Registros de Decisão de Arquitetura — Movies Allie

Este arquivo lista as decisões de arquitetura do projeto.
Cada decisão segue o formato ADR (Architecture Decision Record).

Um ADR tem quatro partes:

- **Contexto**: o problema ou a situação.
- **Decisão**: a escolha feita.
- **Motivo**: a justificativa da escolha.
- **Consequências**: os efeitos da escolha.

---

## ADR-001: Usar Spring Boot 3.4.1

**Contexto**
O projeto precisa de um framework backend estável e moderno.

**Decisão**
Usar Spring Boot na versão 3.4.1.

**Motivo**
A versão 3.4.1 é estável. A versão 4.x ainda não é estável para produção.
A versão 3.4.1 é compatível com Java 21.

**Consequências**
- A documentação e o ecossistema são compatíveis.
- A migração para a versão 4.x será necessária no futuro.

---

## ADR-002: Usar SQLite em desenvolvimento e PostgreSQL em produção

**Contexto**
O projeto precisa de um banco simples em desenvolvimento e robusto em produção.

**Decisão**
Usar SQLite no perfil `dev` e PostgreSQL no perfil `prod`.

**Motivo**
- SQLite não exige servidor. Isso acelera o desenvolvimento.
- PostgreSQL é o padrão de mercado para produção.
- Os perfis do Spring Boot permitem trocar o banco sem mudar o código.

**Consequências**
- O SQLite permite apenas um escritor por vez.
  O pool de conexões é limitado a 1.
- As migrações Flyway usam placeholders para suportar os dois dialetos.

---

## ADR-003: Usar JWT para autenticação

**Contexto**
A aplicação precisa autenticar usuários em uma API REST.

**Decisão**
Usar tokens JWT com a biblioteca JJWT 0.12.6.

**Motivo**
- JWT é stateless. O servidor não guarda sessão.
- JWT é o padrão de mercado para APIs REST.
- JJWT é uma biblioteca madura e bem documentada.

**Consequências**
- O frontend guarda o token e o envia em cada requisição.
- O backend valida o token a cada requisição.
- A chave secreta precisa ser forte e vir de variável de ambiente.

---

## ADR-004: Usar a API do TMDB como fonte de dados

**Contexto**
O projeto precisa de dados de filmes: título, sinopse, pôster, elenco.

**Decisão**
Usar a API do TMDB.

**Motivo**
- O TMDB é gratuito para uso não comercial.
- O TMDB oferece pôsteres de alta qualidade.
- O TMDB oferece o parâmetro `append_to_response`.
  Este parâmetro reduz o número de requisições.

**Consequências**
- O backend precisa de um token do TMDB.
- O backend deve atribuir o TMDB como fonte.
- O backend deve tratar erros de integração.

---

## ADR-005: Guardar apenas o caminho relativo do pôster

**Contexto**
O TMDB devolve apenas o caminho relativo do pôster.

**Decisão**
Guardar no banco apenas o caminho relativo (`poster_path`).
Montar a URL completa na camada de serviço.

**Motivo**
- A URL base é uma constante da aplicação.
- Se a URL base mudar, apenas um ponto do código precisa mudar.
- O banco fica menor.

**Consequências**
- O mapper monta a URL completa antes de devolver o DTO.
- O frontend recebe a URL pronta.

---

## ADR-006: Usar relações LAZY por padrão

**Contexto**
As entidades `User` e `WatchedMovie` têm um relacionamento.

**Decisão**
Usar `FetchType.LAZY` em ambos os lados do relacionamento.

**Motivo**
- LAZY evita o problema N+1.
- LAZY carrega a relação apenas quando o código a acessa.
- O backend sempre tem o `userId` em mãos. Ele não precisa do objeto `User` populado.

**Consequências**
- O código não pode acessar a relação fora de uma transação.
- O `spring.jpa.open-in-view` está desabilitado.
- As consultas usam métodos diretos do repositório.

---

## ADR-007: Não usar `@Data` do Lombok em entidades JPA

**Contexto**
O Lombok oferece a anotação `@Data`.

**Decisão**
Não usar `@Data` em entidades JPA.
Usar `@Getter`, `@Setter`, `@EqualsAndHashCode(of = "id")` e `@ToString`.

**Motivo**
- `@Data` gera `toString` com todos os campos.
  Isso pode vazar a senha nos logs.
- `@Data` gera `toString` com relações.
  Isso pode causar `StackOverflowError`.
- `@Data` gera `equals` e `hashCode` com todos os campos.
  Isso pode disparar consultas LAZY.

**Consequências**
- O código fica mais seguro.
- O código fica mais previsível.

---

## ADR-008: Usar restrição única composta em `WatchedMovie`

**Contexto**
Um usuário não pode marcar o mesmo filme como assistido duas vezes.

**Decisão**
Criar uma restrição única composta em `(user_id, tmdb_id)`.

**Motivo**
- A validação no service não é suficiente.
  Duas requisições simultâneas podem passar pela validação.
- A restrição no banco é a única garantia real.

**Consequências**
- O banco rejeita duplicatas com uma exceção.
- O `GlobalExceptionHandler` traduz a exceção em HTTP 409.
- O service verifica duplicatas antes de salvar, como otimização.

---

## ADR-009: Não expor entidades JPA na API

**Contexto**
As entidades JPA têm relações e campos internos.

**Decisão**
Usar DTOs em todas as respostas da API.

**Motivo**
- Evitar o `LazyInitializationException` na serialização.
- Evitar expor a senha do usuário.
- Manter o frontend desacoplado das entidades.
- Permitir mudanças no banco sem quebrar o frontend.

**Consequências**
- Cada entidade tem um DTO de resposta.
- O MapStruct converte entidades em DTOs.
- O código tem mais classes, mas é mais seguro.

---

## ADR-010: Usar record para DTOs

**Contexto**
Os DTOs são classes simples de transporte de dados.

**Decisão**
Usar `record` para todos os DTOs.

**Motivo**
- `record` é imutável por padrão.
- `record` gera `equals`, `hashCode` e `toString` automaticamente.
- `record` reduz o código boilerplate.
- `record` é o padrão moderno do Java 21.

**Consequências**
- Os DTOs não têm setters.
- Os DTOs são criados pelo construtor.
- Os DTOs são seguros para uso concorrente.

---

## ADR-011: Usar `RestClient` para consumir o TMDB

**Contexto**
O backend precisa consumir uma API externa.

**Decisão**
Usar o `RestClient` do Spring 6.

**Motivo**
- `RestClient` é a alternativa moderna ao `RestTemplate`.
- `RestClient` tem uma API fluente e simples.
- `RestClient` não exige dependências extras.

**Consequências**
- O `RestClientConfig` define um bean `RestClient`.
- O `TmdbClient` usa este bean.
- A troca para outro cliente HTTP é simples.

---

## ADR-012: Documentar o projeto em `docs/`

**Contexto**
O projeto precisa de documentação clara para o portfólio.

**Decisão**
Manter a documentação em arquivos Markdown na pasta `docs/`.

**Motivo**
- Markdown é legível no GitHub.
- A documentação fica junto do código.
- A documentação segue o padrão ASD-STE100.
- A documentação é imune a problemas com chats.

**Consequências**
- Cada área tem um arquivo próprio.
- As decisões ficam registradas em `decisions.md`.
- O portfólio demonstra maturidade profissional.