-- Remove auth/user tables and columns

-- Drop FK and index from processo table
ALTER TABLE processo DROP FOREIGN KEY processo_ibfk_1;
ALTER TABLE processo DROP INDEX idx_usuario;
ALTER TABLE processo DROP COLUMN usuario_id;

-- Drop auth tables
DROP TABLE IF EXISTS usuario_perfil;
DROP TABLE IF EXISTS log_auditoria;
DROP TABLE IF EXISTS usuario;
DROP TABLE IF EXISTS perfil;
