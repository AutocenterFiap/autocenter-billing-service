package br.com.autocenterfiap.billing.bdd;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.time.Instant;

public final class TokensTeste {

	public static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres";

	private TokensTeste() {
	}

	public static String interno() {
		return JWT.create()
				.withIssuer("Auto Center Fiap")
				.withSubject("atendente")
				.withExpiresAt(Instant.now().plusSeconds(3600))
				.sign(Algorithm.HMAC256(SEGREDO));
	}
}
