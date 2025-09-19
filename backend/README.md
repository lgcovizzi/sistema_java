# Sistema Java Backend

## 📋 Visão Geral

Sistema backend completo desenvolvido em Java com Spring Boot, oferecendo autenticação JWT, validação de dados, sistema de email e gerenciamento de usuários com alta segurança.

### 🚀 Principais Funcionalidades

- **Autenticação JWT** com chaves RSA
- **Sistema de Email** com verificação de conta
- **Validação de CPF** brasileira
- **Rate Limiting** e proteção contra ataques
- **Sistema de Captcha** integrado
- **Spring Batch** para processamento em lote
- **Filas de Processamento** (Email, Imagens, Arquivos)
- **Jobs Assíncronos** com monitoramento
- **Tratamento de Erros** padronizado
- **Logs estruturados** e monitoramento
- **Testes automatizados** abrangentes

## 🛠️ Tecnologias Utilizadas

### Core
- **Java 17+**
- **Spring Boot 3.x**
- **Spring Security 6.x**
- **Spring Data JPA**
- **Spring Batch 5.x**
- **Maven**

### Banco de Dados
- **H2 Database** (desenvolvimento)
- **PostgreSQL** (produção)

### Segurança
- **JWT (JSON Web Tokens)**
- **RSA Key Pair** para assinatura de tokens
- **BCrypt** para hash de senhas
- **Rate Limiting** com Redis

### Email
- **Spring Mail**
- **Thymeleaf** para templates
- **Mailtrap** (desenvolvimento)

### Testes
- **JUnit 5**
- **Mockito**
- **TestContainers**
- **Spring Boot Test**

## 📦 Estrutura do Projeto

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/sistema/
│   │   │   ├── batch/          # Configurações Spring Batch
│   │   │   ├── config/         # Configurações
│   │   │   ├── controller/     # Controllers REST
│   │   │   ├── dto/           # Data Transfer Objects
│   │   │   ├── entity/        # Entidades JPA
│   │   │   ├── exception/     # Exceções customizadas
│   │   │   ├── repository/    # Repositórios
│   │   │   ├── security/      # Filtros de segurança
│   │   │   ├── service/       # Serviços de negócio
│   │   │   └── util/          # Utilitários
│   │   └── resources/
│   │       ├── application.yml # Configurações
│   │       ├── static/         # Arquivos estáticos
│   │       └── templates/      # Templates Thymeleaf
│   └── test/                   # Testes automatizados
├── docs/                       # Documentação
├── keys/                       # Chaves RSA (auto-geradas)
└── pom.xml                     # Dependências Maven
```

## 🚀 Instalação e Configuração

### Pré-requisitos

- Java 17 ou superior
- Maven 3.6+
- Git

### 1. Clone o Repositório

```bash
git clone <repository-url>
cd sistema_java/backend
```

### 2. Configuração do Ambiente

Copie o arquivo de configuração:

```bash
cp src/main/resources/application.yml.example src/main/resources/application.yml
```

### 3. Configure as Variáveis de Ambiente

```yaml
# application.yml
spring:
  mail:
    host: smtp.mailtrap.io
    port: 2525
    username: ${MAIL_USERNAME:your-username}
    password: ${MAIL_PASSWORD:your-password}
    
app:
  jwt:
    expiration: 86400000  # 24 horas
  rsa:
    keys:
      directory: ./keys
```

### 4. Instale as Dependências

```bash
./mvnw clean install
```

### 5. Execute a Aplicação

```bash
./mvnw spring-boot:run
```

A aplicação estará disponível em: `http://localhost:8080`

## 🔧 Configuração de Desenvolvimento

### Banco de Dados H2

O H2 é configurado automaticamente para desenvolvimento:

- **Console H2**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **Username**: `sa`
- **Password**: *(vazio)*

### Email de Desenvolvimento

Configure o Mailtrap para testes de email:

1. Crie uma conta em [Mailtrap.io](https://mailtrap.io)
2. Configure as credenciais no `application.yml`
3. Todos os emails serão capturados no Mailtrap

### Chaves RSA

As chaves RSA são geradas automaticamente na primeira execução:

```
keys/
├── private_key.pem
└── public_key.pem
```

## 📚 Documentação da API

### Endpoints Principais

#### Autenticação

```http
POST /api/auth/register
POST /api/auth/login
POST /api/auth/verify-email
POST /api/auth/resend-verification
POST /api/auth/refresh-token
POST /api/auth/logout
```

#### Usuários

```http
GET /api/users/profile
PUT /api/users/profile
DELETE /api/users/account
```

#### Utilitários

```http
GET /api/health
GET /api/captcha
POST /api/captcha/verify
```

### Exemplo de Registro

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "João Silva",
    "email": "joao@example.com",
    "cpf": "12345678901",
    "telefone": "11999999999",
    "senha": "MinhaSenh@123"
  }'
```

### Exemplo de Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao@example.com",
    "senha": "MinhaSenh@123"
  }'
```

