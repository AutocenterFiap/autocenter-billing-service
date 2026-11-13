-- =============================================================================
-- V1__criar_tabelas_billing.sql
-- Tabelas do microsservico de Orcamento e Pagamento (Billing)
-- Mantem a mesma estrutura de orcamento do monolito + pagamento + deduplicacao
-- =============================================================================

CREATE TABLE orcamento (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    ordem_servico_id        BIGINT         NOT NULL,
    cliente_id              BIGINT,
    correlation_id          VARCHAR(64),
    valor_total             NUMERIC(15, 2) NOT NULL,
    status_orcamento        VARCHAR(30)    NOT NULL,
    motivo                  VARCHAR(255),
    data_criacao            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_ultima_atualizacao TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orcamento_os     ON orcamento (ordem_servico_id);
CREATE INDEX idx_orcamento_status ON orcamento (status_orcamento);

CREATE TABLE pagamento (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    ordem_servico_id        BIGINT         NOT NULL,
    orcamento_id            BIGINT         NOT NULL,
    correlation_id          VARCHAR(64),
    valor                   NUMERIC(15, 2) NOT NULL,
    tentativa               INT            NOT NULL,
    status_pagamento        VARCHAR(30)    NOT NULL,
    url_checkout            VARCHAR(500),
    expira_em               TIMESTAMP,
    mp_preference_id        VARCHAR(100),
    mp_payment_id           VARCHAR(100),
    motivo                  VARCHAR(255),
    data_criacao            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_ultima_atualizacao TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pagamento_orcamento FOREIGN KEY (orcamento_id) REFERENCES orcamento(id)
);

CREATE INDEX idx_pagamento_os ON pagamento (ordem_servico_id);

CREATE TABLE eventos_processados (
    event_id      VARCHAR(64) PRIMARY KEY,
    processado_em TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

