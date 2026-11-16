package br.com.autocenterfiap.billing.application.exception;

public class OrcamentoNaoEncontradoException extends RuntimeException {

	public OrcamentoNaoEncontradoException(Long id) {
		super("Orcamento nao encontrado com o ID: " + id);
	}

	public OrcamentoNaoEncontradoException(String message) {
		super(message);
	}
}

