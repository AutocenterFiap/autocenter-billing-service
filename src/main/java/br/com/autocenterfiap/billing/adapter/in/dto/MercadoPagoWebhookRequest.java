package br.com.autocenterfiap.billing.adapter.in.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Notificação enviada pelo Mercado Pago")
public record MercadoPagoWebhookRequest(
		@Schema(description = "Identificador da notificação ou do pagamento (formato IPN)", example = "987654321")
		String id,
		@Schema(description = "Tipo da notificação; apenas payment é processado", example = "payment") String type,
		@Schema(description = "Tópico da notificação no formato IPN", example = "payment") String topic,
		@Schema(description = "Ação que gerou a notificação", example = "payment.updated") String action,
		@Schema(description = "Dados do recurso notificado") WebhookData data) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	@Schema(name = "MercadoPagoWebhookData", description = "Recurso notificado")
	public record WebhookData(
			@Schema(description = "Identificador do pagamento no Mercado Pago", example = "987654321") String id) {
	}

	public String extrairPaymentId() {
		if (data != null && data.id() != null && !data.id().isBlank()) {
			return data.id();
		}
		if (id != null && !id.isBlank()) {
			return id;
		}
		return null;
	}
}

