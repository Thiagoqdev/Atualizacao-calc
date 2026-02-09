-- =============================================
-- Migration V2: Atualizar códigos para séries BCB SGS
-- Unifica todas as fontes para usar a API do BCB
-- =============================================

-- IPCA-E: série IBGE 1737 → série BCB SGS 10764
UPDATE tabela_indice SET codigo_oficial = '10764', fonte_api = 'BCB'
WHERE nome = 'IPCA_E';

-- INPC: série IBGE 1736 → série BCB SGS 188
UPDATE tabela_indice SET codigo_oficial = '188', fonte_api = 'BCB'
WHERE nome = 'INPC';

-- IGP-M: código FGV → série BCB SGS 189
UPDATE tabela_indice SET codigo_oficial = '189', fonte_api = 'BCB'
WHERE nome = 'IGPM';

-- TR: série BCB 226 (já correto, apenas confirmar fonte)
UPDATE tabela_indice SET codigo_oficial = '226', fonte_api = 'BCB'
WHERE nome = 'TR';

-- SELIC: série BCB 4390 (já correto, apenas confirmar fonte)
UPDATE tabela_indice SET codigo_oficial = '4390', fonte_api = 'BCB'
WHERE nome = 'SELIC';
