package com.smartparcelguard.alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    Optional<SecurityEvent> findByParcelIdAndDedupeKey(Long parcelId, String dedupeKey);

    List<SecurityEvent> findByParcelIdAndRecordedAtAfterOrderByRecordedAtAsc(Long parcelId, Instant recordedAt);
}
