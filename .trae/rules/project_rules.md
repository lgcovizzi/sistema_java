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
```yaml
server:
  port: 8080
```

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

### Sistema de Ativação por Email

**REGRA OBRIGATÓRIA**: Todo usuário cadastrado deve ativar sua conta através de email antes de poder fazer login.

#### REGRA CRÍTICA DE ENVIO DE EMAIL
**OBRIGATÓRIO**: O sistema DEVE enviar automaticamente um email de ativação imediatamente após o cadastro de qualquer usuário (exceto ADMIN). Esta operação é MANDATÓRIA e não pode falhar silenciosamente.

##### Requisitos de Implementação:
1. **Envio Automático**: O email de ativação deve ser enviado automaticamente no momento do cadastro
2. **Falha Crítica**: Se o envio do email falhar, o cadastro deve ser revertido (rollback da transação)
3. **Logs Obrigatórios**: Toda tentativa de envio deve ser registrada em logs
4. **Testes Unitários**: Devem existir testes que validem o envio obrigatório do email
5. **Validação de Integração**: Testes de integração devem verificar o fluxo completo

#### Fluxo de Ativação:

1. **Cadastro de Usuário**:
   - Ao se cadastrar, o usuário recebe automaticamente um email de verificação
   - O campo `emailVerified` é definido como `false` por padrão
   - Um token de verificação é gerado e armazenado no campo `verificationToken`
   - O token tem expiração definida em `verificationTokenExpiresAt`
   - **CRÍTICO**: O email DEVE ser enviado antes da confirmação do cadastro

2. **Validação no Login**:
   - **OBRIGATÓRIO**: O sistema deve verificar se `user.isEmailVerified()` é `true` antes de permitir login
   - Se o email não estiver verificado, o login deve falhar com a mensagem: "Email não verificado. Verifique sua caixa de entrada e clique no link de verificação."
   - Esta validação está implementada no método `authenticate()` do `AuthService`

3. **Endpoints de Verificação**:
   - `POST /api/auth/verify-email`: Verifica o token de ativação
   - `POST /api/auth/resend-verification`: Reenvia o email de verificação

#### Componentes Implementados:

**EmailVerificationService** (`com.sistema.service.EmailVerificationService`)
- `generateVerificationToken()`: Gera token de verificação
- `verifyEmailToken()`: Valida e ativa a conta do usuário
- `needsEmailVerification()`: Verifica se o usuário precisa verificar email

**EmailService** (`com.sistema.service.EmailService`)
- `sendVerificationEmail()`: Envia email de verificação inicial
- `resendVerificationEmail()`: Reenvia email de verificação

**SmtpService** (`com.sistema.service.SmtpService`)
- Responsável pelo envio físico dos emails
- Configurado para usar Mailtrap em desenvolvimento

#### Configurações de Email:

```yaml
app:
  email:
    enabled: true
    verification:
      url: "http://localhost:8080/verify-email"
      token:
        expiration:
          hours: 24
```

#### Templates de Email:
- `email-verification.html`: Template para email inicial de verificação
- `email-verification-resend.html`: Template para reenvio de verificação

#### Campos da Entidade User:
- `emailVerified`: Boolean indicando se o email foi verificado
- `verificationToken`: Token único para verificação
- `verificationTokenExpiresAt`: Data de expiração do token

#### Regras de Negócio:
1. Usuários ADMIN são automaticamente verificados (`emailVerified = true`)
2. Usuários comuns devem verificar o email antes do primeiro login
3. Tokens de verificação expiram em 24 horas
4. É possível reenviar o email de verificação quantas vezes necessário
5. Após verificação bem-sucedida, o token é limpo e `emailVerified` é definido como `true`

#### Testes Unitários Obrigatórios para Email de Ativação:

**REGRA CRÍTICA**: Os seguintes testes unitários são OBRIGATÓRIOS e devem sempre passar:

##### Testes de Cadastro com Email:
1. **Teste de Envio Obrigatório**: Verificar que o email de ativação é enviado automaticamente após cadastro
2. **Teste de Falha de Email**: Verificar que o cadastro falha se o envio de email falhar
3. **Teste de Rollback**: Verificar que a transação é revertida se o email não for enviado
4. **Teste de Logs**: Verificar que tentativas de envio são registradas em logs
5. **Teste de Token**: Verificar que o token de verificação é gerado corretamente
6. **Teste de Expiração**: Verificar que o token tem data de expiração definida
7. **Teste de Status**: Verificar que `emailVerified` é definido como `false` para usuários comuns
8. **Teste de Admin**: Verificar que usuários ADMIN não recebem email de ativação

