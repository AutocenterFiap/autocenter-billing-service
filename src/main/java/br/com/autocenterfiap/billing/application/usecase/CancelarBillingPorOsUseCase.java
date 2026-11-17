package br.com.autocenterfiap.billing.application.usecase;

import br.com.autocenterfiap.billing.application.port.out.MercadoPagoPort;
import br.com.autocenterfiap.billing.application.port.out.OrcamentoRepositoryPort;
import br.com.autocenterfiap.billing.application.port.out.PagamentoRepositoryPort;
import br.com.autocenterfiap.billing.domain.entity.Orcamento;
import br.com.autocenterfiap.billing.domain.entity.Pagamento;
import br.com.autocenterfiap.billing.domain.enums.StatusPagamento;

import java.util.List;

/**
 * Caso de uso da Saga para tratar {@code 22 - os.cancelada}.
 * Cancela orcamentos nao terminais e pagamentos pendentes vinculados a OS cancelada,
 * alem de expirar a preferencia Checkout Pro no Mercado Pago quando aplicavel.
 */
public class CancelarBillingPorOsUseCase {

	private final OrcamentoRepositoryPort orcamentoRepository;
	private final PagamentoRepositoryPort pagamentoRepository;
	private final MercadoPagoPort mercadoPagoPort;

	public CancelarBillingPorOsUseCase(
			OrcamentoRepositoryPort orcamentoRepository,
			PagamentoRepositoryPort pagamentoRepository,
			MercadoPagoPort mercadoPagoPort) {
		this.orcamentoRepository = orcamentoRepository;
		this.pagamentoRepository = pagamentoRepository;
		this.mercadoPagoPort = mercadoPagoPort;
	}

	public void executar(Long osId, String motivo) {
		if (osId == null) {
			return;
		}
		String motivoFinal = (motivo == null || motivo.isBlank()) ? "OS cancelada" : motivo;

		List<Orcamento> orcamentos = orcamentoRepository.buscarPorOrdemServicoId(osId);
		for (Orcamento orcamento : orcamentos) {
			if (orcamento.cancelar(motivoFinal)) {
				orcamentoRepository.salvar(orcamento);
			}
		}

		List<Pagamento> pagamentos = pagamentoRepository.buscarTodosPorOrdemServicoId(osId);
		for (Pagamento pagamento : pagamentos) {
			if (pagamento.getStatusPagamento() == StatusPagamento.PENDENTE) {
				pagamento.cancelar(motivoFinal);
				pagamentoRepository.salvar(pagamento);
				if (pagamento.getMpPreferenceId() != null && !pagamento.getMpPreferenceId().isBlank()) {
					mercadoPagoPort.expirarPreferencia(pagamento.getMpPreferenceId());
				}
			}
		}
	}
}

