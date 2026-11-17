package br.com.autocenterfiap.billing.adapter.in.controller;

import br.com.autocenterfiap.billing.adapter.in.dto.GerarCobrancaRequest;
import br.com.autocenterfiap.billing.adapter.in.dto.MercadoPagoWebhookRequest;
import br.com.autocenterfiap.billing.adapter.in.dto.PagamentoResponse;
import br.com.autocenterfiap.billing.adapter.in.dto.RecusarPagamentoRequest;
import br.com.autocenterfiap.billing.adapter.in.handler.ErroResponse;
import br.com.autocenterfiap.billing.application.dto.PagamentoOutput;
import br.com.autocenterfiap.billing.application.usecase.BuscarPagamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.EstornarPagamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.GerarCobrancaUseCase;
import br.com.autocenterfiap.billing.application.usecase.ProcessarPagamentoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
@Tag(name = "Pagamentos", description = "Cobrança via Checkout Pro do Mercado Pago, consulta, webhook, "
		+ "confirmação e recusa manuais e estorno")
public class PagamentoController {

	private static final Logger log = LoggerFactory.getLogger(PagamentoController.class);

	private final GerarCobrancaUseCase gerarCobrancaUseCase;
	private final BuscarPagamentoUseCase buscarPagamentoUseCase;
	private final ProcessarPagamentoUseCase processarPagamentoUseCase;
	private final EstornarPagamentoUseCase estornarPagamentoUseCase;

	public PagamentoController(
			GerarCobrancaUseCase gerarCobrancaUseCase,
			BuscarPagamentoUseCase buscarPagamentoUseCase,
			ProcessarPagamentoUseCase processarPagamentoUseCase,
			EstornarPagamentoUseCase estornarPagamentoUseCase) {
		this.gerarCobrancaUseCase = gerarCobrancaUseCase;
		this.buscarPagamentoUseCase = buscarPagamentoUseCase;
		this.processarPagamentoUseCase = processarPagamentoUseCase;
		this.estornarPagamentoUseCase = estornarPagamentoUseCase;
	}

