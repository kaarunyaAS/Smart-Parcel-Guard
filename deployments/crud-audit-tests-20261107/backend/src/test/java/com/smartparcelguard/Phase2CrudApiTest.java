package com.smartparcelguard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparcelguard.device.Device;
import com.smartparcelguard.device.DeviceAssignment;
import com.smartparcelguard.device.DeviceAssignmentRepository;
import com.smartparcelguard.device.DeviceRepository;
import com.smartparcelguard.device.DeviceTokenHasher;
import com.smartparcelguard.parcel.Parcel;
import com.smartparcelguard.parcel.ParcelRepository;
import com.smartparcelguard.parcel.ParcelStatusHistory;
import com.smartparcelguard.parcel.ParcelStatusHistoryRepository;
import com.smartparcelguard.security.JwtService;
import com.smartparcelguard.user.User;
import com.smartparcelguard.user.UserRepository;
import com.smartparcelguard.user.UserRole;
import jakarta.persistence.EntityManager;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
class Phase2CrudApiTest {

    private static final String TEST_PASSWORD = "CrudTest1";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private DeviceRepository deviceRepository;
    @Autowired private DeviceAssignmentRepository assignmentRepository;
    @Autowired private ParcelRepository parcelRepository;
    @Autowired private ParcelStatusHistoryRepository historyRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private EntityManager entityManager;

    @Test
    void userCreateReadUpdateAndActivationPersistAndEnforceAdminRole() throws Exception {
        User admin = saveUser("crud-admin", UserRole.ADMIN);
        String adminToken = token(admin);
        String username = unique("crud-operator");
        String email = username + "@example.com";

        MvcResult created = mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(username, email, TEST_PASSWORD, "CRUD Operator", "OPERATOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.role").value("OPERATOR"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andReturn();
        long userId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        entityManager.flush();
        entityManager.clear();
        User stored = userRepository.findById(userId).orElseThrow();
        assertEquals(email, stored.getEmail());
        assertTrue(passwordEncoder.matches(TEST_PASSWORD, stored.getPasswordHash()));
        assertNotEquals(TEST_PASSWORD, stored.getPasswordHash());

        String operatorToken = token(stored);
        mockMvc.perform(get("/api/users?size=100&sort=username,asc")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == " + userId + ")].username").value(username));
        mockMvc.perform(get("/api/users/" + userId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));
        mockMvc.perform(get("/api/users").header("Authorization", bearer(operatorToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bad user\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(unique("duplicate-user"), email, TEST_PASSWORD, "Duplicate", "OPERATOR")))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/users/" + userId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(username, username + ".updated@example.com", "UpdatedPassword1",
                                "Updated CRUD Operator", "OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Updated CRUD Operator"));

        entityManager.flush();
        entityManager.clear();
        stored = userRepository.findById(userId).orElseThrow();
        assertEquals(username + ".updated@example.com", stored.getEmail());
        assertTrue(passwordEncoder.matches("UpdatedPassword1", stored.getPasswordHash()));

