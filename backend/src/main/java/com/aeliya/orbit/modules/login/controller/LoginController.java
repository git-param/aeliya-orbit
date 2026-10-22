package com.aeliya.orbit.modules.login.controller;

import com.aeliya.orbit.modules.login.dto.LoginRequest;
import com.aeliya.orbit.modules.login.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class LoginController {
    private final LoginService login;

    public LoginController(LoginService login) {
        this.login = login;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody LoginRequest credentials,
            HttpServletRequest request, HttpServletResponse response) {
        try {
            login.authenticate(credentials, request, response);
            var moduleIds = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority()).filter(code -> code.startsWith("MODULE_"))
                .map(code -> code.substring(7)).sorted().toList();
            return ResponseEntity.ok(Map.of("status", "authenticated", "moduleIds", moduleIds));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
