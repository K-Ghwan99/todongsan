package com.todongsan.marketservice.market.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record MarketCommentCreateRequest(
        @Schema(description = "댓글 본문. 공백 불가, 최대 500자", example = "이 Market 결과가 궁금합니다.")
        @NotBlank String content
) {
}
