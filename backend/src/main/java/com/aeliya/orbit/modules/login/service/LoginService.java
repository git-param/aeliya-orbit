package com.aeliya.orbit.modules.login.service;

import com.aeliya.orbit.modules.login.dto.LoginRequest;
import com.aeliya.orbit.modules.user.service.CustomUserDetailsService;
import com.aeliya.orbit.security.SessionAccountFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService users;
    private final HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    public LoginService(AuthenticationManager authenticationManager, CustomUserDetailsService users) {
        this.authenticationManager = authenticationManager;
        this.users = users;
    }

    public void authenticate(LoginRequest credentials, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(credentials.getUsername(), credentials.getPassword()));
        if (request.getSession(false) == null) request.getSession(true);
        else request.changeSessionId();
        request.getSession().setAttribute(SessionAccountFilter.PASSWORD_HASH_ATTRIBUTE,
            users.loadUserByUsername(authentication.getName()).getPassword());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
    }
}
