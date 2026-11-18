package br.com.autocenterfiap.billing.adapter.in.controller;

import br.com.autocenterfiap.billing.adapter.in.dto.EnvioRequest;
import br.com.autocenterfiap.billing.adapter.in.dto.OrcamentoResponse;
import br.com.autocenterfiap.billing.adapter.in.dto.ReprovarOrcamentoRequest;
import br.com.autocenterfiap.billing.adapter.in.handler.ErroResponse;
import br.com.autocenterfiap.billing.adapter.mapper.OrcamentoAdapterMapper;
import br.com.autocenterfiap.billing.application.dto.OrcamentoOutput;
import br.com.autocenterfiap.billing.application.dto.PageResult;
import br.com.autocenterfiap.billing.application.dto.PaginationRequest;
import br.com.autocenterfiap.billing.application.usecase.AprovarOrcamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.BuscarOrcamentoPorIdUseCase;
import br.com.autocenterfiap.billing.application.usecase.BuscarTodosOrcamentosUseCase;
import br.com.autocenterfiap.billing.application.usecase.EnviarOrcamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.ReprovarOrcamentoUseCase;
import br.com.autocenterfiap.billing.domain.enums.StatusOrcamento;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/orcamentos")
@Tag(name = "Orçamentos",
		description = "Consulta, envio ao cliente, aprovação e reprovação dos orçamentos gerados pela Saga")
public class OrcamentoController {

	private final BuscarOrcamentoPorIdUseCase buscarOrcamentoPorIdUseCase;
	private final BuscarTodosOrcamentosUseCase buscarTodosOrcamentosUseCase;
	private final AprovarOrcamentoUseCase aprovarOrcamentoUseCase;
	private final ReprovarOrcamentoUseCase reprovarOrcamentoUseCase;
	private final EnviarOrcamentoUseCase enviarOrcamentoUseCase;

	public OrcamentoController(
			BuscarOrcamentoPorIdUseCase buscarOrcamentoPorIdUseCase,
			BuscarTodosOrcamentosUseCase buscarTodosOrcamentosUseCase,
			AprovarOrcamentoUseCase aprovarOrcamentoUseCase,
			ReprovarOrcamentoUseCase reprovarOrcamentoUseCase,
			EnviarOrcamentoUseCase enviarOrcamentoUseCase
	) {
		this.buscarOrcamentoPorIdUseCase = buscarOrcamentoPorIdUseCase;
		this.buscarTodosOrcamentosUseCase = buscarTodosOrcamentosUseCase;
		this.aprovarOrcamentoUseCase = aprovarOrcamentoUseCase;
		this.reprovarOrcamentoUseCase = reprovarOrcamentoUseCase;
		this.enviarOrcamentoUseCase = enviarOrcamentoUseCase;
	}

