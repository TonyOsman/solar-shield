package br.com.fiap.solarshield.notifier.service;

import br.com.fiap.solarshield.notifier.domain.AlertPayload;

public interface AlertStore {
    void save(AlertPayload payload);
}
