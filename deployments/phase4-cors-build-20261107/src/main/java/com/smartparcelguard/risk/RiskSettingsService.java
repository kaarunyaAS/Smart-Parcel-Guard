package com.smartparcelguard.risk;

import com.smartparcelguard.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RiskSettingsService {

    private final SecuritySettingRepository settingRepository;

    public RiskSettingsService(SecuritySettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Transactional(readOnly = true)
    public RiskSettings get() {
        Map<String, String> values = new LinkedHashMap<>();
        settingRepository.findAll().forEach(setting -> values.put(setting.getSettingKey(), setting.getSettingValue()));
        return new RiskSettings(
                decimal(values, "impact.threshold_g"),
                decimal(values, "vibration.threshold_g"),
                integer(values, "impact.min_duration_ms"),
                integer(values, "device.offline_after_seconds"),
                integer(values, "risk.dedupe_window_seconds"),
                integer(values, "risk.weight.strong_impact"),
                integer(values, "risk.weight.unauthorized_opening"),
                integer(values, "risk.weight.unusual_vibration"),
                integer(values, "risk.weight.route_deviation"),
                integer(values, "risk.weight.device_offline"),
                integer(values, "risk.weight.correlation_bonus"),
                integer(values, "risk.max_score"),
                integer(values, "risk.correlation_window_seconds"),
                integer(values, "risk.vibration.repeat_count"),
                integer(values, "risk.level.medium"),
                integer(values, "risk.level.high"),
                integer(values, "risk.level.critical"),
                decimal(values, "gps.anomaly.minimum_jump_meters"),
                decimal(values, "gps.anomaly.maximum_plausible_speed_mps")
        );
    }

    @Transactional
    public RiskSettings update(RiskSettings settings) {
        validateRelationships(settings);
        put("impact.threshold_g", settings.impactThresholdG(), "Acceleration threshold that counts as an impact, in g");
        put("vibration.threshold_g", settings.vibrationThresholdG(), "Acceleration threshold that counts as vibration, in g");
        put("impact.min_duration_ms", settings.impactMinDurationMs(), "Minimum impact duration in milliseconds");
        put("device.offline_after_seconds", settings.deviceOfflineAfterSeconds(), "Heartbeat age after which a device is offline");
        put("risk.dedupe_window_seconds", settings.riskDedupeWindowSeconds(), "Window that suppresses a repeated identical event");
        put("risk.weight.strong_impact", settings.riskStrongImpactPoints(), "Points for a strong impact");
        put("risk.weight.unauthorized_opening", settings.riskUnauthorizedOpeningPoints(), "Points for an unauthorized opening");
        put("risk.weight.unusual_vibration", settings.riskUnusualVibrationPoints(), "Points for repeated unusual vibration");
        put("risk.weight.route_deviation", settings.riskRouteDeviationPoints(), "Points for an anomalous GPS jump");
        put("risk.weight.device_offline", settings.riskDeviceOfflinePoints(), "Points when an assigned device is offline");
        put("risk.weight.correlation_bonus", settings.riskCorrelationBonusPoints(), "Extra points when different suspicious events correlate");
        put("risk.max_score", settings.riskMaxScore(), "Upper bound of the parcel risk score");
        put("risk.correlation_window_seconds", settings.riskCorrelationWindowSeconds(), "Window used to correlate different event types");
        put("risk.vibration.repeat_count", settings.riskVibrationRepeatCount(), "Vibration readings required inside the correlation window");
        put("risk.level.medium", settings.riskLevelMedium(), "Minimum score for MEDIUM risk");
        put("risk.level.high", settings.riskLevelHigh(), "Minimum score for HIGH risk");
        put("risk.level.critical", settings.riskLevelCritical(), "Minimum score for CRITICAL risk");
        put("gps.anomaly.minimum_jump_meters", settings.gpsAnomalyMinimumJumpMeters(), "Minimum GPS jump distance checked for anomalies");
        put("gps.anomaly.maximum_plausible_speed_mps", settings.gpsAnomalyMaximumPlausibleSpeedMps(), "Maximum plausible GPS speed in meters per second");
        return settings;
    }

    private void put(String key, Object value, String description) {
        SecuritySetting setting = settingRepository.findBySettingKey(key).orElseGet(() -> {
            SecuritySetting created = new SecuritySetting();
            created.setSettingKey(key);
            created.setDescription(description);
            return created;
        });
        setting.setSettingValue(value.toString());
        if (setting.getDescription() == null) {
            setting.setDescription(description);
        }
        settingRepository.save(setting);
    }

    private static void validateRelationships(RiskSettings settings) {
        if (settings.vibrationThresholdG().compareTo(settings.impactThresholdG()) > 0) {
            throw invalid("Vibration threshold cannot exceed the impact threshold");
        }
        if (!(settings.riskLevelMedium() < settings.riskLevelHigh()
                && settings.riskLevelHigh() < settings.riskLevelCritical()
                && settings.riskLevelCritical() <= settings.riskMaxScore())) {
            throw invalid("Risk thresholds must satisfy medium < high < critical <= maximum score");
        }
    }

    private static BigDecimal decimal(Map<String, String> values, String key) {
        try {
            return new BigDecimal(required(values, key));
        } catch (NumberFormatException exception) {
            throw invalid("Stored risk setting is not a valid decimal: " + key);
        }
    }

    private static int integer(Map<String, String> values, String key) {
        try {
            return Integer.parseInt(required(values, key));
        } catch (NumberFormatException exception) {
            throw invalid("Stored risk setting is not a valid integer: " + key);
        }
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw invalid("Required risk setting is missing: " + key);
        }
        return value.trim();
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
