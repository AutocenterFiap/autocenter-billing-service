package br.com.autocenterfiap.billing.application.usecase;

import br.com.autocenterfiap.billing.application.dto.PagamentoOutput;
import br.com.autocenterfiap.billing.application.exception.PagamentoNaoEncontradoException;
import br.com.autocenterfiap.billing.application.port.out.PagamentoRepositoryPort;

public class BuscarPagamentoUseCase {

	private final PagamentoRepositoryPort pagamentoRepository;

	public BuscarPagamentoUseCase(PagamentoRepositoryPort pagamentoRepository) {
		this.pagamentoRepository = pagamentoRepository;
	}

	public PagamentoOutput buscarPorId(Long id) {
		return pagamentoRepository.buscarPorId(id)
				.map(PagamentoOutput::de)
				.orElseThrow(() -> new PagamentoNaoEncontradoException(id));
	}

	public PagamentoOutput buscarMaisRecentePorOsId(Long osId) {
		return pagamentoRepository.buscarMaisRecentePorOrdemServicoId(osId)
				.map(PagamentoOutput::de)
				.orElseThrow(() -> new PagamentoNaoEncontradoException(
						"Nenhum pagamento encontrado para a OS " + osId));
	}
}

