/**
 * Testes de Integração para ToastManager
 * Testa funcionalidades avançadas e integração com DOM
 */

const fs = require('fs');
const path = require('path');

// Carrega o código do ToastManager
const toastCode = fs.readFileSync(
  path.join(__dirname, '../../main/resources/static/js/toast.js'),
  'utf8'
);

// Avalia o código no contexto global
eval(toastCode);

describe('ToastManager - Integração e Funcionalidades Avançadas', () => {
  let toastManager;

  beforeEach(() => {
    document.body.innerHTML = '';
    document.head.innerHTML = '';
    toastManager = new ToastManager();
  });

  afterEach(() => {
    if (toastManager) {
      toastManager.hideAll();
    }
    const container = document.querySelector('.toast-container');
    if (container) {
      container.remove();
    }
  });

  describe('Integração com API REST', () => {
    test('deve fazer requisição para obter mensagens do servidor', async () => {
      // Mock do fetch
      global.fetch = jest.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve([
          { type: 'success', title: 'Sucesso', message: 'Operação realizada' },
          { type: 'error', title: 'Erro', message: 'Falha na operação' }
        ])
      });

      await toastManager.loadServerMessages();

      expect(fetch).toHaveBeenCalledWith('/api/toast/messages');
      
      // Verifica se toasts foram criados
      jest.advanceTimersByTime(100);
      expect(document.querySelector('.toast.success')).toBeTruthy();
      expect(document.querySelector('.toast.error')).toBeTruthy();
    });

    test('deve lidar com erro na requisição de mensagens', async () => {
      global.fetch = jest.fn().mockRejectedValue(new Error('Network error'));

      // Não deve lançar exceção
      await expect(toastManager.loadServerMessages()).resolves.not.toThrow();
    });

    test('deve enviar mensagem para servidor via API', async () => {
      global.fetch = jest.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ success: true })
      });

      await toastManager.sendToServer('success', 'Mensagem de teste');

      expect(fetch).toHaveBeenCalledWith('/api/toast/success', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          title: 'Sucesso',
          message: 'Mensagem de teste'
        })
      });
    });
  });

  describe('Integração com Formulários', () => {
    test('deve interceptar submit de formulário com data-toast', () => {
      const form = document.createElement('form');
      form.setAttribute('data-toast', 'true');
      form.setAttribute('data-toast-success', 'Formulário enviado com sucesso!');
      
      const submitButton = document.createElement('button');
      submitButton.type = 'submit';
      submitButton.textContent = 'Enviar';
      form.appendChild(submitButton);
      
      document.body.appendChild(form);

      // Configura integração
      toastManager.handleFormSubmission(form, 'Formulário enviado com sucesso!');

      // Simula submit
      const submitEvent = new Event('submit', { bubbles: true, cancelable: true });
      form.dispatchEvent(submitEvent);

      // Verifica se toast foi criado
      jest.advanceTimersByTime(100);
      const toast = document.querySelector('.toast.success');
      expect(toast).toBeTruthy();
      expect(toast.textContent).toContain('Formulário enviado com sucesso!');
    });

    test('deve validar formulário antes de mostrar toast', () => {
      const form = document.createElement('form');
      form.setAttribute('data-toast', 'true');
      
      const input = document.createElement('input');
      input.type = 'email';
      input.required = true;
      input.value = 'email-inválido'; // Email inválido
      form.appendChild(input);
      
      document.body.appendChild(form);

      toastManager.handleFormSubmission(form, 'Sucesso');

      // Simula submit com dados inválidos
      const submitEvent = new Event('submit', { bubbles: true, cancelable: true });
      form.dispatchEvent(submitEvent);

      // Não deve criar toast de sucesso
      const successToast = document.querySelector('.toast.success');
      expect(successToast).toBeFalsy();
    });
  });

  describe('Integração com Eventos Globais', () => {
    test('deve capturar erros JavaScript globais', () => {
      // Simula erro global
      const errorEvent = new ErrorEvent('error', {
        message: 'ReferenceError: variavel is not defined',
        filename: 'app.js',
        lineno: 42,
        colno: 15
      });

      window.dispatchEvent(errorEvent);

      jest.advanceTimersByTime(100);
      const errorToast = document.querySelector('.toast.error');
      expect(errorToast).toBeTruthy();
      expect(errorToast.textContent).toContain('Erro JavaScript');
    });

    test('deve capturar promises rejeitadas', () => {
      // Simula promise rejeitada
      const rejectionEvent = new PromiseRejectionEvent('unhandledrejection', {
        promise: Promise.reject(new Error('Promise rejeitada')),
        reason: new Error('Promise rejeitada')
      });

      window.dispatchEvent(rejectionEvent);

      jest.advanceTimersByTime(100);
      const errorToast = document.querySelector('.toast.error');
      expect(errorToast).toBeTruthy();
      expect(errorToast.textContent).toContain('Erro não tratado');
    });
  });

  describe('Responsividade e Layout', () => {
    test('deve ajustar posição em telas pequenas', () => {
      // Simula tela pequena
      Object.defineProperty(window, 'innerWidth', {
        writable: true,
        configurable: true,
        value: 480
      });

      toastManager.updateLayout();

      const container = document.querySelector('.toast-container');
      expect(container.classList.contains('mobile')).toBe(true);
    });

    test('deve empilhar toasts corretamente', () => {
      toastManager.success('Toast 1');
      toastManager.success('Toast 2');
      toastManager.success('Toast 3');

      const toasts = document.querySelectorAll('.toast');
      expect(toasts.length).toBe(3);

      // Verifica se estão empilhados (cada um com z-index diferente)
      toasts.forEach((toast, index) => {
        const zIndex = window.getComputedStyle(toast).zIndex;
        expect(parseInt(zIndex)).toBeGreaterThan(0);
      });
    });
  });

  describe('Animações e Transições', () => {
    test('deve aplicar animação de entrada', () => {
      const toastId = toastManager.success('Teste');

      jest.advanceTimersByTime(100);
      const toast = document.querySelector('.toast.success');
      expect(toast.classList.contains('animate-in')).toBe(true);
    });

    test('deve aplicar animação de saída', () => {
      const toastId = toastManager.success('Teste');
      
      jest.advanceTimersByTime(100);
      toastManager.hide(toastId);

      const toast = document.querySelector('.toast.success');
      expect(toast.classList.contains('animate-out')).toBe(true);
    });

    test('deve aguardar fim da animação antes de remover do DOM', () => {
      const toastId = toastManager.success('Teste');
      
      toastManager.hide(toastId);

      // Imediatamente após hide, ainda deve estar no DOM
      expect(document.querySelector('.toast.success')).toBeTruthy();

      // Após tempo da animação, deve ser removido
      jest.advanceTimersByTime(300);
      expect(document.querySelector('.toast.success')).toBeFalsy();
    });
  });

  describe('Persistência e Estado', () => {
    test('deve salvar configurações no localStorage', () => {
      toastManager.saveSettings({
        defaultDuration: 3000,
        maxToasts: 3,
        position: 'bottom-left'
      });

      expect(localStorage.setItem).toHaveBeenCalledWith(
        'toastSettings',
        JSON.stringify({
          defaultDuration: 3000,
          maxToasts: 3,
          position: 'bottom-left'
        })
      );
    });

    test('deve carregar configurações do localStorage', () => {
      localStorage.getItem.mockReturnValue(JSON.stringify({
        defaultDuration: 2000,
        maxToasts: 8
      }));

      const settings = toastManager.loadSettings();

      expect(settings.defaultDuration).toBe(2000);
      expect(settings.maxToasts).toBe(8);
    });
  });

  describe('Temas e Customização', () => {
    test('deve aplicar tema escuro', () => {
      toastManager.setTheme('dark');

      const container = document.querySelector('.toast-container');
      expect(container.classList.contains('theme-dark')).toBe(true);
    });

    test('deve aplicar tema claro', () => {
      toastManager.setTheme('light');

      const container = document.querySelector('.toast-container');
      expect(container.classList.contains('theme-light')).toBe(true);
    });

    test('deve detectar tema do sistema', () => {
      // Mock do matchMedia
      window.matchMedia = jest.fn().mockImplementation(query => ({
        matches: query === '(prefers-color-scheme: dark)',
        media: query,
        onchange: null,
        addListener: jest.fn(),
        removeListener: jest.fn(),
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn(),
      }));

      const theme = toastManager.detectSystemTheme();
      expect(theme).toBe('dark');
    });
  });

  describe('Internacionalização', () => {
    test('deve usar textos em português por padrão', () => {
      toastManager.success('Teste');

      const toast = document.querySelector('.toast.success');
      expect(toast.textContent).toContain('Sucesso');
    });

    test('deve permitir mudança de idioma', () => {
      toastManager.setLanguage('en');
      toastManager.success('Test');

      const toast = document.querySelector('.toast.success');
      expect(toast.textContent).toContain('Success');
    });

    test('deve formatar datas conforme idioma', () => {
      toastManager.setLanguage('pt-BR');
      const formattedDate = toastManager.formatDate(new Date('2024-01-15'));
      
      expect(formattedDate).toMatch(/15\/01\/2024/);
    });
  });

  describe('Acessibilidade Avançada', () => {
    test('deve anunciar toasts para leitores de tela', () => {
      const announcer = document.createElement('div');
      announcer.setAttribute('aria-live', 'assertive');
      announcer.setAttribute('aria-atomic', 'true');
      announcer.className = 'sr-only';
      document.body.appendChild(announcer);

      toastManager.success('Mensagem importante');

      expect(announcer.textContent).toContain('Mensagem importante');
    });

    test('deve permitir navegação por teclado', () => {
      toastManager.success('Toast 1');
      toastManager.success('Toast 2');

      const toasts = document.querySelectorAll('.toast');
      
      // Simula Tab para navegar entre toasts
      toasts[0].focus();
      expect(document.activeElement).toBe(toasts[0]);

      // Simula Enter para fechar toast focado
      const enterEvent = new KeyboardEvent('keydown', { key: 'Enter' });
      toasts[0].dispatchEvent(enterEvent);

      jest.advanceTimersByTime(100);
      expect(document.querySelectorAll('.toast').length).toBe(1);
    });

    test('deve suportar Escape para fechar todos os toasts', () => {
      toastManager.success('Toast 1');
      toastManager.success('Toast 2');
      toastManager.success('Toast 3');

      const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });
      document.dispatchEvent(escapeEvent);

      jest.advanceTimersByTime(100);
      expect(document.querySelectorAll('.toast').length).toBe(0);
    });
  });

  describe('Performance e Otimização', () => {
    test('deve usar throttling para eventos de resize', () => {
      const resizeHandler = jest.fn();
      toastManager.onResize = resizeHandler;

      // Dispara múltiplos eventos de resize rapidamente
      for (let i = 0; i < 10; i++) {
        window.dispatchEvent(new Event('resize'));
      }

      jest.advanceTimersByTime(100);

      // Deve ter sido chamado apenas uma vez devido ao throttling
      expect(resizeHandler).toHaveBeenCalledTimes(1);
    });

    test('deve limpar recursos ao destruir', () => {
      const toastId = toastManager.success('Teste');
      
      toastManager.destroy();

      // Todos os timers devem ter sido limpos
      expect(toastManager.toasts.size).toBe(0);
      
      // Container deve ter sido removido
      expect(document.querySelector('.toast-container')).toBeFalsy();
    });

    test('deve reutilizar elementos DOM quando possível', () => {
      // Cria e remove vários toasts
      for (let i = 0; i < 5; i++) {
        const id = toastManager.success(`Toast ${i}`);
        toastManager.hide(id);
        jest.advanceTimersByTime(300);
      }

      // Verifica se pool de elementos foi criado
      expect(toastManager.elementPool).toBeDefined();
      expect(toastManager.elementPool.length).toBeGreaterThan(0);
    });
  });

  describe('Integração com Frameworks', () => {
    test('deve funcionar com jQuery se disponível', () => {
      // Mock do jQuery
      global.$ = jest.fn().mockReturnValue({
        on: jest.fn(),
        off: jest.fn(),
        trigger: jest.fn()
      });

      toastManager.initJQueryIntegration();

      expect(global.$).toHaveBeenCalled();
    });

    test('deve expor API para React/Vue', () => {
      const reactComponent = {
        showToast: (type, message) => toastManager[type](message)
      };

      reactComponent.showToast('success', 'Teste React');

      const toast = document.querySelector('.toast.success');
      expect(toast).toBeTruthy();
      expect(toast.textContent).toContain('Teste React');
    });
  });

  describe('Casos de Uso Complexos', () => {
    test('deve lidar com múltiplas instâncias', () => {
      const manager1 = new ToastManager();
      const manager2 = new ToastManager();

      manager1.success('Manager 1');
      manager2.error('Manager 2');

      // Cada manager deve ter seu próprio container
      const containers = document.querySelectorAll('.toast-container');
      expect(containers.length).toBe(3); // 2 novos + 1 do beforeEach
    });

    test('deve sincronizar entre abas do navegador', () => {
      // Mock do BroadcastChannel
      global.BroadcastChannel = jest.fn().mockImplementation(() => ({
        postMessage: jest.fn(),
        addEventListener: jest.fn(),
        close: jest.fn()
      }));

      toastManager.enableCrossTabSync();
      toastManager.success('Mensagem sincronizada');

      expect(BroadcastChannel).toHaveBeenCalledWith('toast-sync');
    });

    test('deve integrar com sistema de notificações do browser', async () => {
      // Mock da Notification API
      global.Notification = jest.fn().mockImplementation(() => ({
        close: jest.fn()
      }));
      global.Notification.permission = 'granted';
      global.Notification.requestPermission = jest.fn().mockResolvedValue('granted');

      await toastManager.enableBrowserNotifications();
      toastManager.success('Notificação importante', { 
        browserNotification: true 
      });

      expect(global.Notification).toHaveBeenCalledWith(
        'Sucesso',
        expect.objectContaining({
          body: 'Notificação importante'
        })
      );
    });
  });
});