package br.com.autocenterfiap.billing.adapter.out.messaging;

import br.com.autocenterfiap.billing.adapter.messaging.contract.BillingPayloads;
import br.com.autocenterfiap.billing.adapter.messaging.contract.EventoEnvelope;
import br.com.autocenterfiap.billing.adapter.messaging.contract.TipoEvento;
import br.com.autocenterfiap.billing.application.port.out.BillingEventPublisherPort;
import br.com.autocenterfiap.billing.domain.entity.Orcamento;
import br.com.autocenterfiap.billing.infrastructure.config.KafkaTopicsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Publica os eventos do Billing no topico {@code billing.eventos}, usando o envelope
 * padrao e o {@code osId} como chave Kafka (mesmo padrao do ExecucaoKafkaPublisher).
 */
@Component
public class BillingKafkaPublisher implements BillingEventPublisherPort {

	private static final Logger log = LoggerFactory.getLogger(BillingKafkaPublisher.class);

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final KafkaTopicsProperties topics;

	public BillingKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate, KafkaTopicsProperties topics) {
		this.kafkaTemplate = kafkaTemplate;
		this.topics = topics;
	}

	@Override
	public void orcamentoGerado(Orcamento orcamento) {
		publicar(TipoEvento.ORCAMENTO_GERADO, orcamento.getOrdemServicoId(), orcamento.getCorrelationId(),
				new BillingPayloads.OrcamentoGerado(
						orcamento.getId(),
						BillingPayloads.formatarValor(orcamento.getValorTotal())));
	}

	@Override
	public void orcamentoAprovado(Orcamento orcamento) {
		publicar(TipoEvento.ORCAMENTO_APROVADO, orcamento.getOrdemServicoId(), orcamento.getCorrelationId(),
				new BillingPayloads.OrcamentoAprovado(orcamento.getId()));
	}

	@Override
	public void orcamentoFalhou(Long osId, String correlationId, String motivo) {
		publicar(TipoEvento.ORCAMENTO_FALHOU, osId, correlationId,
				new BillingPayloads.OrcamentoFalhou(motivo));
	}

	@Override
	public void orcamentoReprovado(Orcamento orcamento) {
		publicar(TipoEvento.ORCAMENTO_REPROVADO, orcamento.getOrdemServicoId(), orcamento.getCorrelationId(),
				new BillingPayloads.OrcamentoReprovado(orcamento.getId(), orcamento.getMotivo()));
	}

	@Override
	public void pagamentoLinkGerado(Long osId, String correlationId, Long pagamentoId,
	                                String url, Integer tentativa, Instant expiraEm) {
		publicar(TipoEvento.PAGAMENTO_LINK_GERADO, osId, correlationId,
				new BillingPayloads.PagamentoLinkGerado(pagamentoId, url, tentativa, expiraEm));
	}

	@Override
	public void pagamentoConfirmado(Long osId, String correlationId, Long pagamentoId, BigDecimal valor) {
		publicar(TipoEvento.PAGAMENTO_CONFIRMADO, osId, correlationId,
				new BillingPayloads.PagamentoConfirmado(pagamentoId, BillingPayloads.formatarValor(valor)));
	}

	@Override
	public void pagamentoRecusado(Long osId, String correlationId, Long pagamentoId, Integer tentativa, String motivo) {
		publicar(TipoEvento.PAGAMENTO_RECUSADO, osId, correlationId,
				new BillingPayloads.PagamentoRecusado(pagamentoId, tentativa, motivo));
	}

	@Override
	public void pagamentoEstornado(Long osId, String correlationId, Long pagamentoId) {
		publicar(TipoEvento.PAGAMENTO_ESTORNADO, osId, correlationId,
				new BillingPayloads.PagamentoEstornado(pagamentoId));
	}

	@Override
	public void pagamentoEstornoFalhou(Long osId, String correlationId, Long pagamentoId, String motivo) {
		publicar(TipoEvento.PAGAMENTO_ESTORNO_FALHOU, osId, correlationId,
				new BillingPayloads.PagamentoEstornoFalhou(pagamentoId, motivo));
	}

	private void publicar(String tipo, Long osId, String correlationId, Object payload) {
		EventoEnvelope<Object> envelope = EventoEnvelope.novo(tipo, osId, correlationId, payload);
		log.info("Publicando {} para OS {} em {}", tipo, osId, topics.billingEventos());
		kafkaTemplate.send(topics.billingEventos(), String.valueOf(osId), envelope);
	}
}

