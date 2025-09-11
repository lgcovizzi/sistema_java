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


## Banco de Dados Relacional

### Modelo de Dados
O sistema utiliza PostgreSQL como banco de dados principal com as seguintes entidades:

#### Tabelas Principais

**1. usuarios**
- ID (chave primária)
- Nome (obrigatório, até 100 caracteres)
- Sobrenome (obrigatório, até 100 caracteres)
- CPF (único, obrigatório, 11 caracteres, validado)
- Email (único, obrigatório, até 150 caracteres, validado com regex)
- Senha (obrigatória, criptografada)
- Telefone (opcional, até 20 caracteres)
- Data de nascimento (opcional, formato DATE)
- Avatar (opcional, URL/arquivo armazenado em diretório configurado)
- Ativo (boolean, padrão true)
- Papel/role (enum: ADMINISTRADOR, FUNDADOR, COLABORADOR, ASSOCIADO, USUARIO, CONVIDADO)
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
- CPF dos usuários (único)
- Papel/Role dos usuários (para controle de acesso)
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

### Documentação e Comentários
- **Comentários Obrigatórios**: Todas as funções, métodos, classes e componentes devem incluir comentários que referenciem as regras específicas do projeto implementadas
- **Formato de Referência**: Usar o formato "Referência: [Nome da Regra] - project_rules.md" nos comentários
- **Exemplos de Referência**:
  - "Referência: Sistema de Temas Claros e Escuros - project_rules.md"
  - "Referência: Controle de Acesso - project_rules.md"
  - "Referência: Padrões para Entidades JPA - project_rules.md"
- **Localização dos Comentários**:
  - Classes: Javadoc da classe
  - Métodos: Javadoc do método
  - Funções JavaScript: Comentário de linha acima da função
  - Componentes JSF: Comentários HTML quando aplicável
- **Rastreabilidade**: Facilitar a identificação de quais regras foram implementadas em cada parte do código

### Java/Spring Boot
- Usar anotações Spring adequadas
- Implementar tratamento de exceções
- Configurar CORS para desenvolvimento
- Usar DTOs para transferência de dados
- Implementar validação de entrada

### Frontend (JSF + PrimeFaces)
- Usar componentes PrimeFaces sempre que possível
- **Tema PrimeFaces**: Bootstrap ou Saga
- **Temas Claros e Escuros**: Implementar sistema de alternância entre temas
- Estrutura de páginas JSF com templates
- Managed Beans com anotações CDI
- Validação JSF integrada
- Temas PrimeFaces responsivos
- Otimização para SEO básico
- Acessibilidade (ARIA labels)
- Sistema de temas claros e escuros implementado

### Funcionalidades JSF
- **Páginas JSF com componentes PrimeFaces**
- **Header com navegação**
- **Seção de notícias/comunicados**
- **Footer com informações de contato**
- **Design responsivo para mobile**
- **Componentes ricos (DataTable, Charts, etc.)**
- **Alternador de tema claro/escuro no header**
- **Detecção automática de preferência do sistema**
- **Persistência da preferência do usuário**

### Estrutura JSF
```
src/main/webapp/
├── WEB-INF/
│   ├── web.xml
│   ├── faces-config.xml
│   └── templates/
├── resources/
│   ├── css/
│   │   ├── themes/
│   │   │   ├── light-theme.css
│   │   │   └── dark-theme.css
│   │   └── custom.css
│   ├── js/
│   │   └── theme-switcher.js
│   └── images/
└── pages/
    ├── index.xhtml
    ├── noticias.xhtml
    └── admin/
```

## Sistema de Temas Claros e Escuros

### Especificações Técnicas

#### Paleta de Cores
**Tema Claro:**
- Background principal: #ffffff
- Background secundário: #f8fafc
- Texto principal: #1f2937
- Texto secundário: #6b7280
- Azul primário: #1e3a8a
- Azul secundário: #3b82f6
- Bordas: #e5e7eb
- Sombras: rgba(0, 0, 0, 0.1)

