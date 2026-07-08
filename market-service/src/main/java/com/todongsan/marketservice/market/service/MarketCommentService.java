package com.todongsan.marketservice.market.service;

import com.todongsan.marketservice.global.exception.CustomException;
import com.todongsan.marketservice.global.exception.errorcode.CommonErrorCode;
import com.todongsan.marketservice.global.exception.errorcode.MarketErrorCode;
import com.todongsan.marketservice.market.dto.request.MarketCommentCreateRequest;
import com.todongsan.marketservice.market.dto.response.MarketCommentDeleteResponse;
import com.todongsan.marketservice.market.dto.response.MarketCommentPageResponse;
import com.todongsan.marketservice.market.dto.response.MarketCommentResponse;
import com.todongsan.marketservice.market.entity.Market;
import com.todongsan.marketservice.market.entity.MarketComment;
import com.todongsan.marketservice.market.repository.MarketCommentMapper;
import com.todongsan.marketservice.market.repository.MarketMapper;
import com.todongsan.marketservice.market.type.MarketStatus;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketCommentService {

    private static final int MAX_CONTENT_LENGTH = 500;
    private static final Set<MarketStatus> COMMENT_ALLOWED_STATUSES = EnumSet.of(
            MarketStatus.ACTIVE,
            MarketStatus.CLOSED,
            MarketStatus.DATA_PENDING,
            MarketStatus.SETTLEMENT_IN_PROGRESS,
            MarketStatus.SETTLED
    );

    private final MarketMapper marketMapper;
    private final MarketCommentMapper marketCommentMapper;

    @Transactional
    public MarketCommentResponse createComment(
            long marketId,
            Long memberId,
            MarketCommentCreateRequest request
    ) {
        validateMemberId(memberId);
        Market market = findMarket(marketId);
        if (!COMMENT_ALLOWED_STATUSES.contains(market.getStatus())) {
            throw new CustomException(MarketErrorCode.MARKET_COMMENT_NOT_ALLOWED);
        }
        validateContent(request.content());

        LocalDateTime now = LocalDateTime.now();
        MarketComment comment = new MarketComment(
                null,
                marketId,
                memberId,
                request.content(),
                null,
                now,
                now
        );
        marketCommentMapper.insertMarketComment(comment);
        return toResponse(comment);
    }

    public MarketCommentPageResponse getComments(long marketId, int page, int size) {
        findMarket(marketId);
        int offset;
        try {
            offset = Math.multiplyExact(page, size);
        } catch (ArithmeticException exception) {
            throw new CustomException(CommonErrorCode.VALIDATION_FAILED);
        }

        long totalElements = marketCommentMapper.countMarketComments(marketId);
        List<MarketCommentResponse> content = marketCommentMapper
                .selectMarketCommentsPage(marketId, offset, size)
                .stream()
                .map(this::toResponse)
                .toList();
        int totalPages = totalPages(totalElements, size);

        return new MarketCommentPageResponse(
                content,
                page,
                size,
                totalElements,
                totalPages,
                isLast(page, size, totalElements)
        );
    }

    @Transactional
    public MarketCommentDeleteResponse deleteComment(long marketId, long commentId, Long memberId) {
        validateMemberId(memberId);
        findMarket(marketId);

        MarketComment comment = marketCommentMapper.selectMarketCommentByIdAndMarketId(commentId, marketId);
        if (comment == null) {
            throw new CustomException(MarketErrorCode.MARKET_COMMENT_NOT_FOUND);
        }
        if (!comment.getMemberId().equals(memberId)) {
            throw new CustomException(MarketErrorCode.MARKET_COMMENT_FORBIDDEN);
        }

        int updated = marketCommentMapper.softDeleteMarketComment(commentId, marketId, LocalDateTime.now());
        if (updated != 1) {
            throw new CustomException(MarketErrorCode.MARKET_COMMENT_NOT_FOUND);
        }
        return new MarketCommentDeleteResponse(commentId, true);
    }

    private Market findMarket(long marketId) {
        Market market = marketMapper.selectMarketById(marketId);
        if (market == null) {
            throw new CustomException(MarketErrorCode.MARKET_NOT_FOUND);
        }
        return market;
    }

    private void validateMemberId(Long memberId) {
        if (memberId == null) {
            throw new CustomException(CommonErrorCode.UNAUTHORIZED);
        }
        if (memberId <= 0) {
            throw new CustomException(CommonErrorCode.VALIDATION_FAILED);
        }
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new CustomException(CommonErrorCode.VALIDATION_FAILED);
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new CustomException(MarketErrorCode.MARKET_COMMENT_TOO_LONG);
        }
    }

    private MarketCommentResponse toResponse(MarketComment comment) {
        return new MarketCommentResponse(
                comment.getId(),
                comment.getMarketId(),
                comment.getMemberId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private int totalPages(long totalElements, int size) {
        return (int) ((totalElements + size - 1) / size);
    }

    private boolean isLast(int page, int size, long totalElements) {
        return (long) (page + 1) * size >= totalElements;
    }
}
