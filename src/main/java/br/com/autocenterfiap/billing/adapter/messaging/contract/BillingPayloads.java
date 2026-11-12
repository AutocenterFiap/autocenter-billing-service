package br.com.autocenterfiap.billing.adapter.messaging.contract;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Payloads das mensagens que o Billing consome e publica, exatamente conforme os
 * arquivos JSON de exemplo do contrato da Saga.
 */
public final class BillingPayloads {

	private BillingPayloads() {
	}

	public static String formatarValor(BigDecimal valor) {
		return valor == null ? null : valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	// =========================================================================
	// Consumidos pelo Billing (04, 07, 19, 22)
	// =========================================================================

	/** 04 - billing.gerar-orcamento (topico: billing.comandos) */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record GerarOrcamento(Long clienteId, ItensContrato itens, String valorTotal) {
	}

	/** 07 - billing.gerar-cobranca (topico: billing.comandos) */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record GerarCobranca(Long orcamentoId, String valorTotal, Integer tentativa, Integer validadeMinutos) {
	}

	/** 19 - billing.estornar-pagamento (topico: billing.comandos) */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record EstornarPagamento(Long pagamentoId, String motivo) {
	}

	/** 22 - os.cancelada (topico: os.eventos) */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record OsCancelada(String motivo) {
	}

	// =========================================================================
	// Publicados pelo Billing (05, 06, 08, 09, 15, 16, 17, 20, 21)
	// =========================================================================

	/** 05 - orcamento.gerado */
	public record OrcamentoGerado(Long orcamentoId, String valorTotal) {
	}

	/** 06 - orcamento.aprovado */
	public record OrcamentoAprovado(Long orcamentoId) {
	}

	/** 08 - pagamento.link-gerado */
	public record PagamentoLinkGerado(
			Long pagamentoId,
			String url,
			Integer tentativa,
			@JsonFormat(shape = JsonFormat.Shape.STRING)
			Instant expiraEm) {
	}

	/** 09 - pagamento.confirmado */
	public record PagamentoConfirmado(Long pagamentoId, String valor) {
	}

	/** 15 - orcamento.falhou */
	public record OrcamentoFalhou(String motivo) {
	}

	/** 16 - orcamento.reprovado */
	public record OrcamentoReprovado(Long orcamentoId, String motivo) {
	}

	/** 17 - pagamento.recusado */
	public record PagamentoRecusado(Long pagamentoId, Integer tentativa, String motivo) {
	}

	/** 20 - pagamento.estornado */
	public record PagamentoEstornado(Long pagamentoId) {
	}

	/** 21 - pagamento.estorno-falhou */
	public record PagamentoEstornoFalhou(Long pagamentoId, String motivo) {
	}
}

