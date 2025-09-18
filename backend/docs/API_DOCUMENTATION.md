# Documentação da API - Sistema Java Backend

## 📋 Visão Geral

Esta documentação detalha todos os endpoints disponíveis na API REST do Sistema Java Backend, incluindo autenticação, validações, exemplos de uso e códigos de resposta.

## 🔐 Autenticação

### Tipos de Autenticação

1. **Endpoints Públicos**: Não requerem autenticação
2. **Endpoints Protegidos**: Requerem token JWT no header `Authorization`

### Formato do Token

```http
Authorization: Bearer <jwt_token>
```

### Obtenção do Token

O token JWT é obtido através do endpoint de login e deve ser incluído em todas as requisições para endpoints protegidos.

## 📚 Endpoints

### 🔓 Autenticação

#### POST /api/auth/register

Registra um novo usuário no sistema.

**Endpoint**: `POST /api/auth/register`  
**Autenticação**: Não requerida  
**Content-Type**: `application/json`

**Parâmetros do Body**:

```json
{
  "nome": "string (obrigatório, 2-100 caracteres)",
  "email": "string (obrigatório, formato email válido)",
  "cpf": "string (obrigatório, CPF válido)",
  "telefone": "string (obrigatório, formato brasileiro)",
  "senha": "string (obrigatório, mín. 8 caracteres)"
}
```

**Exemplo de Requisição**:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "João Silva",
    "email": "joao.silva@example.com",
    "cpf": "12345678901",
    "telefone": "11999999999",
    "senha": "MinhaSenh@123"
  }'
```

**Resposta de Sucesso (201)**:

```json
{
  "error": false,
  "message": "Usuário registrado com sucesso",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
    "refreshToken": "def50200...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": 1,
      "nome": "João Silva",
      "email": "joao.silva@example.com",
      "cpf": "123.456.789-01",
      "telefone": "(11) 99999-9999",
      "emailVerificado": false,
      "role": "USER",
      "criadoEm": "2024-01-15T10:30:00Z"
    }
  }
}
```

**Possíveis Erros**:

- `400` - Dados de entrada inválidos
- `409` - Email ou CPF já cadastrado
- `500` - Erro interno do servidor

---

#### POST /api/auth/login

Autentica um usuário existente.

**Endpoint**: `POST /api/auth/login`  
**Autenticação**: Não requerida  
**Content-Type**: `application/json`

**Parâmetros do Body**:

```json
{
  "email": "string (obrigatório)",
  "senha": "string (obrigatório)",
  "captcha": "string (opcional, obrigatório após 3 tentativas)"
}
```

**Exemplo de Requisição**:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao.silva@example.com",
    "senha": "MinhaSenh@123"
  }'
```

**Resposta de Sucesso (200)**:

```json
{
  "error": false,
  "message": "Login realizado com sucesso",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
    "refreshToken": "def50200...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": 1,
      "nome": "João Silva",
      "email": "joao.silva@example.com",
      "emailVerificado": true,
      "role": "USER"
    }
  }
}
```

**Possíveis Erros**:

- `401` - Credenciais inválidas
- `423` - Conta bloqueada por tentativas excessivas
- `428` - Captcha obrigatório

---

#### POST /api/auth/verify-email

Verifica o email do usuário através do token enviado por email.

**Endpoint**: `POST /api/auth/verify-email`  
**Autenticação**: Não requerida  
**Content-Type**: `application/json`

**Parâmetros do Body**:

```json
{
  "token": "string (obrigatório, token de verificação)"
}
```

**Exemplo de Requisição**:

```bash
curl -X POST http://localhost:8080/api/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{
    "token": "abc123def456..."
  }'
```

**Resposta de Sucesso (200)**:

```json
{
  "error": false,
  "message": "Email verificado com sucesso",
  "data": {
    "verified": true,
    "user": {
      "id": 1,
      "email": "joao.silva@example.com",
      "emailVerificado": true
    }
  }
}
```

---

#### POST /api/auth/resend-verification

Reenvia o email de verificação.

**Endpoint**: `POST /api/auth/resend-verification`  
**Autenticação**: Não requerida  
**Content-Type**: `application/json`

**Parâmetros do Body**:

```json
{
  "email": "string (obrigatório)"
}
```

