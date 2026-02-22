-- Corrige tipos das colunas tipo_calculo e natureza_condenacao de VARCHAR para ENUM
-- para compatibilidade com Hibernate schema validation

ALTER TABLE calculo MODIFY COLUMN tipo_calculo ENUM('PADRAO','FAZENDA_PUBLICA') NOT NULL DEFAULT 'PADRAO';
ALTER TABLE calculo MODIFY COLUMN natureza_condenacao ENUM('ADMINISTRATIVA','SERVIDOR_PUBLICO','TRIBUTARIA') NULL;
