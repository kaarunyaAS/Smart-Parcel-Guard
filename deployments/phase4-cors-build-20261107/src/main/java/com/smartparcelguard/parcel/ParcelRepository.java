package com.smartparcelguard.parcel;

import com.smartparcelguard.risk.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParcelRepository extends JpaRepository<Parcel, Long> {

    Optional<Parcel> findByParcelCode(String parcelCode);

    @Query("""
            SELECT p FROM Parcel p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:operatorId IS NULL OR p.operator.id = :operatorId)
              AND (
                    :query IS NULL
                    OR LOWER(p.parcelCode) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
                    OR LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
                    OR LOWER(p.senderName) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
                    OR LOWER(p.receiverName) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
                  )
            """)
    Page<Parcel> search(@Param("status") ParcelStatus status,
                        @Param("operatorId") Long operatorId,
                        @Param("query") String query,
                        Pageable pageable);

    @Query("""
            SELECT COUNT(p) FROM Parcel p
            WHERE (:operatorId IS NULL OR p.operator.id = :operatorId)
              AND (:status IS NULL OR p.status = :status)
            """)
    long countVisible(@Param("operatorId") Long operatorId, @Param("status") ParcelStatus status);

    @Query("""
            SELECT COUNT(p) FROM Parcel p
            WHERE p.latestRiskLevel IN :levels
              AND (:operatorId IS NULL OR p.operator.id = :operatorId)
            """)
    long countByRisk(@Param("operatorId") Long operatorId, @Param("levels") Collection<RiskLevel> levels);

    @Query("""
            SELECT p.latestRiskLevel, COUNT(p) FROM Parcel p
            WHERE (:operatorId IS NULL OR p.operator.id = :operatorId)
            GROUP BY p.latestRiskLevel
            """)
    List<Object[]> riskDistribution(@Param("operatorId") Long operatorId);
}
