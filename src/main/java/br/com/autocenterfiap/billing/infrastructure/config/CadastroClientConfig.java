package br.com.autocenterfiap.billing.infrastructure.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class CadastroClientConfig {

	@Bean
	RestClient cadastroRestClient(RestClient.Builder builder, @Value("${cadastro.url}") String url) {
		var fabrica = new SimpleClientHttpRequestFactory();
		fabrica.setConnectTimeout(Duration.ofSeconds(2));
		fabrica.setReadTimeout(Duration.ofSeconds(3));
		return builder.baseUrl(url).requestFactory(fabrica).requestInterceptor(new RepassaTokenInterceptor()).build();
	}
}
