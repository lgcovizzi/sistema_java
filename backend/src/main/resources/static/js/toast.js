/**
 * Sistema de Toast Notifications - Sistema Java
 * Gerencia notificações toast com diferentes tipos e configurações
 */

class ToastManager {
    constructor() {
        this.container = null;
        this.toasts = new Map();
        this.defaultDuration = 5000;
        this.maxToasts = 5;
        this.init();
    }

    /**
     * Inicializa o sistema de toast
     */
    init() {
        this.createContainer();
        this.setupGlobalErrorHandler();
    }

    /**
     * Cria o container principal dos toasts
     */
    createContainer() {
        if (this.container) return;

        this.container = document.createElement('div');
        this.container.className = 'toast-container';
        this.container.setAttribute('aria-live', 'polite');
        this.container.setAttribute('aria-atomic', 'false');
        document.body.appendChild(this.container);
    }

    /**
     * Configura handler global para erros
     */
    setupGlobalErrorHandler() {
        // Intercepta erros de fetch/AJAX
        const originalFetch = window.fetch;
        window.fetch = async (...args) => {
            try {
                const response = await originalFetch(...args);
                if (!response.ok && response.headers.get('content-type')?.includes('application/json')) {
                    const errorData = await response.clone().json();
                    if (errorData.message) {
                        this.error(errorData.message);
                    }
                }
                return response;
            } catch (error) {
                this.error('Erro de conexão. Verifique sua internet.');
                throw error;
            }
        };
    }

    /**
     * Mostra toast de sucesso
     */
    success(message, options = {}) {
        return this.show(message, 'success', options);
    }

    /**
     * Mostra toast de erro
     */
    error(message, options = {}) {
        return this.show(message, 'error', { ...options, duration: options.duration || 7000 });
    }

    /**
     * Mostra toast de aviso
     */
    warning(message, options = {}) {
        return this.show(message, 'warning', options);
    }

    /**
     * Mostra toast de informação
     */
    info(message, options = {}) {
        return this.show(message, 'info', options);
    }

    /**
     * Mostra um toast
     */
    show(message, type = 'info', options = {}) {
        const config = {
            title: this.getDefaultTitle(type),
            duration: this.defaultDuration,
            closable: true,
            progress: true,
            ...options
        };

        // Limita o número de toasts
        if (this.toasts.size >= this.maxToasts) {
            const oldestToast = this.toasts.values().next().value;
            this.hide(oldestToast.id);
        }

        const toast = this.createToast(message, type, config);
        this.container.appendChild(toast.element);
        this.toasts.set(toast.id, toast);

        // Mostra o toast
        requestAnimationFrame(() => {
            toast.element.classList.add('show', 'animate-in');
        });

        // Auto-hide se duration > 0
        if (config.duration > 0) {
            toast.timer = setTimeout(() => {
                this.hide(toast.id);
            }, config.duration);

            // Configura barra de progresso
            if (config.progress) {
                this.startProgress(toast, config.duration);
            }
        }

        return toast.id;
    }

    /**
     * Cria elemento do toast
     */
    createToast(message, type, config) {
        const id = this.generateId();
        const element = document.createElement('div');
        element.className = `toast ${type}`;
        element.setAttribute('role', 'alert');
        element.setAttribute('aria-live', 'assertive');
        element.setAttribute('data-toast-id', id);

        element.innerHTML = `
            <div class="toast-header">
                <h4 class="toast-title">
                    <span class="toast-icon"></span>
                    ${this.escapeHtml(config.title)}
                </h4>
                ${config.closable ? '<button class="toast-close" aria-label="Fechar">&times;</button>' : ''}
            </div>
            <p class="toast-message">${this.escapeHtml(message)}</p>
            ${config.progress ? '<div class="toast-progress"><div class="toast-progress-bar"></div></div>' : ''}
        `;

        // Event listeners
        if (config.closable) {
            const closeBtn = element.querySelector('.toast-close');
            closeBtn.addEventListener('click', () => this.hide(id));
        }

        // Pausa timer no hover
        element.addEventListener('mouseenter', () => {
            const toast = this.toasts.get(id);
            if (toast && toast.timer) {
                clearTimeout(toast.timer);
                toast.paused = true;
            }
        });

        element.addEventListener('mouseleave', () => {
            const toast = this.toasts.get(id);
            if (toast && toast.paused && config.duration > 0) {
                const remaining = toast.remainingTime || config.duration;
                toast.timer = setTimeout(() => this.hide(id), remaining);
                toast.paused = false;
                
                if (config.progress) {
                    this.resumeProgress(toast, remaining);
                }
            }
        });

        return {
            id,
            element,
            type,
            timer: null,
            paused: false,
            remainingTime: config.duration
        };
    }

