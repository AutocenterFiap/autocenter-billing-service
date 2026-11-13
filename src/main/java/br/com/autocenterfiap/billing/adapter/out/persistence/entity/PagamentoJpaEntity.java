package br.com.autocenterfiap.billing.adapter.out.persistence.entity;

import br.com.autocenterfiap.billing.domain.enums.StatusPagamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "ordem_servico_id", nullable = false)
	private Long ordemServicoId;

	@Column(name = "orcamento_id", nullable = false)
	private Long orcamentoId;

	@Column(name = "correlation_id")
	private String correlationId;

	@Column(name = "valor", nullable = false)
	private BigDecimal valor;

	@Column(name = "tentativa", nullable = false)
	private Integer tentativa;

	@Enumerated(EnumType.STRING)
	@Column(name = "status_pagamento", nullable = false)
	private StatusPagamento statusPagamento;

	@Column(name = "url_checkout")
	private String urlCheckout;

	@Column(name = "expira_em")
	private Instant expiraEm;

	@Column(name = "mp_preference_id")
	private String mpPreferenceId;

	@Column(name = "mp_payment_id")
	private String mpPaymentId;

	@Column(name = "motivo")
	private String motivo;

	@Column(name = "data_criacao", nullable = false)
	private LocalDateTime dataCriacao;

	@Column(name = "data_ultima_atualizacao")
	private LocalDateTime dataUltimaAtualizacao;
}

