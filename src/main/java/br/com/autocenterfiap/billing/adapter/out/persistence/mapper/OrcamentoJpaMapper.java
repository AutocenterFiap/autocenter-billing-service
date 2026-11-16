package br.com.autocenterfiap.billing.adapter.out.persistence.mapper;

import br.com.autocenterfiap.billing.adapter.out.persistence.entity.OrcamentoJpaEntity;
import br.com.autocenterfiap.billing.domain.entity.Orcamento;

public final class OrcamentoJpaMapper {

	private OrcamentoJpaMapper() {
	}

	public static Orcamento toDomain(OrcamentoJpaEntity entity) {
		if (entity == null) {
			return null;
		}

		return Orcamento.builder()
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

	public static OrcamentoJpaEntity toJpa(Orcamento domain) {
		if (domain == null) {
			return null;
		}

		return OrcamentoJpaEntity.builder()
				.id(domain.getId())
				.ordemServicoId(domain.getOrdemServicoId())
				.clienteId(domain.getClienteId())
				.correlationId(domain.getCorrelationId())
				.valorTotal(domain.getValorTotal())
				.statusOrcamento(domain.getStatusOrcamento())
				.motivo(domain.getMotivo())
				.dataCriacao(domain.getDataCriacao())
				.dataUltimaAtualizacao(domain.getDataUltimaAtualizacao())
				.build();
	}
}

