package com.todongsan.battle_service.battle.scheduler;

import com.todongsan.battle_service.battle.service.BattleService;
import com.todongsan.battle_service.client.InsightClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BattleCloseScheduler {

    private final BattleService battleService;
    private final InsightClient insightClient;

    @Scheduled(fixedDelay = 60000)
    public void closeExpiredBattles() {
        List<Long> closedIds = battleService.closeExpiredBattles();
        for (Long battleId : closedIds) {
            insightClient.triggerAiReport(battleId);
        }
    }
}
