package br.com.autocenterfiap.billing.adapter.messaging.contract;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventoEnvelopeTest {

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
	}

	@Test
	void deveCriarEnvelopeNovoComVersaoEPayload() {
		var payload = new BillingPayloads.OrcamentoGerado(501L, "165.00");
		EventoEnvelope<BillingPayloads.OrcamentoGerado> envelope =
				EventoEnvelope.novo(TipoEvento.ORCAMENTO_GERADO, 10L, "corr-123", payload);

		assertThat(envelope.eventId()).isNotBlank();
		assertThat(envelope.tipo()).isEqualTo("orcamento.gerado");
		assertThat(envelope.versao()).isEqualTo(1);
		assertThat(envelope.osId()).isEqualTo(10L);
		assertThat(envelope.correlationId()).isEqualTo("corr-123");
		assertThat(envelope.ocorridoEm()).isNotNull();
		assertThat(envelope.payload().orcamentoId()).isEqualTo(501L);
		assertThat(envelope.payload().valorTotal()).isEqualTo("165.00");
	}

	@Test
	void deveDesserializarComandoGerarOrcamento04EIgnorarCamposDesconhecidos() throws Exception {
		String json = """
				{
				  "eventId": "00000000-0000-0000-0000-000000000000",
				  "tipo": "billing.gerar-orcamento",
				  "versao": 1,
				  "osId": 42,
				  "correlationId": "corr-42",
				  "ocorridoEm": "2026-01-01T00:00:00Z",
				  "campoExtra": "ignorado",
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

		EventoEnvelope<JsonNode> envelope = objectMapper.readValue(json, new TypeReference<>() {
		});
		BillingPayloads.GerarOrcamento payload =
				objectMapper.treeToValue(envelope.payload(), BillingPayloads.GerarOrcamento.class);

		assertThat(envelope.tipo()).isEqualTo(TipoEvento.BILLING_GERAR_ORCAMENTO);
		assertThat(envelope.osId()).isEqualTo(42L);
		assertThat(payload.clienteId()).isEqualTo(42L);
		assertThat(payload.valorTotal()).isEqualTo("165.00");
		assertThat(payload.itens().vazio()).isFalse();
		assertThat(payload.itens().calcularTotal()).isEqualByComparingTo(new BigDecimal("165.00"));
	}

	@Test
	void deveDesserializarComandosCobranca07Estorno19ECancelada22() throws Exception {
		String json07 = """
				{
				  "eventId": "id-07",
				  "tipo": "billing.gerar-cobranca",
				  "versao": 1,
				  "osId": 10,
				  "correlationId": "corr-10",
				  "ocorridoEm": "2026-01-01T00:00:00Z",
				  "payload": { "orcamentoId": 501, "valorTotal": "165.00", "tentativa": 1, "validadeMinutos": 30 }
				}
				""";
		EventoEnvelope<BillingPayloads.GerarCobranca> env07 =
				objectMapper.readValue(json07, new TypeReference<>() {
				});
		assertThat(env07.payload().orcamentoId()).isEqualTo(501L);
		assertThat(env07.payload().tentativa()).isEqualTo(1);
		assertThat(env07.payload().validadeMinutos()).isEqualTo(30);

		String json19 = """
				{
				  "eventId": "id-19",
				  "tipo": "billing.estornar-pagamento",
				  "versao": 1,
				  "osId": 10,
				  "correlationId": "corr-10",
				  "ocorridoEm": "2026-01-01T00:00:00Z",
				  "payload": { "pagamentoId": 9001, "motivo": "EXECUCAO_FALHOU" }
				}
				""";
		EventoEnvelope<BillingPayloads.EstornarPagamento> env19 =
				objectMapper.readValue(json19, new TypeReference<>() {
				});
		assertThat(env19.payload().pagamentoId()).isEqualTo(9001L);
		assertThat(env19.payload().motivo()).isEqualTo("EXECUCAO_FALHOU");

		String json22 = """
				{
				  "eventId": "id-22",
				  "tipo": "os.cancelada",
				  "versao": 1,
				  "osId": 10,
				  "correlationId": "corr-10",
				  "ocorridoEm": "2026-01-01T00:00:00Z",
				  "payload": { "motivo": "ORCAMENTO_REPROVADO" }
				}
				""";
		EventoEnvelope<BillingPayloads.OsCancelada> env22 =
				objectMapper.readValue(json22, new TypeReference<>() {
				});
		assertThat(env22.payload().motivo()).isEqualTo("ORCAMENTO_REPROVADO");
	}

	@Test
	void deveCalcularItensEFormatarValoresDeTodosOsPayloadsDeSaida() {
		ItensContrato vazio = new ItensContrato(null, List.of());
		assertThat(vazio.vazio()).isTrue();
		assertThat(vazio.calcularTotal()).isEqualByComparingTo(BigDecimal.ZERO);

		assertThat(BillingPayloads.formatarValor(new BigDecimal("165"))).isEqualTo("165.00");
		assertThat(BillingPayloads.formatarValor(null)).isNull();

		var p06 = new BillingPayloads.OrcamentoAprovado(501L);
		var p08 = new BillingPayloads.PagamentoLinkGerado(9001L, "https://mp/9001", 1, Instant.now());
		var p09 = new BillingPayloads.PagamentoConfirmado(9001L, "165.00");
		var p15 = new BillingPayloads.OrcamentoFalhou("Falha");
		var p16 = new BillingPayloads.OrcamentoReprovado(501L, "Cliente recusou");
		var p17 = new BillingPayloads.PagamentoRecusado(9001L, 1, "Cartao recusado");
		var p20 = new BillingPayloads.PagamentoEstornado(9001L);
		var p21 = new BillingPayloads.PagamentoEstornoFalhou(9001L, "MP indisponivel");

		assertThat(p06.orcamentoId()).isEqualTo(501L);
		assertThat(p08.pagamentoId()).isEqualTo(9001L);
		assertThat(p09.valor()).isEqualTo("165.00");
		assertThat(p15.motivo()).isEqualTo("Falha");
		assertThat(p16.motivo()).isEqualTo("Cliente recusou");
		assertThat(p17.tentativa()).isEqualTo(1);
		assertThat(p20.pagamentoId()).isEqualTo(9001L);
		assertThat(p21.motivo()).isEqualTo("MP indisponivel");
	}
}

