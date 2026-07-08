package com.todongsan.insightreputation.visitcertification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentCertificationRequest {
    
    @NotBlank(message = "시/도는 필수입니다.")
    private String sido;
    
    @NotBlank(message = "시/구는 필수입니다.")
    private String sigu;
    
    @NotNull(message = "댓글 ID는 필수입니다.")
    private Long commentId;
}