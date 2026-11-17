package br.com.autocenterfiap.billing.domain.enums;

public enum StatusOrcamento {
	GERADO,
	AGUARDANDO_APROVACAO,
	APROVADO,
	REPROVADO,
	CANCELADO;

	public boolean ehTerminal() {
		return this == APROVADO || this == REPROVADO || this == CANCELADO;
	}
}

