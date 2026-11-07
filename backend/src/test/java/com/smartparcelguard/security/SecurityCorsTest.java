package com.smartparcelguard.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.http.HttpMethod;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SecurityCorsTest {

    @Test
    void allowsLiveServerOriginsAndRequiredApiHeaders() {
        CorsConfigurationSource source = configurationSource();
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/auth/login"));

        assertEquals(List.of("http://localhost:5500", "http://127.0.0.1:5500"), configuration.getAllowedOrigins());
        assertTrue(configuration.getAllowedMethods().containsAll(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")));
        assertEquals(List.of("Accept", "Authorization", "Content-Type"), configuration.getAllowedHeaders());
        assertTrue(configuration.checkOrigin("http://localhost:5500") != null);
        assertTrue(configuration.checkOrigin("http://127.0.0.1:5500") != null);
        assertTrue(configuration.checkHttpMethod(HttpMethod.POST) != null);
        assertTrue(configuration.checkHeaders(List.of("Authorization", "Content-Type")).containsAll(
                List.of("Authorization", "Content-Type")));
    }

    @Test
    void rejectsUnlistedCorsOrigin() {
        CorsConfiguration configuration = configurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/auth/login"));

        assertNull(configuration.checkOrigin("http://localhost:5501"));
    }

    private CorsConfigurationSource configurationSource() {
        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                mock(com.smartparcelguard.telemetry.DeviceTokenFilter.class),
                mock(RestSecurityHandlers.class)
        );
        return securityConfig.corsConfigurationSource();
    }
}
