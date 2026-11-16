package br.com.autocenterfiap.billing.application.dto;

import br.com.autocenterfiap.billing.domain.enums.StatusOrcamento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriarOrcamentoInput {

	private Long id;
	private Long ordemServicoId;
	private Long clienteId;
	private String correlationId;
	private BigDecimal valorTotal;
	private StatusOrcamento statusOrcamento;
	private LocalDateTime dataCriacao;
	private LocalDateTime dataUltimaAtualizacao;

}

