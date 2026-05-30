package br.com.fiap.solarshield.notifier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.com.fiap.solarshield.notifier.domain.AlertPayload;
import br.com.fiap.solarshield.notifier.domain.HandleResult;
import org.junit.jupiter.api.Test;

class AlertHandlerTest {
    @Test
    void naoProcessaDuasVezesOMesmoEventId() {
        FakeIdempotencyStore idempotencyStore = new FakeIdempotencyStore();
        FakeAlertStore alertStore = new FakeAlertStore();
        AlertHandler handler = new AlertHandler(idempotencyStore, alertStore);
        AlertPayload payload = payload("evt-001");

        HandleResult first = handler.handle(payload);
        HandleResult second = handler.handle(payload);

        assertThat(first.status()).isEqualTo("processed");
        assertThat(second.status()).isEqualTo("duplicate");
        assertThat(alertStore.saved).hasSize(1);
    }

    @Test
    void liberaIdempotenciaQuandoPersistenciaFalha() {
        FakeIdempotencyStore idempotencyStore = new FakeIdempotencyStore();
        AlertStore alertStore = payload -> {
            throw new IllegalStateException("fail");
        };
        AlertHandler handler = new AlertHandler(idempotencyStore, alertStore);

        assertThatThrownBy(() -> handler.handle(payload("evt-fail")))
                .isInstanceOf(IllegalStateException.class);

        assertThat(idempotencyStore.released).contains("evt-fail");
    }

    private AlertPayload payload(String eventId) {
        return new AlertPayload(eventId, 8.1, "severe", true, 1, "2026-06-09T18:42Z", "2026-06-09T18:42Z", "NASA DONKI");
    }

    private static class FakeIdempotencyStore implements IdempotencyStore {
        private final Set<String> processed = new HashSet<>();
        private final List<String> released = new ArrayList<>();

        @Override
        public boolean markFirst(String eventId) {
            return processed.add(eventId);
        }

        @Override
        public void release(String eventId) {
            released.add(eventId);
            processed.remove(eventId);
        }
    }

    private static class FakeAlertStore implements AlertStore {
        private final List<AlertPayload> saved = new ArrayList<>();

        @Override
        public void save(AlertPayload payload) {
            saved.add(payload);
        }
    }
}
