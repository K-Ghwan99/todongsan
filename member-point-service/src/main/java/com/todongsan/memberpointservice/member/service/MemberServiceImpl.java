package com.todongsan.memberpointservice.member.service;

import com.todongsan.memberpointservice.global.exception.CustomException;
import com.todongsan.memberpointservice.global.exception.ErrorCode;
import com.todongsan.memberpointservice.member.dto.request.MemberUpdateRequest;
import com.todongsan.memberpointservice.member.dto.response.MemberBatchItemResponse;
import com.todongsan.memberpointservice.member.dto.response.MemberResponse;
import com.todongsan.memberpointservice.member.dto.response.MemberUpdateResponse;
import com.todongsan.memberpointservice.member.entity.Member;
import com.todongsan.memberpointservice.member.repository.MemberRepository;
import com.todongsan.memberpointservice.member.repository.OauthTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final OauthTokenRepository oauthTokenRepository;


    @Override
    public MemberResponse getMe(Long memberId) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        return new MemberResponse(member);
    }

    @Override
    @Transactional
    public MemberUpdateResponse updateMe(Long memberId, MemberUpdateRequest request) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 닉네임 변경
        if (request.getNickname() != null) {
            if (memberRepository.existsByNicknameAndDeletedAtIsNull(request.getNickname())) {
                throw new CustomException(ErrorCode.MEMBER_NICKNAME_DUPLICATE);
            }
            member.updateNickname(request.getNickname());
        }

        // 거주지 변경
        if (request.getResidenceSido() != null || request.getResidenceSigu() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime changedAt = member.getResidenceChangedAt();
            if (changedAt != null && changedAt.plusDays(30).isAfter(now)) {
                throw new CustomException(ErrorCode.MEMBER_RESIDENCE_CHANGE_COOLDOWN);
            }
            member.updateResidence(
                    request.getResidenceSido(),
                    request.getResidenceSigu(),
                    now
            );
        }

        return new MemberUpdateResponse(member);
    }

    @Override
    @Transactional
    public Long withdraw(Long memberId) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        oauthTokenRepository.deleteByMemberId(memberId);
        member.delete(LocalDateTime.now());

        return memberId;
    }

    @Override
    public List<MemberBatchItemResponse> getBatch(List<Long> memberIds) {
        return memberRepository.findAllByIdIn(memberIds).stream()
                .map(MemberBatchItemResponse::new)
                .toList();
    }
}
