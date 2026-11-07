package com.smartparcelguard.alert;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    boolean existsByParcelIdAndDedupeKey(Long parcelId, String dedupeKey);

    boolean existsByParcelIdAndAlertTypeAndStatusIn(Long parcelId, AlertType alertType, List<AlertStatus> statuses);

    Optional<Alert> findByIdAndParcelId(Long id, Long parcelId);

    @Query("""
            SELECT a FROM Alert a
            WHERE (:operatorId IS NULL OR a.parcel.operator.id = :operatorId)
              AND (:status IS NULL OR a.status = :status)
              AND (:unreadOnly = FALSE OR a.readAt IS NULL)
            ORDER BY a.triggeredAt DESC
            """)
    List<Alert> search(@Param("operatorId") Long operatorId,
                       @Param("status") AlertStatus status,
                       @Param("unreadOnly") boolean unreadOnly,
                       Pageable pageable);
}
