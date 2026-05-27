package br.com.fiap.solarshield.ingestor.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import br.com.fiap.solarshield.ingestor.domain.AlertPayload;
import br.com.fiap.solarshield.ingestor.domain.CurrentWeatherResponse;
import br.com.fiap.solarshield.ingestor.domain.IngestResponse;
import br.com.fiap.solarshield.ingestor.domain.NeoFeedResponse;
import br.com.fiap.solarshield.ingestor.service.SpaceWeatherService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpaceWeatherController {
    private final SpaceWeatherService service;

    public SpaceWeatherController(SpaceWeatherService service) {
        this.service = service;
    }

    @GetMapping("/")
    Map<String, Object> index() {
        return Map.of(
                "service", "solar-shield",
                "status", "ok",
                "endpoints", List.of(
                        "/health",
                        "/api/space-weather/current",
                        "/api/space-weather/ingest",
                        "/api/ingest/gst",
                        "/api/neo/feed?date=YYYY-MM-DD",
                        "/api/alerts"
                )
        );
    }

    @GetMapping("/health")
    Map<String, String> health() {
        return Map.of("status", "ok", "service", "ingestor-service");
    }

    @GetMapping("/api/space-weather/current")
    ResponseEntity<CurrentWeatherResponse> current() {
        CurrentWeatherResponse response = service.current();
        return ResponseEntity.ok()
                .header("X-Cache", response.cache())
                .body(response);
    }

    @PostMapping("/api/space-weather/ingest")
    ResponseEntity<IngestResponse> ingest() {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.ingest());
    }

    @PostMapping("/api/ingest/gst")
    ResponseEntity<IngestResponse> ingestAlias() {
        return ingest();
    }

    @GetMapping("/api/neo/feed")
    NeoFeedResponse neoFeed(@RequestParam(name = "date", required = false) String date) {
        LocalDate selectedDate = date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date);
        return service.neoFeed(selectedDate);
    }

    @GetMapping("/api/alerts")
    Map<String, List<AlertPayload>> alerts() {
        return Map.of("items", service.alerts());
    }
}
