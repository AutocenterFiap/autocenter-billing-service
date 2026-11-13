package br.com.autocenterfiap.billing.application.usecase;

import br.com.autocenterfiap.billing.application.dto.OrcamentoOutput;
import br.com.autocenterfiap.billing.application.dto.PageResult;
import br.com.autocenterfiap.billing.application.dto.PaginationRequest;
import br.com.autocenterfiap.billing.application.exception.OrcamentoNaoEncontradoException;
import br.com.autocenterfiap.billing.application.port.out.BillingEventPublisherPort;
import br.com.autocenterfiap.billing.application.port.out.OrcamentoRepositoryPort;
import br.com.autocenterfiap.billing.domain.entity.Orcamento;
import br.com.autocenterfiap.billing.domain.enums.StatusOrcamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrcamentoUseCasesTest {

	@Mock
	private OrcamentoRepositoryPort repository;

	@Mock
	private BillingEventPublisherPort publisher;

	private CriarOrcamentoUseCase criarUseCase;
	private GerarOrcamentoUseCase gerarUseCase;
	private AprovarOrcamentoUseCase aprovarUseCase;
	private ReprovarOrcamentoUseCase reprovarUseCase;
	private BuscarOrcamentoPorIdUseCase buscarPorIdUseCase;
	private BuscarTodosOrcamentosUseCase buscarTodosUseCase;

	@BeforeEach
	void setUp() {
		criarUseCase = new CriarOrcamentoUseCase(repository);
		gerarUseCase = new GerarOrcamentoUseCase(repository, criarUseCase, publisher);
		aprovarUseCase = new AprovarOrcamentoUseCase(repository, publisher);
		reprovarUseCase = new ReprovarOrcamentoUseCase(repository, publisher);
		buscarPorIdUseCase = new BuscarOrcamentoPorIdUseCase(repository);
		buscarTodosUseCase = new BuscarTodosOrcamentosUseCase(repository);
	}

	@Test
	void deveGerarOrcamentoEPublicarOrcamentoGerado() {
		Orcamento anterior = Orcamento.builder()
				.id(500L)
				.ordemServicoId(10L)
				.valorTotal(new BigDecimal("100.00"))
				.statusOrcamento(StatusOrcamento.AGUARDANDO_APROVACAO)
				.build();
		when(repository.buscarOrcamentoAguardandoAprovacaoPorOS(10L)).thenReturn(Optional.of(anterior));
		when(repository.salvar(any(Orcamento.class))).thenAnswer(inv -> {
			Orcamento o = inv.getArgument(0);
			if (o.getId() == null) {
				o.setId(501L);
			}
			return o;
		});

		OrcamentoOutput output = gerarUseCase.executar(10L, 42L, new BigDecimal("165.00"), "corr-10");

		assertThat(output).isNotNull();
		assertThat(output.getId()).isEqualTo(501L);
		assertThat(output.getStatusOrcamento()).isEqualTo(StatusOrcamento.AGUARDANDO_APROVACAO);
		verify(publisher).orcamentoGerado(any(Orcamento.class));
	}

	@Test
	void devePublicarOrcamentoFalhouQuandoValorInvalido() {
		OrcamentoOutput output = gerarUseCase.executar(10L, 42L, BigDecimal.ZERO, "corr-10");

		assertThat(output).isNull();
		verify(publisher).orcamentoFalhou(10L, "corr-10", "Falha ao calcular o orçamento");
	}

	@Test
	void deveAprovarEReprovarOrcamentoEPublicarEventos() {
		Orcamento pendente = Orcamento.builder()
				.id(501L)
				.ordemServicoId(10L)
				.correlationId("corr-10")
				.valorTotal(new BigDecimal("165.00"))
				.statusOrcamento(StatusOrcamento.AGUARDANDO_APROVACAO)
				.build();
		when(repository.buscarPorId(501L)).thenReturn(Optional.of(pendente));
		when(repository.salvar(any(Orcamento.class))).thenAnswer(inv -> inv.getArgument(0));

		OrcamentoOutput aprovado = aprovarUseCase.executar(501L);
		assertThat(aprovado.getStatusOrcamento()).isEqualTo(StatusOrcamento.APROVADO);
		verify(publisher).orcamentoAprovado(any(Orcamento.class));

		Orcamento reprovado = Orcamento.builder()
				.id(502L)
				.ordemServicoId(11L)
				.correlationId("corr-11")
				.valorTotal(new BigDecimal("200.00"))
				.statusOrcamento(StatusOrcamento.AGUARDANDO_APROVACAO)
				.build();
		when(repository.buscarPorId(502L)).thenReturn(Optional.of(reprovado));

		OrcamentoOutput outReprovado = reprovarUseCase.executar(502L);
		assertThat(outReprovado.getStatusOrcamento()).isEqualTo(StatusOrcamento.REPROVADO);
		assertThat(outReprovado.getMotivo()).isEqualTo("Cliente recusou o valor");
		verify(publisher).orcamentoReprovado(any(Orcamento.class));
	}

	@Test
	void deveBuscarOrcamentosELancarExcecaoQuandoNaoEncontrado() {
		Orcamento o = Orcamento.builder()
				.id(501L)
				.ordemServicoId(10L)
				.valorTotal(new BigDecimal("165.00"))
				.statusOrcamento(StatusOrcamento.AGUARDANDO_APROVACAO)
				.build();
		when(repository.buscarPorId(501L)).thenReturn(Optional.of(o));
		when(repository.buscarPorId(999L)).thenReturn(Optional.empty());
		when(repository.buscarPorOrdemServicoId(10L)).thenReturn(List.of(o));
		when(repository.buscarPorStatusOrcamento(any(), any()))
				.thenReturn(new PageResult<>(List.of(o), 0, 20, 1, 1));

		assertThat(buscarPorIdUseCase.executar(501L).getId()).isEqualTo(501L);
		assertThat(buscarPorIdUseCase.buscarPorOsId(10L)).hasSize(1);
		assertThat(buscarTodosUseCase.executar(StatusOrcamento.AGUARDANDO_APROVACAO,
				new PaginationRequest(0, 20)).getContent()).hasSize(1);

		assertThatThrownBy(() -> buscarPorIdUseCase.executar(999L))
				.isInstanceOf(OrcamentoNaoEncontradoException.class);
		assertThatThrownBy(() -> aprovarUseCase.executar(999L))
				.isInstanceOf(OrcamentoNaoEncontradoException.class);
		assertThatThrownBy(() -> reprovarUseCase.executar(999L))
				.isInstanceOf(OrcamentoNaoEncontradoException.class);
	}
}

