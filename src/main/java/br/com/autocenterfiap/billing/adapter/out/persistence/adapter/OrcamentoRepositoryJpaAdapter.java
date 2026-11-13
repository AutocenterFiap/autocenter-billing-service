package br.com.autocenterfiap.billing.adapter.out.persistence.adapter;

import br.com.autocenterfiap.billing.adapter.out.persistence.entity.OrcamentoJpaEntity;
import br.com.autocenterfiap.billing.adapter.out.persistence.mapper.OrcamentoJpaMapper;
import br.com.autocenterfiap.billing.adapter.out.persistence.repository.OrcamentoJpaRepository;
import br.com.autocenterfiap.billing.application.dto.PageResult;
import br.com.autocenterfiap.billing.application.dto.PaginationRequest;
import br.com.autocenterfiap.billing.application.port.out.OrcamentoRepositoryPort;
import br.com.autocenterfiap.billing.domain.entity.Orcamento;
import br.com.autocenterfiap.billing.domain.enums.StatusOrcamento;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrcamentoRepositoryJpaAdapter implements OrcamentoRepositoryPort {

	private final OrcamentoJpaRepository orcamentoJpaRepository;

	@Override
	public Optional<Orcamento> buscarPorId(Long id) {
		return this.orcamentoJpaRepository.findById(id)
				.map(OrcamentoJpaMapper::toDomain);
	}

	@Override
	public Optional<Orcamento> buscarOrcamentoAguardandoAprovacaoPorOS(Long osId) {
		return this.orcamentoJpaRepository.buscarOrcamentoAguardandoAprovacaoPorOS(osId)
				.map(OrcamentoJpaMapper::toDomain);
	}

	@Override
	public List<Orcamento> buscarPorOrdemServicoId(Long osId) {
		return this.orcamentoJpaRepository.findByOrdemServicoIdOrderByIdDesc(osId).stream()
				.map(OrcamentoJpaMapper::toDomain)
				.toList();
	}

	@Override
	public PageResult<Orcamento> buscarPorStatusOrcamento(StatusOrcamento status, PaginationRequest pagination) {
		PageRequest pageRequest = PageRequest.of(
				pagination.getPageNumber(),
				pagination.getPageSize(),
				Sort.by(Sort.Direction.fromString(pagination.getSortDirection()), pagination.getSortBy())
		);

		Page<OrcamentoJpaEntity> page = this.orcamentoJpaRepository.findByStatusOrcamento(status, pageRequest);
		return new PageResult<>(
				page.getContent().stream().map(OrcamentoJpaMapper::toDomain).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages()
		);
	}

	@Override
	public Orcamento salvar(Orcamento orcamento) {
		OrcamentoJpaEntity orcamentoJpaEntity = OrcamentoJpaMapper.toJpa(orcamento);
		OrcamentoJpaEntity salvo = this.orcamentoJpaRepository.save(orcamentoJpaEntity);
		return OrcamentoJpaMapper.toDomain(salvo);
	}
}

