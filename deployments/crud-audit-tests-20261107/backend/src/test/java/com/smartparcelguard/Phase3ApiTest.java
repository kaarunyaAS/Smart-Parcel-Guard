package com.smartparcelguard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparcelguard.camera.CameraImageRepository;
import com.smartparcelguard.device.DeviceRepository;
import com.smartparcelguard.device.DeviceTokenHasher;
import com.smartparcelguard.parcel.Parcel;
import com.smartparcelguard.parcel.ParcelRepository;
import com.smartparcelguard.parcel.ParcelStatus;
import com.smartparcelguard.sensor.SensorReadingRepository;
import com.smartparcelguard.tamper.TamperEventRepository;
import com.smartparcelguard.tamper.TamperEventType;
import com.smartparcelguard.tracking.LocationFixRepository;
import com.smartparcelguard.user.User;
import com.smartparcelguard.user.UserRepository;
import com.smartparcelguard.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spg.storage.image-dir=${java.io.tmpdir}/spg-phase3-evidence")
@AutoConfigureMockMvc
@Transactional
class Phase3ApiTest {

    private static final byte[] PNG = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ParcelRepository parcelRepository;

    @Autowired
    private LocationFixRepository locationRepository;

    @Autowired
    private SensorReadingRepository sensorRepository;

    @Autowired
    private TamperEventRepository tamperRepository;

    @Autowired
    private CameraImageRepository imageRepository;

