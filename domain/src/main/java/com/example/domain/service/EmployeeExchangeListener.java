package com.example.domain.service;

import com.example.domain.employee.Employee;
import com.example.domain.employee.EmployeeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.example.domain.config.RabbitMQConfig.*;

@Service
@Slf4j
public class EmployeeExchangeListener {

    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;

    public EmployeeExchangeListener(EmployeeRepository employeeRepository, ObjectMapper objectMapper) {
        this.employeeRepository = employeeRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = employeePostRequestQueueName)
    public void onPostMethod(byte[] message) {
        try {
            log.info("Received POST request: {}", new String(message));
            Employee employee = objectMapper.readValue(message, Employee.class);
            employeeRepository.save(employee);
            log.info("Employee successfully created: {}", employee);
        } catch (Exception e) {
            log.error("Error processing POST message", e);
        }
    }

    @RabbitListener(queues = employeePutRequestQueueName)
    public void onPutMethod(byte[] message) {
        try {
            log.info("Received PUT request: {}", new String(message));
            Employee employee = objectMapper.readValue(message, Employee.class);
            if (employeeRepository.existsById(employee.getId())) {
                employeeRepository.save(employee);
                log.info("Employee successfully updated: {}", employee);
            } else {
                log.warn("Employee not found for update: {}", employee.getId());
            }
        } catch (Exception e) {
            log.error("Error processing PUT message", e);
        }
    }

    @RabbitListener(queues = employeeDeleteRequestQueueName)
    public void onDeleteMethod(byte[] message) {
        try {
            log.info("Received DELETE request: {}", new String(message));
            UUID id = UUID.fromString(new String(message));
            employeeRepository.deleteById(id);
            log.info("Employee successfully deleted for ID: {}", id);
        } catch (Exception e) {
            log.error("Error processing DELETE message", e);
        }
    }
}