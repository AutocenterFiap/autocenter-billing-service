package br.com.autocenterfiap.billing.application.usecase;

import br.com.autocenterfiap.billing.application.dto.OrcamentoOutput;
import br.com.autocenterfiap.billing.application.exception.OrcamentoNaoEncontradoException;
import br.com.autocenterfiap.billing.application.mapper.OrcamentoApplicationMapper;
import br.com.autocenterfiap.billing.application.port.out.BillingEventPublisherPort;
import br.com.autocenterfiap.billing.application.port.out.OrcamentoRepositoryPort;
import br.com.autocenterfiap.billing.domain.entity.Orcamento;

/**
 * Reprova o orcamento (mesmo fluxo do monolito) e publica o evento
 * {@code 16 - orcamento.reprovado} para que a OS cancele a ordem na Saga.
 */
public class ReprovarOrcamentoUseCase {

	private final OrcamentoRepositoryPort orcamentoRepositoryPort;
	private final BillingEventPublisherPort publisher;

	public ReprovarOrcamentoUseCase(OrcamentoRepositoryPort orcamentoRepositoryPort,
	                                BillingEventPublisherPort publisher) {
		this.orcamentoRepositoryPort = orcamentoRepositoryPort;
		this.publisher = publisher;
	}

	public OrcamentoOutput executar(Long id) {
		return executar(id, null);
	}

	public OrcamentoOutput executar(Long id, String motivo) {
		Orcamento orcamento = this.orcamentoRepositoryPort.buscarPorId(id)
				.orElseThrow(() -> new OrcamentoNaoEncontradoException(id));

		orcamento.reprovar(motivo);
		Orcamento orcamentoSalvo = this.orcamentoRepositoryPort.salvar(orcamento);
		this.publisher.orcamentoReprovado(orcamentoSalvo);

		return OrcamentoApplicationMapper.toOutput(orcamentoSalvo);
	}
}

