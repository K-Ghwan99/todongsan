package com.todongsan.insightreputation.integration;

import com.todongsan.insightreputation.reputation.entity.Reputation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InternalApiIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("투표 3회 누적 → activityConfirmed 전환")
    void internalApi_activity_threeVotes_confirmed() {
        // given - 먼저 거주지 선언을 통해 Reputation 생성 (실제 비즈니스 플로우)
        Long memberId = 1L;
        Map<String, String> residenceRequest = Map.of("sido", "서울", "sigu", "성동구");
        
        restTemplate.exchange(
                "/api/v1/reputations/me/residence",
                HttpMethod.PUT,
                new HttpEntity<>(residenceRequest, createMemberHeaders(memberId)),
                Map.class
        );

        Map<String, Object> voteRequest = Map.of(
                "memberId", 1L,
                "activityType", "VOTE",
                "region", Map.of(
                        "sido", "서울",
                        "sigu", "성동구"
                )
        );

        // when - 1차 투표
        ResponseEntity<Map> response1 = restTemplate.exchange(
                "/api/v1/reputations/activity",
                HttpMethod.POST,
                new HttpEntity<>(voteRequest, createHeaders()),
                Map.class
        );

        // then - 1차 투표 후
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Boolean) response1.getBody().get("success")).isTrue();
        
        Map<String, Object> data1 = (Map<String, Object>) response1.getBody().get("data");
        assertThat(data1.get("activityCount")).isEqualTo(1);
        assertThat(data1.get("activityScore")).isEqualTo(10);
        assertThat(data1.get("activityConfirmed")).isEqualTo(false);

        // when - 2차 투표
        ResponseEntity<Map> response2 = restTemplate.exchange(
                "/api/v1/reputations/activity",
                HttpMethod.POST,
                new HttpEntity<>(voteRequest, createHeaders()),
                Map.class
        );

        // then - 2차 투표 후
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data2 = (Map<String, Object>) response2.getBody().get("data");
        assertThat(data2.get("activityCount")).isEqualTo(2);
        assertThat(data2.get("activityScore")).isEqualTo(20);
        assertThat(data2.get("activityConfirmed")).isEqualTo(false);

        // when - 3차 투표 (confirmed 전환)
        ResponseEntity<Map> response3 = restTemplate.exchange(
                "/api/v1/reputations/activity",
                HttpMethod.POST,
                new HttpEntity<>(voteRequest, createHeaders()),
                Map.class
        );

        // then - 3차 투표 후 confirmed 전환
        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data3 = (Map<String, Object>) response3.getBody().get("data");
        assertThat(data3.get("activityCount")).isEqualTo(3);
        assertThat(data3.get("activityScore")).isEqualTo(30);
        assertThat(data3.get("activityConfirmed")).isEqualTo(true); // confirmed 전환

        // DB에서도 확인
        Reputation updatedReputation = reputationRepository.findByMemberId(1L).orElseThrow();
        assertThat(updatedReputation.getActivityCount()).isEqualTo(3);
        assertThat(updatedReputation.getActivityConfirmedAt()).isNotNull();

        // when - 4차 투표 (더 이상 count 증가하지 않음)
        ResponseEntity<Map> response4 = restTemplate.exchange(
                "/api/v1/reputations/activity",
                HttpMethod.POST,
                new HttpEntity<>(voteRequest, createHeaders()),
                Map.class
        );

        // then - 4차 투표 후 count 변경 없음
        assertThat(response4.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data4 = (Map<String, Object>) response4.getBody().get("data");
        assertThat(data4.get("activityCount")).isEqualTo(3); // 변경 없음
        assertThat(data4.get("activityScore")).isEqualTo(40); // 점수는 증가
        assertThat(data4.get("activityConfirmed")).isEqualTo(true);
    }

    @Test
    @DisplayName("거주지 불일치 활동 → activityScore만 증가, activityCount 유지")
    void internalApi_activity_regionMismatch_noCount() {
        // given - 먼저 거주지 선언을 통해 Reputation 생성
        Long memberId = 2L;
        Map<String, String> residenceRequest = Map.of("sido", "서울", "sigu", "성동구");
        
        restTemplate.exchange(
                "/api/v1/reputations/me/residence",
                HttpMethod.PUT,
                new HttpEntity<>(residenceRequest, createMemberHeaders(memberId)),
                Map.class
        );

        // 거주지와 다른 지역에서 활동
        Map<String, Object> mismatchRequest = Map.of(
                "memberId", 2L,
                "activityType", "VOTE",
                "region", Map.of(
                        "sido", "서울",
                        "sigu", "마포구" // 거주지와 다름
                )
        );

        // when
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/reputations/activity",
                HttpMethod.POST,
                new HttpEntity<>(mismatchRequest, createHeaders()),
                Map.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Boolean) response.getBody().get("success")).isTrue();
        
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("activityCount")).isEqualTo(0); // 변경 없음
        assertThat(data.get("activityScore")).isEqualTo(10); // 점수는 증가
        assertThat(data.get("activityConfirmed")).isEqualTo(false);

        // DB에서도 확인
        Reputation updatedReputation = reputationRepository.findByMemberId(2L).orElseThrow();
        assertThat(updatedReputation.getActivityCount()).isEqualTo(0);
        assertThat(updatedReputation.getActivityScore()).isEqualTo(10);
        assertThat(updatedReputation.getActivityConfirmedAt()).isNull();
    }

    @Test
    @DisplayName("예측 정확도 소수점 버림 확인")
    void internalApi_prediction_accuracyFloor() {
        // given - 먼저 거주지 선언을 통해 Reputation 생성
        Long memberId = 3L;
        Map<String, String> residenceRequest = Map.of("sido", "서울", "sigu", "성동구");
        
        restTemplate.exchange(
                "/api/v1/reputations/me/residence",
                HttpMethod.PUT,
                new HttpEntity<>(residenceRequest, createMemberHeaders(memberId)),
                Map.class
        );
        
        // 기존 예측 기록 설정 - 먼저 내부 API로 예측 업데이트를 여러 번 호출
        for (int i = 0; i < 10; i++) {
            Map<String, Object> predictionRequest = Map.of(
                    "memberId", memberId,
                    "marketId", 90L + i,
                    "isCorrect", i < 7  // 처음 7개만 정답
            );
            
            restTemplate.exchange(
                    "/api/v1/reputations/prediction",
                    HttpMethod.POST,
                    new HttpEntity<>(predictionRequest, createHeaders()),
                    Map.class
            );
        }

        // 이제 하나 더 추가해서 11번째로 만들기
        Map<String, Object> finalPredictionRequest = Map.of(
                "memberId", memberId,
                "marketId", 100L,
                "isCorrect", true
        );

        // when
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/reputations/prediction",
                HttpMethod.POST,
                new HttpEntity<>(finalPredictionRequest, createHeaders()),
                Map.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Boolean) response.getBody().get("success")).isTrue();
        
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("predictionCount")).isEqualTo(11); // 10 + 1
        assertThat(data.get("predictionCorrect")).isEqualTo(8); // 7 + 1
        
        // accuracy = FLOOR(8/11 * 10000) / 100 = FLOOR(7272.727) / 100 = 7272 / 100 = 72.72
        assertThat(data.get("predictionAccuracy")).isEqualTo(72.72);

        // DB에서도 확인
        Reputation updatedReputation = reputationRepository.findByMemberId(memberId).orElseThrow();
        assertThat(updatedReputation.getPredictionCount()).isEqualTo(11);
        assertThat(updatedReputation.getPredictionCorrect()).isEqualTo(8);
        assertThat(updatedReputation.getPredictionAccuracy().doubleValue()).isEqualTo(72.72);
    }

    @Test
    @DisplayName("다양한 활동 타입별 점수 확인")
    void internalApi_activity_differentTypes_scores() {
        // given - 먼저 거주지 선언을 통해 Reputation 생성
        Long memberId = 4L;
        Map<String, String> residenceRequest = Map.of("sido", "서울", "sigu", "성동구");
        
        restTemplate.exchange(
                "/api/v1/reputations/me/residence",
                HttpMethod.PUT,
                new HttpEntity<>(residenceRequest, createMemberHeaders(memberId)),
                Map.class
        );

        // when & then - VOTE (+10점)
        Map<String, Object> voteRequest = Map.of(
                "memberId", 4L,
                "activityType", "VOTE",
                "region", Map.of("sido", "서울", "sigu", "성동구")
        );
        
        ResponseEntity<Map> voteResponse = restTemplate.exchange(
                "/api/v1/reputations/activity",
                HttpMethod.POST,
                new HttpEntity<>(voteRequest, createHeaders()),
                Map.class
        );
        
        Map<String, Object> voteData = (Map<String, Object>) voteResponse.getBody().get("data");
        assertThat(voteData.get("activityScore")).isEqualTo(10);

        // when & then - COMMENT (+2점)
        Map<String, Object> commentRequest = Map.of(
                "memberId", 4L,
                "activityType", "COMMENT",
                "region", Map.of("sido", "서울", "sigu", "성동구")
        );
        
        ResponseEntity<Map> commentResponse = restTemplate.exchange(
                "/api/v1/reputations/activity",
                HttpMethod.POST,
                new HttpEntity<>(commentRequest, createHeaders()),
                Map.class
        );
        
        Map<String, Object> commentData = (Map<String, Object>) commentResponse.getBody().get("data");
        assertThat(commentData.get("activityScore")).isEqualTo(12); // 10 + 2

        // when & then - BATTLE_APPROVED (+20점)
        Map<String, Object> approvedRequest = Map.of(
                "memberId", 4L,
                "activityType", "BATTLE_APPROVED",
                "region", Map.of("sido", "서울", "sigu", "성동구")
        );
        
        ResponseEntity<Map> approvedResponse = restTemplate.exchange(
                "/api/v1/reputations/activity",
                HttpMethod.POST,
                new HttpEntity<>(approvedRequest, createHeaders()),
                Map.class
        );
        
        Map<String, Object> approvedData = (Map<String, Object>) approvedResponse.getBody().get("data");
        assertThat(approvedData.get("activityScore")).isEqualTo(32); // 12 + 20
        assertThat(approvedData.get("activityCount")).isEqualTo(3); // 모두 일치 지역이므로 3
        assertThat(approvedData.get("activityConfirmed")).isEqualTo(true); // 3 도달
    }

    @Test
    @DisplayName("존재하지 않는 회원 → RESOURCE_NOT_FOUND")
    void internalApi_activity_nonExistentMember_notFound() {
        // given
        Map<String, Object> request = Map.of(
                "memberId", 999L,
                "activityType", "VOTE",
                "region", Map.of("sido", "서울", "sigu", "성동구")
        );

        // when
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/reputations/activity",
                HttpMethod.POST,
                new HttpEntity<>(request, createHeaders()),
                Map.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat((Boolean) response.getBody().get("success")).isFalse();
        assertThat(response.getBody().get("errorCode")).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().get("message")).isNotNull();
        assertThat(response.getBody().get("data")).isNull();
    }
}