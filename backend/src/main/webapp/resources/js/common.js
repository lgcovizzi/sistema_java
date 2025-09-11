/**
 * Funcionalidades JavaScript Comuns - Sistema Java
 * Referência: Configurar recursos estáticos - project_rules.md
 * Referência: Frontend JSF com PrimeFaces - project_rules.md
 */

// ==========================================================================
// UTILITÁRIOS GERAIS
// ==========================================================================

/**
 * Utilitários para manipulação de elementos DOM
 */
const DOMUtils = {
    /**
     * Seleciona um elemento pelo seletor CSS
     */
    select: (selector) => document.querySelector(selector),
    
    /**
     * Seleciona múltiplos elementos pelo seletor CSS
     */
    selectAll: (selector) => document.querySelectorAll(selector),
    
    /**
     * Adiciona classe a um elemento
     */
    addClass: (element, className) => {
        if (element) element.classList.add(className);
    },
    
    /**
     * Remove classe de um elemento
     */
    removeClass: (element, className) => {
        if (element) element.classList.remove(className);
    },
    
    /**
     * Alterna classe em um elemento
     */
    toggleClass: (element, className) => {
        if (element) element.classList.toggle(className);
    },
    
    /**
     * Verifica se elemento possui classe
     */
    hasClass: (element, className) => {
        return element ? element.classList.contains(className) : false;
    }
};

/**
 * Utilitários para validação de formulários
 */
const ValidationUtils = {
    /**
     * Valida email usando regex
     */
    isValidEmail: (email) => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    },
    
    /**
     * Valida CPF brasileiro
     */
    isValidCPF: (cpf) => {
        cpf = cpf.replace(/[^\d]/g, '');
        
        if (cpf.length !== 11 || /^(\d)\1{10}$/.test(cpf)) {
            return false;
        }
        
        let sum = 0;
        for (let i = 0; i < 9; i++) {
            sum += parseInt(cpf.charAt(i)) * (10 - i);
        }
        let remainder = (sum * 10) % 11;
        if (remainder === 10 || remainder === 11) remainder = 0;
        if (remainder !== parseInt(cpf.charAt(9))) return false;
        
        sum = 0;
        for (let i = 0; i < 10; i++) {
            sum += parseInt(cpf.charAt(i)) * (11 - i);
        }
        remainder = (sum * 10) % 11;
        if (remainder === 10 || remainder === 11) remainder = 0;
        
        return remainder === parseInt(cpf.charAt(10));
    },
    
    /**
     * Valida telefone brasileiro
     */
    isValidPhone: (phone) => {
        const phoneRegex = /^[0-9\-()+ ]{8,20}$/;
        return phoneRegex.test(phone);
    },
    
    /**
     * Valida senha forte
     */
    isStrongPassword: (password) => {
        return password.length >= 8 &&
               /[A-Z]/.test(password) &&
               /[a-z]/.test(password) &&
               /[0-9]/.test(password) &&
               /[^A-Za-z0-9]/.test(password);
    }
};

/**
 * Utilitários para formatação de dados
 */
const FormatUtils = {
    /**
     * Formata CPF com máscara
     */
    formatCPF: (cpf) => {
        cpf = cpf.replace(/\D/g, '');
        return cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
    },
    
    /**
     * Formata telefone com máscara
     */
    formatPhone: (phone) => {
        phone = phone.replace(/\D/g, '');
        if (phone.length === 11) {
            return phone.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
        } else if (phone.length === 10) {
            return phone.replace(/(\d{2})(\d{4})(\d{4})/, '($1) $2-$3');
        }
        return phone;
    },
    
    /**
     * Formata data para exibição
     */
    formatDate: (date, format = 'dd/MM/yyyy') => {
        if (!date) return '';
        
        const d = new Date(date);
        const day = String(d.getDate()).padStart(2, '0');
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const year = d.getFullYear();
        
        return format
            .replace('dd', day)
            .replace('MM', month)
            .replace('yyyy', year);
    },
    
    /**
     * Formata número como moeda brasileira
     */
    formatCurrency: (value) => {
        return new Intl.NumberFormat('pt-BR', {
            style: 'currency',
            currency: 'BRL'
        }).format(value);
    }
};

// ==========================================================================
// NOTIFICAÇÕES E MENSAGENS
// ==========================================================================

/**
 * Sistema de notificações toast
 */
