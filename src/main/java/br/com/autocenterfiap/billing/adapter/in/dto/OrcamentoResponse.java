package br.com.autocenterfiap.billing.adapter.in.dto;

import br.com.autocenterfiap.billing.domain.enums.StatusOrcamento;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Orçamento de uma ordem de serviço")
public class OrcamentoResponse {
	@Schema(description = "Identificador do orçamento", example = "7")
	private Long id;
	@Schema(description = "Identificador da OS", example = "42")
	private Long ordemServicoId;
	@Schema(description = "Identificador do cliente", example = "1")
	private Long clienteId;
	@Schema(description = "Identificador de correlação da Saga", example = "3f1c2b9e-7a44-4c1e-9d0b-1a2b3c4d5e6f")
	private String correlationId;
	@Schema(description = "Valor total com serviços e produtos", example = "773.90")
	private BigDecimal valorTotal;
	@Schema(description = "Status do orçamento", example = "AGUARDANDO_APROVACAO")
	private StatusOrcamento statusOrcamento;
	@Schema(description = "Motivo da reprovação ou do cancelamento", example = "Cliente recusou o valor")
	private String motivo;
	@Schema(description = "Data de criação", example = "2026-11-17T09:00:00")
	private LocalDateTime dataCriacao;
	@Schema(description = "Data da última alteração", example = "2026-11-17T09:30:00")
	private LocalDateTime dataUltimaAtualizacao;
}

