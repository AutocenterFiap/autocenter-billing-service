package br.com.autocenterfiap.billing.application.usecase;

import br.com.autocenterfiap.billing.application.dto.PagamentoOutput;
import br.com.autocenterfiap.billing.application.port.out.BillingEventPublisherPort;
import br.com.autocenterfiap.billing.application.port.out.MercadoPagoPort;
import br.com.autocenterfiap.billing.application.port.out.OrcamentoRepositoryPort;
import br.com.autocenterfiap.billing.application.port.out.PagamentoRepositoryPort;
import br.com.autocenterfiap.billing.domain.entity.Orcamento;
import br.com.autocenterfiap.billing.domain.entity.Pagamento;
import br.com.autocenterfiap.billing.domain.enums.StatusOrcamento;
import br.com.autocenterfiap.billing.domain.enums.StatusPagamento;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompensacaoUseCasesTest {

	@Mock
	private PagamentoRepositoryPort pagamentoRepository;

	@Mock
	private OrcamentoRepositoryPort orcamentoRepository;

	@Mock
	private MercadoPagoPort mercadoPagoPort;

	@Mock
	private BillingEventPublisherPort publisher;

	@Test
	void deveEstornarPagamentoConfirmadoEPublicarPagamentoEstornado() {
		Pagamento confirmado = Pagamento.novoPendente(
				10L, 50L, "corr-10", new BigDecimal("165.00"), 1, Instant.now());
		confirmado.setId(90L);
		confirmado.confirmar("mp-777");

		when(pagamentoRepository.buscarPorId(90L)).thenReturn(Optional.of(confirmado));
		when(mercadoPagoPort.estornarPagamento(90L, "mp-777", new BigDecimal("165.00")))
				.thenReturn(new MercadoPagoPort.ResultadoEstorno(true, "ref-1", null));
		when(pagamentoRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

		EstornarPagamentoUseCase useCase = new EstornarPagamentoUseCase(
				pagamentoRepository, mercadoPagoPort, publisher);

		PagamentoOutput out = useCase.executar(10L, 90L, "Execucao falhou", "corr-10");
		assertThat(out).isNotNull();
		assertThat(out.statusPagamento()).isEqualTo(StatusPagamento.ESTORNADO);
		assertThat(out.motivo()).isEqualTo("Execucao falhou");
		verify(publisher).pagamentoEstornado(10L, "corr-10", 90L);
	}

	@Test
	void devePublicarEstornoFalhouQuandoOperadoraRecusarOuPagamentoNaoEncontrado() {
		Pagamento confirmado = Pagamento.novoPendente(
				10L, 50L, "corr-10", new BigDecimal("165.00"), 1, Instant.now());
		confirmado.setId(91L);
		confirmado.confirmar("mp-888");

		when(pagamentoRepository.buscarPorId(91L)).thenReturn(Optional.of(confirmado));
		when(mercadoPagoPort.estornarPagamento(91L, "mp-888", new BigDecimal("165.00")))
				.thenReturn(new MercadoPagoPort.ResultadoEstorno(false, null, "Operadora recusou"));
		when(pagamentoRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
		when(pagamentoRepository.buscarPorId(999L)).thenReturn(Optional.empty());

		EstornarPagamentoUseCase useCase = new EstornarPagamentoUseCase(
				pagamentoRepository, mercadoPagoPort, publisher);

		PagamentoOutput falhou = useCase.executar(10L, 91L, "Execucao falhou", "corr-10");
		assertThat(falhou.statusPagamento()).isEqualTo(StatusPagamento.ESTORNO_FALHOU);
		verify(publisher).pagamentoEstornoFalhou(10L, "corr-10", 91L, "Operadora recusou");

		PagamentoOutput naoEncontrado = useCase.executar(10L, 999L, "Motivo", "corr-10");
		assertThat(naoEncontrado).isNull();
		verify(publisher).pagamentoEstornoFalhou(10L, "corr-10", 999L, "Pagamento nao encontrado para estorno");
	}

	@Test
	void deveSerIdempotenteSeJaEstornadoERecusarSeNaoConfirmado() {
		Pagamento jaEstornado = Pagamento.novoPendente(
				10L, 50L, "corr-10", new BigDecimal("165.00"), 1, Instant.now());
		jaEstornado.setId(92L);
		jaEstornado.estornar("Ja estornado");

		Pagamento pendente = Pagamento.novoPendente(
				11L, 51L, "corr-11", new BigDecimal("100.00"), 1, Instant.now());
		pendente.setId(93L);

		when(pagamentoRepository.buscarPorId(92L)).thenReturn(Optional.of(jaEstornado));
		when(pagamentoRepository.buscarMaisRecentePorOrdemServicoId(11L)).thenReturn(Optional.of(pendente));

		EstornarPagamentoUseCase useCase = new EstornarPagamentoUseCase(
				pagamentoRepository, mercadoPagoPort, publisher);

		PagamentoOutput outIdempotente = useCase.executar(10L, 92L, "Repete", "corr-10");
		assertThat(outIdempotente.statusPagamento()).isEqualTo(StatusPagamento.ESTORNADO);
		verify(mercadoPagoPort, never()).estornarPagamento(any(), any(), any());

		PagamentoOutput outPendente = useCase.executar(11L, null, "Cancelar", "corr-11");
		assertThat(outPendente.statusPagamento()).isEqualTo(StatusPagamento.PENDENTE);
		verify(publisher).pagamentoEstornoFalhou(
				11L, "corr-11", 93L, "Pagamento nao esta confirmado para estorno (status: PENDENTE)");
	}

	@Test
	void deveCancelarOrcamentosNaoTerminaisEPagamentosPendentesQuandoOsCancelada() {
		Orcamento orcAguardando = Orcamento.builder()
				.id(1L).ordemServicoId(10L).valorTotal(new BigDecimal("100.00"))
				.statusOrcamento(StatusOrcamento.AGUARDANDO_APROVACAO).build();
		Orcamento orcAprovado = Orcamento.builder()
				.id(2L).ordemServicoId(10L).valorTotal(new BigDecimal("100.00"))
				.statusOrcamento(StatusOrcamento.APROVADO).build();

		Pagamento pagPendente = Pagamento.novoPendente(
				10L, 2L, "corr-10", new BigDecimal("100.00"), 1, Instant.now());
		pagPendente.setId(5L);
		Pagamento pagConfirmado = Pagamento.novoPendente(
				10L, 2L, "corr-10", new BigDecimal("100.00"), 2, Instant.now());
		pagConfirmado.setId(6L);
		pagConfirmado.confirmar("mp-1");

		when(orcamentoRepository.buscarPorOrdemServicoId(10L))
				.thenReturn(List.of(orcAguardando, orcAprovado));
		when(pagamentoRepository.buscarTodosPorOrdemServicoId(10L))
				.thenReturn(List.of(pagPendente, pagConfirmado));

		CancelarBillingPorOsUseCase useCase = new CancelarBillingPorOsUseCase(
				orcamentoRepository, pagamentoRepository, mercadoPagoPort);

		useCase.executar(10L, "Cliente desistiu");

		assertThat(orcAguardando.getStatusOrcamento()).isEqualTo(StatusOrcamento.CANCELADO);
		assertThat(orcAprovado.getStatusOrcamento()).isEqualTo(StatusOrcamento.APROVADO);
		assertThat(pagPendente.getStatusPagamento()).isEqualTo(StatusPagamento.CANCELADO);
		assertThat(pagConfirmado.getStatusPagamento()).isEqualTo(StatusPagamento.CONFIRMADO);

		verify(orcamentoRepository).salvar(orcAguardando);
		verify(orcamentoRepository, never()).salvar(orcAprovado);
		verify(pagamentoRepository).salvar(pagPendente);
		verify(pagamentoRepository, never()).salvar(pagConfirmado);

		// osId nulo nao deve fazer nada
		useCase.executar(null, "Ignorar");
	}
}

