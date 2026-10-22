package com.aeliya.orbit.security;

import com.aeliya.orbit.modules.user.service.CustomUserDetailsService;
import com.aeliya.orbit.modules.login.dto.LoginRequest;
import com.aeliya.orbit.modules.login.service.LoginService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordSessionRevocationTest {
    @Test void loginSurvivesCredentialErasureOnTheNextRequest() throws Exception {
        var details = mock(CustomUserDetailsService.class);
        var manager = mock(AuthenticationManager.class);
        var login = new LoginService(manager, details);
        var account = User.withUsername("employee@example.com").password("stored-hash").roles("USER").build();
        var erased = User.withUsername("employee@example.com").password("stored-hash").roles("USER").build();
        ((CredentialsContainer) erased).eraseCredentials();
        when(manager.authenticate(any())).thenReturn(
            new UsernamePasswordAuthenticationToken(erased, null, erased.getAuthorities()));
        when(details.loadUserByUsername("employee@example.com")).thenReturn(account);
        var credentials = new LoginRequest();
        credentials.setUsername("employee@example.com");
        credentials.setPassword("password");
        var request = new MockHttpServletRequest();
        login.authenticate(credentials, request, new MockHttpServletResponse());
        var next = new MockHttpServletRequest("GET", "/api/me");
        next.setSession(request.getSession());
        try {
            new SessionAccountFilter(details).doFilter(next, new MockHttpServletResponse(), mock(FilterChain.class));
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            assertEquals("employee@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        } finally { SecurityContextHolder.clearContext(); }
    }

    @Test void changedPasswordInvalidatesExistingSession() throws Exception {
        var details = mock(CustomUserDetailsService.class);
        var filter = new SessionAccountFilter(details);
        var old = User.withUsername("employee@example.com").password("old-hash").roles("USER").build();
        var current = User.withUsername("employee@example.com").password("new-hash").roles("USER").build();
        var request = new MockHttpServletRequest();
        var session = new MockHttpSession();
        session.setAttribute(SessionAccountFilter.PASSWORD_HASH_ATTRIBUTE, "old-hash");
        request.setSession(session);
        when(details.loadUserByUsername("employee@example.com")).thenReturn(current);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(old, null, old.getAuthorities()));
        FilterChain chain = mock(FilterChain.class);
        try {
            filter.doFilter(request, new MockHttpServletResponse(), chain);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            assertThrows(IllegalStateException.class, () -> session.getAttribute("anything"));
            verify(chain).doFilter(eq(request), any());
        } finally { SecurityContextHolder.clearContext(); }
    }

    public static void main(String[] args) throws Exception {
        var test = new PasswordSessionRevocationTest();
        test.loginSurvivesCredentialErasureOnTheNextRequest();
        test.changedPasswordInvalidatesExistingSession();
    }
}
