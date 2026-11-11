package br.com.autocenterfiap.billing.application.mapper;

import br.com.autocenterfiap.billing.application.dto.CriarOrcamentoInput;
import br.com.autocenterfiap.billing.application.dto.OrcamentoOutput;
import br.com.autocenterfiap.billing.domain.entity.Orcamento;

public final class OrcamentoApplicationMapper {

	private OrcamentoApplicationMapper() {
	}

	public static Orcamento toEntity(CriarOrcamentoInput input) {
		if (input == null) {
			return null;
		}

		return Orcamento.builder()
				.id(input.getId())
				.ordemServicoId(input.getOrdemServicoId())
				.clienteId(input.getClienteId())
				.correlationId(input.getCorrelationId())
				.valorTotal(input.getValorTotal())
				.statusOrcamento(input.getStatusOrcamento())
				.dataCriacao(input.getDataCriacao())
				.dataUltimaAtualizacao(input.getDataUltimaAtualizacao())
				.build();
	}

	public static OrcamentoOutput toOutput(Orcamento entity) {
		if (entity == null) {
			return null;
		}

		return OrcamentoOutput.builder()
				.id(entity.getId())
				.ordemServicoId(entity.getOrdemServicoId())
				.clienteId(entity.getClienteId())
				.correlationId(entity.getCorrelationId())
				.valorTotal(entity.getValorTotal())
				.statusOrcamento(entity.getStatusOrcamento())
				.motivo(entity.getMotivo())
				.dataCriacao(entity.getDataCriacao())
				.dataUltimaAtualizacao(entity.getDataUltimaAtualizacao())
				.build();
	}
}

