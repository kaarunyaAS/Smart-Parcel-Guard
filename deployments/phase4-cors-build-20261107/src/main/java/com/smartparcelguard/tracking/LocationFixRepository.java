package com.smartparcelguard.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationFixRepository extends JpaRepository<LocationFix, Long> {

    List<LocationFix> findByParcelIdOrderByRecordedAtAsc(Long parcelId);

    Optional<LocationFix> findFirstByParcelIdOrderByRecordedAtDesc(Long parcelId);

    List<LocationFix> findByParcelIdAndRecordedAtAfterOrderByRecordedAtAsc(Long parcelId, java.time.Instant recordedAt);
}
