package com.aeliya.orbit.modules.employee.dto;

import java.time.LocalDate;

public record EmployeeRequest(String employeeCode, String password, String fullName,
    String address, String phone, String workEmail, LocalDate dateOfBirth,
    LocalDate dateOfJoining, Long designationId, Long departmentId, Long levelId,
    Long reportingManagerId) {
    @Override public String toString() { return "EmployeeRequest[redacted]"; }
}
