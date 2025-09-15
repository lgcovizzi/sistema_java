# Exemplo de Cliente JWT - Dispensar Login

Este documento demonstra como implementar a funcionalidade de dispensar o login quando o JWT já está salvo no cliente.

## Fluxo de Autenticação

### 1. Verificação de Token Existente

Antes de exibir a tela de login, o cliente deve verificar se já possui um token JWT válido armazenado.

```javascript
// Exemplo em JavaScript
class AuthManager {
    constructor() {
        this.baseURL = 'http://localhost:8080/api/auth';
        this.tokenKey = 'jwt_access_token';
        this.refreshTokenKey = 'jwt_refresh_token';
    }

    // Verifica se o usuário já está autenticado
    async isAuthenticated() {
        const token = this.getStoredToken();
        
        if (!token) {
            return false;
        }

        // Valida o token com o servidor
        const isValid = await this.validateToken(token);
        
        if (isValid) {
            console.log('Token válido encontrado - dispensando login');
            return true;
        } else {
            // Token inválido, tenta renovar
            return await this.tryRefreshToken();
        }
    }

    // Obtém o token armazenado
    getStoredToken() {
        return localStorage.getItem(this.tokenKey);
    }

    // Valida o token com o servidor
    async validateToken(token) {
        try {
            const response = await fetch(`${this.baseURL}/validate-token`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ token })
            });

            const result = await response.json();
            return result.valid === true;
        } catch (error) {
            console.error('Erro ao validar token:', error);
            return false;
        }
    }

    // Tenta renovar o token usando refresh token
    async tryRefreshToken() {
        const refreshToken = localStorage.getItem(this.refreshTokenKey);
        
        if (!refreshToken) {
            return false;
        }

        try {
            const response = await fetch(`${this.baseURL}/refresh`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ refreshToken })
            });

            if (response.ok) {
                const data = await response.json();
                this.storeTokens(data.accessToken, data.refreshToken);
                console.log('Token renovado com sucesso');
                return true;
            }
        } catch (error) {
            console.error('Erro ao renovar token:', error);
        }

        // Se chegou aqui, não foi possível renovar
        this.clearTokens();
        return false;
    }

    // Armazena os tokens
    storeTokens(accessToken, refreshToken) {
        localStorage.setItem(this.tokenKey, accessToken);
        if (refreshToken) {
            localStorage.setItem(this.refreshTokenKey, refreshToken);
        }
    }

    // Remove os tokens
    clearTokens() {
        localStorage.removeItem(this.tokenKey);
        localStorage.removeItem(this.refreshTokenKey);
    }

    // Realiza login
    async login(usernameOrEmail, password) {
        try {
            const response = await fetch(`${this.baseURL}/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    usernameOrEmail,
                    password
                })
            });

            if (response.ok) {
                const data = await response.json();
                this.storeTokens(data.accessToken, data.refreshToken);
                return { success: true, user: data.user };
            } else {
                return { success: false, error: 'Credenciais inválidas' };
            }
        } catch (error) {
            return { success: false, error: 'Erro de conexão' };
        }
    }

    // Obtém dados do usuário atual
    async getCurrentUser() {
        const token = this.getStoredToken();
        
        if (!token) {
            return null;
        }

        try {
            const response = await fetch(`${this.baseURL}/me`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                return await response.json();
            }
        } catch (error) {
            console.error('Erro ao obter usuário:', error);
        }

        return null;
    }

    // Realiza logout
    async logout() {
        const refreshToken = localStorage.getItem(this.refreshTokenKey);
        
        if (refreshToken) {
            try {
                await fetch(`${this.baseURL}/logout`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ refreshToken })
                });
            } catch (error) {
                console.error('Erro ao fazer logout:', error);
            }
        }

        this.clearTokens();
    }
}
```

### 2. Implementação na Aplicação

```javascript
// Exemplo de uso na aplicação
const authManager = new AuthManager();

// Função principal de inicialização
async function initializeApp() {
    // Verifica se o usuário já está autenticado
    const isAuthenticated = await authManager.isAuthenticated();
    
    if (isAuthenticated) {
        // Usuário já autenticado - vai direto para a aplicação
        const user = await authManager.getCurrentUser();
        showDashboard(user);
    } else {
        // Usuário não autenticado - mostra tela de login
        showLoginScreen();
    }
}

