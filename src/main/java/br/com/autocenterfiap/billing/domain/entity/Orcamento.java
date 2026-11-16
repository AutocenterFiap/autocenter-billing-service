package br.com.autocenterfiap.billing.domain.entity;

import br.com.autocenterfiap.billing.domain.enums.StatusOrcamento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade de dominio de Orcamento (mesmo modelo do monolito {@code autocenter},
 * acrescido de {@code clienteId}, {@code correlationId} e {@code motivo} para a Saga).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Orcamento implements Serializable {

	private Long id;

	private Long ordemServicoId;

	private Long clienteId;

	private String correlationId;

	private BigDecimal valorTotal;

	private StatusOrcamento statusOrcamento;

	private String motivo;

	private LocalDateTime dataCriacao;

	private LocalDateTime dataUltimaAtualizacao;

	public void aprovar() {
		this.statusOrcamento = StatusOrcamento.APROVADO;
	}

	public void reprovar() {
		reprovar("Cliente recusou o valor");
	}

	public void reprovar(String motivo) {
		this.statusOrcamento = StatusOrcamento.REPROVADO;
		this.motivo = (motivo == null || motivo.isBlank()) ? "Cliente recusou o valor" : motivo;
	}

	public boolean cancelar(String motivo) {
		if (this.statusOrcamento != null && this.statusOrcamento.ehTerminal()) {
			return false;
		}
		this.statusOrcamento = StatusOrcamento.CANCELADO;
		this.motivo = motivo;
		return true;
	}

	public void validarDominio() {
		if (ordemServicoId == null) {
			throw new IllegalArgumentException("O ID da ordem de servico nao pode ser nulo");
		}

		if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("O valor total do orcamento deve ser maior que zero");
		}

		if (statusOrcamento == null) {
			throw new IllegalArgumentException("O status do orcamento deve ser informado");
		}
	}
}

