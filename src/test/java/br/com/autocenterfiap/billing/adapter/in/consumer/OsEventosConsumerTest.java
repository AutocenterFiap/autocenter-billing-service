package br.com.autocenterfiap.billing.adapter.in.consumer;

import br.com.autocenterfiap.billing.application.port.out.EventoProcessadoPort;
import br.com.autocenterfiap.billing.application.usecase.CancelarBillingPorOsUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OsEventosConsumerTest {

	@Mock
	private CancelarBillingPorOsUseCase cancelarBillingPorOsUseCase;

	@Mock
	private EventoProcessadoPort eventoProcessado;

	private OsEventosConsumer consumer;

	@BeforeEach
	void setUp() {
		ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
		consumer = new OsEventosConsumer(cancelarBillingPorOsUseCase, eventoProcessado, mapper);
	}

	@Test
	void deveProcessarOsCanceladaEIgnorarRepetidoOuOutroTipo() throws Exception {
		String json = """
				{
				  "eventId": "evt-22",
				  "tipo": "os.cancelada",
				  "versao": 1,
				  "osId": 10,
				  "correlationId": "corr-10",
				  "ocorridoEm": "2026-01-01T00:00:00Z",
				  "payload": { "motivo": "Cliente desistiu" }
				}
				""";

		when(eventoProcessado.jaProcessado("evt-22")).thenReturn(false);
		consumer.onMensagem(json);

		verify(cancelarBillingPorOsUseCase).executar(10L, "Cliente desistiu");
		verify(eventoProcessado).marcarProcessado("evt-22");

		when(eventoProcessado.jaProcessado("evt-22")).thenReturn(true);
		consumer.onMensagem(json);
		verify(cancelarBillingPorOsUseCase).executar(10L, "Cliente desistiu");

		String outro = """
				{
				  "eventId": "evt-01",
				  "tipo": "os.criada",
				  "versao": 1,
				  "osId": 10,
				  "correlationId": "corr-10",
				  "ocorridoEm": "2026-01-01T00:00:00Z",
				  "payload": {}
				}
				""";
		when(eventoProcessado.jaProcessado("evt-01")).thenReturn(false);
		consumer.onMensagem(outro);
		verify(eventoProcessado, never()).marcarProcessado("evt-01");

		consumer.tratarDlt(outro);
	}
}

