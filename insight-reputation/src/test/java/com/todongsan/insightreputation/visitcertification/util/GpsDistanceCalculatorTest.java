package com.todongsan.insightreputation.visitcertification.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class GpsDistanceCalculatorTest {

    @Test
    @DisplayName("하버사인 공식 거리 계산 - 서울 강남구 내 짧은 거리")
    void calculateDistance_shortDistance_returnsCorrectDistance() {
        // Given: 서울 강남구 내 두 지점 (약 1km 거리)
        double lat1 = 37.517235; // 강남구 중심
        double lon1 = 127.047739;
        double lat2 = 37.525000; // 약 1km 북쪽
        double lon2 = 127.047739;

        // When
        double distance = GpsDistanceCalculator.calculateDistance(lat1, lon1, lat2, lon2);

        // Then: 약 0.86km 정도 나와야 함
        assertThat(distance).isCloseTo(0.86, within(0.1));
    }

    @Test
    @DisplayName("반경 내 좌표 체크 - 2km 거리는 3km 반경 내")
    void isWithinRadius_2kmDistance_returnsTrue() {
        // Given: 서울 강남구 중심 좌표
        double centerLat = 37.517235;
        double centerLon = 127.047739;
        
        // 약 2km 떨어진 지점
        double userLat = 37.535000;
        double userLon = 127.047739;

        // When
        boolean result = GpsDistanceCalculator.isWithinRadius(userLat, userLon, centerLat, centerLon);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("반경 외 좌표 체크 - 4km 거리는 3km 반경 외")
    void isWithinRadius_4kmDistance_returnsFalse() {
        // Given: 서울 강남구 중심 좌표
        double centerLat = 37.517235;
        double centerLon = 127.047739;
        
        // 약 4km 떨어진 지점 (위도 약 0.036도 차이 ≈ 4km)
        double userLat = 37.553235;
        double userLon = 127.047739;

        // When
        boolean result = GpsDistanceCalculator.isWithinRadius(userLat, userLon, centerLat, centerLon);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("경계값 테스트 - 2.9km는 반경 내")
    void isWithinRadius_2point9km_returnsTrue() {
        // Given: 강남구 중심에서 약 2.9km 떨어진 지점
        double centerLat = 37.517235;
        double centerLon = 127.047739;
        
        // 약 2.9km = 위도 약 0.026도 차이
        double userLat = 37.543235;
        double userLon = 127.047739;

        // When
        boolean result = GpsDistanceCalculator.isWithinRadius(userLat, userLon, centerLat, centerLon);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("경계값 테스트 - 3.1km는 반경 외")
    void isWithinRadius_slightly3point1km_returnsFalse() {
        // Given: 강남구 중심에서 3.1km 떨어진 지점
        double centerLat = 37.517235;
        double centerLon = 127.047739;
        
        // 3.1km = 위도 약 0.028도 차이
        double userLat = 37.545235;
        double userLon = 127.047739;

        // When
        boolean result = GpsDistanceCalculator.isWithinRadius(userLat, userLon, centerLat, centerLon);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("정확한 거리 계산 검증")
    void calculateDistance_verifyAccuracy() {
        // Given: 강남구 중심 좌표
        double centerLat = 37.517235;
        double centerLon = 127.047739;
        
        // When: 여러 거리의 지점들에 대해 계산
        double distance1 = GpsDistanceCalculator.calculateDistance(centerLat, centerLon, 37.543235, centerLon);
        double distance2 = GpsDistanceCalculator.calculateDistance(centerLat, centerLon, 37.545235, centerLon);
        
        // Then: 실제 계산된 거리 확인 (로그용)
        System.out.println("Distance 1 (to 37.543235): " + distance1 + " km");
        System.out.println("Distance 2 (to 37.545235): " + distance2 + " km");
        
        assertThat(distance1).isLessThan(3.0);
        assertThat(distance2).isGreaterThan(3.0);
    }

    @Test
    @DisplayName("동일한 좌표 - 거리는 0")
    void calculateDistance_sameCoordinates_returnsZero() {
        // Given: 동일한 좌표
        double lat = 37.517235;
        double lon = 127.047739;

        // When
        double distance = GpsDistanceCalculator.calculateDistance(lat, lon, lat, lon);

        // Then
        assertThat(distance).isEqualTo(0.0);
    }

    @Test
    @DisplayName("서울-부산 간 장거리 계산")
    void calculateDistance_seoulToBusan_returnsLongDistance() {
        // Given: 서울과 부산 좌표
        double seoulLat = 37.5665;
        double seoulLon = 126.9780;
        double busanLat = 35.1796;
        double busanLon = 129.0756;

        // When
        double distance = GpsDistanceCalculator.calculateDistance(seoulLat, seoulLon, busanLat, busanLon);

        // Then: 약 325km 정도
        assertThat(distance).isCloseTo(325.0, within(50.0));
    }
}