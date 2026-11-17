package br.com.autocenterfiap.billing.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Dados para gerar a cobrança de um orçamento")
public record GerarCobrancaRequest(
		@Schema(description = "Identificador da OS", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "osId e obrigatorio")
		Long osId,
		@Schema(description = "Identificador do orçamento", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "orcamentoId e obrigatorio")
		Long orcamentoId,
		@Schema(description = "Valor a cobrar", example = "773.90", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "valorTotal e obrigatorio")
		@Positive(message = "valorTotal deve ser maior que zero")
		BigDecimal valorTotal,
		@Schema(description = "Número da tentativa de cobrança", example = "1")
		Integer tentativa,
		@Schema(description = "Validade do link em minutos; padrão de 30", example = "30")
		Integer validadeMinutos) {
}

