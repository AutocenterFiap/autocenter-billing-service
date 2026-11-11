package br.com.autocenterfiap.billing.infrastructure.config;

import br.com.autocenterfiap.billing.application.port.out.BillingEventPublisherPort;
import br.com.autocenterfiap.billing.application.port.out.OrcamentoRepositoryPort;
import br.com.autocenterfiap.billing.application.usecase.AprovarOrcamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.BuscarOrcamentoPorIdUseCase;
import br.com.autocenterfiap.billing.application.usecase.BuscarTodosOrcamentosUseCase;
import br.com.autocenterfiap.billing.application.usecase.CriarOrcamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.GerarOrcamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.ReprovarOrcamentoUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Monta os casos de uso (camada de aplicacao, livre de framework) a partir das ports,
 * seguindo a mesma estrutura de OrcamentoConfiguration do monolito e UseCaseConfig da Execucao.
 */
@Configuration
public class UseCaseConfig {

	@Bean
	public CriarOrcamentoUseCase criarOrcamentoUseCase(OrcamentoRepositoryPort orcamentoRepositoryPort) {
		return new CriarOrcamentoUseCase(orcamentoRepositoryPort);
	}

	@Bean
	public GerarOrcamentoUseCase gerarOrcamentoUseCase(
			OrcamentoRepositoryPort orcamentoRepositoryPort,
			CriarOrcamentoUseCase criarOrcamentoUseCase,
			BillingEventPublisherPort publisher) {
		return new GerarOrcamentoUseCase(orcamentoRepositoryPort, criarOrcamentoUseCase, publisher);
	}

	@Bean
	public BuscarOrcamentoPorIdUseCase buscarOrcamentoPorIdUseCase(OrcamentoRepositoryPort orcamentoRepositoryPort) {
		return new BuscarOrcamentoPorIdUseCase(orcamentoRepositoryPort);
	}

	@Bean
	public BuscarTodosOrcamentosUseCase buscarTodosOrcamentosUseCase(OrcamentoRepositoryPort orcamentoRepositoryPort) {
		return new BuscarTodosOrcamentosUseCase(orcamentoRepositoryPort);
	}

	@Bean
	public AprovarOrcamentoUseCase aprovarOrcamentoUseCase(
			OrcamentoRepositoryPort orcamentoRepositoryPort,
			BillingEventPublisherPort publisher) {
		return new AprovarOrcamentoUseCase(orcamentoRepositoryPort, publisher);
	}

	@Bean
	public ReprovarOrcamentoUseCase reprovarOrcamentoUseCase(
			OrcamentoRepositoryPort orcamentoRepositoryPort,
			BillingEventPublisherPort publisher) {
		return new ReprovarOrcamentoUseCase(orcamentoRepositoryPort, publisher);
	}
}

