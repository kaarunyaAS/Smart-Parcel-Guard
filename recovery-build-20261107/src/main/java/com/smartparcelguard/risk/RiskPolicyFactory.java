package com.smartparcelguard.risk;

import com.smartparcelguard.telemetry.ThresholdSettings;
import org.springframework.stereotype.Service;

@Service
public class RiskPolicyFactory {

    private final ThresholdSettings thresholds;
    private final SecuritySettingRepository settings;

    public RiskPolicyFactory(ThresholdSettings thresholds, SecuritySettingRepository settings) {
        this.thresholds = thresholds;
        this.settings = settings;
    }

    public RiskPolicy current() {
        return new RiskPolicy(
                integer("risk.weight.strong_impact", 25),
                integer("risk.weight.unauthorized_opening", 50),
                integer("risk.weight.unusual_vibration", 10),
                integer("risk.weight.route_deviation", 20),
                integer("risk.weight.device_offline", 15),
                integer("risk.weight.correlation_bonus", 15),
                integer("risk.max_score", 100),
                thresholds.dedupeWindowSeconds(),
                integer("risk.correlation_window_seconds", 600),
                integer("risk.vibration.repeat_count", 5),
                integer("risk.level.medium", 25),
                integer("risk.level.high", 50),
                integer("risk.level.critical", 75),
                decimal("gps.anomaly.minimum_jump_meters", 100),
                decimal("gps.anomaly.maximum_plausible_speed_mps", 55)
        );
    }

    public int offlineSeconds() {
        return thresholds.offlineAfterSeconds();
    }

    private int integer(String key, int fallback) {
        return settings.findBySettingKey(key)
                .map(setting -> {
                    try {
                        return Integer.parseInt(setting.getSettingValue().trim());
                    } catch (NumberFormatException exception) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }

    private double decimal(String key, double fallback) {
        return settings.findBySettingKey(key)
                .map(setting -> {
                    try {
                        return Double.parseDouble(setting.getSettingValue().trim());
                    } catch (NumberFormatException exception) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }
}
