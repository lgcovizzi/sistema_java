# Regras do Projeto - Sistema Java

## Visão Geral do Projeto

Sistema Java completo implementado com Spring Boot 3.2.0, utilizando PostgreSQL como banco de dados principal, Redis para cache, e MailHog para testes de email. O projeto está totalmente containerizado com Docker Compose.

## Estrutura Implementada

### Backend Spring Boot
- **Framework**: Spring Boot 3.2.0
- **Java**: 17
- **Dependências principais**:
  - Spring Web
  - Spring Data JPA
  - Spring Data Redis
  - Spring Boot Actuator
  - Spring Boot Starter Thymeleaf
  - PostgreSQL Driver
  - Lettuce (Redis client)

### Controladores REST Implementados

#### 1. HealthController (`/api`)
- **GET /api/health**: Health check da aplicação
- **GET /api/info**: Informações da aplicação
- **GET /api/redis-test**: Teste de conectividade com Redis

#### 2. HomeController (`/`)
- **GET /**: Página inicial servindo template Thymeleaf responsivo
- **GET /api-simple**: Redirecionamento para página principal
- **Configuração**: Integrado com Thymeleaf para servir interface web responsiva

### Configurações Implementadas

#### RedisConfig
- Cache manager configurado com TTL de 10 minutos
- Serialização JSON para valores
- Serialização String para chaves
- Configuração de RedisTemplate

#### ThymeleafConfig
- Template engine integrado ao Spring Boot
- Configuração automática via spring-boot-starter-thymeleaf
- Templates localizados em `/src/main/resources/templates/`
- Suporte a expressões Thymeleaf para dados dinâmicos
- Integração com Model do Spring MVC

#### RSAKeyManager
- Gerenciamento automático de chaves RSA na inicialização da aplicação
- Geração de par de chaves RSA de 2048 bits se não existirem
- Validação de chaves existentes através de teste de criptografia/descriptografia
- Armazenamento seguro em formato PEM
- Configuração do diretório de chaves via `app.rsa.keys.directory`
- Logs detalhados para monitoramento do processo
- Regeneração automática de chaves inválidas ou corrompidas

### Interface Thymeleaf Implementada

#### Template Responsivo
- **Arquivo**: `/src/main/resources/templates/index.html`
- **Características**:
  - Design mobile-first com CSS Grid e Flexbox
  - Variáveis CSS customizadas para consistência visual
  - Breakpoints responsivos adaptativos
  - Gradiente de fundo moderno
  - Cards com sombras e efeitos hover
  - Typography responsiva com clamp()

#### Estrutura da Interface
- **Header**: Título principal com texto responsivo
- **Cards de Informação**:
  - Status da aplicação com indicadores visuais
  - Estatísticas do sistema (uptime, endpoints, serviços)
  - Lista de endpoints da API com links funcionais
  - Tecnologias utilizadas no projeto
- **Footer**: Informações do desenvolvedor e links úteis

#### Integração com Spring Boot
- **HomeController**: Configurado para servir templates Thymeleaf
- **Model Attributes**: Dados dinâmicos passados para o template
  - `appName`: Nome da aplicação
  - `version`: Versão atual do sistema
- **Thymeleaf Expressions**: Uso de `th:text` para dados dinâmicos

#### Design System
- **Paleta de Cores**: Gradiente roxo/azul moderno
- **Tipografia**: Segoe UI com fallbacks
- **Espaçamento**: Sistema consistente com variáveis CSS
- **Responsividade**: Breakpoints para mobile, tablet e desktop
- **Acessibilidade**: Contraste adequado e navegação por teclado

## Docker Compose Configuration

### Arquitetura dos Serviços

O projeto utiliza Docker Compose com os seguintes serviços:

#### 1. Backend Spring Boot
- **Serviço**: `app`
- **Contexto de Build**: `./backend`
- **Dockerfile**: `./backend/Dockerfile`
- **Imagem Base**: `maven:3.9.6-eclipse-temurin-17` (build) + `eclipse-temurin:17-jre-alpine` (runtime)
- **Porta**: 8080:8080
- **Dependências**: postgres, redis
- **Variáveis de ambiente**:
  - `SPRING_PROFILES_ACTIVE=docker`
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/sistema_db`
  - `SPRING_DATASOURCE_USERNAME=sistema_user`
  - `SPRING_DATASOURCE_PASSWORD=sistema_pass`
  - `SPRING_REDIS_HOST=redis`
  - `SPRING_REDIS_PORT=6379`
  - `SPRING_MAIL_HOST=mailhog`
  - `SPRING_MAIL_PORT=1025`

#### 2. PostgreSQL Database
- **Serviço**: `postgres`
- **Imagem**: `postgres:15-alpine`
- **Porta**: 5432:5432
- **Variáveis de ambiente**:
  - `POSTGRES_DB=sistema_db`
  - `POSTGRES_USER=sistema_user`
  - `POSTGRES_PASSWORD=sistema_pass`
- **Volume**: `postgres_data:/var/lib/postgresql/data`

#### 3. Redis Cache
- **Serviço**: `redis`
- **Imagem**: `redis:7-alpine`
- **Porta**: 6379:6379
- **Comando**: `redis-server --appendonly yes`
- **Volume**: `redis_data:/data`

#### 4. MailHog (Email Testing)
- **Serviço**: `mailhog`
- **Imagem**: `mailhog/mailhog:latest`
- **Portas**: 
  - `1025:1025` (SMTP)
  - `8025:8025` (Web UI)

### Networks
- **Rede**: `sistema-network`
- **Driver**: bridge
- Todos os serviços devem estar na mesma rede

### Volumes
- `postgres_data`: Para persistência do PostgreSQL
- `redis_data`: Para persistência do Redis

### Ordem de Inicialização
1. postgres
2. redis
3. mailhog
4. app (dependente dos demais)

### Configurações Adicionais
- Usar `restart: unless-stopped` para todos os serviços
- Configurar healthchecks para postgres e redis
- Usar variáveis de ambiente para configurações sensíveis
- Implementar wait-for-it ou dockerize para aguardar dependências

### Dockerfile Implementado

#### Multi-stage Build
- **Stage 1 (build)**: `maven:3.9.6-eclipse-temurin-17`
  - Copia `pom.xml` e baixa dependências
  - Copia código fonte e compila aplicação
  - Gera JAR executável
- **Stage 2 (runtime)**: `eclipse-temurin:17-jre-alpine`
  - Copia apenas o JAR da aplicação
  - Configuração otimizada para produção
  - Imagem final menor e mais segura

### Maven Wrapper
- **mvnw**: Script executável para Linux/Mac
- **maven-wrapper.properties**: Configuração da versão do Maven
- **maven-wrapper.jar**: JAR do wrapper (baixado automaticamente)

### Estrutura do Projeto Implementada
- **Backend**: `./backend/`
  - **src/main/java/com/sistema/**:
    - `SistemaJavaApplication.java`: Classe principal
    - `controller/HealthController.java`: Endpoints de API
    - `controller/HomeController.java`: Página inicial
    - `config/RedisConfig.java`: Configuração do Redis
  - **src/main/resources/**:
    - `application.properties`: Configurações da aplicação
    - `application-docker.properties`: Configurações para Docker
  - **Dockerfile**: Build multi-stage implementado
  - **pom.xml**: Dependências e configurações Maven
  - **mvnw**: Maven wrapper
- **Documentação**: `./docs/`
  - **README.md**: Documentação principal e quick start
  - **api.md**: Documentação completa da API REST
  - **installation.md**: Guia de instalação e configuração
  - **docker.md**: Documentação Docker Compose e arquitetura
- **Configuração**:
  - **docker-compose.yml**: Orquestração de todos os serviços
  - **.trae/rules/project_rules.md**: Regras e documentação do projeto

### Endpoints Funcionais
- **http://localhost:8080/**: Interface web responsiva com template Thymeleaf (via Docker Compose)
- **http://localhost:8080/api-simple**: Redirecionamento para página principal
- **http://localhost:8080/api/health**: Status da aplicação
- **http://localhost:8080/api/info**: Informações do sistema
- **http://localhost:8080/api/redis-test**: Teste do Redis
- **http://localhost:8080/actuator**: Endpoints do Actuator
- **http://localhost:8025**: Interface web do MailHog

**Nota**: A aplicação está configurada para rodar na porta 8080 através do Docker Compose. Para desenvolvimento local, a aplicação pode ser executada na porta 8082 via Maven (`mvn spring-boot:run`) para evitar conflitos de porta.

### Documentação Implementada

O projeto possui uma estrutura completa de documentação na pasta `docs/`:

#### Arquivos de Documentação
- **docs/README.md**: Visão geral do projeto, quick start e navegação
- **docs/api.md**: Documentação completa da API com todos os endpoints
- **docs/installation.md**: Guia detalhado de instalação e configuração
- **docs/docker.md**: Documentação completa do Docker Compose

#### Conteúdo da Documentação

**README.md**:
- Visão geral do sistema
- Tecnologias utilizadas
- Estrutura do projeto
- Quick start guide
- Links para documentação específica

**API.md**:
- Documentação de todos os endpoints REST
- Exemplos de requisições e respostas
- Códigos de erro e status
- Métodos de teste (cURL, HTTPie, scripts)
- Informações de monitoramento

**Installation.md**:
- Pré-requisitos do sistema
- Instalação rápida e detalhada
- Configurações de ambiente
- Comandos úteis para desenvolvimento
- Troubleshooting completo
- Configurações para produção

**Docker.md**:
- Visão geral da arquitetura Docker
- Detalhes de cada serviço
- Configurações e volumes
- Comandos de gerenciamento
- Health checks e monitoramento
- Otimizações e boas práticas

### Status do Projeto
- ✅ **Aplicação funcionando**: Todos os serviços rodando na porta 8080 via Docker Compose
- ✅ **Conectividade PostgreSQL**: Configurada e testada
- ✅ **Conectividade Redis**: Configurada e testada
- ✅ **MailHog**: Disponível para testes de email
- ✅ **Endpoints REST**: Implementados e funcionais
- ✅ **Docker Compose**: Totalmente operacional
- ✅ **Configuração de Porta**: Aplicação acessível em http://localhost:8080
- ✅ **Desenvolvimento Local**: Configurado para porta 8082 quando executado via Maven
- ✅ **Documentação**: Estrutura completa implementada
- ✅ **Guias de instalação**: Documentação detalhada criada
- ✅ **API Documentation**: Todos os endpoints documentados
- ✅ **Docker Documentation**: Arquitetura e configurações documentadas
- ✅ **Estrutura de Documentação**: Pasta `docs/` organizada e completa
- ✅ **Guias de Desenvolvimento**: Procedimentos e boas práticas documentadas
- ✅ **Troubleshooting**: Soluções para problemas comuns documentadas
- ✅ **Regras de Documentação**: Padrões e processos estabelecidos
- ✅ **Interface Thymeleaf**: Template responsivo implementado e funcionando
- ✅ **Design Responsivo**: CSS mobile-first com breakpoints adaptativos
- ✅ **HomeController**: Configurado para servir templates Thymeleaf
- ✅ **Template Engine**: Thymeleaf integrado ao Spring Boot

### Arquivos de Documentação Detalhados

#### docs/README.md
**Propósito**: Ponto de entrada principal da documentação
**Conteúdo**:
- Visão geral do Sistema Java
- Stack tecnológico completo
- Estrutura de diretórios
- Serviços disponíveis (Spring Boot, PostgreSQL, Redis, MailHog)
- Quick start com comandos essenciais
- Links para documentação específica
- Informações de contribuição

#### docs/api.md
**Propósito**: Documentação completa da API REST
**Conteúdo**:
- Visão geral da API
- Endpoints implementados:
  - `GET /`: Página inicial com navegação
  - `GET /api/health`: Health check da aplicação
  - `GET /api/info`: Informações da aplicação
  - `GET /api/redis-test`: Teste de conectividade Redis
  - `GET /actuator/*`: Endpoints do Spring Actuator
- Códigos de erro padronizados
- Exemplos de teste com cURL, HTTPie e scripts bash
- Informações de monitoramento e métricas

#### docs/installation.md
**Propósito**: Guia completo de instalação e configuração
**Conteúdo**:
- Pré-requisitos (Docker, Docker Compose, Git)
- Instalação rápida (3 comandos)
- Instalação detalhada passo a passo
- Configurações de ambiente
- Comandos úteis para desenvolvimento
- Desenvolvimento local com hot reload
- Troubleshooting de problemas comuns
- Configurações para ambiente de produção

#### docs/docker.md
**Propósito**: Documentação da arquitetura Docker
**Conteúdo**:
- Visão geral da arquitetura multi-serviços
- Detalhes de cada serviço:
  - Spring Boot (app)
  - PostgreSQL (postgres)
  - Redis (redis)
  - MailHog (mailhog)
- Configurações de rede e volumes
- Dockerfile multi-stage explicado
- Comandos de gerenciamento Docker
- Health checks e monitoramento
- Variáveis de ambiente
- Otimizações e boas práticas

### Desenvolvimento
- **Produção**: Usar Docker Compose (`docker-compose up -d`) - aplicação disponível em http://localhost:8080
- **Desenvolvimento Local**: Executar via Maven (`mvn spring-boot:run`) - aplicação disponível em http://localhost:8082
- Para desenvolvimento local, mapear o código fonte como volume
- Usar profiles do Spring Boot para diferentes ambientes
- Configurar hot reload quando possível
- Manter separação clara entre backend e outros componentes
- Consultar documentação na pasta `docs/` para referências
- Atualizar documentação ao implementar novas features
- **Configuração de Porta**:
  - Docker Compose: Porta 8080 (configurada no docker-compose.yml)
  - Maven local: Porta 8082 (configurada no application.yml)
- **Thymeleaf**:
  - Templates HTML em `/src/main/resources/templates/`
  - Controladores Spring MVC em `/src/main/java/com/sistema/controller/`
  - Configuração automática via Spring Boot
  - CSS responsivo integrado nos templates
  - Expressões Thymeleaf para dados dinâmicos

### Segurança
- Não expor portas desnecessárias
- Usar secrets para senhas em produção
- Configurar redes isoladas quando necessário

#### Gerenciamento de Chaves RSA
- **Inicialização Automática**: Par de chaves RSA gerado automaticamente na primeira execução
- **Validação**: Chaves existentes são validadas a cada inicialização da aplicação
- **Regeneração**: Chaves inválidas ou corrompidas são automaticamente regeneradas
- **Armazenamento**: Chaves armazenadas em formato PEM no diretório configurado
- **Configuração**: Diretório padrão `./keys`, configurável via `app.rsa.keys.directory`
- **Algoritmo**: RSA 2048 bits para garantir segurança adequada
- **Logs**: Monitoramento completo do processo de geração e validação
- **Estrutura de Arquivos**:
  - `private_key.pem`: Chave privada RSA
  - `public_key.pem`: Chave pública RSA
- **Classe Responsável**: `com.sistema.config.RSAKeyManager`

## Regras de Documentação

### Estrutura da Documentação

A documentação do projeto está organizada na pasta `docs/` com a seguinte estrutura:

```
docs/
├── README.md           # Visão geral e quick start
├── api.md             # Documentação completa da API
├── installation.md    # Guia de instalação e configuração
└── docker.md          # Documentação Docker Compose
```

### Padrões de Documentação

#### 1. Formato e Estilo
- **Formato**: Markdown (.md)
- **Encoding**: UTF-8
- **Idioma**: Português brasileiro
- **Títulos**: Usar hierarquia clara (H1, H2, H3)
- **Código**: Sempre usar blocos de código com syntax highlighting

#### 2. Estrutura dos Documentos

Todos os documentos devem seguir esta estrutura básica:

```markdown
# Título Principal

## Visão Geral
[Descrição breve do conteúdo]

## Seções Principais
[Conteúdo organizado em seções lógicas]

## Exemplos
[Exemplos práticos quando aplicável]

## Troubleshooting
[Problemas comuns e soluções]

---

## Próximos Passos
[Links para documentação relacionada]
```

#### 3. Documentação de API

**Padrão para endpoints**:
```markdown
### Endpoint Name

#### METHOD /path

Descrição do endpoint.

**Parâmetros**:
- `param1` (tipo): Descrição

**Resposta**:
```json
{
  "example": "response"
}
```

**Códigos de Status**:
- `200 OK`: Sucesso
- `404 Not Found`: Recurso não encontrado

**Exemplo de uso**:
```bash
curl http://localhost:8080/endpoint
```
```

#### 4. Documentação de Configuração

**Padrão para configurações**:
```markdown
### Nome da Configuração

**Arquivo**: `caminho/para/arquivo`

**Descrição**: Explicação da configuração

**Exemplo**:
```yaml
configuração:
  exemplo: valor
```

**Variáveis de ambiente**:
- `VAR_NAME`: Descrição da variável
```

### Regras de Manutenção

#### 1. Atualização Obrigatória

A documentação DEVE ser atualizada sempre que:
- Novos endpoints forem adicionados
- Configurações forem modificadas
- Dependências forem alteradas
- Processo de instalação mudar
- Novos serviços forem adicionados ao Docker Compose

#### 2. Versionamento

- Manter histórico de mudanças importantes
- Documentar breaking changes
- Incluir data da última atualização
- Versionar junto com releases do projeto

#### 3. Validação

Antes de commit, verificar:
- [ ] Links funcionam corretamente
- [ ] Exemplos de código são válidos
- [ ] Comandos foram testados
- [ ] Screenshots estão atualizados (se aplicável)
- [ ] Não há informações sensíveis (senhas, tokens)

### Responsabilidades

#### Desenvolvedor
- Atualizar documentação ao implementar features
- Documentar APIs e configurações
- Manter exemplos funcionais
- Reportar inconsistências

#### Tech Lead
- Revisar documentação em PRs
- Garantir padrões de qualidade
- Aprovar mudanças estruturais
- Manter visão geral atualizada

#### DevOps
- Documentar configurações de infraestrutura
- Manter guias de deployment
- Documentar procedimentos de backup
- Atualizar troubleshooting

### Ferramentas e Automação

#### 1. Validação Automática

```bash
# Script para validar links
#!/bin/bash
find docs/ -name "*.md" -exec markdown-link-check {} \;

# Validar sintaxe Markdown
markdownlint docs/

# Verificar ortografia
aspell check docs/*.md
```

#### 2. Geração Automática

- **API Docs**: Usar Swagger/OpenAPI quando possível
- **Diagramas**: Mermaid para diagramas de arquitetura
- **Screenshots**: Automatizar captura quando aplicável

#### 3. Templates

**Template para nova feature**:
```markdown
# Feature: [Nome]

## Descrição
[O que a feature faz]

## Configuração
[Como configurar]

## Uso
[Como usar com exemplos]

## API
[Endpoints relacionados]

## Alterações Recentes

### Resolução de Conflito de Mapeamento (Dezembro 2024)

#### Problema Identificado
- **Erro**: `java.lang.IllegalStateException: Ambiguous mapping. Cannot map 'webController' method`
- **Causa**: Conflito entre métodos `dashboard` no `WebController` e `LoginController`
- **Endpoint conflitante**: `GET /dashboard`

#### Solução Implementada
- **Arquivo alterado**: `/backend/src/main/java/com/sistema/controller/WebController.java`
- **Ação**: Removido método `dashboard` duplicado do `WebController`
- **Resultado**: Mantido apenas o método `dashboard` no `LoginController`

#### Funcionalidades Implementadas

##### 1. Sistema de Login
- **Controller**: `LoginController`
- **Endpoints**:
  - `POST /api/login`: Autenticação de usuários
  - `GET /dashboard`: Página do dashboard pós-login
- **Credenciais de teste**:
  - Username: `demo`
  - Password: `demo123`

##### 2. Interface de Dashboard
- **Template**: `/backend/src/main/resources/templates/dashboard.html`
- **Funcionalidades**:
  - Interface responsiva
  - Navegação integrada
  - Dados dinâmicos via Thymeleaf

#### Testes Realizados
- ✅ Login via API (`POST /api/login`)
- ✅ Acesso ao dashboard (`GET /dashboard`)
- ✅ Página principal (`GET /`)
- ✅ Health checks (`GET /api/health`)
- ✅ Teste Redis (`GET /api/redis-test`)
- ✅ Informações da aplicação (`GET /api/info`)

#### Commit e Deploy
- **Commit**: "Resolve conflito de mapeamento ambíguo removendo método dashboard duplicado do WebController"
- **Arquivos alterados**: 6 arquivos (695 inserções, 325 deleções)
- **Novos arquivos**: `LoginController.java`, `dashboard.html`
- **Status**: Enviado para repositório Git (branch main)
- **Deploy**: Aplicação rodando em http://localhost:8080

## Troubleshooting

### Problemas Conhecidos e Soluções

#### 1. Conflito de Mapeamento Ambíguo
**Sintoma**: `IllegalStateException: Ambiguous mapping`
**Causa**: Múltiplos controladores mapeando o mesmo endpoint
**Solução**: 
- Verificar logs com `docker-compose logs app | grep -A 10 'Ambiguous mapping'`
- Identificar controladores conflitantes
- Remover ou renomear endpoints duplicados

#### 2. Falha na Inicialização da Aplicação
**Sintoma**: Container não inicia ou para imediatamente
**Diagnóstico**:
```bash
# Verificar logs detalhados
docker-compose logs app

# Verificar status dos containers
docker-compose ps

# Rebuild completo
docker-compose down
docker-compose up -d --build
```

#### 3. Problemas de Conectividade com Banco
**Sintoma**: Erro de conexão com PostgreSQL
**Verificação**:
```bash
# Status do PostgreSQL
docker-compose logs postgres

# Teste de conectividade
docker-compose exec app curl -f http://localhost:8080/api/health
```

```

### Qualidade da Documentação

#### Critérios de Qualidade

1. **Clareza**: Linguagem simples e direta
2. **Completude**: Todas as funcionalidades documentadas
3. **Precisão**: Informações corretas e atualizadas
4. **Usabilidade**: Fácil navegação e busca
5. **Exemplos**: Casos práticos e funcionais

#### Checklist de Revisão

- [ ] Título descritivo e claro
- [ ] Visão geral presente
- [ ] Pré-requisitos listados
- [ ] Passos numerados quando aplicável
- [ ] Exemplos de código testados
- [ ] Links internos funcionais
- [ ] Formatação consistente
- [ ] Sem erros de português
- [ ] Informações sensíveis removidas

### Métricas e Monitoramento

#### KPIs da Documentação

- **Cobertura**: % de features documentadas
- **Atualização**: Tempo desde última atualização
- **Qualidade**: Feedback dos usuários
- **Uso**: Páginas mais acessadas

#### Feedback

- Coletar feedback regular dos desenvolvedores
- Monitorar issues relacionadas à documentação
- Realizar reviews periódicas
- Atualizar baseado em dúvidas frequentes

### Integração com Desenvolvimento

#### Definition of Done

Uma feature só está completa quando:
- [ ] Código implementado e testado
- [ ] Documentação atualizada
- [ ] Exemplos funcionais
- [ ] Review da documentação aprovada

#### Pull Request Template

```markdown
## Mudanças
- [ ] Código
- [ ] Documentação
- [ ] Testes

## Documentação
- [ ] README atualizado
- [ ] API docs atualizadas
- [ ] Guias de configuração atualizados
- [ ] Exemplos testados

## Checklist
- [ ] Documentação revisada
- [ ] Links verificados
- [ ] Exemplos funcionais
```