        mockMvc.perform(patch("/api/users/" + userId + "/status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
        entityManager.flush();
        entityManager.clear();
        assertFalse(userRepository.findById(userId).orElseThrow().isEnabled());

        mockMvc.perform(patch("/api/users/" + userId + "/status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
        entityManager.flush();
        entityManager.clear();
        assertTrue(userRepository.findById(userId).orElseThrow().isEnabled());
    }

    @Test
    void parcelAndDeviceCreateReadUpdateAndAssignmentHistoryPersist() throws Exception {
        User admin = saveUser("crud-admin", UserRole.ADMIN);
        User operator = saveUser("crud-operator", UserRole.OPERATOR);
        String adminToken = token(admin);
        String operatorToken = token(operator);

        MvcResult deviceResult = mockMvc.perform(post("/api/devices")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firmwareVersion\":\"qa-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.deviceToken").isNotEmpty())
                .andReturn();
        JsonNode deviceResponse = objectMapper.readTree(deviceResult.getResponse().getContentAsString()).path("data");
        long deviceId = deviceResponse.path("id").asLong();
        String deviceToken = deviceResponse.path("deviceToken").asText();

        entityManager.flush();
        entityManager.clear();
        Device storedDevice = deviceRepository.findById(deviceId).orElseThrow();
        assertEquals(DeviceTokenHasher.sha256(deviceToken), storedDevice.getTokenHash());
        assertNotEquals(deviceToken, storedDevice.getTokenHash());
        mockMvc.perform(get("/api/devices/" + deviceId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceToken").doesNotExist());
        mockMvc.perform(get("/api/devices?size=100")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == " + deviceId + ")].firmwareVersion").value("qa-1"));

        mockMvc.perform(post("/api/devices")
                        .header("Authorization", bearer(operatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/devices/" + deviceId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        MvcResult parcelResult = mockMvc.perform(post("/api/parcels")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(parcelJson("CRUD parcel", operator.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("REGISTERED"))
                .andExpect(jsonPath("$.data.senderPhone").doesNotExist())
                .andReturn();
        long parcelId = objectMapper.readTree(parcelResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        entityManager.flush();
        entityManager.clear();
        Parcel storedParcel = parcelRepository.findById(parcelId).orElseThrow();
        assertEquals("CRUD parcel", storedParcel.getName());
        assertEquals(operator.getId(), storedParcel.getOperator().getId());
        mockMvc.perform(get("/api/parcels/" + parcelId).header("Authorization", bearer(operatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("CRUD parcel"));
        mockMvc.perform(get("/api/parcels").param("query", "CRUD parcel")
                        .header("Authorization", bearer(operatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(parcelId));

        mockMvc.perform(put("/api/parcels/" + parcelId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(parcelJson("Updated CRUD parcel", operator.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated CRUD parcel"));
        entityManager.flush();
        entityManager.clear();
        assertEquals("Updated CRUD parcel", parcelRepository.findById(parcelId).orElseThrow().getName());

        mockMvc.perform(post("/api/devices/" + deviceId + "/assign")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parcelId\":" + parcelId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedParcelId").value(parcelId));
        entityManager.flush();
        entityManager.clear();
        DeviceAssignment assignment = assignmentRepository.findByActiveDeviceKey(deviceId).orElseThrow();
        assertEquals(parcelId, assignment.getParcel().getId());
        assertNotNull(assignment.getAssignedAt());

        mockMvc.perform(put("/api/devices/" + deviceId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firmwareVersion\":\"qa-2\",\"enabled\":true,\"status\":\"MAINTENANCE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firmwareVersion").value("qa-2"))
                .andExpect(jsonPath("$.data.status").value("MAINTENANCE"));
        entityManager.flush();
        entityManager.clear();
        storedDevice = deviceRepository.findById(deviceId).orElseThrow();
        assertEquals("qa-2", storedDevice.getFirmwareVersion());
        assertEquals("MAINTENANCE", storedDevice.getStatus().name());

        mockMvc.perform(patch("/api/parcels/" + parcelId + "/status")
                        .header("Authorization", bearer(operatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"READY_FOR_PICKUP\",\"note\":\"CRUD test transition\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY_FOR_PICKUP"));
        entityManager.flush();
        entityManager.clear();
        assertEquals(2, historyRepository.findByParcelIdOrderByChangedAtAsc(parcelId).size());

        mockMvc.perform(post("/api/devices/" + deviceId + "/unassign")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedParcelId").doesNotExist());
        entityManager.flush();
        entityManager.clear();
        DeviceAssignment released = assignmentRepository.findById(assignment.getId()).orElseThrow();
        assertFalse(released.isActive());
        assertNotNull(released.getReleasedAt());
        assertEquals(parcelId, released.getParcel().getId());
    }

    private User saveUser(String prefix, UserRole role) {
        String username = unique(prefix);
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setFullName(prefix);
        user.setRole(role);
        user.setEnabled(true);
        user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        return userRepository.saveAndFlush(user);
    }

    private String token(User user) {
        return jwtService.generateToken(user.getUsername(), user.getRole().name(), user.getId());
    }

    private static String userJson(String username, String email, String password, String fullName, String role) {
        return """
                {"username":"%s","email":"%s","password":"%s","fullName":"%s","role":"%s"}
                """.formatted(username, email, password, fullName, role);
    }

    private static String parcelJson(String name, long operatorId) {
        return """
                {
                  "name":"%s","category":"QA documents","declaredValue":100.00,
                  "senderName":"Synthetic sender","senderPhone":"   ",
                  "receiverName":"Synthetic receiver","receiverPhone":"",
                  "pickupAddress":"Synthetic pickup","destinationAddress":"Synthetic destination",
                  "priority":"NORMAL","operatorId":%d
                }
                """.formatted(name, operatorId);
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 12);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