const NotificationSystem = {
    /**
     * Mostra notificação de sucesso
     */
    success: (message, duration = 5000) => {
        NotificationSystem.show(message, 'success', duration);
    },
    
    /**
     * Mostra notificação de erro
     */
    error: (message, duration = 7000) => {
        NotificationSystem.show(message, 'error', duration);
    },
    
    /**
     * Mostra notificação de aviso
     */
    warning: (message, duration = 6000) => {
        NotificationSystem.show(message, 'warning', duration);
    },
    
    /**
     * Mostra notificação de informação
     */
    info: (message, duration = 5000) => {
        NotificationSystem.show(message, 'info', duration);
    },
    
    /**
     * Cria e exibe notificação
     */
    show: (message, type, duration) => {
        const container = NotificationSystem.getContainer();
        const notification = NotificationSystem.createNotification(message, type);
        
        container.appendChild(notification);
        
        // Anima entrada
        setTimeout(() => {
            notification.classList.add('show');
        }, 10);
        
        // Remove automaticamente
        setTimeout(() => {
            NotificationSystem.remove(notification);
        }, duration);
        
        // Permite remoção manual
        const closeBtn = notification.querySelector('.notification-close');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => {
                NotificationSystem.remove(notification);
            });
        }
    },
    
    /**
     * Obtém ou cria container de notificações
     */
    getContainer: () => {
        let container = document.getElementById('notification-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'notification-container';
            container.className = 'notification-container';
            document.body.appendChild(container);
        }
        return container;
    },
    
    /**
     * Cria elemento de notificação
     */
    createNotification: (message, type) => {
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        
        const icons = {
            success: '✓',
            error: '✕',
            warning: '⚠',
            info: 'ℹ'
        };
        
        notification.innerHTML = `
            <div class="notification-icon">${icons[type] || 'ℹ'}</div>
            <div class="notification-message">${message}</div>
            <button class="notification-close" type="button">×</button>
        `;
        
        return notification;
    },
    
    /**
     * Remove notificação com animação
     */
    remove: (notification) => {
        notification.classList.add('hide');
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 300);
    }
};

// ==========================================================================
// LOADING E ESTADOS
// ==========================================================================

/**
 * Sistema de loading
 */
const LoadingSystem = {
    /**
     * Mostra loading global
     */
    show: (message = 'Carregando...') => {
        const overlay = LoadingSystem.createOverlay(message);
        document.body.appendChild(overlay);
        setTimeout(() => overlay.classList.add('show'), 10);
    },
    
    /**
     * Esconde loading global
     */
    hide: () => {
        const overlay = document.getElementById('loading-overlay');
        if (overlay) {
            overlay.classList.add('hide');
            setTimeout(() => {
                if (overlay.parentNode) {
                    overlay.parentNode.removeChild(overlay);
                }
            }, 300);
        }
    },
    
    /**
     * Cria overlay de loading
     */
    createOverlay: (message) => {
        const overlay = document.createElement('div');
        overlay.id = 'loading-overlay';
        overlay.className = 'loading-overlay';
        
        overlay.innerHTML = `
            <div class="loading-content">
                <div class="loading-spinner"></div>
                <div class="loading-message">${message}</div>
            </div>
        `;
        
        return overlay;
    },
    
    /**
     * Mostra loading em elemento específico
     */
    showInElement: (element, message = 'Carregando...') => {
        if (!element) return;
        
        const loading = document.createElement('div');
        loading.className = 'element-loading';
        loading.innerHTML = `
            <div class="element-loading-content">
                <div class="loading-spinner-sm"></div>
                <span>${message}</span>
            </div>
        `;
        
        element.style.position = 'relative';
        element.appendChild(loading);
    },
    
    /**
     * Remove loading de elemento específico
     */
    hideInElement: (element) => {
        if (!element) return;
        
        const loading = element.querySelector('.element-loading');
        if (loading) {
            loading.remove();
        }
    }
};

// ==========================================================================
// UTILITÁRIOS DE AJAX
// ==========================================================================

/**
 * Utilitários para requisições AJAX
 */
const AjaxUtils = {
    /**
     * Faz requisição GET
     */
    get: async (url, options = {}) => {
        return AjaxUtils.request(url, { ...options, method: 'GET' });
    },
    
    /**
     * Faz requisição POST
     */
    post: async (url, data, options = {}) => {
        return AjaxUtils.request(url, {
            ...options,
            method: 'POST',
            body: JSON.stringify(data),
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            }
        });
    },
    
    /**
     * Faz requisição PUT
     */
    put: async (url, data, options = {}) => {
        return AjaxUtils.request(url, {
            ...options,
            method: 'PUT',
            body: JSON.stringify(data),
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            }
        });
    },
    
    /**
     * Faz requisição DELETE
     */
    delete: async (url, options = {}) => {
        return AjaxUtils.request(url, { ...options, method: 'DELETE' });
    },
    
    /**
     * Faz requisição genérica
     */
    request: async (url, options = {}) => {
        try {
            const response = await fetch(url, {
                credentials: 'same-origin',
                ...options
            });
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return await response.json();
            }
            
            return await response.text();
        } catch (error) {
            console.error('Erro na requisição:', error);
            throw error;
        }
    }
};

