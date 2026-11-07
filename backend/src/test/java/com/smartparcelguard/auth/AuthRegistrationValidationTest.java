package com.smartparcelguard.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.smartparcelguard.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthRegistrationValidationTest {

    private final AuthService authService = mock(AuthService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void closeValidator() {
        validator.close();
    }

    @Test
    void reportsInvalidUsernameWithoutEchoingSubmittedValues() throws Exception {
        assertValidationError(
                """
                        {"username":"bad username","email":"admin@example.com","password":"SecretValue-123","fullName":"Admin"}
                        """,
                "username",
                "letters, numbers",
                "bad username",
                "SecretValue-123");
    }

    @Test
    void reportsInvalidEmailWithoutEchoingSubmittedValues() throws Exception {
        assertValidationError(
                """
                        {"username":"admin","email":"not-an-email","password":"SecretValue-123","fullName":"Admin"}
                        """,
                "email",
                "email",
                "not-an-email",
                "SecretValue-123");
    }

    @Test
    void reportsShortPasswordWithoutEchoingPassword() throws Exception {
        assertValidationError(
                """
                        {"username":"admin","email":"admin@example.com","password":"short","fullName":"Admin"}
                        """,
                "password",
                "between 8 and 72",
                "short");
    }

    @Test
    void reportsMissingFieldsByNameWithoutEchoingSubmittedValues() throws Exception {
        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("Validation failed", response.path("message").asText());
        for (String field : new String[]{"username", "email", "password", "fullName"}) {
            assertTrue(hasFieldError(response, field), "Expected validation error for " + field);
        }
        assertFalse(result.getResponse().getContentAsString().contains("passwordHash"));
        verifyNoInteractions(authService);
    }

    @Test
    void reportsMalformedJsonSeparatelyWithoutEchoingSubmittedValues() throws Exception {
        String malformedBody = "{\"username\":\"private-value\",\"password\":\"SecretValue-123";
        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(malformedBody))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        assertEquals("Request body is invalid", response.path("message").asText());
        assertFalse(response.has("errors"));
        assertFalse(responseBody.contains("private-value"));
        assertFalse(responseBody.contains("SecretValue-123"));
        verifyNoInteractions(authService);
    }

    private void assertValidationError(String requestBody,
                                       String field,
                                       String messageFragment,
                                       String... submittedValues) throws Exception {
        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        assertEquals("Validation failed", response.path("message").asText());
        JsonNode fieldError = findFieldError(response, field);
        assertTrue(fieldError.isObject(), "Expected a structured error for " + field);
        assertTrue(fieldError.path("message").asText().toLowerCase().contains(messageFragment.toLowerCase()),
                "Unexpected validation message for " + field);
        for (String submittedValue : submittedValues) {
            assertFalse(responseBody.contains(submittedValue), "Response must not echo submitted values");
        }
        verifyNoInteractions(authService);
    }

    private boolean hasFieldError(JsonNode response, String field) {
        return findFieldError(response, field).isObject();
    }

    private JsonNode findFieldError(JsonNode response, String field) {
        for (JsonNode error : response.path("errors")) {
            if (field.equals(error.path("field").asText())) {
                return error;
            }
        }
        return NullNode.getInstance();
    }
}
