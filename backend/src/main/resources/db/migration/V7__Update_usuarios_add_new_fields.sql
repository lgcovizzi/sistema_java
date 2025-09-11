-- Migration V7: Atualizar tabela usuarios com novos campos
-- Adiciona campos: sobrenome, cpf, telefone, data_nascimento, avatar, papel
-- Adiciona campos de controle: email_verificado, token_verificacao, token_reset_senha, data_expiracao_token, ultimo_acesso
-- Adiciona índices para performance

-- Adicionar novos campos à tabela usuarios
ALTER TABLE usuarios 
ADD COLUMN IF NOT EXISTS sobrenome VARCHAR(100),
ADD COLUMN IF NOT EXISTS cpf VARCHAR(11) UNIQUE,
ADD COLUMN IF NOT EXISTS telefone VARCHAR(20),
ADD COLUMN IF NOT EXISTS data_nascimento DATE,
ADD COLUMN IF NOT EXISTS avatar VARCHAR(500),
ADD COLUMN IF NOT EXISTS papel VARCHAR(20) NOT NULL DEFAULT 'USUARIO',
ADD COLUMN IF NOT EXISTS email_verificado BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS token_verificacao VARCHAR(255),
ADD COLUMN IF NOT EXISTS token_reset_senha VARCHAR(255),
ADD COLUMN IF NOT EXISTS data_expiracao_token TIMESTAMP,
ADD COLUMN IF NOT EXISTS ultimo_acesso TIMESTAMP;

-- Adicionar constraint para validar papel
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_papel') THEN
        ALTER TABLE usuarios ADD CONSTRAINT chk_papel 
        CHECK (papel IN ('ADMINISTRADOR', 'FUNDADOR', 'COLABORADOR', 'PARCEIRO', 'ASSOCIADO', 'USUARIO', 'CONVIDADO'));
    END IF;
END $$;

-- Adicionar constraint para validar CPF (11 dígitos)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_cpf_format') THEN
        ALTER TABLE usuarios ADD CONSTRAINT chk_cpf_format 
        CHECK (cpf IS NULL OR cpf ~ '^[0-9]{11}$');
    END IF;
END $$;

-- Adicionar constraint para validar email
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_email_format') THEN
        ALTER TABLE usuarios ADD CONSTRAINT chk_email_format 
        CHECK (email ~ '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$');
    END IF;
END $$;

-- Adicionar constraint para validar telefone
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_telefone_format') THEN
        ALTER TABLE usuarios ADD CONSTRAINT chk_telefone_format 
        CHECK (telefone IS NULL OR telefone ~ '^[0-9\-()+ ]{8,20}$');
    END IF;
END $$;

-- Criar índices para performance
CREATE INDEX IF NOT EXISTS idx_usuario_cpf ON usuarios(cpf);
CREATE INDEX IF NOT EXISTS idx_usuario_papel ON usuarios(papel);
CREATE INDEX IF NOT EXISTS idx_usuario_email ON usuarios(email);
CREATE INDEX IF NOT EXISTS idx_usuario_ativo ON usuarios(ativo);
CREATE INDEX IF NOT EXISTS idx_usuario_email_verificado ON usuarios(email_verificado);
CREATE INDEX IF NOT EXISTS idx_usuario_token_verificacao ON usuarios(token_verificacao);
CREATE INDEX IF NOT EXISTS idx_usuario_token_reset ON usuarios(token_reset_senha);
CREATE INDEX IF NOT EXISTS idx_usuario_data_criacao ON usuarios(data_criacao);
CREATE INDEX IF NOT EXISTS idx_usuario_ultimo_acesso ON usuarios(ultimo_acesso);

-- Atualizar usuários existentes com papel padrão se necessário
UPDATE usuarios 
SET papel = 'USUARIO' 
WHERE papel IS NULL OR papel = '';

-- Atualizar email_verificado para usuários existentes
UPDATE usuarios 
SET email_verificado = TRUE 
WHERE email_verificado IS NULL AND ativo = TRUE;

-- Comentários para documentação
COMMENT ON COLUMN usuarios.sobrenome IS 'Sobrenome do usuário (opcional)';
COMMENT ON COLUMN usuarios.cpf IS 'CPF do usuário - 11 dígitos únicos (opcional)';
COMMENT ON COLUMN usuarios.telefone IS 'Telefone do usuário com formato brasileiro (opcional)';
COMMENT ON COLUMN usuarios.data_nascimento IS 'Data de nascimento do usuário (opcional)';
COMMENT ON COLUMN usuarios.avatar IS 'Caminho para o arquivo de avatar do usuário (opcional)';
COMMENT ON COLUMN usuarios.papel IS 'Papel/Role do usuário no sistema';
COMMENT ON COLUMN usuarios.email_verificado IS 'Indica se o email foi verificado';
COMMENT ON COLUMN usuarios.token_verificacao IS 'Token para verificação de email';
COMMENT ON COLUMN usuarios.token_reset_senha IS 'Token para reset de senha';
COMMENT ON COLUMN usuarios.data_expiracao_token IS 'Data de expiração dos tokens';
COMMENT ON COLUMN usuarios.ultimo_acesso IS 'Data e hora do último acesso do usuário';

-- Criar usuário administrador padrão se não existir
INSERT INTO usuarios (nome, email, senha, papel, ativo, email_verificado, data_criacao, data_atualizacao)
SELECT 
    'Administrador',
    'admin@sistema.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdOIGGrNG5f8X.VG', -- senha: admin123
    'ADMINISTRADOR',
    TRUE,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM usuarios WHERE email = 'admin@sistema.com'
);

-- Criar usuário fundador padrão se não existir
INSERT INTO usuarios (nome, email, senha, papel, ativo, email_verificado, data_criacao, data_atualizacao)
SELECT 
    'Fundador',
    'fundador@sistema.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdOIGGrNG5f8X.VG', -- senha: admin123
    'FUNDADOR',
    TRUE,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM usuarios WHERE email = 'fundador@sistema.com'
);