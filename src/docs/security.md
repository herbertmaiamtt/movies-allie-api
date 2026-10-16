# Segurança — Movies Allie

## 1. Visão Geral

A aplicação usa Spring Security com tokens JWT.
JWT significa JSON Web Token.

A aplicação é **stateless**. O servidor não guarda sessão.
Cada requisição protegida envia um token no cabeçalho `Authorization`.

## 2. Fluxo de Autenticação

### 2.1. Cadastro

1. O usuário envia `username`, `email` e `password` para `/api/auth/register`.
2. O backend verifica se o email ou o username já existem.
3. O backend criptografa a senha com BCrypt.
4. O backend salva o usuário no banco.
5. O backend devolve os dados do usuário. A senha não é devolvida.

### 2.2. Login

1. O usuário envia `email` e `password` para `/api/auth/login`.
2. O `AuthenticationManager` verifica as credenciais.
3. O backend gera um token JWT.
4. O backend devolve o token para o frontend.

### 2.3. Requisição Protegida

1. O frontend envia o token no cabeçalho `Authorization: Bearer <token>`.
2. O `JwtAuthenticationFilter` intercepta a requisição.
3. O filtro valida a assinatura do token.
4. O filtro carrega o usuário do banco.
5. O filtro coloca o usuário no `SecurityContext`.
6. O Controller acessa o usuário com `@AuthenticationPrincipal`.

## 3. Componentes

| Componente | Pacote | Responsabilidade |
| :--- | :--- | :--- |
| JwtService | security | Gera e valida tokens JWT. |
| JwtProperties | security | Mapeia as propriedades `jwt.*`. |
| JwtAuthenticationFilter | security | Intercepta e valida requisições. |
| CustomUserDetailsService | security | Carrega usuários do banco. |
| SecurityConfig | config | Define as regras de autorização. |
| ApplicationConfig | config | Define o `AuthenticationProvider`. |
| PasswordEncoderConfig | config | Define o `PasswordEncoder`. |

## 4. Token JWT

### 4.1. Estrutura

O token tem três partes separadas por ponto:

1. **Header**: contém o algoritmo de assinatura.
2. **Payload**: contém o email do usuário e as datas.
3. **Signature**: garante a integridade do token.

### 4.2. Propriedades

| Propriedade | Descrição |
| :--- | :--- |
| `jwt.secret` | Chave secreta em Base64. Mínimo 32 bytes. |
| `jwt.expiration` | Tempo de validade em milissegundos. Padrão: 86400000 (24 horas). |

### 4.3. Regras

- O token usa o algoritmo HS256.
- A chave secreta vem de uma variável de ambiente.
- A chave secreta nunca é salva no código.
- O token é assinado com a chave secreta.

## 5. Regras de Autorização

O `SecurityConfig` define as rotas públicas e as rotas protegidas.

| Rota | Acesso |
| :--- | :--- |
| `/api/auth/**` | Público. |
| `/api/movies/**` | Público. |
| `/v3/api-docs/**`, `/swagger-ui/**` | Público. |
| Qualquer outra rota | Requer autenticação. |

## 6. Filtro JWT

O `JwtAuthenticationFilter` segue esta sequência:

1. Lê o cabeçalho `Authorization`.
2. Verifica se o cabeçalho começa com `Bearer `.
3. Extrai o token.
4. Extrai o email do token.
5. Carrega o usuário do banco.
6. Valida o token contra o usuário.
7. Coloca o usuário no `SecurityContext`.
8. Continua a cadeia de filtros.

Se qualquer passo falhar, o filtro não autentica.
O `SecurityConfig` decide se a rota exige autenticação.

## 7. Criptografia de Senha

A aplicação usa BCrypt para criptografar senhas.
BCrypt aplica um hash adaptativo com salt.

O `PasswordEncoder` é definido como um bean.
A criptografia acontece no `AuthServiceImpl`, antes de salvar o usuário.

## 8. Boas Práticas Aplicadas

- A aplicação é stateless.
- O CSRF está desabilitado. Motivo: a aplicação não usa cookies de sessão.
- A chave secreta vem de variável de ambiente.
- A senha nunca é devolvida nas respostas.
- O token é validado em toda requisição protegida.
- O usuário é carregado do banco a cada requisição.

## 9. Referências

- `docs/architecture.md` — Arquitetura geral.
- `docs/decisions.md` — Decisões sobre segurança.