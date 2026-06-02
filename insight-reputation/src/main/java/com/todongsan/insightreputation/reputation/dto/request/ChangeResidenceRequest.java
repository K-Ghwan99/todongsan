package com.todongsan.insightreputation.reputation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChangeResidenceRequest {
    
    @NotBlank(message = "시/도는 필수입니다.")
    private String sido;
    
    @NotBlank(message = "시/구는 필수입니다.")
    private String sigu;
}