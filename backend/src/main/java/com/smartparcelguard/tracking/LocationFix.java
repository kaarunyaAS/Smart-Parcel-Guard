package com.smartparcelguard.tracking;

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
@Table(name = "location_history", indexes = {
        @Index(name = "idx_location_history_parcel_time", columnList = "parcel_id, recorded_at")
})
public class LocationFix extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parcel_id", nullable = false)
    private Parcel parcel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "latitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "altitude_meters", precision = 8, scale = 2)
    private BigDecimal altitudeMeters;

    @Column(name = "accuracy_meters", precision = 8, scale = 2)
    private BigDecimal accuracyMeters;

    @Column(name = "speed_mps", precision = 8, scale = 2)
    private BigDecimal speedMps;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 20)
    private DataSource dataSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "gps_signal_status", nullable = false, length = 20)
    private GpsSignalStatus gpsSignalStatus;

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

    public BigDecimal getAltitudeMeters() {
        return altitudeMeters;
    }

    public void setAltitudeMeters(BigDecimal altitudeMeters) {
        this.altitudeMeters = altitudeMeters;
    }

    public BigDecimal getAccuracyMeters() {
        return accuracyMeters;
    }

    public void setAccuracyMeters(BigDecimal accuracyMeters) {
        this.accuracyMeters = accuracyMeters;
    }

    public BigDecimal getSpeedMps() {
        return speedMps;
    }

    public void setSpeedMps(BigDecimal speedMps) {
        this.speedMps = speedMps;
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

    public GpsSignalStatus getGpsSignalStatus() {
        return gpsSignalStatus;
    }

    public void setGpsSignalStatus(GpsSignalStatus gpsSignalStatus) {
        this.gpsSignalStatus = gpsSignalStatus;
    }
}
