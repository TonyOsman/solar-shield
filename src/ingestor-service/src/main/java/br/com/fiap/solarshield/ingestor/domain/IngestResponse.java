package br.com.fiap.solarshield.ingestor.domain;

import java.util.List;

public record IngestResponse(String status, int queuedCount, List<String> eventIds) {
}
