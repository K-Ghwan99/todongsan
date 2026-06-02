package com.todongsan.insightreputation.global.client;

import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.global.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BattleClientTest {

    @InjectMocks
    BattleClient battleClient;

    @Mock
    RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(battleClient, "battleServiceBaseUrl", "http://test-battle-service");
    }

    @Test
    @DisplayName("댓글 조회 성공 - 정상 응답")
    void getComment_success() {
        // Given
        Long commentId = 15L;
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 28, 10, 0, 0);
        
        Map<String, Object> responseData = Map.of(
            "commentId", commentId,
            "battleId", 42L,
            "memberId", 678L,
            "createdAt", createdAt.toString()
        );
        
        ApiResponse<Map<String, Object>> mockResponse = ApiResponse.success(responseData);
        
        when(restTemplate.getForObject(anyString(), eq(ApiResponse.class)))
            .thenReturn(mockResponse);

        // When
        BattleCommentResponse response = battleClient.getComment(commentId);

        // Then
        assertThat(response.getCommentId()).isEqualTo(commentId);
        assertThat(response.getBattleId()).isEqualTo(42L);
        assertThat(response.getMemberId()).isEqualTo(678L);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        
        verify(restTemplate).getForObject(
            "http://test-battle-service/api/v1/battles/comments/15",
            ApiResponse.class
        );
    }

    @Test
    @DisplayName("댓글 조회 실패 - 404 Not Found")
    void getComment_notFound() {
        // Given
        Long commentId = 999L;
        
        when(restTemplate.getForObject(anyString(), eq(ApiResponse.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> battleClient.getComment(commentId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VISIT_CERT_COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글 조회 실패 - 서비스 연결 오류")
    void getComment_connectionError() {
        // Given
        Long commentId = 15L;
        
        when(restTemplate.getForObject(anyString(), eq(ApiResponse.class)))
            .thenThrow(new ResourceAccessException("Connection refused"));

        // When & Then
        assertThatThrownBy(() -> battleClient.getComment(commentId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("댓글 조회 실패 - 서비스 오류 (500)")
    void getComment_serverError() {
        // Given
        Long commentId = 15L;
        
        when(restTemplate.getForObject(anyString(), eq(ApiResponse.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        // When & Then
        assertThatThrownBy(() -> battleClient.getComment(commentId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXTERNAL_SERVICE_ERROR);
    }

    @Test
    @DisplayName("댓글 조회 실패 - 잘못된 응답 타입")
    void getComment_wrongResponseType() {
        // Given
        Long commentId = 15L;
        
        // ApiResponse는 성공이지만 data가 Map이 아닌 경우
        ApiResponse<Object> mockResponse = ApiResponse.<Object>builder()
            .success(true)
            .data("invalid data type")  // String 타입으로 잘못된 데이터
            .timestamp(LocalDateTime.now())
            .build();
        
        when(restTemplate.getForObject(anyString(), eq(ApiResponse.class)))
            .thenReturn(mockResponse);

        // When & Then
        assertThatThrownBy(() -> battleClient.getComment(commentId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE);
    }

    @Test
    @DisplayName("댓글 조회 실패 - 응답 data null")
    void getComment_nullData() {
        // Given
        Long commentId = 15L;
        
        // ApiResponse에서 data가 null인 경우를 직접 모킹
        ApiResponse<Object> mockResponse = mock(ApiResponse.class);
        when(mockResponse.isSuccess()).thenReturn(true);
        when(mockResponse.getData()).thenReturn(null);
        
        when(restTemplate.getForObject(anyString(), eq(ApiResponse.class)))
            .thenReturn(mockResponse);

        // When & Then
        assertThatThrownBy(() -> battleClient.getComment(commentId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VISIT_CERT_COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("Battle 조회 성공")
    void getBattle_success() {
        // Given
        Long battleId = 42L;
        
        Map<String, Object> responseData = Map.of(
            "battleId", battleId,
            "title", "성수 vs 연남",
            "sido", "서울",
            "sigu", "성동구",
            "status", "ACTIVE"
        );
        
        ApiResponse<Map<String, Object>> mockResponse = ApiResponse.success(responseData);
        
        when(restTemplate.getForObject(anyString(), eq(ApiResponse.class)))
            .thenReturn(mockResponse);

        // When
        BattleResponse response = battleClient.getBattle(battleId);

        // Then
        assertThat(response.getBattleId()).isEqualTo(battleId);
        assertThat(response.getTitle()).isEqualTo("성수 vs 연남");
        assertThat(response.getSido()).isEqualTo("서울");
        assertThat(response.getSigu()).isEqualTo("성동구");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        
        verify(restTemplate).getForObject(
            "http://test-battle-service/api/v1/battles/42",
            ApiResponse.class
        );
    }

    @Test
    @DisplayName("Battle 조회 실패 - 404 Not Found")
    void getBattle_notFound() {
        // Given
        Long battleId = 999L;
        
        when(restTemplate.getForObject(anyString(), eq(ApiResponse.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> battleClient.getBattle(battleId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }
}