// ==========================================================================
// UTILITÁRIOS DE ARQUIVO
// ==========================================================================

/**
 * Utilitários para manipulação de arquivos
 */
const FileUtils = {
    /**
     * Valida tipo de arquivo
     */
    isValidImageType: (file) => {
        const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
        return validTypes.includes(file.type);
    },
    
    /**
     * Valida tamanho de arquivo
     */
    isValidSize: (file, maxSizeMB = 5) => {
        const maxSizeBytes = maxSizeMB * 1024 * 1024;
        return file.size <= maxSizeBytes;
    },
    
    /**
     * Converte arquivo para base64
     */
    toBase64: (file) => {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.readAsDataURL(file);
            reader.onload = () => resolve(reader.result);
            reader.onerror = error => reject(error);
        });
    },
    
    /**
     * Redimensiona imagem
     */
    resizeImage: (file, maxWidth = 800, maxHeight = 600, quality = 0.8) => {
        return new Promise((resolve) => {
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');
            const img = new Image();
            
            img.onload = () => {
                const { width, height } = FileUtils.calculateDimensions(
                    img.width, img.height, maxWidth, maxHeight
                );
                
                canvas.width = width;
                canvas.height = height;
                
                ctx.drawImage(img, 0, 0, width, height);
                
                canvas.toBlob(resolve, file.type, quality);
            };
            
            img.src = URL.createObjectURL(file);
        });
    },
    
    /**
     * Calcula dimensões proporcionais
     */
    calculateDimensions: (originalWidth, originalHeight, maxWidth, maxHeight) => {
        let { width, height } = { width: originalWidth, height: originalHeight };
        
        if (width > maxWidth) {
            height = (height * maxWidth) / width;
            width = maxWidth;
        }
        
        if (height > maxHeight) {
            width = (width * maxHeight) / height;
            height = maxHeight;
        }
        
        return { width: Math.round(width), height: Math.round(height) };
    }
};

// ==========================================================================
// INICIALIZAÇÃO
// ==========================================================================

/**
 * Inicialização do sistema quando DOM estiver pronto
 */
document.addEventListener('DOMContentLoaded', () => {
    // Adiciona estilos CSS para notificações e loading
    const style = document.createElement('style');
    style.textContent = `
        .notification-container {
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 9999;
            max-width: 400px;
        }
        
        .notification {
            display: flex;
            align-items: center;
            padding: 12px 16px;
            margin-bottom: 8px;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
            transform: translateX(100%);
            opacity: 0;
            transition: all 0.3s ease;
        }
        
        .notification.show {
            transform: translateX(0);
            opacity: 1;
        }
        
        .notification.hide {
            transform: translateX(100%);
            opacity: 0;
        }
        
        .notification-success {
            background: #10b981;
            color: white;
        }
        
        .notification-error {
            background: #ef4444;
            color: white;
        }
        
        .notification-warning {
            background: #f59e0b;
            color: white;
        }
        
        .notification-info {
            background: #3b82f6;
            color: white;
        }
        
        .notification-icon {
            margin-right: 8px;
            font-weight: bold;
        }
        
        .notification-message {
            flex: 1;
        }
        
        .notification-close {
            background: none;
            border: none;
            color: inherit;
            font-size: 18px;
            cursor: pointer;
            margin-left: 8px;
            padding: 0;
            width: 20px;
            height: 20px;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        
        .loading-overlay {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.5);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 10000;
            opacity: 0;
            transition: opacity 0.3s ease;
        }
        
        .loading-overlay.show {
            opacity: 1;
        }
        
        .loading-overlay.hide {
            opacity: 0;
        }
        
        .loading-content {
            background: white;
            padding: 24px;
            border-radius: 8px;
            text-align: center;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        }
        
        .loading-spinner {
            width: 40px;
            height: 40px;
            border: 4px solid #f3f4f6;
            border-top: 4px solid #3b82f6;
            border-radius: 50%;
            animation: spin 1s linear infinite;
            margin: 0 auto 16px;
        }
        
        .loading-spinner-sm {
            width: 20px;
            height: 20px;
            border: 2px solid #f3f4f6;
            border-top: 2px solid #3b82f6;
            border-radius: 50%;
            animation: spin 1s linear infinite;
            margin-right: 8px;
        }
        
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
        
        .element-loading {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(255, 255, 255, 0.8);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 100;
        }
        
        .element-loading-content {
            display: flex;
            align-items: center;
            padding: 8px 16px;
            background: white;
            border-radius: 4px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }
    `;
    document.head.appendChild(style);
    
    console.log('Sistema JavaScript inicializado com sucesso!');
});

// Exporta utilitários para uso global
window.SistemaJava = {
    DOMUtils,
    ValidationUtils,
    FormatUtils,
    NotificationSystem,
    LoadingSystem,
    AjaxUtils,
    FileUtils
};