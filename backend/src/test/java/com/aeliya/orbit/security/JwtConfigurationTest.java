package com.aeliya.orbit.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtConfigurationTest {
    @Test void rejectsWeakOrMissingSigningConfiguration() {
        JwtUtil jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secret", "short");
        ReflectionTestUtils.setField(jwt, "expiration", 1000L);
        assertThrows(IllegalStateException.class, jwt::validateConfiguration);
        ReflectionTestUtils.setField(jwt, "secret", "a".repeat(32));
        ReflectionTestUtils.setField(jwt, "expiration", 0L);
        assertThrows(IllegalStateException.class, jwt::validateConfiguration);
        ReflectionTestUtils.setField(jwt, "expiration", 1000L);
        assertDoesNotThrow(jwt::validateConfiguration);
    }

    public static void main(String[] args) {
        new JwtConfigurationTest().rejectsWeakOrMissingSigningConfiguration();
    }
}
