# Sistema Java – Auth, JWT, Redis, Postgres, MailHog

Backend em Spring Boot (Java 17) com:
- Postgres (JPA/Hibernate)
- Autenticação JWT (access token) + Refresh token persistente em Redis
- Blacklist de JWT em Redis (logout)
- E-mails via MailHog (ativação de conta, recuperação de e-mail e senha)
- Fluxos: registro, login, ativação, recuperação de e-mail por CPF, reset de senha (email+CPF)
- Perfil do usuário (dashboard): nome, sobrenome, nascimento, avatar; CPF único (no registro)
- Papéis: USER, ASSOCIADO, COLABORADOR, PARCEIRO, FUNDADOR, ADMIN (primeiro usuário criado é ADMIN)
- CSRF habilitado (CookieCsrfTokenRepository) para proteção de formulários

## Requisitos
- Docker e Docker Compose (plugin compose)

Opcional (para rodar testes localmente sem Docker):
- Maven 3.9+
- Java 17+

## Como subir com Docker

1) Build e subir serviços (db, redis, mailhog, app):
```bash
docker compose up -d --build
```

2) Ver logs da aplicação:
```bash
docker compose logs -f app | cat
```

3) Health check:
```bash
curl -s http://localhost:8080/actuator/health
```

4) Serviços úteis:
- MailHog UI: http://localhost:8025 (visualiza e-mails)
- Postgres: localhost:5432 (user: app / pass: app / db: appdb)
- Redis: localhost:6379

O perfil `docker` é ativado pelo compose, com configurações em `src/main/resources/application.yml`.

## Configurações principais
- `app.security.jwt.secret`: segredo do JWT (string longa)
- `app.security.jwt.expiration`: validade do access token (ms)
- `app.security.jwt.refreshExpiration`: validade do refresh token (ms, padrão 30 dias)
- `app.frontendBaseUrl`: base usada em links enviados por e-mail

## Endpoints e fluxos
Base: `http://localhost:8080`

### CSRF (para formulários)
- GET `/csrf` → body: `{ headerName, parameterName, token }`, além do cookie CSRF.

Exemplo (PUT com JWT + CSRF):
```bash
TOKEN=$(curl -s http://localhost:8080/csrf | jq -r .token)
curl -X PUT http://localhost:8080/users/me \
  -H 'Content-Type: application/json' \
  -H "X-CSRF-TOKEN: $TOKEN" \
  -H "Authorization: Bearer SEU_JWT" \
  -d '{"firstName":"Nome","lastName":"Sobrenome","birthDate":"1990-01-01","avatarUrl":"https://..."}'
```

### Registro e ativação
- POST `/auth/register`
  - body: `{ "email", "password", "cpf" }` (CPF 11 dígitos, único)
  - envia e-mail com link `app.frontendBaseUrl/activate?token=...`
- POST `/auth/activate` → `{ "token" }`
- POST `/auth/resend-activation` → `{ "email" }`

### Login, refresh e logout
- POST `/auth/login` → `{ "email", "password" }` → `{ "token": <JWT>, "refreshToken": <UUID> }`
- POST `/auth/refresh` → `{ "email", "refreshToken" }` → `{ "token": <JWT> }`
- POST `/auth/logout` (opcional: Bearer + refresh para revogar)
  - header: `Authorization: Bearer <JWT>` (opcional)
  - body: `{ "email", "refreshToken" }` (opcional)
  - ação: adiciona JWT à blacklist em Redis e revoga o refresh informado

### Recuperação de e-mail e senha
- POST `/auth/recover-email` → `{ "cpf" }` (envia e-mail com o endereço cadastrado)
- POST `/auth/password/reset-request` → `{ "email", "cpf" }` (envia e-mail com token)
- POST `/auth/password/reset` → `{ "token", "newPassword", "cpf" }`

### Dashboard de usuário
- GET `/users/me` → retorna perfil do logado (JWT)
- PUT `/users/me` → atualiza `firstName`, `lastName`, `birthDate`, `avatarUrl` (JWT + CSRF)

### Papéis
- Primeiro usuário cadastrado no banco recebe `ADMIN`; os demais `USER`.
- Para proteger rotas por papel, ajuste `SecurityConfig` (ex.: `.requestMatchers("/admin/**").hasRole("ADMIN")`).

## Executar testes

Com Maven local:
```bash
mvn -q -DskipTests=false test
```

Sem Maven local (Docker):
```bash
docker run --rm \
  -v "$PWD":/workspace \
  -w /workspace \
  maven:3.9-eclipse-temurin-17 \
  mvn -q -DskipTests=false test
```

Testes unitários incluídos:
- Regras de papéis no registro (primeiro ADMIN, demais USER)
- Blacklist de JWT (Redis)
- Refresh tokens (emissão/validação em Redis)

Arquivos principais de teste:
- `src/test/java/com/example/sistemajava/user/UserServiceTest.java`
- `src/test/java/com/example/sistemajava/security/JwtBlacklistServiceTest.java`
- `src/test/java/com/example/sistemajava/security/RefreshTokenServiceTest.java`

## Exemplos rápidos (curl)

Registro:
```bash
curl -s -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@exemplo.com","password":"SenhaForte123","cpf":"12345678901"}'
```

Ativar conta (copie do MailHog UI http://localhost:8025):
```bash
curl -s -X POST http://localhost:8080/auth/activate \
  -H 'Content-Type: application/json' \
  -d '{"token":"COLE_O_TOKEN"}'
```

Login:
```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@exemplo.com","password":"SenhaForte123"}'
```

Refresh:
```bash
curl -s -X POST http://localhost:8080/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@exemplo.com","refreshToken":"SEU_REFRESH"}'
```

Logout (JWT + refresh):
```bash
curl -s -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer SEU_JWT" \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@exemplo.com","refreshToken":"SEU_REFRESH"}'
```

Reset – solicitar (email + CPF):
```bash
curl -s -X POST http://localhost:8080/auth/password/reset-request \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@exemplo.com","cpf":"12345678901"}'
```

Reset – confirmar (token + CPF):
```bash
curl -s -X POST http://localhost:8080/auth/password/reset \
  -H 'Content-Type: application/json' \
  -d '{"token":"COLE_O_TOKEN","newPassword":"NovaSenha123","cpf":"12345678901"}'
```

Dashboard – obter CSRF e atualizar perfil:
```bash
CSRF=$(curl -s http://localhost:8080/csrf | jq -r .token)
curl -s -X PUT http://localhost:8080/users/me \
  -H 'Content-Type: application/json' \
  -H "X-CSRF-TOKEN: $CSRF" \
  -H "Authorization: Bearer SEU_JWT" \
  -d '{"firstName":"Nome","lastName":"Sobrenome","birthDate":"1990-01-01","avatarUrl":"https://..."}'
```

## Dicas
- Em produção, mova segredos para variáveis de ambiente/secret manager.
- Ajuste `app.frontendBaseUrl` para seu frontend real.
- Habilite HTTPS e configure CORS conforme necessário.
