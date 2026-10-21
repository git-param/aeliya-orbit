package com.aeliya.orbit.modules.login.controller;

import com.aeliya.orbit.modules.login.dto.LoginRequest;
import com.aeliya.orbit.modules.login.dto.LoginResponse;
import com.aeliya.orbit.modules.login.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            // The service will handle authentication and token generation
            LoginResponse response = loginService.authenticate(loginRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
             // Return 401 Unauthorized if login fails
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }
}