package br.com.autocenterfiap.billing.adapter.in.controller;

import br.com.autocenterfiap.billing.application.exception.CadastroIndisponivelException;
import br.com.autocenterfiap.billing.application.exception.ClienteNaoEncontradoException;
import br.com.autocenterfiap.billing.application.usecase.EnviarOrcamentoUseCase;
import br.com.autocenterfiap.billing.domain.enums.TipoEnvio;
import br.com.autocenterfiap.billing.adapter.in.handler.GlobalExceptionHandler;
import br.com.autocenterfiap.billing.application.dto.OrcamentoOutput;
import br.com.autocenterfiap.billing.application.dto.PageResult;
import br.com.autocenterfiap.billing.application.exception.OrcamentoNaoEncontradoException;
import br.com.autocenterfiap.billing.application.usecase.AprovarOrcamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.BuscarOrcamentoPorIdUseCase;
import br.com.autocenterfiap.billing.application.usecase.BuscarTodosOrcamentosUseCase;
import br.com.autocenterfiap.billing.application.usecase.ReprovarOrcamentoUseCase;
import br.com.autocenterfiap.billing.domain.enums.StatusOrcamento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrcamentoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrcamentoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private BuscarOrcamentoPorIdUseCase buscarPorIdUseCase;

	@MockBean
	private BuscarTodosOrcamentosUseCase buscarTodosUseCase;

	@MockBean
	private AprovarOrcamentoUseCase aprovarUseCase;

	@MockBean
	private ReprovarOrcamentoUseCase reprovarUseCase;

	@MockBean
	private EnviarOrcamentoUseCase enviarUseCase;

	@Test
	void deveAprovarReprovarEnviarBuscarEListarOrcamentos() throws Exception {
		OrcamentoOutput aprovado = OrcamentoOutput.builder()
				.id(501L)
				.ordemServicoId(10L)
				.valorTotal(new BigDecimal("165.00"))
				.statusOrcamento(StatusOrcamento.APROVADO)
				.build();
		when(aprovarUseCase.executar(501L)).thenReturn(aprovado);

		mockMvc.perform(patch("/v1/orcamentos/501/aprovar"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(501))
				.andExpect(jsonPath("$.statusOrcamento").value("APROVADO"));

		OrcamentoOutput reprovado = OrcamentoOutput.builder()
				.id(501L)
				.ordemServicoId(10L)
				.valorTotal(new BigDecimal("165.00"))
				.statusOrcamento(StatusOrcamento.REPROVADO)
				.motivo("Muito caro")
				.build();
		when(reprovarUseCase.executar(501L, "Muito caro")).thenReturn(reprovado);

		mockMvc.perform(patch("/v1/orcamentos/501/reprovar")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"motivo\":\"Muito caro\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.statusOrcamento").value("REPROVADO"))
				.andExpect(jsonPath("$.motivo").value("Muito caro"));

		when(enviarUseCase.executar(501L, TipoEnvio.EMAIL)).thenReturn("Orçamento 501 enviado via EMAIL para joao@email.com");
		mockMvc.perform(post("/v1/orcamentos/501/enviar-cliente")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"tipo\":\"EMAIL\"}"))
				.andExpect(status().isOk())
				.andExpect(content().string("Orçamento 501 enviado via EMAIL para joao@email.com"));

		when(buscarPorIdUseCase.executar(501L)).thenReturn(aprovado);
		mockMvc.perform(get("/v1/orcamentos/501"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(501));

		when(buscarPorIdUseCase.buscarPorOsId(10L)).thenReturn(List.of(aprovado));
		mockMvc.perform(get("/v1/orcamentos/os/10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].ordemServicoId").value(10));

		when(buscarTodosUseCase.executar(eq(StatusOrcamento.APROVADO), any()))
				.thenReturn(new PageResult<>(List.of(aprovado), 0, 20, 1, 1));
		mockMvc.perform(get("/v1/orcamentos").param("status", "APROVADO"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(501));
	}

	@Test
	void deveRetornar404E400NosErros() throws Exception {
		when(buscarPorIdUseCase.executar(999L)).thenThrow(new OrcamentoNaoEncontradoException(999L));

		mockMvc.perform(get("/v1/orcamentos/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));

		mockMvc.perform(post("/v1/orcamentos/501/enviar-cliente")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void enviarComCadastroForaRetorna503() throws Exception {
		when(enviarUseCase.executar(501L, TipoEnvio.EMAIL))
				.thenThrow(new CadastroIndisponivelException("fora", new RuntimeException()));

		mockMvc.perform(post("/v1/orcamentos/501/enviar-cliente")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"tipo\":\"EMAIL\"}"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value(503));
	}

	@Test
	void enviarComClienteInexistenteRetorna422() throws Exception {
		when(enviarUseCase.executar(501L, TipoEnvio.WHATSAPP))
				.thenThrow(new ClienteNaoEncontradoException(1L));

		mockMvc.perform(post("/v1/orcamentos/501/enviar-cliente")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"tipo\":\"WHATSAPP\"}"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.status").value(422));
	}
}
