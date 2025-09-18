# Guia de Deploy - Sistema Java Backend

## 🚀 Visão Geral

Este documento fornece instruções completas para deploy do Sistema Java Backend em diferentes ambientes, incluindo desenvolvimento, homologação e produção.

## 📋 Índice

1. [Pré-requisitos](#pré-requisitos)
2. [Configuração de Ambiente](#configuração-de-ambiente)
3. [Deploy com Docker](#deploy-com-docker)
4. [Deploy Manual](#deploy-manual)
5. [CI/CD Pipeline](#cicd-pipeline)
6. [Configurações por Ambiente](#configurações-por-ambiente)
7. [Monitoramento e Logs](#monitoramento-e-logs)
8. [Backup e Recuperação](#backup-e-recuperação)
9. [Troubleshooting](#troubleshooting)
10. [Checklist de Deploy](#checklist-de-deploy)

---

## 🔧 Pré-requisitos

### Requisitos de Sistema

| Componente | Versão Mínima | Recomendada | Observações |
|------------|---------------|-------------|-------------|
| **Java** | 17 | 21 LTS | OpenJDK ou Oracle JDK |
| **Maven** | 3.8.0 | 3.9.x | Para build da aplicação |
| **PostgreSQL** | 12 | 15+ | Banco de dados principal |
| **Redis** | 6.0 | 7.0+ | Cache e rate limiting |
| **Docker** | 20.10 | 24.x | Para containerização |
| **Docker Compose** | 2.0 | 2.x | Orquestração local |

### Recursos de Hardware

#### Desenvolvimento
- **CPU**: 2 cores
- **RAM**: 4 GB
- **Disco**: 20 GB SSD
- **Rede**: 10 Mbps

#### Produção
- **CPU**: 4+ cores
- **RAM**: 8+ GB
- **Disco**: 100+ GB SSD
- **Rede**: 100+ Mbps

---

## 🌍 Configuração de Ambiente

### Variáveis de Ambiente

Crie um arquivo `.env` baseado no template:

```bash
# .env.template
# Copie para .env e configure os valores

# === APLICAÇÃO ===
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
APP_NAME=sistema-java-backend
APP_VERSION=1.0.0

# === BANCO DE DADOS ===
DB_HOST=localhost
DB_PORT=5432
DB_NAME=sistema_prod
DB_USERNAME=sistema_user
DB_PASSWORD=senha_super_segura
DB_SSL_MODE=require
DB_MAX_POOL_SIZE=20

# === REDIS ===
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_senha_segura
REDIS_SSL=true
REDIS_TIMEOUT=2000

# === JWT/SEGURANÇA ===
JWT_ACCESS_TOKEN_VALIDITY=86400000
JWT_REFRESH_TOKEN_VALIDITY=604800000
JWT_ISSUER=sistema-java-backend
JWT_AUDIENCE=sistema-java-frontend
RSA_KEYS_DIRECTORY=./keys
RSA_AUTO_GENERATE=true

# === EMAIL ===
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=sistema@empresa.com
EMAIL_PASSWORD=senha_email_app
EMAIL_FROM=sistema@empresa.com
EMAIL_TLS_ENABLED=true

# === CAPTCHA ===
CAPTCHA_ENABLED=true
CAPTCHA_LENGTH=6
CAPTCHA_EXPIRY_MINUTES=5

# === RATE LIMITING ===
RATE_LIMIT_LOGIN_MAX_ATTEMPTS=5
RATE_LIMIT_LOGIN_WINDOW_MINUTES=15
RATE_LIMIT_REGISTER_MAX_ATTEMPTS=3
RATE_LIMIT_REGISTER_WINDOW_MINUTES=60

# === LOGS ===
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
LOG_FILE_PATH=./logs/application.log
LOG_MAX_FILE_SIZE=10MB
LOG_MAX_HISTORY=30

# === SSL (Produção) ===
SSL_ENABLED=true
SSL_KEYSTORE_PATH=/etc/ssl/keystore.p12
SSL_KEYSTORE_PASSWORD=keystore_password
SSL_KEY_ALIAS=sistema-java

# === MONITORAMENTO ===
MANAGEMENT_ENDPOINTS_ENABLED=true
MANAGEMENT_SECURITY_ENABLED=true
METRICS_EXPORT_ENABLED=true

# === CORS ===
CORS_ALLOWED_ORIGINS=https://app.empresa.com,https://admin.empresa.com
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS
CORS_ALLOWED_HEADERS=*
CORS_ALLOW_CREDENTIALS=true
```

### Configuração por Ambiente

#### application-dev.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sistema_dev
    username: dev_user
    password: dev_password
    hikari:
      maximum-pool-size: 5
  
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update
  
  redis:
    host: localhost
    port: 6379
    password: ""
    ssl: false

logging:
  level:
    com.sistema: DEBUG
    org.springframework.security: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

app:
  security:
    require-https: false
  email:
    enabled: false
  captcha:
    enabled: false
```

#### application-prod.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?sslmode=${DB_SSL_MODE}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: ${DB_MAX_POOL_SIZE:20}
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: false
        use_sql_comments: false
  
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}
    ssl: ${REDIS_SSL:true}
    timeout: ${REDIS_TIMEOUT:2000}ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5

server:
  port: ${SERVER_PORT:8080}
  ssl:
    enabled: ${SSL_ENABLED:false}
    key-store: ${SSL_KEYSTORE_PATH}
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: ${SSL_KEY_ALIAS}
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain
  http2:
    enabled: true

logging:
  level:
    root: ${LOG_LEVEL_ROOT:INFO}
    com.sistema: ${LOG_LEVEL_APP:INFO}
  file:
    name: ${LOG_FILE_PATH:./logs/application.log}
    max-size: ${LOG_MAX_FILE_SIZE:10MB}
    max-history: ${LOG_MAX_HISTORY:30}
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{traceId}] %logger{36} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## 🐳 Deploy com Docker

### Dockerfile

```dockerfile
# Dockerfile
FROM openjdk:21-jdk-slim as builder

WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY mvnw .
COPY .mvn ./.mvn

# Build da aplicação
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# Imagem final
FROM openjdk:21-jre-slim

# Instalar dependências do sistema
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Criar usuário não-root
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copiar JAR da aplicação
COPY --from=builder /app/target/*.jar app.jar

# Criar diretórios necessários
RUN mkdir -p logs keys && chown -R appuser:appuser /app

# Configurar usuário
USER appuser

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

# Expor porta
EXPOSE 8080

# Comando de inicialização
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:prod}", \
  "-Xms512m", \
  "-Xmx2g", \
  "-XX:+UseG1GC", \
  "-XX:+UseStringDeduplication", \
  "-jar", \
  "app.jar"]
```

### Docker Compose

#### docker-compose.yml (Produção)
```yaml
version: '3.8'

services:
  app:
    build: .
    container_name: sistema-java-backend
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=postgres
      - REDIS_HOST=redis
    env_file:
      - .env
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    volumes:
      - ./logs:/app/logs
      - ./keys:/app/keys
      - ./ssl:/etc/ssl:ro
    networks:
      - sistema-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  postgres:
    image: postgres:15-alpine
    container_name: sistema-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d:ro
    networks:
      - sistema-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME} -d ${DB_NAME}"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: sistema-redis
    restart: unless-stopped
    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes
    volumes:
      - redis_data:/data
    networks:
      - sistema-network
    healthcheck:
      test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

  nginx:
    image: nginx:alpine
    container_name: sistema-nginx
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
      - ./nginx/logs:/var/log/nginx
    depends_on:
      - app
    networks:
      - sistema-network

volumes:
  postgres_data:
    driver: local
  redis_data:
    driver: local

networks:
  sistema-network:
    driver: bridge
```

#### docker-compose.dev.yml (Desenvolvimento)
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: sistema-postgres-dev
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: sistema_dev
      POSTGRES_USER: dev_user
      POSTGRES_PASSWORD: dev_password
    volumes:
      - postgres_dev_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    container_name: sistema-redis-dev
    ports:
      - "6379:6379"
    volumes:
      - redis_dev_data:/data

volumes:
  postgres_dev_data:
  redis_dev_data:
```

### Comandos Docker

```bash
# Desenvolvimento - apenas dependências
docker-compose -f docker-compose.dev.yml up -d

# Produção - aplicação completa
docker-compose up -d

# Build e deploy
docker-compose up --build -d

# Verificar logs
docker-compose logs -f app

# Verificar status
docker-compose ps

# Parar serviços
docker-compose down

# Parar e remover volumes (CUIDADO!)
docker-compose down -v
```

---

## 🔧 Deploy Manual

### 1. Preparação do Servidor

```bash
# Atualizar sistema
sudo apt update && sudo apt upgrade -y

# Instalar Java 21
sudo apt install openjdk-21-jdk -y

# Verificar instalação
java -version
javac -version

# Instalar PostgreSQL
sudo apt install postgresql postgresql-contrib -y

# Instalar Redis
sudo apt install redis-server -y

# Instalar Nginx (opcional)
sudo apt install nginx -y
```

### 2. Configuração do Banco de Dados

```bash
# Conectar ao PostgreSQL
sudo -u postgres psql

-- Criar banco e usuário
CREATE DATABASE sistema_prod;
CREATE USER sistema_user WITH ENCRYPTED PASSWORD 'senha_super_segura';
GRANT ALL PRIVILEGES ON DATABASE sistema_prod TO sistema_user;
ALTER USER sistema_user CREATEDB;

-- Configurar extensões necessárias
\c sistema_prod
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\q
```

### 3. Configuração do Redis

```bash
# Editar configuração do Redis
sudo nano /etc/redis/redis.conf

# Configurações importantes:
# requirepass senha_redis_segura
# bind 127.0.0.1
# port 6379
# save 900 1
# save 300 10
# save 60 10000

# Reiniciar Redis
sudo systemctl restart redis-server
sudo systemctl enable redis-server
```

### 4. Build e Deploy da Aplicação

```bash
# Clonar repositório
git clone https://github.com/empresa/sistema-java-backend.git
cd sistema-java-backend

# Configurar variáveis de ambiente
cp .env.template .env
nano .env  # Editar com valores de produção

# Build da aplicação
./mvnw clean package -DskipTests

# Criar diretórios necessários
mkdir -p /opt/sistema-java/{logs,keys,config}

# Copiar JAR
sudo cp target/*.jar /opt/sistema-java/app.jar

# Copiar configurações
sudo cp .env /opt/sistema-java/
sudo cp -r keys/ /opt/sistema-java/

# Configurar permissões
sudo chown -R sistema:sistema /opt/sistema-java
sudo chmod +x /opt/sistema-java/app.jar
```

### 5. Configuração do Systemd

```bash
# Criar usuário do sistema
sudo useradd -r -s /bin/false sistema

# Criar arquivo de serviço
sudo nano /etc/systemd/system/sistema-java.service
```

```ini
[Unit]
Description=Sistema Java Backend
After=network.target postgresql.service redis.service
Wants=postgresql.service redis.service

[Service]
Type=simple
User=sistema
Group=sistema
WorkingDirectory=/opt/sistema-java
ExecStart=/usr/bin/java -jar \
  -Dspring.profiles.active=prod \
  -Xms512m \
  -Xmx2g \
  -XX:+UseG1GC \
  -XX:+UseStringDeduplication \
  -Djava.security.egd=file:/dev/./urandom \
  /opt/sistema-java/app.jar
ExecStop=/bin/kill -TERM $MAINPID
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=sistema-java
KillMode=mixed
KillSignal=SIGTERM
TimeoutStopSec=30
EnvironmentFile=/opt/sistema-java/.env

[Install]
WantedBy=multi-user.target
```

```bash
# Habilitar e iniciar serviço
sudo systemctl daemon-reload
sudo systemctl enable sistema-java
sudo systemctl start sistema-java

# Verificar status
sudo systemctl status sistema-java

# Verificar logs
sudo journalctl -u sistema-java -f
```

### 6. Configuração do Nginx (Opcional)

```bash
# Criar configuração do Nginx
sudo nano /etc/nginx/sites-available/sistema-java
```

```nginx
upstream sistema_backend {
    server 127.0.0.1:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name api.empresa.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.empresa.com;

    # SSL Configuration
    ssl_certificate /etc/ssl/certs/sistema.crt;
    ssl_certificate_key /etc/ssl/private/sistema.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options DENY always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # Gzip Compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types application/json application/javascript text/css text/javascript text/xml application/xml application/xml+rss;

    # Rate Limiting
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
    limit_req zone=api burst=20 nodelay;

    location / {
        proxy_pass http://sistema_backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }

    # Health Check
    location /api/health {
        proxy_pass http://sistema_backend;
        access_log off;
    }

    # Static files (if any)
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        proxy_pass http://sistema_backend;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

```bash
# Habilitar site
sudo ln -s /etc/nginx/sites-available/sistema-java /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

---

## 🔄 CI/CD Pipeline

### GitHub Actions

#### .github/workflows/deploy.yml
```yaml
name: Deploy to Production

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: test_password
          POSTGRES_DB: test_db
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
      
      redis:
        image: redis:7
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 6379:6379

    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven
    
    - name: Run tests
      run: ./mvnw test
      env:
        SPRING_PROFILES_ACTIVE: test
        DB_HOST: localhost
        DB_PORT: 5432
        DB_NAME: test_db
        DB_USERNAME: postgres
        DB_PASSWORD: test_password
        REDIS_HOST: localhost
        REDIS_PORT: 6379
    
    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Maven Tests
        path: target/surefire-reports/*.xml
        reporter: java-junit

  build:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven
    
    - name: Build application
      run: ./mvnw clean package -DskipTests
    
    - name: Log in to Container Registry
      uses: docker/login-action@v3
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
    
    - name: Extract metadata
      id: meta
      uses: docker/metadata-action@v5
      with:
        images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
        tags: |
          type=ref,event=branch
          type=ref,event=pr
          type=sha,prefix={{branch}}-
          type=raw,value=latest,enable={{is_default_branch}}
    
    - name: Build and push Docker image
      uses: docker/build-push-action@v5
      with:
        context: .
        push: true
        tags: ${{ steps.meta.outputs.tags }}
        labels: ${{ steps.meta.outputs.labels }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment: production
    
    steps:
    - name: Deploy to production
      uses: appleboy/ssh-action@v1.0.0
      with:
        host: ${{ secrets.PROD_HOST }}
        username: ${{ secrets.PROD_USER }}
        key: ${{ secrets.PROD_SSH_KEY }}
        script: |
          cd /opt/sistema-java
          
          # Pull latest image
          docker pull ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
          
          # Update docker-compose
          docker-compose pull
          docker-compose up -d --no-deps app
          
          # Wait for health check
          sleep 30
          
          # Verify deployment
          curl -f http://localhost:8080/api/health || exit 1
          
          echo "Deployment completed successfully!"
```

### GitLab CI/CD

#### .gitlab-ci.yml
```yaml
stages:
  - test
  - build
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
  MAVEN_CLI_OPTS: "--batch-mode --errors --fail-at-end --show-version"

cache:
  paths:
    - .m2/repository/

test:
  stage: test
  image: openjdk:21-jdk
  services:
    - postgres:15
    - redis:7
  variables:
    POSTGRES_DB: test_db
    POSTGRES_USER: test_user
    POSTGRES_PASSWORD: test_password
    SPRING_PROFILES_ACTIVE: test
  script:
    - ./mvnw $MAVEN_CLI_OPTS test
  artifacts:
    reports:
      junit:
        - target/surefire-reports/TEST-*.xml
    paths:
      - target/

build:
  stage: build
  image: docker:latest
  services:
    - docker:dind
  before_script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
  script:
    - docker build -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA .
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA
    - docker tag $CI_REGISTRY_IMAGE:$CI_COMMIT_SHA $CI_REGISTRY_IMAGE:latest
    - docker push $CI_REGISTRY_IMAGE:latest
  only:
    - main

deploy:
  stage: deploy
  image: alpine:latest
  before_script:
    - apk add --no-cache openssh-client
    - eval $(ssh-agent -s)
    - echo "$SSH_PRIVATE_KEY" | tr -d '\r' | ssh-add -
    - mkdir -p ~/.ssh
    - chmod 700 ~/.ssh
    - ssh-keyscan $PROD_HOST >> ~/.ssh/known_hosts
    - chmod 644 ~/.ssh/known_hosts
  script:
    - ssh $PROD_USER@$PROD_HOST "
        cd /opt/sistema-java &&
        docker pull $CI_REGISTRY_IMAGE:latest &&
        docker-compose up -d --no-deps app &&
        sleep 30 &&
        curl -f http://localhost:8080/api/health
      "
  only:
    - main
  when: manual
```

---

## 📊 Monitoramento e Logs

### Configuração do Prometheus

#### prometheus.yml
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "sistema_rules.yml"

scrape_configs:
  - job_name: 'sistema-java-backend'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093
```

#### sistema_rules.yml
```yaml
groups:
- name: sistema-java-alerts
  rules:
  - alert: HighErrorRate
    expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
    for: 2m
    labels:
      severity: critical
    annotations:
      summary: "High error rate detected"
      description: "Error rate is {{ $value }} errors per second"

  - alert: HighMemoryUsage
    expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.8
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High memory usage"
      description: "Memory usage is {{ $value | humanizePercentage }}"

  - alert: DatabaseConnectionsHigh
    expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
    for: 3m
    labels:
      severity: warning
    annotations:
      summary: "High database connection usage"
      description: "Database connections usage is {{ $value | humanizePercentage }}"
```

### Grafana Dashboard

```json
{
  "dashboard": {
    "title": "Sistema Java Backend",
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count[5m])",
            "legendFormat": "{{method}} {{uri}}"
          }
        ]
      },
      {
        "title": "Response Time",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))",
            "legendFormat": "95th percentile"
          }
        ]
      },
      {
        "title": "JVM Memory",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes",
            "legendFormat": "{{area}}"
          }
        ]
      }
    ]
  }
}
```

### Configuração de Logs

#### logback-spring.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProfile name="!prod">
        <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
        <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <springProfile name="prod">
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/application.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
                <fileNamePattern>logs/application.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
                <maxFileSize>10MB</maxFileSize>
                <maxHistory>30</maxHistory>
                <totalSizeCap>1GB</totalSizeCap>
            </rollingPolicy>
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <mdc/>
                    <message/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>

        <appender name="SECURITY" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/security.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
                <fileNamePattern>logs/security.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
                <maxFileSize>10MB</maxFileSize>
                <maxHistory>90</maxHistory>
            </rollingPolicy>
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <mdc/>
                    <message/>
                </providers>
            </encoder>
        </appender>

        <logger name="SECURITY" level="INFO" additivity="false">
            <appender-ref ref="SECURITY"/>
        </logger>

        <root level="INFO">
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>
</configuration>
```

---

## 💾 Backup e Recuperação

### Script de Backup

```bash
#!/bin/bash
# backup.sh

set -e

# Configurações
BACKUP_DIR="/opt/backups/sistema-java"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

# Criar diretório de backup
mkdir -p "$BACKUP_DIR"

echo "Iniciando backup em $(date)"

# Backup do banco de dados
echo "Fazendo backup do PostgreSQL..."
pg_dump -h localhost -U sistema_user -d sistema_prod | gzip > "$BACKUP_DIR/db_backup_$DATE.sql.gz"

# Backup dos arquivos de configuração
echo "Fazendo backup das configurações..."
tar -czf "$BACKUP_DIR/config_backup_$DATE.tar.gz" \
    /opt/sistema-java/.env \
    /opt/sistema-java/keys/ \
    /etc/nginx/sites-available/sistema-java \
    /etc/systemd/system/sistema-java.service

# Backup dos logs (últimos 7 dias)
echo "Fazendo backup dos logs..."
find /opt/sistema-java/logs -name "*.log*" -mtime -7 | \
    tar -czf "$BACKUP_DIR/logs_backup_$DATE.tar.gz" -T -

# Backup do Redis (se necessário)
echo "Fazendo backup do Redis..."
redis-cli --rdb "$BACKUP_DIR/redis_backup_$DATE.rdb"

# Limpeza de backups antigos
echo "Removendo backups antigos..."
find "$BACKUP_DIR" -name "*backup*" -mtime +$RETENTION_DAYS -delete

echo "Backup concluído em $(date)"

# Verificar integridade dos backups
echo "Verificando integridade dos backups..."
for file in "$BACKUP_DIR"/*backup_$DATE.*; do
    if [[ $file == *.gz ]]; then
        gzip -t "$file" && echo "✓ $file OK" || echo "✗ $file CORRUPTED"
    fi
done

echo "Backup finalizado com sucesso!"
```

### Configuração do Cron

```bash
# Adicionar ao crontab
sudo crontab -e

# Backup diário às 2:00 AM
0 2 * * * /opt/scripts/backup.sh >> /var/log/backup.log 2>&1

# Backup semanal completo aos domingos às 1:00 AM
0 1 * * 0 /opt/scripts/full_backup.sh >> /var/log/backup.log 2>&1
```

### Script de Recuperação

```bash
#!/bin/bash
# restore.sh

set -e

if [ $# -ne 1 ]; then
    echo "Uso: $0 <data_backup_YYYYMMDD_HHMMSS>"
    exit 1
fi

BACKUP_DATE=$1
BACKUP_DIR="/opt/backups/sistema-java"

echo "Iniciando recuperação do backup $BACKUP_DATE"

# Parar aplicação
echo "Parando aplicação..."
sudo systemctl stop sistema-java

# Restaurar banco de dados
echo "Restaurando banco de dados..."
gunzip -c "$BACKUP_DIR/db_backup_$BACKUP_DATE.sql.gz" | \
    psql -h localhost -U sistema_user -d sistema_prod

# Restaurar configurações
echo "Restaurando configurações..."
tar -xzf "$BACKUP_DIR/config_backup_$BACKUP_DATE.tar.gz" -C /

# Restaurar Redis (se necessário)
echo "Restaurando Redis..."
sudo systemctl stop redis-server
cp "$BACKUP_DIR/redis_backup_$BACKUP_DATE.rdb" /var/lib/redis/dump.rdb
sudo chown redis:redis /var/lib/redis/dump.rdb
sudo systemctl start redis-server

# Reiniciar aplicação
echo "Reiniciando aplicação..."
sudo systemctl start sistema-java

# Verificar status
sleep 10
if curl -f http://localhost:8080/api/health; then
    echo "✓ Recuperação concluída com sucesso!"
else
    echo "✗ Erro na recuperação - verificar logs"
    exit 1
fi
```

---

## 🔍 Troubleshooting

### Problemas Comuns

#### 1. Aplicação não inicia

**Sintomas**:
- Erro na inicialização
- Porta já em uso
- Falha na conexão com banco

**Diagnóstico**:
```bash
# Verificar logs
sudo journalctl -u sistema-java -f

# Verificar porta
sudo netstat -tlnp | grep :8080

# Verificar conectividade do banco
pg_isready -h localhost -p 5432 -U sistema_user

# Verificar Redis
redis-cli ping
```

**Soluções**:
```bash
# Matar processo na porta
sudo lsof -ti:8080 | xargs sudo kill -9

# Reiniciar dependências
sudo systemctl restart postgresql redis-server

# Verificar configurações
cat /opt/sistema-java/.env
```

#### 2. Alto uso de memória

**Sintomas**:
- OutOfMemoryError
- Aplicação lenta
- GC frequente

**Diagnóstico**:
```bash
# Verificar uso de memória
free -h
ps aux | grep java

# Heap dump (se necessário)
jcmd <PID> GC.run_finalization
jcmd <PID> VM.gc
```

**Soluções**:
```bash
# Ajustar parâmetros JVM no systemd
sudo nano /etc/systemd/system/sistema-java.service

# Adicionar/modificar:
ExecStart=/usr/bin/java -jar \
  -Xms1g \
  -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  /opt/sistema-java/app.jar
```

#### 3. Problemas de conectividade

**Sintomas**:
- Timeout de conexão
- Erro 502/503 no Nginx
- Falha na autenticação

**Diagnóstico**:
```bash
# Testar conectividade
curl -v http://localhost:8080/api/health

# Verificar Nginx
sudo nginx -t
sudo systemctl status nginx

# Verificar firewall
sudo ufw status
```

**Soluções**:
```bash
# Reiniciar Nginx
sudo systemctl restart nginx

# Verificar configuração de proxy
sudo nano /etc/nginx/sites-available/sistema-java

# Testar sem proxy
curl http://localhost:8080/api/health
```

### Comandos Úteis

```bash
# Status geral do sistema
sudo systemctl status sistema-java postgresql redis-server nginx

# Logs em tempo real
sudo journalctl -u sistema-java -f

# Verificar conexões de rede
sudo netstat -tlnp | grep -E ':(8080|5432|6379|80|443)'

# Verificar uso de recursos
htop
iotop
df -h

# Verificar logs de aplicação
tail -f /opt/sistema-java/logs/application.log

# Verificar logs de segurança
tail -f /opt/sistema-java/logs/security.log

# Testar endpoints
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","senha":"password"}'

# Verificar métricas
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/health
```

---

## ✅ Checklist de Deploy

### Pré-Deploy

- [ ] **Código**
  - [ ] Testes passando (unitários e integração)
  - [ ] Code review aprovado
  - [ ] Documentação atualizada
  - [ ] Changelog atualizado

- [ ] **Configuração**
  - [ ] Variáveis de ambiente configuradas
  - [ ] Certificados SSL válidos
  - [ ] Chaves RSA geradas
  - [ ] Configurações de banco validadas

- [ ] **Infraestrutura**
  - [ ] Servidor preparado
  - [ ] Banco de dados configurado
  - [ ] Redis configurado
  - [ ] Nginx configurado (se aplicável)
  - [ ] Monitoramento configurado

### Durante o Deploy

- [ ] **Backup**
  - [ ] Backup do banco de dados
  - [ ] Backup das configurações
  - [ ] Backup da aplicação atual

- [ ] **Deploy**
  - [ ] Build da aplicação
  - [ ] Deploy da nova versão
  - [ ] Verificação de saúde
  - [ ] Testes de fumaça

### Pós-Deploy

- [ ] **Verificação**
  - [ ] Aplicação respondendo
  - [ ] Endpoints principais funcionando
  - [ ] Autenticação funcionando
  - [ ] Logs sem erros críticos

- [ ] **Monitoramento**
  - [ ] Métricas normais
  - [ ] Alertas configurados
  - [ ] Dashboard atualizado
  - [ ] Equipe notificada

### Rollback (se necessário)

- [ ] **Preparação**
  - [ ] Identificar problema
  - [ ] Decidir por rollback
  - [ ] Comunicar equipe

- [ ] **Execução**
  - [ ] Parar aplicação atual
  - [ ] Restaurar versão anterior
  - [ ] Restaurar banco (se necessário)
  - [ ] Verificar funcionamento

---

## 📚 Referências

- [Spring Boot Production Ready Features](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [PostgreSQL High Availability](https://www.postgresql.org/docs/current/high-availability.html)
- [Nginx Configuration Guide](https://nginx.org/en/docs/)
- [Prometheus Monitoring](https://prometheus.io/docs/guides/getting_started/)

---

**Documentação atualizada em: Janeiro 2024**  
**Versão: 1.0.0**  
**Responsável: Equipe DevOps**