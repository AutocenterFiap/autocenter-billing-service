package br.com.autocenterfiap.billing.domain.entity;

import br.com.autocenterfiap.billing.domain.enums.StatusPagamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Entidade de dominio de Pagamento (cobranca gerada para um Orcamento aprovado).
 */
public class Pagamento {

	private Long id;
	private Long ordemServicoId;
	private Long orcamentoId;
	private String correlationId;
	private BigDecimal valor;
	private Integer tentativa;
	private StatusPagamento statusPagamento;
	private String urlCheckout;
	private Instant expiraEm;
	private String mpPreferenceId;
	private String mpPaymentId;
	private String motivo;
	private LocalDateTime dataCriacao;
	private LocalDateTime dataUltimaAtualizacao;

	public Pagamento() {
	}

	public Pagamento(
			Long id,
			Long ordemServicoId,
			Long orcamentoId,
			String correlationId,
			BigDecimal valor,
			Integer tentativa,
			StatusPagamento statusPagamento,
			String urlCheckout,
			Instant expiraEm,
			String mpPreferenceId,
			String mpPaymentId,
			String motivo,
			LocalDateTime dataCriacao,
			LocalDateTime dataUltimaAtualizacao) {
		this.id = id;
		this.ordemServicoId = ordemServicoId;
		this.orcamentoId = orcamentoId;
		this.correlationId = correlationId;
		this.valor = valor;
		this.tentativa = tentativa;
		this.statusPagamento = statusPagamento;
		this.urlCheckout = urlCheckout;
		this.expiraEm = expiraEm;
		this.mpPreferenceId = mpPreferenceId;
		this.mpPaymentId = mpPaymentId;
		this.motivo = motivo;
		this.dataCriacao = dataCriacao;
		this.dataUltimaAtualizacao = dataUltimaAtualizacao;
	}

	public static Pagamento novoPendente(
			Long ordemServicoId,
			Long orcamentoId,
			String correlationId,
			BigDecimal valor,
			Integer tentativa,
			Instant expiraEm) {
		if (ordemServicoId == null) {
			throw new IllegalArgumentException("Ordem de servico e obrigatoria");
		}
		if (orcamentoId == null) {
			throw new IllegalArgumentException("Orcamento e obrigatorio");
		}
		if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Valor do pagamento deve ser maior que zero");
		}
		int numTentativa = (tentativa == null || tentativa <= 0) ? 1 : tentativa;
		LocalDateTime agora = LocalDateTime.now();
		return new Pagamento(
				null,
				ordemServicoId,
				orcamentoId,
				correlationId,
				valor,
				numTentativa,
				StatusPagamento.PENDENTE,
				null,
				expiraEm,
				null,
				null,
				null,
				agora,
				agora);
	}

	public void registrarLinkCheckout(String preferenceId, String urlCheckout) {
		this.mpPreferenceId = preferenceId;
		this.urlCheckout = urlCheckout;
		this.dataUltimaAtualizacao = LocalDateTime.now();
	}

	public void confirmar(String mpPaymentId) {
		if (mpPaymentId != null && !mpPaymentId.isBlank()) {
			this.mpPaymentId = mpPaymentId;
		}
		this.statusPagamento = StatusPagamento.CONFIRMADO;
		this.motivo = null;
		this.dataUltimaAtualizacao = LocalDateTime.now();
	}

	public void recusar(String mpPaymentId, String motivo) {
		if (mpPaymentId != null && !mpPaymentId.isBlank()) {
			this.mpPaymentId = mpPaymentId;
		}
		this.statusPagamento = StatusPagamento.RECUSADO;
		this.motivo = (motivo == null || motivo.isBlank()) ? "Pagamento recusado pela operadora" : motivo;
		this.dataUltimaAtualizacao = LocalDateTime.now();
	}

	public void estornar(String motivo) {
		this.statusPagamento = StatusPagamento.ESTORNADO;
		this.motivo = motivo;
		this.dataUltimaAtualizacao = LocalDateTime.now();
	}

	public void falharEstorno(String motivo) {
		this.statusPagamento = StatusPagamento.ESTORNO_FALHOU;
		this.motivo = motivo;
		this.dataUltimaAtualizacao = LocalDateTime.now();
	}

	public void cancelar(String motivo) {
		this.statusPagamento = StatusPagamento.CANCELADO;
		this.motivo = motivo;
		this.dataUltimaAtualizacao = LocalDateTime.now();
	}

	public boolean estaConfirmado() {
		return StatusPagamento.CONFIRMADO == this.statusPagamento;
	}

	public boolean estaRecusado() {
		return StatusPagamento.RECUSADO == this.statusPagamento;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getOrdemServicoId() {
		return ordemServicoId;
	}

	public void setOrdemServicoId(Long ordemServicoId) {
		this.ordemServicoId = ordemServicoId;
	}

	public Long getOrcamentoId() {
		return orcamentoId;
	}

	public void setOrcamentoId(Long orcamentoId) {
		this.orcamentoId = orcamentoId;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}

	public BigDecimal getValor() {
		return valor;
	}

	public void setValor(BigDecimal valor) {
		this.valor = valor;
	}

	public Integer getTentativa() {
		return tentativa;
	}

	public void setTentativa(Integer tentativa) {
		this.tentativa = tentativa;
	}

	public StatusPagamento getStatusPagamento() {
		return statusPagamento;
	}

	public void setStatusPagamento(StatusPagamento statusPagamento) {
		this.statusPagamento = statusPagamento;
	}

	public String getUrlCheckout() {
		return urlCheckout;
	}

	public void setUrlCheckout(String urlCheckout) {
		this.urlCheckout = urlCheckout;
	}

	public Instant getExpiraEm() {
		return expiraEm;
	}

	public void setExpiraEm(Instant expiraEm) {
		this.expiraEm = expiraEm;
	}

	public String getMpPreferenceId() {
		return mpPreferenceId;
	}

	public void setMpPreferenceId(String mpPreferenceId) {
		this.mpPreferenceId = mpPreferenceId;
	}

	public String getMpPaymentId() {
		return mpPaymentId;
	}

	public void setMpPaymentId(String mpPaymentId) {
		this.mpPaymentId = mpPaymentId;
	}

	public String getMotivo() {
		return motivo;
	}

	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}

	public LocalDateTime getDataCriacao() {
		return dataCriacao;
	}

	public void setDataCriacao(LocalDateTime dataCriacao) {
		this.dataCriacao = dataCriacao;
	}

	public LocalDateTime getDataUltimaAtualizacao() {
		return dataUltimaAtualizacao;
	}

	public void setDataUltimaAtualizacao(LocalDateTime dataUltimaAtualizacao) {
		this.dataUltimaAtualizacao = dataUltimaAtualizacao;
	}
}