    /**
     * Esconde um toast
     */
    hide(toastId) {
        const toast = this.toasts.get(toastId);
        if (!toast) return;

        // Limpa timer
        if (toast.timer) {
            clearTimeout(toast.timer);
        }

        // Animação de saída
        toast.element.classList.add('hide', 'animate-out');
        toast.element.classList.remove('show', 'animate-in');

        // Remove do DOM após animação
        setTimeout(() => {
            if (toast.element.parentNode) {
                toast.element.parentNode.removeChild(toast.element);
            }
            this.toasts.delete(toastId);
        }, 300);
    }

    /**
     * Esconde todos os toasts
     */
    hideAll() {
        this.toasts.forEach((toast, id) => {
            this.hide(id);
        });
    }

    /**
     * Inicia barra de progresso
     */
    startProgress(toast, duration) {
        const progressBar = toast.element.querySelector('.toast-progress-bar');
        if (!progressBar) return;

        progressBar.style.transform = 'scaleX(1)';
        progressBar.style.transition = `transform ${duration}ms linear`;
        
        requestAnimationFrame(() => {
            progressBar.style.transform = 'scaleX(0)';
        });
    }

    /**
     * Resume barra de progresso
     */
    resumeProgress(toast, remainingTime) {
        const progressBar = toast.element.querySelector('.toast-progress-bar');
        if (!progressBar) return;

        const currentScale = progressBar.getBoundingClientRect().width / 
                           progressBar.parentElement.getBoundingClientRect().width;
        
        progressBar.style.transition = 'none';
        progressBar.style.transform = `scaleX(${currentScale})`;
        
        requestAnimationFrame(() => {
            progressBar.style.transition = `transform ${remainingTime}ms linear`;
            progressBar.style.transform = 'scaleX(0)';
        });
    }

    /**
     * Obtém título padrão para o tipo
     */
    getDefaultTitle(type) {
        const titles = {
            success: 'Sucesso',
            error: 'Erro',
            warning: 'Atenção',
            info: 'Informação'
        };
        return titles[type] || 'Notificação';
    }

    /**
     * Gera ID único
     */
    generateId() {
        return 'toast_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }

    /**
     * Escapa HTML para prevenir XSS
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * Mostra toast baseado em resposta de erro do servidor
     */
    showFromErrorResponse(errorResponse) {
        if (typeof errorResponse === 'string') {
            this.error(errorResponse);
            return;
        }

        const message = errorResponse.message || 'Erro desconhecido';
        const type = this.getTypeFromErrorCode(errorResponse.code);
        
        this.show(message, type, {
            title: errorResponse.title || this.getDefaultTitle(type)
        });
    }

    /**
     * Determina tipo do toast baseado no código de erro
     */
    getTypeFromErrorCode(code) {
        if (!code) return 'error';
        
        if (code.startsWith('VALIDATION_')) return 'warning';
        if (code.startsWith('AUTH_')) return 'error';
        if (code.startsWith('RESOURCE_')) return 'info';
        if (code.startsWith('RATE_LIMIT_')) return 'warning';
        
        return 'error';
    }

    /**
     * Integração com formulários
     */
    handleFormSubmission(form, successMessage = 'Operação realizada com sucesso!') {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const formData = new FormData(form);
            const submitBtn = form.querySelector('button[type="submit"]');
            const originalText = submitBtn?.textContent;
            
            try {
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.textContent = 'Processando...';
                }
                
                const response = await fetch(form.action, {
                    method: form.method || 'POST',
                    body: formData,
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                });
                
                if (response.ok) {
                    this.success(successMessage);
                    form.reset();
                } else {
                    const errorData = await response.json();
                    this.showFromErrorResponse(errorData);
                }
            } catch (error) {
                this.error('Erro de conexão. Tente novamente.');
            } finally {
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.textContent = originalText;
                }
            }
        });
    }
}

// Instância global
window.Toast = new ToastManager();

// Integração com eventos do documento
document.addEventListener('DOMContentLoaded', () => {
    // Auto-integração com formulários que têm data-toast
    document.querySelectorAll('form[data-toast]').forEach(form => {
        const successMessage = form.getAttribute('data-toast-success') || 
                              'Operação realizada com sucesso!';
        window.Toast.handleFormSubmission(form, successMessage);
    });
    
    // Processa mensagens flash do servidor
    const flashMessages = document.querySelectorAll('[data-flash-message]');
    flashMessages.forEach(element => {
        const message = element.getAttribute('data-flash-message');
        const type = element.getAttribute('data-flash-type') || 'info';
        window.Toast.show(message, type);
        element.remove();
    });
});

// Exporta para uso em módulos
if (typeof module !== 'undefined' && module.exports) {
    module.exports = ToastManager;
}