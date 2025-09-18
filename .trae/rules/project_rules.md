# Regras do Projeto - Sistema Java

## Visão Geral do Projeto

Sistema Java completo implementado com Spring Boot 3.2.0, utilizar o banco H2 do springboot, Redis para cache, e smtp mailtrap. O projeto não usa Docker Compose ou docker, o projeto deve iniciar na porta 8080, caso exista alguma coisa na porta 8080 o projeto deve parar, matar o processo e iniciar novamente.

## Regras de Configuração de Porta

### Porta HTTP Obrigatória: 8080

**REGRA FUNDAMENTAL**: A aplicação DEVE rodar exclusivamente na porta **http://localhost:8080**

#### Comportamento Obrigatório:
1. **Porta Fixa**: A aplicação sempre deve iniciar na porta 8080
2. **Verificação de Conflito**: Antes de iniciar, verificar se a porta 8080 está ocupada
3. **Resolução de Conflito**: Se a porta estiver ocupada:
   - Identificar o processo que está usando a porta 8080
   - Parar/matar o processo conflitante
   - Aguardar liberação da porta
   - Iniciar a aplicação na porta 8080
4. **Falha de Inicialização**: Se não conseguir usar a porta 8080, a aplicação NÃO deve iniciar em porta alternativa
5. **URL de Acesso**: A aplicação deve estar sempre disponível em `http://localhost:8080`

#### Configuração no application.yml:
A aplicação deve ser configurada para usar a porta 8080 através da propriedade "server.port" no arquivo de configuração application.yml.

#### Verificação de Porta:
- Usar comandos do sistema para verificar se a porta está em uso
- No Windows: `Get-NetTCPConnection -LocalPort 8080`
- Implementar verificação automática na inicialização

#### Tratamento de Erros:
- Se a porta 8080 não estiver disponível, a aplicação deve falhar com erro claro
- Logs devem indicar claramente o conflito de porta
- Não permitir fallback para outras portas

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
- Validações específicas (email, senha)
- Métodos de busca otimizados (por ID, email)
- Codificação de senhas com BCrypt
- Atualização de último login

**BaseSecurityService** (`com.sistema.service.base.BaseSecurityService`)
- Estende BaseService para operações de segurança
- Validação de tokens e autenticação
- Verificação de permissões e roles
- Operações de autorização padronizadas
- Validação de entrada sanitizada


##### O cadastro do usuário utiliza:
nome
sobre nome
email,
cpf, 
senha

## Sistema de Email e SMTP

**REGRA FUNDAMENTAL**: O sistema implementa um sistema completo de email com suporte a múltiplos provedores SMTP, ativação por email obrigatória e recuperação de senha segura.

### Configuração SMTP Unificada

#### Provedores SMTP Suportados

**1. Mailtrap (Padrão para Desenvolvimento)**
- **Host**: `smtp.mailtrap.io`
- **Porta**: `587`
- **Autenticação**: Obrigatória
- **TLS**: Habilitado
- **Uso**: Desenvolvimento e testes

**2. Gmail (Produção)**
- **Host**: `smtp.gmail.com`
- **Porta**: `587`
- **Autenticação**: Senha de aplicativo obrigatória
- **TLS**: Habilitado
- **Uso**: Produção

**3. Configuração Dinâmica**
- Suporte a múltiplos provedores via painel administrativo
- Configurações armazenadas no banco de dados
- Alternância dinâmica entre provedores

#### Configuração de Ambiente

**application-dev.yml (Desenvolvimento)**:
```yaml
spring:
  mail:
    host: smtp.mailtrap.io
    port: 587
    username: ${MAILTRAP_USERNAME:default_user}
    password: ${MAILTRAP_PASSWORD:default_pass}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
      mail.smtp.connectiontimeout: 5000
      mail.smtp.timeout: 3000
      mail.smtp.writetimeout: 5000

app:
  email:
    enabled: true
    provider: MAILTRAP
    from-email: noreply@sistema.com
```

### Sistema de Ativação por Email

**REGRA OBRIGATÓRIA**: Todo usuário cadastrado deve ativar sua conta através de email antes de poder fazer login.

#### REGRA CRÍTICA DE ENVIO DE EMAIL
**OBRIGATÓRIO**: O sistema DEVE enviar automaticamente um email de ativação imediatamente após o cadastro de qualquer usuário (exceto ADMIN). Esta operação é MANDATÓRIA e não pode falhar silenciosamente.

#### Fluxo de Ativação

**1. Cadastro de Usuário**:
- Email de verificação enviado automaticamente
- Campo `emailVerified` definido como `false`
- Token de verificação gerado com expiração de 24 horas
- **CRÍTICO**: Email DEVE ser enviado antes da confirmação do cadastro

**2. Validação no Login**:
- Verificação obrigatória de `user.isEmailVerified()` antes do login
- Falha com mensagem: "Email não verificado. Verifique sua caixa de entrada e clique no link de verificação."

**3. Endpoints de Verificação**:
- `POST /api/auth/verify-email`: Verifica token de ativação
- `POST /api/auth/resend-verification`: Reenvia email de verificação

#### Componentes de Email

**EmailVerificationService**:
- `generateVerificationToken()`: Gera token de verificação
- `verifyEmailToken()`: Valida e ativa conta
- `needsEmailVerification()`: Verifica necessidade de verificação

**EmailService**:
- `sendVerificationEmail()`: Envia email inicial
- `resendVerificationEmail()`: Reenvia verificação

**SmtpService**:
- Envio físico de emails
- Configuração dinâmica de provedores

#### Entidade User - Campos de Email
- `emailVerified`: Boolean de verificação
- `verificationToken`: Token único
- `verificationTokenExpiresAt`: Expiração do token

#### Templates de Email
- `email-verification.html`: Verificação inicial
- `email-verification-resend.html`: Reenvio
- `password-recovery-token.html`: Recuperação de senha
- `password-recovery-success.html`: Confirmação de nova senha

## Sistema de Recuperação de Senha com Confirmação de Email

**REGRA OBRIGATÓRIA**: O fluxo de recuperação de senha deve incluir confirmação do email cadastrado antes de prosseguir com a recuperação.

### Fluxo de Recuperação de Senha

#### 1. Verificação de CPF
- **Endpoint**: `POST /api/auth/verify-cpf`
- **Entrada**: CPF do usuário
- **Comportamento**:
  - Verificar se existe usuário com o CPF informado
  - Se encontrado, retornar email mascarado para confirmação
  - Se não encontrado, retornar erro genérico por segurança
- **Resposta de Sucesso**:
  A resposta deve conter um objeto JSON com os campos: success (verdadeiro), maskedEmail (email mascarado como "j***@*****.com"), e message (mensagem "CPF encontrado. Confirme o email para prosseguir.").

#### 2. Confirmação de Email
- **Endpoint**: `POST /api/auth/confirm-email`
- **Entrada**: CPF e email completo informado pelo usuário
- **Comportamento**:
  - Verificar se o email informado corresponde ao email cadastrado para o CPF
  - Validar captcha obrigatório
  - Se confirmado, gerar token de recuperação e enviar por email
  - Aplicar rate limiting para evitar spam
- **Resposta de Sucesso**:
  A resposta deve conter um objeto JSON com os campos: success (verdadeiro), message (mensagem "Email confirmado. Token de recuperação enviado."), e email (endereço de email do usuário).

#### 3. Recuperação de Senha (Modificado)
- **Endpoint**: `POST /api/auth/forgot-password`
- **Entrada**: Token de recuperação recebido por email
- **Comportamento**:
  - Validar token de recuperação
  - Gerar nova senha temporária
  - Enviar nova senha por email
  - Invalidar token após uso
- **Pré-requisito**: Email deve ter sido confirmado previamente

### Regras de Segurança

#### Mascaramento de Email
- **Formato**: Mostrar apenas primeiro caractere, *** no meio, e domínio mascarado
- **Exemplo**: `j***@*****.com` para `joao@exemplo.com`
- **Implementação**: Usar `SecurityUtils.maskEmail()` para consistência

#### Rate Limiting
- **Verificação de CPF**: Máximo 5 tentativas por IP por hora
- **Confirmação de Email**: Máximo 3 tentativas por CPF por hora
- **Recuperação de Senha**: Máximo 2 tentativas por email por hora

#### Captcha Obrigatório
- **Confirmação de Email**: Sempre exigir captcha
- **Recuperação de Senha**: Captcha obrigatório se token fornecido

#### Logs de Segurança
- Registrar todas as tentativas de verificação de CPF
- Registrar tentativas de confirmação de email (sucesso e falha)
- Registrar uso de tokens de recuperação
- Incluir IP, timestamp e resultado da operação

### Componentes a Implementar

#### Novos Endpoints
1. `POST /api/auth/verify-cpf`: Verificação inicial de CPF
2. `POST /api/auth/confirm-email`: Confirmação de email mascarado
3. Modificar `POST /api/auth/forgot-password`: Usar token de recuperação

#### Novos DTOs
- `VerifyCpfRequest`: CPF para verificação
- `VerifyCpfResponse`: Email mascarado e status
- `ConfirmEmailRequest`: CPF, email e captcha
- `ConfirmEmailResponse`: Status de confirmação

#### Serviços
- `PasswordRecoveryService`: Gerenciar fluxo completo de recuperação
- Modificar `AuthService`: Integrar novo fluxo
- Usar `SecurityUtils`: Para mascaramento de email

#### Validações
- CPF deve ser válido (usar `CpfValidator`)
- Email deve ter formato válido
- Captcha obrigatório em confirmação de email
- Rate limiting em todas as operações

### Templates de Email
- `password-recovery-token.html`: Email com token de recuperação
- `password-recovery-success.html`: Confirmação de nova senha

### Configurações
Configurar o sistema de recuperação de senha com token de expiração de 30 minutos e rate limiting: máximo 5 tentativas por hora para verificação de CPF, 3 tentativas por hora para confirmação de email, e 2 tentativas por hora para reset de senha.


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

