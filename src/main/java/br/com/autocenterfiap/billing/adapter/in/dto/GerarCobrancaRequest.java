package br.com.autocenterfiap.billing.adapter.in.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record GerarCobrancaRequest(
		@NotNull(message = "osId e obrigatorio")
		Long osId,
		@NotNull(message = "orcamentoId e obrigatorio")
		Long orcamentoId,
		@NotNull(message = "valorTotal e obrigatorio")
		@Positive(message = "valorTotal deve ser maior que zero")
		BigDecimal valorTotal,
		Integer tentativa,
		Integer validadeMinutos) {
}

