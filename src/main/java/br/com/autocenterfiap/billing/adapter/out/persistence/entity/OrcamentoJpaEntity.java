package br.com.autocenterfiap.billing.adapter.out.persistence.entity;

import br.com.autocenterfiap.billing.domain.enums.StatusOrcamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "orcamento")
public class OrcamentoJpaEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "ordem_servico_id", nullable = false)
	private Long ordemServicoId;

	@Column(name = "cliente_id")
	private Long clienteId;

	@Column(name = "correlation_id", length = 64)
	private String correlationId;

	@Column(name = "valor_total", nullable = false)
	private BigDecimal valorTotal;

	@Enumerated(EnumType.STRING)
	@Column(name = "status_orcamento", nullable = false, length = 30)
	private StatusOrcamento statusOrcamento;

	@Column(name = "motivo")
	private String motivo;

	@Column(name = "data_criacao", nullable = false, updatable = false)
	private LocalDateTime dataCriacao;

	@Column(name = "data_ultima_atualizacao")
	private LocalDateTime dataUltimaAtualizacao;

	@PrePersist
	public void prePersist() {
		if (this.statusOrcamento == null) {
			this.statusOrcamento = StatusOrcamento.GERADO;
		}
		if (this.dataCriacao == null) {
			this.dataCriacao = LocalDateTime.now();
		}
		this.dataUltimaAtualizacao = LocalDateTime.now();
	}

	@PreUpdate
	public void preUpdate() {
		this.dataUltimaAtualizacao = LocalDateTime.now();
	}
}

