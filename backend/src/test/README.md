# Estrutura de Testes - Sistema Java

## Visão Geral

Este projeto implementa uma estrutura completa de testes seguindo as melhores práticas de TDD (Test-Driven Development) e cobertura de código.

## Estrutura de Diretórios

```
src/test/
├── java/
│   └── com/
│       └── sistema/
│           ├── controller/          # Testes unitários dos controladores
│           ├── config/              # Testes de configuração
│           ├── integration/         # Testes de integração
│           └── performance/         # Testes de performance
└── resources/
    ├── application-test.yml         # Configurações para testes
    └── logback-test.xml            # Configuração de logs para testes
```

## Tipos de Testes Implementados

### 1. Testes Unitários

**Localização**: `src/test/java/com/sistema/controller/` e `src/test/java/com/sistema/config/`

**Características**:
- Testam uma única unidade de código isoladamente
- Usam mocks para dependências externas
- Execução rápida (< 100ms por teste)
- Seguem padrão Given-When-Then

**Arquivos**:
- `HealthControllerTest.java`: Testes do controlador de health check
- `HomeControllerTest.java`: Testes do controlador home
- `RedisConfigTest.java`: Testes da configuração Redis
- `RSAKeyManagerTest.java`: Testes do gerenciador de chaves RSA

### 2. Testes de Integração

**Localização**: `src/test/java/com/sistema/integration/`

**Características**:
- Testam a integração entre componentes
- Usam Testcontainers para PostgreSQL e Redis
- Testam endpoints reais da aplicação
- Verificam comportamento end-to-end

**Arquivos**:
- `HealthControllerIntegrationTest.java`: Testes de integração completos

### 3. Testes de Performance

**Localização**: `src/test/java/com/sistema/performance/`

**Características**:
- Verificam tempos de resposta
- Testam comportamento sob carga
- Validam performance de endpoints críticos

**Arquivos**:
- `HealthControllerPerformanceTest.java`: Testes de performance dos endpoints

## Tecnologias Utilizadas

### Frameworks de Teste
- **JUnit 5**: Framework principal de testes
- **Mockito**: Framework para mocks e stubs
- **AssertJ**: Biblioteca de assertions fluentes
- **Spring Boot Test**: Integração com Spring Boot
- **Testcontainers**: Containers Docker para testes de integração

### Cobertura de Código
- **JaCoCo**: Plugin Maven para cobertura de código
- **Relatórios HTML**: Gerados em `target/site/jacoco/`

## Como Executar os Testes

### Executar Todos os Testes
```bash
mvn test
```

### Executar Testes com Cobertura
```bash
mvn clean test jacoco:report
```

### Executar Testes Específicos
```bash
# Apenas testes unitários
mvn test -Dtest="*Test"

# Apenas testes de integração
mvn test -Dtest="*IntegrationTest"

# Apenas testes de performance
mvn test -Dtest="*PerformanceTest"

# Teste específico
mvn test -Dtest="HealthControllerTest"
```

### Executar Testes em Modo Debug
```bash
mvn test -Dmaven.surefire.debug
```

## Relatórios de Cobertura

### Visualizar Relatório
Após executar `mvn clean test jacoco:report`, abra:
```
target/site/jacoco/index.html
```

### Métricas de Cobertura
- **Instruções**: Cobertura de instruções bytecode
- **Branches**: Cobertura de ramificações (if/else, switch)
- **Linhas**: Cobertura de linhas de código
- **Métodos**: Cobertura de métodos
- **Classes**: Cobertura de classes

### Metas de Cobertura
- **Mínimo**: 70% de cobertura de linhas
- **Ideal**: 85% de cobertura de linhas
- **Crítico**: 95% para classes de configuração

## Configurações de Teste

### application-test.yml
- Configurações específicas para ambiente de teste
- Banco H2 em memória para testes unitários
- Configurações Redis para Testcontainers
- Logs otimizados para testes

### logback-test.xml
- Configuração de logs específica para testes
- Reduz verbosidade durante execução
- Mantém logs importantes para debugging

## Boas Práticas Implementadas

### Nomenclatura
- Métodos de teste descritivos em português
- Padrão: `should[ExpectedBehavior]When[StateUnderTest]()`
- Uso de `@DisplayName` para descrições claras

### Estrutura dos Testes
- **Given**: Preparação do cenário
- **When**: Execução da ação
- **Then**: Verificação dos resultados

### Mocks e Stubs
- Uso de `@Mock` para dependências
- `@InjectMocks` para classe sob teste
- Configuração específica por teste quando necessário

### Assertions
- Uso de AssertJ para assertions fluentes
- Mensagens descritivas em caso de falha
- Verificação de múltiplos aspectos quando relevante

## Integração Contínua

### Pipeline de Testes
1. **Compilação**: Verificação de sintaxe
2. **Testes Unitários**: Execução rápida
3. **Testes de Integração**: Verificação com containers
4. **Cobertura**: Geração de relatórios
5. **Qualidade**: Verificação de métricas

### Critérios de Qualidade
- Todos os testes devem passar
- Cobertura mínima de 70%
- Sem warnings de compilação
- Tempo de execução < 2 minutos

## Troubleshooting

### Problemas Comuns

**Testes de Integração Falham**:
- Verificar se Docker está rodando
- Verificar portas disponíveis
- Limpar containers: `docker system prune`

**Cobertura Baixa**:
- Verificar se todos os testes estão executando
- Adicionar testes para código não coberto
- Verificar configuração do JaCoCo

**Testes Lentos**:
- Verificar uso de mocks em testes unitários
- Otimizar configuração de Testcontainers
- Paralelizar execução quando possível

### Comandos Úteis

```bash
# Limpar e recompilar
mvn clean compile

# Executar apenas compilação de testes
mvn test-compile

# Pular testes durante build
mvn install -DskipTests

# Executar com logs detalhados
mvn test -X

# Executar testes em paralelo
mvn test -T 4
```

## Próximos Passos

### Melhorias Planejadas
- [ ] Testes de mutação com PiTest
- [ ] Testes de contrato com Spring Cloud Contract
- [ ] Testes de segurança automatizados
- [ ] Integração com SonarQube
- [ ] Testes de carga com JMeter

### Expansão da Cobertura
- [ ] Testes para camada de serviço
- [ ] Testes para repositórios
- [ ] Testes para entidades
- [ ] Testes para componentes de segurança

---

**Nota**: Esta estrutura de testes segue as melhores práticas de TDD e garante alta qualidade do código através de cobertura abrangente e testes automatizados.