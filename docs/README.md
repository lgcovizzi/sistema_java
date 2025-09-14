# Sistema Java - Documentação

## Visão Geral

Sistema Java completo implementado com Spring Boot 3.2.0, utilizando PostgreSQL como banco de dados principal, Redis para cache, e MailHog para testes de email. O projeto está totalmente containerizado com Docker Compose.

## Tecnologias Utilizadas

- **Backend**: Spring Boot 3.2.0
- **Java**: 17
- **Banco de Dados**: PostgreSQL 15
- **Cache**: Redis 7
- **Email Testing**: MailHog
- **Containerização**: Docker & Docker Compose
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
│   ├── Dockerfile
│   └── pom.xml
├── docs/                    # Documentação do projeto
├── docker-compose.yml       # Configuração dos serviços
└── .trae/rules/            # Regras do projeto
```

## Serviços Disponíveis

### Aplicação Principal
- **URL**: http://localhost:8080
- **Porta**: 8080
- **Descrição**: API REST com Spring Boot

### PostgreSQL
- **Porta**: 5432
- **Database**: sistema_db
- **Usuário**: sistema_user

### Redis
- **Porta**: 6379
- **Descrição**: Cache e sessões

### MailHog
- **SMTP**: localhost:1025
- **Web UI**: http://localhost:8025
- **Descrição**: Servidor de email para testes

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
- Docker
- Docker Compose

### Executando o Projeto

1. Clone o repositório
2. Execute o comando:
   ```bash
   docker compose up -d
   ```
3. Acesse a aplicação em http://localhost:8080

### Parando os Serviços

```bash
docker compose down
```

## Documentação Adicional

- [Guia de Instalação](./installation.md)
- [Documentação da API](./api.md)
- [Configuração Docker](./docker.md)

## Status do Projeto

✅ **Aplicação funcionando**: Todos os serviços rodando  
✅ **Conectividade PostgreSQL**: Configurada e testada  
✅ **Conectividade Redis**: Configurada e testada  
✅ **MailHog**: Disponível para testes de email  
✅ **Endpoints REST**: Implementados e funcionais  
✅ **Docker Compose**: Totalmente operacional  

## Contribuição

Para contribuir com o projeto, consulte as [regras de documentação](../.trae/rules/project_rules.md).