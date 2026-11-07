package com.smartparcelguard.parcel;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParcelStatusHistoryRepository extends JpaRepository<ParcelStatusHistory, Long> {

    List<ParcelStatusHistory> findByParcelIdOrderByChangedAtAsc(Long parcelId);

    @Query("""
            SELECT h FROM ParcelStatusHistory h
            WHERE (:operatorId IS NULL OR h.parcel.operator.id = :operatorId)
            ORDER BY h.changedAt DESC
            """)
    List<ParcelStatusHistory> recent(@Param("operatorId") Long operatorId, Pageable pageable);
}
