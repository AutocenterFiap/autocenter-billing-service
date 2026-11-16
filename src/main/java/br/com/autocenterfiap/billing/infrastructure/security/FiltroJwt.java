package br.com.autocenterfiap.billing.infrastructure.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class FiltroJwt extends OncePerRequestFilter {

	static final String ISSUER = "Auto Center Fiap";
	static final String ROLE_CLIENTE = "ROLE_CLIENTE";
	static final String ROLE_INTERNO = "ROLE_INTERNO";
	private static final String PREFIXO = "Bearer ";
	private static final Logger LOG = LoggerFactory.getLogger(FiltroJwt.class);

	private final JWTVerifier verificador;

	public FiltroJwt(String segredo) {
		this.verificador = JWT.require(Algorithm.HMAC256(segredo))
				.withIssuer(ISSUER)
				.withClaimPresence("exp")
				.build();
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String cabecalho = request.getHeader("Authorization");
		if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
			try {
				DecodedJWT jwt = verificador.verify(cabecalho.substring(PREFIXO.length()).trim());
				String autoridade = "cliente".equals(jwt.getClaim("tipo").asString()) ? ROLE_CLIENTE : ROLE_INTERNO;
				SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
						jwt.getSubject(), null, List.of(new SimpleGrantedAuthority(autoridade))));
			} catch (JWTVerificationException e) {
				LOG.debug("Token JWT rejeitado: {}", e.getClass().getSimpleName());
			}
		}
		chain.doFilter(request, response);
	}
}
