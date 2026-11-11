package br.com.autocenterfiap.billing.application.port.out;

import br.com.autocenterfiap.billing.application.dto.PageResult;
import br.com.autocenterfiap.billing.application.dto.PaginationRequest;
import br.com.autocenterfiap.billing.domain.entity.Orcamento;
import br.com.autocenterfiap.billing.domain.enums.StatusOrcamento;

import java.util.List;
import java.util.Optional;

public interface OrcamentoRepositoryPort {

	Optional<Orcamento> buscarPorId(Long id);

	Optional<Orcamento> buscarOrcamentoAguardandoAprovacaoPorOS(Long osId);

	List<Orcamento> buscarPorOrdemServicoId(Long osId);

	PageResult<Orcamento> buscarPorStatusOrcamento(StatusOrcamento status, PaginationRequest pagination);

	Orcamento salvar(Orcamento orcamento);
}

