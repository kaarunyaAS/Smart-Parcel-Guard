package com.smartparcelguard.parcel;

import com.smartparcelguard.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "geofences")
public class Geofence extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parcel_id", nullable = false)
    private Parcel parcel;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "geofence_type", nullable = false, length = 30)
    private GeofenceType geofenceType;

    @Lob
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "boundary_json", nullable = false)
    private String boundaryJson;

    @Column(name = "deviation_tolerance_meters", precision = 8, scale = 2)
    private BigDecimal deviationToleranceMeters;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Parcel getParcel() {
        return parcel;
    }

    public void setParcel(Parcel parcel) {
        this.parcel = parcel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GeofenceType getGeofenceType() {
        return geofenceType;
    }

    public void setGeofenceType(GeofenceType geofenceType) {
        this.geofenceType = geofenceType;
    }

    public String getBoundaryJson() {
        return boundaryJson;
    }

    public void setBoundaryJson(String boundaryJson) {
        this.boundaryJson = boundaryJson;
    }

    public BigDecimal getDeviationToleranceMeters() {
        return deviationToleranceMeters;
    }

    public void setDeviationToleranceMeters(BigDecimal deviationToleranceMeters) {
        this.deviationToleranceMeters = deviationToleranceMeters;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
