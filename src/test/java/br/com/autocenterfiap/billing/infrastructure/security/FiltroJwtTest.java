package br.com.autocenterfiap.billing.infrastructure.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FiltroJwtTest {

	private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres";

	private final FiltroJwt filtro = new FiltroJwt(SEGREDO);

	@AfterEach
	void limpar() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void deveAutenticarTokenInternoComoRoleInterno() throws Exception {
		executar("Bearer " + token(SEGREDO, null, Instant.now().plusSeconds(60)));

		assertThat(autoridade()).isEqualTo(FiltroJwt.ROLE_INTERNO);
	}

	@Test
	void deveAutenticarTokenDeClienteComoRoleCliente() throws Exception {
		executar("Bearer " + token(SEGREDO, "cliente", Instant.now().plusSeconds(60)));

		assertThat(autoridade()).isEqualTo(FiltroJwt.ROLE_CLIENTE);
	}

	@Test
	void naoDeveAutenticarTokenInvalidoExpiradoOuAusente() throws Exception {
		executar("Bearer " + token("outro-segredo-com-mais-de-32-caracteres", null, Instant.now().plusSeconds(60)));
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

		executar("Bearer " + token(SEGREDO, null, Instant.now().minusSeconds(60)));
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

		executar("Basic abc");
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

		executar(null);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	private void executar(String cabecalho) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/orcamentos");
		if (cabecalho != null) {
			request.addHeader("Authorization", cabecalho);
		}
		filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
	}

	private static String autoridade() {
		return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.findFirst()
				.orElseThrow();
	}

	private static String token(String segredo, String tipo, Instant expiraEm) {
		var builder = JWT.create().withIssuer(FiltroJwt.ISSUER).withSubject("usuario").withExpiresAt(expiraEm);
		if (tipo != null) {
			builder.withClaim("tipo", tipo);
		}
		return builder.sign(Algorithm.HMAC256(segredo));
	}
}
