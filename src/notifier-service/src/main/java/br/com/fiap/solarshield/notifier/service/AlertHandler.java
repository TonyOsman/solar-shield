package br.com.fiap.solarshield.notifier.service;

import br.com.fiap.solarshield.notifier.domain.AlertPayload;
import br.com.fiap.solarshield.notifier.domain.HandleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AlertHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertHandler.class);
    private final IdempotencyStore idempotencyStore;
    private final AlertStore alertStore;

    public AlertHandler(IdempotencyStore idempotencyStore, AlertStore alertStore) {
        this.idempotencyStore = idempotencyStore;
        this.alertStore = alertStore;
    }

    public HandleResult handle(AlertPayload payload) {
        if (!idempotencyStore.markFirst(payload.eventId())) {
            LOGGER.info("Duplicate ignored event_id={}", payload.eventId());
            return new HandleResult("duplicate", payload.eventId());
        }
        try {
            alertStore.save(payload);
            LOGGER.info("Alert processed event_id={} classification={} emergency={}", payload.eventId(), payload.classification(), payload.emergencyNotification());
            return new HandleResult("processed", payload.eventId());
        } catch (RuntimeException exception) {
            idempotencyStore.release(payload.eventId());
            throw exception;
        }
    }
}
