# Regras do Projeto - Sistema Java

## Visão Geral do Projeto

Sistema Java completo implementado com Spring Boot 3.2.0, utilizando PostgreSQL como banco de dados principal, Redis para cache, e MailHog para testes de email. O projeto está totalmente containerizado com Docker Compose.

## Estrutura Implementada

### Arquitetura de Serviços

#### Extração de Lógica Comum

O projeto implementa uma arquitetura baseada em classes abstratas e utilitários compartilhados para evitar duplicação de código e promover reutilização:

##### Classes Base Abstratas

**BaseService** (`com.sistema.service.base.BaseService`)
- Classe abstrata fundamental para todos os serviços
- Fornece logging padronizado com SLF4J
- Métodos de validação comum (validateNotNull, validateNotBlank)
- Tratamento de exceções padronizado
- Métodos de log estruturado (logInfo, logWarn, logError, logDebug)

**BaseRedisService** (`com.sistema.service.base.BaseRedisService`)
- Estende BaseService para operações Redis
- Operações CRUD padronizadas para cache
- Métodos com TTL automático (storeWithTTL)
- Operações de incremento e verificação de existência
- Gerenciamento de chaves com padrões consistentes

**BaseUserService** (`com.sistema.service.base.BaseUserService`)
- Estende BaseService para operações de usuário
- CRUD padronizado para entidades User
- Validações específicas (email, username, senha)
- Métodos de busca otimizados (por ID, email, username)
- Codificação de senhas com BCrypt
- Atualização de último login

**BaseSecurityService** (`com.sistema.service.base.BaseSecurityService`)
- Estende BaseService para operações de segurança
- Validação de tokens e autenticação
- Verificação de permissões e roles
- Operações de autorização padronizadas
- Validação de entrada sanitizada

##### Utilitários Compartilhados

**ValidationUtils** (`com.sistema.util.ValidationUtils`)
- Validações de entrada padronizadas
- Validação de formatos (email, telefone, CPF, CNPJ)
- Verificação de intervalos e limites
- Validação de senhas com critérios de segurança
- Métodos estáticos reutilizáveis

**FormatUtils** (`com.sistema.util.FormatUtils`)
- Formatação de dados padronizada
- Formatação de datas, moedas e números
- Formatação de documentos (CPF, CNPJ, telefone)
- Manipulação de strings (capitalização, remoção de acentos)
- Mascaramento de dados sensíveis

**SecurityUtils** (`com.sistema.util.SecurityUtils`)
- Operações de segurança compartilhadas
- Geração de tokens seguros
- Hash SHA-256 padronizado
- Sanitização de entrada
- Validação de endereços IP
- Mascaramento de dados sensíveis

##### Interfaces Padronizadas

O projeto implementa interfaces específicas para padronizar operações dos serviços:

**TokenOperations** (`com.sistema.service.interfaces.TokenOperations`)
- Interface para operações de token JWT
- Métodos: generateToken, generateAccessToken, extractSubject, validateToken
- Implementada por: JwtService
- Padroniza geração, validação e extração de dados de tokens

**SecurityOperations** (`com.sistema.service.interfaces.SecurityOperations`)
- Interface para operações de segurança
- Métodos: authenticateUser, validateTokenSecurity, revokeTokenSecurity, hasPermission, hasRole
- Implementada por: TokenBlacklistService
- Padroniza operações de autenticação e autorização

**CaptchaOperations** (`com.sistema.service.interfaces.CaptchaOperations`)
- Interface para operações de captcha
- Métodos: createCaptcha, verifyCaptcha, isCaptchaValid, getCaptchaConfiguration
- Implementada por: CaptchaService
- Padroniza geração e validação de captchas

**AttemptControlOperations** (`com.sistema.service.interfaces.AttemptControlOperations`)
- Interface para controle de tentativas
- Métodos: recordAttemptControl, isCaptchaRequiredControl, clearAttemptsControl
- Implementada por: AttemptService
- Padroniza controle de tentativas e rate limiting

##### Padrões de Implementação

**Herança de Serviços**
- Todos os serviços devem estender uma classe base apropriada
- AuthService estende BaseUserService
- CaptchaService estende BaseRedisService
- AttemptService estende BaseRedisService
- TokenBlacklistService estende BaseRedisService
- Novos serviços devem seguir este padrão

**Implementação de Interfaces**
- Serviços devem implementar interfaces específicas do domínio
- JwtService implementa TokenOperations
- CaptchaService implementa CaptchaOperations
- AttemptService implementa AttemptControlOperations
- TokenBlacklistService implementa SecurityOperations
- Interfaces garantem contratos consistentes entre serviços

**Uso de Utilitários**
- Sempre usar ValidationUtils para validação de entrada
- Usar SecurityUtils para operações criptográficas
- Usar FormatUtils para formatação de dados
- Evitar reimplementar lógica já disponível

**Tratamento de Exceções**
- Usar métodos de log da classe base
- Capturar exceções específicas quando possível
- Fornecer mensagens de erro consistentes
- Registrar erros com contexto adequado

### Backend Spring Boot
- **Framework**: Spring Boot 3.2.0
- **Java**: 21
- **Dependências principais**:
  - Spring Web
  - Spring Data JPA
  - Spring Data Redis
  - Spring Boot Actuator
  - Spring Boot Starter Thymeleaf
  - Spring Security
  - PostgreSQL Driver
  - Lettuce (Redis client)
  - JWT (io.jsonwebtoken)
  - SimpleCaptcha
  - BCrypt (Spring Security)
  - Validation API

### Controladores REST Implementados

#### 1. HealthController (`/api`)
- **GET /api/health**: Health check da aplicação
- **GET /api/info**: Informações da aplicação
- **GET /api/redis-test**: Teste de conectividade com Redis

