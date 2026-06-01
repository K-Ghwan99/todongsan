package com.todongsan.insightreputation.visitcertification.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class RegionCenterCoordinateProvider {

    private final Map<String, RegionCoordinate> regionCoordinates;

    public RegionCenterCoordinateProvider() {
        this.regionCoordinates = initializeRegionCoordinates();
    }

    /**
     * 지역의 중심 좌표를 조회합니다.
     * 
     * @param sido 시/도
     * @param sigu 시/구
     * @return 지역 중심 좌표 (Optional)
     */
    public Optional<RegionCoordinate> getCoordinate(String sido, String sigu) {
        String key = sido + "_" + sigu;
        return Optional.ofNullable(regionCoordinates.get(key));
    }

    private Map<String, RegionCoordinate> initializeRegionCoordinates() {
        Map<String, RegionCoordinate> coordinates = new HashMap<>();
        
        // 서울특별시 주요 구 중심 좌표
        coordinates.put("서울_강남구", new RegionCoordinate(37.517235, 127.047739));
        coordinates.put("서울_강동구", new RegionCoordinate(37.530492, 127.123837));
        coordinates.put("서울_강북구", new RegionCoordinate(37.639938, 127.025508));
        coordinates.put("서울_강서구", new RegionCoordinate(37.550937, 126.849642));
        coordinates.put("서울_관악구", new RegionCoordinate(37.478424, 126.951614));
        coordinates.put("서울_광진구", new RegionCoordinate(37.538617, 127.082375));
        coordinates.put("서울_구로구", new RegionCoordinate(37.495472, 126.887536));
        coordinates.put("서울_금천구", new RegionCoordinate(37.445970, 126.895756));
        coordinates.put("서울_노원구", new RegionCoordinate(37.654358, 127.056479));
        coordinates.put("서울_도봉구", new RegionCoordinate(37.668767, 127.047370));
        coordinates.put("서울_동대문구", new RegionCoordinate(37.574626, 127.039600));
        coordinates.put("서울_동작구", new RegionCoordinate(37.512400, 126.939079));
        coordinates.put("서울_마포구", new RegionCoordinate(37.566535, 126.901467));
        coordinates.put("서울_서대문구", new RegionCoordinate(37.579225, 126.936893));
        coordinates.put("서울_서초구", new RegionCoordinate(37.483568, 127.032431));
        coordinates.put("서울_성동구", new RegionCoordinate(37.563456, 127.036821));
        coordinates.put("서울_성북구", new RegionCoordinate(37.589292, 127.016985));
        coordinates.put("서울_송파구", new RegionCoordinate(37.514575, 127.105399));
        coordinates.put("서울_양천구", new RegionCoordinate(37.517063, 126.866632));
        coordinates.put("서울_영등포구", new RegionCoordinate(37.526436, 126.896004));
        coordinates.put("서울_용산구", new RegionCoordinate(37.532600, 126.990370));
        coordinates.put("서울_은평구", new RegionCoordinate(37.602763, 126.929550));
        coordinates.put("서울_종로구", new RegionCoordinate(37.573050, 126.978966));
        coordinates.put("서울_중구", new RegionCoordinate(37.563772, 126.997690));
        coordinates.put("서울_중랑구", new RegionCoordinate(37.606923, 127.092758));
        
        // 부산광역시 주요 구
        coordinates.put("부산_해운대구", new RegionCoordinate(35.163100, 129.163521));
        coordinates.put("부산_부산진구", new RegionCoordinate(35.162299, 129.053073));
        coordinates.put("부산_동래구", new RegionCoordinate(35.204725, 129.078413));
        coordinates.put("부산_남구", new RegionCoordinate(35.136132, 129.084350));
        coordinates.put("부산_중구", new RegionCoordinate(35.106076, 129.032895));
        
        // 대구광역시 주요 구
        coordinates.put("대구_수성구", new RegionCoordinate(35.858347, 128.630806));
        coordinates.put("대구_중구", new RegionCoordinate(35.869444, 128.606154));
        coordinates.put("대구_동구", new RegionCoordinate(35.889000, 128.635263));
        coordinates.put("대구_서구", new RegionCoordinate(35.871917, 128.559356));
        
        // 인천광역시 주요 구
        coordinates.put("인천_남동구", new RegionCoordinate(37.449564, 126.731516));
        coordinates.put("인천_연수구", new RegionCoordinate(37.410400, 126.677942));
        coordinates.put("인천_부평구", new RegionCoordinate(37.508713, 126.721667));
        coordinates.put("인천_중구", new RegionCoordinate(37.473611, 126.621667));
        
        return coordinates;
    }

    @Getter
    public static class RegionCoordinate {
        private final double latitude;
        private final double longitude;

        public RegionCoordinate(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}