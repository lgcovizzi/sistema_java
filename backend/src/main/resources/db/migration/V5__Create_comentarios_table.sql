-- Migration V5: Criar tabela comentarios
-- Autor: Sistema Java
-- Data: 2024-01-15
-- Descrição: Criação da tabela de comentários das notícias

CREATE TABLE comentarios (
    id BIGSERIAL PRIMARY KEY,
    conteudo TEXT NOT NULL,
    aprovado BOOLEAN NOT NULL DEFAULT false,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    autor_id BIGINT NOT NULL,
    noticia_id BIGINT NOT NULL,
    CONSTRAINT fk_comentario_autor FOREIGN KEY (autor_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_comentario_noticia FOREIGN KEY (noticia_id) REFERENCES noticias(id) ON DELETE CASCADE
);

-- Índices para otimização
CREATE INDEX idx_comentario_noticia ON comentarios(noticia_id);
CREATE INDEX idx_comentario_aprovado ON comentarios(aprovado);
CREATE INDEX idx_comentario_autor ON comentarios(autor_id);
CREATE INDEX idx_comentario_data_criacao ON comentarios(data_criacao DESC);

-- Índice composto para consultas de comentários aprovados por notícia
CREATE INDEX idx_comentario_noticia_aprovado ON comentarios(noticia_id, aprovado, data_criacao DESC) WHERE aprovado = true;

-- Comentários nas colunas
COMMENT ON TABLE comentarios IS 'Tabela de comentários das notícias';
COMMENT ON COLUMN comentarios.id IS 'Identificador único do comentário';
COMMENT ON COLUMN comentarios.conteudo IS 'Conteúdo do comentário';
COMMENT ON COLUMN comentarios.aprovado IS 'Indica se o comentário foi aprovado';
COMMENT ON COLUMN comentarios.data_criacao IS 'Data e hora de criação do comentário';
COMMENT ON COLUMN comentarios.autor_id IS 'Referência ao usuário autor do comentário';
COMMENT ON COLUMN comentarios.noticia_id IS 'Referência à notícia comentada';