package com.todongsan.battle_service.vote.service;

import com.todongsan.battle_service.vote.dto.request.VoteRequest;
import com.todongsan.battle_service.vote.dto.response.*;
import org.springframework.data.domain.Page;

public interface VoteService {

    VoteResponse vote(Long battleId, Long memberId, VoteRequest request);

    VoteResultResponse getResult(Long battleId, Long memberId);

    Page<MyVoteBattleResponse> getMyVotedBattles(Long memberId, String status, int page, int size);

    CrossAnalysisResponse getCrossResult(Long battleId);

    CertifiedResultResponse getCertifiedResult(Long battleId);

    VoteRawResponse getRawVotes(Long battleId);
}
