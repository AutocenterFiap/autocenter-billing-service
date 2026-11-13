package br.com.autocenterfiap.billing.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Porta de saida para integracao com o Mercado Pago (Checkout Pro).
 */
public interface MercadoPagoPort {

	PreferenciaGerada criarPreferenciaCheckoutPro(
			Long pagamentoId,
			Long osId,
			Long orcamentoId,
			BigDecimal valor,
			Integer tentativa,
			Instant expiraEm);

	Optional<DadosPagamentoExterno> consultarPagamento(String mpPaymentId);

	ResultadoEstorno estornarPagamento(Long pagamentoId, String mpPaymentId, BigDecimal valor);

	default void expirarPreferencia(String preferenceId) {
	}

	record PreferenciaGerada(String preferenceId, String urlCheckout) {
	}

	record DadosPagamentoExterno(
			String mpPaymentId,
			Long externalReferencePagamentoId,
			String status,
			String statusDetail,
			BigDecimal valor) {

		public boolean aprovado() {
			return "approved".equalsIgnoreCase(status);
		}

		public boolean recusadoOuCancelado() {
			return "rejected".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status);
		}
	}

	record ResultadoEstorno(boolean sucesso, String refundId, String motivoErro) {
	}
}

