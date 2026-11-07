package com.smartparcelguard.tamper;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface TamperEventRepository extends JpaRepository<TamperEvent, Long> {

    boolean existsByParcelIdAndEventTypeAndRecordedAtAfter(Long parcelId, TamperEventType eventType, Instant recordedAt);
}
