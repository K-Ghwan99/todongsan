package com.todongsan.memberpointservice.member.service;

import com.todongsan.memberpointservice.member.dto.request.MemberUpdateRequest;
import com.todongsan.memberpointservice.member.dto.response.MemberBatchItemResponse;
import com.todongsan.memberpointservice.member.dto.response.MemberResponse;
import com.todongsan.memberpointservice.member.dto.response.MemberUpdateResponse;

import java.util.List;

public interface MemberService {

    MemberResponse getMe(Long memberId);

    MemberUpdateResponse updateMe(Long memberId, MemberUpdateRequest request);

    Long withdraw(Long memberId);

    List<MemberBatchItemResponse> getBatch(List<Long> memberIds);
}
