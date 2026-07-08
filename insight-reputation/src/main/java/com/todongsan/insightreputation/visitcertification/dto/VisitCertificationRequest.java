package com.todongsan.insightreputation.visitcertification.dto;

import com.todongsan.insightreputation.enums.VisitCertMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "방문 인증 등록 요청")
public class VisitCertificationRequest {

    @NotBlank
    @Schema(description = "시/도", example = "서울", required = true)
    private String sido;

    @NotBlank
    @Schema(description = "시/구", example = "성동구", required = true)
    private String sigu;

    @NotNull
    @Schema(description = "인증 방법", example = "GPS", required = true)
    private VisitCertMethod method;

    @Valid
    @Schema(description = "GPS 기반 인증 데이터 (method가 GPS일 때만 필수)", required = false)
    private GpsData data;

    public VisitCertificationRequest(String sido, String sigu, VisitCertMethod method, GpsData data) {
        this.sido = sido;
        this.sigu = sigu;
        this.method = method;
        this.data = data;
    }
}