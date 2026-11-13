package br.com.autocenterfiap.billing.bdd;

import br.com.autocenterfiap.billing.adapter.messaging.contract.EventoEnvelope;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Coleta os eventos publicados em {@code billing.eventos} para verificacao nos
 * cenarios BDD.
 */
@Component
public class BillingEventCollector {

	private final ObjectMapper objectMapper;
	private final List<EventoEnvelope<JsonNode>> eventos = new CopyOnWriteArrayList<>();

	public BillingEventCollector(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = "${billing.kafka.topics.billing-eventos}", groupId = "bdd-billing-verifier")
	public void onEvento(String mensagem) throws Exception {
		eventos.add(objectMapper.readValue(mensagem, new TypeReference<>() {
		}));
	}

	public boolean contem(Long osId, String tipo) {
		return eventos.stream()
				.anyMatch(e -> osId.equals(e.osId()) && tipo.equals(e.tipo()));
	}
}

