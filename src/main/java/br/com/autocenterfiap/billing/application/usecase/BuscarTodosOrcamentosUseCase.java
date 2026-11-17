package br.com.autocenterfiap.billing.application.usecase;

import br.com.autocenterfiap.billing.application.dto.OrcamentoOutput;
import br.com.autocenterfiap.billing.application.dto.PageResult;
import br.com.autocenterfiap.billing.application.dto.PaginationRequest;
import br.com.autocenterfiap.billing.application.mapper.OrcamentoApplicationMapper;
import br.com.autocenterfiap.billing.application.port.out.OrcamentoRepositoryPort;
import br.com.autocenterfiap.billing.domain.enums.StatusOrcamento;

public class BuscarTodosOrcamentosUseCase {

	private final OrcamentoRepositoryPort orcamentoRepositoryPort;

	public BuscarTodosOrcamentosUseCase(OrcamentoRepositoryPort orcamentoRepositoryPort) {
		this.orcamentoRepositoryPort = orcamentoRepositoryPort;
	}

	public PageResult<OrcamentoOutput> executar(StatusOrcamento statusOrcamento, PaginationRequest pagination) {
		return this.orcamentoRepositoryPort.buscarPorStatusOrcamento(statusOrcamento, pagination)
				.map(OrcamentoApplicationMapper::toOutput);
	}
}

