module.exports = {
  // Ambiente de teste
  testEnvironment: 'jsdom',
  
  // Padrões de arquivos de teste
  testMatch: [
    '**/__tests__/**/*.js',
    '**/?(*.)+(spec|test).js'
  ],
  
  // Arquivos de setup
  setupFilesAfterEnv: ['<rootDir>/setup.js'],
  
  // Cobertura de código
  collectCoverage: true,
  collectCoverageFrom: [
    '../../../main/resources/static/js/**/*.js',
    '!**/node_modules/**',
    '!**/vendor/**'
  ],
  coverageDirectory: 'coverage',
  coverageReporters: ['text', 'lcov', 'html'],
  
  // Thresholds de cobertura
  coverageThreshold: {
    global: {
      branches: 80,
      functions: 80,
      lines: 80,
      statements: 80
    }
  },
  
  // Transformações
  transform: {
    '^.+\\.js$': 'babel-jest'
  },
  
  // Módulos mock
  moduleNameMapping: {
    '^@/(.*)$': '<rootDir>/../../../main/resources/static/js/$1'
  },
  
  // Configurações de timeout
  testTimeout: 10000,
  
  // Configurações de verbose
  verbose: true,
  
  // Configurações de cache
  clearMocks: true,
  restoreMocks: true,
  
  // Configurações de relatórios
  reporters: [
    'default',
    ['jest-html-reporters', {
      publicPath: './coverage',
      filename: 'test-report.html',
      expand: true
    }]
  ],
  
  // Configurações de watch
  watchPathIgnorePatterns: [
    '<rootDir>/node_modules/',
    '<rootDir>/coverage/'
  ],
  
  // Configurações de globals
  globals: {
    'window': {},
    'document': {},
    'navigator': {
      userAgent: 'node.js'
    }
  }
};