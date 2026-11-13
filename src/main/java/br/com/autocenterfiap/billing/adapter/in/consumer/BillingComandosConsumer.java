package br.com.autocenterfiap.billing.adapter.in.consumer;

import br.com.autocenterfiap.billing.adapter.messaging.contract.BillingPayloads;
import br.com.autocenterfiap.billing.adapter.messaging.contract.EventoEnvelope;
import br.com.autocenterfiap.billing.adapter.messaging.contract.TipoEvento;
import br.com.autocenterfiap.billing.application.port.out.EventoProcessadoPort;
import br.com.autocenterfiap.billing.application.usecase.EstornarPagamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.GerarCobrancaUseCase;
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
	private final GerarCobrancaUseCase gerarCobrancaUseCase;
	private final EstornarPagamentoUseCase estornarPagamentoUseCase;
	private final EventoProcessadoPort eventoProcessado;
	private final ObjectMapper objectMapper;

	public BillingComandosConsumer(
			GerarOrcamentoUseCase gerarOrcamentoUseCase,
			GerarCobrancaUseCase gerarCobrancaUseCase,
			EstornarPagamentoUseCase estornarPagamentoUseCase,
			EventoProcessadoPort eventoProcessado,
			ObjectMapper objectMapper) {
		this.gerarOrcamentoUseCase = gerarOrcamentoUseCase;
		this.gerarCobrancaUseCase = gerarCobrancaUseCase;
		this.estornarPagamentoUseCase = estornarPagamentoUseCase;
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
			BigDecimal valorTotal = extrairValorTotalOrcamento(p);
			gerarOrcamentoUseCase.executar(
					envelope.osId(),
					p != null ? p.clienteId() : null,
					valorTotal,
					envelope.correlationId());
			return true;
		}
		if (TipoEvento.BILLING_GERAR_COBRANCA.equals(envelope.tipo())) {
			BillingPayloads.GerarCobranca p =
					objectMapper.treeToValue(envelope.payload(), BillingPayloads.GerarCobranca.class);
			BigDecimal valorTotal = parseDecimalSeguro(p != null ? p.valorTotal() : null);
			gerarCobrancaUseCase.executar(
					envelope.osId(),
					p != null ? p.orcamentoId() : null,
					valorTotal,
					p != null ? p.tentativa() : null,
					p != null ? p.validadeMinutos() : null,
					envelope.correlationId());
			return true;
		}
		if (TipoEvento.BILLING_ESTORNAR_PAGAMENTO.equals(envelope.tipo())) {
			BillingPayloads.EstornarPagamento p =
					objectMapper.treeToValue(envelope.payload(), BillingPayloads.EstornarPagamento.class);
			estornarPagamentoUseCase.executar(
					envelope.osId(),
					p != null ? p.pagamentoId() : null,
					p != null ? p.motivo() : null,
					envelope.correlationId());
			return true;
		}
		log.debug("Tipo {} nao tratado por billing.comandos; ignorando", envelope.tipo());
		return false;
	}

	private BigDecimal extrairValorTotalOrcamento(BillingPayloads.GerarOrcamento payload) {
		if (payload == null) {
			return null;
		}
		if (payload.itens() != null && payload.itens().vazio()) {
			return BigDecimal.ZERO;
		}
		BigDecimal informado = parseDecimalSeguro(payload.valorTotal());
		if (informado != null) {
			return informado;
		}
		if (payload.itens() != null) {
			return payload.itens().calcularTotal();
		}
		return null;
	}

	private BigDecimal parseDecimalSeguro(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		try {
			return new BigDecimal(valor.trim());
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	@DltHandler
	public void tratarDlt(String mensagem) {
		log.error("Mensagem de billing.comandos enviada para a DLT: {}", mensagem);
	}
}