**Exemplo de Requisição**:

```bash
curl -X POST http://localhost:8080/api/auth/resend-verification \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao.silva@example.com"
  }'
```

**Resposta de Sucesso (200)**:

```json
{
  "error": false,
  "message": "Email de verificação reenviado com sucesso",
  "data": {
    "sent": true,
    "email": "joao.silva@example.com"
  }
}
```

---

#### POST /api/auth/refresh-token

Renova o token de acesso usando o refresh token.

**Endpoint**: `POST /api/auth/refresh-token`  
**Autenticação**: Não requerida  
**Content-Type**: `application/json`

**Parâmetros do Body**:

```json
{
  "refreshToken": "string (obrigatório)"
}
```

**Exemplo de Requisição**:

```bash
curl -X POST http://localhost:8080/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "def50200..."
  }'
```

**Resposta de Sucesso (200)**:

```json
{
  "error": false,
  "message": "Token renovado com sucesso",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
    "refreshToken": "ghi78901...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  }
}
```

---

#### POST /api/auth/logout

Realiza logout invalidando o token atual.

**Endpoint**: `POST /api/auth/logout`  
**Autenticação**: Requerida  
**Content-Type**: `application/json`

**Headers**:

```http
Authorization: Bearer <jwt_token>
```

**Exemplo de Requisição**:

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

**Resposta de Sucesso (200)**:

```json
{
  "error": false,
  "message": "Logout realizado com sucesso",
  "data": {
    "loggedOut": true
  }
}
```

---

### 👤 Usuários

#### GET /api/users/profile

Obtém o perfil do usuário autenticado.

**Endpoint**: `GET /api/users/profile`  
**Autenticação**: Requerida

**Headers**:

```http
Authorization: Bearer <jwt_token>
```

**Exemplo de Requisição**:

```bash
curl -X GET http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

**Resposta de Sucesso (200)**:

```json
{
  "error": false,
  "message": "Perfil obtido com sucesso",
  "data": {
    "id": 1,
    "nome": "João Silva",
    "email": "joao.silva@example.com",
    "cpf": "123.456.789-01",
    "telefone": "(11) 99999-9999",
    "emailVerificado": true,
    "role": "USER",
    "criadoEm": "2024-01-15T10:30:00Z",
    "atualizadoEm": "2024-01-15T10:30:00Z"
  }
}
```

---

#### PUT /api/users/profile

Atualiza o perfil do usuário autenticado.

**Endpoint**: `PUT /api/users/profile`  
**Autenticação**: Requerida  
**Content-Type**: `application/json`

**Headers**:

```http
Authorization: Bearer <jwt_token>
```

**Parâmetros do Body**:

```json
{
  "nome": "string (opcional, 2-100 caracteres)",
  "telefone": "string (opcional, formato brasileiro)",
  "senhaAtual": "string (obrigatório se alterando senha)",
  "novaSenha": "string (opcional, mín. 8 caracteres)"
}
```

**Exemplo de Requisição**:

```bash
curl -X PUT http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "João Silva Santos",
    "telefone": "11888888888"
  }'
```

**Resposta de Sucesso (200)**:

```json
{
  "error": false,
  "message": "Perfil atualizado com sucesso",
  "data": {
    "id": 1,
    "nome": "João Silva Santos",
    "email": "joao.silva@example.com",
    "telefone": "(11) 88888-8888",
    "atualizadoEm": "2024-01-15T11:30:00Z"
  }
}
```

---

#### DELETE /api/users/account

Exclui a conta do usuário autenticado.

**Endpoint**: `DELETE /api/users/account`  
**Autenticação**: Requerida  
**Content-Type**: `application/json`

**Headers**:

```http
Authorization: Bearer <jwt_token>
```

**Parâmetros do Body**:

```json
{
  "senha": "string (obrigatório, confirmação)",
  "confirmacao": "string (obrigatório, deve ser 'EXCLUIR')"
}
```

**Exemplo de Requisição**:

```bash
curl -X DELETE http://localhost:8080/api/users/account \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "senha": "MinhaSenh@123",
    "confirmacao": "EXCLUIR"
  }'