    @AfterEach
    void removeEvidenceFiles() throws Exception {
        Path root = Path.of(System.getProperty("java.io.tmpdir"), "spg-phase3-evidence");
        if (Files.exists(root)) {
            try (var paths = Files.walk(root)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                        // The test assertion already recorded whether the upload succeeded.
                    }
                });
            }
        }
    }

    @Test
    void storesGpsForTheAssignedParcelAndRejectsInvalidCoordinates() throws Exception {
        User admin = saveUser("admin", UserRole.ADMIN);
        User operator = saveUser("operator", UserRole.OPERATOR);
        User stranger = saveUser("stranger", UserRole.OPERATOR);
        String adminToken = login(admin.getUsername());
        long deviceId = registerDevice(adminToken);
        String deviceToken = lastDeviceToken;
        long parcelId = createParcel(adminToken, operator.getId(), deviceId);

        mockMvc.perform(post("/api/telemetry/gps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson("13.082680", "80.270718")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/telemetry/gps")
                        .header("X-Device-Token", "not-a-device-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson("13.082680", "80.270718")))
                .andExpect(status().isUnauthorized());

        MvcResult stored = mockMvc.perform(post("/api/telemetry/gps")
                        .header("X-Device-Token", deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson("13.082680", "80.270718")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dataSource").value("SIMULATED"))
                .andExpect(jsonPath("$.data.latLng[0]").value(13.082680))
                .andExpect(jsonPath("$.data.latLng[1]").value(80.270718))
                .andExpect(jsonPath("$.data.altitudeMeters").value(12.5))
                .andReturn();
        assertFalse(stored.getResponse().getContentAsString().contains(deviceToken));

        var fixes = locationRepository.findByParcelIdOrderByRecordedAtAsc(parcelId);
        assertEquals(1, fixes.size());
        assertEquals(parcelId, fixes.get(0).getParcel().getId());
        assertEquals(deviceId, fixes.get(0).getDevice().getId());
        assertEquals(0, fixes.get(0).getAltitudeMeters().compareTo(new BigDecimal("12.50")));
        assertEquals(DeviceTokenHasher.sha256(deviceToken), deviceRepository.findById(deviceId).orElseThrow().getTokenHash());

        mockMvc.perform(post("/api/telemetry/gps")
                        .header("X-Device-Token", deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gpsJson("95.000000", "80.270718")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/parcels/" + parcelId + "/locations/latest")
                        .header("Authorization", "Bearer " + login(operator.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parcelId").value(parcelId));

        mockMvc.perform(get("/api/parcels/" + parcelId + "/locations")
                        .header("Authorization", "Bearer " + login(stranger.getUsername())))
                .andExpect(status().isNotFound());
    }

    @Test
    void detectsImpactAndUnauthorizedOpeningWithoutTreatingVibrationAsDamage() throws Exception {
        User admin = saveUser("admin", UserRole.ADMIN);
        User operator = saveUser("operator", UserRole.OPERATOR);
        String adminToken = login(admin.getUsername());
        long deviceId = registerDevice(adminToken);
        String deviceToken = lastDeviceToken;
        long parcelId = createParcel(adminToken, operator.getId(), deviceId);
        Parcel parcel = parcelRepository.findById(parcelId).orElseThrow();
        parcel.setStatus(ParcelStatus.IN_TRANSIT);

        mockMvc.perform(post("/api/telemetry/sensors")
                        .header("X-Device-Token", deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sensorJson("1.5000", "0", "0", "CLOSED", 400)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.movementStatus").value("VIBRATION"))
                .andExpect(jsonPath("$.data.impactDetected").value(false))
                .andExpect(jsonPath("$.data.unauthorizedOpening").value(false));

        mockMvc.perform(post("/api/telemetry/sensors")
                        .header("X-Device-Token", deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sensorJson("3.0000", "0", "0", "CLOSED", 80)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.movementStatus").value("IMPACT"))
                .andExpect(jsonPath("$.data.impactDetected").value(true));

        mockMvc.perform(post("/api/telemetry/sensors")
                        .header("X-Device-Token", deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sensorJson("0", "0", "1.0000", "OPEN", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.unauthorizedOpening").value(true))
                .andExpect(jsonPath("$.data.impactDetected").value(false));

        var readings = sensorRepository.findByParcelIdOrderByRecordedAtDesc(parcelId);
        assertEquals(3, readings.size());
        assertTrue(readings.stream().anyMatch(reading -> reading.isImpactDetected() && reading.getParcel().getId().equals(parcelId)));
        assertTrue(readings.stream().anyMatch(reading -> reading.isUnauthorizedOpening() && reading.getDevice().getId().equals(deviceId)));
        assertEquals(1, tamperRepository.findAll().stream()
                .filter(event -> event.getParcel().getId().equals(parcelId) && event.getEventType() == TamperEventType.OPENED)
                .count());

        mockMvc.perform(get("/api/parcels/" + parcelId + "/sensors")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    void heartbeatSetsOnlineAndTimeoutReturnsOffline() throws Exception {
        User admin = saveUser("admin", UserRole.ADMIN);
        User operator = saveUser("operator", UserRole.OPERATOR);
        String adminToken = login(admin.getUsername());
        long deviceId = registerDevice(adminToken);
        String deviceToken = lastDeviceToken;
        long parcelId = createParcel(adminToken, operator.getId(), deviceId);

        mockMvc.perform(post("/api/telemetry/heartbeat")
                        .header("X-Device-Token", deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batteryPercent\":87,\"gpsStatus\":\"OK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.online").value(true))
                .andExpect(jsonPath("$.data.status").value("ONLINE"))
                .andExpect(jsonPath("$.data.parcelId").value(parcelId));

        var device = deviceRepository.findById(deviceId).orElseThrow();
        assertEquals(87, device.getBatteryPercent());
        assertTrue(device.getLastSeenAt() != null);

        device.setLastSeenAt(Instant.now().minusSeconds(600));
        deviceRepository.saveAndFlush(device);

        mockMvc.perform(get("/api/parcels/" + parcelId + "/device-status")
                        .header("Authorization", "Bearer " + login(operator.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.online").value(false))
                .andExpect(jsonPath("$.data.status").value("OFFLINE"));

        mockMvc.perform(put("/api/devices/" + deviceId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/telemetry/heartbeat")
                        .header("X-Device-Token", deviceToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void storesValidCameraEvidenceAndRejectsNonImages() throws Exception {
        User admin = saveUser("admin", UserRole.ADMIN);
        User operator = saveUser("operator", UserRole.OPERATOR);
        String adminToken = login(admin.getUsername());
        long deviceId = registerDevice(adminToken);
        String deviceToken = lastDeviceToken;
        long parcelId = createParcel(adminToken, operator.getId(), deviceId);

        mockMvc.perform(post("/api/devices/" + deviceId + "/unassign")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(multipart("/api/telemetry/evidence")
                        .file(new MockMultipartFile("file", "lid.png", "image/png", PNG))
                        .param("cameraRole", "INSIDE")
                        .param("dataSource", "SIMULATED")
                        .header("X-Device-Token", deviceToken))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/devices/" + deviceId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parcelId\":" + parcelId + "}"))
                .andExpect(status().isOk());

        MvcResult uploaded = mockMvc.perform(multipart("/api/telemetry/evidence")
                        .file(new MockMultipartFile("file", "lid.png", "image/png", PNG))
                        .param("cameraRole", "INSIDE")
                        .param("dataSource", "SIMULATED")
                        .header("X-Device-Token", deviceToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.cameraRole").value("INSIDE"))
                .andExpect(jsonPath("$.data.dataSource").value("SIMULATED"))
                .andExpect(jsonPath("$.data.checksumSha256").isNotEmpty())
                .andReturn();
        JsonNode evidence = objectMapper.readTree(uploaded.getResponse().getContentAsString()).path("data");
        long imageId = evidence.path("id").asLong();
        var stored = imageRepository.findById(imageId).orElseThrow();
        assertEquals(parcelId, stored.getParcel().getId());
        assertEquals(deviceId, stored.getDevice().getId());
        assertTrue(Files.isRegularFile(Path.of(System.getProperty("java.io.tmpdir"), "spg-phase3-evidence").resolve(stored.getFilePath())));
        assertFalse(uploaded.getResponse().getContentAsString().contains(deviceToken));

        mockMvc.perform(get("/api/parcels/" + parcelId + "/evidence/" + imageId + "/content")
                        .header("Authorization", "Bearer " + login(operator.getUsername())))
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals(PNG.length, result.getResponse().getContentAsByteArray().length));

        mockMvc.perform(multipart("/api/telemetry/evidence")
                        .file(new MockMultipartFile("file", "notes.png", "image/png", "hello".getBytes()))
                        .param("cameraRole", "OUTSIDE")
                        .param("dataSource", "SIMULATED")
                        .header("X-Device-Token", deviceToken))
                .andExpect(status().isBadRequest());
    }

    private String lastDeviceToken;

    private long registerDevice(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/devices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firmwareVersion\":\"1.0.0\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        lastDeviceToken = data.path("deviceToken").asText();
        return data.path("id").asLong();
    }

    private long createParcel(String adminToken, long operatorId, long deviceId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/parcels")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Telemetry parcel",
                                  "category":"Documents",
                                  "declaredValue":100.00,
                                  "senderName":"Sender",
                                  "receiverName":"Receiver",
                                  "pickupAddress":"Chennai",
                                  "destinationAddress":"Madurai",
                                  "priority":"HIGH",
                                  "operatorId":%d,
                                  "deviceId":%d
                                }
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

    private static String gpsJson(String latitude, String longitude) {
        return """
                {
                  "latitude":%s,
                  "longitude":%s,
                  "altitudeMeters":12.5,
                  "speedMps":4.2,
                  "recordedAt":"%s",
                  "dataSource":"SIMULATED",
                  "gpsSignalStatus":"GOOD"
                }
                """.formatted(latitude, longitude, Instant.now().minusSeconds(30));
    }

    private static String sensorJson(String x, String y, String z, String reed, Integer duration) {
        String durationField = duration == null ? "" : ",\"durationMs\":" + duration;
        return """
                {
                  "accelerationX":%s,
                  "accelerationY":%s,
                  "accelerationZ":%s,
                  "gyroscopeX":0,
                  "gyroscopeY":0,
                  "gyroscopeZ":0,
                  "reedSwitch":"%s",
                  "recordedAt":"%s",
                  "dataSource":"SIMULATED"%s
                }
                """.formatted(x, y, z, reed, Instant.now().minusSeconds(30), durationField);
    }
}
