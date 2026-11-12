package br.com.autocenterfiap.billing.adapter.in.controller;

import br.com.autocenterfiap.billing.adapter.in.dto.GerarCobrancaRequest;
import br.com.autocenterfiap.billing.adapter.in.dto.MercadoPagoWebhookRequest;
import br.com.autocenterfiap.billing.adapter.in.dto.PagamentoResponse;
import br.com.autocenterfiap.billing.adapter.in.dto.RecusarPagamentoRequest;
import br.com.autocenterfiap.billing.application.dto.PagamentoOutput;
import br.com.autocenterfiap.billing.application.usecase.BuscarPagamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.EstornarPagamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.GerarCobrancaUseCase;
import br.com.autocenterfiap.billing.application.usecase.ProcessarPagamentoUseCase;
import br.com.autocenterfiap.billing.domain.enums.StatusPagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagamentoControllerTest {

	@Mock
	private GerarCobrancaUseCase gerarCobrancaUseCase;

	@Mock
	private BuscarPagamentoUseCase buscarPagamentoUseCase;

	@Mock
	private ProcessarPagamentoUseCase processarPagamentoUseCase;

	@Mock
	private EstornarPagamentoUseCase estornarPagamentoUseCase;

	private PagamentoController controller;

	@BeforeEach
	void setUp() {
		controller = new PagamentoController(
				gerarCobrancaUseCase,
				buscarPagamentoUseCase,
				processarPagamentoUseCase,
				estornarPagamentoUseCase);
	}

	private PagamentoOutput exemplo(StatusPagamento status) {
		return new PagamentoOutput(
				90L,
				10L,
				50L,
				new BigDecimal("165.00"),
				1,
				status,
				"https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=pref-90",
				Instant.parse("2026-01-01T00:30:00Z"),
				"pref-90",
				"777",
				null,
				LocalDateTime.now(),
				LocalDateTime.now());
	}

	@Test
	void deveGerarCobrancaEBuscarPagamentoPorIdEPorOs() {
		when(gerarCobrancaUseCase.executar(eq(10L), eq(50L), eq(new BigDecimal("165.00")), eq(1), eq(30), any()))
				.thenReturn(exemplo(StatusPagamento.PENDENTE));
		when(buscarPagamentoUseCase.buscarPorId(90L)).thenReturn(exemplo(StatusPagamento.PENDENTE));
		when(buscarPagamentoUseCase.buscarMaisRecentePorOsId(10L)).thenReturn(exemplo(StatusPagamento.PENDENTE));

		ResponseEntity<PagamentoResponse> criado = controller.gerarCobranca(
				new GerarCobrancaRequest(10L, 50L, new BigDecimal("165.00"), 1, 30));
		ResponseEntity<PagamentoResponse> porId = controller.buscarPorId(90L);
		ResponseEntity<PagamentoResponse> porOs = controller.buscarPorOsId(10L);

		assertThat(criado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(criado.getBody().urlCheckout()).contains("pref-90");
		assertThat(porId.getBody().id()).isEqualTo(90L);
		assertThat(porOs.getBody().ordemServicoId()).isEqualTo(10L);
	}

	@Test
	void deveProcessarWebhookMercadoPagoEConfirmarERecusarManualmente() {
		when(processarPagamentoUseCase.processarNotificacaoWebhook("777"))
				.thenReturn(Optional.of(exemplo(StatusPagamento.CONFIRMADO)));

		MercadoPagoWebhookRequest body = new MercadoPagoWebhookRequest(
				null, "payment", null, "payment.updated", new MercadoPagoWebhookRequest.WebhookData("777"));

		ResponseEntity<Map<String, Object>> webhookResp =
				controller.receberWebhookMercadoPago(null, null, null, null, body);
		assertThat(webhookResp.getBody()).containsEntry("processado", true);
		assertThat(webhookResp.getBody()).containsEntry("pagamentoId", 90L);

		// Notificacao de outro topico (ex.: merchant_order) deve retornar processado=false
		ResponseEntity<Map<String, Object>> ignorado =
				controller.receberWebhookMercadoPago("merchant_order", null, "999", null, null);
		assertThat(ignorado.getBody()).containsEntry("processado", false);

		when(processarPagamentoUseCase.confirmar(90L, "777")).thenReturn(exemplo(StatusPagamento.CONFIRMADO));
		ResponseEntity<PagamentoResponse> confirmado = controller.confirmar(90L, "777");
		assertThat(confirmado.getBody().statusPagamento()).isEqualTo(StatusPagamento.CONFIRMADO);

		when(processarPagamentoUseCase.recusar(90L, "888", "Cartao recusado"))
				.thenReturn(exemplo(StatusPagamento.RECUSADO));
		ResponseEntity<PagamentoResponse> recusado = controller.recusar(
				90L, new RecusarPagamentoRequest("Cartao recusado", "888"));
		assertThat(recusado.getBody().statusPagamento()).isEqualTo(StatusPagamento.RECUSADO);
		verify(processarPagamentoUseCase).recusar(90L, "888", "Cartao recusado");

		when(estornarPagamentoUseCase.executar(null, 90L, "OS cancelada", null))
				.thenReturn(exemplo(StatusPagamento.ESTORNADO));
		ResponseEntity<PagamentoResponse> estornado = controller.estornar(90L, "OS cancelada");
		assertThat(estornado.getBody().statusPagamento()).isEqualTo(StatusPagamento.ESTORNADO);
	}
}
