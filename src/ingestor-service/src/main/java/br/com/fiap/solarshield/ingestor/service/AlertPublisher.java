package br.com.fiap.solarshield.ingestor.service;

import br.com.fiap.solarshield.ingestor.domain.AlertPayload;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AlertPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final boolean enabled;

    public AlertPublisher(RabbitTemplate rabbitTemplate, @Value("${solar.rabbit.enabled}") boolean enabled) {
        this.rabbitTemplate = rabbitTemplate;
        this.enabled = enabled;
    }

    public void publish(AlertPayload payload) {
        if (!enabled) {
            return;
        }
        MessagePostProcessor processor = message -> {
            message.getMessageProperties().setMessageId(payload.eventId());
            message.getMessageProperties().setContentType("application/json");
            message.getMessageProperties().setHeader("x-event-id", payload.eventId());
            return message;
        };
        rabbitTemplate.convertAndSend("space.events", "space.weather.alert", payload, processor);
    }
}
