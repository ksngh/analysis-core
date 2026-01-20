package com.analysiscore.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.analysiscore.domain.entity.RankingSnapshot;

public interface RankingSnapshotRepository extends JpaRepository<RankingSnapshot, Long> {
}
