package br.com.autocenterfiap.billing.domain.entity;

import br.com.autocenterfiap.billing.domain.enums.StatusPagamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagamentoTest {

	@Test
	void deveCriarPagamentoPendenteETransicionarEstados() {
		Instant expiraEm = Instant.parse("2026-01-01T00:30:00Z");
		Pagamento p = Pagamento.novoPendente(
				10L,
				50L,
				"corr-10",
				new BigDecimal("165.00"),
				null,
				expiraEm);

		assertThat(p.getOrdemServicoId()).isEqualTo(10L);
		assertThat(p.getOrcamentoId()).isEqualTo(50L);
		assertThat(p.getTentativa()).isEqualTo(1);
		assertThat(p.getStatusPagamento()).isEqualTo(StatusPagamento.PENDENTE);
		assertThat(p.getExpiraEm()).isEqualTo(expiraEm);

		p.registrarLinkCheckout("pref-123", "https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=pref-123");
		assertThat(p.getMpPreferenceId()).isEqualTo("pref-123");
		assertThat(p.getUrlCheckout()).contains("pref-123");

		p.recusar("mp-pay-1", null);
		assertThat(p.estaRecusado()).isTrue();
		assertThat(p.getMpPaymentId()).isEqualTo("mp-pay-1");
		assertThat(p.getMotivo()).isEqualTo("Pagamento recusado pela operadora");

		p.confirmar("mp-pay-2");
		assertThat(p.estaConfirmado()).isTrue();
		assertThat(p.getMpPaymentId()).isEqualTo("mp-pay-2");
		assertThat(p.getMotivo()).isNull();

		p.estornar("Sem estoque");
		assertThat(p.getStatusPagamento()).isEqualTo(StatusPagamento.ESTORNADO);
		assertThat(p.getMotivo()).isEqualTo("Sem estoque");

		p.falharEstorno("Operadora indisponivel");
		assertThat(p.getStatusPagamento()).isEqualTo(StatusPagamento.ESTORNO_FALHOU);

		p.cancelar("OS cancelada");
		assertThat(p.getStatusPagamento()).isEqualTo(StatusPagamento.CANCELADO);
	}

	@Test
	void deveValidarCamposObrigatoriosEBobinarGettersSetters() {
		assertThatThrownBy(() -> Pagamento.novoPendente(null, 1L, "c", BigDecimal.TEN, 1, Instant.now()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Pagamento.novoPendente(1L, null, "c", BigDecimal.TEN, 1, Instant.now()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Pagamento.novoPendente(1L, 1L, "c", BigDecimal.ZERO, 1, Instant.now()))
				.isInstanceOf(IllegalArgumentException.class);

		Pagamento vazio = new Pagamento();
		LocalDateTime agora = LocalDateTime.now();
		vazio.setId(90L);
		vazio.setOrdemServicoId(10L);
		vazio.setOrcamentoId(50L);
		vazio.setCorrelationId("corr");
		vazio.setValor(new BigDecimal("100.00"));
		vazio.setTentativa(2);
		vazio.setStatusPagamento(StatusPagamento.PENDENTE);
		vazio.setUrlCheckout("https://url");
		vazio.setExpiraEm(Instant.now());
		vazio.setMpPreferenceId("pref");
		vazio.setMpPaymentId("pay");
		vazio.setMotivo("motivo");
		vazio.setDataCriacao(agora);
		vazio.setDataUltimaAtualizacao(agora);

		assertThat(vazio.getId()).isEqualTo(90L);
		assertThat(vazio.getCorrelationId()).isEqualTo("corr");
		assertThat(vazio.getDataCriacao()).isEqualTo(agora);
		assertThat(vazio.getDataUltimaAtualizacao()).isEqualTo(agora);
	}
}

