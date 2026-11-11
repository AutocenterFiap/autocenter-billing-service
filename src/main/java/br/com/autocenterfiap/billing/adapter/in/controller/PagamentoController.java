package br.com.autocenterfiap.billing.adapter.in.controller;

import br.com.autocenterfiap.billing.adapter.in.dto.GerarCobrancaRequest;
import br.com.autocenterfiap.billing.adapter.in.dto.MercadoPagoWebhookRequest;
import br.com.autocenterfiap.billing.adapter.in.dto.PagamentoResponse;
import br.com.autocenterfiap.billing.adapter.in.dto.RecusarPagamentoRequest;
import br.com.autocenterfiap.billing.application.dto.PagamentoOutput;
import br.com.autocenterfiap.billing.application.usecase.BuscarPagamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.GerarCobrancaUseCase;
import br.com.autocenterfiap.billing.application.usecase.ProcessarPagamentoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v1/pagamentos")
@Tag(name = "Pagamento", description = "Endpoints de geracao de cobranca Checkout Pro, consulta, webhook Mercado Pago e simulacao")
public class PagamentoController {

	private static final Logger log = LoggerFactory.getLogger(PagamentoController.class);

	private final GerarCobrancaUseCase gerarCobrancaUseCase;
	private final BuscarPagamentoUseCase buscarPagamentoUseCase;
	private final ProcessarPagamentoUseCase processarPagamentoUseCase;

	public PagamentoController(
			GerarCobrancaUseCase gerarCobrancaUseCase,
			BuscarPagamentoUseCase buscarPagamentoUseCase,
			ProcessarPagamentoUseCase processarPagamentoUseCase) {
		this.gerarCobrancaUseCase = gerarCobrancaUseCase;
		this.buscarPagamentoUseCase = buscarPagamentoUseCase;
		this.processarPagamentoUseCase = processarPagamentoUseCase;
	}

	@PostMapping
	@Operation(summary = "Gera cobranca e link Checkout Pro no Mercado Pago para um orcamento")
	public ResponseEntity<PagamentoResponse> gerarCobranca(
			@Valid @RequestBody GerarCobrancaRequest request) {
		PagamentoOutput output = gerarCobrancaUseCase.executar(
				request.osId(),
				request.orcamentoId(),
				request.valorTotal(),
				request.tentativa(),
				request.validadeMinutos(),
				UUID.randomUUID().toString());
		return ResponseEntity.status(HttpStatus.CREATED).body(PagamentoResponse.de(output));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Busca pagamento por ID")
	public ResponseEntity<PagamentoResponse> buscarPorId(@PathVariable Long id) {
		PagamentoOutput output = buscarPagamentoUseCase.buscarPorId(id);
		return ResponseEntity.ok(PagamentoResponse.de(output));
	}

	@GetMapping("/os/{osId}")
	@Operation(summary = "Busca o pagamento mais recente de uma Ordem de Servico")
	public ResponseEntity<PagamentoResponse> buscarPorOsId(@PathVariable Long osId) {
		PagamentoOutput output = buscarPagamentoUseCase.buscarMaisRecentePorOsId(osId);
		return ResponseEntity.ok(PagamentoResponse.de(output));
	}

	@PostMapping("/webhook/mercadopago")
	@Operation(summary = "Recebe notificacoes Webhook/IPN do Mercado Pago (Checkout Pro)")
	public ResponseEntity<Map<String, Object>> receberWebhookMercadoPago(
			@RequestParam(name = "type", required = false) String queryType,
			@RequestParam(name = "topic", required = false) String queryTopic,
			@RequestParam(name = "data.id", required = false) String queryDataId,
			@RequestParam(name = "id", required = false) String queryId,
			@RequestBody(required = false) MercadoPagoWebhookRequest body) {

		String tipo = resolverTipoNotificacao(queryType, queryTopic, body);
		String mpPaymentId = resolverMpPaymentId(queryDataId, queryId, body);

		log.info("Webhook Mercado Pago recebido: tipo={} mpPaymentId={}", tipo, mpPaymentId);

		if (mpPaymentId == null || mpPaymentId.isBlank()
				|| (tipo != null && !tipo.isBlank() && !"payment".equalsIgnoreCase(tipo))) {
			return ResponseEntity.ok(Map.of("recebido", true, "processado", false));
		}

		Optional<PagamentoOutput> resultado = processarPagamentoUseCase.processarNotificacaoWebhook(mpPaymentId);
		if (resultado.isPresent()) {
			return ResponseEntity.ok(Map.of(
					"recebido", true,
					"processado", true,
					"pagamentoId", resultado.get().id(),
					"status", resultado.get().statusPagamento().name()));
		}
		return ResponseEntity.ok(Map.of("recebido", true, "processado", false));
	}

	@PostMapping("/{id}/confirmar")
	@Operation(summary = "Confirma manualmente o pagamento e publica pagamento.confirmado na Saga")
	public ResponseEntity<PagamentoResponse> confirmar(
			@PathVariable Long id,
			@RequestParam(name = "mpPaymentId", required = false) String mpPaymentId) {
		PagamentoOutput output = processarPagamentoUseCase.confirmar(id, mpPaymentId);
		return ResponseEntity.ok(PagamentoResponse.de(output));
	}

	@PostMapping("/{id}/recusar")
	@Operation(summary = "Recusa manualmente o pagamento e publica pagamento.recusado na Saga")
	public ResponseEntity<PagamentoResponse> recusar(
			@PathVariable Long id,
			@RequestBody(required = false) RecusarPagamentoRequest request) {
		String motivo = request != null ? request.motivo() : null;
		String mpPaymentId = request != null ? request.mpPaymentId() : null;
		PagamentoOutput output = processarPagamentoUseCase.recusar(id, mpPaymentId, motivo);
		return ResponseEntity.ok(PagamentoResponse.de(output));
	}

	private String resolverTipoNotificacao(String queryType, String queryTopic, MercadoPagoWebhookRequest body) {
		if (queryType != null && !queryType.isBlank()) {
			return queryType;
		}
		if (queryTopic != null && !queryTopic.isBlank()) {
			return queryTopic;
		}
		if (body != null) {
			if (body.type() != null && !body.type().isBlank()) {
				return body.type();
			}
			if (body.topic() != null && !body.topic().isBlank()) {
				return body.topic();
			}
		}
		return null;
	}

	private String resolverMpPaymentId(String queryDataId, String queryId, MercadoPagoWebhookRequest body) {
		if (queryDataId != null && !queryDataId.isBlank()) {
			return queryDataId;
		}
		if (body != null) {
			String extraido = body.extrairPaymentId();
			if (extraido != null && !extraido.isBlank()) {
				return extraido;
			}
		}
		if (queryId != null && !queryId.isBlank()) {
			return queryId;
		}
		return null;
	}
}