**CpfGenerator** (`com.sistema.util.CpfGenerator`)
- **REGRA OBRIGATÓRIA**: Para testes e desenvolvimento, sempre usar CPFs válidos gerados por esta classe
- **Baseado em**: Repositório gabriel-logan/Gerador-CPF-e-CNPJ-valido (https://github.com/gabriel-logan/Gerador-CPF-e-CNPJ-valido)
- **Licença**: MIT License - implementação open source confiável e testada
- **Algoritmo**: Implementa o algoritmo oficial brasileiro de geração de CPF com dígitos verificadores
- **Características**:
  - Geração de CPFs válidos com algoritmo de verificação correto
  - Suporte a formatação com e sem máscara
  - Validação integrada dos CPFs gerados
  - Métodos estáticos para facilitar uso
  - Compatibilidade total com validadores brasileiros
- **Métodos disponíveis**:
  - `generateCpf()`: Gera um CPF válido aleatório sem formatação (11 dígitos)
  - `generateCpfWithMask()`: Gera CPF válido com formatação (xxx.xxx.xxx-xx)
  - `isValidCpf(String cpf)`: Valida se um CPF é válido
  - `formatCpf(String cpf)`: Formata um CPF adicionando máscara
  - `cleanCpf(String cpf)`: Remove formatação de um CPF
- **Uso em Testes**: Sempre utilizar CPFs gerados por esta classe em testes de integração e unitários
- **Desenvolvimento**: Para dados de exemplo e demonstração, usar apenas CPFs válidos desta classe
- **Validação**: CPFs gerados são 100% compatíveis com a validação implementada em `CpfValidator`
- **Qualidade**: Implementação baseada em biblioteca open source amplamente testada e utilizada
- **Localização**: `.\backend\src\main\java\com\sistema\util\CpfGenerator.java`

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
  - H2 Database (banco em memória)
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

### Documentação Implementada

O projeto possui uma estrutura completa de documentação na pasta `docs/`:

#### Arquivos de Documentação
- **docs/README.md**: Visão geral do projeto, quick start e navegação
- **docs/api.md**: Documentação completa da API com todos os endpoints
- **docs/installation.md**: Guia detalhado de instalação e configuração

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



### Status do Projeto
- ✅ **Aplicação funcionando**: Todos os serviços rodando na porta 8080
- ✅ **Conectividade H2**: Banco de dados em memória configurado e testado
- ✅ **Conectividade Redis**: Configurada e testada
- ✅ **SMTP Mailtrap**: Configurado para envio de emails
- ✅ **Endpoints REST**: Implementados e funcionais
- ✅ **Configuração de Porta**: Aplicação acessível em http://localhost:8080
- ✅ **Desenvolvimento Local**: Configurado para porta 8080 standalone
- ✅ **Documentação**: Estrutura completa implementada
- ✅ **Guias de instalação**: Documentação detalhada criada
- ✅ **API Documentation**: Todos os endpoints documentados
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
- Serviços disponíveis (Spring Boot, H2 Database, Redis, SMTP Mailtrap)
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
- Instalação rápida (3 comandos)
- Instalação detalhada passo a passo
- Configurações de ambiente
- Comandos úteis para desenvolvimento
- Desenvolvimento local com hot reload
- Troubleshooting de problemas comuns
- Configurações para ambiente de produção


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

## Testes de Sistema (End-to-End)

### Visão Geral dos Testes E2E

Os Testes End-to-End (E2E) são testes que verificam o funcionamento completo do sistema, simulando o comportamento real do usuário através de todas as camadas da aplicação. No projeto Sistema Java, os testes E2E garantem que toda a funcionalidade esteja funcionando corretamente desde a interface até o banco de dados.

### Características dos Testes E2E

#### 1. Cobertura Completa
- **Fluxo completo**: Testam desde a entrada do usuário até a resposta final
- **Integração real**: Usam banco de dados real (H2) e Redis real
- **Ambiente próximo à produção**: Configuração similar ao ambiente de produção
- **Validação de regras de negócio**: Verificam se as regras estão sendo aplicadas corretamente

#### 2. Simulação de Usuário Real
- **Comportamento real**: Simulam ações que um usuário real faria
- **Sequência de operações**: Testam fluxos completos de uso
- **Dados realistas**: Usam dados de teste que representam casos reais
- **Cenários complexos**: Testam situações que envolvem múltiplas funcionalidades

#### 3. Validação de Integração
- **Spring Boot completo**: Aplicação Spring Boot real em execução
- **Banco de dados**: H2 Database com dados persistidos
- **Cache Redis**: Operações reais de cache
- **SMTP**: Envio real de emails (configurado para Mailtrap)
- **Segurança**: Validação real de tokens JWT e autenticação

### Estrutura de Testes E2E

#### Organização de Diretórios

```
src/test/java/com/sistema/e2e/
├── config/                    # Configurações específicas para E2E
│   ├── E2ETestConfiguration.java
│   └── TestDataInitializer.java
├── scenarios/                 # Cenários de teste específicos
│   ├── AuthenticationE2ETest.java
│   ├── UserManagementE2ETest.java
│   ├── ApiIntegrationE2ETest.java
│   └── WebInterfaceE2ETest.java
├── utils/                     # Utilitários para testes E2E
│   ├── E2ETestHelper.java
│   ├── TestDataBuilder.java
│   └── AssertionHelper.java
└── base/                      # Classes base para testes E2E
    ├── BaseE2ETest.java
    └── E2ETestContext.java
```

#### Configuração de Ambiente

**E2ETestConfiguration.java**:
Criar uma classe de configuração anotada com @Configuration e @Profile("e2e") que define dois beans: um TestRestTemplate marcado como @Primary para requisições HTTP de teste, e um TestDataInitializer para inicialização de dados de teste.

**application-e2e.yml**:
Configurar o perfil ativo como "e2e", usar banco H2 em memória com nome "e2etestdb" configurado para não fechar automaticamente, definir o driver H2, configurar JPA para recriar o schema a cada execução (create-drop), desabilitar logs SQL, configurar Redis para localhost na porta 6379 usando database 1 (diferente para testes E2E), e configurar email com Mailtrap usando host sandbox.smtp.mailtrap.io na porta 2525 com as credenciais específicas.

logging:
  level:
    com.sistema: INFO
    org.springframework.web: WARN
    org.hibernate: WARN

app:
  jwt:
    access-token-expiration: 300000  # 5 minutos para testes
    refresh-token-expiration: 600000  # 10 minutos para testes
  captcha:
    expiration: 300000  # 5 minutos para testes
  attempt:
    max-attempts: 3  # Reduzido para testes
    lockout-duration: 60000  # 1 minuto para testes
```

### Tipos de Testes E2E

#### 1. Testes de Autenticação E2E

**Cenários testados**:
- Cadastro completo de usuário com verificação de email
- Login com credenciais válidas
- Login com credenciais inválidas
- Controle de tentativas e captcha
- Renovação de tokens
- Logout e invalidação de tokens
- Recuperação de senha

#### 2. Testes de API E2E

**Cenários testados**:
- Health checks e monitoramento
- Endpoints de autenticação
- Endpoints de captcha
- Validação de autorização
- Tratamento de erros
- Performance básica

#### 3. Testes de Interface Web E2E

**Cenários testados**:
- Carregamento da página inicial
- Navegação entre páginas
- Formulários de login
- Dashboard após autenticação
- Responsividade básica

### Utilitários para Testes E2E

#### E2ETestHelper

Criar um componente Spring que fornece métodos utilitários para testes E2E:
- Método para autenticar e obter token: cria um usuário de teste, faz login via API e retorna o token de acesso
- Método para criar usuário de teste: cria um usuário com email "teste@e2e.com", senha codificada "senha123", email verificado e habilitado
- Método para limpeza: remove todos os usuários do banco e limpa cache Redis se necessário
- Injetar dependências: TestRestTemplate para requisições HTTP e UserRepository para operações de banco
```

#### TestDataBuilder

Criar uma classe utilitária com padrão Builder para construção de dados de teste:
- Métodos estáticos para criar builders de RegisterRequest e LoginRequest
- RegisterRequestBuilder com valores padrão: email "teste@email.com", senha "senha123", nome "Usuário Teste", sobrenome "E2E", e CPF gerado automaticamente
- Métodos fluentes para personalizar email e senha
- Método build() que retorna o objeto RegisterRequest construído
- Usar CpfGenerator para gerar CPFs válidos automaticamente

### Configuração de Execução

#### Maven Configuration

**pom.xml**:
Configurar o plugin maven-failsafe-plugin versão 3.0.0-M9 com duas execuções: uma para testes E2E na fase integration-test que inclui arquivos terminados em "E2ETest.java" e define o perfil Spring como "e2e", e outra para verificação na fase verify.

#### Execução dos Testes

**Comandos para executar testes E2E**:
Para executar apenas testes E2E, usar o comando Maven verify com parâmetro -Dtest="*E2ETest". Para executar com perfil específico, adicionar -Dspring.profiles.active=e2e. Para logs detalhados, incluir a opção -X no comando. Para execução em paralelo, usar a opção -T 4.

### Boas Práticas para Testes E2E

#### 1. Isolamento de Testes
- **Cleanup**: Limpar dados entre testes
- **Dados únicos**: Usar dados únicos para cada teste
- **Estado limpo**: Garantir estado inicial consistente

#### 2. Performance
- **Execução paralela**: Configurar execução paralela quando possível
- **Timeouts**: Definir timeouts apropriados
- **Recursos**: Gerenciar recursos adequadamente

#### 3. Manutenibilidade
- **Page Objects**: Usar padrão Page Object para interface web
- **Helpers**: Criar utilitários reutilizáveis
- **Configuração**: Centralizar configurações de teste

#### 4. Relatórios
- **Logs**: Gerar logs detalhados para debugging
- **Screenshots**: Capturar screenshots em caso de falha
- **Métricas**: Coletar métricas de performance

### Integração com CI/CD

#### Pipeline de Testes E2E

Configurar workflow do GitHub Actions para testes E2E que executa em push e pull request, usando Ubuntu latest com serviço Redis 7-alpine na porta 6379. O workflow deve fazer checkout do código, configurar JDK 21, executar testes E2E com Maven usando perfil e2e, e fazer upload dos relatórios de teste como artefatos sempre que executar.

### Métricas e Monitoramento

#### KPIs dos Testes E2E
- **Taxa de sucesso**: % de testes que passam
- **Tempo de execução**: Duração total dos testes E2E
- **Cobertura de cenários**: % de fluxos de usuário testados
- **Detecção de bugs**: Bugs encontrados pelos testes E2E

#### Relatórios
- **Surefire Reports**: Relatórios padrão do Maven
- **Allure Reports**: Relatórios detalhados com Allure
- **Custom Reports**: Relatórios customizados para métricas específicas

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

### Estratégias de Teste

#### 1. Testes Unitários

**Características**:
- Testam uma única unidade de código isoladamente
- Usam mocks para dependências externas
- Execução rápida (< 100ms por teste)
- Cobertura de código > 80%

**Exemplo de Estrutura**:
A classe de teste HealthServiceTest deve usar a anotação @ExtendWith(MockitoExtension.class) e conter um mock do RedisTemplate injetado no HealthService. O teste shouldReturnUpWhenRedisIsAvailable deve verificar que quando o Redis está disponível (hasKey retorna true), o método checkRedisHealth retorna um HealthStatus com status "UP".

#### 2. Testes de Integração

**Características**:
- Testam integração entre componentes
- Usam banco de dados real ou Testcontainers
- Verificam fluxo completo de dados
- Executados em ambiente isolado

**Configuração com Testcontainers**:
A classe HealthControllerIntegrationTest deve usar as anotações @SpringBootTest e @Testcontainers. Para testes, o H2 Database em memória é usado automaticamente, não sendo necessário container externo. A classe deve injetar um TestRestTemplate e conter o teste shouldReturnHealthCheckWithStatus200 que verifica se o endpoint /api/health retorna status 200 e contém a chave "status" no corpo da resposta.

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

**Exemplo de Teste Given-When-Then**:
O teste `shouldCalculateTotalWhenValidItemsProvided` deve seguir a estrutura: Given (preparar lista de itens com "item1" valor 10.0 e "item2" valor 20.0), When (executar calculateTotal do calculator), Then (verificar que o total é igual a 30.0). Usar anotação @Test e assertThat para verificações.

### Ferramentas de Qualidade

#### 1. Cobertura de Código

**Configuração do Plugin JaCoCo**:
No `pom.xml`, adicionar o plugin JaCoCo (org.jacoco:jacoco-maven-plugin) versão 0.8.8 com duas execuções: uma para preparar o agente (goal: prepare-agent) e outra para gerar relatórios (id: report, phase: test, goal: report). Esta configuração permite coleta de dados de cobertura durante os testes e geração automática de relatórios.

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

**Configuração de Testes (application-test.yml)**:
O arquivo de configuração para testes deve configurar o Spring com datasource H2 em memória (jdbc:h2:mem:testdb), driver H2, JPA com Hibernate configurado para criar e dropar tabelas automaticamente (create-drop), Redis no localhost porta 6370 (diferente da produção), e logging DEBUG para o pacote com.sistema.

#### Configuração para Testes

**application-test.yml**:
- H2 Database em memória para testes de integração
- Redis configurado na porta 6370 para cache de testes
- Configuração isolada do ambiente de desenvolvimento
- Dados temporários em memória para testes

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

**Configuração do Pipeline de Testes no GitHub Actions**:
O arquivo `.github/workflows/test.yml` deve configurar um workflow chamado "Tests" que é executado em push e pull requests. O job "test" roda no Ubuntu latest e executa os seguintes passos: checkout do código, configuração do JDK 21, execução de testes unitários com `./mvnw test`, execução de testes de integração com `./mvnw verify -P integration-tests`, geração de relatório de cobertura com `./mvnw jacoco:report`, e upload da cobertura usando a action codecov/codecov-action@v3.

#### Quality Gates

- **Todos os testes devem passar**: 100% de sucesso
- **Cobertura mínima**: 70% para merge
- **Sem vulnerabilidades críticas**: SonarQube analysis
- **Performance**: Testes não devem exceder tempo limite

### Troubleshooting de Testes

#### Problemas Conuns

**1. Testes Flaky (Instáveis)**:
- Dependência de tempo ou ordem
- Estado compartilhado entre testes
- Recursos externos indisponíveis

**Solução**:
**Soluções para Testes Flaky**:
- Para timeouts: usar anotação @Timeout com valor 5 segundos e unidade TimeUnit.SECONDS no método de teste
- Para isolamento: usar anotação @BeforeEach no método setUp para limpar estado antes de cada teste

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

### Padrões de Implementação TDD

#### 1. Workflow de Desenvolvimento

**Sequência Obrigatória**:
1. **Escrever teste falhando** (Red)
2. **Implementar código mínimo** (Green)
3. **Refatorar mantendo testes** (Refactor)
4. **Commit apenas com testes passando**

**Exemplo Prático - Implementando UserService**:

PASSO 1 (RED): Criar teste shouldCreateUserWithValidData que verifica se o UserService.createUser aceita um CreateUserRequest com nome "João Silva", email "joao@email.com" e senha "senha123", e retorna um User com ID não nulo, nome e email corretos, e senha criptografada (diferente da original).

PASSO 2 (GREEN): Implementar a classe UserService com anotação @Service contendo o método createUser que aceita CreateUserRequest e retorna User.
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        return user;
    }
}