##### Testes de Integração:
1. **Teste de Fluxo Completo**: Cadastro → Envio de Email → Ativação → Login
2. **Teste de Falha de SMTP**: Simular falha do servidor SMTP e verificar comportamento
3. **Teste de Template**: Verificar que o template de email é renderizado corretamente
4. **Teste de Configuração**: Verificar que as configurações de email estão corretas

##### Localização dos Testes:
- **Testes Unitários**: `src/test/java/com/sistema/service/AuthServiceTest.java`
- **Testes de Integração**: `src/test/java/com/sistema/integration/EmailActivationIntegrationTest.java`
- **Testes de Controller**: `src/test/java/com/sistema/controller/AuthControllerTest.java`

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
  ```json
  {
    "success": true,
    "maskedEmail": "j***@*****.com",
    "message": "CPF encontrado. Confirme o email para prosseguir."
  }
  ```

#### 2. Confirmação de Email
- **Endpoint**: `POST /api/auth/confirm-email`
- **Entrada**: CPF e email completo informado pelo usuário
- **Comportamento**:
  - Verificar se o email informado corresponde ao email cadastrado para o CPF
  - Validar captcha obrigatório
  - Se confirmado, gerar token de recuperação e enviar por email
  - Aplicar rate limiting para evitar spam
- **Resposta de Sucesso**:
  ```json
  {
    "success": true,
    "message": "Email confirmado. Token de recuperação enviado.",
    "email": "usuario@exemplo.com"
  }
  ```

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
```yaml
app:
  password-recovery:
    token:
      expiration:
        minutes: 30
    rate-limit:
      cpf-verification: 5  # tentativas por hora
      email-confirmation: 3  # tentativas por hora
      password-reset: 2  # tentativas por hora
```


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
```java
@Configuration
@Profile("e2e")
public class E2ETestConfiguration {
    
    @Bean
    @Primary
    public TestRestTemplate testRestTemplate() {
        return new TestRestTemplate();
    }
    
    @Bean
    public TestDataInitializer testDataInitializer() {
        return new TestDataInitializer();
    }
}
```

**application-e2e.yml**:
```yaml
spring:
  profiles:
    active: e2e
  datasource:
    url: jdbc:h2:mem:e2etestdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
  redis:
    host: localhost
    port: 6379
    database: 1  # Database diferente para testes E2E
  mail:
    host: sandbox.smtp.mailtrap.io
    port: 2525
    username: 67af468e706c8e
    password: c9c83240f6d045

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

```java
@Component
public class E2ETestHelper {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    public String authenticateAndGetToken() {
        // Criar usuário de teste
        User testUser = createTestUser();
        
        // Fazer login
        LoginRequest request = new LoginRequest(testUser.getEmail(), "senha123");
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
            "/api/auth/login", request, LoginResponse.class);
        
        return response.getBody().getAccessToken();
    }
    
    public User createTestUser() {
        User user = new User();
        user.setEmail("teste@e2e.com");
        user.setPassword(passwordEncoder.encode("senha123"));
        user.setEmailVerified(true);
        user.setEnabled(true);
        return userRepository.save(user);
    }
    
    public void cleanupTestData() {
        userRepository.deleteAll();
        // Limpar cache Redis se necessário
    }
}
```

#### TestDataBuilder

```java
public class TestDataBuilder {
    
    public static RegisterRequestBuilder aRegisterRequest() {
        return new RegisterRequestBuilder();
    }
    
    public static LoginRequestBuilder aLoginRequest() {
        return new LoginRequestBuilder();
    }
    
    public static class RegisterRequestBuilder {
        private String email = "teste@email.com";
        private String password = "senha123";
        private String name = "Usuário Teste";
        private String lastName = "E2E";
        private String cpf = CpfGenerator.generateCpfWithMask();
        
        public RegisterRequestBuilder withEmail(String email) {
            this.email = email;
            return this;
        }
        
        public RegisterRequestBuilder withPassword(String password) {
            this.password = password;
            return this;
        }
        
