package br.com.autocenterfiap.billing.application.usecase;

import br.com.autocenterfiap.billing.application.dto.PagamentoOutput;
import br.com.autocenterfiap.billing.application.exception.PagamentoNaoEncontradoException;
import br.com.autocenterfiap.billing.application.port.out.BillingEventPublisherPort;
import br.com.autocenterfiap.billing.application.port.out.MercadoPagoPort;
import br.com.autocenterfiap.billing.application.port.out.PagamentoRepositoryPort;
import br.com.autocenterfiap.billing.domain.entity.Pagamento;
import br.com.autocenterfiap.billing.domain.enums.StatusPagamento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagamentoUseCasesTest {

	@Mock
	private PagamentoRepositoryPort pagamentoRepository;

	@Mock
	private MercadoPagoPort mercadoPagoPort;

	@Mock
	private BillingEventPublisherPort publisher;

	@Test
	void deveGerarCobrancaCriarPreferenciaNoMercadoPagoEPublicarLinkGerado() {
		when(pagamentoRepository.salvar(any())).thenAnswer(inv -> {
			Pagamento p = inv.getArgument(0);
			if (p.getId() == null) {
				p.setId(90L);
			}
			return p;
		});
		when(mercadoPagoPort.criarPreferenciaCheckoutPro(eq(90L), eq(10L), eq(50L), any(), eq(1), any()))
				.thenReturn(new MercadoPagoPort.PreferenciaGerada(
						"pref-90",
						"https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=pref-90"));

		GerarCobrancaUseCase useCase = new GerarCobrancaUseCase(pagamentoRepository, mercadoPagoPort, publisher);
		PagamentoOutput out = useCase.executar(10L, 50L, new BigDecimal("165.00"), 1, 30, "corr-10");

		assertThat(out.id()).isEqualTo(90L);
		assertThat(out.mpPreferenceId()).isEqualTo("pref-90");
		assertThat(out.urlCheckout()).contains("pref-90");
		verify(publisher).pagamentoLinkGerado(
				eq(10L),
				eq("corr-10"),
				eq(90L),
				eq("https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=pref-90"),
				eq(1),
				any(Instant.class));
	}

	@Test
	void deveProcessarWebhookAprovadoERecusadoNoMercadoPago() {
		Pagamento pendente = Pagamento.novoPendente(10L, 50L, "corr-10", new BigDecimal("165.00"), 1, Instant.now());
		pendente.setId(90L);

		when(mercadoPagoPort.consultarPagamento("777"))
				.thenReturn(Optional.of(new MercadoPagoPort.DadosPagamentoExterno(
						"777", 90L, "approved", "accredited", new BigDecimal("165.00"))));
		when(pagamentoRepository.buscarPorId(90L)).thenReturn(Optional.of(pendente));
		when(pagamentoRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

		ProcessarPagamentoUseCase useCase = new ProcessarPagamentoUseCase(
				pagamentoRepository, mercadoPagoPort, publisher);

		Optional<PagamentoOutput> confirmado = useCase.processarNotificacaoWebhook("777");
		assertThat(confirmado).isPresent();
		assertThat(confirmado.get().statusPagamento()).isEqualTo(StatusPagamento.CONFIRMADO);
		verify(publisher).pagamentoConfirmado(10L, "corr-10", 90L, new BigDecimal("165.00"));

		// Segunda chamada idempotente nao republica
		useCase.confirmar(90L, "777");

		// Caso recusado via webhook
		Pagamento pendente2 = Pagamento.novoPendente(10L, 50L, "corr-10", new BigDecimal("165.00"), 2, Instant.now());
		pendente2.setId(91L);
		when(mercadoPagoPort.consultarPagamento("888"))
				.thenReturn(Optional.of(new MercadoPagoPort.DadosPagamentoExterno(
						"888", 91L, "rejected", "cc_rejected_bad_filled_security_code", new BigDecimal("165.00"))));
		when(pagamentoRepository.buscarPorId(91L)).thenReturn(Optional.of(pendente2));

		Optional<PagamentoOutput> recusado = useCase.processarNotificacaoWebhook("888");
		assertThat(recusado).isPresent();
		assertThat(recusado.get().statusPagamento()).isEqualTo(StatusPagamento.RECUSADO);
		verify(publisher).pagamentoRecusado(
				eq(10L),
				eq("corr-10"),
				eq(91L),
				eq(2),
				eq("Cartao recusado pela operadora (cc_rejected_bad_filled_security_code)"));

		// Idempotente na recusa manual
		useCase.recusar(91L, "888", "Motivo");

		assertThat(useCase.processarNotificacaoWebhook("")).isEmpty();
	}

	@Test
	void deveBuscarPagamentoPorIdEPorOsIdOuLancar404() {
		Pagamento p = Pagamento.novoPendente(10L, 50L, "corr-10", new BigDecimal("165.00"), 1, Instant.now());
		p.setId(90L);
		when(pagamentoRepository.buscarPorId(90L)).thenReturn(Optional.of(p));
		when(pagamentoRepository.buscarPorId(999L)).thenReturn(Optional.empty());
		when(pagamentoRepository.buscarMaisRecentePorOrdemServicoId(10L)).thenReturn(Optional.of(p));
		when(pagamentoRepository.buscarMaisRecentePorOrdemServicoId(999L)).thenReturn(Optional.empty());

		BuscarPagamentoUseCase buscar = new BuscarPagamentoUseCase(pagamentoRepository);
		assertThat(buscar.buscarPorId(90L).id()).isEqualTo(90L);
		assertThat(buscar.buscarMaisRecentePorOsId(10L).id()).isEqualTo(90L);

		assertThatThrownBy(() -> buscar.buscarPorId(999L))
				.isInstanceOf(PagamentoNaoEncontradoException.class);
		assertThatThrownBy(() -> buscar.buscarMaisRecentePorOsId(999L))
				.isInstanceOf(PagamentoNaoEncontradoException.class);
	}
}

