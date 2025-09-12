-- Migration V6: Inserir dados iniciais
-- Autor: Sistema Java
-- Data: 2024-01-15
-- Descrição: Inserção de dados iniciais para desenvolvimento e testes

-- Inserir usuário administrador padrão
-- Senha: admin123 (deve ser alterada em produção)
INSERT INTO usuarios (nome, email, senha, ativo) VALUES 
('Administrador do Sistema', 'admin@sistema.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdOIGGrNG5f8X.VG', true),
('Editor de Conteúdo', 'editor@sistema.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdOIGGrNG5f8X.VG', true),
('Usuário Teste', 'usuario@teste.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdOIGGrNG5f8X.VG', true);

-- Inserir categorias padrão
INSERT INTO categorias (nome, descricao, ativa) VALUES 
('Notícias Gerais', 'Notícias e informações gerais do sistema', true),
('Comunicados', 'Comunicados oficiais e avisos importantes', true),
('Eventos', 'Informações sobre eventos e atividades', true),
('Tecnologia', 'Notícias relacionadas à tecnologia e inovação', true),
('Institucional', 'Informações institucionais e corporativas', true);

-- Inserir notícias de exemplo
INSERT INTO noticias (titulo, conteudo, resumo, publicada, data_publicacao, autor_id) VALUES 
(
    'Bem-vindos ao Sistema Java',
    'Este é o primeiro post do nosso sistema de notícias desenvolvido em Java com Spring Boot e JSF. O sistema oferece funcionalidades completas para gerenciamento de conteúdo, incluindo criação de notícias, categorização, comentários e muito mais.',
    'Primeira notícia do sistema apresentando as funcionalidades principais.',
    true,
    CURRENT_TIMESTAMP,
    1
),
(
    'Funcionalidades do Sistema',
    'Nosso sistema oferece diversas funcionalidades: gerenciamento de usuários, criação e edição de notícias, sistema de categorias, comentários moderados, interface responsiva com PrimeFaces, e muito mais. Tudo desenvolvido seguindo as melhores práticas de desenvolvimento.',
    'Conheça todas as funcionalidades disponíveis no sistema.',
    true,
    CURRENT_TIMESTAMP,
    2
),
(
    'Tecnologias Utilizadas',
    'O sistema foi desenvolvido utilizando as seguintes tecnologias: Java 17, Spring Boot 3.x, JSF 4.0, PrimeFaces 13.x, PostgreSQL 15, Redis, Docker, Maven, JUnit 5, Mockito, TestContainers e muito mais. Uma stack moderna e robusta para aplicações enterprise.',
    'Conheça a stack tecnológica utilizada no desenvolvimento.',
    false,
    NULL,
    1
);

-- Associar notícias às categorias
INSERT INTO noticia_categorias (noticia_id, categoria_id) VALUES 
(1, 1), -- Bem-vindos -> Notícias Gerais
(1, 5), -- Bem-vindos -> Institucional
(2, 1), -- Funcionalidades -> Notícias Gerais
(2, 4), -- Funcionalidades -> Tecnologia
(3, 4), -- Tecnologias -> Tecnologia
(3, 5); -- Tecnologias -> Institucional

-- Inserir comentários de exemplo
INSERT INTO comentarios (conteudo, aprovado, autor_id, noticia_id) VALUES 
('Excelente iniciativa! Estou ansioso para usar o sistema.', true, 3, 1),
('As funcionalidades parecem muito completas. Parabéns!', true, 3, 2),
('Ótima escolha de tecnologias. Stack muito moderna!', false, 3, 3);

-- Comentários sobre a migração
COMMENT ON TABLE usuarios IS 'Dados iniciais: 3 usuários (admin, editor, teste)';
COMMENT ON TABLE categorias IS 'Dados iniciais: 5 categorias padrão';
COMMENT ON TABLE noticias IS 'Dados iniciais: 3 notícias de exemplo';
COMMENT ON TABLE comentarios IS 'Dados iniciais: 3 comentários de exemplo';