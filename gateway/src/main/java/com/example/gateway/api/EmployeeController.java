package com.example.gateway.api;

import com.example.gateway.api.dto.EmployeeRequestDTO;
import com.example.gateway.api.dto.EmployeeResponseDTO;
import com.example.gateway.service.EmployeeService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    @Timed(value = "gateway.getAllEmployees", description = "Time taken to fetch all employees")
    @GetMapping
    public ResponseEntity<?> getAllEmployees() {
        log.info("Received request to fetch all employees");
        try {
            List<EmployeeResponseDTO> employees = employeeService.getAllEmployees();
            log.info("Returning {} employees", employees.size());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "employees", employees
            ));
        } catch (Exception e) {
            log.error("Error occurred while fetching all employees", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getLocalizedMessage()
            ));
        }
    }

    @Timed(value = "gateway.getEmployeeById", description = "Time taken to fetch employee by ID")
    @GetMapping("/{id}")
    public ResponseEntity<?> getEmployeeById(@PathVariable String id) {
        log.info("Received request to fetch employee by ID: {}", id);
        try {
            EmployeeResponseDTO employee = employeeService.getEmployeeById(id);
            log.info("Returning employee: {}", employee);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "employee", employee
            ));
        } catch (Exception e) {
            log.error("Error occurred while fetching employee by ID {}: {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getLocalizedMessage()
            ));
        }
    }

    @Timed(value = "gateway.createEmployee", description = "Time taken to create an employee")
    @PostMapping
    public ResponseEntity<?> createEmployee(@RequestBody EmployeeRequestDTO employeeRequestDTO) {
        log.info("Received request to create new employee: {}", employeeRequestDTO);
        try {
            employeeService.saveNewEmployee(employeeRequestDTO);
            log.info("Employee successfully created: {}", employeeRequestDTO);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Employee successfully created"
            ));
        } catch (Exception e) {
            log.error("Error occurred while creating employee: {}", employeeRequestDTO, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getLocalizedMessage()
            ));
        }
    }

    @Timed(value = "gateway.updateEmployee", description = "Time taken to update an employee")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEmployee(@PathVariable String id, @RequestBody EmployeeRequestDTO employeeRequestDTO) {
        log.info("Received request to update employee with ID: {}", id);
        try {
            employeeService.updateEmployee(id, employeeRequestDTO);
            log.info("Employee with ID {} successfully updated.", id);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Employee successfully updated"
            ));
        } catch (Exception e) {
            log.error("Error occurred while updating employee with ID {}: {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getLocalizedMessage()
            ));
        }
    }

    @Timed(value = "gateway.deleteEmployee", description = "Time taken to delete an employee")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable String id) {
        log.info("Received request to delete employee with ID: {}", id);
        try {
            employeeService.deleteEmployee(id);
            log.info("Employee with ID {} successfully deleted.", id);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Employee successfully deleted"
            ));
        } catch (Exception e) {
            log.error("Error occurred while deleting employee with ID {}: {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getLocalizedMessage()
            ));
        }
    }
}