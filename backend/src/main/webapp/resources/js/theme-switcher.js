/**
 * Sistema de Alternância de Temas - Sistema Java
 * Referência: Sistema de Temas Claros e Escuros - project_rules.md
 * Referência: Funcionalidades JavaScript - project_rules.md
 */

(function() {
    'use strict';

    // Constantes
    const THEME_KEY = 'sistema-java-theme';
    const THEMES = {
        LIGHT: 'light',
        DARK: 'dark',
        AUTO: 'auto'
    };

    // Estado do tema
    let currentTheme = THEMES.AUTO;
    let systemPreference = 'light';

    /**
     * Detecta a preferência do sistema operacional
     * Referência: Detecção automática de preferência do sistema - project_rules.md
     */
    function detectSystemPreference() {
        if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            systemPreference = 'dark';
        } else {
            systemPreference = 'light';
        }
        return systemPreference;
    }

    /**
     * Aplica o tema ao documento HTML
     * Referência: Aplicação do tema antes do carregamento completo da página - project_rules.md
     */
    function applyTheme(theme) {
        const html = document.documentElement;
        
        // Remove classes de tema existentes
        html.classList.remove('light-theme', 'dark-theme');
        
        // Aplica o tema apropriado
        if (theme === THEMES.AUTO) {
            // Usa a preferência do sistema
            const systemTheme = detectSystemPreference();
            html.classList.add(systemTheme + '-theme');
        } else {
            html.classList.add(theme + '-theme');
        }
        
        // Atualiza o estado atual
        currentTheme = theme;
        
        // Atualiza os controles de tema
        updateThemeControls();
        
        // Dispara evento customizado
        window.dispatchEvent(new CustomEvent('themeChanged', {
            detail: { theme: theme, effectiveTheme: getEffectiveTheme() }
        }));
    }

    /**
     * Retorna o tema efetivo (resolvendo 'auto')
     */
    function getEffectiveTheme() {
        if (currentTheme === THEMES.AUTO) {
            return detectSystemPreference();
        }
        return currentTheme;
    }

    /**
     * Salva a preferência do usuário no localStorage
     * Referência: Persistência da preferência do usuário - project_rules.md
     */
    function saveThemePreference(theme) {
        try {
            localStorage.setItem(THEME_KEY, theme);
        } catch (error) {
            console.warn('Não foi possível salvar a preferência de tema:', error);
        }
    }

    /**
     * Carrega a preferência do usuário do localStorage
     * Referência: Persistência da preferência do usuário - project_rules.md
     */
    function loadThemePreference() {
        try {
            const saved = localStorage.getItem(THEME_KEY);
            if (saved && Object.values(THEMES).includes(saved)) {
                return saved;
            }
        } catch (error) {
            console.warn('Não foi possível carregar a preferência de tema:', error);
        }
        return THEMES.AUTO;
    }

    /**
     * Alterna para o próximo tema na sequência
     */
    function toggleTheme() {
        let nextTheme;
        
        switch (currentTheme) {
            case THEMES.LIGHT:
                nextTheme = THEMES.DARK;
                break;
            case THEMES.DARK:
                nextTheme = THEMES.AUTO;
                break;
            case THEMES.AUTO:
            default:
                nextTheme = THEMES.LIGHT;
                break;
        }
        
        setTheme(nextTheme);
    }

    /**
     * Define um tema específico
     */
    function setTheme(theme) {
        if (!Object.values(THEMES).includes(theme)) {
            console.warn('Tema inválido:', theme);
            return;
        }
        
        applyTheme(theme);
        saveThemePreference(theme);
    }

    /**
     * Atualiza os controles visuais de tema
     * Referência: Ícones e indicadores visuais para o estado do tema - project_rules.md
     */
    function updateThemeControls() {
        const toggleButtons = document.querySelectorAll('.theme-toggle');
        const themeSelects = document.querySelectorAll('.theme-select');
        
        toggleButtons.forEach(button => {
            const icon = button.querySelector('.theme-icon');
            const text = button.querySelector('.theme-text');
            
            if (icon) {
                // Atualiza o ícone baseado no tema efetivo
                const effectiveTheme = getEffectiveTheme();
                if (effectiveTheme === 'dark') {
                    icon.className = 'theme-icon fas fa-moon';
                } else {
                    icon.className = 'theme-icon fas fa-sun';
                }
            }
            
            if (text) {
                // Atualiza o texto baseado no tema atual
                switch (currentTheme) {
                    case THEMES.LIGHT:
                        text.textContent = 'Claro';
                        break;
                    case THEMES.DARK:
                        text.textContent = 'Escuro';
                        break;
                    case THEMES.AUTO:
                        text.textContent = 'Auto';
                        break;
                }
            }
            
            // Atualiza aria-label para acessibilidade
            button.setAttribute('aria-label', `Tema atual: ${currentTheme}. Clique para alternar.`);
        });
        
        themeSelects.forEach(select => {
            select.value = currentTheme;
        });
    }

    /**
     * Inicializa os event listeners
     * Referência: Suporte a navegação por teclado no toggle - project_rules.md
     */
    function initializeEventListeners() {
        // Toggle buttons
        document.addEventListener('click', function(event) {
            if (event.target.closest('.theme-toggle')) {
                event.preventDefault();
                toggleTheme();
            }
        });
        
        // Suporte a teclado para toggle buttons
        document.addEventListener('keydown', function(event) {
            const toggleButton = event.target.closest('.theme-toggle');
            if (toggleButton && (event.key === 'Enter' || event.key === ' ')) {
                event.preventDefault();
                toggleTheme();
            }
        });
        
        // Theme selects
        document.addEventListener('change', function(event) {
            if (event.target.classList.contains('theme-select')) {
                setTheme(event.target.value);
            }
        });
        
        // Escuta mudanças na preferência do sistema
        // Referência: Sincronizar com mudanças de preferência do sistema - project_rules.md
        if (window.matchMedia) {
            const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
            
            // Função para lidar com mudanças
            function handleSystemThemeChange(e) {
                systemPreference = e.matches ? 'dark' : 'light';
                
                // Se o tema atual é 'auto', reaplica o tema
                if (currentTheme === THEMES.AUTO) {
                    applyTheme(THEMES.AUTO);
                }
            }
            
            // Adiciona listener (método moderno)
            if (mediaQuery.addEventListener) {
                mediaQuery.addEventListener('change', handleSystemThemeChange);
            } else {
                // Fallback para navegadores mais antigos
                mediaQuery.addListener(handleSystemThemeChange);
            }
        }
    }

    /**
     * Inicializa o sistema de temas
     * Referência: Aplicar tema imediatamente ao carregar a página - project_rules.md
     */
    function initializeThemeSystem() {
        // Detecta preferência do sistema
        detectSystemPreference();
        
        // Carrega preferência salva
        const savedTheme = loadThemePreference();
        
        // Aplica o tema
        applyTheme(savedTheme);
        
        // Inicializa event listeners
        initializeEventListeners();
        
        // Marca como inicializado
        document.documentElement.setAttribute('data-theme-initialized', 'true');
        
        console.log('Sistema de temas inicializado:', {
            currentTheme: currentTheme,
            effectiveTheme: getEffectiveTheme(),
            systemPreference: systemPreference
        });
    }

    /**
     * API pública do sistema de temas
     */
    window.ThemeSystem = {
        // Métodos públicos
        setTheme: setTheme,
        toggleTheme: toggleTheme,
        getCurrentTheme: () => currentTheme,
        getEffectiveTheme: getEffectiveTheme,
        getSystemPreference: () => systemPreference,
        
        // Constantes
        THEMES: THEMES,
        
        // Eventos
        on: function(event, callback) {
            window.addEventListener(event, callback);
        },
        
        off: function(event, callback) {
            window.removeEventListener(event, callback);
        }
    };

    // Inicialização
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializeThemeSystem);
    } else {
        initializeThemeSystem();
    }

    // Aplicação imediata do tema para evitar flash
    // Referência: Aplicar tema antes do carregamento completo da página - project_rules.md
    (function immediateThemeApplication() {
        const savedTheme = loadThemePreference();
        const html = document.documentElement;
        
        // Remove classes existentes
        html.classList.remove('light-theme', 'dark-theme');
        
        // Aplica tema imediatamente
        if (savedTheme === THEMES.AUTO) {
            const systemTheme = detectSystemPreference();
            html.classList.add(systemTheme + '-theme');
        } else {
            html.classList.add(savedTheme + '-theme');
        }
    })();

})();

/**
 * Utilitários para integração com JSF
 * Referência: Frontend JSF + PrimeFaces - project_rules.md
 */
window.SistemaJavaTheme = {
    /**
     * Função para ser chamada via JSF
     */
    setThemeFromJSF: function(theme) {
        if (window.ThemeSystem) {
            window.ThemeSystem.setTheme(theme);
        }
    },
    
    /**
     * Retorna o tema atual para JSF
     */
    getCurrentThemeForJSF: function() {
        return window.ThemeSystem ? window.ThemeSystem.getCurrentTheme() : 'auto';
    },
    
    /**
     * Retorna o tema efetivo para JSF
     */
    getEffectiveThemeForJSF: function() {
        return window.ThemeSystem ? window.ThemeSystem.getEffectiveTheme() : 'light';
    }
};