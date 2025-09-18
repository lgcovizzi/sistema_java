# Sistema Java Backend

## 📋 Visão Geral

Sistema backend completo desenvolvido em Java com Spring Boot, oferecendo autenticação JWT, validação de dados, sistema de email e gerenciamento de usuários com alta segurança.

### 🚀 Principais Funcionalidades

- **Autenticação JWT** com chaves RSA
- **Sistema de Email** com verificação de conta
- **Validação de CPF** brasileira
- **Rate Limiting** e proteção contra ataques
- **Sistema de Captcha** integrado
- **Tratamento de Erros** padronizado
- **Logs estruturados** e monitoramento
- **Testes automatizados** abrangentes

## 🛠️ Tecnologias Utilizadas

### Core
- **Java 17+**
- **Spring Boot 3.x**
- **Spring Security 6.x**
- **Spring Data JPA**
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
│   │   │   ├── config/          # Configurações
│   │   │   ├── controller/      # Controllers REST
│   │   │   ├── dto/            # Data Transfer Objects
│   │   │   ├── entity/         # Entidades JPA
│   │   │   ├── exception/      # Exceções customizadas
│   │   │   ├── repository/     # Repositórios
│   │   │   ├── security/       # Filtros de segurança
│   │   │   ├── service/        # Serviços de negócio
│   │   │   └── util/           # Utilitários
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
- [ ] Dashboard administrativo
- [ ] Relatórios e analytics
- [ ] API de pagamentos

### Melhorias Técnicas

- [ ] Cache distribuído com Redis
- [ ] Mensageria com RabbitMQ
- [ ] Observabilidade com OpenTelemetry
- [ ] CI/CD com GitHub Actions
- [ ] Kubernetes deployment
- [ ] Backup automatizado

---

**Desenvolvido com ❤️ pela equipe Sistema Java**