package com.analysiscore.domain.repository;

import com.analysiscore.domain.entity.RankingItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingItemRepository extends JpaRepository<RankingItem, Long> {
}
