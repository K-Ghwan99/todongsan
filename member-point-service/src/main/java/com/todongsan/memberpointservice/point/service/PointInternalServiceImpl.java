package com.todongsan.memberpointservice.point.service;

import com.todongsan.memberpointservice.global.exception.CustomException;
import com.todongsan.memberpointservice.global.exception.ErrorCode;
import com.todongsan.memberpointservice.global.util.RequestHashUtil;
import com.todongsan.memberpointservice.member.entity.Member;
import com.todongsan.memberpointservice.member.repository.MemberRepository;
import com.todongsan.memberpointservice.point.dto.request.EarnRequest;
import com.todongsan.memberpointservice.point.dto.response.EarnResponse;
import com.todongsan.memberpointservice.point.entity.PointHistory;
import com.todongsan.memberpointservice.point.entity.PointHistoryType;
import com.todongsan.memberpointservice.point.entity.PointReferenceType;
import com.todongsan.memberpointservice.point.entity.PointTransactionStatus;
import com.todongsan.memberpointservice.point.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointInternalServiceImpl implements PointInternalService {

    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Override
    @Transactional
    public PointResult<EarnResponse> earn(String idempotencyKey, EarnRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new CustomException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        Optional<PointHistory> existing = pointHistoryRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            String newHash = RequestHashUtil.compute(
                    request.getMemberId(), request.getType(),
                    request.getAmount(), request.getReferenceType(), request.getReferenceId());
            if (newHash.equals(existing.get().getRequestHash())) {
                return PointResult.alreadyProcessed(new EarnResponse(existing.get()));
            }
            throw new CustomException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.POINT_INVALID_AMOUNT);
        }

        PointReferenceType refType = parseReferenceType(request.getReferenceType());
        PointHistoryType histType = parseHistoryType(request.getType());

        memberRepository.findByIdAndDeletedAtIsNull(request.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        BigDecimal normalizedAmount = request.getAmount().setScale(2, RoundingMode.DOWN);
        memberRepository.earnPoint(request.getMemberId(), normalizedAmount);

        Member updated = memberRepository.findByIdAndDeletedAtIsNull(request.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        String requestHash = RequestHashUtil.compute(
                request.getMemberId(), request.getType(),
                request.getAmount(), request.getReferenceType(), request.getReferenceId());

        PointHistory history = PointHistory.builder()
                .memberId(request.getMemberId())
                .type(histType)
                .amount(normalizedAmount)
                .balanceSnapshot(updated.getPointBalance())
                .reason(request.getReason())
                .referenceType(refType)
                .referenceId(request.getReferenceId())
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .status(PointTransactionStatus.SUCCEEDED)
                .build();

        pointHistoryRepository.save(history);

        return PointResult.of(new EarnResponse(history));
    }

    private PointReferenceType parseReferenceType(String referenceType) {
        try {
            return PointReferenceType.valueOf(referenceType);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.POINT_INVALID_REFERENCE_TYPE);
        }
    }

    private PointHistoryType parseHistoryType(String type) {
        try {
            return PointHistoryType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.VALIDATION_FAILED);
        }
    }

}