## 🔄 Sistema de Processamento em Lote (Spring Batch)

O sistema implementa um robusto sistema de processamento em lote usando Spring Batch para gerenciar operações assíncronas e de alto volume.

### 📋 Filas de Processamento

#### 1. EmailQueue - Fila de Emails
Gerencia o envio de emails em lote com alta performance e confiabilidade.

**Características:**
- Envio assíncrono de emails
- Retry automático em caso de falha
- Priorização de emails
- Templates dinâmicos
- Controle de tentativas

**Endpoints:**
```http
POST /api/email-queue/add           # Adicionar email à fila
GET  /api/email-queue               # Listar emails (paginado)
GET  /api/email-queue/{id}          # Buscar email por ID
GET  /api/email-queue/statistics    # Estatísticas da fila
POST /api/email-queue/reprocess/{id} # Reprocessar email
DELETE /api/email-queue/cleanup     # Limpar emails antigos
```

#### 2. ImageResizeQueue - Fila de Redimensionamento de Imagens
Processa redimensionamento e otimização de imagens em lote.

**Características:**
- Redimensionamento inteligente
- Múltiplos formatos (JPEG, PNG, WebP)
- Compressão otimizada
- Marca d'água automática
- Preservação de metadados

**Endpoints:**
```http
POST /api/image-queue/add           # Adicionar imagem à fila
GET  /api/image-queue               # Listar imagens (paginado)
GET  /api/image-queue/{id}          # Buscar imagem por ID
GET  /api/image-queue/statistics    # Estatísticas da fila
POST /api/image-queue/reprocess/{id} # Reprocessar imagem
DELETE /api/image-queue/cleanup     # Limpar imagens antigas
```

#### 3. FileProcessingQueue - Fila de Processamento de Arquivos
Gerencia operações em lote com arquivos e diretórios.

**Características:**
- Listagem de diretórios
- Análise de arquivos
- Compressão/descompressão
- Detecção de duplicatas
- Operações em lote

**Endpoints:**
```http
POST /api/file-queue/add            # Adicionar arquivo à fila
GET  /api/file-queue                # Listar arquivos (paginado)
GET  /api/file-queue/{id}           # Buscar arquivo por ID
GET  /api/file-queue/statistics     # Estatísticas da fila
GET  /api/file-queue/operation-types # Tipos de operação disponíveis
POST /api/file-queue/reprocess/{id} # Reprocessar arquivo
DELETE /api/file-queue/cleanup      # Limpar arquivos antigos
```

### 🎯 Jobs Spring Batch

#### 1. Email Job
```java
@Component
public class EmailJobConfig {
    // Configuração do job de envio de emails
    // Reader: Lê emails da fila
    // Processor: Prepara email para envio
    // Writer: Envia email via SMTP
}
```

#### 2. Image Resize Job
```java
@Component
public class ImageResizeJobConfig {
    // Configuração do job de redimensionamento
    // Reader: Lê imagens da fila
    // Processor: Redimensiona e otimiza
    // Writer: Salva imagem processada
}
```

#### 3. File Processing Job
```java
@Component
public class FileProcessingJobConfig {
    // Configuração do job de processamento
    // Reader: Lê arquivos da fila
    // Processor: Executa operação específica
    // Writer: Salva resultado
}
```

### 🎮 Controlador de Jobs

#### BatchJobController
Gerencia execução e monitoramento de todos os jobs.

```http
POST /api/batch/execute/{jobType}   # Executar job específico
GET  /api/batch/{id}                # Buscar job por ID
GET  /api/batch                     # Listar jobs (paginado)
GET  /api/batch/statistics          # Estatísticas gerais
GET  /api/batch/running             # Jobs em execução
POST /api/batch/cancel/{id}         # Cancelar job
DELETE /api/batch/cleanup           # Limpar jobs antigos
```

### 📊 Monitoramento e Estatísticas

#### Métricas Disponíveis
- **Jobs executados**: Total e por tipo
- **Taxa de sucesso**: Percentual de jobs bem-sucedidos
- **Tempo médio**: Duração média de processamento
- **Filas**: Tamanho atual das filas
- **Erros**: Logs de falhas e retry

#### Exemplo de Estatísticas
```json
{
  "totalJobs": 1250,
  "successfulJobs": 1180,
  "failedJobs": 70,
  "successRate": 94.4,
  "averageProcessingTime": "00:02:15",
  "queueSizes": {
    "emailQueue": 45,
    "imageQueue": 12,
    "fileQueue": 8
  }
}
```

### 🔧 Configuração do Spring Batch

#### application.yml
```yaml
spring:
  batch:
    job:
      enabled: false  # Não executar jobs automaticamente
    jdbc:
      initialize-schema: always
      
app:
  batch:
    chunk-size: 100
    thread-pool-size: 5
    retry-limit: 3
    cleanup-days: 30
```

### 🚀 Exemplos de Uso

