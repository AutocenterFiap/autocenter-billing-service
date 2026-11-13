package br.com.autocenterfiap.billing.adapter.out.persistence.adapter;

import br.com.autocenterfiap.billing.adapter.out.persistence.entity.PagamentoJpaEntity;
import br.com.autocenterfiap.billing.adapter.out.persistence.mapper.PagamentoJpaMapper;
import br.com.autocenterfiap.billing.adapter.out.persistence.repository.PagamentoJpaRepository;
import br.com.autocenterfiap.billing.application.port.out.PagamentoRepositoryPort;
import br.com.autocenterfiap.billing.domain.entity.Pagamento;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PagamentoRepositoryJpaAdapter implements PagamentoRepositoryPort {

	private final PagamentoJpaRepository repository;
	private final PagamentoJpaMapper mapper;

	public PagamentoRepositoryJpaAdapter(
			PagamentoJpaRepository repository,
			PagamentoJpaMapper mapper) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Override
	public Pagamento salvar(Pagamento pagamento) {
		PagamentoJpaEntity salvo = repository.save(mapper.paraJpa(pagamento));
		return mapper.paraDominio(salvo);
	}

	@Override
	public Optional<Pagamento> buscarPorId(Long id) {
		return repository.findById(id).map(mapper::paraDominio);
	}

	@Override
	public Optional<Pagamento> buscarMaisRecentePorOrdemServicoId(Long ordemServicoId) {
		return repository.findFirstByOrdemServicoIdOrderByIdDesc(ordemServicoId).map(mapper::paraDominio);
	}

	@Override
	public List<Pagamento> buscarTodosPorOrdemServicoId(Long ordemServicoId) {
		return repository.findByOrdemServicoIdOrderByIdDesc(ordemServicoId)
				.stream()
				.map(mapper::paraDominio)
				.toList();
	}
}

