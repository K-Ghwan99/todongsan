package com.todongsan.insightreputation.visitcertification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class GpsCertificationRequest {
    
    @NotBlank(message = "시/도는 필수입니다.")
    private String sido;
    
    @NotBlank(message = "시/구는 필수입니다.")
    private String sigu;
    
    @NotNull(message = "위도는 필수입니다.")
    private BigDecimal latitude;
    
    @NotNull(message = "경도는 필수입니다.")
    private BigDecimal longitude;
}