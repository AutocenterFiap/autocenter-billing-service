package br.com.autocenterfiap.billing.bdd;

import br.com.autocenterfiap.billing.adapter.messaging.contract.BillingPayloads;
import br.com.autocenterfiap.billing.adapter.messaging.contract.EventoEnvelope;
import br.com.autocenterfiap.billing.adapter.messaging.contract.ItensContrato;
import br.com.autocenterfiap.billing.adapter.messaging.contract.TipoEvento;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BillingSteps {

	private static final Duration TIMEOUT = Duration.ofSeconds(30);

	@LocalServerPort
	private int port;
	@Autowired
	private KafkaTemplate<String, Object> kafka;
	@Autowired
	private BillingEventCollector collector;
	@Autowired
	private ObjectMapper objectMapper;
	@Value("${billing.kafka.topics.billing-comandos}")
	private String topicoBillingComandos;

	private Long orcamentoIdAtual;
	private Long pagamentoIdAtual;

	private RestClient restClient() {
		return RestClient.builder()
				.baseUrl("http://localhost:" + port)
				.requestFactory(new JdkClientHttpRequestFactory())
				.build();
	}

	@Dado("que o servico de OS solicita a geracao de orcamento para a OS {long} no valor de {string}")
	public void solicitarOrcamento(Long osId, String valorTotal) {
		var itens = new ItensContrato(
				List.of(new ItensContrato.Servico(1L, "Troca de oleo", valorTotal)),
				List.of()
		);
		var payload = new BillingPayloads.GerarOrcamento(42L, itens, valorTotal);
		publicar(topicoBillingComandos, TipoEvento.BILLING_GERAR_ORCAMENTO, osId, payload);
	}

	@Entao("o orcamento da OS {long} fica com status {string}")
	public void verificarStatusOrcamento(Long osId, String statusEsperado) {
		await().atMost(TIMEOUT).untilAsserted(() -> {
			ResponseEntity<String> r = restClient().get()
					.uri("/v1/orcamentos/os/{osId}", osId)
					.retrieve()
					.toEntity(String.class);
			assertEquals(HttpStatus.OK, r.getStatusCode());
			JsonNode array = objectMapper.readTree(r.getBody());
			assertTrue(array.isArray() && !array.isEmpty(), "Nenhum orcamento encontrado para OS " + osId);
			JsonNode orcamento = array.get(0);
			orcamentoIdAtual = orcamento.get("id").asLong();
			assertEquals(statusEsperado, orcamento.path("statusOrcamento").asText());
		});
	}

	@Quando("o cliente aprova o orcamento da OS {long}")
	public void aprovarOrcamento(Long osId) {
		ResponseEntity<String> r = restClient().patch()
				.uri("/v1/orcamentos/{id}/aprovar", orcamentoIdAtual)
				.retrieve()
				.toEntity(String.class);
		assertEquals(HttpStatus.OK, r.getStatusCode());
	}

	@Quando("o servico de OS solicita a geracao de cobranca para a OS {long} no valor de {string}")
	public void solicitarCobranca(Long osId, String valorTotal) {
		var payload = new BillingPayloads.GerarCobranca(orcamentoIdAtual, valorTotal, 1, 30);
		publicar(topicoBillingComandos, TipoEvento.BILLING_GERAR_COBRANCA, osId, payload);
	}

	@Entao("o pagamento da OS {long} fica com status {string}")
	public void verificarStatusPagamento(Long osId, String statusEsperado) {
		await().atMost(TIMEOUT).untilAsserted(() -> {
			ResponseEntity<String> r = restClient().get()
					.uri("/v1/pagamentos/os/{osId}", osId)
					.retrieve()
					.toEntity(String.class);
			assertEquals(HttpStatus.OK, r.getStatusCode());
			JsonNode node = objectMapper.readTree(r.getBody());
			JsonNode pagamento = node.isArray() ? node.get(0) : node;
			assertTrue(pagamento != null && pagamento.has("id"), "Nenhum pagamento encontrado para OS " + osId);
			pagamentoIdAtual = pagamento.get("id").asLong();
			assertEquals(statusEsperado, pagamento.path("statusPagamento").asText());
		});
	}

	@Quando("o pagamento da OS {long} e confirmado")
	public void confirmarPagamento(Long osId) {
		ResponseEntity<String> r = restClient().post()
				.uri("/v1/pagamentos/{id}/confirmar", pagamentoIdAtual)
				.retrieve()
				.toEntity(String.class);
		assertEquals(HttpStatus.OK, r.getStatusCode());
	}

	@Entao("o evento {string} e publicado para a OS {long}")
	public void eventoPublicado(String tipo, Long osId) {
		await().atMost(TIMEOUT).untilAsserted(() ->
				assertTrue(collector.contem(osId, tipo),
						"Evento " + tipo + " nao recebido para a OS " + osId));
	}

	private void publicar(String topico, String tipo, Long osId, Object payload) {
		EventoEnvelope<Object> envelope = EventoEnvelope.novo(tipo, osId, UUID.randomUUID().toString(), payload);
		kafka.send(topico, String.valueOf(osId), envelope);
	}
}
