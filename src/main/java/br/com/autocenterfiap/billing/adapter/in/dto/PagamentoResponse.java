package br.com.autocenterfiap.billing.adapter.in.dto;

import br.com.autocenterfiap.billing.application.dto.PagamentoOutput;
import br.com.autocenterfiap.billing.domain.enums.StatusPagamento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Schema(description = "Cobrança de um orçamento no Mercado Pago")
public record PagamentoResponse(
		@Schema(description = "Identificador do pagamento", example = "15") Long id,
		@Schema(description = "Identificador da OS", example = "42") Long ordemServicoId,
		@Schema(description = "Identificador do orçamento", example = "7") Long orcamentoId,
		@Schema(description = "Valor cobrado", example = "773.90") BigDecimal valor,
		@Schema(description = "Número da tentativa de cobrança da OS", example = "1") Integer tentativa,
		@Schema(description = "Status do pagamento", example = "PENDENTE") StatusPagamento statusPagamento,
		@Schema(description = "Link do Checkout Pro para o cliente pagar",
				example = "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=123") String urlCheckout,
		@Schema(description = "Expiração do link de pagamento", example = "2026-11-17T12:30:00Z") Instant expiraEm,
		@Schema(description = "Identificador da preferência no Mercado Pago", example = "123456-abcd")
		String mpPreferenceId,
		@Schema(description = "Identificador do pagamento no Mercado Pago", example = "987654321")
		String mpPaymentId,
		@Schema(description = "Motivo da recusa, do cancelamento ou da falha de estorno", example = "Cartao recusado")
		String motivo,
		@Schema(description = "Data de criação", example = "2026-11-17T12:00:00") LocalDateTime dataCriacao,
		@Schema(description = "Data da última alteração", example = "2026-11-17T12:05:00")
		LocalDateTime dataUltimaAtualizacao) {

	public static PagamentoResponse de(PagamentoOutput o) {
		return new PagamentoResponse(
				o.id(),
				o.ordemServicoId(),
				o.orcamentoId(),
				o.valor(),
				o.tentativa(),
				o.statusPagamento(),
				o.urlCheckout(),
				o.expiraEm(),
				o.mpPreferenceId(),
				o.mpPaymentId(),
				o.motivo(),
				o.dataCriacao(),
				o.dataUltimaAtualizacao());
	}
}

