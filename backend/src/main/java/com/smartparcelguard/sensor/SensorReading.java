package com.smartparcelguard.sensor;

import com.smartparcelguard.common.BaseEntity;
import com.smartparcelguard.common.DataSource;
import com.smartparcelguard.device.Device;
import com.smartparcelguard.parcel.Parcel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "sensor_readings", indexes = {
        @Index(name = "idx_sensor_readings_parcel_time", columnList = "parcel_id, recorded_at")
})
public class SensorReading extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parcel_id", nullable = false)
    private Parcel parcel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "acceleration_x", nullable = false, precision = 10, scale = 4)
    private BigDecimal accelerationX;

    @Column(name = "acceleration_y", nullable = false, precision = 10, scale = 4)
    private BigDecimal accelerationY;

    @Column(name = "acceleration_z", nullable = false, precision = 10, scale = 4)
    private BigDecimal accelerationZ;

    @Column(name = "gyroscope_x", nullable = false, precision = 10, scale = 4)
    private BigDecimal gyroscopeX;

    @Column(name = "gyroscope_y", nullable = false, precision = 10, scale = 4)
    private BigDecimal gyroscopeY;

    @Column(name = "gyroscope_z", nullable = false, precision = 10, scale = 4)
    private BigDecimal gyroscopeZ;

    @Column(name = "magnitude", nullable = false, precision = 10, scale = 4)
    private BigDecimal magnitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "reed_switch", nullable = false, length = 20)
    private ReedSwitch reedSwitch = ReedSwitch.CLOSED;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_status", nullable = false, length = 40)
    private MovementStatus movementStatus;

    @Column(name = "impact_detected", nullable = false)
    private boolean impactDetected;

    @Column(name = "unauthorized_opening", nullable = false)
    private boolean unauthorizedOpening;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 20)
    private DataSource dataSource;

    public Parcel getParcel() {
        return parcel;
    }

    public void setParcel(Parcel parcel) {
        this.parcel = parcel;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public BigDecimal getAccelerationX() {
        return accelerationX;
    }

    public void setAccelerationX(BigDecimal accelerationX) {
        this.accelerationX = accelerationX;
    }

    public BigDecimal getAccelerationY() {
        return accelerationY;
    }

    public void setAccelerationY(BigDecimal accelerationY) {
        this.accelerationY = accelerationY;
    }

    public BigDecimal getAccelerationZ() {
        return accelerationZ;
    }

    public void setAccelerationZ(BigDecimal accelerationZ) {
        this.accelerationZ = accelerationZ;
    }

    public BigDecimal getGyroscopeX() {
        return gyroscopeX;
    }

    public void setGyroscopeX(BigDecimal gyroscopeX) {
        this.gyroscopeX = gyroscopeX;
    }

    public BigDecimal getGyroscopeY() {
        return gyroscopeY;
    }

    public void setGyroscopeY(BigDecimal gyroscopeY) {
        this.gyroscopeY = gyroscopeY;
    }

    public BigDecimal getGyroscopeZ() {
        return gyroscopeZ;
    }

    public void setGyroscopeZ(BigDecimal gyroscopeZ) {
        this.gyroscopeZ = gyroscopeZ;
    }

    public BigDecimal getMagnitude() {
        return magnitude;
    }

    public void setMagnitude(BigDecimal magnitude) {
        this.magnitude = magnitude;
    }

    public ReedSwitch getReedSwitch() {
        return reedSwitch;
    }

    public void setReedSwitch(ReedSwitch reedSwitch) {
        this.reedSwitch = reedSwitch;
    }

    public boolean isImpactDetected() {
        return impactDetected;
    }

    public void setImpactDetected(boolean impactDetected) {
        this.impactDetected = impactDetected;
    }

    public boolean isUnauthorizedOpening() {
        return unauthorizedOpening;
    }

    public void setUnauthorizedOpening(boolean unauthorizedOpening) {
        this.unauthorizedOpening = unauthorizedOpening;
    }

    public MovementStatus getMovementStatus() {
        return movementStatus;
    }

    public void setMovementStatus(MovementStatus movementStatus) {
        this.movementStatus = movementStatus;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
