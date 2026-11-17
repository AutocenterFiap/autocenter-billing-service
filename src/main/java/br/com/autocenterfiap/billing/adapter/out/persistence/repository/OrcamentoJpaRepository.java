package br.com.autocenterfiap.billing.adapter.out.persistence.repository;

import br.com.autocenterfiap.billing.adapter.out.persistence.entity.OrcamentoJpaEntity;
import br.com.autocenterfiap.billing.domain.enums.StatusOrcamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrcamentoJpaRepository extends JpaRepository<OrcamentoJpaEntity, Long> {

	@Query("SELECT o FROM OrcamentoJpaEntity o WHERE o.statusOrcamento = 'AGUARDANDO_APROVACAO' AND o.ordemServicoId = :osId")
	Optional<OrcamentoJpaEntity> buscarOrcamentoAguardandoAprovacaoPorOS(@Param("osId") Long osId);

	List<OrcamentoJpaEntity> findByOrdemServicoIdOrderByIdDesc(Long ordemServicoId);

	Page<OrcamentoJpaEntity> findByStatusOrcamento(StatusOrcamento status, Pageable pageable);
}

