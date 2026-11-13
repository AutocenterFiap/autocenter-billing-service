package br.com.autocenterfiap.billing.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Configuracao do contexto Spring para os cenarios BDD, com MySQL e Kafka reais
 * via Testcontainers e Mercado Pago em modo simulado.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class CucumberSpringConfiguration {

	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
			.withDatabaseName("billing")
			.withUsername("billing")
			.withPassword("billing")
			.withUrlParam("useSSL", "false")
			.withUrlParam("allowPublicKeyRetrieval", "true");

	static final KafkaContainer KAFKA =
			new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"));

	static {
		MYSQL.start();
		KAFKA.start();
	}

	@DynamicPropertySource
	static void props(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
		registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
		registry.add("billing.mercadopago.modo", () -> "simulado");
		registry.add("billing.mercadopago.access-token", () -> "");
		registry.add("billing.mercadopago.notification-url", () -> "");
	}
}