        public RegisterRequest build() {
            return new RegisterRequest(name, lastName, email, cpf, password);
        }
    }
}
```

### Configuração de Execução

#### Maven Configuration

**pom.xml**:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>3.0.0-M9</version>
    <executions>
        <execution>
            <id>e2e-tests</id>
            <phase>integration-test</phase>
            <goals>
                <goal>integration-test</goal>
            </goals>
            <configuration>
                <includes>
                    <include>**/*E2ETest.java</include>
                </includes>
                <systemPropertyVariables>
                    <spring.profiles.active>e2e</spring.profiles.active>
                </systemPropertyVariables>
            </configuration>
        </execution>
        <execution>
            <id>verify</id>
            <phase>verify</phase>
            <goals>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### Execução dos Testes

**Comandos para executar testes E2E**:
```bash
# Executar apenas testes E2E
mvn verify -Dtest="*E2ETest"

# Executar com perfil específico
mvn verify -Dspring.profiles.active=e2e

# Executar com logs detalhados
mvn verify -Dtest="*E2ETest" -X

# Executar em paralelo
mvn verify -Dtest="*E2ETest" -T 4
```

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

```yaml
# .github/workflows/e2e-tests.yml
name: E2E Tests
on: [push, pull_request]

jobs:
  e2e-tests:
    runs-on: ubuntu-latest
    services:
      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
      
      - name: Run E2E Tests
        run: mvn verify -Dtest="*E2ETest" -Dspring.profiles.active=e2e
      
      - name: Upload Test Reports
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: e2e-test-reports
          path: target/surefire-reports/
```

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
    // H2 Database em memória é usado automaticamente para testes
// Não é necessário container externo
    
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

#### Problemas Conuns

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

### Padrões de Implementação TDD

#### 1. Workflow de Desenvolvimento

**Sequência Obrigatória**:
1. **Escrever teste falhando** (Red)
2. **Implementar código mínimo** (Green)
3. **Refatorar mantendo testes** (Refactor)
4. **Commit apenas com testes passando**

**Exemplo Prático - Implementando UserService**:

```java
// PASSO 1: RED - Teste falhando
@Test
@DisplayName("Deve criar usuário com dados válidos")
void shouldCreateUserWithValidData() {
    // Given
    CreateUserRequest request = CreateUserRequest.builder()
        .name("João Silva")
        .email("joao@email.com")
        .password("senha123")
        .build();
    
    // When
    User user = userService.createUser(request);
    
    // Then
    assertThat(user.getId()).isNotNull();
    assertThat(user.getName()).isEqualTo("João Silva");
    assertThat(user.getEmail()).isEqualTo("joao@email.com");
    assertThat(user.getPassword()).isNotEqualTo("senha123"); // deve estar criptografada
}

// PASSO 2: GREEN - Implementação mínima
@Service
public class UserService {
    public User createUser(CreateUserRequest request) {
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
```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    @DisplayName("POST /api/users deve retornar 201 quando dados válidos")
    void shouldReturn201WhenValidUserData() throws Exception {
        // Given
        CreateUserRequest request = new CreateUserRequest("João", "joao@email.com", "senha123");
        User expectedUser = User.builder().id("123").name("João").build();
        
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(expectedUser);
        
        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("123"))
                .andExpect(jsonPath("$.name").value("João"));
    }
}
```

**Service Layer**:
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    @DisplayName("Deve lançar exceção quando email já existe")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Given
        CreateUserRequest request = new CreateUserRequest("João", "joao@email.com", "senha123");
        when(userRepository.existsByEmail("joao@email.com")).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> userService.createUser(request))
            .isInstanceOf(UserAlreadyExistsException.class)
            .hasMessage("Email já cadastrado");
    }
}
```

**Repository Layer**:
```java
@DataJpaTest
class UserRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    @DisplayName("Deve encontrar usuário por email")
    void shouldFindUserByEmail() {
        // Given
        User user = User.builder()
            .name("João")
            .email("joao@email.com")
            .password("hashedPassword")
            .build();
        entityManager.persistAndFlush(user);
        
        // When
        Optional<User> found = userRepository.findByEmail("joao@email.com");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("João");
    }
}
```

#### 3. Test Data Builders

**Padrão Builder para Testes**:
```java
public class UserTestDataBuilder {
    