// PASSO 3: REFACTOR - Melhorar sem quebrar testes
@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public User createUser(CreateUserRequest request) {
        validateUserRequest(request);
        
        User user = User.builder()
            .id(UUID.randomUUID().toString())
            .name(request.getName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .createdAt(LocalDateTime.now())
            .build();
            
        return userRepository.save(user);
    }
    
    private void validateUserRequest(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email já cadastrado");
        }
    }
}
```

#### 2. Padrões de Teste por Camada

**Controller Layer**:
A classe UserControllerTest deve usar @WebMvcTest(UserController.class) e injetar MockMvc e um @MockBean do UserService. O teste shouldReturn201WhenValidUserData deve verificar que uma requisição POST para /api/users com dados válidos (nome "João", email "joao@email.com", senha "senha123") retorna status 201 e um JSON com id "123" e name "João".

**Service Layer**:
A classe UserServiceTest deve usar @ExtendWith(MockitoExtension.class) e conter mocks do UserRepository e PasswordEncoder injetados no UserService. O teste shouldThrowExceptionWhenEmailAlreadyExists deve verificar que quando o email já existe (userRepository.existsByEmail retorna true), o método createUser lança UserAlreadyExistsException com mensagem "Email já cadastrado".

**Repository Layer**:
A classe UserRepositoryTest deve usar @DataJpaTest e injetar TestEntityManager e UserRepository. O teste shouldFindUserByEmail deve persistir um usuário com nome "João" e email "joao@email.com", depois verificar que o método findByEmail encontra o usuário corretamente.

#### 3. Test Data Builders

**Padrão Builder para Testes**:
A classe UserTestDataBuilder deve conter valores padrão (id UUID aleatório, name "João Silva", email "joao@email.com", password "senha123", createdAt atual) e métodos fluentes: aUser() estático para criar instância, withId(), withName(), withEmail() para personalizar valores, e build() que retorna User construído com os valores definidos.
@Test
void shouldUpdateUserName() {
    // Given
    User user = aUser()
        .withName("João Original")
        .withEmail("joao@test.com")
        .build();
    
    // When & Then...
}
```

#### 4. Testes de Comportamento (BDD Style)

**Estrutura Given-When-Then Expandida**:
**Teste BDD userTriesToLoginWithValidCredentials()**:
Método anotado com @Test e @DisplayName("Cenário: Usuário tenta fazer login com credenciais válidas"). Given: cria User usando builder pattern com email "joao@email.com" e senha codificada "senha123", mocka userRepository.findByEmail retornando Optional.of(existingUser) e passwordEncoder.matches retornando true. When: cria LoginRequest com email "joao@email.com" e senha "senha123", executa authService.login. Then: verifica que response.isSuccess() é true, token não está em branco, email do usuário é correto, e jwtService.isValidToken retorna true.

#### 5. Testes de Exceções e Edge Cases

**Padrão para Testes de Exceção**:
**Padrão para Testes de Exceção**:
O teste `shouldThrowValidationExceptionWhenInvalidData` deve usar @Test e @DisplayName, criar CreateUserRequest com nome vazio, email inválido e senha curta, e verificar que userService.createUser lança ValidationException com mensagens específicas sobre nome obrigatório, email inválido e senha mínima.

Para testes parametrizados, usar @ParameterizedTest com @ValueSource contendo strings inválidas ("", " ", "a", "ab"), verificando que cada nome inválido causa ValidationException.

#### 6. Testes de Integração com Transações

**Padrão para Testes Transacionais**:
**Padrão para Testes Transacionais**:
A classe `UserServiceIntegrationTest` deve usar anotações @SpringBootTest, @Transactional e @Rollback. Injetar UserService e UserRepository com @Autowired. O teste `shouldCreateUserAndPersistInDatabase` deve criar CreateUserRequest com dados válidos, executar userService.createUser, verificar que o ID não é nulo, e confirmar persistência buscando o usuário no repositório.
        assertThat(persistedUser.get().getName()).isEqualTo("João");
    }
}
```

### Diretrizes de Qualidade TDD

#### 1. Regras de Ouro

- **Teste primeiro, sempre**: Nunca escrever código sem teste falhando
- **Um teste, uma responsabilidade**: Cada teste verifica apenas um comportamento
- **Testes independentes**: Não devem depender uns dos outros
- **Nomes expressivos**: Teste deve documentar o comportamento esperado
- **Arrange-Act-Assert**: Estrutura clara e consistente

#### 2. Code Review Checklist

**Para Testes**:
- [ ] Teste falha antes da implementação?
- [ ] Nome do teste é descritivo e claro?
- [ ] Usa Given-When-Then ou Arrange-Act-Assert?
- [ ] Verifica apenas um comportamento?
- [ ] Usa mocks apropriadamente?
- [ ] Tem assertions suficientes?

**Para Código de Produção**:
- [ ] Implementação mínima para passar no teste?
- [ ] Código foi refatorado após passar?
- [ ] Mantém todos os testes passando?
- [ ] Segue padrões de design estabelecidos?

#### 3. Métricas de Qualidade TDD

**Indicadores de Sucesso**:
- **Cobertura de testes**: > 85%
- **Tempo de feedback**: < 10 minutos para suite completa
- **Taxa de bugs em produção**: < 1% por release
- **Velocidade de desenvolvimento**: Mantida ou aumentada
- **Confiança do time**: Alta para fazer mudanças

## Regras de Documentação

### Estrutura da Documentação

A documentação do projeto está organizada na pasta `docs/` com a seguinte estrutura:

```
docs/
├── README.md           # Visão geral e quick start
├── api.md             # Documentação completa da API
└── installation.md    # Guia de instalação e configuração
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

**Estrutura Básica de Documentos**:
Todo documento deve ter título principal (H1), seção de visão geral com descrição breve, seções principais com conteúdo organizado, exemplos práticos quando aplicável, troubleshooting com problemas e soluções, separador horizontal, e seção de próximos passos com links relacionados.

#### 3. Documentação de API

**Padrão para Documentação de Endpoints**:
Cada endpoint deve ter nome (H3), método e path (H4), descrição, seção de parâmetros listando nome, tipo e descrição, seção de resposta descrevendo estrutura JSON, códigos de status com significado (200 OK para sucesso, 404 Not Found para recurso não encontrado), e exemplo de uso com comando curl.

#### 4. Documentação de Configuração

**Padrão para configurações**:
```markdown
### Nome da Configuração

**Arquivo**: `caminho/para/arquivo`

**Descrição**: Explicação da configuração

**Exemplo**:
A configuração deve conter a chave "configuração" com subcampo "exemplo" definido como "valor".

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
- Novos serviços forem adicionados ao projeto

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
- Verificar logs da aplicação para identificar endpoints conflitantes
- Identificar controladores conflitantes
- Remover ou renomear endpoints duplicados

#### 2. Falha na Inicialização da Aplicação
**Sintoma**: Aplicação não inicia ou para imediatamente
**Diagnóstico**:
```bash
# Verificar logs detalhados
mvn spring-boot:run

# Verificar se porta 8080 está ocupada (Windows)
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue

# Alternativa com netstat (se disponível)
netstat -ano | findstr :8080

# Identificar processo usando a porta 8080
Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess

# Matar processo na porta 8080 se necessário
Stop-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess -Force

# Alternativa com taskkill
taskkill /PID <PID> /F

