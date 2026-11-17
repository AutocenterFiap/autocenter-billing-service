package br.com.autocenterfiap.billing.application.port.out;

import java.util.Optional;

/**
 * Contato do cliente no Cadastro Service. Cliente inexistente: {@link Optional#empty()}; falha de comunicacao:
 * {@code CadastroIndisponivelException}.
 */
public interface ClientePort {

	Optional<ContatoCliente> buscarContato(Long clienteId);

	record ContatoCliente(String email, String telefone) {
	}
}
