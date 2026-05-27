package br.com.fiap.solarshield.ingestor.service;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class NasaClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final boolean useMock;

    public NasaClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${solar.nasa.api-key}") String apiKey,
            @Value("${solar.nasa.use-mock}") boolean useMock
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.useMock = useMock;
    }

    public List<JsonNode> fetchGstEvents(LocalDate startDate, LocalDate endDate) {
        JsonNode data = get("/DONKI/notifications",
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "type", "GST"
        );
        List<JsonNode> events = new ArrayList<>();
        if (data.isArray()) {
            data.forEach(events::add);
        }
        return events;
    }

    public JsonNode fetchNeoFeed(LocalDate startDate, LocalDate endDate) {
        return get("/neo/rest/v1/feed",
                "start_date", startDate.toString(),
                "end_date", endDate.toString()
        );
    }

    public int countHazardousNeos(LocalDate day) {
        JsonNode feed = fetchNeoFeed(day.minusDays(1), day.plusDays(1));
        int count = 0;
        JsonNode dates = feed.path("near_earth_objects");
        if (dates.isObject()) {
            for (JsonNode objects : dates) {
                if (objects.isArray()) {
                    for (JsonNode item : objects) {
                        if (item.path("is_potentially_hazardous_asteroid").asBoolean(false)) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    private JsonNode get(String path, String... params) {
        if (useMock) {
            return mock(path);
        }
        return retry(() -> restTemplate.getForObject(uri(path, params), JsonNode.class));
    }

    private URI uri(String path, String... params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://api.nasa.gov" + path);
        for (int i = 0; i < params.length; i += 2) {
            builder.queryParam(params[i], params[i + 1]);
        }
        builder.queryParam("api_key", apiKey);
        return builder.build(true).toUri();
    }

    private JsonNode retry(Supplier<JsonNode> operation) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return operation.get();
            } catch (RuntimeException exception) {
                last = exception;
                if (!isTransient(exception) || attempt == 3) {
                    throw exception;
                }
                sleep(attempt);
            }
        }
        throw last;
    }

    private boolean isTransient(RuntimeException exception) {
        if (exception instanceof ResourceAccessException) {
            return true;
        }
        if (exception instanceof RestClientResponseException responseException) {
            HttpStatusCode status = HttpStatusCode.valueOf(responseException.getStatusCode().value());
            return status.value() == 429 || status.is5xxServerError();
        }
        return false;
    }

    private void sleep(int attempt) {
        try {
            Thread.sleep((long) (500 * Math.pow(2, attempt - 1)));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private JsonNode mock(String path) {
        String json = switch (path) {
            case "/DONKI/notifications" -> """
                    [
                      {
                        "gstID": "2026-06-09T18:42:00-GST-001",
                        "startTime": "2026-06-09T18:42Z",
                        "allKpIndex": [
                          { "observedTime": "2026-06-09T18:00Z", "kpIndex": 8.1 }
                        ]
                      }
                    ]
                    """;
            case "/neo/rest/v1/feed" -> """
                    {
                      "near_earth_objects": {
                        "2026-06-09": [
                          { "id": "neo-1", "name": "Demo NEO 1", "is_potentially_hazardous_asteroid": true },
                          { "id": "neo-2", "name": "Demo NEO 2", "is_potentially_hazardous_asteroid": false }
                        ]
                      }
                    }
                    """;
            default -> "{}";
        };
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
