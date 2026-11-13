package br.com.autocenterfiap.billing.application.usecase;

import br.com.autocenterfiap.billing.application.dto.CriarOrcamentoInput;
import br.com.autocenterfiap.billing.application.dto.OrcamentoOutput;
import br.com.autocenterfiap.billing.application.port.out.BillingEventPublisherPort;
import br.com.autocenterfiap.billing.application.port.out.OrcamentoRepositoryPort;
import br.com.autocenterfiap.billing.domain.entity.Orcamento;
import br.com.autocenterfiap.billing.domain.enums.StatusOrcamento;

import java.math.BigDecimal;

/**
 * Gera o orcamento ao receber o comando Kafka {@code 04 - billing.gerar-orcamento}
 * e publica {@code 05 - orcamento.gerado} (ou {@code 15 - orcamento.falhou} em caso de erro).
 */
public class GerarOrcamentoUseCase {

	private final OrcamentoRepositoryPort orcamentoRepositoryPort;
	private final CriarOrcamentoUseCase criarOrcamentoUseCase;
	private final BillingEventPublisherPort publisher;

	public GerarOrcamentoUseCase(OrcamentoRepositoryPort orcamentoRepositoryPort,
	                             CriarOrcamentoUseCase criarOrcamentoUseCase,
	                             BillingEventPublisherPort publisher) {
		this.orcamentoRepositoryPort = orcamentoRepositoryPort;
		this.criarOrcamentoUseCase = criarOrcamentoUseCase;
		this.publisher = publisher;
	}

	public OrcamentoOutput executar(Long osId, Long clienteId, BigDecimal valorTotal, String correlationId) {
		if (osId == null || valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
			publisher.orcamentoFalhou(osId, correlationId, "Falha ao calcular o orçamento");
			return null;
		}

		// Se ja houver um orcamento aguardando aprovacao para a mesma OS (ex.: novo diagnostico),
		// cancela o anterior antes de gerar o novo.
		orcamentoRepositoryPort.buscarOrcamentoAguardandoAprovacaoPorOS(osId)
				.ifPresent(anterior -> {
					if (anterior.cancelar("Substituido por novo orcamento")) {
						orcamentoRepositoryPort.salvar(anterior);
					}
				});

		CriarOrcamentoInput input = CriarOrcamentoInput.builder()
				.ordemServicoId(osId)
				.clienteId(clienteId)
				.correlationId(correlationId)
				.valorTotal(valorTotal)
				.statusOrcamento(StatusOrcamento.AGUARDANDO_APROVACAO)
				.build();

		OrcamentoOutput criado = criarOrcamentoUseCase.executar(input);
		Orcamento entidade = Orcamento.builder()
				.id(criado.getId())
				.ordemServicoId(criado.getOrdemServicoId())
				.clienteId(criado.getClienteId())
				.correlationId(criado.getCorrelationId())
				.valorTotal(criado.getValorTotal())
				.statusOrcamento(criado.getStatusOrcamento())
				.build();
		publisher.orcamentoGerado(entidade);
		return criado;
	}
}

