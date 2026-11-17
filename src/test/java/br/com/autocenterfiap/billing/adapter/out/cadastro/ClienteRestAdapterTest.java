package br.com.autocenterfiap.billing.adapter.out.cadastro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.autocenterfiap.billing.application.exception.CadastroIndisponivelException;
import br.com.autocenterfiap.billing.application.port.out.ClientePort.ContatoCliente;
import br.com.autocenterfiap.billing.infrastructure.config.RepassaTokenInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ClienteRestAdapterTest {

	private static final String CLIENTE = "{\"id\":1,\"nome\":\"Joao\",\"email\":\"joao@email.com\",\"telefone\":\"11987654321\",\"status\":\"ATIVO\"}";

	private MockRestServiceServer servidor;
	private ClienteRestAdapter adapter;

	@BeforeEach
	void preparar() {
		var builder = RestClient.builder().baseUrl("http://cadastro").requestInterceptor(new RepassaTokenInterceptor());
		servidor = MockRestServiceServer.bindTo(builder).build();
		adapter = new ClienteRestAdapter(builder.build());
	}

	@AfterEach
	void limpar() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void mapeiaContatoQuandoCadastroRetorna200() {
		servidor.expect(requestTo("http://cadastro/v1/clientes/1")).andRespond(withSuccess(CLIENTE, MediaType.APPLICATION_JSON));

		assertThat(adapter.buscarContato(1L)).contains(new ContatoCliente("joao@email.com", "11987654321"));
	}

	@Test
	void retornaVazioQuandoCadastroRetorna404() {
		servidor.expect(requestTo("http://cadastro/v1/clientes/9")).andRespond(withResourceNotFound());

		assertThat(adapter.buscarContato(9L)).isEmpty();
	}

	@Test
	void lancaIndisponivelQuandoCadastroRetorna500() {
		servidor.expect(requestTo("http://cadastro/v1/clientes/1")).andRespond(withServerError());

		assertThatThrownBy(() -> adapter.buscarContato(1L)).isInstanceOf(CadastroIndisponivelException.class);
	}

	@Test
	void repassaAuthorization() {
		var entrada = new MockHttpServletRequest();
		entrada.addHeader("Authorization", "Bearer x");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(entrada));
		servidor.expect(requestTo("http://cadastro/v1/clientes/1")).andExpect(header("Authorization", "Bearer x"))
				.andRespond(withSuccess(CLIENTE, MediaType.APPLICATION_JSON));

		adapter.buscarContato(1L);
		servidor.verify();
	}
}
