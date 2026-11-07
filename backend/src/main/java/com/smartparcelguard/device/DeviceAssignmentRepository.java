package com.smartparcelguard.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceAssignmentRepository extends JpaRepository<DeviceAssignment, Long> {

    Optional<DeviceAssignment> findByActiveDeviceKey(Long activeDeviceKey);

    List<DeviceAssignment> findByParcelIdAndActiveDeviceKeyIsNotNull(Long parcelId);

    @Query("""
            SELECT a FROM DeviceAssignment a
            WHERE a.activeDeviceKey IS NOT NULL
              AND (:operatorId IS NULL OR a.parcel.operator.id = :operatorId)
            """)
    List<DeviceAssignment> findActive(@Param("operatorId") Long operatorId);
}
