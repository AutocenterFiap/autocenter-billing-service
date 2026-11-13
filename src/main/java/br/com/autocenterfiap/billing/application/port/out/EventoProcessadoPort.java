package br.com.autocenterfiap.billing.application.port.out;

/**
 * Porta de saida para deduplicacao de mensagens consumidas, por {@code eventId}.
 *
 * <p>A verificacao e a marcacao sao separadas de proposito: so marcamos apos o
 * processamento concluir, para que uma falha (e o retry) nao descarte a mensagem.
 */
public interface EventoProcessadoPort {

	boolean jaProcessado(String eventId);

	void marcarProcessado(String eventId);
}

