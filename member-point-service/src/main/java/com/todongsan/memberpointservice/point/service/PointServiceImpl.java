package com.todongsan.memberpointservice.point.service;

import com.todongsan.memberpointservice.global.exception.CustomException;
import com.todongsan.memberpointservice.global.exception.ErrorCode;
import com.todongsan.memberpointservice.member.entity.Member;
import com.todongsan.memberpointservice.member.repository.MemberRepository;
import com.todongsan.memberpointservice.point.dto.response.PointBalanceResponse;
import com.todongsan.memberpointservice.point.dto.response.PointHistoryPageResponse;
import com.todongsan.memberpointservice.point.dto.response.PointHistoryResponse;
import com.todongsan.memberpointservice.point.entity.PointTransactionStatus;
import com.todongsan.memberpointservice.point.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointServiceImpl implements PointService {

    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Override
    public PointBalanceResponse getBalance(Long memberId) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        return new PointBalanceResponse(member);
    }

    @Override
    public PointHistoryPageResponse getHistory(Long memberId, String type, int page, int size) {
        memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String typePrefix = resolveTypePrefix(type);

        var historyPage = (typePrefix == null)
                ? pointHistoryRepository.findByMemberIdAndStatus(memberId, PointTransactionStatus.SUCCEEDED, pageable)
                : pointHistoryRepository.findByMemberIdAndTypeStartingWithAndStatusSucceeded(memberId, typePrefix, pageable);

        return new PointHistoryPageResponse(historyPage.map(PointHistoryResponse::new));
    }

    private String resolveTypePrefix(String type) {
        if (type == null) return null;
        return switch (type.toUpperCase()) {
            case "EARN"   -> "EARN_";
            case "SPEND"  -> "SPEND_";
            case "SETTLE" -> "SETTLE_";
            case "REFUND" -> "REFUND_";
            default -> throw new CustomException(ErrorCode.VALIDATION_FAILED);
        };
    }
}