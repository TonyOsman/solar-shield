package br.com.fiap.solarshield.ingestor.domain;

public record CurrentWeatherResponse(
        String eventId,
        double kpIndex,
        String classification,
        boolean emergencyNotification,
        String capturedAt,
        String source,
        String cache
) {
    public CurrentWeatherResponse withCache(String value) {
        return new CurrentWeatherResponse(eventId, kpIndex, classification, emergencyNotification, capturedAt, source, value);
    }
}
