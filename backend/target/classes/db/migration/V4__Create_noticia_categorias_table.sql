-- Migration V4: Criar tabela noticia_categorias
-- Autor: Sistema Java
-- Data: 2024-01-15
-- Descrição: Criação da tabela de relacionamento muitos-para-muitos entre notícias e categorias

CREATE TABLE noticia_categorias (
    noticia_id BIGINT NOT NULL,
    categoria_id BIGINT NOT NULL,
    PRIMARY KEY (noticia_id, categoria_id),
    CONSTRAINT fk_noticia_categoria_noticia FOREIGN KEY (noticia_id) REFERENCES noticias(id) ON DELETE CASCADE,
    CONSTRAINT fk_noticia_categoria_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id) ON DELETE CASCADE
);

-- Índices para otimização
CREATE INDEX idx_noticia_categorias_noticia ON noticia_categorias(noticia_id);
CREATE INDEX idx_noticia_categorias_categoria ON noticia_categorias(categoria_id);

-- Comentários na tabela
COMMENT ON TABLE noticia_categorias IS 'Tabela de relacionamento entre notícias e categorias';
COMMENT ON COLUMN noticia_categorias.noticia_id IS 'Referência à notícia';
COMMENT ON COLUMN noticia_categorias.categoria_id IS 'Referência à categoria';