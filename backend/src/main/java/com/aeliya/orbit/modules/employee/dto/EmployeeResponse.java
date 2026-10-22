package com.aeliya.orbit.modules.employee.dto;

import java.time.LocalDate;

public record EmployeeResponse(Long id, String employeeCode, String fullName, String workEmail,
    String designation, String department, String level, Long designationId, Long departmentId, Long levelId, Long reportingManagerId,
    boolean active, String pictureUrl, String address, String phone,
    LocalDate dateOfBirth, LocalDate dateOfJoining) {}
