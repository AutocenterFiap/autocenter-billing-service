package br.com.autocenterfiap.billing.adapter.out.messaging;

import br.com.autocenterfiap.billing.adapter.messaging.contract.EventoEnvelope;
import br.com.autocenterfiap.billing.adapter.messaging.contract.TipoEvento;
import br.com.autocenterfiap.billing.domain.entity.Orcamento;
import br.com.autocenterfiap.billing.domain.enums.StatusOrcamento;
import br.com.autocenterfiap.billing.infrastructure.config.KafkaTopicsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BillingKafkaPublisherTest {

	@Mock
	private KafkaTemplate<String, Object> kafkaTemplate;

	private BillingKafkaPublisher publisher;

	@BeforeEach
	void setUp() {
		KafkaTopicsProperties topics = new KafkaTopicsProperties(
				"os.eventos", "billing.comandos", "billing.eventos");
		publisher = new BillingKafkaPublisher(kafkaTemplate, topics);
	}

	@Test
	void devePublicarTodosOsEventosDoBillingNoTopicoBillingEventos() {
		Orcamento orcamento = Orcamento.builder()
				.id(501L)
				.ordemServicoId(10L)
				.correlationId("corr-10")
				.valorTotal(new BigDecimal("165.00"))
				.statusOrcamento(StatusOrcamento.AGUARDANDO_APROVACAO)
				.motivo("Cliente recusou o valor")
				.build();

		publisher.orcamentoGerado(orcamento);
		publisher.orcamentoAprovado(orcamento);
		publisher.orcamentoFalhou(10L, "corr-10", "Falha");
		publisher.orcamentoReprovado(orcamento);
		publisher.pagamentoLinkGerado(10L, "corr-10", 9001L, "https://mp/9001", 1, Instant.now());
		publisher.pagamentoConfirmado(10L, "corr-10", 9001L, new BigDecimal("165.00"));
		publisher.pagamentoRecusado(10L, "corr-10", 9001L, 1, "Cartao recusado");
		publisher.pagamentoEstornado(10L, "corr-10", 9001L);
		publisher.pagamentoEstornoFalhou(10L, "corr-10", 9001L, "MP indisponivel");

		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		verify(kafkaTemplate, times(9)).send(eq("billing.eventos"), eq("10"), captor.capture());

		assertThat(captor.getAllValues())
				.extracting(v -> ((EventoEnvelope<?>) v).tipo())
				.containsExactly(
						TipoEvento.ORCAMENTO_GERADO,
						TipoEvento.ORCAMENTO_APROVADO,
						TipoEvento.ORCAMENTO_FALHOU,
						TipoEvento.ORCAMENTO_REPROVADO,
						TipoEvento.PAGAMENTO_LINK_GERADO,
						TipoEvento.PAGAMENTO_CONFIRMADO,
						TipoEvento.PAGAMENTO_RECUSADO,
						TipoEvento.PAGAMENTO_ESTORNADO,
						TipoEvento.PAGAMENTO_ESTORNO_FALHOU);
	}
}

