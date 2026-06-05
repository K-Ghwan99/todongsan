package com.todongsan.memberpointservice.member.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class MemberBatchRequest {

    @NotEmpty
    private List<Long> memberIds;
}
