# Guia de Instalação e Configuração

## Pré-requisitos

### Requisitos Mínimos
- **Java**: 21 ou superior
- **Maven**: 3.9.6 ou superior (ou usar o wrapper incluído)
- **Sistema Operacional**: Linux, macOS ou Windows
- **Memória RAM**: Mínimo 2GB (recomendado 4GB)
- **Espaço em Disco**: Mínimo 1GB livre

### Verificando Pré-requisitos

```bash
# Verificar versão do Java
java --version

# Verificar versão do Maven (opcional)
mvn --version

# O projeto inclui Maven Wrapper, então Maven não é obrigatório
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
# Navegar para o diretório backend
cd backend

# Executar a aplicação usando Maven Wrapper
./mvnw spring-boot:run
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
- **Console H2**: http://localhost:8080/h2-console
- **Actuator**: http://localhost:8080/actuator

---

## Instalação Detalhada

### Estrutura da Aplicação

O sistema é uma aplicação Spring Boot standalone com:

1. **Aplicação Spring Boot**: API REST principal
2. **Banco H2**: Banco de dados em memória
3. **Redis Embarcado**: Cache para desenvolvimento local
4. **Thymeleaf**: Templates para interface web

### Configuração Local

A aplicação usa o perfil `local` por padrão com:

```yaml
spring:
  profiles:
    active: local
  datasource:
    url: jdbc:h2:mem:sistema_db
    username: sa
    password: 
```

### Dados em Memória

- **H2**: Dados são perdidos ao reiniciar (desenvolvimento)
- **Redis**: Cache temporário para sessões e dados

---

## Configurações de Ambiente

### Configuração Padrão

A aplicação usa configurações padrão definidas em `application.yml`:

```yaml
spring:
  profiles:
    active: local
  datasource:
    url: jdbc:h2:mem:sistema_db
    username: sa
    password: 
  data:
    redis:
      host: localhost
      port: 6379
```

### Personalizando Configurações

Para personalizar as configurações, você pode:

1. **Modificar application.yml** diretamente
2. **Usar variáveis de ambiente**:

```bash
# Exemplo de variáveis de ambiente
export SERVER_PORT=8081
export SPRING_DATASOURCE_URL=jdbc:h2:file:./data/sistema_db
export SPRING_REDIS_PORT=6380
```

3. **Criar application-dev.yml** para desenvolvimento:

```yaml
spring:
  profiles:
    active: dev
server:
  port: 8081
logging:
  level:
    com.sistema: TRACE
```

---

## Comandos Úteis

### Gerenciamento da Aplicação

```bash
# Executar a aplicação
./mvnw spring-boot:run

# Executar em modo debug
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Executar com perfil específico
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Parar a aplicação
# Pressione Ctrl+C no terminal
```

### Build e Testes

```bash
# Compilar o projeto
./mvnw compile

# Executar testes
./mvnw test

# Gerar JAR
./mvnw package

# Limpar e recompilar
./mvnw clean compile

# Executar JAR gerado
java -jar target/sistema-java-*.jar
```

### Desenvolvimento

```bash
# Executar com live reload (DevTools)
./mvnw spring-boot:run

# Verificar dependências
./mvnw dependency:tree

# Atualizar dependências
./mvnw versions:display-dependency-updates

# Verificar código
./mvnw checkstyle:check
```

---

## Desenvolvimento Local

### Configuração para Desenvolvimento

Para desenvolvimento local, configure o perfil dev:

```yaml
# application-dev.yml
spring:
  profiles:
    active: dev
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true
logging:
  level:
    com.sistema: DEBUG
```

```bash
# Executar com perfil de desenvolvimento
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Hot Reload

O projeto já inclui Spring Boot DevTools para hot reload:

1. **DevTools já configurado** no `pom.xml`
2. **Restart automático** quando arquivos são modificados
3. **LiveReload** para atualização automática do browser

```bash
# Executar com DevTools ativo
./mvnw spring-boot:run

# Modificar arquivos Java - restart automático
# Modificar templates/static - reload automático
```

---

## Troubleshooting

### Problemas Comuns

#### 1. Porta já em uso
```bash
# Erro: Port 8080 was already in use
# Solução: Verificar processos usando a porta
sudo netstat -tulpn | grep :8080

# Ou mudar a porta no application.yml
server:
  port: 8081  # Usar porta 8081
```

#### 2. Erro de compilação
```bash
# Limpar e recompilar
./mvnw clean compile

# Verificar versão do Java
java -version

# Deve ser Java 21 ou superior
```

#### 3. Banco H2 não inicializa
```bash
# Verificar configuração no application.yml
# Verificar se o diretório tem permissão de escrita

# Acessar console H2
# http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:testdb
```

#### 4. Aplicação não inicia
```bash
# Verificar logs detalhados
./mvnw spring-boot:run -Dlogging.level.root=DEBUG

# Verificar se todas as dependências estão no pom.xml
./mvnw dependency:tree

# Limpar cache do Maven
./mvnw dependency:purge-local-repository
```

### Logs e Debugging

```bash
# Executar com logs detalhados
./mvnw spring-boot:run -Dlogging.level.com.sistema=DEBUG

# Logs específicos do Spring
./mvnw spring-boot:run -Dlogging.level.org.springframework=INFO

# Debug remoto (porta 5005)
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Verificar configurações ativas
curl http://localhost:8080/actuator/configprops
```

### Limpeza do Sistema

```bash
# Limpar build do Maven
./mvnw clean

# Limpar cache de dependências
./mvnw dependency:purge-local-repository

# Remover dados H2 (se usando arquivo)
rm -rf ~/testdb*

# Limpar logs da aplicação
rm -rf logs/
```

---

## Configuração de Produção

### Considerações de Segurança

1. **Senhas**: Use senhas fortes e variáveis de ambiente
2. **Banco de Dados**: Configure PostgreSQL ou MySQL para produção
3. **SSL/TLS**: Configure certificados para HTTPS
4. **Backup**: Configure backup automático dos dados

### Exemplo de Configuração de Produção

```yaml
# application-prod.yml
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:postgresql://localhost:5432/sistema_prod
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
server:
  port: ${SERVER_PORT:8080}
logging:
  level:
    root: WARN
    com.sistema: INFO
```

### Monitoramento em Produção

```bash
# Health check endpoint
curl http://localhost:8080/api/health

# Métricas do Actuator
curl http://localhost:8080/actuator/metrics

# Executar como serviço systemd
sudo systemctl enable sistema-java
sudo systemctl start sistema-java
```

---

## Próximos Passos

Após a instalação bem-sucedida:

1. Consulte a [Documentação da API](./api.md)
2. Explore os endpoints em http://localhost:8080
3. Acesse o console H2 em http://localhost:8080/h2-console
4. Configure banco de dados externo para produção
5. Implemente monitoramento e alertas