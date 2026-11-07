package com.smartparcelguard.telemetry;

import com.smartparcelguard.parcel.ParcelStatus;
import com.smartparcelguard.sensor.MovementStatus;
import com.smartparcelguard.sensor.ReedSwitch;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensorEventDetectorTest {

    private static final BigDecimal VIBRATION = new BigDecimal("1.3");
    private static final BigDecimal IMPACT = new BigDecimal("2.5");

    @Test
    void restingGravityIsNotDamage() {
        SensorAssessment assessment = SensorEventDetector.assess(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, null,
                ReedSwitch.CLOSED, ParcelStatus.IN_TRANSIT, VIBRATION, IMPACT, 50);

        assertEquals(MovementStatus.NORMAL, assessment.movementStatus());
        assertFalse(assessment.impactDetected());
        assertFalse(assessment.vibrationOnly());
        assertFalse(assessment.unauthorizedOpening());
    }

    @Test
    void vibrationBelowImpactThresholdIsNotDamage() {
        SensorAssessment assessment = SensorEventDetector.assess(
                new BigDecimal("1.5"), BigDecimal.ZERO, BigDecimal.ZERO, 500,
                ReedSwitch.CLOSED, ParcelStatus.IN_TRANSIT, VIBRATION, IMPACT, 50);

        assertEquals(MovementStatus.VIBRATION, assessment.movementStatus());
        assertTrue(assessment.vibrationOnly());
        assertFalse(assessment.impactDetected());
    }

    @Test
    void shortSpikeIsNotRecordedAsImpact() {
        SensorAssessment assessment = SensorEventDetector.assess(
                new BigDecimal("4.0"), BigDecimal.ZERO, BigDecimal.ZERO, 10,
                ReedSwitch.CLOSED, ParcelStatus.IN_TRANSIT, VIBRATION, IMPACT, 50);

        assertEquals(MovementStatus.VIBRATION, assessment.movementStatus());
        assertFalse(assessment.impactDetected());
    }

    @Test
    void sustainedHighAccelerationIsImpact() {
        SensorAssessment assessment = SensorEventDetector.assess(
                new BigDecimal("3.0"), BigDecimal.ZERO, BigDecimal.ZERO, 80,
                ReedSwitch.CLOSED, ParcelStatus.IN_TRANSIT, VIBRATION, IMPACT, 50);

        assertEquals(MovementStatus.IMPACT, assessment.movementStatus());
        assertTrue(assessment.impactDetected());
        assertFalse(assessment.vibrationOnly());
    }

    @Test
    void openingWhileInTransitIsUnauthorized() {
        SensorAssessment assessment = SensorEventDetector.assess(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, null,
                ReedSwitch.OPEN, ParcelStatus.IN_TRANSIT, VIBRATION, IMPACT, 50);

        assertTrue(assessment.unauthorizedOpening());
    }

    @Test
    void openingAfterDeliveryIsNotUnauthorized() {
        SensorAssessment delivered = SensorEventDetector.assess(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, null,
                ReedSwitch.OPEN, ParcelStatus.DELIVERED, VIBRATION, IMPACT, 50);
        SensorAssessment registered = SensorEventDetector.assess(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, null,
                ReedSwitch.OPEN, ParcelStatus.REGISTERED, VIBRATION, IMPACT, 50);

        assertFalse(delivered.unauthorizedOpening());
        assertFalse(registered.unauthorizedOpening());
    }
}
