package com.aeliya.orbit.modules.user.controller;

import com.aeliya.orbit.modules.user.service.PasswordManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class PasswordManagementController {
    private final PasswordManagementService passwords;
    public PasswordManagementController(PasswordManagementService passwords) { this.passwords = passwords; }

    @GetMapping public PasswordManagementService.AccountPage list(@RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page, Authentication actor) {
        return passwords.list(search, page, actor);
    }

    @PutMapping("/{id}/password") public ResponseEntity<Void> change(@PathVariable long id,
            @RequestBody PasswordManagementService.PasswordInput input, Authentication actor) {
        passwords.change(id, input, actor);
        return ResponseEntity.noContent().build();
    }
}
