# Sistema Java - Documentação

## Visão Geral

Sistema Java completo implementado com Spring Boot 3.2.0, utilizando H2 como banco de dados em memória para desenvolvimento local e Redis embarcado para cache.

## Tecnologias Utilizadas

- **Backend**: Spring Boot 3.2.0
- **Java**: 21
- **Banco de Dados**: H2 (em memória para desenvolvimento)
- **Cache**: Redis (embarcado para desenvolvimento local)
- **Email Testing**: Mailtrap
- **Build Tool**: Maven 3.9.6

## Estrutura do Projeto

```
sistema_java/
├── backend/                 # Aplicação Spring Boot
│   ├── src/
│   │   ├── main/java/com/sistema/
│   │   │   ├── SistemaJavaApplication.java
│   │   │   ├── controller/
│   │   │   └── config/
│   │   └── main/resources/
│   │       ├── application.yml
│   │       └── templates/
│   └── pom.xml
├── docs/                    # Documentação do projeto
└── .trae/rules/            # Regras do projeto
```

## Aplicação

### Aplicação Principal
- **URL**: http://localhost:8080
- **Porta**: 8080
- **Descrição**: API REST com Spring Boot

### Banco de Dados H2
- **Console**: http://localhost:8080/h2-console
- **JDBC URL**: jdbc:h2:mem:sistema_db
- **Usuário**: sa
- **Senha**: (vazio)

### Cache Redis
- **Porta**: 6379 (embarcado)
- **Descrição**: Cache em memória para desenvolvimento

## Endpoints Disponíveis

### Página Inicial
- **GET /**: Página inicial com navegação

### API Endpoints
- **GET /api/health**: Health check da aplicação
- **GET /api/info**: Informações da aplicação
- **GET /api/redis-test**: Teste de conectividade com Redis

### Actuator
- **GET /actuator**: Endpoints de monitoramento do Spring Boot

## Quick Start

### Pré-requisitos
- Java 21
- Maven 3.9.6 (ou usar o wrapper incluído)

### Executando o Projeto

1. Clone o repositório
2. Navegue até o diretório backend:
   ```bash
   cd backend
   ```
3. Execute a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```
4. Acesse a aplicação em http://localhost:8080

### Parando a Aplicação

Pressione `Ctrl+C` no terminal onde a aplicação está rodando.

## Documentação Adicional

- [Guia de Instalação](./installation.md)
- [Documentação da API](./api.md)

## Status do Projeto

✅ **Aplicação funcionando**: Spring Boot rodando localmente  
✅ **Banco H2**: Configurado e funcionando em memória  
✅ **Cache Redis**: Configurado para desenvolvimento local  
✅ **MailHog**: Disponível para testes de email  
✅ **Endpoints REST**: Implementados e funcionais  
✅ **Docker Compose**: Totalmente operacional  

## Contribuição

Para contribuir com o projeto, consulte as [regras de documentação](../.trae/rules/project_rules.md).