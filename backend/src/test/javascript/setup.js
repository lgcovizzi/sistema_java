/**
 * Setup para testes Jest
 * Configurações globais para testes JavaScript do sistema de toast
 */

// Importa matchers customizados do testing-library
import '@testing-library/jest-dom';

// Mock do console para evitar logs desnecessários nos testes
global.console = {
  ...console,
  // Desabilita logs durante os testes, mas mantém errors e warns
  log: jest.fn(),
  debug: jest.fn(),
  info: jest.fn(),
  warn: console.warn,
  error: console.error,
};

// Mock do requestAnimationFrame para testes
global.requestAnimationFrame = (callback) => {
  return setTimeout(callback, 0);
};

global.cancelAnimationFrame = (id) => {
  clearTimeout(id);
};

// Mock do getComputedStyle
global.getComputedStyle = (element) => {
  return {
    getPropertyValue: (prop) => '',
    width: '100px',
    height: '50px',
  };
};

// Mock do getBoundingClientRect
Element.prototype.getBoundingClientRect = jest.fn(() => ({
  width: 100,
  height: 50,
  top: 0,
  left: 0,
  bottom: 50,
  right: 100,
  x: 0,
  y: 0,
  toJSON: jest.fn(),
}));

// Mock do localStorage
const localStorageMock = {
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
  clear: jest.fn(),
};
global.localStorage = localStorageMock;

// Mock do sessionStorage
const sessionStorageMock = {
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
  clear: jest.fn(),
};
global.sessionStorage = sessionStorageMock;

// Configuração global para timeouts de teste
jest.setTimeout(10000);

// Limpa todos os mocks após cada teste
afterEach(() => {
  jest.clearAllMocks();
  jest.clearAllTimers();
  
  // Limpa o DOM
  document.body.innerHTML = '';
  document.head.innerHTML = '';
});

// Configuração de timers para testes
beforeEach(() => {
  jest.useFakeTimers();
});

afterEach(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
});