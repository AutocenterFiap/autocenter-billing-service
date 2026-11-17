package br.com.autocenterfiap.billing.infrastructure.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
public class ConfiguracaoSeguranca {

	@Bean
	SecurityFilterChain filtroSeguranca(HttpSecurity http,
			@Value("${sistema.seguranca.chave.secreta}") String segredo) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers("/actuator/health/**", "/actuator/prometheus", "/actuator/info",
								"/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers(HttpMethod.POST, "/v1/pagamentos/webhook/mercadopago").permitAll()
						.requestMatchers("/v1/**").hasAuthority(FiltroJwt.ROLE_INTERNO)
						.anyRequest().denyAll())
				.exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.addFilterBefore(new FiltroJwt(segredo), BasicAuthenticationFilter.class)
				.build();
	}
}
