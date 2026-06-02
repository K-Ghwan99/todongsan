package com.todongsan.battle_service.vote.service;

import com.todongsan.battle_service.vote.dto.request.VoteRequest;
import com.todongsan.battle_service.vote.dto.response.*;

public interface VoteService {

    VoteResponse vote(Long battleId, Long memberId, VoteRequest request);

    VoteResultResponse getResult(Long battleId, Long memberId);

    CrossAnalysisResponse getCrossResult(Long battleId, Long memberId, String idempotencyKey);

    CertifiedResultResponse getCertifiedResult(Long battleId, Long memberId, String idempotencyKey);

    VoteRawResponse getRawVotes(Long battleId);
}
