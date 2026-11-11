package br.com.autocenterfiap.billing.adapter.out.persistence.mapper;

import br.com.autocenterfiap.billing.adapter.out.persistence.entity.PagamentoJpaEntity;
import br.com.autocenterfiap.billing.domain.entity.Pagamento;
import org.springframework.stereotype.Component;

@Component
public class PagamentoJpaMapper {

	public PagamentoJpaEntity paraJpa(Pagamento d) {
		if (d == null) {
			return null;
		}
		return new PagamentoJpaEntity(
				d.getId(),
				d.getOrdemServicoId(),
				d.getOrcamentoId(),
				d.getCorrelationId(),
				d.getValor(),
				d.getTentativa(),
				d.getStatusPagamento(),
				d.getUrlCheckout(),
				d.getExpiraEm(),
				d.getMpPreferenceId(),
				d.getMpPaymentId(),
				d.getMotivo(),
				d.getDataCriacao(),
				d.getDataUltimaAtualizacao());
	}

	public Pagamento paraDominio(PagamentoJpaEntity e) {
		if (e == null) {
			return null;
		}
		return new Pagamento(
				e.getId(),
				e.getOrdemServicoId(),
				e.getOrcamentoId(),
				e.getCorrelationId(),
				e.getValor(),
				e.getTentativa(),
				e.getStatusPagamento(),
				e.getUrlCheckout(),
				e.getExpiraEm(),
				e.getMpPreferenceId(),
				e.getMpPaymentId(),
				e.getMotivo(),
				e.getDataCriacao(),
				e.getDataUltimaAtualizacao());
	}
}

