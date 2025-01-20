package com.example.domain.grpc;

import com.example.domain.config.RabbitMQConfig;
import com.example.domain.employee.Employee;
import com.example.domain.employee.EmployeeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
public class EmployeeGrpcService extends EmployeeServiceGrpc.EmployeeServiceImplBase {

    private final EmployeeRepository employeeRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public EmployeeGrpcService(EmployeeRepository employeeRepository, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.employeeRepository = employeeRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Timed(value = "grpc.get_employee", description = "Time taken to get an employee by ID")
    public void getEmployee(EmployeeProto.EmployeeRequest request, StreamObserver<EmployeeProto.EmployeeResponse> responseObserver) {
        log.info("Received GET request for Employee ID: {}", request.getId());
        Employee employee = employeeRepository.findById(UUID.fromString(request.getId()))
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        EmployeeProto.Employee grpcEmployee = EmployeeProto.Employee.newBuilder()
                .setId(employee.getId().toString())
                .setName(employee.getName())
                .setPosition(employee.getPosition())
                .setSalary(employee.getSalary())
                .setHireDate(employee.getHireDate().toString())
                .build();

        EmployeeProto.EmployeeResponse response = EmployeeProto.EmployeeResponse.newBuilder()
                .setEmployee(grpcEmployee)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @Timed(value = "grpc.list_employees", description = "Time taken to list all employees")
    public void listEmployees(EmployeeProto.Empty request, StreamObserver<EmployeeProto.EmployeeListResponse> responseObserver) {
        log.info("Received LIST request for all employees");

        List<EmployeeProto.Employee> grpcEmployees = employeeRepository.findAll().stream()
                .map(employee -> EmployeeProto.Employee.newBuilder()
                        .setId(employee.getId().toString())
                        .setName(employee.getName())
                        .setPosition(employee.getPosition())
                        .setSalary(employee.getSalary())
                        .setHireDate(employee.getHireDate().toString())
                        .build())
                .collect(Collectors.toList());

        EmployeeProto.EmployeeListResponse response = EmployeeProto.EmployeeListResponse.newBuilder()
                .addAllEmployees(grpcEmployees)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @Timed(value = "grpc.create_employee", description = "Time taken to create an employee")
    public void createEmployee(EmployeeProto.Employee request, StreamObserver<EmployeeProto.Empty> responseObserver) {
        try {
            log.info("Received CREATE request for Employee: {}", request);
            Employee employee = new Employee(UUID.randomUUID(), request.getName(), request.getPosition(), request.getSalary(), java.sql.Date.valueOf(request.getHireDate()));
            byte[] message = objectMapper.writeValueAsBytes(employee);
            rabbitTemplate.convertAndSend(RabbitMQConfig.directExchangeName, RabbitMQConfig.employeePostRequestQueueRoutingKey, message);

            responseObserver.onNext(EmployeeProto.Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error while processing CREATE request", e);
            responseObserver.onError(e);
        }
    }

    @Override
    @Timed(value = "grpc.update_employee", description = "Time taken to update an employee")
    public void updateEmployee(EmployeeProto.Employee request, StreamObserver<EmployeeProto.Empty> responseObserver) {
        try {
            log.info("Received UPDATE request for Employee: {}", request);
            Employee employee = new Employee(UUID.fromString(request.getId()), request.getName(), request.getPosition(), request.getSalary(), java.sql.Date.valueOf(request.getHireDate()));
            byte[] message = objectMapper.writeValueAsBytes(employee);
            rabbitTemplate.convertAndSend(RabbitMQConfig.directExchangeName, RabbitMQConfig.employeePutRequestQueueRoutingKey, message);

            responseObserver.onNext(EmployeeProto.Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error while processing UPDATE request", e);
            responseObserver.onError(e);
        }
    }

    @Override
    @Timed(value = "grpc.delete_employee", description = "Time taken to delete an employee by ID")
    public void deleteEmployee(EmployeeProto.EmployeeRequest request, StreamObserver<EmployeeProto.Empty> responseObserver) {
        try {
            log.info("Received DELETE request for Employee ID: {}", request.getId());
            byte[] message = request.getId().getBytes();
            rabbitTemplate.convertAndSend(RabbitMQConfig.directExchangeName, RabbitMQConfig.employeeDeleteRequestQueueRoutingKey, message);

            responseObserver.onNext(EmployeeProto.Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error while processing DELETE request", e);
            responseObserver.onError(e);
        }
    }
}