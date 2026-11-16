package br.com.autocenterfiap.billing.adapter.in.controller;

import br.com.autocenterfiap.billing.adapter.in.dto.EnvioRequest;
import br.com.autocenterfiap.billing.adapter.in.dto.OrcamentoResponse;
import br.com.autocenterfiap.billing.adapter.in.dto.ReprovarOrcamentoRequest;
import br.com.autocenterfiap.billing.adapter.mapper.OrcamentoAdapterMapper;
import br.com.autocenterfiap.billing.application.dto.OrcamentoOutput;
import br.com.autocenterfiap.billing.application.dto.PageResult;
import br.com.autocenterfiap.billing.application.dto.PaginationRequest;
import br.com.autocenterfiap.billing.application.usecase.AprovarOrcamentoUseCase;
import br.com.autocenterfiap.billing.application.usecase.BuscarOrcamentoPorIdUseCase;
import br.com.autocenterfiap.billing.application.usecase.BuscarTodosOrcamentosUseCase;
import br.com.autocenterfiap.billing.application.usecase.ReprovarOrcamentoUseCase;
import br.com.autocenterfiap.billing.domain.enums.StatusOrcamento;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "Orcamentos", description = "API para gerenciamento de Orcamentos da oficina")
public class OrcamentoController {

	private final BuscarOrcamentoPorIdUseCase buscarOrcamentoPorIdUseCase;
	private final BuscarTodosOrcamentosUseCase buscarTodosOrcamentosUseCase;
	private final AprovarOrcamentoUseCase aprovarOrcamentoUseCase;
	private final ReprovarOrcamentoUseCase reprovarOrcamentoUseCase;

	public OrcamentoController(
			BuscarOrcamentoPorIdUseCase buscarOrcamentoPorIdUseCase,
			BuscarTodosOrcamentosUseCase buscarTodosOrcamentosUseCase,
			AprovarOrcamentoUseCase aprovarOrcamentoUseCase,
			ReprovarOrcamentoUseCase reprovarOrcamentoUseCase
	) {
		this.buscarOrcamentoPorIdUseCase = buscarOrcamentoPorIdUseCase;
		this.buscarTodosOrcamentosUseCase = buscarTodosOrcamentosUseCase;
		this.aprovarOrcamentoUseCase = aprovarOrcamentoUseCase;
		this.reprovarOrcamentoUseCase = reprovarOrcamentoUseCase;
	}

	@Operation(
			summary = "Aprovar orcamento",
			description = "Atualiza o status do orcamento para aprovado e publica orcamento.aprovado"
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Orcamento aprovado com sucesso"),
			@ApiResponse(responseCode = "404", description = "Orcamento não encontrado")
	})
	@PatchMapping("/{id}/aprovar")
	public ResponseEntity<OrcamentoResponse> aprovar(
			@Parameter(description = "ID do orcamento a ser aprovado", required = true)
			@PathVariable Long id
	) {
		OrcamentoOutput orcamento = this.aprovarOrcamentoUseCase.executar(id);
		return ResponseEntity.ok(OrcamentoAdapterMapper.orcamentoOutputToOrcamentoResponse(orcamento));
	}

	@Operation(
			summary = "Reprovar orcamento",
			description = "Atualiza o status do orcamento para reprovado e publica orcamento.reprovado"
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Orcamento reprovado com sucesso"),
			@ApiResponse(responseCode = "404", description = "Orcamento não encontrado")
	})
	@PatchMapping("/{id}/reprovar")
	public ResponseEntity<OrcamentoResponse> reprovar(
			@Parameter(description = "ID do orcamento a ser reprovado", required = true)
			@PathVariable Long id,
			@RequestBody(required = false) ReprovarOrcamentoRequest request
	) {
		String motivo = request != null ? request.motivo() : null;
		OrcamentoOutput orcamento = this.reprovarOrcamentoUseCase.executar(id, motivo);
		return ResponseEntity.ok(OrcamentoAdapterMapper.orcamentoOutputToOrcamentoResponse(orcamento));
	}

	@Operation(
			summary = "Enviar orcamento",
			description = "Envia o orcamento para o cliente"
	)
	@PostMapping("/{id}/enviar-cliente")
	public ResponseEntity<String> enviarOrcamento(
			@PathVariable Long id,
			@Valid @RequestBody EnvioRequest envioRequest) {

		String mensagem = String.format("Orçamento %d enviado via %s com sucesso!",
				id, envioRequest.tipo().name());

		return ResponseEntity.ok(mensagem);
	}

	@Operation(
			summary = "Listar todos os orcamentos",
			description = "Retorna uma lista paginada com todos os orcamentos por status."
	)
	@GetMapping
	public ResponseEntity<PageResult<OrcamentoResponse>> listarTodos(
			@RequestParam(required = true) StatusOrcamento status, Pageable pageable) {

		PaginationRequest pagination = new PaginationRequest(
				pageable.getPageNumber(), pageable.getPageSize());

		return ResponseEntity.ok(
				this.buscarTodosOrcamentosUseCase.executar(status, pagination)
						.map(OrcamentoAdapterMapper::orcamentoOutputToOrcamentoResponse)
		);
	}

	@Operation(
			summary = "Buscar orcamento por ID",
			description = "Retorna um orcamento específico pelo seu identificador único"
	)
	@GetMapping("/{id}")
	public ResponseEntity<OrcamentoResponse> buscarPorId(
			@Parameter(description = "ID do orcamento a ser buscado", required = true)
			@PathVariable Long id) {
		OrcamentoOutput orcamento = this.buscarOrcamentoPorIdUseCase.executar(id);
		return ResponseEntity.ok(OrcamentoAdapterMapper.orcamentoOutputToOrcamentoResponse(orcamento));
	}

	@Operation(
			summary = "Buscar orcamentos por ID da Ordem de Servico",
			description = "Retorna os orcamentos associados a uma Ordem de Servico"
	)
	@GetMapping("/os/{osId}")
	public ResponseEntity<List<OrcamentoResponse>> buscarPorOsId(@PathVariable Long osId) {
		List<OrcamentoResponse> lista = this.buscarOrcamentoPorIdUseCase.buscarPorOsId(osId).stream()
				.map(OrcamentoAdapterMapper::orcamentoOutputToOrcamentoResponse)
				.toList();
		return ResponseEntity.ok(lista);
	}
}

