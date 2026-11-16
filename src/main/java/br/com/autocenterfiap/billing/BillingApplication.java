package br.com.autocenterfiap.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Microsservico de Orcamento e Pagamento (Billing) - Fase 4.
 *
 * <p>Responsavel por gerar orcamentos a partir dos itens da OS, permitir aprovacao ou
 * reprovacao, integrar com o Mercado Pago para processar/confirmar pagamentos e estornos,
 * e publicar os eventos da Saga em {@code billing.eventos}.
 *
 * <p>Persistencia relacional (MySQL + Flyway).
 */
@SpringBootApplication
@EnableKafka
@ConfigurationPropertiesScan
public class BillingApplication {

	public static void main(String[] args) {
		SpringApplication.run(BillingApplication.class, args);
	}

}

