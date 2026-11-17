package br.com.autocenterfiap.billing.adapter.out.persistence.adapter;

import br.com.autocenterfiap.billing.adapter.out.persistence.entity.OrcamentoJpaEntity;
import br.com.autocenterfiap.billing.adapter.out.persistence.mapper.OrcamentoJpaMapper;
import br.com.autocenterfiap.billing.adapter.out.persistence.repository.OrcamentoJpaRepository;
import br.com.autocenterfiap.billing.application.dto.PageResult;
import br.com.autocenterfiap.billing.application.dto.PaginationRequest;
import br.com.autocenterfiap.billing.domain.entity.Orcamento;
import br.com.autocenterfiap.billing.domain.enums.StatusOrcamento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrcamentoRepositoryJpaAdapterTest {

	@Mock
	private OrcamentoJpaRepository jpaRepository;

	@InjectMocks
	private OrcamentoRepositoryJpaAdapter adapter;

	@Test
	void deveSalvarEBuscarOrcamentos() {
		OrcamentoJpaEntity entity = OrcamentoJpaEntity.builder()
				.id(501L)
				.ordemServicoId(10L)
				.clienteId(42L)
				.correlationId("corr-10")
				.valorTotal(new BigDecimal("165.00"))
				.statusOrcamento(StatusOrcamento.AGUARDANDO_APROVACAO)
				.build();

		when(jpaRepository.save(any(OrcamentoJpaEntity.class))).thenReturn(entity);
		when(jpaRepository.findById(501L)).thenReturn(Optional.of(entity));
		when(jpaRepository.buscarOrcamentoAguardandoAprovacaoPorOS(10L)).thenReturn(Optional.of(entity));
		when(jpaRepository.findByOrdemServicoIdOrderByIdDesc(10L)).thenReturn(List.of(entity));
		when(jpaRepository.findByStatusOrcamento(eq(StatusOrcamento.AGUARDANDO_APROVACAO), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(entity)));

		Orcamento salvo = adapter.salvar(OrcamentoJpaMapper.toDomain(entity));
		assertThat(salvo.getId()).isEqualTo(501L);
		assertThat(adapter.buscarPorId(501L)).isPresent();
		assertThat(adapter.buscarOrcamentoAguardandoAprovacaoPorOS(10L)).isPresent();
		assertThat(adapter.buscarPorOrdemServicoId(10L)).hasSize(1);

		PageResult<Orcamento> page = adapter.buscarPorStatusOrcamento(
				StatusOrcamento.AGUARDANDO_APROVACAO, new PaginationRequest(0, 10));
		assertThat(page.getContent()).hasSize(1);
		assertThat(OrcamentoJpaMapper.toDomain(null)).isNull();
		assertThat(OrcamentoJpaMapper.toJpa(null)).isNull();
	}
}

