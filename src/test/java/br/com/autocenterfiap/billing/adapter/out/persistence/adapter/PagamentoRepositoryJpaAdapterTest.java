package br.com.autocenterfiap.billing.adapter.out.persistence.adapter;

import br.com.autocenterfiap.billing.adapter.out.persistence.entity.PagamentoJpaEntity;
import br.com.autocenterfiap.billing.adapter.out.persistence.mapper.PagamentoJpaMapper;
import br.com.autocenterfiap.billing.adapter.out.persistence.repository.PagamentoJpaRepository;
import br.com.autocenterfiap.billing.domain.entity.Pagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagamentoRepositoryJpaAdapterTest {

	@Mock
	private PagamentoJpaRepository repository;

	private PagamentoJpaMapper mapper;
	private PagamentoRepositoryJpaAdapter adapter;

	@BeforeEach
	void setUp() {
		mapper = new PagamentoJpaMapper();
		adapter = new PagamentoRepositoryJpaAdapter(repository, mapper);
	}

	@Test
	void deveSalvarEBuscarPagamentos() {
		Pagamento dominio = Pagamento.novoPendente(
				10L, 50L, "corr-10", new BigDecimal("165.00"), 1, Instant.now());
		dominio.setId(90L);
		PagamentoJpaEntity entidade = mapper.paraJpa(dominio);

		when(repository.save(any())).thenReturn(entidade);
		when(repository.findById(90L)).thenReturn(Optional.of(entidade));
		when(repository.findFirstByOrdemServicoIdOrderByIdDesc(10L)).thenReturn(Optional.of(entidade));
		when(repository.findByOrdemServicoIdOrderByIdDesc(10L)).thenReturn(List.of(entidade));

		assertThat(adapter.salvar(dominio).getId()).isEqualTo(90L);
		assertThat(adapter.buscarPorId(90L)).isPresent();
		assertThat(adapter.buscarMaisRecentePorOrdemServicoId(10L)).isPresent();
		assertThat(adapter.buscarTodosPorOrdemServicoId(10L)).hasSize(1);
		assertThat(mapper.paraJpa(null)).isNull();
		assertThat(mapper.paraDominio(null)).isNull();
	}
}

