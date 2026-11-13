package br.com.autocenterfiap.billing.application.usecase;

import br.com.autocenterfiap.billing.application.dto.PagamentoOutput;
import br.com.autocenterfiap.billing.application.port.out.BillingEventPublisherPort;
import br.com.autocenterfiap.billing.application.port.out.MercadoPagoPort;
import br.com.autocenterfiap.billing.application.port.out.PagamentoRepositoryPort;
import br.com.autocenterfiap.billing.domain.entity.Pagamento;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Caso de uso da Saga para tratar o comando {@code 07 - billing.gerar-cobranca}.
 * Cria o registro de pagamento pendente, gera a preferencia Checkout Pro no Mercado Pago
 * e publica {@code 08 - pagamento.link-gerado}.
 */
public class GerarCobrancaUseCase {

	private static final int VALIDADE_PADRAO_MINUTOS = 30;

	private final PagamentoRepositoryPort pagamentoRepository;
	private final MercadoPagoPort mercadoPagoPort;
	private final BillingEventPublisherPort publisher;

	public GerarCobrancaUseCase(
			PagamentoRepositoryPort pagamentoRepository,
			MercadoPagoPort mercadoPagoPort,
			BillingEventPublisherPort publisher) {
		this.pagamentoRepository = pagamentoRepository;
		this.mercadoPagoPort = mercadoPagoPort;
		this.publisher = publisher;
	}

	public PagamentoOutput executar(
			Long osId,
			Long orcamentoId,
			BigDecimal valorTotal,
			Integer tentativa,
			Integer validadeMinutos,
			String correlationId) {
		int minutos = (validadeMinutos == null || validadeMinutos <= 0)
				? VALIDADE_PADRAO_MINUTOS
				: validadeMinutos;
		Instant expiraEm = Instant.now()
				.plus(Duration.ofMinutes(minutos))
				.truncatedTo(ChronoUnit.SECONDS);

		Pagamento pendente = Pagamento.novoPendente(
				osId,
				orcamentoId,
				correlationId,
				valorTotal,
				tentativa,
				expiraEm);
		Pagamento salvo = pagamentoRepository.salvar(pendente);

		MercadoPagoPort.PreferenciaGerada preferencia = mercadoPagoPort.criarPreferenciaCheckoutPro(
				salvo.getId(),
				osId,
				orcamentoId,
				valorTotal,
				salvo.getTentativa(),
				expiraEm);

		salvo.registrarLinkCheckout(preferencia.preferenceId(), preferencia.urlCheckout());
		Pagamento atualizado = pagamentoRepository.salvar(salvo);

		publisher.pagamentoLinkGerado(
				atualizado.getOrdemServicoId(),
				atualizado.getCorrelationId(),
				atualizado.getId(),
				atualizado.getUrlCheckout(),
				atualizado.getTentativa(),
				atualizado.getExpiraEm());

		return PagamentoOutput.de(atualizado);
	}
}