```

**Resposta de Sucesso (200)**:

```json
{
  "error": false,
  "message": "Conta excluída com sucesso",
  "data": {
    "deleted": true,
    "deletedAt": "2024-01-15T12:00:00Z"
  }
}
```

---

### 🛡️ Captcha

#### GET /api/captcha

Gera um novo captcha.

**Endpoint**: `GET /api/captcha`  
**Autenticação**: Não requerida

**Exemplo de Requisição**:

```bash
curl -X GET http://localhost:8080/api/captcha
```

**Resposta de Sucesso (200)**:

```json
{
  "error": false,
  "message": "Captcha gerado com sucesso",
  "data": {
    "captchaId": "abc123def456",
    "captchaImage": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
    "expiresIn": 300
  }
}
```

---

#### POST /api/captcha/verify

Verifica um captcha.

**Endpoint**: `POST /api/captcha/verify`  
**Autenticação**: Não requerida  
**Content-Type**: `application/json`

**Parâmetros do Body**:

```json
{
  "captchaId": "string (obrigatório)",
  "captchaValue": "string (obrigatório)"
}
```

**Exemplo de Requisição**:

```bash
curl -X POST http://localhost:8080/api/captcha/verify \
  -H "Content-Type: application/json" \
  -d '{
    "captchaId": "abc123def456",
    "captchaValue": "ABCD"
  }'
