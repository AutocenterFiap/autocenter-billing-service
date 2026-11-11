package br.com.autocenterfiap.billing.application.dto;

import br.com.autocenterfiap.billing.domain.entity.Pagamento;
import br.com.autocenterfiap.billing.domain.enums.StatusPagamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record PagamentoOutput(
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

	public static PagamentoOutput de(Pagamento p) {
		return new PagamentoOutput(
				p.getId(),
				p.getOrdemServicoId(),
				p.getOrcamentoId(),
				p.getValor(),
				p.getTentativa(),
				p.getStatusPagamento(),
				p.getUrlCheckout(),
				p.getExpiraEm(),
				p.getMpPreferenceId(),
				p.getMpPaymentId(),
				p.getMotivo(),
				p.getDataCriacao(),
				p.getDataUltimaAtualizacao());
	}
}

