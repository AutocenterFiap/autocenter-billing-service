package br.com.autocenterfiap.billing.adapter.in.handler;

import br.com.autocenterfiap.billing.application.exception.OrcamentoNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(OrcamentoNaoEncontradoException.class)
	public ResponseEntity<ErroResponse> naoEncontrado(OrcamentoNaoEncontradoException ex) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErroResponse> argumentoInvalido(IllegalArgumentException ex) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex) {
		String mensagem = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(e -> e.getField() + ": " + e.getDefaultMessage())
				.orElse("Requisicao invalida");
		return build(HttpStatus.BAD_REQUEST, mensagem);
	}

	private ResponseEntity<ErroResponse> build(HttpStatus status, String mensagem) {
		return ResponseEntity.status(status)
				.body(ErroResponse.de(status.value(), status.getReasonPhrase(), mensagem));
	}
}