// Função de login
async function handleLogin(usernameOrEmail, password) {
    const result = await authManager.login(usernameOrEmail, password);
    
    if (result.success) {
        showDashboard(result.user);
    } else {
        showError(result.error);
    }
}

// Função de logout
async function handleLogout() {
    await authManager.logout();
    showLoginScreen();
}

// Interceptador para requisições HTTP
function setupHttpInterceptor() {
    // Adiciona token automaticamente em todas as requisições
    const originalFetch = window.fetch;
    
    window.fetch = function(url, options = {}) {
        const token = authManager.getStoredToken();
        
        if (token && !options.headers?.Authorization) {
            options.headers = {
                ...options.headers,
                'Authorization': `Bearer ${token}`
            };
        }
        
        return originalFetch(url, options)
            .then(response => {
                // Se receber 401/403, token pode estar expirado
                if (response.status === 401 || response.status === 403) {
                    authManager.clearTokens();
                    showLoginScreen();
                }
                return response;
            });
    };
}

// Inicializa a aplicação
initializeApp();
setupHttpInterceptor();
```

### 3. Exemplo em React

```jsx
import React, { useState, useEffect, createContext, useContext } from 'react';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const authManager = new AuthManager();

    useEffect(() => {
        checkAuthentication();
    }, []);

    const checkAuthentication = async () => {
        setLoading(true);
        
        const isAuthenticated = await authManager.isAuthenticated();
        
        if (isAuthenticated) {
            const userData = await authManager.getCurrentUser();
            setUser(userData);
        }
        
        setLoading(false);
    };

    const login = async (usernameOrEmail, password) => {
        const result = await authManager.login(usernameOrEmail, password);
        
        if (result.success) {
            setUser(result.user);
        }
        
        return result;
    };

    const logout = async () => {
        await authManager.logout();
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, login, logout, loading }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth deve ser usado dentro de AuthProvider');
    }
    return context;
};

// Componente principal da aplicação
const App = () => {
    const { user, loading } = useAuth();

    if (loading) {
        return <div>Carregando...</div>;
    }

    return (
        <div>
            {user ? (
                <Dashboard user={user} />
            ) : (
                <LoginScreen />
            )}
        </div>
    );
};
```

## Endpoints Disponíveis

### Validação de Token
```http
POST /api/auth/validate-token
Content-Type: application/json

{
    "token": "eyJhbGciOiJSUzI1NiJ9..."
}
```

**Resposta (Token Válido):**
```json
{
    "valid": true,
    "timeToExpiration": 3587,
    "tokenInfo": {
        "expired": false,
        "subject": "testuser",
        "expiration": "2025-09-15T20:05:31.000+00:00",
        "issuedAt": "2025-09-15T19:05:31.000+00:00",
        "type": "access",
        "issuer": "sistema-java"
    }
}
```

### Renovação de Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
    "refreshToken": "qgkZPcsZLVA9KFJ3UcZKhGO7gQ6KHpoZ..."
}
```

### Dados do Usuário Atual
```http
GET /api/auth/me
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
```

## Boas Práticas

1. **Armazenamento Seguro**: Use `localStorage` para tokens de acesso e considere `httpOnly cookies` para refresh tokens em produção.

2. **Validação Automática**: Sempre valide tokens antes de assumir que o usuário está autenticado.

3. **Renovação Transparente**: Implemente renovação automática de tokens para melhor experiência do usuário.

4. **Interceptadores HTTP**: Configure interceptadores para adicionar tokens automaticamente e tratar expiração.

5. **Fallback Gracioso**: Sempre tenha um plano B quando a validação/renovação falhar.

6. **Logs de Segurança**: Monitore tentativas de acesso com tokens inválidos.

## Fluxo Resumido

1. **Inicialização**: Aplicação verifica se existe token armazenado
2. **Validação**: Token é validado com o servidor
3. **Decisão**: 
   - Token válido → Usuário vai direto para a aplicação
   - Token inválido → Tenta renovar com refresh token
   - Renovação falha → Mostra tela de login
4. **Interceptação**: Todas as requisições incluem o token automaticamente
5. **Monitoramento**: Sistema monitora expiração e renova quando necessário

Esta implementação garante que o usuário só precise fazer login uma vez, e o sistema automaticamente gerencia a autenticação em segundo plano.