**Tema Escuro:**
- Background principal: #111827
- Background secundário: #1f2937
- Texto principal: #f9fafb
- Texto secundário: #d1d5db
- Azul primário: #3b82f6
- Azul secundário: #60a5fa
- Bordas: #374151
- Sombras: rgba(0, 0, 0, 0.3)

#### Implementação CSS
- Usar CSS Custom Properties (variáveis CSS) para cores
- Implementar `@media (prefers-color-scheme)` para detecção automática
- Classes CSS `.light-theme` e `.dark-theme` no elemento `<html>`
- Transições suaves entre temas (transition: all 0.3s ease)

#### Funcionalidades JavaScript
- Toggle button no header para alternar temas
- Detecção automática da preferência do sistema operacional
- Persistência da escolha do usuário no localStorage
- Aplicação do tema antes do carregamento completo da página

#### Componentes PrimeFaces
- Adaptar temas PrimeFaces para modo escuro
- Customizar cores de componentes (DataTable, Dialog, etc.)
- Manter consistência visual em todos os componentes

#### Acessibilidade
- Contraste mínimo WCAG AA (4.5:1 para texto normal)
- Ícones e indicadores visuais para o estado do tema
- Suporte a navegação por teclado no toggle
- Aria-labels apropriados para leitores de tela

#### Persistência
- Salvar preferência no localStorage do navegador
- Chave: 'sistema-java-theme' com valores 'light', 'dark', 'auto'
- Aplicar tema imediatamente ao carregar a página
- Sincronizar com mudanças de preferência do sistema

## Segurança
- Senhas em variáveis de ambiente
- CORS configurado adequadamente
- Validação de entrada no backend
- Headers de segurança configurados

## Login e Registro

### Regras de Registro
- Campos obrigatórios: nome, sobrenome, CPF, email e senha
- O CPF deve ter 11 dígitos válidos e será único no sistema
- O email deve ser válido (regex padrão) e único no sistema
- A senha deve ter mínimo de 8 caracteres e seguir requisitos de segurança
- Após registro, o usuário recebe o papel USUARIO (nível padrão)

## Controle de Acesso — Níveis de Usuário e Dashboards

### Papéis definidos:
- **ADMINISTRADOR**: Acesso total ao sistema, gerenciamento completo
- **FUNDADOR**: Acesso administrativo com privilégios especiais, pode editar colaborador, associado, parceiro, 
- **PARCEIRO**: acesso a um dashboard proprio
- **COLABORADOR**: Criação e edição de conteúdo, moderação pode editar associados, e usuários.
- **ASSOCIADO**: Acesso a funcionalidades específicas de associados
- **USUARIO**: Visualização de conteúdo público, comentários
- **CONVIDADO**: Acesso limitado apenas a registro/login e a pagina de notícias abertas ao publico.

### Regras de Dashboards
- CONVIDADO não possui dashboard, apenas acesso a registro/login
- USUARIO e níveis superiores têm dashboard com acesso ao próprio perfil e funcionalidades específicas de cada papel

### Regras de Edição de Perfil (para todos os papéis autenticados)
- Cada usuário pode editar apenas seus próprios dados pessoais
- Campos disponíveis para edição:
  - Nome
  - Sobrenome
  - Telefone (opcional)
  - Data de nascimento (opcional)
  - Avatar (opcional, processado em segundo plano)

### Regras de Avatar
- O upload do arquivo pode ser de qualquer tamanho, sem restrição inicial
- O sistema deve realizar automaticamente:
  - Crop centralizado ou definido pelo usuário
  - Redimensionamento em diferentes tamanhos (64x64, 256x256, 512x512)
  - Processamento em segundo plano para não afetar a experiência do usuário
- Restrições:
  - Formatos aceitos: JPEG, PNG

### Regras de Validação de Perfil
- Telefone deve seguir regex: [0-9\-()+ ]{8,20}
- Data de nascimento: idade mínima de 16 anos
- ADMINISTRADOR pode suspender ou reativar contas de usuários, mas não alterar seus dados pessoais

### Testes de Regras
- Registro deve falhar se email for inválido
- Registro deve falhar se CPF for inválido ou duplicado
- Upload de avatar deve ser processado em segundo plano com crop e redimensionamento

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