```

**Resposta de Sucesso (200)**:

```json
{
  "error": false,
  "message": "Captcha verificado com sucesso",
  "data": {
    "valid": true
  }
}
```

---

### 🔧 Utilitários

#### GET /api/health

Verifica o status da aplicação.

**Endpoint**: `GET /api/health`  
**Autenticação**: Não requerida

**Exemplo de Requisição**:

```bash
curl -X GET http://localhost:8080/api/health
```

**Resposta de Sucesso (200)**:

```json
{
  "error": false,
  "message": "Sistema operacional",
  "data": {
    "status": "UP",
    "timestamp": "2024-01-15T10:30:00Z",
    "version": "1.0.0",
    "environment": "development",
    "database": "UP",
    "email": "UP",
    "redis": "UP"
  }
}
```

---

#### GET /api/info

Obtém informações sobre a aplicação.

**Endpoint**: `GET /api/info`  
**Autenticação**: Não requerida

**Exemplo de Requisição**:

```bash
curl -X GET http://localhost:8080/api/info
```

**Resposta de Sucesso (200)**:

```json
{
  "error": false,
  "message": "Informações da aplicação",
  "data": {
    "name": "Sistema Java Backend",
    "version": "1.0.0",
    "description": "Sistema backend completo com autenticação JWT",
    "build": {
      "time": "2024-01-15T08:00:00Z",
      "version": "1.0.0-SNAPSHOT"
    },
    "git": {
      "branch": "main",
      "commit": "abc123def456"
    }
  }
}
```

---

## 📋 Códigos de Status HTTP

### Códigos de Sucesso

| Código | Descrição | Uso |
|--------|-----------|-----|
| `200` | OK | Operação realizada com sucesso |
| `201` | Created | Recurso criado com sucesso |
| `204` | No Content | Operação realizada sem conteúdo de retorno |

### Códigos de Erro do Cliente

| Código | Descrição | Uso |
|--------|-----------|-----|
| `400` | Bad Request | Dados de entrada inválidos |
| `401` | Unauthorized | Não autenticado ou token inválido |
| `403` | Forbidden | Não autorizado para a operação |
| `404` | Not Found | Recurso não encontrado |
| `409` | Conflict | Conflito (ex: email já existe) |
| `422` | Unprocessable Entity | Dados válidos mas não processáveis |
| `423` | Locked | Conta bloqueada |
| `428` | Precondition Required | Captcha obrigatório |
| `429` | Too Many Requests | Rate limit excedido |

### Códigos de Erro do Servidor

| Código | Descrição | Uso |
|--------|-----------|-----|
| `500` | Internal Server Error | Erro interno do servidor |
| `502` | Bad Gateway | Erro de gateway |
| `503` | Service Unavailable | Serviço indisponível |

---

## 🔍 Estrutura de Resposta

### Resposta de Sucesso

```json
{
  "error": false,
  "message": "Mensagem de sucesso",
  "data": {
    // Dados específicos da operação
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Resposta de Erro

```json
{
  "error": true,
  "message": "Mensagem de erro",
  "errorCode": "CODIGO_ERRO",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/endpoint",
  "status": 400,
  "fieldErrors": {
    "campo": "Mensagem de erro específica"
  },
  "additionalData": {
    // Dados adicionais específicos do erro
  }
}
```

---

## 🔐 Validações

### Validação de CPF

- Formato: `12345678901` ou `123.456.789-01`
- Validação de dígitos verificadores
- Rejeição de CPFs conhecidos como inválidos

### Validação de Email

- Formato RFC 5322 compliant
- Verificação de domínio
- Normalização automática

### Validação de Senha

- Mínimo 8 caracteres
- Pelo menos 1 letra maiúscula
- Pelo menos 1 letra minúscula
- Pelo menos 1 número
- Pelo menos 1 caractere especial

### Validação de Telefone

- Formato brasileiro: `(11) 99999-9999`
- Aceita: `11999999999`, `(11)999999999`, etc.
- Validação de DDD válido

---

## 🚀 Rate Limiting

### Limites por Endpoint

| Endpoint | Limite | Janela | Bloqueio |
|----------|--------|--------|----------|
| `/api/auth/login` | 5 tentativas | 15 min | 30 min |
| `/api/auth/register` | 3 tentativas | 1 hora | 1 hora |
| `/api/auth/resend-verification` | 3 tentativas | 1 hora | 1 hora |
| Outros endpoints | 100 req | 1 min | 1 min |

### Headers de Rate Limiting

```http
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 3
X-RateLimit-Reset: 1642248000
```

---

## 📝 Exemplos de Uso

### Fluxo Completo de Registro e Login

```bash
# 1. Registrar usuário
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "João Silva",
    "email": "joao@example.com",
    "cpf": "12345678901",
    "telefone": "11999999999",
    "senha": "MinhaSenh@123"
  }'

# 2. Verificar email (token recebido por email)
curl -X POST http://localhost:8080/api/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{
    "token": "token_recebido_por_email"
  }'

# 3. Fazer login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao@example.com",
    "senha": "MinhaSenh@123"
  }'

# 4. Usar token para acessar perfil
curl -X GET http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer TOKEN_RECEBIDO_NO_LOGIN"
```

### Tratamento de Erros

```javascript
// Exemplo em JavaScript
async function login(email, senha) {
  try {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ email, senha })
    });

    const data = await response.json();

    if (!response.ok) {
      // Tratar erro específico
      if (data.errorCode === 'CAPTCHA_REQUIRED') {
        // Mostrar captcha
        showCaptcha();
      } else if (data.errorCode === 'ACCOUNT_LOCKED') {
        // Mostrar mensagem de conta bloqueada
        showAccountLocked(data.additionalData.remainingSeconds);
      } else {
        // Mostrar erro genérico
        showError(data.message);
      }
      return;
    }

    // Login bem-sucedido
    localStorage.setItem('token', data.data.accessToken);
    localStorage.setItem('refreshToken', data.data.refreshToken);
    
    // Redirecionar para dashboard
    window.location.href = '/dashboard';
    
  } catch (error) {
    console.error('Erro na requisição:', error);
    showError('Erro de conexão. Tente novamente.');
  }
}
```

---

## 🔧 Configuração do Cliente

### Headers Recomendados

```http
Content-Type: application/json
Accept: application/json
User-Agent: SeuApp/1.0.0
X-Requested-With: XMLHttpRequest
```

### Interceptadores de Token

```javascript
// Axios interceptor para adicionar token automaticamente
axios.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  error => Promise.reject(error)
);

// Interceptor para renovar token automaticamente
axios.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          const response = await axios.post('/api/auth/refresh-token', {
            refreshToken
          });
          
          const newToken = response.data.data.accessToken;
          localStorage.setItem('token', newToken);
          
          // Repetir requisição original
          error.config.headers.Authorization = `Bearer ${newToken}`;
          return axios.request(error.config);
        } catch (refreshError) {
          // Refresh falhou, redirecionar para login
          localStorage.removeItem('token');
          localStorage.removeItem('refreshToken');
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);
```

---

**Documentação atualizada em: Janeiro 2024**  
**Versão da API: 1.0.0**