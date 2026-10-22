package com.aeliya.orbit.config;

import com.aeliya.orbit.security.SessionAccountFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SessionAccountFilter accountFilter) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/admin/permissions", "/api/admin/permissions/roles/*", "/api/admin/permissions/users/*").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/admin/permissions/roles/*", "/api/admin/permissions/users/*").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/employee-accounts").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/organization/options").access(organizationSetupAccess())
                .requestMatchers(HttpMethod.POST, "/api/admin/organization/options").access(organizationSetupAccess())
                .requestMatchers(HttpMethod.PUT, "/api/admin/organization/options/*", "/api/admin/organization/options/*/status").access(organizationSetupAccess())
                .requestMatchers(HttpMethod.GET, "/api/employees", "/api/employees/count").hasAuthority("EMPLOYEE_VIEW")
                .requestMatchers(HttpMethod.GET, "/api/employees/manager-options").hasAuthority("EMPLOYEE_VIEW")
                .requestMatchers(HttpMethod.GET, "/api/employees/me", "/api/employees/*", "/api/employees/*/picture", "/api/organization/options").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/employees").hasAuthority("EMPLOYEE_CREATE")
                .requestMatchers(HttpMethod.PUT, "/api/employees/*").hasAuthority("EMPLOYEE_UPDATE")
                .requestMatchers(HttpMethod.POST, "/api/employees/*/picture").hasAuthority("EMPLOYEE_UPDATE")
                .requestMatchers(HttpMethod.POST, "/api/employees/*/deactivate").hasAuthority("EMPLOYEE_DEACTIVATE")
                .requestMatchers(HttpMethod.POST, "/api/employees/*/reactivate").hasAuthority("EMPLOYEE_UPDATE")
                .requestMatchers(HttpMethod.GET, "/api/workday/settings", "/api/workday/holidays", "/api/breaks/today", "/api/dashboard/birthdays").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/workday/settings").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/workday/holidays").hasAuthority("HOLIDAY_MANAGE")
                .requestMatchers(HttpMethod.POST, "/api/breaks/start", "/api/breaks/end").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/me", "/api/health").authenticated()
                .anyRequest().denyAll()
            )
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .logout(logout -> logout.disable())
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, exception) -> response.sendError(401)))
            .addFilterAfter(accountFilter, SecurityContextHolderFilter.class);
        return http.build();
    }

    private AuthorizationManager<RequestAuthorizationContext> organizationSetupAccess() {
        return (authentication, context) -> {
            var authorities = authentication.get().getAuthorities().stream().map(a -> a.getAuthority()).toList();
            boolean allowed = (authorities.contains("ROLE_ADMIN") || authorities.contains("ROLE_SUPER_ADMIN"))
                && authorities.contains("DEPARTMENT_MANAGE");
            return new AuthorizationDecision(allowed);
        };
    }

    @Bean
    public FilterRegistrationBean<SessionAccountFilter> sessionAccountFilterRegistration(SessionAccountFilter filter) {
        FilterRegistrationBean<SessionAccountFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}") List<String> origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowCredentials(true);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
