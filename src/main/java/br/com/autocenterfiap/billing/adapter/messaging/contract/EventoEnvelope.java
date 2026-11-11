package br.com.autocenterfiap.billing.adapter.messaging.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope comum a todas as mensagens da Saga da OS (ver contrato da equipe).
 *
 * <p>A chave Kafka e sempre o {@code osId}. Campos desconhecidos sao ignorados; o
 * {@code eventId} e usado para descartar mensagens repetidas (idempotencia).
 *
 * @param <T> tipo do payload especifico da mensagem
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoEnvelope<T>(
		String eventId,
		String tipo,
		Integer versao,
		Long osId,
		String correlationId,
		Instant ocorridoEm,
		T payload) {

	public static final int VERSAO_ATUAL = 1;

	/**
	 * Cria um envelope novo para publicacao, gerando {@code eventId} e {@code ocorridoEm}.
	 */
	public static <T> EventoEnvelope<T> novo(String tipo, Long osId, String correlationId, T payload) {
		return new EventoEnvelope<>(
				UUID.randomUUID().toString(),
				tipo,
				VERSAO_ATUAL,
				osId,
				correlationId,
				Instant.now(),
				payload);
	}
}

