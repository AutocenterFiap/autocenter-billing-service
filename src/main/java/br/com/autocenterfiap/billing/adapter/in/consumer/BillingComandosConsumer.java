package br.com.autocenterfiap.billing.adapter.in.consumer;

import br.com.autocenterfiap.billing.adapter.messaging.contract.BillingPayloads;
import br.com.autocenterfiap.billing.adapter.messaging.contract.EventoEnvelope;
import br.com.autocenterfiap.billing.adapter.messaging.contract.TipoEvento;
import br.com.autocenterfiap.billing.application.port.out.EventoProcessadoPort;
import br.com.autocenterfiap.billing.application.usecase.GerarOrcamentoUseCase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Consome o topico {@code billing.comandos} e roteia por {@code tipo}.
 * Deduplica por {@code eventId} e propaga o {@code correlationId} no MDC.
 */
@Component
public class BillingComandosConsumer {

	private static final Logger log = LoggerFactory.getLogger(BillingComandosConsumer.class);

	private final GerarOrcamentoUseCase gerarOrcamentoUseCase;
	private final EventoProcessadoPort eventoProcessado;
	private final ObjectMapper objectMapper;

	public BillingComandosConsumer(GerarOrcamentoUseCase gerarOrcamentoUseCase,
	                               EventoProcessadoPort eventoProcessado,
	                               ObjectMapper objectMapper) {
		this.gerarOrcamentoUseCase = gerarOrcamentoUseCase;
		this.eventoProcessado = eventoProcessado;
		this.objectMapper = objectMapper;
	}

	@RetryableTopic(
			attempts = "4",
			backoff = @Backoff(delay = 2000, multiplier = 2.0),
			dltStrategy = DltStrategy.FAIL_ON_ERROR,
			autoCreateTopics = "true")
	@KafkaListener(topics = "${billing.kafka.topics.billing-comandos}",
			groupId = "${spring.kafka.consumer.group-id}")
	public void onMensagem(String mensagem) throws Exception {
		EventoEnvelope<JsonNode> envelope = objectMapper.readValue(mensagem, new TypeReference<>() {
		});

		if (eventoProcessado.jaProcessado(envelope.eventId())) {
			log.debug("Evento {} ja processado; ignorando", envelope.eventId());
			return;
		}

		MDC.put("correlationId", envelope.correlationId());
		try {
			boolean tratado = rotear(envelope);
			if (tratado) {
				eventoProcessado.marcarProcessado(envelope.eventId());
			}
		} finally {
			MDC.remove("correlationId");
		}
	}

	private boolean rotear(EventoEnvelope<JsonNode> envelope) throws Exception {
		if (TipoEvento.BILLING_GERAR_ORCAMENTO.equals(envelope.tipo())) {
			BillingPayloads.GerarOrcamento p =
					objectMapper.treeToValue(envelope.payload(), BillingPayloads.GerarOrcamento.class);
			BigDecimal valorTotal = extrairValorTotal(p);
			gerarOrcamentoUseCase.executar(
					envelope.osId(),
					p != null ? p.clienteId() : null,
					valorTotal,
					envelope.correlationId());
			return true;
		}
		log.debug("Tipo {} nao tratado por billing.comandos; ignorando", envelope.tipo());
		return false;
	}

	private BigDecimal extrairValorTotal(BillingPayloads.GerarOrcamento payload) {
		if (payload == null) {
			return null;
		}
		if (payload.itens() != null && payload.itens().vazio()) {
			return BigDecimal.ZERO;
		}
		if (payload.valorTotal() != null && !payload.valorTotal().isBlank()) {
			try {
				return new BigDecimal(payload.valorTotal());
			} catch (NumberFormatException ex) {
				return null;
			}
		}
		if (payload.itens() != null) {
			return payload.itens().calcularTotal();
		}
		return null;
	}

	@DltHandler
	public void tratarDlt(String mensagem) {
		log.error("Mensagem de billing.comandos enviada para a DLT: {}", mensagem);
	}
}