	@Operation(
			summary = "Aprovar orçamento",
			description = "Registra a aprovação do cliente e publica orcamento.aprovado; a Saga gera a cobrança"
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Orçamento aprovado"),
			@ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
					content = @Content),
			@ApiResponse(responseCode = "403", description = "Token sem o perfil interno (funcionário)",
					content = @Content),
			@ApiResponse(responseCode = "404", description = "Orçamento não encontrado",
					content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	})
	@PatchMapping("/{id}/aprovar")
	public ResponseEntity<OrcamentoResponse> aprovar(
			@Parameter(description = "Identificador do orçamento", example = "7", required = true)
			@PathVariable Long id
	) {
		OrcamentoOutput orcamento = this.aprovarOrcamentoUseCase.executar(id);
		return ResponseEntity.ok(OrcamentoAdapterMapper.orcamentoOutputToOrcamentoResponse(orcamento));
	}

	@Operation(
			summary = "Reprovar orçamento",
			description = "Registra a reprovação do cliente e publica orcamento.reprovado; a OS é encerrada "
					+ "como ORCAMENTO_REPROVADO. O corpo é opcional"
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Orçamento reprovado"),
			@ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
					content = @Content),
			@ApiResponse(responseCode = "403", description = "Token sem o perfil interno (funcionário)",
					content = @Content),
			@ApiResponse(responseCode = "404", description = "Orçamento não encontrado",
					content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	})
	@PatchMapping("/{id}/reprovar")
	public ResponseEntity<OrcamentoResponse> reprovar(
			@Parameter(description = "Identificador do orçamento", example = "7", required = true)
			@PathVariable Long id,
			@RequestBody(required = false) ReprovarOrcamentoRequest request
	) {
		String motivo = request != null ? request.motivo() : null;
		OrcamentoOutput orcamento = this.reprovarOrcamentoUseCase.executar(id, motivo);
		return ResponseEntity.ok(OrcamentoAdapterMapper.orcamentoOutputToOrcamentoResponse(orcamento));
	}

	@Operation(
			summary = "Enviar orçamento ao cliente",
			description = "Simula o envio do orçamento por e-mail ou WhatsApp, com o contato buscado no Cadastro. "
					+ "IMPRESSAO não consulta o Cadastro"
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Mensagem de confirmação do envio",
					content = @Content(mediaType = "text/plain",
							schema = @Schema(type = "string",
									example = "Orçamento 7 enviado via EMAIL para cliente@email.com"))),
			@ApiResponse(responseCode = "400", description = "Tipo de envio ausente ou inválido",
					content = @Content(schema = @Schema(implementation = ErroResponse.class))),
			@ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
					content = @Content),
			@ApiResponse(responseCode = "403", description = "Token sem o perfil interno (funcionário)",
					content = @Content),
			@ApiResponse(responseCode = "404", description = "Orçamento não encontrado",
					content = @Content(schema = @Schema(implementation = ErroResponse.class))),
			@ApiResponse(responseCode = "422", description = "Cliente do orçamento não encontrado no Cadastro",
					content = @Content(schema = @Schema(implementation = ErroResponse.class))),
			@ApiResponse(responseCode = "503", description = "Serviço de Cadastro indisponível",
					content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	})
	@PostMapping("/{id}/enviar-cliente")
	public ResponseEntity<String> enviarOrcamento(
			@Parameter(description = "Identificador do orçamento", example = "7") @PathVariable Long id,
			@Valid @RequestBody EnvioRequest envioRequest) {

		return ResponseEntity.ok(enviarOrcamentoUseCase.executar(id, envioRequest.tipo()));
	}

	@Operation(
			summary = "Listar orçamentos por status",
			description = "Retorna os orçamentos do status informado de forma paginada"
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Página retornada"),
			@ApiResponse(responseCode = "400", description = "Status ausente ou inválido", content = @Content),
			@ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
					content = @Content),
			@ApiResponse(responseCode = "403", description = "Token sem o perfil interno (funcionário)",
					content = @Content)
	})
	@GetMapping
	public ResponseEntity<PageResult<OrcamentoResponse>> listarTodos(
			@Parameter(description = "Status dos orçamentos", example = "AGUARDANDO_APROVACAO", required = true)
			@RequestParam(required = true) StatusOrcamento status,
			@ParameterObject Pageable pageable) {

		PaginationRequest pagination = new PaginationRequest(
				pageable.getPageNumber(), pageable.getPageSize());

		return ResponseEntity.ok(
				this.buscarTodosOrcamentosUseCase.executar(status, pagination)
						.map(OrcamentoAdapterMapper::orcamentoOutputToOrcamentoResponse)
		);
	}

	@Operation(
			summary = "Buscar orçamento por ID",
			description = "Retorna um orçamento pelo seu identificador"
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Orçamento encontrado"),
			@ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
					content = @Content),
			@ApiResponse(responseCode = "403", description = "Token sem o perfil interno (funcionário)",
					content = @Content),
			@ApiResponse(responseCode = "404", description = "Orçamento não encontrado",
					content = @Content(schema = @Schema(implementation = ErroResponse.class)))
	})
	@GetMapping("/{id}")
	public ResponseEntity<OrcamentoResponse> buscarPorId(
			@Parameter(description = "Identificador do orçamento", example = "7", required = true)
			@PathVariable Long id) {
		OrcamentoOutput orcamento = this.buscarOrcamentoPorIdUseCase.executar(id);
		return ResponseEntity.ok(OrcamentoAdapterMapper.orcamentoOutputToOrcamentoResponse(orcamento));
	}

	@Operation(
			summary = "Listar orçamentos de uma OS",
			description = "Retorna os orçamentos associados à ordem de serviço; lista vazia se não houver"
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Lista retornada"),
			@ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
					content = @Content),
			@ApiResponse(responseCode = "403", description = "Token sem o perfil interno (funcionário)",
					content = @Content)
	})
	@GetMapping("/os/{osId}")
	public ResponseEntity<List<OrcamentoResponse>> buscarPorOsId(
			@Parameter(description = "Identificador da OS", example = "42") @PathVariable Long osId) {
		List<OrcamentoResponse> lista = this.buscarOrcamentoPorIdUseCase.buscarPorOsId(osId).stream()
				.map(OrcamentoAdapterMapper::orcamentoOutputToOrcamentoResponse)
				.toList();
		return ResponseEntity.ok(lista);
	}
}

