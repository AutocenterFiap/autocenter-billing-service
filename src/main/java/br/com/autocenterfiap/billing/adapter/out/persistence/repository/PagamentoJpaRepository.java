package br.com.autocenterfiap.billing.adapter.out.persistence.repository;

import br.com.autocenterfiap.billing.adapter.out.persistence.entity.PagamentoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagamentoJpaRepository extends JpaRepository<PagamentoJpaEntity, Long> {

	Optional<PagamentoJpaEntity> findFirstByOrdemServicoIdOrderByIdDesc(Long ordemServicoId);

	List<PagamentoJpaEntity> findByOrdemServicoIdOrderByIdDesc(Long ordemServicoId);
}

