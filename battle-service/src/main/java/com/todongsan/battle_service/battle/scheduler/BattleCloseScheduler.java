package com.todongsan.battle_service.battle.scheduler;

import com.todongsan.battle_service.battle.entity.Battle;
import com.todongsan.battle_service.battle.repository.BattleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BattleCloseScheduler {

    private final BattleRepository battleRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void closeExpiredBattles() {
        List<Battle> expired = battleRepository.findExpiredActiveBattles(LocalDateTime.now());

        for (Battle battle : expired) {
            String winningOption = determineWinner(battle);
            battle.close(winningOption);
            log.info("Battle [{}] closed. winner={}", battle.getId(), winningOption);
        }
    }

    private String determineWinner(Battle battle) {
        if (battle.getOptionACount() > battle.getOptionBCount()) return "A";
        if (battle.getOptionBCount() > battle.getOptionACount()) return "B";
        return "DRAW";
    }
}
