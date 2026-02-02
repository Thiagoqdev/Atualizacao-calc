-- =============================================
-- Sistema de Cálculos Jurídicos Financeiros
-- Migration V1: Schema Inicial
-- =============================================

-- Tabela de Perfis (Roles)
CREATE TABLE perfil (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela de Usuários
CREATE TABLE usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome_completo VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE,
    data_criacao DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela de associação Usuário-Perfil
CREATE TABLE usuario_perfil (
    usuario_id BIGINT NOT NULL,
    perfil_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, perfil_id),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
    FOREIGN KEY (perfil_id) REFERENCES perfil(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela de Índices Monetários
CREATE TABLE tabela_indice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE,
    descricao VARCHAR(255),
    codigo_oficial VARCHAR(20),
    fonte_api VARCHAR(20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela de Valores dos Índices
CREATE TABLE valor_indice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tabela_indice_id BIGINT NOT NULL,
    competencia DATE NOT NULL,
    valor DECIMAL(18,8) NOT NULL,
    fonte VARCHAR(20) DEFAULT 'MANUAL',
    UNIQUE KEY uk_indice_competencia (tabela_indice_id, competencia),
    INDEX idx_competencia (competencia),
    FOREIGN KEY (tabela_indice_id) REFERENCES tabela_indice(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela de Processos Jurídicos
CREATE TABLE processo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_processo VARCHAR(50),
    descricao VARCHAR(500),
    vara_tribunal VARCHAR(255),
    tipo_acao ENUM('TRABALHISTA', 'CIVEL', 'PREVIDENCIARIA', 'TRIBUTARIA') NOT NULL,
    usuario_id BIGINT NOT NULL,
    data_criacao DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_usuario (usuario_id),
    INDEX idx_numero (numero_processo),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela de Cálculos
CREATE TABLE calculo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    processo_id BIGINT,
    titulo VARCHAR(255) NOT NULL,
    valor_principal DECIMAL(18,2) NOT NULL,
    data_inicial DATE NOT NULL,
    data_final DATE NOT NULL,
    tabela_indice_id BIGINT,
    tipo_juros ENUM('SIMPLES', 'COMPOSTO') DEFAULT 'SIMPLES',
    taxa_juros DECIMAL(8,4),
    periodicidade_juros ENUM('DIARIO', 'MENSAL', 'ANUAL') DEFAULT 'MENSAL',
    multa_percentual DECIMAL(8,4) DEFAULT 0,
    honorarios_percentual DECIMAL(8,4) DEFAULT 0,
    juros_sobre_corrigido BOOLEAN DEFAULT TRUE,
    status ENUM('RASCUNHO', 'CALCULADO', 'FINALIZADO') DEFAULT 'RASCUNHO',
    data_criacao DATETIME DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao DATETIME ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_processo (processo_id),
    INDEX idx_status (status),
    FOREIGN KEY (processo_id) REFERENCES processo(id) ON DELETE SET NULL,
    FOREIGN KEY (tabela_indice_id) REFERENCES tabela_indice(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela de Parcelas do Cálculo
CREATE TABLE parcela_calculo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    calculo_id BIGINT NOT NULL,
    descricao VARCHAR(255),
    valor_original DECIMAL(18,2) NOT NULL,
    data_vencimento DATE NOT NULL,
    FOREIGN KEY (calculo_id) REFERENCES calculo(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela de Resultados do Cálculo
CREATE TABLE resultado_calculo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    calculo_id BIGINT NOT NULL UNIQUE,
    valor_corrigido DECIMAL(18,2) NOT NULL,
    valor_juros DECIMAL(18,2) NOT NULL,
    valor_multa DECIMAL(18,2) NOT NULL,
    valor_honorarios DECIMAL(18,2) NOT NULL,
    valor_total DECIMAL(18,2) NOT NULL,
    data_calculo DATETIME DEFAULT CURRENT_TIMESTAMP,
    detalhamento_json LONGTEXT,
    FOREIGN KEY (calculo_id) REFERENCES calculo(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela de Log de Auditoria
CREATE TABLE log_auditoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT,
    acao VARCHAR(100) NOT NULL,
    entidade VARCHAR(50),
    entidade_id BIGINT,
    descricao TEXT,
    ip VARCHAR(45),
    data_hora DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_usuario (usuario_id),
    INDEX idx_log_data (data_hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Dados Iniciais
-- =============================================

-- Inserir perfis padrão
INSERT INTO perfil (nome) VALUES ('ROLE_ADMIN');
INSERT INTO perfil (nome) VALUES ('ROLE_USER');

-- Inserir índices monetários padrão
INSERT INTO tabela_indice (nome, descricao, codigo_oficial, fonte_api) VALUES
('IPCA_E', 'IPCA-E - Índice Nacional de Preços ao Consumidor Amplo Especial (IBGE)', '1737', 'IBGE'),
('INPC', 'INPC - Índice Nacional de Preços ao Consumidor (IBGE)', '1736', 'IBGE'),
('IGPM', 'IGP-M - Índice Geral de Preços do Mercado (FGV)', 'IGP12_IGPMG12', 'FGV'),
('TR', 'TR - Taxa Referencial (BCB)', '226', 'BCB'),
('SELIC', 'SELIC - Taxa SELIC acumulada (BCB)', '4390', 'BCB');
