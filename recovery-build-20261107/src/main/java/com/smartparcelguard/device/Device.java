package com.smartparcelguard.device;

import com.smartparcelguard.common.BaseEntity;
import com.smartparcelguard.common.ComponentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "devices")
public class Device extends BaseEntity {

    @Column(name = "device_code", nullable = false, unique = true, length = 64)
    private String deviceCode;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "firmware_version", length = 40)
    private String firmwareVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeviceStatus status = DeviceStatus.OFFLINE;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "battery_percent")
    private Integer batteryPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "gps_status", nullable = false, length = 20)
    private ComponentStatus gpsStatus = ComponentStatus.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_status", nullable = false, length = 20)
    private ComponentStatus sensorStatus = ComponentStatus.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(name = "camera_status", nullable = false, length = 20)
    private ComponentStatus cameraStatus = ComponentStatus.UNKNOWN;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Integer getBatteryPercent() {
        return batteryPercent;
    }

    public void setBatteryPercent(Integer batteryPercent) {
        this.batteryPercent = batteryPercent;
    }

    public ComponentStatus getGpsStatus() {
        return gpsStatus;
    }

    public void setGpsStatus(ComponentStatus gpsStatus) {
        this.gpsStatus = gpsStatus;
    }

    public ComponentStatus getSensorStatus() {
        return sensorStatus;
    }

    public void setSensorStatus(ComponentStatus sensorStatus) {
        this.sensorStatus = sensorStatus;
    }

    public ComponentStatus getCameraStatus() {
        return cameraStatus;
    }

    public void setCameraStatus(ComponentStatus cameraStatus) {
        this.cameraStatus = cameraStatus;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
