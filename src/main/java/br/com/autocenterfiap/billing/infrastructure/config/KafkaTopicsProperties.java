package br.com.autocenterfiap.billing.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nomes dos topicos Kafka do ecossistema, em {@code billing.kafka.topics}.
 *
 * <ul>
 *   <li>{@code osEventos} e {@code billingComandos}: consumidos pelo Billing;</li>
 *   <li>{@code billingEventos}: publicado pelo Billing.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "billing.kafka.topics")
public record KafkaTopicsProperties(
		String osEventos,
		String billingComandos,
		String billingEventos) {
}