# Verificar se a porta foi liberada
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
```

#### 3. Problemas de Conectividade com Banco H2
**Sintoma**: Erro de conexão com H2 Database
**Verificação**:
```bash
# Teste de conectividade
curl -f http://localhost:8080/api/health

# Verificar console H2 (se habilitado)
# http://localhost:8080/h2-console
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

### Configuração Obrigatória de Porta

**IMPORTANTE**: A configuração de porta no `application.yml` DEVE ser sempre 8080:

```yaml
server:
  port: 8080
  servlet:
    context-path: /
```

**Regras de Configuração de Porta**:
- ✅ **OBRIGATÓRIO**: `server.port: 8080`
- ❌ **PROIBIDO**: Qualquer outra porta (8081, 9090, etc.)
- ❌ **PROIBIDO**: Porta dinâmica (`server.port: 0`)
- ❌ **PROIBIDO**: Configuração condicional de porta por perfil

## Sistema de Logging e Debug

### REGRA FUNDAMENTAL DE DESENVOLVIMENTO

**OBRIGATÓRIO**: Em ambiente de desenvolvimento, TODAS as funções, métodos e classes devem implementar logging detalhado e tratamento de exceções extremamente verboso para facilitar o debug.

### Configuração de Logging por Ambiente

#### Ambiente de Desenvolvimento (application-dev.yml)
**Configuração de Logging para Desenvolvimento**:
O arquivo `application-dev.yml` deve configurar logging detalhado com nível DEBUG para o pacote `com.sistema` e componentes Spring (security, web, data, transaction), DEBUG para SQL do Hibernate, TRACE para BasicBinder, e DEBUG para cache e Redis. Os padrões de console e arquivo devem incluir timestamp completo, thread, nível, logger com linha e mensagem. O arquivo de log deve ser `logs/sistema-dev.log` com tamanho máximo de 100MB e histórico de 30 arquivos.

#### Ambiente de Produção (application-prod.yml)
**Configuração de Logging para Produção**:
O arquivo `application-prod.yml` deve configurar logging otimizado com nível INFO para o pacote `com.sistema`, WARN para Spring e Hibernate. Os padrões de console e arquivo devem usar timestamp simplificado sem milissegundos, thread, nível, logger e mensagem. O arquivo de log deve ser `logs/sistema-prod.log` com tamanho máximo de 50MB e histórico de 90 arquivos para conservar espaço em disco.

### Regras Obrigatórias de Logging em Desenvolvimento

#### 1. Logging de Entrada e Saída de Métodos

**OBRIGATÓRIO**: Todo método público deve logar entrada e saída com parâmetros e retorno.

```java
@Service
public class ExampleService extends BaseService {
    
    public UserDto createUser(CreateUserRequest request) {
        logDebug("Iniciando createUser com parâmetros: {}", request);
        
        try {
            // Validação de entrada
            logDebug("Validando dados de entrada para usuário: {}", request.getEmail());
            validateNotNull(request, "Request não pode ser nulo");
            validateNotBlank(request.getEmail(), "Email é obrigatório");
            
            // Lógica de negócio
            logDebug("Criando usuário no banco de dados");
            User user = userRepository.save(convertToEntity(request));
            logDebug("Usuário criado com sucesso. ID: {}", user.getId());
            
            // Conversão de resposta
            UserDto response = convertToDto(user);
            logDebug("Finalizando createUser. Retornando: {}", response);
            
            return response;
            
        } catch (Exception e) {
            logError("Erro ao criar usuário. Request: {}, Erro: {}", request, e.getMessage(), e);
            throw new ServiceException("Falha ao criar usuário", e);
        }
    }
}
```

#### 2. Logging de Operações de Banco de Dados

**OBRIGATÓRIO**: Todas as operações de banco devem ser logadas com detalhes.

```java
@Repository
public class UserRepository extends BaseService {
    
    public Optional<User> findByEmail(String email) {
        logDebug("Buscando usuário por email: {}", email);
        
        try {
            Optional<User> user = repository.findByEmail(email);
            
            if (user.isPresent()) {
                logDebug("Usuário encontrado. ID: {}, Email: {}", user.get().getId(), email);
            } else {
                logDebug("Nenhum usuário encontrado para email: {}", email);
            }
            
            return user;
            
        } catch (Exception e) {
            logError("Erro ao buscar usuário por email: {}. Erro: {}", email, e.getMessage(), e);
            throw new RepositoryException("Falha na consulta de usuário", e);
        }
    }
}
```

#### 3. Logging de Operações Redis

**OBRIGATÓRIO**: Todas as operações de cache devem ser logadas.

```java
@Service
public class CacheService extends BaseRedisService {
    
    public void storeUserSession(String userId, String sessionData) {
        String key = "user:session:" + userId;
        logDebug("Armazenando sessão no Redis. Key: {}, Data: {}", key, sessionData);
        
        try {
            redisTemplate.opsForValue().set(key, sessionData, Duration.ofHours(24));
            logDebug("Sessão armazenada com sucesso no Redis. Key: {}", key);
            
        } catch (Exception e) {
            logError("Erro ao armazenar sessão no Redis. Key: {}, Data: {}, Erro: {}", 
                    key, sessionData, e.getMessage(), e);
            throw new CacheException("Falha ao armazenar sessão", e);
        }
    }
}
```

#### 4. Logging de Controladores REST

**OBRIGATÓRIO**: Todos os endpoints devem logar requisições e respostas.

**Padrão para Logging de Controladores REST**:
A classe UserController deve estender BaseService, usar @RestController e @RequestMapping. O método createUser deve capturar IP do cliente, logar requisição recebida com IP e dados, executar userService.createUser, logar sucesso com IP, UserID e resposta, retornar 201 CREATED. Para ValidationException, logar warning com IP, request e erro, retornar 400 Bad Request. Para Exception genérica, logar error com IP, request e stacktrace, retornar 500 Internal Server Error.

### Tratamento de Exceções Verboso

#### 1. Exceções Customizadas com Contexto Detalhado

**OBRIGATÓRIO**: Todas as exceções devem incluir contexto máximo para debug.

**Padrão para Exceções Customizadas com Contexto**:
A classe ServiceException deve estender RuntimeException, ter campos operation, context (Map) e errorCode. O construtor deve receber message, operation, context e cause, chamar super com buildDetailedMessage, e definir campos. O método buildDetailedMessage deve concatenar "Operação: " + operation + " | Mensagem: " + message + " | Contexto: " + context.

#### 2. Global Exception Handler Verboso

**OBRIGATÓRIO**: Handler global deve logar todos os detalhes da exceção.

**Padrão para Global Exception Handler Verboso**:
A classe GlobalExceptionHandler deve usar @ControllerAdvice e estender BaseService. O método handleGenericException deve gerar requestId único, capturar IP, UserAgent, URL e método, logar erro crítico com todos os detalhes incluindo stacktrace completo, logar parâmetros da requisição se existirem, logar headers seguros, e retornar ApiResponse com erro e requestId. Implementar getFullStackTrace usando StringWriter e PrintWriter.

### Logging de Segurança

#### 1. Autenticação e Autorização

**OBRIGATÓRIO**: Todas as operações de segurança devem ser logadas.

**Padrão para Logging de Autenticação**:
A classe AuthService deve estender BaseUserService e usar @Service. O método authenticate deve logar tentativa de login com email e IP, buscar usuário por email e logar debug com ID encontrado, verificar senha com passwordEncoder e logar warning para senha incorreta, verificar se conta está ativa e logar warning para conta desabilitada, gerar tokens JWT, logar sucesso de autenticação com email, IP e UserID, e capturar exceções logando erro com email, IP e mensagem.

### Logging de Performance

#### 1. Monitoramento de Tempo de Execução

**OBRIGATÓRIO**: Métodos críticos devem medir tempo de execução.

**Padrão para Monitoramento de Performance**:
A classe PerformanceService deve estender BaseService e usar @Service. O método executeComplexOperation deve capturar startTime, logar debug de início com ID e timestamp, executar performComplexLogic, calcular duração, logar info de conclusão com ID e duração, alertar com warning se duração > 5000ms, e no catch calcular duração e logar erro com ID, duração e mensagem de erro.

### Configuração de Debug Específico

#### 1. Debug de SQL e JPA

**OBRIGATÓRIO**: Em desenvolvimento, mostrar todas as queries SQL.

```yaml
# application-dev.yml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
        type: trace
        stat: debug
        
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    org.hibernate.type.descriptor.sql.BasicExtractor: TRACE
```

#### 2. Debug de Redis

**OBRIGATÓRIO**: Logar todas as operações Redis em desenvolvimento.

**Configuração de Debug para Redis**:
No arquivo `application-dev.yml`, configure o logging com nível DEBUG para `redis.clients.lettuce` e `org.springframework.data.redis` para obter logs detalhados das operações Redis e conexões Lettuce.

#### 3. Debug de Security

**OBRIGATÓRIO**: Logar todo o fluxo de segurança em desenvolvimento.

**Configuração de Debug para Security**:
No arquivo `application-dev.yml`, configure o logging com nível DEBUG para `org.springframework.security`, `com.sistema.security` e `com.sistema.filter` para obter logs detalhados de todo o fluxo de segurança, autenticação e filtros.

### Utilitários de Debug

#### 1. Debug Utils

**OBRIGATÓRIO**: Classe utilitária para debug avançado.

**Padrão para DebugUtils**:
A classe DebugUtils deve usar @Component com métodos estáticos. O método logObjectState deve verificar se objeto é null, usar ObjectMapper para serializar em JSON, logar debug com contexto, classe e estado JSON, ou logar erro de serialização. O método logMethodExecution deve logar execução com classe, método, argumentos, resultado e duração. O método logMemoryUsage deve usar Runtime para obter memória total, livre e usada, calculando em MB e logando com contexto.

### Regras de Implementação

#### 1. Obrigatório em Todas as Classes

**REGRA**: Toda classe de serviço, controlador, repositório e componente DEVE implementar:

1. **Logging de entrada e saída** de todos os métodos públicos
2. **Try-catch com logging detalhado** em todos os métodos
3. **Logging de estado de objetos** em pontos críticos
4. **Medição de performance** em operações complexas
5. **Logging de contexto** com informações relevantes

#### 2. Padrão de Nomenclatura de Logs

**REGRA**: Usar prefixos padronizados para facilitar filtros:

- `INICIO` - Início de operação
- `FIM` - Fim de operação
- `ERRO` - Erros e exceções
- `WARN` - Avisos e situações suspeitas
- `PERFORMANCE` - Alertas de performance
- `SECURITY` - Eventos de segurança
- `DEBUG` - Informações de debug
- `VALIDATION` - Erros de validação
- `DATABASE` - Operações de banco
- `CACHE` - Operações de cache
- `EMAIL` - Operações de email

#### 3. Informações Obrigatórias nos Logs

**REGRA**: Todo log deve incluir quando aplicável:

