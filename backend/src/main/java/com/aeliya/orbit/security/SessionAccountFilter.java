package com.aeliya.orbit.security;

import com.aeliya.orbit.modules.user.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionAccountFilter extends OncePerRequestFilter {
    public static final String PASSWORD_HASH_ATTRIBUTE = SessionAccountFilter.class.getName() + ".passwordHash";
    private final CustomUserDetailsService users;

    public SessionAccountFilter(CustomUserDetailsService users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current != null && current.isAuthenticated() && current.getPrincipal() instanceof UserDetails) {
            try {
                UserDetails latest = users.loadUserByUsername(current.getName());
                var session = request.getSession(false);
                Object loginHash = session == null ? null : session.getAttribute(PASSWORD_HASH_ATTRIBUTE);
                if (loginHash == null) loginHash = ((UserDetails) current.getPrincipal()).getPassword();
                if (!Objects.equals(loginHash, latest.getPassword())) {
                    SecurityContextHolder.clearContext();
                    if (request.getSession(false) != null) request.getSession(false).invalidate();
                    chain.doFilter(request, response);
                    return;
                }
                SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(latest, null, latest.getAuthorities()));
            } catch (UsernameNotFoundException ex) {
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) request.getSession(false).invalidate();
            }
        }
        chain.doFilter(request, response);
    }
}
