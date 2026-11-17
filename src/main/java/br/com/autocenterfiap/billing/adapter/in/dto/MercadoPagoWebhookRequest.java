package br.com.autocenterfiap.billing.adapter.in.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoWebhookRequest(
		String id,
		String type,
		String topic,
		String action,
		WebhookData data) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record WebhookData(String id) {
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

