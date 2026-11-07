package com.smartparcelguard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparcelguard.device.Device;
import com.smartparcelguard.device.DeviceRepository;
import com.smartparcelguard.device.DeviceTokenHasher;
import com.smartparcelguard.user.User;
import com.smartparcelguard.user.UserRepository;
import com.smartparcelguard.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Phase2ApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void healthRemainsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void registersLogsInAndRejectsInvalidCredentials() throws Exception {
        String username = unique("admin");
        long before = userRepository.count();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(username, username + "@example.com", "ParcelTest1", "Admin User")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.role").value(before == 0 ? "ADMIN" : "OPERATOR"));

        var saved = userRepository.findByUsername(username).orElseThrow();
        assertTrue(passwordEncoder.matches("ParcelTest1", saved.getPasswordHash()));
        assertNotEquals("ParcelTest1", saved.getPasswordHash());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"wrong-password"}
                                """.formatted(username)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"ParcelTest1"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void enforcesRolesParcelUpdatesAndDeviceAssignment() throws Exception {
        String adminName = unique("admin");
        String adminToken = tokenFor(adminName, UserRole.ADMIN);
        String operatorName = unique("operator");
        long operatorId = saveUser(operatorName, UserRole.OPERATOR).getId();
        String operatorToken = login(operatorName, "ParcelTest1");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s","password":"ParcelTest1","fullName":"Duplicate","role":"OPERATOR"}
                                """.formatted(unique("other"), operatorName + "@example.com")))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/parcels")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        MvcResult deviceResult = mockMvc.perform(post("/api/devices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firmwareVersion\":\"1.0.0\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.deviceToken").isNotEmpty())
                .andReturn();
        JsonNode device = objectMapper.readTree(deviceResult.getResponse().getContentAsString()).path("data");
        long deviceId = device.path("id").asLong();
        String rawToken = device.path("deviceToken").asText();
        Device stored = deviceRepository.findById(deviceId).orElseThrow();
        assertEquals(DeviceTokenHasher.sha256(rawToken), stored.getTokenHash());
        assertNotEquals(rawToken, stored.getTokenHash());

        mockMvc.perform(get("/api/devices/" + deviceId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceToken").doesNotExist());

        mockMvc.perform(post("/api/devices").header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        MvcResult parcelResult = mockMvc.perform(post("/api/parcels")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(parcelJson("Sealed documents", operatorId, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.parcelCode").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("REGISTERED"))
                .andReturn();
        long parcelId = objectMapper.readTree(parcelResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(put("/api/parcels/" + parcelId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(parcelJson("Updated documents", operatorId, deviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated documents"))
                .andExpect(jsonPath("$.data.deviceId").value(deviceId));

        mockMvc.perform(get("/api/parcels?query=Updated")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(parcelId));

        mockMvc.perform(patch("/api/parcels/" + parcelId + "/status")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"READY_FOR_PICKUP\",\"note\":\"Packed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY_FOR_PICKUP"));

        mockMvc.perform(patch("/api/parcels/" + parcelId + "/status")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/parcels/" + parcelId + "/history")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(post("/api/devices/" + deviceId + "/unassign")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedParcelId").doesNotExist());

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/parcels/" + parcelId).header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isUnauthorized());
    }

    private String tokenFor(String username, UserRole role) throws Exception {
        saveUser(username, role);
        return login(username, "ParcelTest1");
    }

    private User saveUser(String username, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setFullName("Test User");
        user.setRole(role);
        user.setEnabled(true);
        user.setPasswordHash(passwordEncoder.encode("ParcelTest1"));
        return userRepository.saveAndFlush(user);
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }

    private String userJson(String username, String email, String password, String fullName) {
        return """
                {"username":"%s","email":"%s","password":"%s","fullName":"%s"}
                """.formatted(username, email, password, fullName);
    }

    private String parcelJson(String name, long operatorId, Long deviceId) {
        String deviceField = deviceId == null ? "" : ",\"deviceId\":" + deviceId;
        return """
                {
                  "name":"%s",
                  "description":"Confidential papers",
                  "category":"Documents",
                  "declaredValue":1500.00,
                  "senderName":"Sender",
                  "senderPhone":"9876543210",
                  "senderAddress":"Chennai",
                  "receiverName":"Receiver",
                  "receiverPhone":"9876543211",
                  "receiverAddress":"Madurai",
                  "pickupAddress":"Chennai warehouse",
                  "destinationAddress":"Madurai office",
                  "priority":"HIGH",
                  "operatorId":%d%s
                }
                """.formatted(name, operatorId, deviceField);
    }

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
