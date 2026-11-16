package br.com.autocenterfiap.billing.adapter.in.consumer;

import br.com.autocenterfiap.billing.adapter.messaging.contract.BillingPayloads;
import br.com.autocenterfiap.billing.adapter.messaging.contract.EventoEnvelope;
import br.com.autocenterfiap.billing.adapter.messaging.contract.TipoEvento;
import br.com.autocenterfiap.billing.application.port.out.EventoProcessadoPort;
import br.com.autocenterfiap.billing.application.usecase.CancelarBillingPorOsUseCase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consome o topico {@code os.eventos} e trata {@code 22 - os.cancelada}.
 * Deduplica por {@code eventId} e propaga o {@code correlationId} no MDC.
 */
@Component
public class OsEventosConsumer {

	private static final Logger log = LoggerFactory.getLogger(OsEventosConsumer.class);

	private final CancelarBillingPorOsUseCase cancelarBillingPorOsUseCase;
	private final EventoProcessadoPort eventoProcessado;
	private final ObjectMapper objectMapper;

	public OsEventosConsumer(
			CancelarBillingPorOsUseCase cancelarBillingPorOsUseCase,
			EventoProcessadoPort eventoProcessado,
			ObjectMapper objectMapper) {
		this.cancelarBillingPorOsUseCase = cancelarBillingPorOsUseCase;
		this.eventoProcessado = eventoProcessado;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = "${billing.kafka.topics.os-eventos}",
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
			if (TipoEvento.OS_CANCELADA.equals(envelope.tipo())) {
				BillingPayloads.OsCancelada p =
						objectMapper.treeToValue(envelope.payload(), BillingPayloads.OsCancelada.class);
				cancelarBillingPorOsUseCase.executar(
						envelope.osId(),
						p != null ? p.motivo() : null);
				eventoProcessado.marcarProcessado(envelope.eventId());
			} else {
				log.debug("Tipo {} nao tratado por os.eventos no Billing; ignorando", envelope.tipo());
			}
		} finally {
			MDC.remove("correlationId");
		}
	}
}

