package br.com.autocenterfiap.billing.adapter.in.consumer;

import br.com.autocenterfiap.billing.application.port.out.EventoProcessadoPort;
import br.com.autocenterfiap.billing.application.usecase.EstornarPagamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.GerarCobrancaUseCase;
import br.com.autocenterfiap.billing.application.usecase.GerarOrcamentoUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingComandosConsumerTest {

	@Mock
	private GerarOrcamentoUseCase gerarOrcamentoUseCase;

	@Mock
	private GerarCobrancaUseCase gerarCobrancaUseCase;

	@Mock
	private EstornarPagamentoUseCase estornarPagamentoUseCase;

	@Mock
	private EventoProcessadoPort eventoProcessado;

	private BillingComandosConsumer consumer;

	@BeforeEach
	void setUp() {
		ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
		consumer = new BillingComandosConsumer(
				gerarOrcamentoUseCase,
				gerarCobrancaUseCase,
				estornarPagamentoUseCase,
				eventoProcessado,
				mapper);
	}

	@Test
	void deveProcessarComandoGerarOrcamentoEMarcarProcessado() throws Exception {
		String json = """
				{
				  "eventId": "evt-04",
				  "tipo": "billing.gerar-orcamento",
				  "versao": 1,
				  "osId": 10,
				  "correlationId": "corr-10",
				  "ocorridoEm": "2026-01-01T00:00:00Z",
				  "payload": {
				    "clienteId": 42,
				    "itens": {
				      "servicos": [{ "servicoId": 1, "descricao": "Troca de oleo", "valor": "120.00" }],
				      "produtos": [{ "produtoId": 7, "descricao": "Filtro de oleo", "quantidade": 1, "precoUnitario": "45.00" }]
				    },
				    "valorTotal": "165.00"
				  }
				}
				""";

		when(eventoProcessado.jaProcessado("evt-04")).thenReturn(false);

		consumer.onMensagem(json);

		verify(gerarOrcamentoUseCase).executar(10L, 42L, new BigDecimal("165.00"), "corr-10");
		verify(eventoProcessado).marcarProcessado("evt-04");
	}

	@Test
	void deveProcessarComandoGerarCobrancaEMarcarProcessado() throws Exception {
		String json = """
				{
				  "eventId": "evt-07",
				  "tipo": "billing.gerar-cobranca",
				  "versao": 1,
				  "osId": 10,
				  "correlationId": "corr-10",
				  "ocorridoEm": "2026-01-01T00:00:00Z",
				  "payload": {
				    "orcamentoId": 50,
				    "valorTotal": "165.00",
				    "tentativa": 1,
				    "validadeMinutos": 30
				  }
				}
				""";

		when(eventoProcessado.jaProcessado("evt-07")).thenReturn(false);

		consumer.onMensagem(json);

		verify(gerarCobrancaUseCase).executar(10L, 50L, new BigDecimal("165.00"), 1, 30, "corr-10");
		verify(eventoProcessado).marcarProcessado("evt-07");
	}

	@Test
	void deveProcessarComandoEstornarPagamentoEMarcarProcessado() throws Exception {
		String json = """
				{
				  "eventId": "evt-19",
				  "tipo": "billing.estornar-pagamento",
				  "versao": 1,
				  "osId": 10,
				  "correlationId": "corr-10",
				  "ocorridoEm": "2026-01-01T00:00:00Z",
				  "payload": {
				    "pagamentoId": 90,
				    "motivo": "Execucao falhou"
				  }
				}
				""";

		when(eventoProcessado.jaProcessado("evt-19")).thenReturn(false);

		consumer.onMensagem(json);

		verify(estornarPagamentoUseCase).executar(10L, 90L, "Execucao falhou", "corr-10");
		verify(eventoProcessado).marcarProcessado("evt-19");
	}

	@Test
	void deveCalcularValorPelosItensQuandoValorTotalAusenteEIgnorarRepetido() throws Exception {
		String jsonSemTotal = """
				{
				  "eventId": "evt-04-b",
				  "tipo": "billing.gerar-orcamento",
				  "versao": 1,
				  "osId": 11,
				  "correlationId": "corr-11",
				  "ocorridoEm": "2026-01-01T00:00:00Z",
				  "payload": {
				    "clienteId": 42,
				    "itens": {
				      "servicos": [{ "servicoId": 1, "descricao": "Troca de oleo", "valor": "120.00" }],
				      "produtos": []
				    }
				  }
				}
				""";

		when(eventoProcessado.jaProcessado("evt-04-b")).thenReturn(false);
		consumer.onMensagem(jsonSemTotal);
		verify(gerarOrcamentoUseCase).executar(11L, 42L, new BigDecimal("120.00"), "corr-11");

		when(eventoProcessado.jaProcessado("evt-04-b")).thenReturn(true);
		consumer.onMensagem(jsonSemTotal);
		verify(eventoProcessado).marcarProcessado("evt-04-b");

		String jsonDesconhecido = """
				{
				  "eventId": "evt-x",
				  "tipo": "outro.tipo",
				  "versao": 1,
				  "osId": 11,
				  "correlationId": "corr-11",
				  "ocorridoEm": "2026-01-01T00:00:00Z",
				  "payload": {}
				}
				""";
		when(eventoProcessado.jaProcessado("evt-x")).thenReturn(false);
		consumer.onMensagem(jsonDesconhecido);
		verify(eventoProcessado, never()).marcarProcessado("evt-x");

		consumer.tratarDlt(jsonDesconhecido);
	}
}
