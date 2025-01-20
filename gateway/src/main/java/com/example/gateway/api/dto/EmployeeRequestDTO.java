package com.example.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeRequestDTO {
    private String id;
    private String name;
    private String position;
    private double salary;
    private String hireDate;
}