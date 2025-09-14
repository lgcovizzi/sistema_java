# Documentação da API

## Visão Geral

A API do Sistema Java fornece endpoints para monitoramento da aplicação, informações do sistema e testes de conectividade.

**Base URL**: `http://localhost:8080`

## Endpoints

### 1. Página Inicial

#### GET /

Retorna a página inicial da aplicação com links para todos os endpoints disponíveis.

**Resposta**:
```html
Content-Type: text/html

<!DOCTYPE html>
<html>
<head>
    <title>Sistema Java</title>
    <meta charset="UTF-8">
</head>
<body>
    <h1>Sistema Java - Spring Boot</h1>
    <p>Aplicação funcionando corretamente!</p>
    
    <h2>Endpoints Disponíveis:</h2>
    <ul>
        <li><a href="/api/health">Health Check</a></li>
        <li><a href="/api/info">Informações da Aplicação</a></li>
        <li><a href="/api/redis-test">Teste do Redis</a></li>
        <li><a href="/actuator">Actuator</a></li>
    </ul>
</body>
</html>
```

**Exemplo de uso**:
```bash
curl http://localhost:8080/
```

---

### 2. Health Check

#### GET /api/health

Verifica o status de saúde da aplicação.

**Resposta**:
```json
{
  "service": "Sistema Java",
  "version": "1.0.0",
  "status": "UP",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**Códigos de Status**:
- `200 OK`: Aplicação funcionando normalmente

**Exemplo de uso**:
```bash
curl http://localhost:8080/api/health
```

---

### 3. Informações da Aplicação

#### GET /api/info

Retorna informações detalhadas sobre a aplicação e ambiente.

**Resposta**:
```json
{
  "app": {
    "name": "Sistema Java",
    "description": "Sistema completo com Spring Boot",
    "version": "1.0.0"
  },
  "java": {
    "version": "17.0.9",
    "vendor": "Eclipse Adoptium"
  },
  "spring": {
    "profiles": ["docker"]
  }
}
```

**Códigos de Status**:
- `200 OK`: Informações retornadas com sucesso

**Exemplo de uso**:
```bash
curl http://localhost:8080/api/info
```

---

### 4. Teste do Redis

#### GET /api/redis-test

Testa a conectividade e funcionalidade do Redis.

**Resposta**:
```json
{
  "redis": {
    "status": "Connected",
    "test": "Redis is working correctly"
  },
  "operations": {
    "set": "test-key -> test-value",
    "get": "test-value"
  },
  "result": "SUCCESS"
}
```

**Códigos de Status**:
- `200 OK`: Redis funcionando corretamente
- `500 Internal Server Error`: Erro de conectividade com Redis

**Exemplo de uso**:
```bash
curl http://localhost:8080/api/redis-test
```

---

### 5. Actuator Endpoints

#### GET /actuator

Retorna os endpoints disponíveis do Spring Boot Actuator.

**Resposta**:
```json
{
  "_links": {
    "self": {
      "href": "http://localhost:8080/actuator",
      "templated": false
    },
    "health": {
      "href": "http://localhost:8080/actuator/health",
      "templated": false
    },
    "info": {
      "href": "http://localhost:8080/actuator/info",
      "templated": false
    }
  }
}
```

**Endpoints Actuator Disponíveis**:
- `/actuator/health`: Health check detalhado
- `/actuator/info`: Informações da aplicação
- `/actuator/metrics`: Métricas da aplicação

**Exemplo de uso**:
```bash
curl http://localhost:8080/actuator
```

---

## Códigos de Erro Comuns

### 404 Not Found
```json
{
  "timestamp": "2024-01-15T10:30:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "path": "/endpoint-inexistente"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2024-01-15T10:30:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/endpoint"
}
```

---

## Testando a API

### Usando cURL

```bash
# Teste básico de conectividade
curl -i http://localhost:8080/

# Health check
curl -i http://localhost:8080/api/health

# Informações da aplicação
curl -i http://localhost:8080/api/info

# Teste do Redis
curl -i http://localhost:8080/api/redis-test

# Actuator
curl -i http://localhost:8080/actuator
```

### Usando HTTPie

```bash
# Instalação: pip install httpie

# Teste básico
http GET localhost:8080/

# Health check
http GET localhost:8080/api/health

# Informações da aplicação
http GET localhost:8080/api/info

# Teste do Redis
http GET localhost:8080/api/redis-test
```

### Script de Teste Completo

```bash
#!/bin/bash

echo "=== Testando API do Sistema Java ==="

echo "\n1. Testando página inicial..."
curl -s http://localhost:8080/ | head -5

echo "\n2. Testando health check..."
curl -s http://localhost:8080/api/health | jq .

echo "\n3. Testando informações da aplicação..."
curl -s http://localhost:8080/api/info | jq .

echo "\n4. Testando Redis..."
curl -s http://localhost:8080/api/redis-test | jq .

echo "\n5. Testando Actuator..."
curl -s http://localhost:8080/actuator | jq .

echo "\n=== Testes concluídos ==="
```

---

## Monitoramento

### Health Checks

A aplicação fornece health checks em múltiplos níveis:

1. **Aplicação**: `/api/health`
2. **Actuator**: `/actuator/health`
3. **Componentes**: Verificação automática de PostgreSQL e Redis

### Métricas

Acesse `/actuator/metrics` para métricas detalhadas da aplicação, incluindo:
- Uso de memória
- Threads ativas
- Conexões de banco de dados
- Cache hits/misses

### Logs

Para visualizar logs da aplicação:
```bash
docker logs sistema-app --tail 50 -f
```