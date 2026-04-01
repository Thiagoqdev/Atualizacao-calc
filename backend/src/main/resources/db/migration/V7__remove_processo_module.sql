-- Remove módulo de processos (modularização: sistema focado apenas em cálculos)

-- Drop FK de calculo para processo
ALTER TABLE calculo DROP FOREIGN KEY calculo_ibfk_1;
ALTER TABLE calculo DROP INDEX idx_processo;
ALTER TABLE calculo DROP COLUMN processo_id;

-- Drop tabela processo
DROP TABLE IF EXISTS processo;
