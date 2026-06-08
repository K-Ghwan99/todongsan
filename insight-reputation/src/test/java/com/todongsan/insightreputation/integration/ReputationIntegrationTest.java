package com.todongsan.insightreputation.integration;

import com.todongsan.insightreputation.reputation.entity.Reputation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReputationIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("거주지역 최초 선언 → 쿨다운 skip → 조회")
    void reputation_firstResidenceDeclaration_flow() {
        // given
        Long memberId = 1L;
        Map<String, String> residenceRequest = Map.of(
            "sido", "서울",
            "sigu", "성동구"
        );

        // when - 1. 최초 거주지역 선언
        ResponseEntity<Map> declareResponse = restTemplate.exchange(
                "/api/v1/reputations/me/residence",
                HttpMethod.PUT,
                new HttpEntity<>(residenceRequest, createMemberHeaders(memberId)),
                Map.class
        );

        // then - 1. 최초 선언 성공
        System.out.println("=== 첫 번째 테스트 디버그 ===");
        System.out.println("Status Code: " + declareResponse.getStatusCode());
        System.out.println("Response Body: " + declareResponse.getBody());
        
        assertThat(declareResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Boolean) declareResponse.getBody().get("success")).isTrue();
        
        Map<String, Object> declareData = (Map<String, Object>) declareResponse.getBody().get("data");
        System.out.println("Declare Data: " + declareData);
        
        assertThat(declareData.get("sido")).isEqualTo("서울");
        assertThat(declareData.get("sigu")).isEqualTo("성동구");
        assertThat(declareData.get("residenceChangedAt")).isNull(); // 최초 선언이므로 null
        assertThat(declareData.get("nextChangeAvailableDate")).isNull(); // 최초 선언이므로 null

        // when - 2. 내 신뢰도 조회
        ResponseEntity<Map> getMyResponse = restTemplate.exchange(
                "/api/v1/reputations/me",
                HttpMethod.GET,
                new HttpEntity<>(createMemberHeaders(memberId)),
                Map.class
        );

        // then - 2. 내 신뢰도 조회 성공
        assertThat(getMyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Boolean) getMyResponse.getBody().get("success")).isTrue();
        
        Map<String, Object> myData = (Map<String, Object>) getMyResponse.getBody().get("data");
        assertThat(myData.get("memberId")).isEqualTo(memberId.intValue());
        assertThat(myData.get("residenceSido")).isEqualTo("서울");
        assertThat(myData.get("residenceSigu")).isEqualTo("성동구");
        assertThat(myData.get("visitCertifications")).isNotNull(); // 빈 리스트라도 존재
    }

    @Test
    @DisplayName("거주지역 변경 성공 → activityCount 초기화 → 즉시 재변경 실패")
    void reputation_residenceChange_cooldown_flow() {
        // given
        Long memberId = 2L;
        
        // 1. 최초 거주지역 선언
        Map<String, String> firstRequest = Map.of("sido", "서울", "sigu", "성동구");
        restTemplate.exchange(
                "/api/v1/reputations/me/residence",
                HttpMethod.PUT,
                new HttpEntity<>(firstRequest, createMemberHeaders(memberId)),
                Map.class
        );

        // 2. activityCount를 3으로 설정하기 위해 3번의 활동 API 호출
        Map<String, Object> activityRequest = Map.of(
                "memberId", memberId,
                "activityType", "VOTE",
                "region", Map.of("sido", "서울", "sigu", "성동구")
        );
        
        // 3번 호출하여 activityCount를 3으로 만들기
        for (int i = 0; i < 3; i++) {
            restTemplate.exchange(
                    "/internal/api/v1/reputations/activity",
                    HttpMethod.POST,
                    new HttpEntity<>(activityRequest, createHeaders()),
                    Map.class
            );
        }

        // when - 거주지역 변경
        Map<String, String> changeRequest = Map.of("sido", "부산", "sigu", "해운대구");
        ResponseEntity<Map> changeResponse = restTemplate.exchange(
                "/api/v1/reputations/me/residence",
                HttpMethod.PUT,
                new HttpEntity<>(changeRequest, createMemberHeaders(memberId)),
                Map.class
        );

        // then - 1. 변경 성공 및 activityCount 초기화 확인
        System.out.println("=== 두 번째 테스트 디버그 ===");
        System.out.println("Change Status Code: " + changeResponse.getStatusCode());
        System.out.println("Change Response Body: " + changeResponse.getBody());
        
        assertThat(changeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Boolean) changeResponse.getBody().get("success")).isTrue();
        
        Map<String, Object> changeData = (Map<String, Object>) changeResponse.getBody().get("data");
        assertThat(changeData.get("sido")).isEqualTo("부산");
        assertThat(changeData.get("sigu")).isEqualTo("해운대구");
        assertThat(changeData.get("residenceChangedAt")).isNotNull(); // 변경 시간 기록
        
        // DB에서 activityCount 초기화 확인
        reputationRepository.flush(); // Ensure changes are persisted
        Reputation updatedReputation = reputationRepository.findByMemberId(memberId).orElseThrow();
        assertThat(updatedReputation.getActivityCount()).isEqualTo(0);
        assertThat(updatedReputation.getActivityConfirmedAt()).isNull();

        // when - 즉시 재변경 시도
        Map<String, String> immediateChangeRequest = Map.of("sido", "대구", "sigu", "중구");
        ResponseEntity<Map> cooldownResponse = restTemplate.exchange(
                "/api/v1/reputations/me/residence",
                HttpMethod.PUT,
                new HttpEntity<>(immediateChangeRequest, createMemberHeaders(memberId)),
                Map.class
        );

        // then - 2. 쿨다운 에러 확인
        assertThat(cooldownResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Boolean) cooldownResponse.getBody().get("success")).isFalse();
        assertThat(cooldownResponse.getBody().get("errorCode")).isEqualTo("REPUTATION_RESIDENCE_CHANGE_COOLDOWN");
        assertThat(cooldownResponse.getBody().get("message")).isNotNull();
        
        // nextChangeAvailableDate 확인
        Map<String, Object> errorData = (Map<String, Object>) cooldownResponse.getBody().get("data");
        assertThat(errorData.get("nextChangeAvailableDate")).isNotNull();
    }

    @Test
    @DisplayName("타인 신뢰도 조회 → 민감 정보 제외")
    void reputation_getOther_sensitiveFieldsExcluded() {
        // given
        Long memberId = 3L;
        
        // 신뢰도 데이터 생성
        Map<String, String> residenceRequest = Map.of("sido", "인천", "sigu", "연수구");
        restTemplate.exchange(
                "/api/v1/reputations/me/residence",
                HttpMethod.PUT,
                new HttpEntity<>(residenceRequest, createMemberHeaders(memberId)),
                Map.class
        );

        // when - 타인의 신뢰도 조회
        ResponseEntity<Map> getOtherResponse = restTemplate.exchange(
                "/api/v1/reputations/" + memberId,
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                Map.class
        );

        // then - 민감 정보 제외 확인
        assertThat(getOtherResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Boolean) getOtherResponse.getBody().get("success")).isTrue();
        
        Map<String, Object> otherData = (Map<String, Object>) getOtherResponse.getBody().get("data");
        assertThat(otherData.get("memberId")).isEqualTo(memberId.intValue());
        assertThat(otherData.get("predictionCount")).isNotNull(); // 0이라도 포함
        assertThat(otherData.get("predictionAccuracy")).isNotNull(); // 0.00이라도 포함
        
        // 민감 정보는 포함되지 않아야 함
        assertThat(otherData).doesNotContainKey("predictionCorrect"); // 민감 정보
        assertThat(otherData).doesNotContainKey("residenceSido"); // 민감 정보
        assertThat(otherData).doesNotContainKey("residenceSigu"); // 민감 정보
        assertThat(otherData).doesNotContainKey("visitCertifications"); // 내 정보에만 포함
    }

    @Test
    @DisplayName("존재하지 않는 회원 신뢰도 조회 → NOT_FOUND")
    void reputation_getNonExistent_notFound() {
        // given
        Long nonExistentMemberId = 999L;

        // when
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/reputations/" + nonExistentMemberId,
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                Map.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat((Boolean) response.getBody().get("success")).isFalse();
        assertThat(response.getBody().get("errorCode")).isEqualTo("REPUTATION_NOT_FOUND");
        assertThat(response.getBody().get("message")).isNotNull();
        assertThat(response.getBody().get("data")).isNull();
    }
}