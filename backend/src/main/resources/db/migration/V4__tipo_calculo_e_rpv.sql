-- Adiciona campos para cálculo de condenação da Fazenda Pública e RPV/Precatório
ALTER TABLE calculo ADD COLUMN tipo_calculo VARCHAR(30) DEFAULT 'PADRAO' NOT NULL;
ALTER TABLE calculo ADD COLUMN natureza_condenacao VARCHAR(30) NULL;
ALTER TABLE calculo ADD COLUMN rpv_precatorio BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE calculo ADD COLUMN data_rpv_precatorio DATE NULL;
