package br.com.autocenterfiap.billing.application.usecase;

import br.com.autocenterfiap.billing.application.dto.OrcamentoOutput;
import br.com.autocenterfiap.billing.application.exception.OrcamentoNaoEncontradoException;
import br.com.autocenterfiap.billing.application.mapper.OrcamentoApplicationMapper;
import br.com.autocenterfiap.billing.application.port.out.BillingEventPublisherPort;
import br.com.autocenterfiap.billing.application.port.out.OrcamentoRepositoryPort;
import br.com.autocenterfiap.billing.domain.entity.Orcamento;

/**
 * Aprova o orcamento (mesmo fluxo do monolito) e publica o evento
 * {@code 06 - orcamento.aprovado} para que a OS prossiga na Saga.
 */
public class AprovarOrcamentoUseCase {

	private final OrcamentoRepositoryPort orcamentoRepositoryPort;
	private final BillingEventPublisherPort publisher;

	public AprovarOrcamentoUseCase(OrcamentoRepositoryPort orcamentoRepositoryPort,
	                               BillingEventPublisherPort publisher) {
		this.orcamentoRepositoryPort = orcamentoRepositoryPort;
		this.publisher = publisher;
	}

	public OrcamentoOutput executar(Long id) {
		Orcamento orcamento = this.orcamentoRepositoryPort.buscarPorId(id).orElseThrow(
				() -> new OrcamentoNaoEncontradoException("Orçamento não encontrado para o id: " + id)
		);

		orcamento.aprovar();
		Orcamento orcamentoSalvo = this.orcamentoRepositoryPort.salvar(orcamento);
		this.publisher.orcamentoAprovado(orcamentoSalvo);

		return OrcamentoApplicationMapper.toOutput(orcamentoSalvo);
	}
}

