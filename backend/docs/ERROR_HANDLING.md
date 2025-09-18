# Documentação do Sistema de Tratamento de Erros

## Visão Geral

Este documento detalha o sistema completo de tratamento de erros implementado no projeto, incluindo exceções customizadas, códigos de erro padronizados e estruturas de resposta.

## Estrutura de Arquivos

```
backend/src/main/java/com/sistema/
├── exception/
│   ├── BusinessException.java          # Exceção base
│   ├── ValidationException.java        # Erros de validação
│   ├── AuthenticationException.java    # Erros de autenticação
│   ├── ResourceNotFoundException.java  # Recursos não encontrados
│   ├── RateLimitException.java         # Rate limiting
│   └── GlobalExceptionHandler.java     # Handler global
├── dto/
│   └── ErrorResponse.java              # Resposta padronizada
└── util/
    └── ErrorCodes.java                 # Códigos de erro
```

## Hierarquia de Exceções

### BusinessException (Classe Base)

```java
public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final Object[] parameters;
    
    // Construtores disponíveis:
    public BusinessException(String errorCode, String message)
    public BusinessException(String errorCode, String message, Object... parameters)
    public BusinessException(String errorCode, String message, Throwable cause)
    public BusinessException(String errorCode, String message, Throwable cause, Object... parameters)
}
```

**Características**:
- Exceção base para todas as regras de negócio
- Suporte a internacionalização com `parameters`
- Código de erro como primeiro parâmetro (obrigatório)
- Estende `RuntimeException` (não checked)
- Campos `errorCode` e `parameters` são finais e imutáveis

### Exceções Específicas

#### 1. ValidationException

```java
public class ValidationException extends BusinessException {
    private final Map<String, String> fieldErrors;
    
    // Construtores disponíveis:
    public ValidationException(String message)
    public ValidationException(String message, Map<String, String> fieldErrors)
    public ValidationException(String errorCode, String message, Map<String, String> fieldErrors)
}
```

**Uso**: Erros de validação de entrada, formulários, DTOs

**Exemplo**:
```java
Map<String, String> fieldErrors = new HashMap<>();
fieldErrors.put("email", "Email é obrigatório");
fieldErrors.put("senha", "Senha deve ter pelo menos 8 caracteres");

ValidationException ex = new ValidationException(
    ErrorCodes.VALIDATION_FAILED, 
    "Dados inválidos", 
    fieldErrors
);
throw ex;
```

#### 2. AuthenticationException

```java
public class AuthenticationException extends BusinessException {
    private final boolean requiresCaptcha;
    private final long remainingSeconds;
    
    // Construtores disponíveis:
    public AuthenticationException(String message)
    public AuthenticationException(String errorCode, String message)
    public AuthenticationException(String errorCode, String message, boolean requiresCaptcha)
    public AuthenticationException(String errorCode, String message, long remainingSeconds)
    public AuthenticationException(String errorCode, String message, boolean requiresCaptcha, long remainingSeconds)
}
```

**Uso**: Erros de autenticação, autorização, tokens

**Exemplo**:
```java
throw new AuthenticationException(
    ErrorCodes.INVALID_CREDENTIALS,
    "Credenciais inválidas", 
    true,  // requiresCaptcha
    300L   // remainingSeconds
);
```

#### 3. ResourceNotFoundException

```java
public class ResourceNotFoundException extends BusinessException {
    private final String resourceType;
    private final String resourceId;
    
    // Construtores disponíveis:
    public ResourceNotFoundException(String message)
    public ResourceNotFoundException(String resourceType, String resourceId)
    public ResourceNotFoundException(String errorCode, String message, String resourceType, String resourceId)
}
```

**Uso**: Quando recursos não são encontrados

**Exemplo**:
```java
// Usando construtor com tipo e ID
throw new ResourceNotFoundException("User", "123");

// Usando construtor completo
throw new ResourceNotFoundException(
    ErrorCodes.USER_NOT_FOUND,
    "Usuário não encontrado", 
    "User", 
    "123"
);
```

#### 4. RateLimitException

```java
public class RateLimitException extends BusinessException {
    private final long remainingSeconds;
    private final int maxAttempts;
    private final String limitType;
    
    // Construtores disponíveis:
    public RateLimitException(String message, long remainingSeconds)
    public RateLimitException(String message, long remainingSeconds, int maxAttempts, String limitType)
    public RateLimitException(String errorCode, String message, long remainingSeconds, int maxAttempts, String limitType)
}
```

**Uso**: Violações de rate limiting

**Exemplo**:
```java
// Construtor simples
throw new RateLimitException("Muitas tentativas de login", 300L);

// Construtor completo
throw new RateLimitException(
    ErrorCodes.ACCOUNT_LOCKED,
    "Muitas tentativas de login", 
    300L,   // remainingSeconds
    5,      // maxAttempts
    "LOGIN" // limitType
);
```

