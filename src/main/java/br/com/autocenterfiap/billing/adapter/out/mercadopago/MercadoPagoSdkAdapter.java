package br.com.autocenterfiap.billing.adapter.out.mercadopago;

import br.com.autocenterfiap.billing.application.port.out.MercadoPagoPort;
import br.com.autocenterfiap.billing.infrastructure.config.MercadoPagoProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.payment.PaymentRefund;
import com.mercadopago.resources.preference.Preference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de integracao com o Mercado Pago (modelo Checkout Pro) utilizando o SDK
 * oficial {@code com.mercadopago:sdk-java}.
 *
 * <p>Suporta descoberta automatica de tunel HTTPS do ngrok ({@code http://localhost:4040/api/tunnels})
 * para preencher o {@code notification_url} do Webhook sem configuracao manual a cada sessao.
 */
@Component
public class MercadoPagoSdkAdapter implements MercadoPagoPort {

	private static final Logger log = LoggerFactory.getLogger(MercadoPagoSdkAdapter.class);
	private static final String WEBHOOK_PATH = "/v1/pagamentos/webhook/mercadopago";

	private final MercadoPagoProperties properties;
	private final PreferenceClient preferenceClient;
	private final PaymentClient paymentClient;
	private final PaymentRefundClient paymentRefundClient;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;

	@Autowired
	public MercadoPagoSdkAdapter(MercadoPagoProperties properties, ObjectMapper objectMapper) {
		this(
				properties,
				new PreferenceClient(),
				new PaymentClient(),
				new PaymentRefundClient(),
				objectMapper,
				HttpClient.newBuilder().connectTimeout(Duration.ofMillis(1500)).build());
	}

	MercadoPagoSdkAdapter(
			MercadoPagoProperties properties,
			PreferenceClient preferenceClient,
			PaymentClient paymentClient,
			PaymentRefundClient paymentRefundClient,
			ObjectMapper objectMapper,
			HttpClient httpClient) {
		this.properties = properties;
		this.preferenceClient = preferenceClient;
		this.paymentClient = paymentClient;
		this.paymentRefundClient = paymentRefundClient;
		this.objectMapper = objectMapper;
		this.httpClient = httpClient;
		if (properties.ehModoReal()) {
			MercadoPagoConfig.setAccessToken(properties.accessToken());
			log.info("SDK do Mercado Pago (Checkout Pro) inicializado em modo REAL");
		} else {
			log.info("MercadoPagoSdkAdapter inicializado em modo STUB (sem chamadas externas)");
		}
	}

	@Override
	public PreferenciaGerada criarPreferenciaCheckoutPro(
			Long pagamentoId,
			Long osId,
			Long orcamentoId,
			BigDecimal valor,
			Integer tentativa,
			Instant expiraEm) {
		if (!properties.ehModoReal()) {
			return gerarPreferenciaStub(pagamentoId, osId, tentativa);
		}

		try {
			MercadoPagoConfig.setAccessToken(properties.accessToken());

			PreferenceItemRequest item = PreferenceItemRequest.builder()
					.id("OS-" + osId + "-ORC-" + orcamentoId)
					.title("Ordem de Servico #" + osId + " - Orcamento #" + orcamentoId)
					.description("Pagamento da Ordem de Servico #" + osId + " (tentativa " + tentativa + ")")
					.categoryId("services")
					.quantity(1)
					.currencyId("BRL")
					.unitPrice(valor)
					.build();

			PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
					.success(properties.backUrlSuccess())
					.failure(properties.backUrlFailure())
					.pending(properties.backUrlPending())
					.build();

			OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
			OffsetDateTime expiracao = expiraEm != null
					? expiraEm.atOffset(ZoneOffset.UTC)
					: agora.plusMinutes(30);

			PreferenceRequest.PreferenceRequestBuilder requestBuilder = PreferenceRequest.builder()
					.items(List.of(item))
					.externalReference(String.valueOf(pagamentoId))
					.backUrls(backUrls)
					.autoReturn("approved")
					.expires(true)
					.expirationDateFrom(agora)
					.expirationDateTo(expiracao);

			String notificationUrl = resolverNotificationUrl();
			if (notificationUrl != null && !notificationUrl.isBlank()) {
				requestBuilder.notificationUrl(notificationUrl);
			}

			Preference preference = preferenceClient.create(requestBuilder.build());
			String urlCheckout = (preference.getSandboxInitPoint() != null && !preference.getSandboxInitPoint().isBlank())
					? preference.getSandboxInitPoint()
					: preference.getInitPoint();

			log.info("Preferencia Checkout Pro criada no Mercado Pago: prefId={} pagamentoId={} osId={} url={}",
					preference.getId(), pagamentoId, osId, urlCheckout);
			return new PreferenciaGerada(preference.getId(), urlCheckout);
		} catch (Exception ex) {
			log.error("Erro ao criar preferencia Checkout Pro no Mercado Pago para pagamentoId={} osId={}: {}",
					pagamentoId, osId, ex.getMessage(), ex);
			throw new IllegalStateException("Falha ao criar preferencia no Mercado Pago: " + ex.getMessage(), ex);
		}
	}

	@Override
	public Optional<DadosPagamentoExterno> consultarPagamento(String mpPaymentId) {
		if (!properties.ehModoReal() || mpPaymentId == null || mpPaymentId.isBlank()) {
			return Optional.empty();
		}
		try {
			MercadoPagoConfig.setAccessToken(properties.accessToken());
			Long paymentIdNumerico = Long.parseLong(mpPaymentId.trim());
			Payment payment = paymentClient.get(paymentIdNumerico);
			if (payment == null) {
				return Optional.empty();
			}
			Long externalRef = parseLongSeguro(payment.getExternalReference());
			return Optional.of(new DadosPagamentoExterno(
					String.valueOf(payment.getId()),
					externalRef,
					payment.getStatus(),
					payment.getStatusDetail(),
					payment.getTransactionAmount()));
		} catch (Exception ex) {
			log.error("Erro ao consultar pagamento {} no Mercado Pago: {}", mpPaymentId, ex.getMessage(), ex);
			return Optional.empty();
		}
	}

	@Override
	public ResultadoEstorno estornarPagamento(Long pagamentoId, String mpPaymentId, BigDecimal valor) {
		if (!properties.ehModoReal() || mpPaymentId == null || mpPaymentId.isBlank()) {
			return new ResultadoEstorno(true, "refund-stub-" + pagamentoId, null);
		}
		Long paymentIdNumerico = parseLongSeguro(mpPaymentId);
		if (paymentIdNumerico == null) {
			return new ResultadoEstorno(true, "refund-local-" + pagamentoId, null);
		}
		try {
			MercadoPagoConfig.setAccessToken(properties.accessToken());
			MPRequestOptions requestOptions = MPRequestOptions.builder()
					.customHeaders(Map.of("X-Idempotency-Key", UUID.randomUUID().toString()))
					.build();
			PaymentRefund refund = paymentRefundClient.refund(paymentIdNumerico, valor, requestOptions);
			String refundId = refund != null && refund.getId() != null
					? String.valueOf(refund.getId())
					: "refund-" + pagamentoId;
			log.info("Estorno realizado no Mercado Pago: pagamentoId={} mpPaymentId={} refundId={}",
					pagamentoId, mpPaymentId, refundId);
			return new ResultadoEstorno(true, refundId, null);
		} catch (Exception ex) {
			log.error("Falha ao estornar pagamento {} (mpPaymentId={}) no Mercado Pago: {}",
					pagamentoId, mpPaymentId, ex.getMessage(), ex);
			return new ResultadoEstorno(false, null, "Operadora recusou o estorno: " + ex.getMessage());
		}
	}

	@Override
	public void expirarPreferencia(String preferenceId) {
		if (!properties.ehModoReal() || preferenceId == null || preferenceId.isBlank()) {
			return;
		}
		try {
			MercadoPagoConfig.setAccessToken(properties.accessToken());
			OffsetDateTime passado = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1);
			PreferenceRequest request = PreferenceRequest.builder()
					.expires(true)
					.expirationDateFrom(passado.minusHours(1))
					.expirationDateTo(passado)
					.build();
			preferenceClient.update(preferenceId, request);
			log.info("Preferencia Checkout Pro expirada no Mercado Pago: prefId={}", preferenceId);
		} catch (Exception ex) {
			log.warn("Nao foi possivel expirar preferencia {} no Mercado Pago: {}", preferenceId, ex.getMessage());
		}
	}

	String resolverNotificationUrl() {
		if (properties.notificationUrl() != null && !properties.notificationUrl().isBlank()) {
			return properties.notificationUrl().trim();
		}
		if (properties.ngrokApiUrl() == null || properties.ngrokApiUrl().isBlank()) {
			return null;
		}
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(properties.ngrokApiUrl().trim()))
					.timeout(Duration.ofMillis(1500))
					.GET()
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
				JsonNode root = objectMapper.readTree(response.body());
				JsonNode tunnels = root.path("tunnels");
				if (tunnels.isArray()) {
					String fallbackUrl = null;
					for (JsonNode tunnel : tunnels) {
						String publicUrl = tunnel.path("public_url").asText(null);
						if (publicUrl != null && !publicUrl.isBlank()) {
							if (publicUrl.startsWith("https://")) {
								String url = normalizarUrlWebhook(publicUrl);
								log.info("Ngrok HTTPS detectado automaticamente para webhook do Mercado Pago: {}", url);
								return url;
							}
							if (fallbackUrl == null) {
								fallbackUrl = normalizarUrlWebhook(publicUrl);
							}
						}
					}
					if (fallbackUrl != null) {
						return fallbackUrl;
					}
				}
			}
		} catch (Exception ex) {
			log.debug("Ngrok local nao detectado em {} (prosseguindo sem notification_url automatica): {}",
					properties.ngrokApiUrl(), ex.getMessage());
		}
		return null;
	}

	private String normalizarUrlWebhook(String baseUrl) {
		String limpa = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		return limpa + WEBHOOK_PATH;
	}

	private PreferenciaGerada gerarPreferenciaStub(Long pagamentoId, Long osId, Integer tentativa) {
		String prefId = "pref-os-" + osId + "-pag-" + pagamentoId + "-t" + tentativa;
		String url = "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=os-" + osId + "-t" + tentativa;
		return new PreferenciaGerada(prefId, url);
	}

	private Long parseLongSeguro(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(valor.trim());
		} catch (NumberFormatException ex) {
			return null;
		}
	}
}

