package br.com.fiap.solarshield.ingestor.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.solarshield.ingestor.domain.Severity;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SeverityClassifierTest {
    private final SeverityClassifier classifier = new SeverityClassifier();

    @ParameterizedTest
    @CsvSource({
            "0, low, false",
            "4, low, false",
            "5, moderate, false",
            "7, moderate, false",
            "8, severe, true",
            "9, severe, true"
    })
    void classificaKpNasFronteiras(double kp, String expectedClassification, boolean expectedEmergency) {
        Severity severity = classifier.classify(kp);
        assertThat(severity.classification()).isEqualTo(expectedClassification);
        assertThat(severity.emergencyNotification()).isEqualTo(expectedEmergency);
    }
}
