package com.example.domain.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String employeePostRequestQueueName = "employeePostRequestQueue";
    public static final String employeePutRequestQueueName = "employeePutRequestQueue";
    public static final String employeeDeleteRequestQueueName = "employeeDeleteRequestQueue";

    public static final String employeePostRequestQueueRoutingKey = "employee.post";
    public static final String employeePutRequestQueueRoutingKey = "employee.put";
    public static final String employeeDeleteRequestQueueRoutingKey = "employee.delete";

    public static final String directExchangeName = "employeeExchange";

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public DirectExchange exchange() {
        return ExchangeBuilder.directExchange(directExchangeName)
                .durable(true)
                .build();
    }

    @Bean
    public Queue employeePostRequestQueue() {
        return QueueBuilder.durable(employeePostRequestQueueName).build();
    }

    @Bean
    public Queue employeePutRequestQueue() {
        return QueueBuilder.durable(employeePutRequestQueueName).build();
    }

    @Bean
    public Queue employeeDeleteRequestQueue() {
        return QueueBuilder.durable(employeeDeleteRequestQueueName).build();
    }

    @Bean
    public Binding postRequestBinding(Queue employeePostRequestQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(employeePostRequestQueue).to(directExchange).with(employeePostRequestQueueRoutingKey);
    }

    @Bean
    public Binding putRequestBinding(Queue employeePutRequestQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(employeePutRequestQueue).to(directExchange).with(employeePutRequestQueueRoutingKey);
    }

    @Bean
    public Binding deleteRequestBinding(Queue employeeDeleteRequestQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(employeeDeleteRequestQueue).to(directExchange).with(employeeDeleteRequestQueueRoutingKey);
    }
}