-- Script de inicialização do banco de dados
-- Executado automaticamente na primeira inicialização do container

-- Criar extensões necessárias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Criar schema de auditoria
CREATE SCHEMA IF NOT EXISTS audit;

-- Comentários
COMMENT ON DATABASE voto_dev IS 'Database para Sistema de Votação Eletrônica';
COMMENT ON SCHEMA audit IS 'Schema dedicado para tabelas de auditoria';

-- Configurações de timezone
SET timezone = 'UTC';

-- Grant permissões
GRANT ALL PRIVILEGES ON DATABASE voto_dev TO voto_user;
GRANT ALL PRIVILEGES ON SCHEMA public TO voto_user;
GRANT ALL PRIVILEGES ON SCHEMA audit TO voto_user;
