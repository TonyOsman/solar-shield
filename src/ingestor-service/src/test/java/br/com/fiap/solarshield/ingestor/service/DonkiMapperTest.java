package br.com.fiap.solarshield.ingestor.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.solarshield.ingestor.domain.AlertPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class DonkiMapperTest {
    private final DonkiMapper mapper = new DonkiMapper(new SeverityClassifier());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extraiMaiorKpDoPayloadDonki() throws Exception {
        JsonNode event = objectMapper.readTree("""
                {
                  "gstID": "2026-06-09T18:42:00-GST-001",
                  "startTime": "2026-06-09T18:42Z",
                  "allKpIndex": [
                    { "observedTime": "2026-06-09T15:00Z", "kpIndex": 5 },
                    { "observedTime": "2026-06-09T18:00Z", "kpIndex": 8.1 }
                  ]
                }
                """);

        AlertPayload payload = mapper.toAlert(event, 2);

        assertThat(payload.eventId()).isEqualTo("2026-06-09T18:42:00-GST-001");
        assertThat(payload.kpIndex()).isEqualTo(8.1);
        assertThat(payload.classification()).isEqualTo("severe");
        assertThat(payload.emergencyNotification()).isTrue();
        assertThat(payload.neoHazardousCount()).isEqualTo(2);
    }
}
