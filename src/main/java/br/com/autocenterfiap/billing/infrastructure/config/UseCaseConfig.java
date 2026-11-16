package br.com.autocenterfiap.billing.infrastructure.config;

import br.com.autocenterfiap.billing.application.port.out.BillingEventPublisherPort;
import br.com.autocenterfiap.billing.application.port.out.MercadoPagoPort;
import br.com.autocenterfiap.billing.application.port.out.OrcamentoRepositoryPort;
import br.com.autocenterfiap.billing.application.port.out.PagamentoRepositoryPort;
import br.com.autocenterfiap.billing.application.usecase.AprovarOrcamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.BuscarOrcamentoPorIdUseCase;
import br.com.autocenterfiap.billing.application.usecase.BuscarPagamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.BuscarTodosOrcamentosUseCase;
import br.com.autocenterfiap.billing.application.usecase.CancelarBillingPorOsUseCase;
import br.com.autocenterfiap.billing.application.usecase.CriarOrcamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.EstornarPagamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.GerarCobrancaUseCase;
import br.com.autocenterfiap.billing.application.usecase.GerarOrcamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.ProcessarPagamentoUseCase;
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

	@Bean
	public GerarCobrancaUseCase gerarCobrancaUseCase(
			PagamentoRepositoryPort pagamentoRepositoryPort,
			MercadoPagoPort mercadoPagoPort,
			BillingEventPublisherPort publisher) {
		return new GerarCobrancaUseCase(pagamentoRepositoryPort, mercadoPagoPort, publisher);
	}

	@Bean
	public ProcessarPagamentoUseCase processarPagamentoUseCase(
			PagamentoRepositoryPort pagamentoRepositoryPort,
			MercadoPagoPort mercadoPagoPort,
			BillingEventPublisherPort publisher) {
		return new ProcessarPagamentoUseCase(pagamentoRepositoryPort, mercadoPagoPort, publisher);
	}

	@Bean
	public BuscarPagamentoUseCase buscarPagamentoUseCase(PagamentoRepositoryPort pagamentoRepositoryPort) {
		return new BuscarPagamentoUseCase(pagamentoRepositoryPort);
	}

	@Bean
	public EstornarPagamentoUseCase estornarPagamentoUseCase(
			PagamentoRepositoryPort pagamentoRepositoryPort,
			MercadoPagoPort mercadoPagoPort,
			BillingEventPublisherPort publisher) {
		return new EstornarPagamentoUseCase(pagamentoRepositoryPort, mercadoPagoPort, publisher);
	}

	@Bean
	public CancelarBillingPorOsUseCase cancelarBillingPorOsUseCase(
			OrcamentoRepositoryPort orcamentoRepositoryPort,
			PagamentoRepositoryPort pagamentoRepositoryPort,
			MercadoPagoPort mercadoPagoPort) {
		return new CancelarBillingPorOsUseCase(orcamentoRepositoryPort, pagamentoRepositoryPort, mercadoPagoPort);
	}
}