1. **Timestamp** preciso
2. **ID da operação** ou request
3. **IP do cliente** (em controllers)
4. **ID do usuário** (quando autenticado)
5. **Parâmetros de entrada**
6. **Resultado da operação**
7. **Tempo de execução**
8. **Stack trace completo** (em erros)

### Ativação do Modo Debug

#### 1. Profile de Desenvolvimento

**OBRIGATÓRIO**: Usar profile específico para desenvolvimento com debug máximo.

**Comandos para Ativação do Modo Debug**:
Para iniciar a aplicação em modo debug, execute o comando `java -jar app.jar --spring.profiles.active=dev --debug`. Alternativamente, ao executar via IDE, configure as propriedades JVM com `-Dspring.profiles.active=dev -Ddebug=true` para ativar o profile de desenvolvimento e o modo debug.

#### 2. Configuração de IDE

**RECOMENDADO**: Configurar IDE para debug avançado:

- Breakpoints condicionais
- Logging de variáveis
- Watch expressions
- Memory profiling

Esta seção estabelece as regras fundamentais para logging detalhado e debug verboso em ambiente de desenvolvimento, garantindo máxima visibilidade para identificação e resolução de problemas.

## Sistema de Configuração de Email SMTP

### REGRA FUNDAMENTAL DE CONFIGURAÇÃO

**OBRIGATÓRIO**: O sistema deve suportar múltiplos provedores SMTP com configuração dinâmica através de painel administrativo, mantendo Mailtrap como padrão e Gmail como alternativa principal.

### Provedores SMTP Suportados

#### 1. Mailtrap (Padrão)
**Configuração Padrão**: Usado para desenvolvimento e testes

**Configuração SMTP do Mailtrap**:
O arquivo `application-dev.yml` deve configurar o Spring Mail com host `smtp.mailtrap.io`, porta 587, username e password usando variáveis de ambiente com valores padrão. As propriedades SMTP devem incluir autenticação habilitada, STARTTLS habilitado, timeout de conexão de 5000ms, timeout geral de 3000ms e timeout de escrita de 5000ms. A configuração da aplicação deve definir provider como MAILTRAP, email de origem como "noreply@sistema.com" e email habilitado.

#### 2. Gmail (Alternativa)
**Configuração com Senha de App**: Usar senha específica de aplicativo

**Configuração SMTP do Gmail**:
Para usar Gmail como provedor SMTP, configure o Spring Mail com host `smtp.gmail.com`, porta 587, username e password usando variáveis de ambiente (GMAIL_USERNAME e GMAIL_APP_PASSWORD). As propriedades SMTP devem incluir autenticação e STARTTLS habilitados, timeout de conexão de 5000ms, timeout geral de 3000ms e timeout de escrita de 5000ms. A configuração da aplicação deve definir provider como GMAIL, email de origem usando a variável GMAIL_USERNAME e email habilitado. É obrigatório usar senha de aplicativo específica, não a senha regular da conta Gmail.

### Entidade de Configuração de Email

#### EmailConfiguration Entity

**OBRIGATÓRIO**: Entidade para armazenar configurações SMTP no banco de dados.

**Entidade EmailConfiguration**:
A classe deve usar @Entity e @Table(name = "email_configurations"). Campos obrigatórios: id (Long, @Id, @GeneratedValue), provider (EmailProvider enum, @Enumerated STRING), host, port, username, password criptografado, fromEmail. Campos opcionais: fromName, description. Campos booleanos com padrões: useTls (true), useAuthentication (true), isDefault (false), isActive (true). Campos de timeout: connectionTimeout (5000), timeout (3000), writeTimeout (5000). Timestamps automáticos: createdAt (@CreationTimestamp), updatedAt (@UpdateTimestamp).

#### EmailProvider Enum

**OBRIGATÓRIO**: Enum para definir provedores suportados.

**Enum EmailProvider**:
Enum com provedores de email suportados: MAILTRAP ("Mailtrap", "smtp.mailtrap.io", porta 587), GMAIL ("Gmail", "smtp.gmail.com", porta 587), OUTLOOK ("Outlook", "smtp-mail.outlook.com", porta 587), YAHOO ("Yahoo", "smtp.mail.yahoo.com", porta 587), CUSTOM ("Personalizado", host vazio, porta 587). Cada enum possui campos: displayName, defaultHost, defaultPort, defaultUseTls (true), defaultUseAuth (true). Construtor recebe estes parâmetros e métodos getters para acessá-los.

### Serviços de Configuração

#### EmailConfigurationService

**OBRIGATÓRIO**: Serviço para gerenciar configurações SMTP.

