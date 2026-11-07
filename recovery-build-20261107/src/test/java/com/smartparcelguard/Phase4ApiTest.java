package com.smartparcelguard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparcelguard.alert.AlertRepository;
import com.smartparcelguard.device.DeviceAssignmentRepository;
import com.smartparcelguard.device.DeviceRepository;
import com.smartparcelguard.parcel.Parcel;
import com.smartparcelguard.parcel.ParcelRepository;
import com.smartparcelguard.parcel.ParcelStatus;
import com.smartparcelguard.risk.RiskAnalysisRepository;
import com.smartparcelguard.risk.RiskRecalculationScheduler;
import com.smartparcelguard.risk.SecuritySetting;
import com.smartparcelguard.risk.SecuritySettingRepository;
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

import java.time.Instant;
import java.time.Clock;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Phase4ApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ParcelRepository parcelRepository;
    @Autowired private RiskAnalysisRepository riskAnalysisRepository;
    @Autowired private AlertRepository alertRepository;
    @Autowired private DeviceAssignmentRepository assignmentRepository;
    @Autowired private DeviceRepository deviceRepository;
    @Autowired private SecuritySettingRepository settingRepository;
    @Autowired private RiskRecalculationScheduler riskRecalculationScheduler;
    @Autowired private Clock clock;

    @Test
    void managesRiskSettingsForAdminsOnlyAndRejectsInvalidThresholds() throws Exception {
        User admin = saveUser("settings-admin", UserRole.ADMIN);
        User operator = saveUser("settings-operator", UserRole.OPERATOR);
        String adminToken = login(admin.getUsername());
        String operatorToken = login(operator.getUsername());

        mockMvc.perform(get("/api/admin/risk-settings").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.riskMaxScore").value(100));
        mockMvc.perform(get("/api/admin/risk-settings").header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/risk-settings")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(riskSettings(25, 50, 75, 100)))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/risk-settings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(riskSettings(50, 25, 75, 100)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/risk-settings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(riskSettings(25, 50, 75, 100)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.riskLevelHigh").value(50));

        assertEquals("50", settingRepository.findBySettingKey("risk.level.high").orElseThrow().getSettingValue());
    }

    @Test
    void staleDeviceRecalculationPersistsRiskAndDoesNotDuplicateOpenAlert() throws Exception {
        User admin = saveUser("scheduler-admin", UserRole.ADMIN);
        User operator = saveUser("scheduler-operator", UserRole.OPERATOR);
        String adminToken = login(admin.getUsername());
        long deviceId = registerDevice(adminToken);
        long parcelId = createParcel(adminToken, operator.getId(), deviceId);
        Parcel parcel = parcelRepository.findById(parcelId).orElseThrow();
        parcel.setStatus(ParcelStatus.IN_TRANSIT);

        var assignment = assignmentRepository.findByParcelIdAndActiveDeviceKeyIsNotNull(parcelId).get(0);
        assignment.getDevice().setLastSeenAt(clock.instant().minusSeconds(300));
        deviceRepository.saveAndFlush(assignment.getDevice());
        SecuritySetting offlineWeight = settingRepository.findBySettingKey("risk.weight.device_offline").orElseThrow();
        offlineWeight.setSettingValue("80");
        settingRepository.saveAndFlush(offlineWeight);

        riskRecalculationScheduler.recalculateAssignedParcels();
        SecuritySetting dedupeWindow = settingRepository.findBySettingKey("risk.dedupe_window_seconds").orElseThrow();
        dedupeWindow.setSettingValue("1");
        settingRepository.saveAndFlush(dedupeWindow);
        riskRecalculationScheduler.recalculateAssignedParcels();

        var analyses = riskAnalysisRepository.findByParcelIdOrderByCalculatedAtAsc(parcelId);
        assertTrue(analyses.stream()
                        .anyMatch(analysis -> analysis.getRiskLevel().name().equals("CRITICAL")
                                && analysis.getReasons().contains("DEVICE_OFFLINE")),
                "Unexpected risk analyses: " + analyses.stream()
                        .map(analysis -> analysis.getRiskLevel() + ":" + analysis.getScore() + ":" + analysis.getReasons())
                        .toList());
        assertEquals(1, alertRepository.findAll().stream()
                .filter(alert -> alert.getParcel().getId().equals(parcelId))
                .count());
    }

    @Test
    void scoresImpactAndOpeningPersistsOneAlertAndProtectsRoles() throws Exception {
        User admin = saveUser("admin", UserRole.ADMIN);
        User operator = saveUser("operator", UserRole.OPERATOR);
        User stranger = saveUser("stranger", UserRole.OPERATOR);
        String adminToken = login(admin.getUsername());
        String operatorToken = login(operator.getUsername());
        long deviceId = registerDevice(adminToken);
        long parcelId = createParcel(adminToken, operator.getId(), deviceId);
        Parcel parcel = parcelRepository.findById(parcelId).orElseThrow();
        parcel.setStatus(ParcelStatus.IN_TRANSIT);

        mockMvc.perform(post("/api/telemetry/sensors")
                        .header("X-Device-Token", deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sensor("1.5000", "CLOSED", 100)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.impactDetected").value(false));
        assertEquals(0, alertRepository.findAll().stream().filter(alert -> alert.getParcel().getId().equals(parcelId)).count());

        mockMvc.perform(post("/api/telemetry/sensors")
                        .header("X-Device-Token", deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sensor("3.0000", "OPEN", 80)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/telemetry/sensors")
                        .header("X-Device-Token", deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sensor("3.0000", "OPEN", 80)))
                .andExpect(status().isCreated());

        var analyses = riskAnalysisRepository.findByParcelIdOrderByCalculatedAtAsc(parcelId);
        assertTrue(analyses.stream().anyMatch(analysis -> analysis.getScore() >= 75));
        assertEquals(1, alertRepository.findAll().stream().filter(alert -> alert.getParcel().getId().equals(parcelId)).count());

        MvcResult alerts = mockMvc.perform(get("/api/alerts").header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].unread").value(true))
                .andReturn();
        long alertId = objectMapper.readTree(alerts.getResponse().getContentAsString()).path("data").path(0).path("id").asLong();

        mockMvc.perform(post("/api/alerts/" + alertId + "/acknowledge").header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"));
        mockMvc.perform(post("/api/alerts/" + alertId + "/resolve")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Checked the seal\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        mockMvc.perform(get("/api/parcels/" + parcelId + "/risk").header("Authorization", "Bearer " + login(stranger.getUsername())))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalParcels").value(1))
                .andExpect(jsonPath("$.data.highRisk").value(1));
    }

    private String deviceToken;

    private long registerDevice(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/devices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firmwareVersion\":\"1.0.0\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        deviceToken = data.path("deviceToken").asText();
        return data.path("id").asLong();
    }

    private long createParcel(String adminToken, long operatorId, long deviceId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/parcels")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Risk parcel","category":"Documents","declaredValue":10,"senderName":"A","receiverName":"B","pickupAddress":"Chennai","destinationAddress":"Madurai","priority":"HIGH","operatorId":%d,"deviceId":%d}
                                """.formatted(operatorId, deviceId)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private User saveUser(String prefix, UserRole role) {
        String name = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUsername(name);
        user.setEmail(name + "@example.com");
        user.setFullName(prefix);
        user.setRole(role);
        user.setEnabled(true);
        user.setPasswordHash(passwordEncoder.encode("ParcelTest1"));
        return userRepository.saveAndFlush(user);
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"ParcelTest1"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }

    private static String riskSettings(int medium, int high, int critical, int maxScore) {
        return """
                {
                  "impactThresholdG":2.5,"vibrationThresholdG":1.3,"impactMinDurationMs":50,
                  "deviceOfflineAfterSeconds":180,"riskDedupeWindowSeconds":300,
                  "riskStrongImpactPoints":25,"riskUnauthorizedOpeningPoints":50,
                  "riskUnusualVibrationPoints":10,"riskRouteDeviationPoints":20,
                  "riskDeviceOfflinePoints":15,"riskCorrelationBonusPoints":15,
                  "riskMaxScore":%d,"riskCorrelationWindowSeconds":600,"riskVibrationRepeatCount":5,
                  "riskLevelMedium":%d,"riskLevelHigh":%d,"riskLevelCritical":%d,
                  "gpsAnomalyMinimumJumpMeters":100,"gpsAnomalyMaximumPlausibleSpeedMps":55
                }
                """.formatted(maxScore, medium, high, critical);
    }

    private static String sensor(String x, String reed, int duration) {
        return """
                {"accelerationX":%s,"accelerationY":0,"accelerationZ":0,"gyroscopeX":0,"gyroscopeY":0,"gyroscopeZ":0,"reedSwitch":"%s","durationMs":%d,"recordedAt":"%s","dataSource":"SIMULATED"}
                """.formatted(x, reed, duration, Instant.now().minusSeconds(20));
    }
}
