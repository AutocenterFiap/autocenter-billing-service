package br.com.autocenterfiap.billing.adapter.in.dto;

import br.com.autocenterfiap.billing.application.dto.PagamentoOutput;
import br.com.autocenterfiap.billing.domain.enums.StatusPagamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record PagamentoResponse(
		Long id,
		Long ordemServicoId,
		Long orcamentoId,
		BigDecimal valor,
		Integer tentativa,
		StatusPagamento statusPagamento,
		String urlCheckout,
		Instant expiraEm,
		String mpPreferenceId,
		String mpPaymentId,
		String motivo,
		LocalDateTime dataCriacao,
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

