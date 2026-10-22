package com.aeliya.orbit.security;

import com.aeliya.orbit.modules.user.service.CustomUserDetailsService;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthorizationUnitTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void grantsRefreshAndInactiveAccountLosesSession() throws Exception {
        CustomUserDetailsService details = mock(CustomUserDetailsService.class);
        SessionAccountFilter filter = new SessionAccountFilter(details);
        AtomicReference<Set<String>> grants = new AtomicReference<>(Set.of("ROLE_USER", "EMPLOYEE_VIEW"));
        when(details.loadUserByUsername("employee")).thenAnswer(call -> principal(grants.get()));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal(Set.of("ROLE_USER")), null, principal(Set.of("ROLE_USER")).getAuthorities()));
        filter.doFilter(new MockHttpServletRequest("GET", "/api/me"), new MockHttpServletResponse(), new MockFilterChain());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("EMPLOYEE_VIEW")));
        grants.set(Set.of("ROLE_USER"));
        filter.doFilter(new MockHttpServletRequest("GET", "/api/me"), new MockHttpServletResponse(), new MockFilterChain());
        assertFalse(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("EMPLOYEE_VIEW")));
        when(details.loadUserByUsername("employee")).thenThrow(new UsernameNotFoundException("inactive"));
        filter.doFilter(new MockHttpServletRequest("GET", "/api/me"), new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(details, times(3)).loadUserByUsername("employee");
    }
    private UserDetails principal(Set<String> authorities) {
        return org.springframework.security.core.userdetails.User.withUsername("employee")
            .password("hash").authorities(authorities.toArray(String[]::new)).build();
    }
    public static void main(String[] args) throws Exception { new AuthorizationUnitTest().grantsRefreshAndInactiveAccountLosesSession(); }
}
