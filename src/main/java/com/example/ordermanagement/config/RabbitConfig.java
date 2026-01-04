package com.example.ordermanagement.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "ordermanagement.jobs";
    public static final String INVOICE_QUEUE = "ordermanagement.invoice_generation.queue";
    public static final String REFUND_QUEUE = "ordermanagement.refund_processing.queue";

    @Bean
    public TopicExchange jobsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue invoiceQueue() {
        return QueueBuilder.durable(INVOICE_QUEUE).build();
    }

    @Bean
    public Queue refundQueue() {
        return QueueBuilder.durable(REFUND_QUEUE).build();
    }

    @Bean
    public Binding invoiceBinding(Queue invoiceQueue, TopicExchange jobsExchange) {
        return BindingBuilder.bind(invoiceQueue).to(jobsExchange).with("invoice.generate");
    }

    @Bean
    public Binding refundBinding(Queue refundQueue, TopicExchange jobsExchange) {
        return BindingBuilder.bind(refundQueue).to(jobsExchange).with("refund.process");
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rt = new RabbitTemplate(connectionFactory);
        rt.setMessageConverter(messageConverter);
        return rt;
    }
}
