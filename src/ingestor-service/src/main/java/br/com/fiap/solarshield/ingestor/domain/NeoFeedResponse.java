package br.com.fiap.solarshield.ingestor.domain;

import java.util.List;

public record NeoFeedResponse(
        String startDate,
        String endDate,
        int totalCount,
        int hazardousCount,
        List<NeoObject> objects
) {
}
