-- Migration V1: Criar tabela usuarios
-- Autor: Sistema Java
-- Data: 2024-01-15
-- Descrição: Criação da tabela de usuários com campos básicos e auditoria

CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT true,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para otimização
CREATE UNIQUE INDEX idx_usuario_email ON usuarios(email);
CREATE INDEX idx_usuario_ativo ON usuarios(ativo);
CREATE INDEX idx_usuario_data_criacao ON usuarios(data_criacao);

-- Comentários nas colunas
COMMENT ON TABLE usuarios IS 'Tabela de usuários do sistema';
COMMENT ON COLUMN usuarios.id IS 'Identificador único do usuário';
COMMENT ON COLUMN usuarios.nome IS 'Nome completo do usuário';
COMMENT ON COLUMN usuarios.email IS 'Email único do usuário para login';
COMMENT ON COLUMN usuarios.senha IS 'Senha criptografada do usuário';
COMMENT ON COLUMN usuarios.ativo IS 'Indica se o usuário está ativo no sistema';
COMMENT ON COLUMN usuarios.data_criacao IS 'Data e hora de criação do registro';
COMMENT ON COLUMN usuarios.data_atualizacao IS 'Data e hora da última atualização';