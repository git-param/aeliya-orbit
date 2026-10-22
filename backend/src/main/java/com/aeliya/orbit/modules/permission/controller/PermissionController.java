package com.aeliya.orbit.modules.permission.controller;

import com.aeliya.orbit.modules.permission.dto.PermissionGrantRequest;
import com.aeliya.orbit.modules.permission.service.PermissionService;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/permissions")
public class PermissionController {
    private final PermissionService permissions;

    public PermissionController(PermissionService permissions) {
        this.permissions = permissions;
    }

    @GetMapping
    public Set<String> catalog() {
        return permissions.catalog();
    }

    @GetMapping("/roles/{role}")
    public Set<String> roleGrants(@PathVariable String role) {
        return permissions.roleGrants(role);
    }

    @PutMapping("/roles/{role}")
    public Set<String> replaceRoleGrants(@PathVariable String role, @RequestBody PermissionGrantRequest request,
            Authentication actor) {
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permission list is required");
        return permissions.replaceRoleGrants(role, request.permissions(), actor);
    }

    @GetMapping("/users/{userId}")
    public Set<String> userGrants(@PathVariable long userId) {
        return permissions.userGrants(userId);
    }

    @PutMapping("/users/{userId}")
    public Set<String> replaceUserGrants(@PathVariable long userId, @RequestBody PermissionGrantRequest request,
            Authentication actor) {
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permission list is required");
        return permissions.replaceUserGrants(userId, request.permissions(), actor);
    }
}