    private String id = UUID.randomUUID().toString();
    private String name = "João Silva";
    private String email = "joao@email.com";
    private String password = "senha123";
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public static UserTestDataBuilder aUser() {
        return new UserTestDataBuilder();
    }
    
    public UserTestDataBuilder withId(String id) {
        this.id = id;
        return this;
    }
    
    public UserTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public UserTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public User build() {
        return User.builder()
            .id(id)
            .name(name)
            .email(email)
            .password(password)
            .createdAt(createdAt)
            .build();
    }
}

// Uso nos testes
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
```java
@Test
@DisplayName("Cenário: Usuário tenta fazer login com credenciais válidas")
void userTriesToLoginWithValidCredentials() {
    // Given: Um usuário cadastrado no sistema
    User existingUser = aUser()
        .withEmail("joao@email.com")
        .withPassword(passwordEncoder.encode("senha123"))
        .build();
    when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(existingUser));
    when(passwordEncoder.matches("senha123", existingUser.getPassword())).thenReturn(true);
    
    // When: O usuário tenta fazer login
    LoginRequest request = new LoginRequest("joao@email.com", "senha123");
    LoginResponse response = authService.login(request);
    
    // Then: O login deve ser bem-sucedido
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.getToken()).isNotBlank();
    assertThat(response.getUser().getEmail()).isEqualTo("joao@email.com");
    
    // And: O token deve ser válido
    assertThat(jwtService.isValidToken(response.getToken())).isTrue();
}
```

#### 5. Testes de Exceções e Edge Cases

**Padrão para Testes de Exceção**:
```java
@Test
@DisplayName("Deve lançar ValidationException quando dados inválidos")
void shouldThrowValidationExceptionWhenInvalidData() {
    // Given
    CreateUserRequest invalidRequest = CreateUserRequest.builder()
        .name("") // nome vazio
        .email("email-inválido") // email inválido
        .password("123") // senha muito curta
        .build();
    
    // When & Then
    assertThatThrownBy(() -> userService.createUser(invalidRequest))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("Nome é obrigatório")
        .hasMessageContaining("Email inválido")
        .hasMessageContaining("Senha deve ter pelo menos 8 caracteres");
}

@ParameterizedTest
@ValueSource(strings = {"", " ", "a", "ab"})
@DisplayName("Deve rejeitar nomes inválidos")
void shouldRejectInvalidNames(String invalidName) {
    // Given
    CreateUserRequest request = aCreateUserRequest()
        .withName(invalidName)
        .build();
    
    // When & Then
    assertThatThrownBy(() -> userService.createUser(request))
        .isInstanceOf(ValidationException.class);
}
```

#### 6. Testes de Integração com Transações

**Padrão para Testes Transacionais**:
```java
@SpringBootTest
@Transactional
@Rollback
class UserServiceIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    @DisplayName("Deve criar usuário e persistir no banco")
    void shouldCreateUserAndPersistInDatabase() {
        // Given
        CreateUserRequest request = new CreateUserRequest("João", "joao@email.com", "senha123");
        
        // When
        User createdUser = userService.createUser(request);
        
        // Then
        assertThat(createdUser.getId()).isNotNull();
        
        // And: Verificar persistência
        Optional<User> persistedUser = userRepository.findById(createdUser.getId());
        assertThat(persistedUser).isPresent();
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

## Configuração SMTP Mailtrap

### Configuração Spring Boot

#### Configuração Obrigatória de Porta

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

#### Configuração de Email via Mailtrap

Para configurar o envio de emails via Mailtrap, adicione as seguintes propriedades no `application.yml`:

```yaml
spring:
  mail:
    host: sandbox.smtp.mailtrap.io
    port: 2525
    username: 67af468e706c8e
    password: c9c83240f6d045
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          ssl:
            trust: sandbox.smtp.mailtrap.io
```

### Configuração de Desenvolvimento

Para ambiente de desenvolvimento, use as configurações do Mailtrap:
- **Host**: sandbox.smtp.mailtrap.io
- **Porta**: 2525
- **Username**: 67af468e706c8e
- **Password**: c9c83240f6d045
- **TLS**: Habilitado
- **Autenticação**: Obrigatória

### Uso no Código

```java
@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom("noreply@sistema.com");
        
        mailSender.send(message);
    }
}