package br.com.fiap.solarshield.ingestor.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.fiap.solarshield.ingestor.domain.AlertPayload;
import br.com.fiap.solarshield.ingestor.domain.CurrentWeatherResponse;
import br.com.fiap.solarshield.ingestor.domain.Severity;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class DonkiMapper {
    private static final Pattern KP_PATTERN = Pattern.compile("\\bKp(?:\\s*index)?\\D{0,16}(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);
    private final SeverityClassifier classifier;

    public DonkiMapper(SeverityClassifier classifier) {
        this.classifier = classifier;
    }

    public AlertPayload toAlert(JsonNode event, int hazardousNeoCount) {
        double kp = extractMaxKp(event);
        Severity severity = classifier.classify(kp);
        return new AlertPayload(
                eventId(event),
                kp,
                severity.classification(),
                severity.emergencyNotification(),
                hazardousNeoCount,
                OffsetDateTime.now().toString(),
                eventTime(event),
                "NASA DONKI"
        );
    }

    public CurrentWeatherResponse toCurrentWeather(JsonNode event) {
        AlertPayload alert = toAlert(event, 0);
        return new CurrentWeatherResponse(
                alert.eventId(),
                alert.kpIndex(),
                alert.classification(),
                alert.emergencyNotification(),
                alert.capturedAt(),
                alert.source(),
                null
        );
    }

    public double extractMaxKp(JsonNode event) {
        List<Double> values = new ArrayList<>();
        addNumeric(values, event.path("kpIndex"));
        addNumeric(values, event.path("kp"));
        addNumeric(values, event.path("maxKpIndex"));
        JsonNode allKp = event.path("allKpIndex");
        if (allKp.isArray()) {
            allKp.forEach(item -> addNumeric(values, item.path("kpIndex")));
        }
        for (String field : List.of("messageBody", "message", "body")) {
            JsonNode text = event.path(field);
            if (text.isTextual()) {
                Matcher matcher = KP_PATTERN.matcher(text.asText());
                while (matcher.find()) {
                    values.add(Double.parseDouble(matcher.group(1)));
                }
            }
        }
        return values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
    }

    private void addNumeric(List<Double> values, JsonNode node) {
        if (node.isNumber()) {
            values.add(node.asDouble());
        } else if (node.isTextual()) {
            try {
                values.add(Double.parseDouble(node.asText()));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private String eventId(JsonNode event) {
        for (String field : List.of("gstID", "eventID", "notificationID", "messageID", "activityID")) {
            JsonNode value = event.path(field);
            if (!value.isMissingNode() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return "gst-" + eventTime(event);
    }

    private String eventTime(JsonNode event) {
        for (String field : List.of("startTime", "eventTime", "messageIssueTime")) {
            JsonNode value = event.path(field);
            if (!value.isMissingNode() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return OffsetDateTime.now().toString();
    }
}