	@PostMapping
	@Operation(summary = "Gerar cobrança",
			description = "Cria o pagamento PENDENTE, gera o link do Checkout Pro no Mercado Pago e publica "
					+ "pagamento.link-gerado. Cobranças pendentes anteriores da mesma OS são invalidadas. "
					+ "Na Saga a cobrança é gerada automaticamente após a aprovação do orçamento")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Cobrança gerada"),
			@ApiResponse(responseCode = "400", description = "Campos obrigatórios ausentes ou inválidos",
					content = @Content(schema = @Schema(implementation = ErroResponse.class))),
			@ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
					content = @Content),
			@ApiResponse(responseCode = "403", description = "Token sem o perfil interno (funcionário)",
					content = @Content)
	})
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
	@Operation(summary = "Buscar pagamento por ID")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Pagamento encontrado"),
			@ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
					content = @Content),
			@ApiResponse(responseCode = "403", description = "Token sem o perfil interno (funcionário)",
					content = @Content),
			@ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
					content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	})
	public ResponseEntity<PagamentoResponse> buscarPorId(
			@Parameter(description = "Identificador do pagamento", example = "15") @PathVariable Long id) {
		PagamentoOutput output = buscarPagamentoUseCase.buscarPorId(id);
		return ResponseEntity.ok(PagamentoResponse.de(output));
	}

	@GetMapping("/os/{osId}")
	@Operation(summary = "Buscar pagamento mais recente de uma OS")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Pagamento encontrado"),
			@ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
					content = @Content),
			@ApiResponse(responseCode = "403", description = "Token sem o perfil interno (funcionário)",
					content = @Content),
			@ApiResponse(responseCode = "404", description = "Nenhum pagamento para a OS",
					content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	})
	public ResponseEntity<PagamentoResponse> buscarPorOsId(
			@Parameter(description = "Identificador da OS", example = "42") @PathVariable Long osId) {
		PagamentoOutput output = buscarPagamentoUseCase.buscarMaisRecentePorOsId(osId);
		return ResponseEntity.ok(PagamentoResponse.de(output));
	}

	@PostMapping("/webhook/mercadopago")
	@SecurityRequirements
	@Operation(summary = "Receber notificação do Mercado Pago",
			description = "Endpoint público chamado pelo Mercado Pago nos formatos Webhook e IPN. O identificador "
					+ "do pagamento pode vir na query ou no corpo. Apenas notificações do tipo payment são "
					+ "processadas; o status é consultado no Mercado Pago e a Saga é notificada. Sempre "
					+ "responde 200 para o Mercado Pago não reenviar")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Notificação recebida",
					content = @Content(examples = {
							@ExampleObject(name = "processada",
									value = "{\"recebido\":true,\"processado\":true,\"pagamentoId\":15,"
											+ "\"status\":\"CONFIRMADO\"}"),
							@ExampleObject(name = "ignorada", value = "{\"recebido\":true,\"processado\":false}")
					}))
	})
	public ResponseEntity<Map<String, Object>> receberWebhookMercadoPago(
			@Parameter(description = "Tipo da notificação (Webhook)", example = "payment")
			@RequestParam(name = "type", required = false) String queryType,
			@Parameter(description = "Tópico da notificação (IPN)", example = "payment")
			@RequestParam(name = "topic", required = false) String queryTopic,
			@Parameter(description = "Identificador do pagamento no Mercado Pago (Webhook)", example = "987654321")
			@RequestParam(name = "data.id", required = false) String queryDataId,
			@Parameter(description = "Identificador do pagamento no Mercado Pago (IPN)", example = "987654321")
			@RequestParam(name = "id", required = false) String queryId,
			@RequestBody(required = false) MercadoPagoWebhookRequest body) {

		String tipo = resolverTipoNotificacao(queryType, queryTopic, body);
		String mpPaymentId = resolverMpPaymentId(queryDataId, queryId, body);

		log.info("Webhook Mercado Pago recebido: tipo={} mpPaymentId={}", paraLog(tipo), paraLog(mpPaymentId));

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
	@Operation(summary = "Confirmar pagamento manualmente",
			description = "Confirma um pagamento PENDENTE e publica pagamento.confirmado; a Saga inicia a "
					+ "execução. Útil para simular o pagamento sem o Mercado Pago. Pagamento já confirmado "
					+ "ou estornado é retornado sem alteração; recebido em cobrança recusada ou cancelada é "
					+ "estornado")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Pagamento após a confirmação"),
			@ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
					content = @Content),
			@ApiResponse(responseCode = "403", description = "Token sem o perfil interno (funcionário)",
					content = @Content),
			@ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
					content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	})
	public ResponseEntity<PagamentoResponse> confirmar(
			@Parameter(description = "Identificador do pagamento", example = "15") @PathVariable Long id,
			@Parameter(description = "Identificador do pagamento no Mercado Pago", example = "987654321")
			@RequestParam(name = "mpPaymentId", required = false) String mpPaymentId) {
		PagamentoOutput output = processarPagamentoUseCase.confirmar(id, mpPaymentId);
		return ResponseEntity.ok(PagamentoResponse.de(output));
	}

	@PostMapping("/{id}/recusar")
	@Operation(summary = "Recusar pagamento manualmente",
			description = "Recusa um pagamento PENDENTE e publica pagamento.recusado; a OS vai para "
					+ "PAGAMENTO_RECUSADO. O corpo é opcional")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Pagamento após a recusa"),
			@ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
					content = @Content),
			@ApiResponse(responseCode = "403", description = "Token sem o perfil interno (funcionário)",
					content = @Content),
			@ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
					content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	})
	public ResponseEntity<PagamentoResponse> recusar(
			@Parameter(description = "Identificador do pagamento", example = "15") @PathVariable Long id,
			@RequestBody(required = false) RecusarPagamentoRequest request) {
		String motivo = request != null ? request.motivo() : null;
		String mpPaymentId = request != null ? request.mpPaymentId() : null;
		PagamentoOutput output = processarPagamentoUseCase.recusar(id, mpPaymentId, motivo);
		return ResponseEntity.ok(PagamentoResponse.de(output));
	}

	@PostMapping("/{id}/estornar")
	@Operation(summary = "Estornar pagamento",
			description = "Solicita ou reprocessa no Mercado Pago o estorno de um pagamento CONFIRMADO ou "
					+ "ESTORNO_FALHOU e publica pagamento.estornado ou pagamento.estorno-falhou na Saga. "
					+ "Em outros status o pagamento é retornado sem alteração")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Pagamento após a tentativa de estorno"),
			@ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
					content = @Content),
			@ApiResponse(responseCode = "403", description = "Token sem o perfil interno (funcionário)",
					content = @Content),
			@ApiResponse(responseCode = "404", description = "Pagamento não encontrado", content = @Content)
	})
	public ResponseEntity<PagamentoResponse> estornar(
			@Parameter(description = "Identificador do pagamento", example = "15") @PathVariable Long id,
			@Parameter(description = "Motivo do estorno", example = "Falha no reparo")
			@RequestParam(name = "motivo", required = false) String motivo) {
		PagamentoOutput output = estornarPagamentoUseCase.executar(null, id, motivo, null);
		if (output == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(PagamentoResponse.de(output));
	}

	private static String paraLog(String valor) {
		return valor == null ? null : valor.replaceAll("[\n\r]", "_");
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
