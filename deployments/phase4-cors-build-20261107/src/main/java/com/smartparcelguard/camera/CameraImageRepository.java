package com.smartparcelguard.camera;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CameraImageRepository extends JpaRepository<CameraImage, Long> {

    List<CameraImage> findByParcelIdOrderByCapturedAtDesc(Long parcelId);

    Optional<CameraImage> findByIdAndParcelId(Long id, Long parcelId);
}
