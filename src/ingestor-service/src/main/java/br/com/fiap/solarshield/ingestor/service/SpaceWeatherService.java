package br.com.fiap.solarshield.ingestor.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import br.com.fiap.solarshield.ingestor.domain.AlertPayload;
import br.com.fiap.solarshield.ingestor.domain.CurrentWeatherResponse;
import br.com.fiap.solarshield.ingestor.domain.IngestResponse;
import br.com.fiap.solarshield.ingestor.domain.NeoFeedResponse;
import br.com.fiap.solarshield.ingestor.domain.NeoObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SpaceWeatherService {
    private static final String CURRENT_CACHE_KEY = "space:current:weather";
    private final NasaClient nasaClient;
    private final DonkiMapper donkiMapper;
    private final AlertPublisher alertPublisher;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final int lookbackDays;
    private final int cacheTtlSeconds;

    public SpaceWeatherService(
            NasaClient nasaClient,
            DonkiMapper donkiMapper,
            AlertPublisher alertPublisher,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${solar.nasa.lookback-days}") int lookbackDays,
            @Value("${solar.cache.ttl-seconds}") int cacheTtlSeconds
    ) {
        this.nasaClient = nasaClient;
        this.donkiMapper = donkiMapper;
        this.alertPublisher = alertPublisher;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.lookbackDays = lookbackDays;
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public CurrentWeatherResponse current() {
        try {
            String cached = redisTemplate.opsForValue().get(CURRENT_CACHE_KEY);
            if (cached != null) {
                return readCurrent(cached).withCache("HIT");
            }
            CurrentWeatherResponse current = fetchCurrent().withCache(null);
            redisTemplate.opsForValue().set(CURRENT_CACHE_KEY, write(current), Duration.ofSeconds(cacheTtlSeconds));
            return current.withCache("MISS");
        } catch (RedisConnectionFailureException exception) {
            return fetchCurrent().withCache("BYPASS");
        }
    }

    public IngestResponse ingest() {
        List<JsonNode> events = nasaClient.fetchGstEvents(LocalDate.now().minusDays(lookbackDays), LocalDate.now());
        List<String> ids = new ArrayList<>();
        for (JsonNode event : events) {
            AlertPayload preliminary = donkiMapper.toAlert(event, 0);
            int hazardousCount = "severe".equals(preliminary.classification())
                    ? nasaClient.countHazardousNeos(eventDate(preliminary.occurredAt()))
                    : 0;
            AlertPayload payload = donkiMapper.toAlert(event, hazardousCount);
            alertPublisher.publish(payload);
            ids.add(payload.eventId());
        }
        return new IngestResponse(ids.isEmpty() ? "no_events" : "queued", ids.size(), ids);
    }

    public NeoFeedResponse neoFeed(LocalDate date) {
        LocalDate start = date;
        LocalDate end = date.plusDays(1);
        JsonNode feed = nasaClient.fetchNeoFeed(start, end);
        int total = 0;
        int hazardous = 0;
        List<NeoObject> objects = new ArrayList<>();
        JsonNode grouped = feed.path("near_earth_objects");
        if (grouped.isObject()) {
            grouped.fields().forEachRemaining(entry -> {
                JsonNode array = entry.getValue();
                if (array.isArray()) {
                    for (JsonNode item : array) {
                        if (objects.size() < 10) {
                            objects.add(new NeoObject(
                                    entry.getKey(),
                                    item.path("id").asText(),
                                    item.path("name").asText(),
                                    item.path("is_potentially_hazardous_asteroid").asBoolean(false)
                            ));
                        }
                    }
                }
            });
            for (JsonNode array : grouped) {
                if (array.isArray()) {
                    for (JsonNode item : array) {
                        total++;
                        if (item.path("is_potentially_hazardous_asteroid").asBoolean(false)) {
                            hazardous++;
                        }
                    }
                }
            }
        }
        return new NeoFeedResponse(start.toString(), end.toString(), total, hazardous, objects);
    }

    public List<AlertPayload> alerts() {
        try {
            List<String> records = redisTemplate.opsForList().range("alerts:processed", 0, 49);
            if (records == null) {
                return List.of();
            }
            return records.stream().map(this::readAlert).toList();
        } catch (RedisConnectionFailureException exception) {
            return List.of();
        }
    }

    private CurrentWeatherResponse fetchCurrent() {
        List<JsonNode> events = nasaClient.fetchGstEvents(LocalDate.now().minusDays(lookbackDays), LocalDate.now());
        return events.stream()
                .map(donkiMapper::toCurrentWeather)
                .max((left, right) -> Double.compare(left.kpIndex(), right.kpIndex()))
                .orElse(new CurrentWeatherResponse(
                        "no-gst-in-window",
                        0,
                        "low",
                        false,
                        OffsetDateTime.now().toString(),
                        "NASA DONKI",
                        null
                ));
    }

    private LocalDate eventDate(String value) {
        return LocalDate.parse(value.substring(0, 10));
    }

    private String write(CurrentWeatherResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private CurrentWeatherResponse readCurrent(String value) {
        try {
            return objectMapper.readValue(value, CurrentWeatherResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private AlertPayload readAlert(String value) {
        try {
            return objectMapper.readValue(value, AlertPayload.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
