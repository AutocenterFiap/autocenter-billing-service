package br.com.autocenterfiap.billing.infrastructure.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaErrorConfig {

	static final String SUFIXO_DLT = ".dlt";
	private static final int PARTICOES = 3;
	private static final int MAX_RETENTATIVAS = 3;
	private static final long INTERVALO_INICIAL_MS = 1000L;
	private static final double MULTIPLICADOR = 2.0;

	@Bean
	DefaultErrorHandler tratadorDeErrosKafka(KafkaProperties kafkaProperties) {
		var produtor = new DefaultKafkaProducerFactory<String, String>(
				kafkaProperties.buildProducerProperties(null), new StringSerializer(), new StringSerializer());
		var publicadorDlt = new DeadLetterPublishingRecoverer(new KafkaTemplate<>(produtor),
				(registro, erro) -> new TopicPartition(registro.topic() + SUFIXO_DLT, registro.partition()));
		var tratador = new DefaultErrorHandler(publicadorDlt, intervaloDeRetentativas());
		tratador.addNotRetryableExceptions(JsonProcessingException.class, IllegalArgumentException.class);
		return tratador;
	}

	@Bean
	NewTopic dltBillingComandos(KafkaTopicsProperties topics) {
		return TopicBuilder.name(topics.billingComandos() + SUFIXO_DLT).partitions(PARTICOES).replicas(1).build();
	}

	@Bean
	NewTopic dltOsEventos(KafkaTopicsProperties topics) {
		return TopicBuilder.name(topics.osEventos() + SUFIXO_DLT).partitions(PARTICOES).replicas(1).build();
	}

	static ExponentialBackOffWithMaxRetries intervaloDeRetentativas() {
		var backOff = new ExponentialBackOffWithMaxRetries(MAX_RETENTATIVAS);
		backOff.setInitialInterval(INTERVALO_INICIAL_MS);
		backOff.setMultiplier(MULTIPLICADOR);
		return backOff;
	}
}
