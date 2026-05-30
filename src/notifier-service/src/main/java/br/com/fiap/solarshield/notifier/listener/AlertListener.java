package br.com.fiap.solarshield.notifier.listener;

import java.io.IOException;

import br.com.fiap.solarshield.notifier.domain.AlertPayload;
import br.com.fiap.solarshield.notifier.service.AlertHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AlertListener {
    private final ObjectMapper objectMapper;
    private final AlertHandler alertHandler;

    public AlertListener(ObjectMapper objectMapper, AlertHandler alertHandler) {
        this.objectMapper = objectMapper;
        this.alertHandler = alertHandler;
    }

    @RabbitListener(queues = "notifier.alerts", containerFactory = "rabbitListenerContainerFactory")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            AlertPayload payload = objectMapper.readValue(message.getBody(), AlertPayload.class);
            alertHandler.handle(payload);
            channel.basicAck(deliveryTag, false);
        } catch (Exception exception) {
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
