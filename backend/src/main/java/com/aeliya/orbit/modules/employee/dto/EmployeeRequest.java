package com.aeliya.orbit.modules.employee.dto;

import java.time.LocalDate;

public record EmployeeRequest(String employeeCode, Long accountId, String fullName,
    String address, String phone, String workEmail, LocalDate dateOfBirth,
    LocalDate dateOfJoining, Long designationId, Long departmentId, Long levelId,
    Long reportingManagerId) {}
