package br.com.autocenterfiap.billing.adapter.mapper;

import br.com.autocenterfiap.billing.adapter.in.dto.OrcamentoResponse;
import br.com.autocenterfiap.billing.application.dto.OrcamentoOutput;

public final class OrcamentoAdapterMapper {

	private OrcamentoAdapterMapper() {
	}

	public static OrcamentoResponse orcamentoOutputToOrcamentoResponse(OrcamentoOutput orcamentoResponse) {
		if (orcamentoResponse == null) {
			return null;
		}

		return OrcamentoResponse.builder()
				.id(orcamentoResponse.getId())
				.ordemServicoId(orcamentoResponse.getOrdemServicoId())
				.clienteId(orcamentoResponse.getClienteId())
				.correlationId(orcamentoResponse.getCorrelationId())
				.valorTotal(orcamentoResponse.getValorTotal())
				.statusOrcamento(orcamentoResponse.getStatusOrcamento())
				.motivo(orcamentoResponse.getMotivo())
				.dataCriacao(orcamentoResponse.getDataCriacao())
				.dataUltimaAtualizacao(orcamentoResponse.getDataUltimaAtualizacao())
				.build();
	}
}

