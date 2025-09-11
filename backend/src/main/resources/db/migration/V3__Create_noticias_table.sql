-- Migration V3: Criar tabela noticias
-- Autor: Sistema Java
-- Data: 2024-01-15
-- Descrição: Criação da tabela de notícias com relacionamento para usuários

CREATE TABLE noticias (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(200) NOT NULL,
    conteudo TEXT NOT NULL,
    resumo VARCHAR(500),
    publicada BOOLEAN NOT NULL DEFAULT false,
    data_publicacao TIMESTAMP,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    autor_id BIGINT NOT NULL,
    CONSTRAINT fk_noticia_autor FOREIGN KEY (autor_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- Índices para otimização
CREATE INDEX idx_noticia_publicada ON noticias(publicada);
CREATE INDEX idx_noticia_data_publicacao ON noticias(data_publicacao DESC);
CREATE INDEX idx_noticia_autor ON noticias(autor_id);
CREATE INDEX idx_noticia_data_criacao ON noticias(data_criacao DESC);
CREATE INDEX idx_noticia_titulo ON noticias(titulo);

-- Índice composto para consultas de notícias publicadas por data
CREATE INDEX idx_noticia_publicada_data ON noticias(publicada, data_publicacao DESC) WHERE publicada = true;

-- Comentários nas colunas
COMMENT ON TABLE noticias IS 'Tabela de notícias do sistema';
COMMENT ON COLUMN noticias.id IS 'Identificador único da notícia';
COMMENT ON COLUMN noticias.titulo IS 'Título da notícia';
COMMENT ON COLUMN noticias.conteudo IS 'Conteúdo completo da notícia';
COMMENT ON COLUMN noticias.resumo IS 'Resumo ou chamada da notícia';
COMMENT ON COLUMN noticias.publicada IS 'Indica se a notícia está publicada';
COMMENT ON COLUMN noticias.data_publicacao IS 'Data e hora de publicação da notícia';
COMMENT ON COLUMN noticias.data_criacao IS 'Data e hora de criação do registro';
COMMENT ON COLUMN noticias.data_atualizacao IS 'Data e hora da última atualização';
COMMENT ON COLUMN noticias.autor_id IS 'Referência ao usuário autor da notícia';