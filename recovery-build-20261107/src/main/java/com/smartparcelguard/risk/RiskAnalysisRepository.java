package com.smartparcelguard.risk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiskAnalysisRepository extends JpaRepository<RiskAnalysis, Long> {

    Optional<RiskAnalysis> findFirstByParcelIdOrderByCalculatedAtDesc(Long parcelId);

    List<RiskAnalysis> findByParcelIdOrderByCalculatedAtAsc(Long parcelId);
}
