package br.com.autocenterfiap.billing.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.autocenterfiap.billing.application.exception.ClienteNaoEncontradoException;
import br.com.autocenterfiap.billing.application.exception.OrcamentoNaoEncontradoException;
import br.com.autocenterfiap.billing.application.port.out.ClientePort;
import br.com.autocenterfiap.billing.application.port.out.ClientePort.ContatoCliente;
import br.com.autocenterfiap.billing.application.port.out.OrcamentoRepositoryPort;
import br.com.autocenterfiap.billing.domain.entity.Orcamento;
import br.com.autocenterfiap.billing.domain.enums.TipoEnvio;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EnviarOrcamentoUseCaseTest {

	private final OrcamentoRepositoryPort orcamentos = mock(OrcamentoRepositoryPort.class);
	private final ClientePort clientes = mock(ClientePort.class);
	private final EnviarOrcamentoUseCase enviar = new EnviarOrcamentoUseCase(orcamentos, clientes);

	private void orcamento(Long clienteId) {
		when(orcamentos.buscarPorId(7L)).thenReturn(Optional.of(Orcamento.builder().id(7L).clienteId(clienteId).build()));
	}

	@Test
	void emailUsaEmailDoCadastro() {
		orcamento(1L);
		when(clientes.buscarContato(1L)).thenReturn(Optional.of(new ContatoCliente("joao@email.com", "11987654321")));

		assertThat(enviar.executar(7L, TipoEnvio.EMAIL)).isEqualTo("Orçamento 7 enviado via EMAIL para joao@email.com");
	}

	@Test
	void whatsappUsaTelefone() {
		orcamento(1L);
		when(clientes.buscarContato(1L)).thenReturn(Optional.of(new ContatoCliente("joao@email.com", "11987654321")));

		assertThat(enviar.executar(7L, TipoEnvio.WHATSAPP)).isEqualTo("Orçamento 7 enviado via WHATSAPP para 11987654321");
	}

	@Test
	void impressaoNaoConsultaCadastro() {
		orcamento(1L);

		assertThat(enviar.executar(7L, TipoEnvio.IMPRESSAO)).isEqualTo("Orçamento 7 enviado via IMPRESSAO com sucesso!");
		verifyNoInteractions(clientes);
	}

	@Test
	void clienteInexistenteLanca() {
		orcamento(1L);
		when(clientes.buscarContato(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> enviar.executar(7L, TipoEnvio.EMAIL)).isInstanceOf(ClienteNaoEncontradoException.class);
	}

	@Test
	void orcamentoSemClienteLanca() {
		orcamento(null);

		assertThatThrownBy(() -> enviar.executar(7L, TipoEnvio.EMAIL)).isInstanceOf(ClienteNaoEncontradoException.class);
		verifyNoInteractions(clientes);
	}

	@Test
	void orcamentoInexistenteLanca() {
		when(orcamentos.buscarPorId(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> enviar.executar(7L, TipoEnvio.EMAIL)).isInstanceOf(OrcamentoNaoEncontradoException.class);
	}
}
