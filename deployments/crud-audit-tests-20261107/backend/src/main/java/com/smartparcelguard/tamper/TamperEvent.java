package com.smartparcelguard.tamper;

import com.smartparcelguard.alert.AlertSeverity;
import com.smartparcelguard.alert.SecurityEvent;
import com.smartparcelguard.common.BaseEntity;
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
@Table(name = "tamper_events", indexes = {
        @Index(name = "idx_tamper_events_parcel_time", columnList = "parcel_id, recorded_at")
})
public class TamperEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parcel_id", nullable = false)
    private Parcel parcel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private TamperEventType eventType;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @Column(name = "during_maintenance", nullable = false)
    private boolean duringMaintenance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "security_event_id")
    private SecurityEvent securityEvent;

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

    public TamperEventType getEventType() {
        return eventType;
    }

    public void setEventType(TamperEventType eventType) {
        this.eventType = eventType;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }

    public boolean isDuringMaintenance() {
        return duringMaintenance;
    }

    public void setDuringMaintenance(boolean duringMaintenance) {
        this.duringMaintenance = duringMaintenance;
    }

    public SecurityEvent getSecurityEvent() {
        return securityEvent;
    }

    public void setSecurityEvent(SecurityEvent securityEvent) {
        this.securityEvent = securityEvent;
    }
}
