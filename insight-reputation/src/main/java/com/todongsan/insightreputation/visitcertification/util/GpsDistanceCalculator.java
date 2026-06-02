package com.todongsan.insightreputation.visitcertification.util;

public class GpsDistanceCalculator {
    
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double CERT_RADIUS_KM = 3.0;

    /**
     * 두 GPS 좌표 간의 거리를 계산합니다 (하버사인 공식)
     * 
     * @param lat1 첫 번째 지점의 위도
     * @param lon1 첫 번째 지점의 경도
     * @param lat2 두 번째 지점의 위도
     * @param lon2 두 번째 지점의 경도
     * @return 거리 (킬로미터)
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // 위도와 경도를 라디안으로 변환
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);
        
        // 위도와 경도 차이 계산
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;
        
        // 하버사인 공식
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }

    /**
     * 사용자 좌표가 지역 중심 좌표 기준 3km 반경 내에 있는지 확인합니다.
     * 
     * @param userLat 사용자 위도
     * @param userLon 사용자 경도
     * @param centerLat 지역 중심 위도
     * @param centerLon 지역 중심 경도
     * @return 3km 반경 내에 있으면 true, 그렇지 않으면 false
     */
    public static boolean isWithinRadius(double userLat, double userLon, 
                                        double centerLat, double centerLon) {
        double distance = calculateDistance(userLat, userLon, centerLat, centerLon);
        return distance <= CERT_RADIUS_KM;
    }
}