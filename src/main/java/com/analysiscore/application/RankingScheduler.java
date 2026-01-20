package com.analysiscore.application;

import com.analysiscore.application.service.OliveYoungRankingService;
import com.analysiscore.model.SourceType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RankingScheduler {
    private final OliveYoungRankingService rankingService;

    public RankingScheduler(OliveYoungRankingService rankingService) {
        this.rankingService = rankingService;
    }

    @Scheduled(fixedDelayString = "600000", initialDelayString = "10000")
    public void collectOliveYoungKr() {
        rankingService.collect(SourceType.OLIVEYOUNG_KR);
    }
}
