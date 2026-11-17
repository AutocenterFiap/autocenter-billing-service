package br.com.autocenterfiap.billing.application.port.out;

import br.com.autocenterfiap.billing.domain.entity.Orcamento;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Porta de saida para publicacao dos 9 eventos do Billing no topico
 * {@code billing.eventos} (envelope padrao, chave = osId).
 */
public interface BillingEventPublisherPort {

	/** 05 - orcamento.gerado */
	void orcamentoGerado(Orcamento orcamento);

	/** 06 - orcamento.aprovado */
	void orcamentoAprovado(Orcamento orcamento);

	/** 15 - orcamento.falhou */
	void orcamentoFalhou(Long osId, String correlationId, String motivo);

	/** 16 - orcamento.reprovado */
	void orcamentoReprovado(Orcamento orcamento);

	/** 08 - pagamento.link-gerado */
	void pagamentoLinkGerado(Long osId, String correlationId, Long pagamentoId,
	                         String url, Integer tentativa, Instant expiraEm);

	/** 09 - pagamento.confirmado */
	void pagamentoConfirmado(Long osId, String correlationId, Long pagamentoId, BigDecimal valor);

	/** 17 - pagamento.recusado */
	void pagamentoRecusado(Long osId, String correlationId, Long pagamentoId, Integer tentativa, String motivo);

	/** 20 - pagamento.estornado */
	void pagamentoEstornado(Long osId, String correlationId, Long pagamentoId);

	/** 21 - pagamento.estorno-falhou */
	void pagamentoEstornoFalhou(Long osId, String correlationId, Long pagamentoId, String motivo);
}