#### Adicionar Email à Fila
```bash
curl -X POST http://localhost:8080/api/email-queue/add \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "to": "usuario@example.com",
    "subject": "Bem-vindo!",
    "template": "welcome",
    "priority": "HIGH"
  }'
```

#### Executar Job de Emails
```bash
curl -X POST http://localhost:8080/api/batch/execute/EMAIL \
  -H "Authorization: Bearer ${TOKEN}"
```

#### Adicionar Imagem para Redimensionamento
```bash
curl -X POST http://localhost:8080/api/image-queue/add \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "originalPath": "/uploads/image.jpg",
    "targetWidth": 800,
    "targetHeight": 600,
    "format": "JPEG",
    "quality": 85
  }'
```

### 🛡️ Segurança e Confiabilidade

#### Características de Segurança
- **Autenticação obrigatória** para todos os endpoints
- **Autorização baseada em roles** (USER/ADMIN)
- **Validação de entrada** em todos os parâmetros
- **Rate limiting** para prevenir abuso
- **Logs de auditoria** para todas as operações

#### Confiabilidade
- **Retry automático** em caso de falha
- **Dead letter queue** para falhas permanentes
- **Transações** para consistência de dados
- **Monitoramento** em tempo real
- **Cleanup automático** de dados antigos

## 🧪 Testes

### Executar Todos os Testes

```bash
./mvnw test
```

### Executar Testes Específicos

```bash
# Testes de unidade
./mvnw test -Dtest="*Test"

# Testes de integração
./mvnw test -Dtest="*IntegrationTest"

# Teste específico
./mvnw test -Dtest="AuthServiceTest"
```

### Cobertura de Testes

```bash
./mvnw jacoco:report
```

Relatório disponível em: `target/site/jacoco/index.html`

## 🔒 Segurança

### Autenticação JWT

- Tokens assinados com chaves RSA 2048-bit
- Expiração configurável (padrão: 24h)
- Refresh tokens para renovação automática
- Blacklist de tokens para logout seguro

### Validações

- **CPF**: Validação completa com dígitos verificadores
- **Email**: Formato e verificação por email
- **Senha**: Critérios de força obrigatórios
- **Rate Limiting**: Proteção contra força bruta

### Proteções

- **CORS** configurado
- **CSRF** desabilitado para API REST
- **Headers de segurança** configurados
- **Logs de auditoria** para ações sensíveis

## 📊 Monitoramento

### Health Check

```bash
curl http://localhost:8080/api/health
```

### Métricas (Actuator)

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
```

### Logs

Os logs são estruturados e incluem:

- **Timestamp**
- **Nível** (ERROR, WARN, INFO, DEBUG)
- **Classe/Método**
- **Mensagem**
- **Contexto** (usuário, IP, endpoint)

## 🚀 Deploy

### Produção

1. **Configure o banco PostgreSQL**:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sistema_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
```

2. **Configure o email SMTP**:

```yaml
spring:
  mail:
    host: ${SMTP_HOST}
    port: ${SMTP_PORT}
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
```

3. **Build da aplicação**:

```bash
./mvnw clean package -DskipTests
```

4. **Execute o JAR**:

```bash
java -jar target/sistema-backend-1.0.0.jar
```

### Docker

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/sistema-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
docker build -t sistema-backend .
docker run -p 8080:8080 sistema-backend
```

## 🤝 Contribuição

### Padrões de Código

- **Java Code Style**: Google Java Style Guide
- **Commits**: Conventional Commits
- **Branches**: GitFlow
- **Testes**: Cobertura mínima de 80%

### Processo de Desenvolvimento

1. Fork do repositório
2. Criar branch feature (`git checkout -b feature/nova-funcionalidade`)
3. Commit das mudanças (`git commit -m 'feat: adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/nova-funcionalidade`)
5. Abrir Pull Request

## 📄 Licença

Este projeto está licenciado sob a Licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes.

## 📞 Suporte

- **Documentação**: [docs/](docs/)
- **Issues**: GitHub Issues
- **Email**: suporte@sistema.com

## 📈 Roadmap

### Próximas Funcionalidades

- [ ] Sistema de notificações push
- [ ] API de upload de arquivos
- [ ] Integração com redes sociais
- [ ] Dashboard administrativo para Spring Batch
- [ ] Relatórios e analytics de jobs
- [ ] API de pagamentos
- [ ] Scheduler automático de jobs
- [ ] Webhooks para notificação de jobs

### Melhorias Técnicas

- [ ] Cache distribuído com Redis
- [ ] Mensageria com RabbitMQ para jobs
- [ ] Particionamento de jobs Spring Batch
- [ ] Cluster de processamento distribuído
- [ ] Observabilidade com OpenTelemetry
- [ ] CI/CD com GitHub Actions
- [ ] Kubernetes deployment
- [ ] Backup automatizado
- [ ] Métricas avançadas de performance
- [ ] Auto-scaling baseado em carga de jobs

---

**Desenvolvido com ❤️ pela equipe Sistema Java**