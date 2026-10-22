package com.aeliya.orbit.modules.permission.controller;

import com.aeliya.orbit.modules.permission.service.DepartmentPermissionService;
import com.aeliya.orbit.modules.permission.service.DepartmentPermissionService.DepartmentView;
import com.aeliya.orbit.modules.permission.service.DepartmentPermissionService.ModuleView;
import com.aeliya.orbit.modules.permission.service.DepartmentPermissionService.UpdateRequest;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/department-permissions")
public class DepartmentPermissionController {
    private final DepartmentPermissionService service;
    public DepartmentPermissionController(DepartmentPermissionService service) { this.service = service; }
    @GetMapping("/modules") public List<ModuleView> modules(Authentication actor) { return service.modules(actor); }
    @GetMapping public List<DepartmentView> departments(Authentication actor) { return service.departments(actor); }
    @PutMapping("/{id}") public DepartmentView replace(@PathVariable long id, @RequestBody UpdateRequest request,
            Authentication actor) { return service.replace(id, request, actor); }
}