## Estrutura de Resposta de Erro

### ErrorResponse

```java
public class ErrorResponse {
    private boolean error = true;
    private String message;
    private String errorCode;
    private LocalDateTime timestamp;
    private String path;
    private int status;
    private Map<String, String> fieldErrors;
    private Map<String, Object> additionalData;
    
    // Construtores e métodos de conveniência
}
```

### Exemplos de Resposta

#### Erro Simples
```json
{
  "error": true,
  "message": "Usuário não encontrado",
  "errorCode": "USER_NOT_FOUND",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/users/123",
  "status": 404
}
```

#### Erro de Validação
```json
{
  "error": true,
  "message": "Dados de entrada inválidos",
  "errorCode": "VALIDATION_FAILED",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/auth/register",
  "status": 400,
  "fieldErrors": {
    "email": "Email é obrigatório",
    "senha": "Senha deve ter pelo menos 8 caracteres",
    "cpf": "CPF inválido"
  }
}
```

#### Erro de Autenticação com Captcha
```json
{
  "error": true,
  "message": "Credenciais inválidas",
  "errorCode": "INVALID_CREDENTIALS",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/auth/login",
  "status": 401,
  "additionalData": {
    "requiresCaptcha": true,
    "remainingSeconds": 300,
    "maxAttempts": 5
  }
}
```

#### Erro de Rate Limiting
```json
{
  "error": true,
  "message": "Muitas tentativas de login",
  "errorCode": "ACCOUNT_LOCKED",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/auth/login",
  "status": 429,
  "additionalData": {
    "remainingSeconds": 1800,
    "maxAttempts": 5,
    "limitType": "LOGIN"
  }
}
```

## Códigos de Erro

### Categorias Disponíveis

#### Erros Gerais
- `INTERNAL_SERVER_ERROR`: Erro interno do servidor
- `INVALID_REQUEST`: Requisição inválida
- `RESOURCE_NOT_FOUND`: Recurso não encontrado
- `ACCESS_DENIED`: Acesso negado

#### Autenticação
- `INVALID_CREDENTIALS`: Credenciais inválidas
- `ACCOUNT_LOCKED`: Conta bloqueada
- `TOKEN_EXPIRED`: Token expirado
- `TOKEN_INVALID`: Token inválido

#### Usuário
- `USER_NOT_FOUND`: Usuário não encontrado
- `EMAIL_ALREADY_EXISTS`: Email já existe
- `CPF_ALREADY_EXISTS`: CPF já existe
- `EMAIL_NOT_VERIFIED`: Email não verificado

#### Captcha
- `CAPTCHA_REQUIRED`: Captcha obrigatório
- `CAPTCHA_INVALID`: Captcha inválido
- `CAPTCHA_EXPIRED`: Captcha expirado

#### Email
- `EMAIL_SEND_FAILED`: Falha no envio de email
- `EMAIL_INVALID_FORMAT`: Formato de email inválido
- `VERIFICATION_TOKEN_INVALID`: Token de verificação inválido

#### Validação
- `VALIDATION_FAILED`: Falha na validação
- `FIELD_REQUIRED`: Campo obrigatório
- `FIELD_INVALID_FORMAT`: Formato de campo inválido
- `PASSWORD_TOO_WEAK`: Senha muito fraca

## GlobalExceptionHandler

### Tratamento Automático

