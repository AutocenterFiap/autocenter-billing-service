package br.com.autocenterfiap.billing.application.usecase;

import br.com.autocenterfiap.billing.application.dto.CriarOrcamentoInput;
import br.com.autocenterfiap.billing.application.dto.OrcamentoOutput;
import br.com.autocenterfiap.billing.application.mapper.OrcamentoApplicationMapper;
import br.com.autocenterfiap.billing.application.port.out.OrcamentoRepositoryPort;
import br.com.autocenterfiap.billing.domain.entity.Orcamento;

public class CriarOrcamentoUseCase {

	private final OrcamentoRepositoryPort orcamentoRepositoryPort;

	public CriarOrcamentoUseCase(OrcamentoRepositoryPort orcamentoRepositoryPort) {
		this.orcamentoRepositoryPort = orcamentoRepositoryPort;
	}

	public OrcamentoOutput executar(CriarOrcamentoInput input) {
		Orcamento orcamento = OrcamentoApplicationMapper.toEntity(input);
		orcamento.validarDominio();

		return OrcamentoApplicationMapper.toOutput(this.orcamentoRepositoryPort.salvar(orcamento));
	}
}