**Classe EmailConfigurationService**:
Serviço anotado com @Service e @Transactional, estende BaseService. Dependências: EmailConfigurationRepository, PasswordEncoder, JavaMailSender. Métodos principais: findAll() busca todas configurações ordenadas por isDefault e createdAt, findById() busca por ID com tratamento de EntityNotFoundException, create() cria nova configuração validando dados e criptografando senha, update() atualiza campos específicos da configuração. Todos métodos incluem logs detalhados (logDebug, logInfo, logError) e tratamento de exceções com ServiceException. Conversão para DTO através de convertToDto().
            
            EmailConfiguration updated = repository.save(config);
            logInfo("Configuração atualizada com sucesso. ID: {}", id);
            
            return convertToDto(updated);
            
        } catch (Exception e) {
            logError("Erro ao atualizar configuração ID {}: {}", id, e.getMessage(), e);
            throw new ServiceException("Falha ao atualizar configuração", e);
        }
    }
    
    public void delete(Long id) {
        logDebug("Excluindo configuração de email ID: {}", id);
        
        try {
            EmailConfiguration config = repository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Configuração não encontrada"));
            
            if (config.getIsDefault()) {
                throw new BusinessException("Não é possível excluir a configuração padrão");
            }
            
            repository.delete(config);
            logInfo("Configuração excluída com sucesso. ID: {}", id);
            
        } catch (Exception e) {
            logError("Erro ao excluir configuração ID {}: {}", id, e.getMessage(), e);
            throw new ServiceException("Falha ao excluir configuração", e);
        }
    }
    
    public EmailConfigurationDto setAsDefault(Long id) {
        logDebug("Definindo configuração como padrão. ID: {}", id);
        
        try {
            // Remover padrão atual
            repository.clearDefaultConfiguration();
            
            // Definir nova padrão
            EmailConfiguration config = repository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Configuração não encontrada"));
            
            config.setIsDefault(true);
            config.setIsActive(true);
            
            EmailConfiguration updated = repository.save(config);
            logInfo("Configuração definida como padrão. ID: {}, Provider: {}", 
                   id, updated.getProvider());
            
            return convertToDto(updated);
            
        } catch (Exception e) {
            logError("Erro ao definir configuração como padrão ID {}: {}", id, e.getMessage(), e);
            throw new ServiceException("Falha ao definir configuração padrão", e);
        }
    }
    
    public void toggleStatus(Long id) {
        logDebug("Alternando status da configuração ID: {}", id);
        
        try {
            EmailConfiguration config = repository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Configuração não encontrada"));
            
            if (config.getIsDefault() && config.getIsActive()) {
                throw new BusinessException("Não é possível desativar a configuração padrão");
            }
            
            config.setIsActive(!config.getIsActive());
            repository.save(config);
            
            logInfo("Status da configuração alternado. ID: {}, Ativo: {}", 
                   id, config.getIsActive());
            
        } catch (Exception e) {
            logError("Erro ao alternar status da configuração ID {}: {}", id, e.getMessage(), e);
            throw new ServiceException("Falha ao alternar status", e);
        }
    }
    
    public TestConnectionResponse testConnection(Long id) {
        logDebug("Testando conexão da configuração ID: {}", id);
        
        try {
            EmailConfiguration config = repository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Configuração não encontrada"));
            
            // Configurar JavaMailSender temporário
            JavaMailSenderImpl testSender = createMailSender(config);
            
            // Testar conexão
            testSender.testConnection();
            
            logInfo("Teste de conexão bem-sucedido. ID: {}, Provider: {}", 
                   id, config.getProvider());
            
            return new TestConnectionResponse(true, "Conexão estabelecida com sucesso");
            
        } catch (Exception e) {
            logWarn("Falha no teste de conexão ID {}: {}", id, e.getMessage());
            return new TestConnectionResponse(false, "Falha na conexão: " + e.getMessage());
        }
    }
    
    public EmailConfigurationDto getDefaultConfiguration() {
        logDebug("Buscando configuração padrão de email");
        
        try {
            Optional<EmailConfiguration> defaultConfig = repository.findByIsDefaultTrueAndIsActiveTrue();
            
            if (defaultConfig.isPresent()) {
                logDebug("Configuração padrão encontrada: {}", defaultConfig.get().getProvider());
                return convertToDto(defaultConfig.get());
            }
            
            // Se não houver padrão, criar Mailtrap como padrão
            logWarn("Nenhuma configuração padrão encontrada. Criando configuração Mailtrap padrão");
            return createDefaultMailtrapConfiguration();
            
        } catch (Exception e) {
            logError("Erro ao buscar configuração padrão: {}", e.getMessage(), e);
            throw new ServiceException("Falha ao buscar configuração padrão", e);
        }
    }
    
    private EmailConfigurationDto createDefaultMailtrapConfiguration() {
        EmailConfiguration config = new EmailConfiguration();
        config.setProvider(EmailProvider.MAILTRAP);
        config.setHost("smtp.mailtrap.io");
        config.setPort(587);
        config.setUsername("default_user");
        config.setPassword(passwordEncoder.encode("default_pass"));
        config.setFromEmail("noreply@sistema.com");
        config.setFromName("Sistema");
        config.setUseTls(true);
        config.setUseAuthentication(true);
        config.setConnectionTimeout(5000);
        config.setTimeout(3000);
        config.setWriteTimeout(5000);
        config.setIsDefault(true);
        config.setIsActive(true);
        config.setDescription("Configuração padrão Mailtrap para desenvolvimento");
        
        EmailConfiguration saved = repository.save(config);
        logInfo("Configuração padrão Mailtrap criada. ID: {}", saved.getId());
        
        return convertToDto(saved);
    }
    
    private JavaMailSenderImpl createMailSender(EmailConfiguration config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.getHost());
        mailSender.setPort(config.getPort());
        mailSender.setUsername(config.getUsername());
        mailSender.setPassword(config.getPassword()); // Já descriptografado
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", config.getUseAuthentication());
        props.put("mail.smtp.starttls.enable", config.getUseTls());
        props.put("mail.smtp.connectiontimeout", config.getConnectionTimeout());
        props.put("mail.smtp.timeout", config.getTimeout());
        props.put("mail.smtp.writetimeout", config.getWriteTimeout());
        
        return mailSender;
    }
    
    private void validateEmailConfiguration(CreateEmailConfigurationRequest request) {
        validateNotNull(request, "Request não pode ser nulo");
        validateNotNull(request.getProvider(), "Provider é obrigatório");
        validateNotBlank(request.getHost(), "Host é obrigatório");
        validateNotNull(request.getPort(), "Porta é obrigatória");
        validateNotBlank(request.getUsername(), "Username é obrigatório");
        validateNotBlank(request.getPassword(), "Password é obrigatória");
        validateNotBlank(request.getFromEmail(), "Email de origem é obrigatório");
        
        if (!ValidationUtils.isValidEmail(request.getFromEmail())) {
            throw new ValidationException("Email de origem inválido");
        }
        
        if (request.getPort() < 1 || request.getPort() > 65535) {
            throw new ValidationException("Porta deve estar entre 1 e 65535");
        }
    }
    
    private EmailConfigurationDto convertToDto(EmailConfiguration config) {
        EmailConfigurationDto dto = new EmailConfigurationDto();
        dto.setId(config.getId());
        dto.setProvider(config.getProvider());
        dto.setHost(config.getHost());
        dto.setPort(config.getPort());
        dto.setUsername(config.getUsername());
        // Não retornar senha por segurança
        dto.setFromEmail(config.getFromEmail());
        dto.setFromName(config.getFromName());
        dto.setUseTls(config.getUseTls());
        dto.setUseAuthentication(config.getUseAuthentication());
        dto.setConnectionTimeout(config.getConnectionTimeout());
        dto.setTimeout(config.getTimeout());
        dto.setWriteTimeout(config.getWriteTimeout());
        dto.setIsDefault(config.getIsDefault());
        dto.setIsActive(config.getIsActive());
        dto.setDescription(config.getDescription());
        dto.setCreatedAt(config.getCreatedAt());
        dto.setUpdatedAt(config.getUpdatedAt());
        return dto;
    }
}
```

### Controlador Administrativo

#### AdminEmailPanelController

**OBRIGATÓRIO**: Controlador para painel administrativo de configuração de email.

**Classe AdminEmailPanelController**:
Controlador REST anotado com @RestController, @RequestMapping("/api/admin/email") e @PreAuthorize("hasRole('ADMIN')"), estende BaseService. Possui dependência EmailConfigurationService injetada. Endpoints implementados: renderEmailPanel() GET "/panel" retorna template "email-admin-panel"; getAllConfigurations() GET "/configurations" lista todas configurações com tratamento de exceções; getConfiguration() GET "/configurations/{id}" busca configuração por ID com tratamento de EntityNotFoundException; createConfiguration() POST "/configurations" cria nova configuração com validação @Valid; updateConfiguration() PUT "/configurations/{id}" atualiza configuração existente; deleteConfiguration() DELETE "/configurations/{id}" exclui configuração com validação de regras de negócio; setAsDefault() PUT "/configurations/{id}/set-default" define configuração como padrão; toggleStatus() PUT "/configurations/{id}/toggle-status" alterna status ativo/inativo. Todos métodos incluem logging detalhado e tratamento de exceções com respostas ApiResponse padronizadas.
        logInfo("Alternando status da configuração ID: {}", id);
        
        try {
            emailConfigurationService.toggleStatus(id);
            
            ApiResponse<Void> response = 
                ApiResponse.success(null, "Status da configuração alternado");
            
            return ResponseEntity.ok(response);
            
        } catch (EntityNotFoundException e) {
            logWarn("Configuração não encontrada para alternar status. ID: {}", id);
            ApiResponse<Void> errorResponse = 
                ApiResponse.error("Configuração não encontrada");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (BusinessException e) {
            logWarn("Erro de negócio ao alternar status ID {}: {}", id, e.getMessage());
            ApiResponse<Void> errorResponse = 
                ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logError("Erro ao alternar status da configuração ID {}: {}", id, e.getMessage(), e);
            ApiResponse<Void> errorResponse = 
                ApiResponse.error("Erro ao alternar status");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @PostMapping("/configurations/{id}/test-connection")
    public ResponseEntity<ApiResponse<TestConnectionResponse>> testConnection(@PathVariable Long id) {
        logInfo("Testando conexão da configuração ID: {}", id);
        
        try {
            TestConnectionResponse testResult = emailConfigurationService.testConnection(id);
            
            if (testResult.isSuccess()) {
                ApiResponse<TestConnectionResponse> response = 
                    ApiResponse.success(testResult, "Teste de conexão realizado");
                return ResponseEntity.ok(response);
            } else {
                ApiResponse<TestConnectionResponse> response = 
                    ApiResponse.success(testResult, "Falha no teste de conexão");
                return ResponseEntity.ok(response);
            }
            
        } catch (EntityNotFoundException e) {
            logWarn("Configuração não encontrada para teste. ID: {}", id);
            ApiResponse<TestConnectionResponse> errorResponse = 
                ApiResponse.error("Configuração não encontrada");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (Exception e) {
            logError("Erro ao testar conexão ID {}: {}", id, e.getMessage(), e);
            ApiResponse<TestConnectionResponse> errorResponse = 
                ApiResponse.error("Erro ao testar conexão");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @PostMapping("/send-test-email")
    public ResponseEntity<ApiResponse<Void>> sendTestEmail(
            @RequestBody @Valid SendTestEmailRequest request) {
        
        logInfo("Enviando email de teste para: {}", request.getToEmail());
        
        try {
            // Implementar envio de email de teste usando configuração específica
            // emailService.sendTestEmail(request);
            
            ApiResponse<Void> response = 
                ApiResponse.success(null, "Email de teste enviado com sucesso");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logError("Erro ao enviar email de teste: {}", e.getMessage(), e);
            ApiResponse<Void> errorResponse = 
                ApiResponse.error("Erro ao enviar email de teste");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<List<EmailProviderInfo>>> getAvailableProviders() {
        logInfo("Listando provedores de email disponíveis");
        
        try {
            List<EmailProviderInfo> providers = Arrays.stream(EmailProvider.values())
                    .map(provider -> new EmailProviderInfo(
                            provider.name(),
                            provider.getDisplayName(),
                            provider.getDefaultHost(),
                            provider.getDefaultPort(),
                            provider.getDefaultUseTls(),
                            provider.getDefaultUseAuth()
                    ))
                    .collect(Collectors.toList());
            
            ApiResponse<List<EmailProviderInfo>> response = 
                ApiResponse.success(providers, "Provedores listados com sucesso");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logError("Erro ao listar provedores: {}", e.getMessage(), e);
            ApiResponse<List<EmailProviderInfo>> errorResponse = 
                ApiResponse.error("Erro ao listar provedores");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/current-status")
    public ResponseEntity<ApiResponse<EmailStatusResponse>> getCurrentStatus() {
        logInfo("Verificando status atual do sistema de email");
        
        try {
            EmailConfigurationDto defaultConfig = emailConfigurationService.getDefaultConfiguration();
            
            EmailStatusResponse status = new EmailStatusResponse();
            status.setCurrentProvider(defaultConfig.getProvider());
            status.setIsActive(defaultConfig.getIsActive());
            status.setFromEmail(defaultConfig.getFromEmail());
            status.setHost(defaultConfig.getHost());
            status.setPort(defaultConfig.getPort());
            
            ApiResponse<EmailStatusResponse> response = 
                ApiResponse.success(status, "Status obtido com sucesso");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logError("Erro ao obter status do email: {}", e.getMessage(), e);
            ApiResponse<EmailStatusResponse> errorResponse = 
                ApiResponse.error("Erro ao obter status");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
```

### Configurações de Segurança

#### Regras de Acesso

**OBRIGATÓRIO**: Apenas administradores podem gerenciar configurações SMTP.

1. **Autenticação**: Usuário deve estar autenticado
2. **Autorização**: Apenas role ADMIN pode acessar endpoints
3. **Validação**: Todas as entradas devem ser validadas
4. **Criptografia**: Senhas devem ser criptografadas no banco
5. **Logs**: Todas as operações devem ser registradas

#### Validações de Entrada

**OBRIGATÓRIO**: Validar todos os dados de configuração SMTP.

**Classe CreateEmailConfigurationRequest**:
DTO de validação anotado com @Valid. Campos obrigatórios com validações: provider (@NotNull), host (@NotBlank, @Size max 255), port (@NotNull, @Min 1, @Max 65535), username (@NotBlank, @Size max 255), password (@NotBlank, @Size min 8 max 255), fromEmail (@NotBlank, @Email, @Size max 255). Campos opcionais: fromName (@Size max 255), description (@Size max 500). Campos com padrões: useTls (true), useAuthentication (true), connectionTimeout (5000), timeout (3000), writeTimeout (5000). Inclui getters e setters.

### Configurações Específicas do Gmail

#### Senha de App Gmail

**OBRIGATÓRIO**: Usar senha específica de aplicativo para Gmail.

**Senha fornecida**: `fgsu vamy tghu ykiq`

#### Configuração Gmail Padrão

```yaml
# Configuração específica para Gmail
gmail:
  smtp:
    host: smtp.gmail.com
    port: 587
    username: sistema.email@gmail.com
    app-password: fgsu vamy tghu ykiq
    from-email: sistema.email@gmail.com
    from-name: Sistema
    use-tls: true
    use-auth: true
    connection-timeout: 5000
    timeout: 3000
    write-timeout: 5000
```

#### Configuração Automática Gmail

**OBRIGATÓRIO**: Método para criar configuração Gmail automaticamente.

**Método createGmailConfiguration()**:
Método público que retorna EmailConfigurationDto. Registra log informativo sobre criação de configuração Gmail. Cria instância de CreateEmailConfigurationRequest e define: provider como GMAIL, host "smtp.gmail.com", porta 587, username e fromEmail "sistema.email@gmail.com", senha de app "fgsu vamy tghu ykiq", fromName "Sistema", useTls e useAuthentication como true, timeouts de conexão (5000ms), leitura (3000ms) e escrita (5000ms), descrição "Configuração Gmail com senha de aplicativo". Retorna resultado do método create() com o request configurado.

### Interface do Painel Administrativo

#### Funcionalidades do Painel

**OBRIGATÓRIO**: Interface web para gerenciar configurações SMTP.

1. **Listagem de Configurações**: Tabela com todas as configurações
2. **Criação de Configuração**: Formulário para nova configuração
3. **Edição de Configuração**: Formulário para editar configuração existente
4. **Teste de Conexão**: Botão para testar conectividade SMTP
5. **Definir como Padrão**: Botão para definir configuração padrão
6. **Ativar/Desativar**: Toggle para ativar/desativar configuração
7. **Exclusão**: Botão para excluir configuração (exceto padrão)
8. **Envio de Teste**: Formulário para enviar email de teste
9. **Status Atual**: Indicador da configuração ativa
10. **Configuração Rápida**: Botões para Gmail e Mailtrap

#### Elementos da Interface

A interface de configuração de email deve conter um painel principal com título "Configuração de Email SMTP", seção de status atual mostrando provider (MAILTRAP), status (Ativo) e email (noreply@sistema.com), área de configuração rápida com botões para Mailtrap e Gmail, tabela de configurações existentes com colunas para Provider, Host, Email, Status, Padrão e Ações, e formulário para nova configuração.

