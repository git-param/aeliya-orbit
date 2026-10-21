package com.aeliya.orbit.modules.login.service;

import com.aeliya.orbit.modules.login.dto.LoginRequest;
import com.aeliya.orbit.modules.login.dto.LoginResponse;
import com.aeliya.orbit.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    public LoginResponse authenticate(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        String jwt = jwtUtil.generateToken(userDetails);

        return new LoginResponse(jwt, role);
    }
}
