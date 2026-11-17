package br.com.autocenterfiap.billing.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de integracao com o Mercado Pago (Checkout Pro) em {@code billing.mercadopago}.
 *
 * <p>Em {@code modo=real} com {@code accessToken} preenchido, usa o SDK oficial do Mercado Pago
 * ({@code com.mercadopago:sdk-java}). Em testes automatizados ou sem credenciais, cai em fallback
 * deterministico para nao exigir chamada externa na CI.
 */
@ConfigurationProperties(prefix = "billing.mercadopago")
public record MercadoPagoProperties(
		String modo,
		String baseUrl,
		String publicKey,
		String accessToken,
		String notificationUrl,
		String ngrokApiUrl,
		String backUrlSuccess,
		String backUrlFailure,
		String backUrlPending) {

	public boolean ehModoReal() {
		return "real".equalsIgnoreCase(modo) && accessToken != null && !accessToken.isBlank();
	}
}
