package br.com.autocenterfiap.billing.application.usecase;

import br.com.autocenterfiap.billing.application.dto.PagamentoOutput;
import br.com.autocenterfiap.billing.application.port.out.BillingEventPublisherPort;
import br.com.autocenterfiap.billing.application.port.out.MercadoPagoPort;
import br.com.autocenterfiap.billing.application.port.out.PagamentoRepositoryPort;
import br.com.autocenterfiap.billing.domain.entity.Pagamento;
import br.com.autocenterfiap.billing.domain.enums.StatusPagamento;

import java.util.Optional;

/**
 * Caso de uso da Saga (compensacao) para tratar {@code 19 - billing.estornar-pagamento}.
 * Solicita o estorno no Mercado Pago e publica {@code 20 - pagamento.estornado} ou
 * {@code 21 - pagamento.estorno-falhou}.
 */
public class EstornarPagamentoUseCase {

	private final PagamentoRepositoryPort pagamentoRepository;
	private final MercadoPagoPort mercadoPagoPort;
	private final BillingEventPublisherPort publisher;

	public EstornarPagamentoUseCase(
			PagamentoRepositoryPort pagamentoRepository,
			MercadoPagoPort mercadoPagoPort,
			BillingEventPublisherPort publisher) {
		this.pagamentoRepository = pagamentoRepository;
		this.mercadoPagoPort = mercadoPagoPort;
		this.publisher = publisher;
	}

	public PagamentoOutput executar(Long osId, Long pagamentoId, String motivo, String correlationId) {
		Optional<Pagamento> pagamentoOpt = buscarPagamento(osId, pagamentoId);
		if (pagamentoOpt.isEmpty()) {
			publisher.pagamentoEstornoFalhou(
					osId,
					correlationId,
					pagamentoId,
					"Pagamento nao encontrado para estorno");
			return null;
		}

		Pagamento pagamento = pagamentoOpt.get();
		Long osIdEfetivo = osId != null ? osId : pagamento.getOrdemServicoId();
		String correlationEfetivo = (correlationId != null && !correlationId.isBlank())
				? correlationId
				: pagamento.getCorrelationId();

		if (pagamento.getStatusPagamento() == StatusPagamento.ESTORNADO) {
			return PagamentoOutput.de(pagamento);
		}

		if (pagamento.getStatusPagamento() != StatusPagamento.CONFIRMADO
				&& pagamento.getStatusPagamento() != StatusPagamento.ESTORNO_FALHOU) {
			String erro = "Pagamento nao esta confirmado para estorno (status: "
					+ pagamento.getStatusPagamento() + ")";
			publisher.pagamentoEstornoFalhou(osIdEfetivo, correlationEfetivo, pagamento.getId(), erro);
			return PagamentoOutput.de(pagamento);
		}

		MercadoPagoPort.ResultadoEstorno resultado = mercadoPagoPort.estornarPagamento(
				pagamento.getId(),
				pagamento.getMpPaymentId(),
				pagamento.getValor());

		if (resultado.sucesso()) {
			String motivoFinal = (motivo == null || motivo.isBlank()) ? "Estorno solicitado pela Saga" : motivo;
			pagamento.estornar(motivoFinal);
			Pagamento salvo = pagamentoRepository.salvar(pagamento);
			publisher.pagamentoEstornado(osIdEfetivo, correlationEfetivo, salvo.getId());
			return PagamentoOutput.de(salvo);
		}

		String erro = (resultado.motivoErro() == null || resultado.motivoErro().isBlank())
				? "Operadora recusou o estorno"
				: resultado.motivoErro();
		pagamento.falharEstorno(erro);
		Pagamento salvo = pagamentoRepository.salvar(pagamento);
		publisher.pagamentoEstornoFalhou(osIdEfetivo, correlationEfetivo, salvo.getId(), erro);
		return PagamentoOutput.de(salvo);
	}

	private Optional<Pagamento> buscarPagamento(Long osId, Long pagamentoId) {
		if (pagamentoId != null) {
			return pagamentoRepository.buscarPorId(pagamentoId);
		}
		if (osId != null) {
			return pagamentoRepository.buscarMaisRecentePorOrdemServicoId(osId);
		}
		return Optional.empty();
	}
}

