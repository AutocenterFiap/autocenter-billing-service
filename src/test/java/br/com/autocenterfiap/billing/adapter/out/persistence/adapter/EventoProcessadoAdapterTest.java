package br.com.autocenterfiap.billing.adapter.out.persistence.adapter;

import br.com.autocenterfiap.billing.adapter.out.persistence.entity.EventoProcessadoJpaEntity;
import br.com.autocenterfiap.billing.adapter.out.persistence.repository.EventoProcessadoJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoProcessadoAdapterTest {

	@Mock
	private EventoProcessadoJpaRepository repository;

	@InjectMocks
	private EventoProcessadoAdapter adapter;

	@Test
	void deveVerificarEMarcarEventoProcessado() {
		when(repository.existsById("evt-1")).thenReturn(true);

		assertThat(adapter.jaProcessado("evt-1")).isTrue();

		adapter.marcarProcessado("evt-2");

		ArgumentCaptor<EventoProcessadoJpaEntity> captor = ArgumentCaptor.forClass(EventoProcessadoJpaEntity.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getEventId()).isEqualTo("evt-2");
		assertThat(captor.getValue().getProcessadoEm()).isNotNull();
	}
}

