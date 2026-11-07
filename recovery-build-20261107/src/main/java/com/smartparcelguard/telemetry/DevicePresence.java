package com.smartparcelguard.telemetry;

import com.smartparcelguard.device.Device;
import com.smartparcelguard.device.DeviceStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DevicePresence {

    private final ThresholdSettings thresholds;

    public DevicePresence(ThresholdSettings thresholds) {
        this.thresholds = thresholds;
    }

    public boolean isOnline(Device device) {
        return DevicePresenceRules.online(
                device.getLastSeenAt(),
                device.isEnabled(),
                Instant.now(),
                thresholds.offlineAfterSeconds());
    }

    public int offlineAfterSeconds() {
        return thresholds.offlineAfterSeconds();
    }

    public void markOfflineIfStale(Device device) {
        boolean fresh = isOnline(device);
        DeviceStatus corrected = DevicePresenceRules.statusAfterTimeout(device.getStatus(), fresh);
        if (corrected != device.getStatus()) {
            device.setStatus(corrected);
        }
    }
}
