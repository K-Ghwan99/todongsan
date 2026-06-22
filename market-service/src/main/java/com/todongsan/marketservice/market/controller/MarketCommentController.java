package com.todongsan.marketservice.market.controller;

import com.todongsan.marketservice.global.exception.CustomException;
import com.todongsan.marketservice.global.exception.errorcode.CommonErrorCode;
import com.todongsan.marketservice.global.response.ApiResponse;
import com.todongsan.marketservice.market.dto.request.MarketCommentCreateRequest;
import com.todongsan.marketservice.market.dto.response.MarketCommentDeleteResponse;
import com.todongsan.marketservice.market.dto.response.MarketCommentPageResponse;
import com.todongsan.marketservice.market.dto.response.MarketCommentResponse;
import com.todongsan.marketservice.market.service.MarketCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/markets/{marketId}/comments")
@Tag(name = "Market Comment API", description = "Market 상세의 공개 댓글 목록과 회원 댓글 작성·삭제 API")
@io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_FAILED / MARKET_COMMENT_TOO_LONG"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "UNAUTHORIZED"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "MARKET_COMMENT_FORBIDDEN"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MARKET_NOT_FOUND / MARKET_COMMENT_NOT_FOUND"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "MARKET_COMMENT_NOT_ALLOWED")
})
public class MarketCommentController {

    private final MarketCommentService marketCommentService;

    @PostMapping
    @Operation(summary = "Market 댓글 작성", description = "인증 회원이 작성 가능한 상태의 Market에 단일 depth 댓글을 작성한다.")
    public ResponseEntity<ApiResponse<MarketCommentResponse>> createComment(
            @Parameter(description = "Market ID", example = "1")
            @PathVariable @Min(1) long marketId,
            @Parameter(
                    name = "X-Member-Id",
                    description = "Gateway가 주입하는 회원 ID",
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "2"
            )
            @RequestHeader(value = "X-Member-Id", required = false) Long memberId,
            @Valid @RequestBody MarketCommentCreateRequest request
    ) {
        MarketCommentResponse response = marketCommentService.createComment(marketId, memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "Market 댓글 목록 조회", description = "삭제되지 않은 댓글을 created_at ASC, id ASC로 공개 조회한다.")
    public ApiResponse<MarketCommentPageResponse> getComments(
            @Parameter(description = "Market ID", example = "1")
            @PathVariable @Min(1) long marketId,
            @Parameter(description = "페이지 번호. 0부터 시작", example = "0", schema = @Schema(type = "integer"))
            @RequestParam(defaultValue = "0") String page,
            @Parameter(description = "페이지 크기. 1~100", example = "10", schema = @Schema(type = "integer"))
            @RequestParam(defaultValue = "10") String size
    ) {
        return ApiResponse.ok(marketCommentService.getComments(
                marketId,
                parseInt(page, 0, Integer.MAX_VALUE),
                parseInt(size, 1, 100)
        ));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Market 댓글 삭제", description = "작성자 본인의 댓글을 deleted_at 기반으로 soft delete한다.")
    public ApiResponse<MarketCommentDeleteResponse> deleteComment(
            @Parameter(description = "Market ID", example = "1")
            @PathVariable @Min(1) long marketId,
            @Parameter(description = "댓글 ID", example = "101")
            @PathVariable @Min(1) long commentId,
            @Parameter(
                    name = "X-Member-Id",
                    description = "Gateway가 주입하는 회원 ID",
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "2"
            )
            @RequestHeader(value = "X-Member-Id", required = false) Long memberId
    ) {
        return ApiResponse.ok(marketCommentService.deleteComment(marketId, commentId, memberId));
    }

    private int parseInt(String value, int min, int max) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) {
                throw new CustomException(CommonErrorCode.VALIDATION_FAILED);
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new CustomException(CommonErrorCode.VALIDATION_FAILED);
        }
    }
}
