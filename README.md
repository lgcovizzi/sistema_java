# Sistema Java – Auth, JWT (RSA), Redis, Postgres, MailHog, Filas e Dashboard

Backend em Spring Boot (Java 17) com:
- Postgres (JPA/Hibernate)
- JWT RS256 com chaves RSA persistidas (geradas/validadas na inicialização)
- Refresh token em Redis e blacklist de JWT (logout)
- E-mails via MailHog enviados por fila assíncrona com rate limit (1/min)
- Fluxos: registro, login, ativação, recuperação de e-mail por CPF, reset de senha (email+CPF)
- Perfil do usuário (dashboard): nome, sobrenome, nascimento, avatar (crop/resize 300x300 em fila); CPF único (no registro)
- Papéis: USER, ASSOCIADO, COLABORADOR, PARCEIRO, FUNDADOR, ADMIN (primeiro usuário criado é ADMIN)
- CSRF baseado em JWT (endpoint /csrf)
- Página inicial (/) com notícias (Thymeleaf) e links para login/registro

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
- Página inicial: http://localhost:8080/

O perfil `docker` é ativado pelo compose, com configurações em `src/main/resources/application.yml`.

## Configurações principais
- `app.security.jwt.expiration`: validade do access token (ms)
- `app.security.jwt.refreshExpiration`: validade do refresh token (ms, padrão 30 dias)
- `app.frontendBaseUrl`: base usada em links enviados por e-mail
- `app.uploadsDir` e `app.uploadsTmpDir`: diretórios de uploads
- `app.security.keysDir`: diretório das chaves RSA

## Endpoints e fluxos
Base: `http://localhost:8080`

### CSRF (para formulários)
- GET `/csrf` → body: `{ headerName, parameterName, token }` (token JWT RS256)

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
- POST `/auth/resend-activation` → `{ "email" }` (rate limit: 1/min)

### Login, refresh e logout
- POST `/auth/login` → `{ "email", "password" }` → `{ "token": <JWT>, "refreshToken": <UUID> }`
- POST `/auth/refresh` → `{ "email", "refreshToken" }` → `{ "token": <JWT> }`
- POST `/auth/logout` (opcional: Bearer + refresh para revogar)
  - header: `Authorization: Bearer <JWT>` (opcional)
  - body: `{ "email", "refreshToken" }` (opcional)
  - ação: adiciona JWT à blacklist em Redis e revoga o refresh informado

### Recuperação de e-mail e senha
- POST `/auth/recover-email` → `{ "cpf" }` (envia e-mail com o endereço cadastrado; rate limit: 1/min)
- POST `/auth/password/reset-request` → `{ "email", "cpf" }` (envia e-mail com token; rate limit: 1/min)
- POST `/auth/password/reset` → `{ "token", "newPassword", "cpf" }`

### Dashboard de usuário
- GET `/users/me` → retorna perfil do logado (JWT)
- PUT `/users/me` → atualiza `firstName`, `lastName`, `birthDate`, `avatarUrl` (JWT + CSRF)
- POST `/users/me/avatar` (multipart) → enfileira processamento (202 Accepted)
  - Tipos aceitos: image/jpeg, image/png; tamanho até 5MB
  - Params opcionais: `x,y,w,h` para crop manual; saída final 300x300

### Papéis e dashboards
- Primeiro usuário recebe `ADMIN`; demais `USER`.
- Dashboards por papel: `/dashboard/user|associado|colaborador|parceiro|fundador|admin`
- Admin: `/admin/users/**` (listar, detalhar, alterar papel, ativar/desativar, excluir)

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
- Rate limit e fila de e-mails

Arquivos principais de teste:
- `src/test/java/com/example/sistemajava/user/UserServiceTest.java`
- `src/test/java/com/example/sistemajava/security/JwtBlacklistServiceTest.java`
- `src/test/java/com/example/sistemajava/security/RefreshTokenServiceTest.java`
- `src/test/java/com/example/sistemajava/email/EmailServiceTest.java`

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

Upload de avatar (assíncrono):
```bash
CSRF=$(curl -s http://localhost:8080/csrf | jq -r .token)
curl -s -X POST http://localhost:8080/users/me/avatar \
  -H "Authorization: Bearer SEU_JWT" \
  -H "X-CSRF-TOKEN: $CSRF" \
  -F "file=@/caminho/para/foto.jpg" \
  -F "x=10" -F "y=10" -F "w=600" -F "h=600"
```

## Dicas
- Em produção, mova segredos para variáveis de ambiente/secret manager.
- Ajuste `app.frontendBaseUrl` para seu frontend real.
- Habilite HTTPS e configure CORS conforme necessário.
- Persistência de chaves RSA em `keys/` (montado no Docker)
- Volumes: `./uploads` e `./tmp-uploads` mapeados
- Em dev, o Redis pode alertar sobre overcommit de memória (opcional ajustar sysctl)
