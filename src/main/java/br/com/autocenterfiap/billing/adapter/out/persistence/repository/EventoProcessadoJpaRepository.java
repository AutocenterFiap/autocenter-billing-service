package br.com.autocenterfiap.billing.adapter.out.persistence.repository;

import br.com.autocenterfiap.billing.adapter.out.persistence.entity.EventoProcessadoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoProcessadoJpaRepository extends JpaRepository<EventoProcessadoJpaEntity, String> {
}

