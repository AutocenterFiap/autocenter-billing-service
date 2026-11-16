package br.com.autocenterfiap.billing.adapter.out.persistence.adapter;

import br.com.autocenterfiap.billing.adapter.out.persistence.entity.EventoProcessadoJpaEntity;
import br.com.autocenterfiap.billing.adapter.out.persistence.repository.EventoProcessadoJpaRepository;
import br.com.autocenterfiap.billing.application.port.out.EventoProcessadoPort;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Deduplicacao por {@code eventId} na tabela {@code eventos_processados}.
 */
@Component
public class EventoProcessadoAdapter implements EventoProcessadoPort {

	private final EventoProcessadoJpaRepository repository;

	public EventoProcessadoAdapter(EventoProcessadoJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public boolean jaProcessado(String eventId) {
		return repository.existsById(eventId);
	}

	@Override
	public void marcarProcessado(String eventId) {
		repository.save(new EventoProcessadoJpaEntity(eventId, Instant.now()));
	}
}

