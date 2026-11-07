package com.smartparcelguard.telemetry;

import com.smartparcelguard.risk.SecuritySettingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ThresholdSettings {

    static final String IMPACT_G = "impact.threshold_g";
    static final String VIBRATION_G = "vibration.threshold_g";
    static final String IMPACT_DURATION_MS = "impact.min_duration_ms";
    static final String OFFLINE_SECONDS = "device.offline_after_seconds";
    static final String DEDUPE_SECONDS = "risk.dedupe_window_seconds";

    private final SecuritySettingRepository settingRepository;

    public ThresholdSettings(SecuritySettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public BigDecimal impactThresholdG() {
        return decimal(IMPACT_G, "2.5");
    }

    public BigDecimal vibrationThresholdG() {
        return decimal(VIBRATION_G, "1.3");
    }

    public int minimumImpactDurationMs() {
        return integer(IMPACT_DURATION_MS, 50);
    }

    public int offlineAfterSeconds() {
        return integer(OFFLINE_SECONDS, 180);
    }

    public int dedupeWindowSeconds() {
        return integer(DEDUPE_SECONDS, 300);
    }

    private BigDecimal decimal(String key, String fallback) {
        return settingRepository.findBySettingKey(key)
                .map(setting -> {
                    try {
                        return new BigDecimal(setting.getSettingValue().trim());
                    } catch (NumberFormatException exception) {
                        return new BigDecimal(fallback);
                    }
                })
                .orElse(new BigDecimal(fallback));
    }

    private int integer(String key, int fallback) {
        return settingRepository.findBySettingKey(key)
                .map(setting -> {
                    try {
                        return Integer.parseInt(setting.getSettingValue().trim());
                    } catch (NumberFormatException exception) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }
}
