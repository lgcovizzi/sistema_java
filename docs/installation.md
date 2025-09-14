# Guia de Instalação e Configuração

## Pré-requisitos

### Requisitos Mínimos
- **Docker**: 20.10.0 ou superior
- **Docker Compose**: 2.0.0 ou superior
- **Sistema Operacional**: Linux, macOS ou Windows com WSL2
- **Memória RAM**: Mínimo 4GB (recomendado 8GB)
- **Espaço em Disco**: Mínimo 2GB livres

### Verificando Pré-requisitos

```bash
# Verificar versão do Docker
docker --version

# Verificar versão do Docker Compose
docker compose version

# Verificar se o Docker está rodando
docker ps
```

---

## Instalação Rápida

### 1. Clone o Repositório

```bash
git clone <repository-url>
cd sistema_java
```

### 2. Execute o Sistema

```bash
# Iniciar todos os serviços
docker compose up -d

# Verificar status dos serviços
docker compose ps
```

### 3. Verificar Instalação

```bash
# Testar aplicação principal
curl http://localhost:8080/

# Testar health check
curl http://localhost:8080/api/health
```

### 4. Acessar Serviços

- **Aplicação Principal**: http://localhost:8080
- **MailHog Web UI**: http://localhost:8025
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379

---

## Instalação Detalhada

### Estrutura dos Serviços

O sistema é composto por 4 serviços principais:

1. **app**: Aplicação Spring Boot
2. **postgres**: Banco de dados PostgreSQL
3. **redis**: Cache Redis
4. **mailhog**: Servidor de email para testes

### Configuração de Rede

Todos os serviços estão na rede `sistema-network`:

```yaml
networks:
  sistema-network:
    driver: bridge
```

### Volumes Persistentes

```yaml
volumes:
  postgres_data:    # Dados do PostgreSQL
  redis_data:       # Dados do Redis
```

---

## Configurações de Ambiente

### Variáveis de Ambiente

#### Aplicação Spring Boot
```bash
SPRING_PROFILES_ACTIVE=docker
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/sistema_db
SPRING_DATASOURCE_USERNAME=sistema_user
SPRING_DATASOURCE_PASSWORD=sistema_pass
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
SPRING_MAIL_HOST=mailhog
SPRING_MAIL_PORT=1025
```

#### PostgreSQL
```bash
POSTGRES_DB=sistema_db
POSTGRES_USER=sistema_user
POSTGRES_PASSWORD=sistema_pass
```

### Personalizando Configurações

Para personalizar as configurações, crie um arquivo `.env`:

```bash
# .env
DB_NAME=meu_sistema_db
DB_USER=meu_usuario
DB_PASSWORD=minha_senha
APP_PORT=8080
POSTGRES_PORT=5432
REDIS_PORT=6379
MAILHOG_SMTP_PORT=1025
MAILHOG_WEB_PORT=8025
```

E modifique o `docker-compose.yml` para usar essas variáveis:

```yaml
services:
  app:
    ports:
      - "${APP_PORT:-8080}:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/${DB_NAME:-sistema_db}
      - SPRING_DATASOURCE_USERNAME=${DB_USER:-sistema_user}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD:-sistema_pass}
```

---

## Comandos Úteis

### Gerenciamento dos Serviços

```bash
# Iniciar todos os serviços
docker compose up -d

# Parar todos os serviços
docker compose down

# Reiniciar um serviço específico
docker compose restart app

# Ver logs de um serviço
docker compose logs app -f

# Ver status dos serviços
docker compose ps

# Reconstruir e iniciar
docker compose up -d --build
```

### Gerenciamento de Dados

```bash
# Backup do banco de dados
docker compose exec postgres pg_dump -U sistema_user sistema_db > backup.sql

# Restaurar backup
docker compose exec -T postgres psql -U sistema_user sistema_db < backup.sql

# Limpar volumes (CUIDADO: apaga todos os dados)
docker compose down -v
```

### Monitoramento

```bash
# Ver uso de recursos
docker stats

# Inspecionar um container
docker compose exec app bash

# Ver logs em tempo real
docker compose logs -f

# Ver logs de um serviço específico
docker compose logs postgres --tail 50
```

---

## Desenvolvimento Local

### Configuração para Desenvolvimento

Para desenvolvimento, você pode mapear o código fonte como volume:

```yaml
# docker-compose.dev.yml
services:
  app:
    volumes:
      - ./backend/src:/app/src
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - SPRING_DEVTOOLS_RESTART_ENABLED=true
```

```bash
# Usar configuração de desenvolvimento
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

### Hot Reload

Para ativar hot reload durante desenvolvimento:

1. Adicione a dependência no `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

2. Configure o profile de desenvolvimento:
```properties
# application-dev.properties
spring.devtools.restart.enabled=true
spring.devtools.livereload.enabled=true
```

---

## Troubleshooting

### Problemas Comuns

#### 1. Porta já em uso
```bash
# Erro: port is already allocated
# Solução: Verificar processos usando a porta
sudo netstat -tulpn | grep :8080

# Ou mudar a porta no docker-compose.yml
ports:
  - "8081:8080"  # Usar porta 8081 no host
```

#### 2. Aplicação não conecta ao banco
```bash
# Verificar se o PostgreSQL está rodando
docker compose ps postgres

# Verificar logs do PostgreSQL
docker compose logs postgres

# Testar conexão manualmente
docker compose exec postgres psql -U sistema_user -d sistema_db
```

#### 3. Redis não conecta
```bash
# Verificar se o Redis está rodando
docker compose ps redis

# Testar conexão Redis
docker compose exec redis redis-cli ping
```

#### 4. Aplicação não inicia
```bash
# Verificar logs da aplicação
docker compose logs app

# Verificar se todas as dependências estão rodando
docker compose ps

# Reconstruir a imagem
docker compose build app --no-cache
```

### Logs e Debugging

```bash
# Ver todos os logs
docker compose logs

# Logs de um serviço específico
docker compose logs app --tail 100 -f

# Entrar no container para debug
docker compose exec app bash

# Verificar variáveis de ambiente
docker compose exec app env | grep SPRING
```

### Limpeza do Sistema

```bash
# Parar e remover containers
docker compose down

# Remover volumes (apaga dados)
docker compose down -v

# Limpar imagens não utilizadas
docker image prune -f

# Limpeza completa do Docker
docker system prune -a --volumes
```

---

## Configuração de Produção

### Considerações de Segurança

1. **Senhas**: Use senhas fortes e variáveis de ambiente
2. **Rede**: Configure firewall adequadamente
3. **SSL/TLS**: Configure certificados para HTTPS
4. **Backup**: Configure backup automático dos dados

### Exemplo de Configuração de Produção

```yaml
# docker-compose.prod.yml
services:
  app:
    restart: unless-stopped
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_PASSWORD_FILE=/run/secrets/db_password
    secrets:
      - db_password
    
  postgres:
    restart: unless-stopped
    environment:
      - POSTGRES_PASSWORD_FILE=/run/secrets/db_password
    secrets:
      - db_password

secrets:
  db_password:
    file: ./secrets/db_password.txt
```

### Monitoramento em Produção

```bash
# Configurar health checks
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/api/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

---

## Próximos Passos

Após a instalação bem-sucedida:

1. Consulte a [Documentação da API](./api.md)
2. Leia sobre [Configuração Docker](./docker.md)
3. Explore os endpoints em http://localhost:8080
4. Configure monitoramento e alertas
5. Implemente backup automático