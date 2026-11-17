package br.com.autocenterfiap.billing.application.port.out;

import br.com.autocenterfiap.billing.domain.entity.Pagamento;

import java.util.List;
import java.util.Optional;

public interface PagamentoRepositoryPort {

	Pagamento salvar(Pagamento pagamento);

	Optional<Pagamento> buscarPorId(Long id);

	Optional<Pagamento> buscarMaisRecentePorOrdemServicoId(Long ordemServicoId);

	List<Pagamento> buscarTodosPorOrdemServicoId(Long ordemServicoId);
}

