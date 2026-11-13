package br.com.autocenterfiap.billing.application.usecase;

import br.com.autocenterfiap.billing.application.dto.PagamentoOutput;
import br.com.autocenterfiap.billing.application.exception.PagamentoNaoEncontradoException;
import br.com.autocenterfiap.billing.application.port.out.BillingEventPublisherPort;
import br.com.autocenterfiap.billing.application.port.out.MercadoPagoPort;
import br.com.autocenterfiap.billing.application.port.out.PagamentoRepositoryPort;
import br.com.autocenterfiap.billing.domain.entity.Pagamento;
import br.com.autocenterfiap.billing.domain.enums.StatusPagamento;

import java.util.Optional;

/**
 * Caso de uso para processar o resultado de um pagamento (via Webhook do Mercado Pago
 * ou simulacao/confirmacao direta) e publicar {@code 09 - pagamento.confirmado} ou
 * {@code 17 - pagamento.recusado}.
 */
public class ProcessarPagamentoUseCase {

	private final PagamentoRepositoryPort pagamentoRepository;
	private final MercadoPagoPort mercadoPagoPort;
	private final BillingEventPublisherPort publisher;

	public ProcessarPagamentoUseCase(
			PagamentoRepositoryPort pagamentoRepository,
			MercadoPagoPort mercadoPagoPort,
			BillingEventPublisherPort publisher) {
		this.pagamentoRepository = pagamentoRepository;
		this.mercadoPagoPort = mercadoPagoPort;
		this.publisher = publisher;
	}

	public Optional<PagamentoOutput> processarNotificacaoWebhook(String mpPaymentId) {
		if (mpPaymentId == null || mpPaymentId.isBlank()) {
			return Optional.empty();
		}
		Optional<MercadoPagoPort.DadosPagamentoExterno> dadosOpt =
				mercadoPagoPort.consultarPagamento(mpPaymentId);
		if (dadosOpt.isEmpty()) {
			return Optional.empty();
		}
		MercadoPagoPort.DadosPagamentoExterno dados = dadosOpt.get();
		if (dados.externalReferencePagamentoId() == null) {
			return Optional.empty();
		}

		Optional<Pagamento> pagamentoOpt =
				pagamentoRepository.buscarPorId(dados.externalReferencePagamentoId());
		if (pagamentoOpt.isEmpty()) {
			return Optional.empty();
		}
		Pagamento pagamento = pagamentoOpt.get();

		if (dados.aprovado()) {
			return Optional.of(confirmarInterno(pagamento, dados.mpPaymentId()));
		}
		if (dados.recusadoOuCancelado()) {
			String motivo = (dados.statusDetail() != null && !dados.statusDetail().isBlank())
					? "Cartao recusado pela operadora (" + dados.statusDetail() + ")"
					: "Cartao recusado pela operadora";
			return Optional.of(recusarInterno(pagamento, dados.mpPaymentId(), motivo));
		}
		return Optional.of(PagamentoOutput.de(pagamento));
	}

	public PagamentoOutput confirmar(Long pagamentoId, String mpPaymentId) {
		Pagamento pagamento = pagamentoRepository.buscarPorId(pagamentoId)
				.orElseThrow(() -> new PagamentoNaoEncontradoException(pagamentoId));
		return confirmarInterno(pagamento, mpPaymentId);
	}

	public PagamentoOutput recusar(Long pagamentoId, String mpPaymentId, String motivo) {
		Pagamento pagamento = pagamentoRepository.buscarPorId(pagamentoId)
				.orElseThrow(() -> new PagamentoNaoEncontradoException(pagamentoId));
		return recusarInterno(pagamento, mpPaymentId, motivo);
	}

	private PagamentoOutput confirmarInterno(Pagamento pagamento, String mpPaymentId) {
		if (pagamento.estaConfirmado()) {
			return PagamentoOutput.de(pagamento);
		}
		if (pagamento.getStatusPagamento() == StatusPagamento.CANCELADO) {
			if (mpPaymentId != null && !mpPaymentId.isBlank()) {
				pagamento.setMpPaymentId(mpPaymentId);
				MercadoPagoPort.ResultadoEstorno estorno = mercadoPagoPort.estornarPagamento(
						pagamento.getId(), mpPaymentId, pagamento.getValor());
				if (estorno.sucesso()) {
					pagamento.estornar("Pagamento recebido apos cancelamento da OS");
					Pagamento salvo = pagamentoRepository.salvar(pagamento);
					publisher.pagamentoEstornado(
							salvo.getOrdemServicoId(), salvo.getCorrelationId(), salvo.getId());
					return PagamentoOutput.de(salvo);
				}
			}
			return PagamentoOutput.de(pagamento);
		}
		pagamento.confirmar(mpPaymentId);
		Pagamento salvo = pagamentoRepository.salvar(pagamento);
		publisher.pagamentoConfirmado(
				salvo.getOrdemServicoId(),
				salvo.getCorrelationId(),
				salvo.getId(),
				salvo.getValor());
		return PagamentoOutput.de(salvo);
	}

	private PagamentoOutput recusarInterno(Pagamento pagamento, String mpPaymentId, String motivo) {
		if (pagamento.estaRecusado()) {
			return PagamentoOutput.de(pagamento);
		}
		pagamento.recusar(mpPaymentId, motivo);
		Pagamento salvo = pagamentoRepository.salvar(pagamento);
		publisher.pagamentoRecusado(
				salvo.getOrdemServicoId(),
				salvo.getCorrelationId(),
				salvo.getId(),
				salvo.getTentativa(),
				salvo.getMotivo());
		return PagamentoOutput.de(salvo);
	}
}