#### 2. HomeController (`/`)
- **GET /**: Página inicial servindo template Thymeleaf responsivo
- **GET /api-simple**: Redirecionamento para página principal
- **Configuração**: Integrado com Thymeleaf para servir interface web responsiva

#### 3. AuthController (`/api/auth`)
- **POST /api/auth/register**: Registro de novos usuários
- **POST /api/auth/login**: Autenticação com captcha após 5 tentativas
- **POST /api/auth/refresh**: Renovação de tokens JWT
- **POST /api/auth/logout**: Logout com invalidação de tokens
- **GET /api/auth/me**: Informações do usuário autenticado
- **PUT /api/auth/change-password**: Alteração de senha
- **POST /api/auth/forgot-password**: Recuperação de senha com captcha
- **PUT /api/auth/enable/{userId}**: Habilitação/desabilitação de usuários (ADMIN)
- **PUT /api/auth/role/{userId}**: Alteração de roles de usuários (ADMIN)
- **GET /api/auth/statistics**: Estatísticas de usuários (ADMIN)

#### 4. CaptchaController (`/api/auth/captcha`)
- **POST /api/auth/captcha/generate**: Geração de imagem captcha
- **POST /api/auth/captcha/validate**: Validação de resposta captcha
- **GET /api/auth/captcha/exists/{captchaId}**: Verificação de existência
- **GET /api/auth/captcha/statistics**: Estatísticas de captchas
- **DELETE /api/auth/captcha/cleanup**: Limpeza de captchas expirados

## Sistema de Autenticação e Segurança

### Autenticação JWT

#### Características
- **Tokens JWT**: Autenticação stateless com tokens seguros
- **Refresh Tokens**: Sistema de renovação automática de tokens
- **Blacklist de Tokens**: Invalidação segura de tokens no logout
- **Expiração Configurável**: Access tokens (15min) e Refresh tokens (7 dias)
- **Chaves RSA**: Assinatura e verificação com chaves RSA 2048 bits

#### Fluxo de Autenticação
1. **Login**: Usuário fornece credenciais (email/senha)
2. **Validação**: Verificação de credenciais e status do usuário
3. **Geração de Tokens**: Access token e refresh token JWT
4. **Resposta**: Tokens retornados para o cliente
5. **Autorização**: Access token usado em requisições subsequentes
6. **Renovação**: Refresh token usado para obter novos access tokens

#### Configuração de Segurança
- **JWT Service**: Geração, validação e parsing de tokens
- **Security Filter**: Interceptação e validação de requisições
- **CORS Configuration**: Configuração para requisições cross-origin
- **Password Encoding**: BCrypt para hash de senhas
- **Role-based Access**: Controle de acesso baseado em roles (USER/ADMIN)

### Sistema de Controle de Tentativas

#### AttemptService
- **Rastreamento por IP**: Controle de tentativas por endereço IP
- **Limite Configurável**: 5 tentativas antes de exigir captcha
- **Armazenamento Redis**: Cache distribuído para tentativas
- **TTL Automático**: Expiração automática após 15 minutos
- **Limpeza Automática**: Remoção de tentativas expiradas

#### Funcionalidades
- **Registro de Tentativas**: Incremento automático por IP
- **Verificação de Limite**: Validação se captcha é necessário
- **Reset de Tentativas**: Limpeza após login bem-sucedido
- **Estatísticas**: Métricas de tentativas por IP
- **Bloqueio Temporário**: Proteção contra ataques de força bruta

### Sistema de Captcha

#### SimpleCaptcha Integration
- **Geração de Imagens**: Captchas visuais com texto aleatório
- **Configuração Customizada**: Tamanho, cores e fontes personalizáveis
- **Armazenamento Seguro**: Respostas armazenadas no Redis com hash
- **Expiração Automática**: TTL de 5 minutos para captchas
- **Validação Única**: Captchas invalidados após uso

#### CaptchaService
- **Geração**: Criação de imagens captcha em base64
- **Validação**: Verificação case-insensitive de respostas
- **Gerenciamento**: Controle de ciclo de vida dos captchas
- **Estatísticas**: Métricas de geração e validação
- **Limpeza**: Remoção automática de captchas expirados

#### Integração com Autenticação
- **Login Protegido**: Captcha obrigatório após 5 tentativas falhadas
- **Recuperação de Senha**: Captcha sempre obrigatório
- **Validação Integrada**: Verificação automática no fluxo de autenticação
- **Feedback ao Cliente**: Informação sobre necessidade de captcha

### Configurações Implementadas

#### RedisConfig
- Cache manager configurado com TTL de 10 minutos
- Serialização JSON para valores
- Serialização String para chaves
- Configuração de RedisTemplate

#### ThymeleafConfig
- Template engine integrado ao Spring Boot
- Configuração automática via spring-boot-starter-thymeleaf
- Templates localizados em `/src/main/resources/templates/`
- Suporte a expressões Thymeleaf para dados dinâmicos
- Integração com Model do Spring MVC

#### RSAKeyManager
- Gerenciamento automático de chaves RSA na inicialização da aplicação
- Geração de par de chaves RSA de 2048 bits se não existirem
- Validação de chaves existentes através de teste de criptografia/descriptografia
- Armazenamento seguro em formato PEM
- Configuração do diretório de chaves via `app.rsa.keys.directory`
- Logs detalhados para monitoramento do processo
- Regeneração automática de chaves inválidas ou corrompidas

#### SecurityConfig
- Configuração de segurança Spring Security
- Filtros JWT para autenticação stateless
- Configuração CORS para requisições cross-origin
- Endpoints públicos e protegidos definidos
- Password encoder BCrypt configurado
- Desabilitação de CSRF para APIs REST

#### JwtAuthenticationFilter
- Filtro personalizado para interceptação de requisições
- Extração e validação de tokens JWT do header Authorization
- Configuração do contexto de segurança Spring
- Tratamento de exceções de autenticação
- Integração com UserDetailsService

### Serviços Implementados

#### AuthService
- **Herança**: Estende BaseUserService
- **Registro de Usuários**: Criação de novos usuários com validação
- **Autenticação**: Verificação de credenciais e geração de tokens
- **Gerenciamento de Usuários**: CRUD completo de usuários
- **Controle de Roles**: Alteração de permissões (USER/ADMIN)
- **Estatísticas**: Métricas de usuários do sistema
- **Validação de Tokens**: Verificação de tokens JWT
- **Busca por Email**: Localização de usuários por email

#### JwtService
- **Herança**: Estende BaseService
- **Interface**: Implementa TokenOperations
- **Geração de Tokens**: Criação de access e refresh tokens
- **Validação**: Verificação de assinatura e expiração
- **Parsing**: Extração de claims dos tokens
- **Chaves RSA**: Uso de chaves assimétricas para segurança
- **Configuração Flexível**: TTL configurável para diferentes tipos de token
- **Métodos da Interface**: generateToken, generateAccessToken, extractSubject, validateToken

#### TokenBlacklistService
- **Herança**: Estende BaseRedisService
- **Interface**: Implementa SecurityOperations
- **Invalidação de Tokens**: Adição de tokens à blacklist
- **Verificação**: Validação se token está na blacklist
- **Armazenamento Redis**: Cache distribuído para blacklist
- **TTL Automático**: Expiração automática baseada no token
- **Limpeza**: Remoção de tokens expirados
- **Métodos da Interface**: authenticateUser, validateTokenSecurity, revokeTokenSecurity, hasPermission, hasRole

#### AttemptService
- **Herança**: Estende BaseRedisService
- **Interface**: Implementa AttemptControlOperations
- **Controle de Tentativas**: Rastreamento por endereço IP
- **Limite Configurável**: Proteção contra força bruta
- **Armazenamento Redis**: Persistência distribuída
- **Reset Automático**: Limpeza após sucesso
- **Estatísticas**: Métricas de tentativas por IP
- **Métodos da Interface**: recordAttemptControl, isCaptchaRequiredControl, clearAttemptsControl

#### CaptchaService
- **Herança**: Estende BaseRedisService
- **Interface**: Implementa CaptchaOperations
- **Geração de Captchas**: Imagens com SimpleCaptcha
- **Validação Segura**: Verificação com hash SHA-256
- **Gerenciamento de Ciclo**: TTL e limpeza automática
- **Configuração Customizada**: Tamanho e aparência personalizáveis
- **Estatísticas**: Métricas de uso e validação
- **Métodos da Interface**: createCaptcha, verifyCaptcha, isCaptchaValid, getCaptchaConfiguration

### Entidades e Repositórios

#### User Entity
- **Campos**: id, username, email, password, role, enabled, timestamps
- **Validações**: Email único, senha forte, campos obrigatórios
- **Relacionamentos**: Configurado para extensões futuras
- **Auditoria**: Timestamps de criação e atualização

#### UserRepository
- **Busca por Email**: Método findByEmail para autenticação
- **Contagem por Role**: Estatísticas de usuários por tipo
- **Usuários Ativos**: Contagem de usuários habilitados
- **Queries Customizadas**: Métodos específicos do domínio

#### Enums
- **UserRole**: USER, ADMIN para controle de acesso
- **Extensibilidade**: Preparado para novos roles

### Interface Thymeleaf Implementada

#### Template Responsivo
- **Arquivo**: `/src/main/resources/templates/index.html`
- **Características**:
  - Design mobile-first com CSS Grid e Flexbox
  - Variáveis CSS customizadas para consistência visual
  - Breakpoints responsivos adaptativos
  - Gradiente de fundo moderno
  - Cards com sombras e efeitos hover
  - Typography responsiva com clamp()

#### Estrutura da Interface
- **Header**: Título principal com texto responsivo
- **Cards de Informação**:
  - Status da aplicação com indicadores visuais
  - Estatísticas do sistema (uptime, endpoints, serviços)
  - Lista de endpoints da API com links funcionais
  - Tecnologias utilizadas no projeto
- **Footer**: Informações do desenvolvedor e links úteis

#### Integração com Spring Boot
- **HomeController**: Configurado para servir templates Thymeleaf
- **Model Attributes**: Dados dinâmicos passados para o template
  - `appName`: Nome da aplicação
  - `version`: Versão atual do sistema
- **Thymeleaf Expressions**: Uso de `th:text` para dados dinâmicos

#### Design System
- **Paleta de Cores**: Gradiente roxo/azul moderno
- **Tipografia**: Segoe UI com fallbacks
- **Espaçamento**: Sistema consistente com variáveis CSS
- **Responsividade**: Breakpoints para mobile, tablet e desktop
- **Acessibilidade**: Contraste adequado e navegação por teclado

## Docker Compose Configuration

### Arquitetura dos Serviços

O projeto utiliza Docker Compose com os seguintes serviços:

#### 1. Backend Spring Boot
- **Serviço**: `app`
- **Contexto de Build**: `./backend`
- **Dockerfile**: `./backend/Dockerfile`
- **Imagem Base**: `maven:3.9.6-eclipse-temurin-21` (build) + `eclipse-temurin:21-jre-alpine` (runtime)
- **Porta**: 8080:8080
- **Dependências**: postgres, redis
- **Variáveis de ambiente**:
  - `SPRING_PROFILES_ACTIVE=docker`
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/sistema_db`
  - `SPRING_DATASOURCE_USERNAME=sistema_user`
  - `SPRING_DATASOURCE_PASSWORD=sistema_pass`
  - `SPRING_REDIS_HOST=redis`
  - `SPRING_REDIS_PORT=6379`
  - `SPRING_MAIL_HOST=mailhog`
  - `SPRING_MAIL_PORT=1025`

#### 2. PostgreSQL Database
- **Serviço**: `postgres`
- **Imagem**: `postgres:15-alpine`
- **Porta**: 5432:5432
- **Variáveis de ambiente**:
  - `POSTGRES_DB=sistema_db`
  - `POSTGRES_USER=sistema_user`
  - `POSTGRES_PASSWORD=sistema_pass`
- **Volume**: `postgres_data:/var/lib/postgresql/data`

#### 3. Redis Cache
- **Serviço**: `redis`
- **Imagem**: `redis:7-alpine`
- **Porta**: 6379:6379
- **Comando**: `redis-server --appendonly yes`
- **Volume**: `redis_data:/data`

#### 4. MailHog (Email Testing)
- **Serviço**: `mailhog`
- **Imagem**: `mailhog/mailhog:latest`
- **Portas**: 
  - `1025:1025` (SMTP)
  - `8025:8025` (Web UI)

### Networks
- **Rede**: `sistema-network`
- **Driver**: bridge
- Todos os serviços devem estar na mesma rede

### Volumes
- `postgres_data`: Para persistência do PostgreSQL
- `redis_data`: Para persistência do Redis

### Ordem de Inicialização
1. postgres
2. redis
3. mailhog
4. app (dependente dos demais)

### Configurações Adicionais
- Usar `restart: unless-stopped` para todos os serviços
- Configurar healthchecks para postgres e redis
- Usar variáveis de ambiente para configurações sensíveis
- Implementar wait-for-it ou dockerize para aguardar dependências

### Dockerfile Implementado

#### Multi-stage Build
- **Stage 1 (build)**: `maven:3.9.6-eclipse-temurin-21`
  - Copia `pom.xml` e baixa dependências
  - Copia código fonte e compila aplicação
  - Gera JAR executável
- **Stage 2 (runtime)**: `eclipse-temurin:21-jre-alpine`
  - Copia apenas o JAR da aplicação
  - Configuração otimizada para produção
  - Imagem final menor e mais segura

### Maven Wrapper
- **mvnw**: Script executável para Linux/Mac
- **maven-wrapper.properties**: Configuração da versão do Maven
- **maven-wrapper.jar**: JAR do wrapper (baixado automaticamente)

### Estrutura do Projeto Implementada
- **Backend**: `./backend/`
  - **src/main/java/com/sistema/**:
    - `SistemaJavaApplication.java`: Classe principal
    - `controller/HealthController.java`: Endpoints de API
    - `controller/HomeController.java`: Página inicial
    - `config/RedisConfig.java`: Configuração do Redis
  - **src/main/resources/**:
    - `application.properties`: Configurações da aplicação
    - `application-docker.properties`: Configurações para Docker
  - **Dockerfile**: Build multi-stage implementado
  - **pom.xml**: Dependências e configurações Maven
  - **mvnw**: Maven wrapper
- **Documentação**: `./docs/`
  - **README.md**: Documentação principal e quick start
  - **api.md**: Documentação completa da API REST
  - **installation.md**: Guia de instalação e configuração
  - **docker.md**: Documentação Docker Compose e arquitetura
- **Configuração**:
  - **docker-compose.yml**: Orquestração de todos os serviços
  - **.trae/rules/project_rules.md**: Regras e documentação do projeto

### Endpoints Funcionais
- **http://localhost:8080/**: Interface web responsiva com template Thymeleaf (via Docker Compose)
- **http://localhost:8080/api-simple**: Redirecionamento para página principal
- **http://localhost:8080/api/health**: Status da aplicação
- **http://localhost:8080/api/info**: Informações do sistema
- **http://localhost:8080/api/redis-test**: Teste do Redis
- **http://localhost:8080/actuator**: Endpoints do Actuator
- **http://localhost:8080/api/auth/register**: Registro de usuários
- **http://localhost:8080/api/auth/login**: Login com controle de tentativas
- **http://localhost:8080/api/auth/refresh**: Renovação de tokens JWT
- **http://localhost:8080/api/auth/logout**: Logout com invalidação de tokens
- **http://localhost:8080/api/auth/me**: Informações do usuário autenticado
- **http://localhost:8080/api/auth/captcha/generate**: Geração de captcha
- **http://localhost:8080/api/auth/captcha/validate**: Validação de captcha
- **http://localhost:8025**: Interface web do MailHog

**Nota**: A aplicação está configurada para rodar na porta 8080 através do Docker Compose. Para desenvolvimento local, a aplicação pode ser executada na porta 8082 via Maven (`mvn spring-boot:run`) para evitar conflitos de porta.

### Documentação Implementada

O projeto possui uma estrutura completa de documentação na pasta `docs/`:

#### Arquivos de Documentação
- **docs/README.md**: Visão geral do projeto, quick start e navegação
- **docs/api.md**: Documentação completa da API com todos os endpoints
- **docs/installation.md**: Guia detalhado de instalação e configuração
- **docs/docker.md**: Documentação completa do Docker Compose

#### Conteúdo da Documentação

**README.md**:
- Visão geral do sistema
- Tecnologias utilizadas
- Estrutura do projeto
- Quick start guide
- Links para documentação específica

**API.md**:
- Documentação de todos os endpoints REST
- Exemplos de requisições e respostas
- Códigos de erro e status
- Métodos de teste (cURL, HTTPie, scripts)
- Informações de monitoramento

**Installation.md**:
- Pré-requisitos do sistema
- Instalação rápida e detalhada
- Configurações de ambiente
- Comandos úteis para desenvolvimento
- Troubleshooting completo
- Configurações para produção

**Docker.md**:
- Visão geral da arquitetura Docker
- Detalhes de cada serviço
- Configurações e volumes
- Comandos de gerenciamento
- Health checks e monitoramento
- Otimizações e boas práticas

### Status do Projeto
- ✅ **Aplicação funcionando**: Todos os serviços rodando na porta 8080 via Docker Compose
- ✅ **Conectividade PostgreSQL**: Configurada e testada
- ✅ **Conectividade Redis**: Configurada e testada
- ✅ **MailHog**: Disponível para testes de email
- ✅ **Endpoints REST**: Implementados e funcionais
- ✅ **Docker Compose**: Totalmente operacional
- ✅ **Configuração de Porta**: Aplicação acessível em http://localhost:8080
- ✅ **Desenvolvimento Local**: Configurado para porta 8082 quando executado via Maven
- ✅ **Documentação**: Estrutura completa implementada
- ✅ **Guias de instalação**: Documentação detalhada criada
- ✅ **API Documentation**: Todos os endpoints documentados
- ✅ **Docker Documentation**: Arquitetura e configurações documentadas
- ✅ **Estrutura de Documentação**: Pasta `docs/` organizada e completa
- ✅ **Guias de Desenvolvimento**: Procedimentos e boas práticas documentadas
- ✅ **Troubleshooting**: Soluções para problemas comuns documentadas
- ✅ **Regras de Documentação**: Padrões e processos estabelecidos
- ✅ **Interface Thymeleaf**: Template responsivo implementado e funcionando
- ✅ **Design Responsivo**: CSS mobile-first com breakpoints adaptativos
- ✅ **HomeController**: Configurado para servir templates Thymeleaf
- ✅ **Template Engine**: Thymeleaf integrado ao Spring Boot
- ✅ **Sistema de Autenticação**: JWT implementado com Spring Security
- ✅ **Controle de Tentativas**: Sistema anti-brute force configurado
- ✅ **Sistema de Captcha**: SimpleCaptcha integrado com Redis
- ✅ **Segurança**: BCrypt para senhas e blacklist de tokens
- ✅ **Autorização**: Sistema de roles (USER, ADMIN) implementado
- ✅ **Validação**: Bean Validation configurado para DTOs
- ✅ **Recuperação de Senha**: Endpoint com captcha obrigatório

### Arquivos de Documentação Detalhados

#### docs/README.md
**Propósito**: Ponto de entrada principal da documentação
**Conteúdo**:
- Visão geral do Sistema Java
- Stack tecnológico completo
- Estrutura de diretórios
- Serviços disponíveis (Spring Boot, PostgreSQL, Redis, MailHog)
- Quick start com comandos essenciais
- Links para documentação específica
- Informações de contribuição

#### docs/api.md
**Propósito**: Documentação completa da API REST
**Conteúdo**:
- Visão geral da API
- Endpoints implementados:
  - `GET /`: Página inicial com navegação
  - `GET /api/health`: Health check da aplicação
  - `GET /api/info`: Informações da aplicação
  - `GET /api/redis-test`: Teste de conectividade Redis
  - `GET /actuator/*`: Endpoints do Spring Actuator
- Códigos de erro padronizados
- Exemplos de teste com cURL, HTTPie e scripts bash
- Informações de monitoramento e métricas

#### docs/installation.md
**Propósito**: Guia completo de instalação e configuração
**Conteúdo**:
- Pré-requisitos (Docker, Docker Compose, Git)
- Instalação rápida (3 comandos)
- Instalação detalhada passo a passo
- Configurações de ambiente
- Comandos úteis para desenvolvimento
- Desenvolvimento local com hot reload
- Troubleshooting de problemas comuns
- Configurações para ambiente de produção

#### docs/docker.md
**Propósito**: Documentação da arquitetura Docker
**Conteúdo**:
- Visão geral da arquitetura multi-serviços
- Detalhes de cada serviço:
  - Spring Boot (app)
  - PostgreSQL (postgres)
  - Redis (redis)
  - MailHog (mailhog)
- Configurações de rede e volumes
- Dockerfile multi-stage explicado
- Comandos de gerenciamento Docker
- Health checks e monitoramento
- Variáveis de ambiente
- Otimizações e boas práticas

### Desenvolvimento
- **Produção**: Usar Docker Compose (`docker-compose up -d`) - aplicação disponível em http://localhost:8080
- **Desenvolvimento Local**: Executar via Maven (`mvn spring-boot:run`) - aplicação disponível em http://localhost:8082
- Para desenvolvimento local, mapear o código fonte como volume
- Usar profiles do Spring Boot para diferentes ambientes
- Configurar hot reload quando possível
- Manter separação clara entre backend e outros componentes
- Consultar documentação na pasta `docs/` para referências
- Atualizar documentação ao implementar novas features
- **Configuração de Porta**:
  - Docker Compose: Porta 8080 (configurada no docker-compose.yml)
  - Maven local: Porta 8082 (configurada no application.yml)
- **Thymeleaf**:
  - Templates HTML em `/src/main/resources/templates/`
  - Controladores Spring MVC em `/src/main/java/com/sistema/controller/`
  - Configuração automática via Spring Boot
  - CSS responsivo integrado nos templates
  - Expressões Thymeleaf para dados dinâmicos
- **Autenticação e Segurança**:
  - JWT tokens com chaves RSA para assinatura
  - Sistema de blacklist de tokens no Redis
  - Controle de tentativas de login por IP
  - Captcha obrigatório após 5 tentativas falhadas
  - BCrypt para hash de senhas
  - Roles de usuário (USER, ADMIN)
  - Endpoints protegidos com Spring Security
- **Testes de Autenticação**:
  - Usar endpoints `/api/auth/register` e `/api/auth/login` para testes
  - Captcha disponível em `/api/auth/captcha/generate`
  - Tokens JWT válidos por 24 horas
  - Refresh tokens válidos por 7 dias

### Segurança
- Não expor portas desnecessárias
- Usar secrets para senhas em produção
- Configurar redes isoladas quando necessário

#### Sistema de Autenticação JWT
- **Algoritmo**: RS256 (RSA com SHA-256)
- **Chaves**: Par RSA 2048 bits gerado automaticamente
- **Access Token**: Válido por 24 horas
- **Refresh Token**: Válido por 7 dias
- **Blacklist**: Tokens invalidados armazenados no Redis
- **Headers**: Authorization Bearer token obrigatório

#### Controle de Tentativas (Anti-Brute Force)
- **Limite**: 5 tentativas por IP em 15 minutos
- **Bloqueio**: IP bloqueado por 30 minutos após exceder limite
- **Captcha**: Obrigatório após 5 tentativas falhadas
- **Armazenamento**: Contadores mantidos no Redis
- **Endpoints Protegidos**: Login e recuperação de senha

#### Sistema de Captcha
- **Biblioteca**: SimpleCaptcha
- **Armazenamento**: Redis com TTL de 5 minutos
- **Formato**: Imagem PNG base64
- **Validação**: Case-insensitive
- **Limpeza**: Automática via TTL do Redis
- **Estatísticas**: Contadores de geração e validação

#### Criptografia de Senhas
- **Algoritmo**: BCrypt com salt automático
- **Strength**: 12 rounds (configurável)
- **Validação**: Comparação segura via BCrypt
- **Política**: Senhas devem ter mínimo 8 caracteres

#### Gerenciamento de Chaves RSA
- **Inicialização Automática**: Par de chaves RSA gerado automaticamente na primeira execução
- **Validação**: Chaves existentes são validadas a cada inicialização da aplicação
- **Regeneração**: Chaves inválidas ou corrompidas são automaticamente regeneradas
- **Armazenamento**: Chaves armazenadas em formato PEM no diretório configurado
- **Configuração**: Diretório padrão `./keys`, configurável via `app.rsa.keys.directory`
- **Algoritmo**: RSA 2048 bits para garantir segurança adequada
- **Logs**: Monitoramento completo do processo de geração e validação
- **Estrutura de Arquivos**:
  - `private_key.pem`: Chave privada RSA
  - `public_key.pem`: Chave pública RSA
- **Classe Responsável**: `com.sistema.config.RSAKeyManager`

## Test-Driven Development (TDD)

### Visão Geral do TDD

O Test-Driven Development é uma metodologia de desenvolvimento onde os testes são escritos antes do código de produção. O projeto Sistema Java adota TDD como prática fundamental para garantir qualidade, confiabilidade e manutenibilidade do código.

### Princípios Fundamentais

#### 1. Ciclo Red-Green-Refactor

**Red (Vermelho)**:
- Escrever um teste que falha
- O teste deve ser específico e focado em uma única funcionalidade
- Executar o teste para confirmar que falha

**Green (Verde)**:
- Escrever o código mínimo necessário para fazer o teste passar
- Foco na funcionalidade, não na elegância do código
- Executar todos os testes para garantir que passam

**Refactor (Refatorar)**:
- Melhorar o código mantendo os testes passando
- Eliminar duplicação e melhorar design
- Executar testes após cada refatoração

#### 2. Regras do TDD

1. **Não escrever código de produção sem um teste falhando**
2. **Não escrever mais teste do que o suficiente para falhar**
3. **Não escrever mais código de produção do que o suficiente para passar no teste**

### Estrutura de Testes

#### Organização de Diretórios

```
src/
├── main/
│   └── java/
│       └── com/
│           └── sistema/
│               ├── controller/
│               ├── service/
│               ├── repository/
│               └── config/
└── test/
    └── java/
        └── com/
            └── sistema/
                ├── controller/     # Testes de controladores
                ├── service/        # Testes de serviços
                ├── repository/     # Testes de repositórios
                ├── config/         # Testes de configuração
                ├── integration/    # Testes de integração
                └── e2e/           # Testes end-to-end
```

#### Dependências de Teste

**Configuração no pom.xml**:
```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Testcontainers -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- AssertJ -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Estratégias de Teste

#### 1. Testes Unitários

**Características**:
- Testam uma única unidade de código isoladamente
- Usam mocks para dependências externas
- Execução rápida (< 100ms por teste)
- Cobertura de código > 80%

**Exemplo de Estrutura**:
```java
@ExtendWith(MockitoExtension.class)
class HealthServiceTest {
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @InjectMocks
    private HealthService healthService;
    
    @Test
    @DisplayName("Deve retornar status UP quando Redis está disponível")
    void shouldReturnUpWhenRedisIsAvailable() {
        // Given
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        
        // When
        HealthStatus status = healthService.checkRedisHealth();
        
        // Then
        assertThat(status.getStatus()).isEqualTo("UP");
    }
}
```

#### 2. Testes de Integração

**Características**:
- Testam integração entre componentes
- Usam banco de dados real ou Testcontainers
- Verificam fluxo completo de dados
- Executados em ambiente isolado

**Configuração com Testcontainers**:
```java
@SpringBootTest
@Testcontainers
class HealthControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("Deve retornar health check com status 200")
    void shouldReturnHealthCheckWithStatus200() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/health", Map.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("status");
    }
}
```

#### 3. Testes End-to-End (E2E)

**Características**:
- Testam fluxo completo da aplicação
- Incluem interface web quando aplicável
- Usam ambiente próximo à produção
- Executados em pipeline de CI/CD

### Convenções de Nomenclatura

#### Classes de Teste

- **Testes Unitários**: `{ClasseTestada}Test`
  - Exemplo: `HealthServiceTest`
- **Testes de Integração**: `{ClasseTestada}IntegrationTest`
  - Exemplo: `HealthControllerIntegrationTest`
- **Testes E2E**: `{Funcionalidade}E2ETest`
  - Exemplo: `LoginFlowE2ETest`

#### Métodos de Teste

**Padrão**: `should{ExpectedBehavior}When{StateUnderTest}`

**Exemplos**:
- `shouldReturnHealthStatusWhenRedisIsAvailable()`
- `shouldThrowExceptionWhenDatabaseIsUnavailable()`
- `shouldRedirectToDashboardWhenLoginIsSuccessful()`

#### Estrutura Given-When-Then

```java
@Test
void shouldCalculateTotalWhenValidItemsProvided() {
    // Given (Arrange)
    List<Item> items = Arrays.asList(
        new Item("item1", 10.0),
        new Item("item2", 20.0)
    );
    
    // When (Act)
    double total = calculator.calculateTotal(items);
    
    // Then (Assert)
    assertThat(total).isEqualTo(30.0);
}
```

### Ferramentas de Qualidade

#### 1. Cobertura de Código

**JaCoCo Configuration**:
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Métricas de Cobertura**:
- **Mínimo aceitável**: 70%
- **Meta**: 85%
- **Classes críticas**: 95%

#### 2. Análise Estática

**SonarQube Integration**:
- Análise de qualidade de código
- Detecção de code smells
- Verificação de vulnerabilidades
- Métricas de complexidade

### Configurações de Ambiente

#### Profiles de Teste

**application-test.yml**:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  redis:
    host: localhost
    port: 6370  # Porta diferente para testes
logging:
  level:
    com.sistema: DEBUG
```

#### Docker para Testes

**docker-compose.test.yml**:
```yaml
version: '3.8'
services:
  postgres-test:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: test_db
      POSTGRES_USER: test_user
      POSTGRES_PASSWORD: test_pass
    ports:
      - "5433:5432"
  
  redis-test:
    image: redis:7-alpine
    ports:
      - "6370:6379"
```

### Boas Práticas

#### 1. Escrita de Testes

- **Um conceito por teste**: Cada teste deve verificar apenas uma funcionalidade
- **Testes independentes**: Não devem depender da ordem de execução
- **Nomes descritivos**: Usar `@DisplayName` para clareza
- **Arrange-Act-Assert**: Estrutura clara e consistente
- **Dados de teste**: Usar builders ou factories para objetos complexos

#### 2. Mocks e Stubs

- **Mock apenas dependências externas**: Não mockar classes do próprio sistema
- **Verificar interações importantes**: Usar `verify()` quando necessário
- **Evitar over-mocking**: Preferir objetos reais quando possível
- **Reset mocks**: Limpar estado entre testes

#### 3. Performance

- **Testes rápidos**: Unitários < 100ms, integração < 5s
- **Paralelização**: Configurar execução paralela quando possível
- **Cleanup**: Limpar recursos após testes
- **Profiles separados**: Usar configurações otimizadas para testes

### Integração com CI/CD

#### Pipeline de Testes

```yaml
# .github/workflows/test.yml
name: Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
      
      - name: Run Unit Tests
        run: ./mvnw test
      
      - name: Run Integration Tests
        run: ./mvnw verify -P integration-tests
      
      - name: Generate Coverage Report
        run: ./mvnw jacoco:report
      
      - name: Upload Coverage
        uses: codecov/codecov-action@v3
```

#### Quality Gates

- **Todos os testes devem passar**: 100% de sucesso
- **Cobertura mínima**: 70% para merge
- **Sem vulnerabilidades críticas**: SonarQube analysis
- **Performance**: Testes não devem exceder tempo limite

### Troubleshooting de Testes

#### Problemas Comuns

**1. Testes Flaky (Instáveis)**:
- Dependência de tempo ou ordem
- Estado compartilhado entre testes
- Recursos externos indisponíveis

**Solução**:
```java
// Usar timeouts apropriados
@Test
@Timeout(value = 5, unit = TimeUnit.SECONDS)
void shouldCompleteWithinTimeout() {
    // teste
}

// Isolar estado
@BeforeEach
void setUp() {
    // limpar estado
}
```

**2. Testes Lentos**:
- Muitas chamadas de rede
- Banco de dados não otimizado
- Configuração inadequada

**Solução**:
- Usar mocks para dependências externas
- Configurar banco em memória para testes
- Otimizar queries e índices

### Métricas e Monitoramento

#### KPIs de Teste

- **Cobertura de código**: % de linhas testadas
- **Tempo de execução**: Duração total dos testes
- **Taxa de sucesso**: % de testes que passam
- **Flakiness**: % de testes instáveis

#### Relatórios

- **JaCoCo**: Cobertura de código
- **Surefire**: Resultados de testes unitários
- **Failsafe**: Resultados de testes de integração
- **SonarQube**: Qualidade geral do código

## Regras de Documentação

### Estrutura da Documentação

A documentação do projeto está organizada na pasta `docs/` com a seguinte estrutura:

```
docs/
├── README.md           # Visão geral e quick start
├── api.md             # Documentação completa da API
├── installation.md    # Guia de instalação e configuração
└── docker.md          # Documentação Docker Compose
```

### Padrões de Documentação

#### 1. Formato e Estilo
- **Formato**: Markdown (.md)
- **Encoding**: UTF-8
- **Idioma**: Português brasileiro
- **Títulos**: Usar hierarquia clara (H1, H2, H3)
- **Código**: Sempre usar blocos de código com syntax highlighting

#### 2. Estrutura dos Documentos

Todos os documentos devem seguir esta estrutura básica:

```markdown
# Título Principal

## Visão Geral
[Descrição breve do conteúdo]

## Seções Principais
[Conteúdo organizado em seções lógicas]

## Exemplos
[Exemplos práticos quando aplicável]

## Troubleshooting
[Problemas comuns e soluções]

---

## Próximos Passos
[Links para documentação relacionada]
```

#### 3. Documentação de API

**Padrão para endpoints**:
```markdown
### Endpoint Name

#### METHOD /path

Descrição do endpoint.

**Parâmetros**:
- `param1` (tipo): Descrição

**Resposta**:
```json
{
  "example": "response"
}
```

**Códigos de Status**:
- `200 OK`: Sucesso
- `404 Not Found`: Recurso não encontrado

**Exemplo de uso**:
```bash
curl http://localhost:8080/endpoint
```
```

#### 4. Documentação de Configuração

**Padrão para configurações**:
```markdown
### Nome da Configuração

**Arquivo**: `caminho/para/arquivo`

**Descrição**: Explicação da configuração

**Exemplo**:
```yaml
configuração:
  exemplo: valor
```

**Variáveis de ambiente**:
- `VAR_NAME`: Descrição da variável
```

### Regras de Manutenção

#### 1. Atualização Obrigatória

A documentação DEVE ser atualizada sempre que:
- Novos endpoints forem adicionados
- Configurações forem modificadas
- Dependências forem alteradas
- Processo de instalação mudar
- Novos serviços forem adicionados ao Docker Compose

#### 2. Versionamento

- Manter histórico de mudanças importantes
- Documentar breaking changes
- Incluir data da última atualização
- Versionar junto com releases do projeto

#### 3. Validação

Antes de commit, verificar:
- [ ] Links funcionam corretamente
- [ ] Exemplos de código são válidos
- [ ] Comandos foram testados
- [ ] Screenshots estão atualizados (se aplicável)
- [ ] Não há informações sensíveis (senhas, tokens)

### Responsabilidades

#### Desenvolvedor
- Atualizar documentação ao implementar features
- Documentar APIs e configurações
- Manter exemplos funcionais
- Reportar inconsistências

#### Tech Lead
- Revisar documentação em PRs
- Garantir padrões de qualidade
- Aprovar mudanças estruturais
- Manter visão geral atualizada

#### DevOps
- Documentar configurações de infraestrutura
- Manter guias de deployment
- Documentar procedimentos de backup
- Atualizar troubleshooting

### Ferramentas e Automação

#### 1. Validação Automática

```bash
# Script para validar links
#!/bin/bash
find docs/ -name "*.md" -exec markdown-link-check {} \;

# Validar sintaxe Markdown
markdownlint docs/

# Verificar ortografia
aspell check docs/*.md
```

#### 2. Geração Automática

- **API Docs**: Usar Swagger/OpenAPI quando possível
- **Diagramas**: Mermaid para diagramas de arquitetura
- **Screenshots**: Automatizar captura quando aplicável

#### 3. Templates

**Template para nova feature**:
```markdown
# Feature: [Nome]

## Descrição
[O que a feature faz]

## Configuração
[Como configurar]

## Uso
[Como usar com exemplos]

## API
[Endpoints relacionados]

## Alterações Recentes

### Resolução de Conflito de Mapeamento (Dezembro 2024)

#### Problema Identificado
- **Erro**: `java.lang.IllegalStateException: Ambiguous mapping. Cannot map 'webController' method`
- **Causa**: Conflito entre métodos `dashboard` no `WebController` e `LoginController`
- **Endpoint conflitante**: `GET /dashboard`

#### Solução Implementada
- **Arquivo alterado**: `/backend/src/main/java/com/sistema/controller/WebController.java`
- **Ação**: Removido método `dashboard` duplicado do `WebController`
- **Resultado**: Mantido apenas o método `dashboard` no `LoginController`

#### Funcionalidades Implementadas

##### 1. Sistema de Login
- **Controller**: `LoginController`
- **Endpoints**:
  - `POST /api/login`: Autenticação de usuários
  - `GET /dashboard`: Página do dashboard pós-login
- **Credenciais de teste**:
  - Username: `demo`
  - Password: `demo123`

##### 2. Interface de Dashboard
- **Template**: `/backend/src/main/resources/templates/dashboard.html`
- **Funcionalidades**:
  - Interface responsiva
  - Navegação integrada
  - Dados dinâmicos via Thymeleaf

#### Testes Realizados
- ✅ Login via API (`POST /api/login`)
- ✅ Acesso ao dashboard (`GET /dashboard`)
- ✅ Página principal (`GET /`)
- ✅ Health checks (`GET /api/health`)
- ✅ Teste Redis (`GET /api/redis-test`)
- ✅ Informações da aplicação (`GET /api/info`)

#### Commit e Deploy
- **Commit**: "Resolve conflito de mapeamento ambíguo removendo método dashboard duplicado do WebController"
- **Arquivos alterados**: 6 arquivos (695 inserções, 325 deleções)
- **Novos arquivos**: `LoginController.java`, `dashboard.html`
- **Status**: Enviado para repositório Git (branch main)
- **Deploy**: Aplicação rodando em http://localhost:8080

## Troubleshooting

### Problemas Conhecidos e Soluções

#### 1. Conflito de Mapeamento Ambíguo
**Sintoma**: `IllegalStateException: Ambiguous mapping`
**Causa**: Múltiplos controladores mapeando o mesmo endpoint
**Solução**: 
- Verificar logs com `docker-compose logs app | grep -A 10 'Ambiguous mapping'`
- Identificar controladores conflitantes
- Remover ou renomear endpoints duplicados

#### 2. Falha na Inicialização da Aplicação
**Sintoma**: Container não inicia ou para imediatamente
**Diagnóstico**:
```bash
# Verificar logs detalhados
docker-compose logs app

# Verificar status dos containers
docker-compose ps

# Rebuild completo
docker-compose down
docker-compose up -d --build
```

#### 3. Problemas de Conectividade com Banco
**Sintoma**: Erro de conexão com PostgreSQL
**Verificação**:
```bash
# Status do PostgreSQL
docker-compose logs postgres

# Teste de conectividade
docker-compose exec app curl -f http://localhost:8080/api/health
```

```

### Qualidade da Documentação

#### Critérios de Qualidade

1. **Clareza**: Linguagem simples e direta
2. **Completude**: Todas as funcionalidades documentadas
3. **Precisão**: Informações corretas e atualizadas
4. **Usabilidade**: Fácil navegação e busca
5. **Exemplos**: Casos práticos e funcionais

#### Checklist de Revisão

- [ ] Título descritivo e claro
- [ ] Visão geral presente
- [ ] Pré-requisitos listados
- [ ] Passos numerados quando aplicável
- [ ] Exemplos de código testados
- [ ] Links internos funcionais
- [ ] Formatação consistente
- [ ] Sem erros de português
- [ ] Informações sensíveis removidas

### Métricas e Monitoramento

#### KPIs da Documentação

- **Cobertura**: % de features documentadas
- **Atualização**: Tempo desde última atualização
- **Qualidade**: Feedback dos usuários
- **Uso**: Páginas mais acessadas

#### Feedback

- Coletar feedback regular dos desenvolvedores
- Monitorar issues relacionadas à documentação
- Realizar reviews periódicas
- Atualizar baseado em dúvidas frequentes

### Integração com Desenvolvimento

#### Definition of Done

Uma feature só está completa quando:
- [ ] Código implementado e testado
- [ ] Documentação atualizada
- [ ] Exemplos funcionais
- [ ] Review da documentação aprovada

#### Pull Request Template

```markdown
## Mudanças
- [ ] Código
- [ ] Documentação
- [ ] Testes

## Documentação
- [ ] README atualizado
- [ ] API docs atualizadas
- [ ] Guias de configuração atualizados
- [ ] Exemplos testados

## Checklist
- [ ] Documentação revisada
- [ ] Links verificados
- [ ] Exemplos funcionais
```