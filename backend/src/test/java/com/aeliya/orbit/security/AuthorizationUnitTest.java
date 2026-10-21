package com.aeliya.orbit.security;

import com.aeliya.orbit.modules.permission.service.PermissionService;
import com.aeliya.orbit.modules.user.entity.User;
import com.aeliya.orbit.modules.user.repository.UserRepository;
import com.aeliya.orbit.modules.user.service.CustomUserDetailsService;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthorizationUnitTest {
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test void authoritiesReloadOnEveryRequestAndRevocationTakesEffect() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        JwtUtil jwt = mock(JwtUtil.class);
        CustomUserDetailsService details = mock(CustomUserDetailsService.class);
        ReflectionTestUtils.setField(filter, "jwtUtil", jwt);
        ReflectionTestUtils.setField(filter, "userDetailsService", details);
        when(jwt.extractUsername("existing-token")).thenReturn("employee");
        when(jwt.validateToken(eq("existing-token"), any())).thenReturn(true);
        AtomicReference<Set<String>> grants = new AtomicReference<>(Set.of("ROLE_USER", "EMPLOYEE_VIEW"));
        when(details.loadUserByUsername("employee")).thenAnswer(invocation -> principal(grants.get()));

        filter.doFilter(request(), new MockHttpServletResponse(), new MockFilterChain());
        assertTrue(hasAuthority("EMPLOYEE_VIEW"));
        SecurityContextHolder.clearContext();
        grants.set(Set.of("ROLE_USER"));
        filter.doFilter(request(), new MockHttpServletResponse(), new MockFilterChain());
        assertFalse(hasAuthority("EMPLOYEE_VIEW"));
        verify(details, times(2)).loadUserByUsername("employee");
    }

    @Test void inactiveUserIsRejectedByPrincipalLoaderAndExistingToken() throws ServletException, IOException {
        UserRepository users = mock(UserRepository.class);
        PermissionService permissions = mock(PermissionService.class);
        User inactive = new User("employee", "hash", "ROLE_USER");
        inactive.setActive(false);
        when(users.findByUsername("employee")).thenReturn(Optional.of(inactive));
        CustomUserDetailsService details = new CustomUserDetailsService();
        ReflectionTestUtils.setField(details, "userRepository", users);
        ReflectionTestUtils.setField(details, "permissionService", permissions);
        assertThrows(UsernameNotFoundException.class, () -> details.loadUserByUsername("employee"));
        verifyNoInteractions(permissions);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        JwtUtil jwt = mock(JwtUtil.class);
        ReflectionTestUtils.setField(filter, "jwtUtil", jwt);
        ReflectionTestUtils.setField(filter, "userDetailsService", details);
        when(jwt.extractUsername("existing-token")).thenReturn("employee");
        filter.doFilter(request(), new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me");
        request.addHeader("Authorization", "Bearer existing-token");
        return request;
    }

    private UserDetails principal(Set<String> authorities) {
        return org.springframework.security.core.userdetails.User.withUsername("employee")
            .password("hash").authorities(authorities.toArray(String[]::new)).build();
    }

    private boolean hasAuthority(String authority) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .anyMatch(a -> authority.equals(a.getAuthority()));
    }

    public static void main(String[] args) throws Exception {
        AuthorizationUnitTest test = new AuthorizationUnitTest();
        test.authoritiesReloadOnEveryRequestAndRevocationTakesEffect();
        test.clearContext();
        test.inactiveUserIsRejectedByPrincipalLoaderAndExistingToken();
        test.clearContext();
    }
}
