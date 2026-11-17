package br.com.autocenterfiap.billing.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados opcionais da recusa do pagamento")
public record RecusarPagamentoRequest(
		@Schema(description = "Motivo da recusa", example = "Cartao recusado") String motivo,
		@Schema(description = "Identificador do pagamento no Mercado Pago", example = "987654321")
		String mpPaymentId) {
}

