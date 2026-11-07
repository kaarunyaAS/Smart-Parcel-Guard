package com.smartparcelguard.telemetry;

import com.smartparcelguard.device.DeviceStatus;

import java.time.Instant;

public final class DevicePresenceRules {

    private DevicePresenceRules() {
    }

    public static boolean online(Instant lastSeenAt, boolean enabled, Instant now, int offlineAfterSeconds) {
        if (!enabled || lastSeenAt == null || offlineAfterSeconds < 1) {
            return false;
        }
        return !lastSeenAt.isBefore(now.minusSeconds(offlineAfterSeconds));
    }

    public static DeviceStatus statusAfterTimeout(DeviceStatus stored, boolean heartbeatFresh) {
        if (stored == DeviceStatus.MAINTENANCE) {
            return DeviceStatus.MAINTENANCE;
        }
        if (!heartbeatFresh && stored != DeviceStatus.OFFLINE) {
            return DeviceStatus.OFFLINE;
        }
        return stored;
    }
}