O `GlobalExceptionHandler` captura automaticamente todas as exceções e gera respostas padronizadas:

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
        BusinessException ex, HttpServletRequest request) {
        // Tratamento específico para exceções de negócio
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        ValidationException ex, HttpServletRequest request) {
        // Tratamento específico para validação
    }
    
    // Outros handlers...
}
```

### Mapeamento de Status HTTP

| Exceção | Status HTTP |
|---------|-------------|
| `BusinessException` | 400 (Bad Request) |
| `ValidationException` | 400 (Bad Request) |
| `AuthenticationException` | 401 (Unauthorized) |
| `ResourceNotFoundException` | 404 (Not Found) |
| `RateLimitException` | 429 (Too Many Requests) |
| `AccessDeniedException` | 403 (Forbidden) |
| `Exception` (genérica) | 500 (Internal Server Error) |

## Logging

### Níveis de Log

- **ERROR**: Exceções não tratadas e erros de sistema
- **WARN**: Exceções de negócio (BusinessException)
- **INFO**: Erros de validação e autenticação

### Formato de Log

```
[LEVEL] GlobalExceptionHandler - {message} | Path: {path} | User: {user} | IP: {ip} | ErrorCode: {errorCode}
```

**Exemplo**:
```
[WARN] GlobalExceptionHandler - Credenciais inválidas | Path: /api/auth/login | User: user@example.com | IP: 192.168.1.100 | ErrorCode: INVALID_CREDENTIALS
```

## Guia de Implementação

### 1. Em Serviços

```java
@Service
public class UserService {
    
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Usuário não encontrado", 
                ErrorCodes.USER_NOT_FOUND, 
                "User", 
                id
            ));
    }
    
    public void validateUser(UserDto dto) {
        ValidationException ex = new ValidationException(
            "Dados inválidos", 
            ErrorCodes.VALIDATION_FAILED
        );
        
        if (dto.getEmail() == null) {
            ex.addFieldError("email", "Email é obrigatório");
        }
        
        if (dto.getSenha() == null || dto.getSenha().length() < 8) {
            ex.addFieldError("senha", "Senha deve ter pelo menos 8 caracteres");
        }
        
        if (ex.hasFieldErrors()) {
            throw ex;
        }
    }
}
```

### 2. Em Controllers

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserDto dto) {
        // Não precisa de try-catch
        // GlobalExceptionHandler trata automaticamente
        User user = userService.createUser(dto);
        return ResponseEntity.ok(new UserResponse(user));
    }
}
```

### 3. Em Testes

```java
@Test
void testUserNotFound() {
    ResourceNotFoundException ex = assertThrows(
        ResourceNotFoundException.class,
        () -> userService.findById(999L)
    );
    
    assertEquals(ErrorCodes.USER_NOT_FOUND, ex.getErrorCode());
    assertEquals("User", ex.getResourceType());
    assertEquals(999L, ex.getResourceId());
}

@Test
void testValidationError() {
    UserDto dto = new UserDto();
    // dto sem dados obrigatórios
    
    ValidationException ex = assertThrows(
        ValidationException.class,
        () -> userService.validateUser(dto)
    );
    
    assertEquals(ErrorCodes.VALIDATION_FAILED, ex.getErrorCode());
    assertTrue(ex.getFieldErrors().containsKey("email"));
    assertTrue(ex.getFieldErrors().containsKey("senha"));
}
```

## Migração de Código Existente

### Identificar Padrões Antigos

Procurar por:
- Métodos que retornam `Map<String, Object>` para erros
- Criação manual de `ResponseEntity` com erros
- Uso de `RuntimeException` genérica
- Tratamento manual em controllers

### Substituir Gradualmente

1. **Identificar** o tipo de erro
2. **Escolher** a exceção customizada apropriada
3. **Substituir** o código antigo
4. **Remover** tratamento manual
5. **Testar** o comportamento

### Exemplo de Migração

**Antes**:
```java
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    try {
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", e.getMessage());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.badRequest().body(error);
    }
}
```

**Depois**:
```java
@PostMapping("/login")
public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
    // GlobalExceptionHandler trata automaticamente
    AuthResponse response = authService.authenticate(request);
    return ResponseEntity.ok(response);
}
```

## Boas Práticas

### 1. Sempre Use Exceções Customizadas
- ✅ `throw new AuthenticationException(...)`
- ❌ `throw new RuntimeException(...)`

### 2. Use Códigos de Erro Padronizados
- ✅ `ErrorCodes.INVALID_CREDENTIALS`
- ❌ `"INVALID_LOGIN"`

### 3. Forneça Contexto Adequado
- ✅ Inclua informações relevantes (ID do recurso, tipo, etc.)
- ❌ Mensagens genéricas sem contexto

### 4. Teste Todos os Cenários de Erro
- ✅ Teste cada tipo de exceção
- ✅ Verifique códigos de erro
- ✅ Valide estrutura de resposta

### 5. Mantenha Logs Estruturados
- ✅ Use o sistema de log automático
- ❌ Logs manuais inconsistentes

## Troubleshooting

### Problemas Comuns

1. **Exceção não capturada pelo GlobalExceptionHandler**
   - Verifique se a exceção estende `BusinessException`
   - Confirme que o handler está no pacote correto

2. **Campos adicionais não aparecem na resposta**
   - Verifique se os getters estão implementados
   - Confirme a serialização JSON

3. **Códigos de erro inconsistentes**
   - Use apenas constantes de `ErrorCodes`
   - Evite strings hardcoded

4. **Logs não aparecem**
   - Verifique configuração do SLF4J
   - Confirme nível de log configurado

### Debug

Para debugar o sistema de erros:

1. **Ative logs de debug**:
```yaml
logging:
  level:
    com.sistema.exception: DEBUG
```

2. **Adicione breakpoints** no `GlobalExceptionHandler`

3. **Verifique stack traces** completos

4. **Teste com Postman** ou similar para ver respostas completas