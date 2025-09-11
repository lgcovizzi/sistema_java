# Regras do Projeto - Sistema Java

## Estrutura do Projeto

### Arquitetura Geral
- **Backend**: Spring Boot (Java) em container Docker
- **Frontend**: Landing page responsiva similar ao site do Sinditest
- **Banco de Dados**: PostgreSQL em container
- **Cache**: Redis em container
- **Email Testing**: MailHog para desenvolvimento

### Estrutura de Diretórios
```
sistema_java/
├── docker-compose.yml
├── backend/
│   ├── Dockerfile
│   ├── src/
│   ├── pom.xml
│   └── ...
├── frontend/
│   ├── index.html
│   ├── css/
│   ├── js/
│   └── assets/
└── .trae/
    └── rules/
        └── project_rules.md
```

## Configurações Docker

### Serviços Obrigatórios
1. **PostgreSQL**
   - Porta: 5432
   - Database: sistema_java
   - Username: postgres
   - Password: postgres123
   - Versão: 15-alpine
   - Encoding: UTF-8
   - Timezone: America/Sao_Paulo

2. **Redis**
   - Porta: 6379
   - Sem autenticação para desenvolvimento

3. **MailHog**
   - SMTP: porta 1025
   - Web UI: porta 8025

4. **Backend Spring Boot**
   - Porta: 8080
   - Profile: development
   - Conecta com PostgreSQL e Redis

## Padrões de Desenvolvimento

### Backend (Spring Boot)
- **Versão Java**: 17 ou superior
- **Spring Boot**: 3.x
- **JSF**: 4.0 com PrimeFaces 13.x
- **Dependências obrigatórias**:
  - Spring Web
  - Spring Data JPA
  - Spring Data Redis
  - PostgreSQL Driver
  - Spring Boot DevTools
  - Spring Boot Actuator
  - Flyway Core (para migrations)
  - Spring Boot Validation
  - JSF (MyFaces)
  - PrimeFaces
  - PrimeFaces Extensions
  - Omnifaces

### Estrutura do Backend
```
src/main/java/
├── config/
├── controller/
├── service/
├── repository/
├── model/
│   ├── entity/     # Entidades JPA
│   ├── dto/        # Data Transfer Objects
│   └── enums/      # Enumerações
└── Application.java
```

### Padrões para Entidades JPA
- Usar anotações JPA adequadas (@Entity, @Table, @Column)
- Implementar equals() e hashCode() baseados no ID
- Usar @CreationTimestamp e @UpdateTimestamp para auditoria
- Aplicar validações Bean Validation (@NotNull, @Size, etc.)
- Usar relacionamentos lazy por padrão
- Implementar construtores padrão e com parâmetros

