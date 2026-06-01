package com.todongsan.insightreputation.visitcertification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "GPS 기반 방문 인증 데이터")
public class GpsData {

    @NotNull
    @DecimalMin(value = "-90.0", message = "위도는 -90도 이상이어야 합니다")
    @DecimalMax(value = "90.0", message = "위도는 90도 이하여야 합니다")
    @Schema(description = "사용자 현재 위치 위도", example = "37.544876", required = true)
    private Double latitude;

    @NotNull
    @DecimalMin(value = "-180.0", message = "경도는 -180도 이상이어야 합니다")
    @DecimalMax(value = "180.0", message = "경도는 180도 이하여야 합니다")
    @Schema(description = "사용자 현재 위치 경도", example = "127.055678", required = true)
    private Double longitude;

    public GpsData(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}