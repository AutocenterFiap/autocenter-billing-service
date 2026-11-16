package br.com.autocenterfiap.billing.application.usecase;

import br.com.autocenterfiap.billing.application.dto.OrcamentoOutput;
import br.com.autocenterfiap.billing.application.exception.OrcamentoNaoEncontradoException;
import br.com.autocenterfiap.billing.application.mapper.OrcamentoApplicationMapper;
import br.com.autocenterfiap.billing.application.port.out.OrcamentoRepositoryPort;
import br.com.autocenterfiap.billing.domain.entity.Orcamento;

import java.util.List;

public class BuscarOrcamentoPorIdUseCase {

	private final OrcamentoRepositoryPort orcamentoRepositoryPort;

	public BuscarOrcamentoPorIdUseCase(OrcamentoRepositoryPort orcamentoRepositoryPort) {
		this.orcamentoRepositoryPort = orcamentoRepositoryPort;
	}

	public OrcamentoOutput executar(Long id) {
		Orcamento orcamento = this.orcamentoRepositoryPort.buscarPorId(id).orElseThrow(
				() -> new OrcamentoNaoEncontradoException("Orçamento não encontrado para o id: " + id)
		);

		return OrcamentoApplicationMapper.toOutput(orcamento);
	}

	public List<OrcamentoOutput> buscarPorOsId(Long osId) {
		return this.orcamentoRepositoryPort.buscarPorOrdemServicoId(osId).stream()
				.map(OrcamentoApplicationMapper::toOutput)
				.toList();
	}
}

