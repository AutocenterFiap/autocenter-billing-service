package br.com.autocenterfiap.billing.adapter.in.dto;

/**
 * Corpo opcional ao reprovar um orcamento (se nao enviado, usa o motivo padrao do contrato).
 */
public record ReprovarOrcamentoRequest(String motivo) {
}

