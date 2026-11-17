package br.com.autocenterfiap.billing.adapter.in.handler;

import java.time.Instant;

public record ErroResponse(int status, String erro, String mensagem, Instant timestamp) {

	public static ErroResponse de(int status, String erro, String mensagem) {
		return new ErroResponse(status, erro, mensagem, Instant.now());
	}
}

