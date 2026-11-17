package br.com.autocenterfiap.billing.application.usecase;

import br.com.autocenterfiap.billing.application.exception.ClienteNaoEncontradoException;
import br.com.autocenterfiap.billing.application.exception.OrcamentoNaoEncontradoException;
import br.com.autocenterfiap.billing.application.port.out.ClientePort;
import br.com.autocenterfiap.billing.application.port.out.OrcamentoRepositoryPort;
import br.com.autocenterfiap.billing.domain.entity.Orcamento;
import br.com.autocenterfiap.billing.domain.enums.TipoEnvio;

/**
 * Envia o orcamento ao cliente pelo canal escolhido, com o contato vindo do Cadastro Service. O envio em si
 * e simulado: o retorno informa o destino.
 */
public class EnviarOrcamentoUseCase {

	private final OrcamentoRepositoryPort orcamentos;
	private final ClientePort clientes;

	public EnviarOrcamentoUseCase(OrcamentoRepositoryPort orcamentos, ClientePort clientes) {
		this.orcamentos = orcamentos;
		this.clientes = clientes;
	}

	public String executar(Long orcamentoId, TipoEnvio tipo) {
		Orcamento orcamento = orcamentos.buscarPorId(orcamentoId)
				.orElseThrow(() -> new OrcamentoNaoEncontradoException(orcamentoId));
		if (tipo == TipoEnvio.IMPRESSAO) {
			return String.format("Orçamento %d enviado via IMPRESSAO com sucesso!", orcamentoId);
		}
		Long clienteId = orcamento.getClienteId();
		if (clienteId == null) {
			throw new ClienteNaoEncontradoException(null);
		}
		var contato = clientes.buscarContato(clienteId)
				.orElseThrow(() -> new ClienteNaoEncontradoException(clienteId));
		String destino = tipo == TipoEnvio.EMAIL ? contato.email() : contato.telefone();
		return String.format("Orçamento %d enviado via %s para %s", orcamentoId, tipo.name(), destino);
	}
}
