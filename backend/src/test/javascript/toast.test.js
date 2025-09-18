/**
 * Testes para ToastManager
 * Sistema de notificações toast - Frontend JavaScript
 */

// Importa o código do ToastManager
const fs = require('fs');
const path = require('path');

// Carrega o código do ToastManager
const toastCode = fs.readFileSync(
  path.join(__dirname, '../../main/resources/static/js/toast.js'),
  'utf8'
);

// Avalia o código no contexto global
eval(toastCode);

describe('ToastManager', () => {
  let toastManager;

  beforeEach(() => {
    // Limpa o DOM
    document.body.innerHTML = '';
    document.head.innerHTML = '';
    
    // Cria nova instância do ToastManager
    toastManager = new ToastManager();
  });

  afterEach(() => {
    // Limpa todos os toasts
    if (toastManager) {
      toastManager.hideAll();
    }
    
    // Remove container do DOM
    const container = document.querySelector('.toast-container');
    if (container) {
      container.remove();
    }
  });

  describe('Inicialização', () => {
    test('deve criar container de toast no DOM', () => {
      expect(document.querySelector('.toast-container')).toBeTruthy();
    });

    test('deve inicializar propriedades padrão', () => {
      expect(toastManager.defaultDuration).toBe(5000);
      expect(toastManager.maxToasts).toBe(5);
      expect(toastManager.toasts).toBeInstanceOf(Map);
      expect(toastManager.toasts.size).toBe(0);
    });

    test('deve configurar handler de erro global', () => {
      // Simula erro global
      const errorEvent = new ErrorEvent('error', {
        message: 'Erro de teste',
        filename: 'test.js',
        lineno: 1
      });

      // Dispara evento de erro
      window.dispatchEvent(errorEvent);

      // Verifica se toast de erro foi criado
      jest.advanceTimersByTime(100);
      const errorToast = document.querySelector('.toast.error');
      expect(errorToast).toBeTruthy();
    });
  });

  describe('Métodos de criação de toast', () => {
    test('success() deve criar toast de sucesso', () => {
      const toastId = toastManager.success('Operação realizada com sucesso!');
      
      expect(toastId).toBeTruthy();
      expect(toastManager.toasts.has(toastId)).toBe(true);
      
      const toastElement = document.querySelector('.toast.success');
      expect(toastElement).toBeTruthy();
      expect(toastElement.textContent).toContain('Operação realizada com sucesso!');
    });

    test('error() deve criar toast de erro', () => {
      const toastId = toastManager.error('Erro na operação');
      
      expect(toastId).toBeTruthy();
      expect(toastManager.toasts.has(toastId)).toBe(true);
      
      const toastElement = document.querySelector('.toast.error');
      expect(toastElement).toBeTruthy();
      expect(toastElement.textContent).toContain('Erro na operação');
    });

    test('warning() deve criar toast de aviso', () => {
      const toastId = toastManager.warning('Atenção necessária');
      
      expect(toastId).toBeTruthy();
      expect(toastManager.toasts.has(toastId)).toBe(true);
      
      const toastElement = document.querySelector('.toast.warning');
      expect(toastElement).toBeTruthy();
      expect(toastElement.textContent).toContain('Atenção necessária');
    });

    test('info() deve criar toast de informação', () => {
      const toastId = toastManager.info('Informação importante');
      
      expect(toastId).toBeTruthy();
      expect(toastManager.toasts.has(toastId)).toBe(true);
      
      const toastElement = document.querySelector('.toast.info');
      expect(toastElement).toBeTruthy();
      expect(toastElement.textContent).toContain('Informação importante');
    });
  });

  describe('Configurações personalizadas', () => {
    test('deve aceitar título personalizado', () => {
      const toastId = toastManager.success('Mensagem', { title: 'Título Personalizado' });
      
      const toastElement = document.querySelector('.toast.success');
      expect(toastElement.textContent).toContain('Título Personalizado');
    });

    test('deve aceitar duração personalizada', () => {
      const customDuration = 3000;
      const toastId = toastManager.info('Teste', { duration: customDuration });
      
      const toast = toastManager.toasts.get(toastId);
      expect(toast.remainingTime).toBe(customDuration);
    });

    test('deve criar toast sem botão de fechar quando closable=false', () => {
      toastManager.success('Teste', { closable: false });
      
      const closeButton = document.querySelector('.toast-close');
      expect(closeButton).toBeFalsy();
    });

    test('deve criar toast sem barra de progresso quando progress=false', () => {
      toastManager.success('Teste', { progress: false });
      
      const progressBar = document.querySelector('.toast-progress');
      expect(progressBar).toBeFalsy();
    });
  });

  describe('Gerenciamento de toasts', () => {
    test('deve limitar número máximo de toasts', () => {
      // Cria mais toasts que o limite
      for (let i = 0; i < 7; i++) {
        toastManager.success(`Toast ${i}`);
      }
      
      // Deve ter apenas o máximo permitido
      expect(toastManager.toasts.size).toBe(toastManager.maxToasts);
      expect(document.querySelectorAll('.toast').length).toBe(toastManager.maxToasts);
    });

    test('deve remover toast mais antigo quando exceder limite', () => {
      // Cria toasts até o limite
      const firstToastId = toastManager.success('Primeiro toast');
      for (let i = 1; i < toastManager.maxToasts; i++) {
        toastManager.success(`Toast ${i}`);
      }
      
      // Adiciona mais um toast
      toastManager.success('Último toast');
      
      // O primeiro toast deve ter sido removido
      expect(toastManager.toasts.has(firstToastId)).toBe(false);
    });

    test('hide() deve remover toast específico', () => {
      const toastId = toastManager.success('Teste');
      
      expect(toastManager.toasts.has(toastId)).toBe(true);
      
      toastManager.hide(toastId);
      
      expect(toastManager.toasts.has(toastId)).toBe(false);
    });

    test('hideAll() deve remover todos os toasts', () => {
      toastManager.success('Toast 1');
      toastManager.error('Toast 2');
      toastManager.warning('Toast 3');
      
      expect(toastManager.toasts.size).toBe(3);
      
      toastManager.hideAll();
      
      expect(toastManager.toasts.size).toBe(0);
    });
  });

  describe('Auto-hide e timers', () => {
    test('deve auto-esconder toast após duração especificada', () => {
      const duration = 1000;
      const toastId = toastManager.success('Teste', { duration });
      
      expect(toastManager.toasts.has(toastId)).toBe(true);
      
      // Avança o tempo
      jest.advanceTimersByTime(duration + 100);
      
      expect(toastManager.toasts.has(toastId)).toBe(false);
    });

    test('não deve auto-esconder quando duration=0', () => {
      const toastId = toastManager.success('Teste', { duration: 0 });
      
      // Avança muito tempo
      jest.advanceTimersByTime(10000);
      
      expect(toastManager.toasts.has(toastId)).toBe(true);
    });

    test('deve pausar timer no hover', () => {
      const duration = 2000;
      const toastId = toastManager.success('Teste', { duration });
      
      const toastElement = document.querySelector('.toast.success');
      
      // Simula hover
      toastElement.dispatchEvent(new Event('mouseenter'));
      
      // Avança o tempo além da duração
      jest.advanceTimersByTime(duration + 500);
      
      // Toast ainda deve existir (pausado)
      expect(toastManager.toasts.has(toastId)).toBe(true);
    });

    test('deve retomar timer ao sair do hover', () => {
      const duration = 1000;
      const toastId = toastManager.success('Teste', { duration });
      
      const toastElement = document.querySelector('.toast.success');
      
      // Simula hover e depois sair do hover
      toastElement.dispatchEvent(new Event('mouseenter'));
      toastElement.dispatchEvent(new Event('mouseleave'));
      
      // Avança o tempo
      jest.advanceTimersByTime(duration + 100);
      
      expect(toastManager.toasts.has(toastId)).toBe(false);
    });
  });

  describe('Interações do usuário', () => {
    test('deve fechar toast ao clicar no botão de fechar', () => {
      const toastId = toastManager.success('Teste');
      
      const closeButton = document.querySelector('.toast-close');
      expect(closeButton).toBeTruthy();
      
      closeButton.click();
      
      expect(toastManager.toasts.has(toastId)).toBe(false);
    });

    test('deve processar cliques em elementos com data-toast-click', () => {
      // Cria botão com atributos de toast
      const button = document.createElement('button');
      button.setAttribute('data-toast-click', 'true');
      button.setAttribute('data-toast-type', 'success');
      button.setAttribute('data-toast-message', 'Mensagem do botão');
      button.setAttribute('data-toast-title', 'Título do botão');
      document.body.appendChild(button);
      
      // Simula clique
      button.click();
      
      // Verifica se toast foi criado
      const toast = document.querySelector('.toast.success');
      expect(toast).toBeTruthy();
      expect(toast.textContent).toContain('Mensagem do botão');
      expect(toast.textContent).toContain('Título do botão');
    });
  });

  describe('Barra de progresso', () => {
    test('deve criar barra de progresso quando progress=true', () => {
      toastManager.success('Teste', { progress: true });
      
      const progressBar = document.querySelector('.toast-progress');
      expect(progressBar).toBeTruthy();
      
      const progressBarInner = document.querySelector('.toast-progress-bar');
      expect(progressBarInner).toBeTruthy();
    });

    test('deve animar barra de progresso', () => {
      const duration = 2000;
      toastManager.success('Teste', { duration, progress: true });
      
      const progressBar = document.querySelector('.toast-progress-bar');
      
      // Verifica se a animação foi configurada
      jest.advanceTimersByTime(100);
      expect(progressBar.style.transition).toContain('transform');
    });
  });

  describe('Escape de HTML', () => {
    test('deve escapar caracteres HTML na mensagem', () => {
      const maliciousMessage = '<script>alert("xss")</script>';
      toastManager.success(maliciousMessage);
      
      const toastElement = document.querySelector('.toast.success');
      expect(toastElement.innerHTML).not.toContain('<script>');
      expect(toastElement.textContent).toContain('<script>alert("xss")</script>');
    });

    test('deve escapar caracteres HTML no título', () => {
      const maliciousTitle = '<img src=x onerror=alert(1)>';
      toastManager.success('Mensagem', { title: maliciousTitle });
      
      const toastElement = document.querySelector('.toast.success');
      expect(toastElement.innerHTML).not.toContain('<img');
      expect(toastElement.textContent).toContain('<img src=x onerror=alert(1)>');
    });
  });

  describe('Acessibilidade', () => {
    test('deve ter atributos ARIA corretos', () => {
      toastManager.success('Teste');
      
      const toastElement = document.querySelector('.toast.success');
      expect(toastElement.getAttribute('role')).toBe('alert');
      expect(toastElement.getAttribute('aria-live')).toBe('assertive');
    });

    test('botão de fechar deve ter aria-label', () => {
      toastManager.success('Teste');
      
      const closeButton = document.querySelector('.toast-close');
      expect(closeButton.getAttribute('aria-label')).toBe('Fechar');
    });

    test('container deve ter atributos ARIA corretos', () => {
      const container = document.querySelector('.toast-container');
      expect(container.getAttribute('aria-live')).toBe('polite');
      expect(container.getAttribute('aria-atomic')).toBe('false');
    });
  });

  describe('Integração com formulários', () => {
    test('deve processar formulários com data-toast', () => {
      // Cria formulário com atributos de toast
      const form = document.createElement('form');
      form.setAttribute('data-toast', 'true');
      form.setAttribute('data-toast-success', 'Formulário enviado!');
      document.body.appendChild(form);
      
      // Simula processamento do formulário
      toastManager.handleFormSubmission(form, 'Formulário enviado!');
      
      // Verifica se toast foi criado
      const toast = document.querySelector('.toast.success');
      expect(toast).toBeTruthy();
      expect(toast.textContent).toContain('Formulário enviado!');
    });
  });

  describe('Mensagens flash do servidor', () => {
    test('deve processar mensagens flash no DOMContentLoaded', () => {
      // Cria elemento com mensagem flash
      const flashElement = document.createElement('div');
      flashElement.setAttribute('data-flash-message', 'Mensagem do servidor');
      flashElement.setAttribute('data-flash-type', 'success');
      document.body.appendChild(flashElement);
      
      // Simula DOMContentLoaded
      document.dispatchEvent(new Event('DOMContentLoaded'));
      
      // Verifica se toast foi criado
      jest.advanceTimersByTime(100);
      const toast = document.querySelector('.toast.success');
      expect(toast).toBeTruthy();
      expect(toast.textContent).toContain('Mensagem do servidor');
      
      // Verifica se elemento flash foi removido
      expect(document.querySelector('[data-flash-message]')).toBeFalsy();
    });
  });

  describe('Geração de IDs', () => {
    test('deve gerar IDs únicos para cada toast', () => {
      const id1 = toastManager.success('Toast 1');
      const id2 = toastManager.success('Toast 2');
      const id3 = toastManager.success('Toast 3');
      
      expect(id1).not.toBe(id2);
      expect(id2).not.toBe(id3);
      expect(id1).not.toBe(id3);
    });

    test('IDs devem ser strings não vazias', () => {
      const id = toastManager.success('Teste');
      
      expect(typeof id).toBe('string');
      expect(id.length).toBeGreaterThan(0);
    });
  });

  describe('Títulos padrão', () => {
    test('deve usar títulos padrão corretos para cada tipo', () => {
      toastManager.success('Teste');
      toastManager.error('Teste');
      toastManager.warning('Teste');
      toastManager.info('Teste');
      
      expect(document.querySelector('.toast.success').textContent).toContain('Sucesso');
      expect(document.querySelector('.toast.error').textContent).toContain('Erro');
      expect(document.querySelector('.toast.warning').textContent).toContain('Atenção');
      expect(document.querySelector('.toast.info').textContent).toContain('Informação');
    });
  });

  describe('Performance e limpeza', () => {
    test('deve limpar timers ao esconder toast', () => {
      const toastId = toastManager.success('Teste', { duration: 5000 });
      const toast = toastManager.toasts.get(toastId);
      
      expect(toast.timer).toBeTruthy();
      
      toastManager.hide(toastId);
      
      // Timer deve ter sido limpo
      expect(toast.timer).toBeFalsy();
    });

    test('deve remover elementos do DOM ao esconder', () => {
      const toastId = toastManager.success('Teste');
      
      expect(document.querySelector('.toast.success')).toBeTruthy();
      
      toastManager.hide(toastId);
      
      // Aguarda animação
      jest.advanceTimersByTime(500);
      
      expect(document.querySelector('.toast.success')).toBeFalsy();
    });
  });

  describe('Casos extremos', () => {
    test('deve lidar com mensagens muito longas', () => {
      const longMessage = 'A'.repeat(1000);
      const toastId = toastManager.success(longMessage);
      
      const toastElement = document.querySelector('.toast.success');
      expect(toastElement.textContent).toContain(longMessage);
    });

    test('deve lidar com mensagens vazias', () => {
      const toastId = toastManager.success('');
      
      expect(toastManager.toasts.has(toastId)).toBe(true);
      expect(document.querySelector('.toast.success')).toBeTruthy();
    });

    test('deve lidar com mensagens null/undefined', () => {
      expect(() => {
        toastManager.success(null);
        toastManager.success(undefined);
      }).not.toThrow();
    });

    test('deve lidar com opções inválidas', () => {
      expect(() => {
        toastManager.success('Teste', null);
        toastManager.success('Teste', 'string inválida');
        toastManager.success('Teste', { duration: 'não é número' });
      }).not.toThrow();
    });
  });
});