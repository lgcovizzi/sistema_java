-- Migration V2: Criar tabela categorias
-- Autor: Sistema Java
-- Data: 2024-01-15
-- Descrição: Criação da tabela de categorias para classificação de notícias

CREATE TABLE categorias (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE,
    descricao TEXT,
    ativa BOOLEAN NOT NULL DEFAULT true,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para otimização
CREATE UNIQUE INDEX idx_categoria_nome ON categorias(nome);
CREATE INDEX idx_categoria_ativa ON categorias(ativa);
CREATE INDEX idx_categoria_data_criacao ON categorias(data_criacao);

-- Comentários nas colunas
COMMENT ON TABLE categorias IS 'Tabela de categorias para classificação de notícias';
COMMENT ON COLUMN categorias.id IS 'Identificador único da categoria';
COMMENT ON COLUMN categorias.nome IS 'Nome único da categoria';
COMMENT ON COLUMN categorias.descricao IS 'Descrição detalhada da categoria';
COMMENT ON COLUMN categorias.ativa IS 'Indica se a categoria está ativa';
COMMENT ON COLUMN categorias.data_criacao IS 'Data e hora de criação do registro';