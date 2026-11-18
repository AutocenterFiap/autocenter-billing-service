package br.com.autocenterfiap.billing.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Corpo opcional ao reprovar um orcamento (se nao enviado, usa o motivo padrao do contrato).
 */
@Schema(description = "Dados opcionais da reprovação do orçamento")
public record ReprovarOrcamentoRequest(
		@Schema(description = "Motivo da reprovação; se omitido, usa \"Cliente recusou o valor\"",
				example = "Valor acima do esperado") String motivo) {
}

