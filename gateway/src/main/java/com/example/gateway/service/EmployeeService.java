package com.example.gateway.service;

import com.example.domain.grpc.EmployeeProto;
import com.example.domain.grpc.EmployeeServiceGrpc;
import com.example.gateway.api.dto.EmployeeRequestDTO;
import com.example.gateway.api.dto.EmployeeResponseDTO;
import com.example.gateway.config.RabbitMQConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.gateway.config.RedisConfig.REDIS_ALL_EMPLOYEES_CACHE_KEY; // New import
import static com.example.gateway.config.RedisConfig.REDIS_EMPLOYEE_BY_ID_CACHE_KEY; // New import


@Service
@Slf4j
public class EmployeeService {

    private final ModelMapper modelMapper = new ModelMapper();

    @GrpcClient("employeeService")
    private EmployeeServiceGrpc.EmployeeServiceBlockingStub employeeServiceGrpc;

    private final RabbitTemplate rabbitTemplate;
    private final CacheManager cacheManager;

    public EmployeeService(RabbitTemplate rabbitTemplate, CacheManager cacheManager) {
        this.rabbitTemplate = rabbitTemplate;
        this.cacheManager = cacheManager;
    }


    @Cacheable(REDIS_ALL_EMPLOYEES_CACHE_KEY) // Use correct key
    public List<EmployeeResponseDTO> getAllEmployees() {
        log.info("Fetching all employees via gRPC...");
        var response = employeeServiceGrpc.listEmployees(EmployeeProto.Empty.newBuilder().build());
        return response.getEmployeesList()
                .stream()
                .map(employee -> modelMapper.map(employee, EmployeeResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Cacheable(value = REDIS_EMPLOYEE_BY_ID_CACHE_KEY, key = "#id") // Use correct key
    public EmployeeResponseDTO getEmployeeById(String id) {
        log.info("Fetching employee by ID via gRPC: {}", id);
        var request = EmployeeProto.EmployeeRequest.newBuilder().setId(id).build();
        var employee = employeeServiceGrpc.getEmployee(request);
        return modelMapper.map(employee.getEmployee(), EmployeeResponseDTO.class);
    }

    public void saveNewEmployee(EmployeeRequestDTO employeeRequestDTO) {
        try {
            if (employeeRequestDTO.getId() == null || employeeRequestDTO.getId().isBlank()) {
                String generatedId = UUID.randomUUID().toString();
                employeeRequestDTO.setId(generatedId);
                log.info("Generated new ID for employee: {}", generatedId);
            }

            byte[] message = new ObjectMapper().writeValueAsBytes(employeeRequestDTO);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.directExchangeName,
                    RabbitMQConfig.employeePostRequestRoutingKey,
                    message
            );

            EmployeeResponseDTO employeeResponseDTO = modelMapper.map(employeeRequestDTO, EmployeeResponseDTO.class);
            saveToCache(employeeResponseDTO);

            deleteFromCache(null);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize EmployeeRequestDTO to JSON", e);
            throw new RuntimeException("Serialization error", e);
        } catch (Exception e) {
            log.error("Error while sending message to RabbitMQ", e);
            throw e;
        }
    }

    public void updateEmployee(String id, EmployeeRequestDTO employeeRequestDTO) {
        try {
            employeeRequestDTO.setId(id);
            byte[] message = new ObjectMapper().writeValueAsBytes(employeeRequestDTO);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.directExchangeName,
                    RabbitMQConfig.employeePutRequestRoutingKey,
                    message
            );

            EmployeeResponseDTO employeeResponseDTO = modelMapper.map(employeeRequestDTO, EmployeeResponseDTO.class);
            saveToCache(employeeResponseDTO);

            deleteFromCache(null);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize EmployeeRequestDTO to JSON", e);
            throw new RuntimeException("Serialization error", e);
        } catch (Exception e) {
            log.error("Error while sending update request to RabbitMQ for ID: {}", id, e);
            throw e;
        }
    }

    public void deleteEmployee(String id) {
        try {
            byte[] message = id.getBytes();
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.directExchangeName,
                    RabbitMQConfig.employeeDeleteRequestRoutingKey,
                    message
            );

            deleteFromCache(id);
            deleteFromCache(null);
        } catch (Exception e) {
            log.error("Error while sending delete request to RabbitMQ for ID: {}", id, e);
            throw e;
        }
    }


    private void saveToCache(EmployeeResponseDTO employeeResponseDTO) {
        Cache cacheById = cacheManager.getCache(REDIS_EMPLOYEE_BY_ID_CACHE_KEY); // Use correct key
        if (cacheById != null) {
            cacheById.put(employeeResponseDTO.getId(), employeeResponseDTO);
            log.info("Saved employee to cache with ID: {}", employeeResponseDTO.getId());
        }
    }

    private void deleteFromCache(String id) {
        if (id == null) {
            Cache allEmployeesCache = cacheManager.getCache(REDIS_ALL_EMPLOYEES_CACHE_KEY); // Use correct key
            if (allEmployeesCache != null) {
                allEmployeesCache.clear();
            }
        } else {
            Cache employeeByIdCache = cacheManager.getCache(REDIS_EMPLOYEE_BY_ID_CACHE_KEY); // Use correct key
            if (employeeByIdCache != null) {
                employeeByIdCache.evictIfPresent(id);
            }
        }
    }
}