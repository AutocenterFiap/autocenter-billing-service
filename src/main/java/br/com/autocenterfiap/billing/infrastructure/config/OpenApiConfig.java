package br.com.autocenterfiap.billing.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String ESQUEMA = "bearerAuth";

	@Bean
	OpenAPI openApi() {
		return new OpenAPI()
				.info(new Info().title("AutoCenter - Billing").version("1.0.0")
						.description("Orçamentos e pagamentos das ordens de serviço. Gera o orçamento com os "
								+ "itens do diagnóstico, registra aprovação e reprovação, cobra pelo Checkout Pro "
								+ "do Mercado Pago e realiza estornos, participando da Saga pelo Kafka."))
				.components(new Components().addSecuritySchemes(ESQUEMA,
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
								.description("JWT de funcionário, com o perfil interno")))
				.addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
	}
}
