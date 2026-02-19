-- =============================================
-- Migration V3: Add per-parcela index override
-- Allows each parcela to optionally use its own correction index
-- NULL = use the parent Calculo's global index
-- =============================================

ALTER TABLE parcela_calculo
    ADD COLUMN tabela_indice_id BIGINT NULL,
    ADD CONSTRAINT fk_parcela_tabela_indice
        FOREIGN KEY (tabela_indice_id) REFERENCES tabela_indice(id);
