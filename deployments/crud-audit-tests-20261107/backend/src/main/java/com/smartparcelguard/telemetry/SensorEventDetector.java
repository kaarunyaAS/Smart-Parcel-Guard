package com.smartparcelguard.telemetry;

import com.smartparcelguard.parcel.ParcelStatus;
import com.smartparcelguard.sensor.MovementStatus;
import com.smartparcelguard.sensor.ReedSwitch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.Set;

public final class SensorEventDetector {

    private static final Set<ParcelStatus> SEALED_IN_CUSTODY = EnumSet.of(
            ParcelStatus.READY_FOR_PICKUP,
            ParcelStatus.PICKED_UP,
            ParcelStatus.IN_TRANSIT,
            ParcelStatus.DELAYED,
            ParcelStatus.UNDER_INVESTIGATION,
            ParcelStatus.OUT_FOR_DELIVERY
    );

    private SensorEventDetector() {
    }

    public static BigDecimal magnitude(BigDecimal x, BigDecimal y, BigDecimal z) {
        double value = Math.sqrt(square(x) + square(y) + square(z));
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    public static SensorAssessment assess(BigDecimal accelerationX,
                                          BigDecimal accelerationY,
                                          BigDecimal accelerationZ,
                                          Integer durationMs,
                                          ReedSwitch reedSwitch,
                                          ParcelStatus parcelStatus,
                                          BigDecimal vibrationThresholdG,
                                          BigDecimal impactThresholdG,
                                          int minimumImpactDurationMs) {
        BigDecimal magnitude = magnitude(accelerationX, accelerationY, accelerationZ);
        boolean sustained = durationMs != null && durationMs >= minimumImpactDurationMs;
        boolean aboveImpact = magnitude.compareTo(impactThresholdG) >= 0;
        boolean aboveVibration = magnitude.compareTo(vibrationThresholdG) >= 0;
        boolean impactDetected = aboveImpact && sustained;
        MovementStatus movement = impactDetected
                ? MovementStatus.IMPACT
                : aboveVibration ? MovementStatus.VIBRATION : MovementStatus.NORMAL;
        boolean unauthorizedOpening = reedSwitch == ReedSwitch.OPEN && SEALED_IN_CUSTODY.contains(parcelStatus);
        return new SensorAssessment(magnitude, movement, impactDetected, movement == MovementStatus.VIBRATION, unauthorizedOpening);
    }

    private static double square(BigDecimal value) {
        double raw = value.doubleValue();
        return raw * raw;
    }
}
