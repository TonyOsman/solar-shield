package br.com.fiap.solarshield.notifier.service;

public interface IdempotencyStore {
    boolean markFirst(String eventId);

    void release(String eventId);
}
