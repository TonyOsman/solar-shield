package br.com.fiap.solarshield.notifier.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.AcknowledgeMode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitConfig {
    @Bean
    TopicExchange spaceEventsExchange() {
        return new TopicExchange("space.events", true, false);
    }

    @Bean
    Queue notifierAlertsQueue() {
        return new Queue("notifier.alerts", true);
    }

    @Bean
    Binding notifierAlertsBinding(Queue notifierAlertsQueue, TopicExchange spaceEventsExchange) {
        return BindingBuilder.bind(notifierAlertsQueue).to(spaceEventsExchange).with("space.weather.alert");
    }

    @Bean
    MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(10);
        factory.setMessageConverter(messageConverter);
        return factory;
    }
}
