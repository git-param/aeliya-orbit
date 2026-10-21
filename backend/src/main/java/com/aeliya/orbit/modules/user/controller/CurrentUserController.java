package com.aeliya.orbit.modules.user.controller;

import com.aeliya.orbit.modules.user.entity.User;
import com.aeliya.orbit.modules.user.repository.UserRepository;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class CurrentUserController {
    private final UserRepository users;

    public CurrentUserController(UserRepository users) {
        this.users = users;
    }

    public record CurrentUser(Long id, String username, String role, Set<String> permissions) {}

    @GetMapping("/api/me")
    public CurrentUser currentUser(Authentication authentication) {
        User user = users.findByUsername(authentication.getName())
            .filter(User::isActive)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Set<String> permissions = new TreeSet<>();
        authentication.getAuthorities().forEach(authority -> {
            if (!authority.getAuthority().startsWith("ROLE_")) permissions.add(authority.getAuthority());
        });
        return new CurrentUser(user.getId(), user.getUsername(), user.getRole(), permissions);
    }
}
