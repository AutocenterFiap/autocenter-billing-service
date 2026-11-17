package br.com.autocenterfiap.billing.domain.entity;

import br.com.autocenterfiap.billing.domain.enums.StatusOrcamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrcamentoTest {

	@Test
	void deveValidarEAprovarEReprovarECancelarOrcamento() {
		Orcamento orcamento = Orcamento.builder()
				.id(1L)
				.ordemServicoId(10L)
				.clienteId(42L)
				.correlationId("corr-1")
				.valorTotal(new BigDecimal("165.00"))
				.statusOrcamento(StatusOrcamento.AGUARDANDO_APROVACAO)
				.build();

		orcamento.validarDominio();
		orcamento.aprovar();
		assertThat(orcamento.getStatusOrcamento()).isEqualTo(StatusOrcamento.APROVADO);
		assertThat(orcamento.cancelar("OS_CANCELADA")).isFalse();

		Orcamento outro = Orcamento.builder()
				.id(2L)
				.ordemServicoId(11L)
				.valorTotal(new BigDecimal("100.00"))
				.statusOrcamento(StatusOrcamento.AGUARDANDO_APROVACAO)
				.build();
		outro.reprovar();
		assertThat(outro.getStatusOrcamento()).isEqualTo(StatusOrcamento.REPROVADO);
		assertThat(outro.getMotivo()).isEqualTo("Cliente recusou o valor");

		Orcamento cancelavel = Orcamento.builder()
				.id(3L)
				.ordemServicoId(12L)
				.valorTotal(new BigDecimal("200.00"))
				.statusOrcamento(StatusOrcamento.AGUARDANDO_APROVACAO)
				.build();
		assertThat(cancelavel.cancelar("ORCAMENTO_REPROVADO")).isTrue();
		assertThat(cancelavel.getStatusOrcamento()).isEqualTo(StatusOrcamento.CANCELADO);
	}

	@Test
	void deveRejeitarDominioInvalido() {
		Orcamento semOs = Orcamento.builder()
				.valorTotal(new BigDecimal("100.00"))
				.statusOrcamento(StatusOrcamento.GERADO)
				.build();
		assertThatThrownBy(semOs::validarDominio).isInstanceOf(IllegalArgumentException.class);

		Orcamento valorZero = Orcamento.builder()
				.ordemServicoId(1L)
				.valorTotal(BigDecimal.ZERO)
				.statusOrcamento(StatusOrcamento.GERADO)
				.build();
		assertThatThrownBy(valorZero::validarDominio).isInstanceOf(IllegalArgumentException.class);

		Orcamento semStatus = Orcamento.builder()
				.ordemServicoId(1L)
				.valorTotal(new BigDecimal("50.00"))
				.build();
		assertThatThrownBy(semStatus::validarDominio).isInstanceOf(IllegalArgumentException.class);
	}
}