**Formulário de Configuração de Email (email-config-form)**:
O formulário deve conter os seguintes campos organizados em grupos:

1. **Seleção de Provider**: Campo select obrigatório com opções para Mailtrap, Gmail, Outlook, Yahoo e Personalizado
2. **Host SMTP**: Campo de texto obrigatório para endereço do servidor SMTP
3. **Porta**: Campo numérico obrigatório com validação de intervalo (1-65535)
4. **Username**: Campo de texto obrigatório para nome de usuário de autenticação
5. **Password**: Campo de senha obrigatório para autenticação SMTP
6. **Email de Origem**: Campo de email obrigatório para endereço remetente
7. **Nome de Origem**: Campo de texto opcional para nome do remetente
8. **Usar TLS**: Checkbox marcado por padrão para habilitar criptografia TLS
9. **Usar Autenticação**: Checkbox marcado por padrão para habilitar autenticação SMTP
10. **Descrição**: Campo de texto multilinha opcional (3 linhas) para descrição da configuração

O formulário deve incluir dois botões de ação: "Criar Configuração" (botão primário para submissão) e "Testar Conexão" (botão secundário que executa função testConnection()).

**Seção de Teste de Email**:
Área separada com título "Enviar Email de Teste" contendo formulário específico (test-email-form) com:

1. **Email de Destino**: Campo de email obrigatório para endereço do destinatário do teste
2. **Configuração**: Campo select obrigatório preenchido dinamicamente via JavaScript com as configurações disponíveis
3. **Botão Enviar Teste**: Botão primário para executar o envio do email de teste

Todos os campos obrigatórios devem ter validação client-side e os formulários devem seguir o padrão de estilização com classes CSS apropriadas (form-group, btn btn-primary, btn btn-secondary).
```

### Regras de Implementação

#### 1. Configuração Padrão

**REGRA**: Mailtrap deve ser a configuração padrão inicial.

- Ao inicializar o sistema, criar configuração Mailtrap como padrão
- Sempre deve existir uma configuração padrão ativa
- Não é possível excluir a configuração padrão
- Não é possível desativar a configuração padrão

#### 2. Alternância de Provedores

**REGRA**: Administrador pode alternar entre Mailtrap e Gmail facilmente.

- Botões de configuração rápida para Mailtrap e Gmail
- Configuração Gmail deve usar a senha de app fornecida
- Alternância deve ser imediata e refletir no sistema
- Logs devem registrar todas as alternâncias

#### 3. Validação de Configurações

**REGRA**: Todas as configurações devem ser testadas antes de serem ativadas.

- Teste de conexão obrigatório antes de definir como padrão
- Validação de formato de email
- Validação de portas e hosts
- Criptografia de senhas no banco de dados

#### 4. Segurança

**REGRA**: Acesso restrito e dados protegidos.

- Apenas administradores podem acessar o painel
- Senhas criptografadas no banco
- Logs de todas as operações
- Validação de entrada rigorosa
- Rate limiting em testes de conexão

#### 5. Interface Responsiva

**REGRA**: Painel deve ser responsivo e intuitivo.

- Design responsivo para desktop e mobile
- Feedback visual para operações
- Confirmações para ações críticas
- Indicadores de status claros
- Mensagens de erro informativas

Esta seção estabelece um sistema completo de configuração SMTP com suporte a múltiplos provedores, painel administrativo e configuração dinâmica, mantendo Mailtrap como padrão e Gmail como alternativa principal.

## Regras de Desenvolvimento (Development Environment)

### Configuração de Ambiente de Desenvolvimento

**REGRA FUNDAMENTAL**: O ambiente de desenvolvimento deve priorizar debugging, logs detalhados e facilidade de desenvolvimento sobre performance.

#### Configurações Obrigatórias para Desenvolvimento

##### 1. Configuração de Logging Ultra Verboso

**application-dev.yml**:
```yaml
# Configuração de Desenvolvimento
spring:
  profiles:
    active: dev
  
  # Configuração de Logging Ultra Verboso
  logging:
    level:
      root: DEBUG
      com.sistema: TRACE
      org.springframework: DEBUG
      org.springframework.web: TRACE
      org.springframework.security: TRACE
      org.springframework.data.jpa: DEBUG
      org.hibernate: DEBUG
      org.hibernate.SQL: DEBUG
      org.hibernate.type.descriptor.sql.BasicBinder: TRACE
      org.springframework.mail: DEBUG
      redis.clients.lettuce: DEBUG
      io.lettuce.core: DEBUG
    pattern:
      console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%logger{50}:%line] - %msg%n"
      file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%logger{50}:%line] - %msg%n"
    file:
      name: logs/sistema-dev.log
      max-size: 100MB
      max-history: 30

  # Configuração H2 para Desenvolvimento
  datasource:
    url: jdbc:h2:mem:devdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driverClassName: org.h2.Driver
    username: sa
    password: 
    
  h2:
    console:
      enabled: true
      path: /h2-console
      settings:
        trace: true
        web-allow-others: true
        
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
        generate_statistics: true
        
  # Redis para Desenvolvimento
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0

# Configuração de Debug
debug: true
trace: true

# Configurações da Aplicação para Desenvolvimento
app:
  debug:
    enabled: true
    ultra-verbose: true
    log-all-requests: true
    log-all-responses: true
    log-sql-parameters: true
    log-redis-operations: true
    log-email-content: true
    log-jwt-operations: true
    log-security-events: true
    
  development:
    auto-reload: true
    disable-cache: true
    show-error-details: true
    enable-dev-tools: true
```

##### 2. Configuração de Email com Mailtrap

**REGRA OBRIGATÓRIA**: Em desenvolvimento, usar exclusivamente Mailtrap para captura de emails.

```yaml
# Configuração de Email para Desenvolvimento (Mailtrap)
app:
  email:
    provider: MAILTRAP
    enabled: true
    debug: true
    log-content: true
    
    mailtrap:
      host: live.smtp.mailtrap.io
      port: 587
      username: api
      password: ${MAILTRAP_PASSWORD:your-mailtrap-password}
      auth: true
      starttls:
        enable: true
      from:
        email: noreply@sistema-dev.com
        name: Sistema Dev
        
    # Configurações de Debug para Email
    smtp:
      debug: true
      log-session: true
      log-transport: true
      connection-timeout: 10000
      timeout: 10000
```

##### 3. Configurações de Debug Ultra Verboso

**REGRA**: Todos os componentes devem ter logging extremamente detalhado em desenvolvimento.

**Classe ExampleService com Logging Ultra Verboso**:
Serviço anotado com @Service e @Slf4j, estende BaseService. Implementa métodos de logging detalhado: logMethodEntry() registra entrada com parâmetros, thread e timestamp quando ultra verboso habilitado; logMethodExit() registra saída com resultado e duração; logException() registra erros com classe, mensagem e stack trace, incluindo variáveis locais e estado do objeto em modo desenvolvimento. Usa log.trace() para entrada/saída e log.error() para exceções.

##### 4. Configurações de Segurança Relaxadas para Desenvolvimento

```yaml
# Configurações de Segurança para Desenvolvimento
app:
  security:
    jwt:
      access-token-expiration: 3600000  # 1 hora (mais longo para desenvolvimento)
      refresh-token-expiration: 604800000  # 7 dias
      
    cors:
      allowed-origins: 
        - "http://localhost:3000"
        - "http://localhost:8080"
        - "http://127.0.0.1:3000"
        - "http://127.0.0.1:8080"
      allowed-methods: "*"
      allowed-headers: "*"
      allow-credentials: true
      
    rate-limiting:
      enabled: false  # Desabilitado em desenvolvimento
      
    captcha:
      required-after-attempts: 10  # Mais permissivo em desenvolvimento
```

##### 5. Ferramentas de Desenvolvimento

**REGRA**: Habilitar todas as ferramentas de desenvolvimento e debugging.

```yaml
# Ferramentas de Desenvolvimento
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
    env:
      show-values: always
      
spring:
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true
    add-properties: true
```

## Regras de Produção (Production Environment)

### Configuração de Ambiente de Produção

**REGRA FUNDAMENTAL**: O ambiente de produção deve priorizar segurança, performance e estabilidade sobre facilidade de debugging.

#### Configurações Obrigatórias para Produção

##### 1. Configuração de Logging Otimizado

**application-prod.yml**:
```yaml
# Configuração de Produção
spring:
  profiles:
    active: prod
    
  # Configuração de Logging Otimizado para Produção
  logging:
    level:
      root: WARN
      com.sistema: INFO
      org.springframework: WARN
      org.springframework.security: WARN
      org.hibernate: WARN
      org.hibernate.SQL: WARN
    pattern:
      console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
      file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file:
      name: logs/sistema-prod.log
      max-size: 50MB
      max-history: 90
    logback:
      rollingpolicy:
        max-file-size: 50MB
        total-size-cap: 1GB

  # Configuração H2 para Produção (ou substituir por PostgreSQL/MySQL)
  datasource:
    url: jdbc:h2:file:./data/proddb;AUTO_SERVER=TRUE
    driverClassName: org.h2.Driver
    username: ${DB_USERNAME:sa}
    password: ${DB_PASSWORD:}
    
  h2:
    console:
      enabled: false  # DESABILITADO em produção
        
  jpa:
    hibernate:
      ddl-auto: validate  # NUNCA usar create-drop em produção
    show-sql: false
    properties:
      hibernate:
        format_sql: false
        generate_statistics: false
        
  # Redis para Produção
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5

# Configurações de Debug DESABILITADAS
debug: false
trace: false

# Configurações da Aplicação para Produção
app:
  debug:
    enabled: false
    ultra-verbose: false
    log-all-requests: false
    log-all-responses: false
    log-sql-parameters: false
    log-redis-operations: false
    log-email-content: false
    log-jwt-operations: false
    log-security-events: true  # Apenas eventos de segurança
    
  production:
    optimize-performance: true
    enable-cache: true
    hide-error-details: true
    disable-dev-tools: true
```

##### 2. Configuração de Email com Gmail

**REGRA OBRIGATÓRIA**: Em produção, usar Gmail com senha de aplicativo para envio de emails.

```yaml
# Configuração de Email para Produção (Gmail)
app:
  email:
    provider: GMAIL
    enabled: true
    debug: false
    log-content: false
    
    gmail:
      host: smtp.gmail.com
      port: 587
      username: ${GMAIL_USERNAME:your-email@gmail.com}
      password: ${GMAIL_APP_PASSWORD:fgsu-vamy-tghu-ykiq}  # Senha de aplicativo
      auth: true
      starttls:
        enable: true
      ssl:
        trust: smtp.gmail.com
      from:
        email: ${GMAIL_FROM_EMAIL:noreply@yourdomain.com}
        name: ${GMAIL_FROM_NAME:Sistema Produção}
        
    # Configurações Otimizadas para Produção
    smtp:
      debug: false
      log-session: false
      log-transport: false
      connection-timeout: 5000
      timeout: 5000
      connection-pool-size: 10
      max-retry-attempts: 3
