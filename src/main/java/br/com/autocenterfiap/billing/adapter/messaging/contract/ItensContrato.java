package br.com.autocenterfiap.billing.adapter.messaging.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Itens do diagnostico/orcamento no formato do contrato (valores monetarios como
 * strings com 2 casas, ex.: "120.00").
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ItensContrato(List<Servico> servicos, List<Produto> produtos) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Servico(Long servicoId, String descricao, String valor) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Produto(Long produtoId, String descricao, Integer quantidade, String precoUnitario) {
	}

	public boolean vazio() {
		return (servicos == null || servicos.isEmpty()) && (produtos == null || produtos.isEmpty());
	}

	/**
	 * Calcula a soma dos servicos e produtos informados nos itens.
	 */
	public BigDecimal calcularTotal() {
		BigDecimal total = BigDecimal.ZERO;
		if (servicos != null) {
			for (Servico s : servicos) {
				if (s != null && s.valor() != null && !s.valor().isBlank()) {
					total = total.add(new BigDecimal(s.valor()));
				}
			}
		}
		if (produtos != null) {
			for (Produto p : produtos) {
				if (p != null && p.precoUnitario() != null && !p.precoUnitario().isBlank()) {
					int qtd = p.quantidade() != null ? p.quantidade() : 1;
					total = total.add(new BigDecimal(p.precoUnitario()).multiply(BigDecimal.valueOf(qtd)));
				}
			}
		}
		return total;
	}
}

