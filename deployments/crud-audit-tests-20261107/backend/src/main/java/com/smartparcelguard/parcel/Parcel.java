package com.smartparcelguard.parcel;

import com.smartparcelguard.common.BaseEntity;
import com.smartparcelguard.risk.RiskLevel;
import com.smartparcelguard.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "parcels")
public class Parcel extends BaseEntity {

    @Column(name = "parcel_code", nullable = false, unique = true, length = 40)
    private String parcelCode;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "category", nullable = false, length = 80)
    private String category;

    @Column(name = "declared_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal declaredValue;

    @Column(name = "sender_name", nullable = false, length = 160)
    private String senderName;

    @Column(name = "sender_phone", length = 30)
    private String senderPhone;

    @Column(name = "sender_address", length = 500)
    private String senderAddress;

    @Column(name = "receiver_name", nullable = false, length = 160)
    private String receiverName;

    @Column(name = "receiver_phone", length = 30)
    private String receiverPhone;

    @Column(name = "receiver_address", length = 500)
    private String receiverAddress;

    @Column(name = "pickup_address", nullable = false, length = 500)
    private String pickupAddress;

    @Column(name = "pickup_latitude", precision = 9, scale = 6)
    private BigDecimal pickupLatitude;

    @Column(name = "pickup_longitude", precision = 9, scale = 6)
    private BigDecimal pickupLongitude;

    @Column(name = "destination_address", nullable = false, length = 500)
    private String destinationAddress;

    @Column(name = "destination_latitude", precision = 9, scale = 6)
    private BigDecimal destinationLatitude;

    @Column(name = "destination_longitude", precision = 9, scale = 6)
    private BigDecimal destinationLongitude;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private ParcelPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ParcelStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "monitoring_state", nullable = false, length = 20)
    private MonitoringState monitoringState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private User operator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(name = "latest_latitude", precision = 9, scale = 6)
    private BigDecimal latestLatitude;

    @Column(name = "latest_longitude", precision = 9, scale = 6)
    private BigDecimal latestLongitude;

    @Column(name = "latest_speed_mps", precision = 8, scale = 2)
    private BigDecimal latestSpeedMps;

    @Column(name = "latest_location_at")
    private Instant latestLocationAt;

    @Column(name = "latest_risk_score")
    private Integer latestRiskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "latest_risk_level", length = 20)
    private RiskLevel latestRiskLevel;

    public String getParcelCode() {
        return parcelCode;
    }

    public void setParcelCode(String parcelCode) {
        this.parcelCode = parcelCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getDeclaredValue() {
        return declaredValue;
    }

    public void setDeclaredValue(BigDecimal declaredValue) {
        this.declaredValue = declaredValue;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public void setSenderPhone(String senderPhone) {
        this.senderPhone = senderPhone;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public BigDecimal getPickupLatitude() {
        return pickupLatitude;
    }

    public void setPickupLatitude(BigDecimal pickupLatitude) {
        this.pickupLatitude = pickupLatitude;
    }

    public BigDecimal getPickupLongitude() {
        return pickupLongitude;
    }

    public void setPickupLongitude(BigDecimal pickupLongitude) {
        this.pickupLongitude = pickupLongitude;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public BigDecimal getDestinationLatitude() {
        return destinationLatitude;
    }

    public void setDestinationLatitude(BigDecimal destinationLatitude) {
        this.destinationLatitude = destinationLatitude;
    }

    public BigDecimal getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(BigDecimal destinationLongitude) {
        this.destinationLongitude = destinationLongitude;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public ParcelPriority getPriority() {
        return priority;
    }

    public void setPriority(ParcelPriority priority) {
        this.priority = priority;
    }

    public ParcelStatus getStatus() {
        return status;
    }

    public void setStatus(ParcelStatus status) {
        this.status = status;
    }

    public MonitoringState getMonitoringState() {
        return monitoringState;
    }

    public void setMonitoringState(MonitoringState monitoringState) {
        this.monitoringState = monitoringState;
    }

    public User getOperator() {
        return operator;
    }

    public void setOperator(User operator) {
        this.operator = operator;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public BigDecimal getLatestLatitude() {
        return latestLatitude;
    }

    public void setLatestLatitude(BigDecimal latestLatitude) {
        this.latestLatitude = latestLatitude;
    }

    public BigDecimal getLatestLongitude() {
        return latestLongitude;
    }

    public void setLatestLongitude(BigDecimal latestLongitude) {
        this.latestLongitude = latestLongitude;
    }

    public BigDecimal getLatestSpeedMps() {
        return latestSpeedMps;
    }

    public void setLatestSpeedMps(BigDecimal latestSpeedMps) {
        this.latestSpeedMps = latestSpeedMps;
    }

    public Instant getLatestLocationAt() {
        return latestLocationAt;
    }

    public void setLatestLocationAt(Instant latestLocationAt) {
        this.latestLocationAt = latestLocationAt;
    }

    public Integer getLatestRiskScore() {
        return latestRiskScore;
    }

    public void setLatestRiskScore(Integer latestRiskScore) {
        this.latestRiskScore = latestRiskScore;
    }

    public RiskLevel getLatestRiskLevel() {
        return latestRiskLevel;
    }

    public void setLatestRiskLevel(RiskLevel latestRiskLevel) {
        this.latestRiskLevel = latestRiskLevel;
    }
}
