package com.example.gateway.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String employeePostRequestQueueName = "employeePostRequestQueue";
    public static final String employeePutRequestQueueName = "employeePutRequestQueue";
    public static final String employeeDeleteRequestQueueName = "employeeDeleteRequestQueue";

    public static final String employeePostRequestRoutingKey = "employee.post";
    public static final String employeePutRequestRoutingKey = "employee.put";
    public static final String employeeDeleteRequestRoutingKey = "employee.delete";

    public static final String directExchangeName = "employeeExchange";

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(directExchangeName);
    }

    @Bean("employeePostRequestQueue")
    public Queue employeePostRequestQueue() {
        return new Queue(employeePostRequestQueueName, true);
    }

    @Bean("employeePutRequestQueue")
    public Queue employeePutRequestQueue() {
        return new Queue(employeePutRequestQueueName, true);
    }

    @Bean("employeeDeleteRequestQueue")
    public Queue employeeDeleteRequestQueue() {
        return new Queue(employeeDeleteRequestQueueName, true);
    }

    @Bean
    public Binding postRequestBinding(@Qualifier("employeePostRequestQueue") Queue employeePostRequestQueue, DirectExchange exchange) {
        return BindingBuilder.bind(employeePostRequestQueue).to(exchange).with(employeePostRequestRoutingKey);
    }

    @Bean
    public Binding putRequestBinding(@Qualifier("employeePutRequestQueue") Queue employeePutRequestQueue, DirectExchange exchange) {
        return BindingBuilder.bind(employeePutRequestQueue).to(exchange).with(employeePutRequestRoutingKey);
    }

    @Bean
    public Binding deleteRequestBinding(@Qualifier("employeeDeleteRequestQueue") Queue employeeDeleteRequestQueue, DirectExchange exchange) {
        return BindingBuilder.bind(employeeDeleteRequestQueue).to(exchange).with(employeeDeleteRequestRoutingKey);
    }
}