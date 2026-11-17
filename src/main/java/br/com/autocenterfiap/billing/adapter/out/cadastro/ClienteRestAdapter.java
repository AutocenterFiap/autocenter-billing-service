package br.com.autocenterfiap.billing.adapter.out.cadastro;

import br.com.autocenterfiap.billing.application.exception.CadastroIndisponivelException;
import br.com.autocenterfiap.billing.application.port.out.ClientePort;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Busca o contato do cliente no Cadastro Service por REST. */
@Component
public class ClienteRestAdapter implements ClientePort {

	private final RestClient cliente;

	public ClienteRestAdapter(@Qualifier("cadastroRestClient") RestClient cliente) {
		this.cliente = cliente;
	}

	@Override
	public Optional<ContatoCliente> buscarContato(Long clienteId) {
		try {
			var resposta = cliente.get().uri("/v1/clientes/{id}", clienteId).retrieve().body(ClienteResposta.class);
			return Optional.ofNullable(resposta).map(r -> new ContatoCliente(r.email(), r.telefone()));
		} catch (HttpClientErrorException.NotFound e) {
			return Optional.empty();
		} catch (RestClientException e) {
			throw new CadastroIndisponivelException("Cadastro indisponivel ao consultar o cliente " + clienteId, e);
		}
	}

	record ClienteResposta(String email, String telefone) {
	}
}
