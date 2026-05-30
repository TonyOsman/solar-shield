package br.com.fiap.solarshield.notifier.domain;

public record AlertPayload(
        String eventId,
        double kpIndex,
        String classification,
        boolean emergencyNotification,
        int neoHazardousCount,
        String capturedAt,
        String occurredAt,
        String source
) {
}
