package br.com.autocenterfiap.billing.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Registro JPA de um {@code eventId} ja processado na tabela {@code eventos_processados}.
 */
@Entity
@Table(name = "eventos_processados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventoProcessadoJpaEntity {

	@Id
	@Column(name = "event_id", length = 64, nullable = false)
	private String eventId;

	@Column(name = "processado_em", nullable = false)
	private Instant processadoEm;
}

