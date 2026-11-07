package com.smartparcelguard.security;

import com.smartparcelguard.config.SmartParcelGuardProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    @Test
    void generatesATokenThatCanBeValidated() {
        JwtService service = serviceWithSecret("12345678901234567890123456789012");

        String token = service.generateToken("operator.one", "OPERATOR", 4L);

        assertTrue(service.isValid(token));
        assertEquals("operator.one", service.parse(token).getSubject());
        assertEquals("OPERATOR", service.parse(token).get("role", String.class));
        assertEquals(4L, service.parse(token).get("uid", Number.class).longValue());
    }

    @Test
    void rejectsATamperedToken() {
        JwtService service = serviceWithSecret("12345678901234567890123456789012");
        String token = service.generateToken("operator.one", "OPERATOR", 4L);

        assertFalse(service.isValid(token + "x"));
    }

    @Test
    void rejectsAShortSecret() {
        assertThrows(IllegalStateException.class,
                () -> serviceWithSecret("too-short"));
    }

    private JwtService serviceWithSecret(String secret) {
        SmartParcelGuardProperties properties = new SmartParcelGuardProperties();
        properties.getJwt().setSecret(secret);
        properties.getJwt().setExpirationMinutes(30);
        return new JwtService(properties);
    }
}
