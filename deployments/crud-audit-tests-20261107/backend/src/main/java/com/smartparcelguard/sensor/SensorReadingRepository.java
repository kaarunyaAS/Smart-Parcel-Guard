package com.smartparcelguard.sensor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    List<SensorReading> findByParcelIdOrderByRecordedAtDesc(Long parcelId);

    List<SensorReading> findByParcelIdAndRecordedAtAfterOrderByRecordedAtAsc(Long parcelId, Instant recordedAt);
}
