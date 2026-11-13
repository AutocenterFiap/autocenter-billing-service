package br.com.autocenterfiap.billing.application.exception;

public class PagamentoNaoEncontradoException extends RuntimeException {

	public PagamentoNaoEncontradoException(Long id) {
		super("Pagamento " + id + " nao encontrado");
	}

	public PagamentoNaoEncontradoException(String mensagem) {
		super(mensagem);
	}
}

