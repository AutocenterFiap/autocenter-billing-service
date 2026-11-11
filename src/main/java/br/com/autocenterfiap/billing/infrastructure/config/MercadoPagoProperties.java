package br.com.autocenterfiap.billing.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de integracao com o Mercado Pago em {@code billing.mercadopago}.
 *
 * <p>Enquanto nao houver credenciais configuradas, {@code modo=stub} mantem o fluxo
 * funcional localmente e nos testes automatizados.
 */
@ConfigurationProperties(prefix = "billing.mercadopago")
public record MercadoPagoProperties(
		String modo,
		String baseUrl,
		String accessToken,
		String notificationUrl) {

	public boolean ehModoReal() {
		return "real".equalsIgnoreCase(modo) && accessToken != null && !accessToken.isBlank();
	}
}