```

##### 3. Configurações de Segurança Rigorosas

**REGRA**: Máxima segurança e validação em produção.

```yaml
# Configurações de Segurança para Produção
app:
  security:
    jwt:
      access-token-expiration: 900000   # 15 minutos (mais curto para segurança)
      refresh-token-expiration: 86400000  # 24 horas
      
    cors:
      allowed-origins: 
        - "https://yourdomain.com"
        - "https://www.yourdomain.com"
      allowed-methods: 
        - "GET"
        - "POST" 
        - "PUT"
        - "DELETE"
      allowed-headers:
        - "Authorization"
        - "Content-Type"
        - "X-Requested-With"
      allow-credentials: true
      
    rate-limiting:
      enabled: true
      requests-per-minute: 60
      requests-per-hour: 1000
      
    captcha:
      required-after-attempts: 3  # Mais rigoroso em produção
      
    headers:
      frame-options: DENY
      content-type-options: nosniff
      xss-protection: "1; mode=block"
      referrer-policy: strict-origin-when-cross-origin
```

##### 4. Configurações de Performance

**REGRA**: Otimizações de performance obrigatórias em produção.

```yaml
# Configurações de Performance para Produção
server:
  port: 8080
  compression:
    enabled: true
    mime-types: 
      - text/html
      - text/xml
      - text/plain
      - text/css
      - text/javascript
      - application/javascript
      - application/json
      - application/xml
    min-response-size: 1024
  http2:
    enabled: true
  tomcat:
    max-threads: 200
    min-spare-threads: 10
    max-connections: 8192
    accept-count: 100
    connection-timeout: 20000

spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
        batch_versioned_data: true
        
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10 minutos
      
  data:
    redis:
      lettuce:
        pool:
          max-active: 50
          max-idle: 20
          min-idle: 10
```

##### 5. Monitoramento e Observabilidade

**REGRA**: Monitoramento completo em produção.

```yaml
# Configurações de Monitoramento para Produção
management:
  endpoints:
    web:
      exposure:
        include: 
          - health
          - info
          - metrics
          - prometheus
  endpoint:
    health:
      show-details: when-authorized
      show-components: when-authorized
    metrics:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
        
# Configurações de Auditoria
app:
  audit:
    enabled: true
    log-all-access: true
    log-failed-attempts: true
    log-admin-actions: true
    retention-days: 90
```

### Regras de Implementação por Ambiente

#### Desenvolvimento (DEV)
1. **Logging**: Ultra verboso com TRACE level
2. **Email**: Mailtrap exclusivamente
3. **Debug**: Habilitado em todos os componentes
4. **Segurança**: Relaxada para facilitar desenvolvimento
5. **Performance**: Não é prioridade
6. **Cache**: Desabilitado ou mínimo
7. **Ferramentas**: DevTools, H2 Console, Actuator completo

#### Produção (PROD)
1. **Logging**: Otimizado com WARN/INFO level
2. **Email**: Gmail com senha de aplicativo
3. **Debug**: Desabilitado completamente
4. **Segurança**: Máxima com rate limiting
5. **Performance**: Otimizada com cache e compressão
6. **Cache**: Redis completo
7. **Ferramentas**: Apenas endpoints essenciais de monitoramento

### Configurações de Variáveis de Ambiente

#### Desenvolvimento
```bash
# Variáveis de Ambiente para Desenvolvimento
SPRING_PROFILES_ACTIVE=dev
MAILTRAP_PASSWORD=your-mailtrap-password
LOG_LEVEL=TRACE
DEBUG_ENABLED=true
```

#### Produção
```bash
# Variáveis de Ambiente para Produção
SPRING_PROFILES_ACTIVE=prod
GMAIL_USERNAME=your-email@gmail.com
GMAIL_APP_PASSWORD=fgsu-vamy-tghu-ykiq
GMAIL_FROM_EMAIL=noreply@yourdomain.com
GMAIL_FROM_NAME=Sistema Produção
DB_USERNAME=prod_user
DB_PASSWORD=secure_password
REDIS_HOST=redis-server
REDIS_PORT=6379
REDIS_PASSWORD=redis_password
LOG_LEVEL=WARN
DEBUG_ENABLED=false
```

Esta seção estabelece configurações específicas e otimizadas para cada ambiente, garantindo facilidade de desenvolvimento e segurança/performance em produção.

## Regras de Desenvolvimento em Linguagem Natural

### Princípios Fundamentais do Projeto

#### Clareza e Simplicidade
- Toda funcionalidade deve ser implementada de forma clara e compreensível
- Evitar complexidade desnecessária em favor da manutenibilidade
- Priorizar soluções simples que resolvam o problema de forma efetiva
- Documentar decisões técnicas importantes para facilitar futuras manutenções

#### Consistência Arquitetural
- Manter padrões consistentes em toda a aplicação
- Seguir as convenções estabelecidas para nomenclatura de classes, métodos e variáveis
- Utilizar as classes base abstratas para evitar duplicação de código
- Implementar interfaces padronizadas para garantir contratos consistentes

#### Segurança por Design
- Toda entrada de dados deve ser validada antes do processamento
- Implementar autenticação e autorização em todos os endpoints sensíveis
- Registrar eventos de segurança para auditoria e monitoramento
- Aplicar princípios de menor privilégio em todas as operações

### Regras de Implementação

#### Estrutura de Código
- Organizar código em pacotes lógicos que reflitam a funcionalidade
- Separar responsabilidades entre controladores, serviços e repositórios
- Utilizar injeção de dependência para facilitar testes e manutenção
- Manter classes focadas em uma única responsabilidade

#### Tratamento de Erros
- Capturar e tratar exceções de forma apropriada em cada camada
- Fornecer mensagens de erro claras e úteis para o usuário final
- Registrar erros com contexto suficiente para facilitar debugging
- Implementar fallbacks graceful quando possível

#### Validação de Dados
- Validar todos os dados de entrada no nível de controlador
- Implementar validações de negócio no nível de serviço
- Utilizar validações padronizadas disponíveis nos utilitários
- Retornar mensagens de validação específicas e acionáveis

#### Logging e Monitoramento
- Registrar eventos importantes para rastreabilidade
- Utilizar níveis de log apropriados para cada tipo de evento
- Incluir informações contextuais relevantes nos logs
- Evitar registrar informações sensíveis como senhas ou tokens

### Regras de Qualidade

#### Testes
- Escrever testes unitários para toda lógica de negócio
- Implementar testes de integração para fluxos críticos
- Manter cobertura de testes adequada para garantir qualidade
- Utilizar dados de teste válidos e representativos

#### Performance
- Otimizar consultas de banco de dados para evitar problemas de performance
- Implementar cache adequado para dados frequentemente acessados
- Monitorar uso de recursos e identificar gargalos
- Implementar paginação para listagens que podem retornar muitos resultados

#### Manutenibilidade
- Escrever código autodocumentado com nomes descritivos
- Adicionar comentários apenas quando necessário para explicar lógica complexa
- Refatorar código regularmente para melhorar qualidade
- Manter dependências atualizadas e seguras

### Regras de Colaboração

#### Versionamento
- Fazer commits pequenos e focados em uma única funcionalidade
- Escrever mensagens de commit claras e descritivas
- Utilizar branches para desenvolvimento de novas funcionalidades
- Revisar código antes de integrar mudanças

#### Documentação
- Manter documentação atualizada para APIs e funcionalidades
- Documentar decisões arquiteturais importantes
- Criar guias de instalação e configuração claros
- Documentar processos de deploy e manutenção

#### Comunicação
- Discutir mudanças significativas antes da implementação
- Compartilhar conhecimento sobre funcionalidades implementadas
- Reportar problemas e bugs de forma clara e detalhada
- Colaborar na resolução de problemas complexos

### Regras de Segurança

#### Autenticação e Autorização
- Implementar autenticação forte para todos os usuários
- Verificar autorização antes de executar operações sensíveis
- Utilizar tokens seguros com expiração apropriada
- Implementar logout seguro com invalidação de tokens

#### Proteção de Dados
- Criptografar dados sensíveis em trânsito e em repouso
- Implementar mascaramento de dados sensíveis em logs
- Aplicar princípios de minimização de dados
- Implementar backup e recuperação de dados críticos

#### Controle de Acesso
- Implementar controle de tentativas para prevenir ataques de força bruta
- Utilizar captcha quando apropriado para proteção adicional
- Monitorar atividades suspeitas e implementar alertas
- Aplicar rate limiting para prevenir abuso de APIs

### Regras de Usabilidade

#### Interface do Usuário
- Projetar interfaces intuitivas e fáceis de usar
- Fornecer feedback claro para ações do usuário
- Implementar validação em tempo real quando apropriado
- Garantir acessibilidade para usuários com necessidades especiais

#### Experiência do Usuário
- Minimizar número de passos necessários para completar tarefas
- Fornecer mensagens de ajuda e orientação quando necessário
- Implementar funcionalidades de busca e filtro eficientes
- Garantir tempos de resposta aceitáveis para todas as operações

#### Responsividade
- Garantir que a aplicação funcione bem em diferentes dispositivos
- Implementar design responsivo para interfaces web
- Otimizar para diferentes tamanhos de tela e resoluções
- Testar funcionalidade em diferentes navegadores

### Regras de Manutenção

#### Monitoramento
- Implementar métricas de saúde da aplicação
- Monitorar uso de recursos como CPU, memória e disco
- Configurar alertas para problemas críticos
- Manter logs de auditoria para rastreabilidade

#### Backup e Recuperação
- Implementar backup regular de dados críticos
- Testar procedimentos de recuperação regularmente
- Documentar processos de backup e recuperação
- Manter backups em locais seguros e acessíveis

#### Atualizações
- Manter dependências atualizadas com versões seguras
- Testar atualizações em ambiente de desenvolvimento antes da produção
- Documentar mudanças e impactos de atualizações
- Implementar rollback quando necessário

### Regras de Compliance

#### Privacidade
- Implementar políticas de privacidade claras
- Obter consentimento apropriado para coleta de dados
- Permitir que usuários acessem e modifiquem seus dados
- Implementar direito ao esquecimento quando aplicável

#### Auditoria
- Manter registros de auditoria para operações críticas
- Implementar rastreabilidade de mudanças em dados sensíveis
- Documentar acessos a informações confidenciais
- Manter logs por período adequado conforme regulamentações

#### Conformidade
- Seguir padrões de segurança da indústria
- Implementar controles de acesso conforme políticas organizacionais
- Manter documentação de conformidade atualizada
- Realizar revisões regulares de segurança e compliance

Estas regras servem como diretrizes fundamentais para o desenvolvimento, manutenção e operação do sistema, garantindo qualidade, segurança e usabilidade em todas as fases do projeto.