package br.com.autocenterfiap.billing.application.exception;

public class ClienteNaoEncontradoException extends RuntimeException {

	public ClienteNaoEncontradoException(Long clienteId) {
		super("Cliente do orcamento nao encontrado no cadastro: " + clienteId);
	}
}
