package com.smartparcelguard.telemetry;

import com.smartparcelguard.device.DeviceStatus;
import com.smartparcelguard.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryRulesTest {

    @Test
    void heartbeatIsRequiredBeforeADeviceIsOnline() {
        Instant now = Instant.parse("2026-11-07T08:00:00Z");

        assertFalse(DevicePresenceRules.online(null, true, now, 180));
        assertFalse(DevicePresenceRules.online(now.minusSeconds(181), true, now, 180));
        assertFalse(DevicePresenceRules.online(now.minusSeconds(10), false, now, 180));
        assertTrue(DevicePresenceRules.online(now.minusSeconds(10), true, now, 180));
    }

    @Test
    void staleOnlineStatusBecomesOffline() {
        assertEquals(DeviceStatus.OFFLINE, DevicePresenceRules.statusAfterTimeout(DeviceStatus.ONLINE, false));
        assertEquals(DeviceStatus.ONLINE, DevicePresenceRules.statusAfterTimeout(DeviceStatus.ONLINE, true));
        assertEquals(DeviceStatus.MAINTENANCE, DevicePresenceRules.statusAfterTimeout(DeviceStatus.MAINTENANCE, false));
    }

    @Test
    void gpsRejectsImpossibleCoordinatesAndFutureTimestamps() {
        Instant now = Instant.parse("2026-11-07T08:00:00Z");

        assertThrows(ApiException.class, () -> GpsCoordinates.validate(
                new BigDecimal("95"), BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE, now, now));
        assertThrows(ApiException.class, () -> GpsCoordinates.validate(
                BigDecimal.ZERO, new BigDecimal("181"), BigDecimal.ONE, BigDecimal.ONE, now, now));
        assertThrows(ApiException.class, () -> GpsCoordinates.validate(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, new BigDecimal("-1"), now, now));
        assertThrows(ApiException.class, () -> GpsCoordinates.validate(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE, now.plusSeconds(600), now));
    }

    @Test
    void imageValidationAcceptsPngAndRejectsOtherBytes() {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        byte[] text = "not-an-image".getBytes();

        assertEquals("image/png", ImageEvidenceValidator.validate(png, "image/png").contentType());
        assertThrows(ApiException.class, () -> ImageEvidenceValidator.validate(text, "image/png"));
        assertThrows(ApiException.class, () -> ImageEvidenceValidator.validate(png, "image/jpeg"));
        assertThrows(ApiException.class, () -> ImageEvidenceValidator.validate(new byte[0], "image/png"));
    }
}
