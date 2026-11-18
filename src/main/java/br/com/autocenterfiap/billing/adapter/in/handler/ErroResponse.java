package br.com.autocenterfiap.billing.adapter.in.handler;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Corpo padrão das respostas de erro")
public record ErroResponse(
		@Schema(description = "Código HTTP", example = "404") int status,
		@Schema(description = "Descrição do código HTTP", example = "Not Found") String erro,
		@Schema(description = "Mensagem do erro", example = "Orçamento não encontrado para o id: 7") String mensagem,
		@Schema(description = "Momento do erro", example = "2026-11-17T12:00:00Z") Instant timestamp) {

	public static ErroResponse de(int status, String erro, String mensagem) {
		return new ErroResponse(status, erro, mensagem, Instant.now());
	}
}