### Frontend
- **Framework**: JSF 4.0 com PrimeFaces 13.x
- **Estilo**: Responsivo, similar ao design do Sinditest
- **Cores principais**: Azul (#1e3a8a), Branco (#ffffff)
- **Tema PrimeFaces**: Bootstrap ou Saga
- **Funcionalidades**:
  - Páginas JSF com componentes PrimeFaces
  - Header com navegação
  - Seção de notícias/comunicados
  - Footer com informações de contato
  - Design responsivo para mobile
  - Componentes ricos (DataTable, Charts, etc.)

## Banco de Dados Relacional

### Modelo de Dados
O sistema utiliza PostgreSQL como banco de dados principal com as seguintes entidades:

#### Tabelas Principais

**1. usuarios**
- ID (chave primária)
- Nome (obrigatório, até 100 caracteres)
- Email (único, obrigatório, até 150 caracteres)
- Senha (obrigatória, criptografada)
- Ativo (boolean, padrão true)
- Data de criação e atualização (timestamps automáticos)

**2. noticias**
- ID (chave primária)
- Título (obrigatório, até 200 caracteres)
- Conteúdo (texto longo, obrigatório)
- Resumo (até 500 caracteres)
- Autor (referência para usuários)
- Publicada (boolean, padrão false)
- Data de publicação, criação e atualização

**3. categorias**
- ID (chave primária)
- Nome (único, obrigatório, até 100 caracteres)
- Descrição (texto)
- Ativa (boolean, padrão true)
- Data de criação

**4. noticia_categorias**
- Tabela de relacionamento muitos-para-muitos
- Chave composta (noticia_id, categoria_id)
- Cascade delete para notícias

**5. comentarios**
- ID (chave primária)
- Conteúdo (texto, obrigatório)
- Autor (referência para usuários)
- Notícia (referência, cascade delete)
- Aprovado (boolean, padrão false)
- Data de criação

### Índices Recomendados
- Email dos usuários (para login rápido)
- Status de publicação das notícias
- Data de publicação das notícias (ordem decrescente)
- Autor das notícias
- Notícia dos comentários
- Status de aprovação dos comentários

### Configurações JPA
- Usar validação de schema em produção (validate)
- Permitir atualizações automáticas apenas em desenvolvimento
- Configurar dialeto PostgreSQL
- Otimizar batch processing (tamanho 20)
- Ordenar inserções e atualizações para performance
- Usar estratégia de nomenclatura padrão

### Migrations com Flyway
Utilizar Flyway para versionamento do banco:
```
src/main/resources/db/migration/
├── V1__Create_usuarios_table.sql
├── V2__Create_noticias_table.sql
├── V3__Create_categorias_table.sql
├── V4__Create_noticia_categorias_table.sql
├── V5__Create_comentarios_table.sql
└── V6__Insert_initial_data.sql
```

### Backup e Manutenção
- Realizar backup diário do banco de dados usando pg_dump
- Nomear backups com timestamp (formato: backup_YYYYMMDD_HHMMSS.sql)
- Manter procedimento de restauração documentado
- Monitorar conexões ativas regularmente
- Verificar integridade dos dados periodicamente

### Configurações de Performance
- Pool máximo de 20 conexões
- Mínimo de 5 conexões idle
- Timeout de idle: 5 minutos
- Tempo de vida máximo: 20 minutos
- Timeout de conexão: 20 segundos
- Usar HikariCP como pool de conexões

## Configurações de Ambiente

### Variáveis de Ambiente
```
# Database
DB_HOST=postgres
DB_PORT=5432
DB_NAME=sistema_java
DB_USER=postgres
DB_PASSWORD=postgres123

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# Mail
MAIL_HOST=mailhog
MAIL_PORT=1025
```

### Portas Utilizadas
- 8080: Backend Spring Boot
- 5432: PostgreSQL
- 6379: Redis
- 8025: MailHog Web UI
- 1025: MailHog SMTP
- 3000: Frontend (se usar servidor de desenvolvimento)

## Comandos Úteis

### Inicialização
- Subir todos os serviços em modo detached
- Verificar logs em tempo real
- Parar todos os serviços quando necessário

### Desenvolvimento
- Rebuild do backend quando houver mudanças
- Acessar container do backend para debugging
- Conectar diretamente ao PostgreSQL para consultas
- Monitorar logs de serviços específicos

## URLs de Acesso
- Backend API: http://localhost:8080
- MailHog Web UI: http://localhost:8025
- Frontend: http://localhost:3000 (ou servir estaticamente)
- PostgreSQL: localhost:5432
- Redis: localhost:6379

## Padrões de Código

### Java/Spring Boot
- Usar anotações Spring adequadas
- Implementar tratamento de exceções
- Configurar CORS para desenvolvimento
- Usar DTOs para transferência de dados
- Implementar validação de entrada

### Frontend (JSF + PrimeFaces)
- Usar componentes PrimeFaces sempre que possível
- Estrutura de páginas JSF com templates
- Managed Beans com anotações CDI
- Validação JSF integrada
- Temas PrimeFaces responsivos
- Otimização para SEO básico
- Acessibilidade (ARIA labels)

### Estrutura JSF
```
src/main/webapp/
├── WEB-INF/
│   ├── web.xml
│   ├── faces-config.xml
│   └── templates/
├── resources/
│   ├── css/
│   ├── js/
│   └── images/
└── pages/
    ├── index.xhtml
    ├── noticias.xhtml
    └── admin/
```

## Segurança
- Senhas em variáveis de ambiente
- CORS configurado adequadamente
- Validação de entrada no backend
- Headers de segurança configurados

## Testes e Qualidade de Código

### Testes Unitários
- **Cobertura mínima**: 80% do código
- **Framework**: JUnit 5 + Mockito
- **Testes de integração**: TestContainers para PostgreSQL e Redis
- **Estrutura de testes**: Espelhar estrutura do código principal

### Dependências de Teste
- JUnit 5 (jupiter)
- Mockito Core
- Spring Boot Test Starter
- TestContainers (PostgreSQL, Redis)
- AssertJ (assertions fluentes)
- WireMock (mocks de APIs externas)

### Padrões de Teste
- **Nomenclatura**: NomeClasseTest para classes de teste
- **Métodos**: should_ReturnExpected_When_Condition
- **Arrange-Act-Assert**: Estrutura clara em todos os testes
- **Given-When-Then**: Para testes BDD quando apropriado

### Cobertura por Camada
- **Entities**: 90% (getters, setters, equals, hashCode)
- **Repositories**: 85% (queries customizadas)
- **Services**: 90% (lógica de negócio)
- **Controllers**: 80% (endpoints e validações)
- **Configurations**: 70% (beans e configurações)

### Ferramentas de Cobertura
- **JaCoCo**: Plugin Maven para relatórios
- **SonarQube**: Análise de qualidade (opcional)
- **Relatórios**: HTML e XML para CI/CD
- **Exclusões**: DTOs simples, constantes, configurações básicas

### Testes de Integração
- **TestContainers**: Containers reais para PostgreSQL e Redis
- **@SpringBootTest**: Testes de contexto completo
- **@DataJpaTest**: Testes específicos de repositório
- **@WebMvcTest**: Testes de controllers isolados

### Mocks e Stubs
- **@MockBean**: Para dependências Spring
- **@Mock**: Para objetos simples
- **@InjectMocks**: Para classes sob teste
- **Verificações**: Interações com mocks quando necessário

### Testes de Performance
- **@Test(timeout)**: Para métodos críticos
- **Profiling**: Identificar gargalos
- **Load testing**: JMeter para endpoints críticos
- **Métricas**: Tempo de resposta e throughput

### Estrutura de Testes
```
src/test/java/
├── unit/
│   ├── service/
│   ├── repository/
│   └── controller/
├── integration/
│   ├── database/
│   └── api/
└── resources/
    ├── application-test.yml
    └── test-data/
```

### Configuração de Teste
- **Profile**: test (application-test.yml)
- **H2**: Para testes unitários rápidos
- **TestContainers**: Para testes de integração
- **Dados de teste**: Fixtures e builders

### Métricas de Qualidade
- **Cobertura de linha**: Mínimo 80%
- **Cobertura de branch**: Mínimo 70%
- **Complexidade ciclomática**: Máximo 10 por método
- **Duplicação de código**: Máximo 3%

### CI/CD e Testes
- **Pipeline**: Executar todos os testes
- **Falha rápida**: Parar build se cobertura < 80%
- **Relatórios**: Publicar resultados de cobertura
- **Notificações**: Alertas para queda de cobertura

## Monitoramento
- Spring Boot Actuator habilitado
- Health checks configurados
- Logs estruturados
- Métricas básicas disponíveis