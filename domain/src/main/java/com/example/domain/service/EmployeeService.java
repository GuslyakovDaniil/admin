package com.example.domain.service;

import com.example.domain.employee.Employee;
import com.example.domain.employee.EmployeeRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EmployeeService {

    private final EmployeeRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public EmployeeService(EmployeeRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public List<Employee> getAllEmployees() {
        return repository.findAll();
    }

    public Employee getEmployeeById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + id));
    }

    public void createEmployee(Employee employee) {
        validateEmployee(employee);
        rabbitTemplate.convertAndSend("employeeExchange", "employee.post", employee);
    }

    public void updateEmployee(UUID id, Employee employee) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Employee not found with ID: " + id);
        }
        validateEmployee(employee);
        employee.setId(id);
        rabbitTemplate.convertAndSend("employeeExchange", "employee.put", employee);
    }

    public void deleteEmployee(UUID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Employee not found with ID: " + id);
        }
        rabbitTemplate.convertAndSend("employeeExchange", "employee.delete", id.toString());
    }

    private void validateEmployee(Employee employee) {
        if (employee.getName() == null || employee.getName().isEmpty()) {
            throw new IllegalArgumentException("Employee name cannot be null or empty");
        }
        if (employee.getPosition() == null || employee.getPosition().isEmpty()) {
            throw new IllegalArgumentException("Employee position cannot be null or empty");
        }
        if (employee.getSalary() <= 0) {
            throw new IllegalArgumentException("Employee salary must be greater than zero");
        }
    }
}