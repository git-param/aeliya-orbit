package com.aeliya.orbit.modules.employee.controller;

import com.aeliya.orbit.modules.employee.service.EmployeeService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/employee-accounts")
public class EmployeeAccountController {
    private final EmployeeService employees;
    public EmployeeAccountController(EmployeeService employees) { this.employees = employees; }

    @GetMapping
    public List<EmployeeService.AvailableAccount> available(@RequestParam(required = false) String search, Authentication actor) {
        return employees.availableAccounts(search, actor);
    }
}
