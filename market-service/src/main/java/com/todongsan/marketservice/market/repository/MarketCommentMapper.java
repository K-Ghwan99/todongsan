package com.todongsan.marketservice.market.repository;

import com.todongsan.marketservice.market.entity.MarketComment;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface MarketCommentMapper {

    void insertMarketComment(MarketComment comment);

    MarketComment selectMarketCommentByIdAndMarketId(
            @Param("commentId") long commentId,
            @Param("marketId") long marketId
    );

    long countMarketComments(@Param("marketId") long marketId);

    List<MarketComment> selectMarketCommentsPage(
            @Param("marketId") long marketId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int softDeleteMarketComment(
            @Param("commentId") long commentId,
            @Param("marketId") long marketId,
            @Param("now") LocalDateTime now
    );
}
