package br.com.fiap.solarshield.ingestor.service;

import br.com.fiap.solarshield.ingestor.domain.Severity;
import org.springframework.stereotype.Component;

@Component
public class SeverityClassifier {
    public Severity classify(double kp) {
        if (kp <= 4) {
            return new Severity("low", false);
        }
        if (kp <= 7) {
            return new Severity("moderate", false);
        }
        return new Severity("severe", true);
    }
}
