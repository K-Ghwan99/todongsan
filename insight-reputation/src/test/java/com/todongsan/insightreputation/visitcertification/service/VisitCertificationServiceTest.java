package com.todongsan.insightreputation.visitcertification.service;

import com.todongsan.insightreputation.enums.VisitCertMethod;
import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.visitcertification.dto.VisitCertificationResponse;
import com.todongsan.insightreputation.visitcertification.entity.VisitCertification;
import com.todongsan.insightreputation.visitcertification.repository.VisitCertificationRepository;
import com.todongsan.insightreputation.visitcertification.util.RegionCenterCoordinateProvider;
import com.todongsan.insightreputation.visitcertification.exception.VisitCertCooldownException;
import com.todongsan.insightreputation.global.client.BattleClient;
import com.todongsan.insightreputation.global.client.BattleCommentResponse;
import com.todongsan.insightreputation.global.client.BattleResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitCertificationServiceTest {

    @InjectMocks
    VisitCertificationService service;

    @Mock
    VisitCertificationRepository repository;

    @Mock
    RegionCenterCoordinateProvider regionCoordinateProvider;

    @Mock
    BattleClient battleClient;

    @Test
    @DisplayName("GPS 최초 인증 - 반경 내 좌표 → 인증 성공")
    void registerGps_firstTimeWithinRadius_success() {
        // Given
        Long memberId = 1L;
        String sido = "서울";
        String sigu = "강남구";
        Double latitude = 37.517235;
        Double longitude = 127.047739;

        // 기존 인증 없음
        when(repository.findByMemberIdAndSidoAndSigu(memberId, sido, sigu))
            .thenReturn(Optional.empty());

        // 지역 중심 좌표 제공 (강남구 중심)
        RegionCenterCoordinateProvider.RegionCoordinate centerCoord = 
            new RegionCenterCoordinateProvider.RegionCoordinate(37.517235, 127.047739);
        when(regionCoordinateProvider.getCoordinate(sido, sigu))
            .thenReturn(Optional.of(centerCoord));

        // 저장할 엔티티 모킹
        VisitCertification savedCert = VisitCertification.builder()
            .memberId(memberId)
            .sido(sido)
            .sigu(sigu)
            .method(VisitCertMethod.GPS)
            .latitude(new BigDecimal(latitude.toString()))
            .longitude(new BigDecimal(longitude.toString()))
            .build();
        when(repository.save(any(VisitCertification.class))).thenReturn(savedCert);

        // When
        VisitCertificationResponse response = service.registerGps(memberId, sido, sigu, latitude, longitude);

        // Then
        assertThat(response.getSido()).isEqualTo(sido);
        assertThat(response.getSigu()).isEqualTo(sigu);
        assertThat(response.getMethod()).isEqualTo(VisitCertMethod.GPS);
        assertThat(response.getCertifiedAt()).isNotNull();
        assertThat(response.getLastCertifiedAt()).isNotNull();
        assertThat(response.getNextAvailableDate()).isNotNull();

        verify(repository).save(any(VisitCertification.class));
    }

    @Test
    @DisplayName("GPS 재인증 - 30일 경과 후 성공")
    void registerGps_recertificationAfter30Days_success() {
        // Given
        Long memberId = 1L;
        String sido = "서울";
        String sigu = "강남구";
        Double latitude = 37.517235;
        Double longitude = 127.047739;

        // 기존 인증 (31일 전)
        VisitCertification existingCert = VisitCertification.builder()
            .memberId(memberId)
            .sido(sido)
            .sigu(sigu)
            .method(VisitCertMethod.GPS)
            .latitude(new BigDecimal("37.517000"))
            .longitude(new BigDecimal("127.047000"))
            .build();
        // 31일 전으로 설정 (쿨다운 경과)
        existingCert.updateGpsCertification(new BigDecimal("37.517000"), new BigDecimal("127.047000"));
        LocalDateTime lastCertified = LocalDateTime.now().minusDays(31);
        
        // Reflection을 사용해서 lastCertifiedAt 설정 (테스트 목적)
        VisitCertification mockExistingCert = mock(VisitCertification.class);
        when(mockExistingCert.getLastCertifiedAt()).thenReturn(lastCertified);
        
        when(repository.findByMemberIdAndSidoAndSigu(memberId, sido, sigu))
            .thenReturn(Optional.of(mockExistingCert));

        // 지역 중심 좌표 제공
        RegionCenterCoordinateProvider.RegionCoordinate centerCoord = 
            new RegionCenterCoordinateProvider.RegionCoordinate(37.517235, 127.047739);
        when(regionCoordinateProvider.getCoordinate(sido, sigu))
            .thenReturn(Optional.of(centerCoord));

        // When
        VisitCertificationResponse response = service.registerGps(memberId, sido, sigu, latitude, longitude);

        // Then
        assertThat(response).isNotNull();
        
        // 기존 레코드 업데이트 확인 (save 호출 없음, 업데이트만)
        verify(mockExistingCert).updateGpsCertification(any(BigDecimal.class), any(BigDecimal.class));
        verify(repository, never()).save(any(VisitCertification.class));
    }

    @Test
    @DisplayName("GPS 인증 - 반경 외 좌표 → VISIT_CERT_OUT_OF_RANGE")
    void registerGps_outsideRadius_throwsOutOfRange() {
        // Given
        Long memberId = 1L;
        String sido = "서울";
        String sigu = "강남구";
        Double latitude = 37.560000; // 강남구 중심에서 약 4.7km 떨어진 지점
        Double longitude = 127.047739;

        // 기존 인증 없음
        when(repository.findByMemberIdAndSidoAndSigu(memberId, sido, sigu))
            .thenReturn(Optional.empty());

        // 지역 중심 좌표 제공 (강남구 중심)
        RegionCenterCoordinateProvider.RegionCoordinate centerCoord = 
            new RegionCenterCoordinateProvider.RegionCoordinate(37.517235, 127.047739);
        when(regionCoordinateProvider.getCoordinate(sido, sigu))
            .thenReturn(Optional.of(centerCoord));

        // When & Then
        assertThatThrownBy(() -> service.registerGps(memberId, sido, sigu, latitude, longitude))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VISIT_CERT_OUT_OF_RANGE);

        verify(repository, never()).save(any(VisitCertification.class));
    }

    @Test
    @DisplayName("GPS 인증 - 30일 쿨다운 미경과 → VISIT_CERT_COOLDOWN")
    void registerGps_cooldownNotExpired_throwsCooldown() {
        // Given
        Long memberId = 1L;
        String sido = "서울";
        String sigu = "강남구";
        Double latitude = 37.517235;
        Double longitude = 127.047739;

        // 기존 인증 (29일 전 - 쿨다운 미경과)
        VisitCertification mockExistingCert = mock(VisitCertification.class);
        LocalDateTime lastCertified = LocalDateTime.now().minusDays(29);
        when(mockExistingCert.getLastCertifiedAt()).thenReturn(lastCertified);
        
        when(repository.findByMemberIdAndSidoAndSigu(memberId, sido, sigu))
            .thenReturn(Optional.of(mockExistingCert));

        // When & Then
        assertThatThrownBy(() -> service.registerGps(memberId, sido, sigu, latitude, longitude))
            .isInstanceOf(VisitCertCooldownException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VISIT_CERT_COOLDOWN);

        verify(repository, never()).save(any(VisitCertification.class));
        verify(mockExistingCert, never()).updateGpsCertification(any(), any());
    }

    @Test
    @DisplayName("GPS 인증 - 경계값: 정확히 30일 경과 → 성공")
    void registerGps_exactly30DaysLater_success() {
        // Given
        Long memberId = 1L;
        String sido = "서울";
        String sigu = "강남구";
        Double latitude = 37.517235;
        Double longitude = 127.047739;

        // 기존 인증 (정확히 30일 전)
        VisitCertification mockExistingCert = mock(VisitCertification.class);
        LocalDateTime lastCertified = LocalDateTime.now().minusDays(30).minusSeconds(1); // 30일 + 1초 전
        when(mockExistingCert.getLastCertifiedAt()).thenReturn(lastCertified);
        
        when(repository.findByMemberIdAndSidoAndSigu(memberId, sido, sigu))
            .thenReturn(Optional.of(mockExistingCert));

        // 지역 중심 좌표 제공
        RegionCenterCoordinateProvider.RegionCoordinate centerCoord = 
            new RegionCenterCoordinateProvider.RegionCoordinate(37.517235, 127.047739);
        when(regionCoordinateProvider.getCoordinate(sido, sigu))
            .thenReturn(Optional.of(centerCoord));

        // When
        VisitCertificationResponse response = service.registerGps(memberId, sido, sigu, latitude, longitude);

        // Then
        assertThat(response).isNotNull();
        verify(mockExistingCert).updateGpsCertification(any(BigDecimal.class), any(BigDecimal.class));
    }

    @Test
    @DisplayName("GPS 인증 - 경계값: 30일 - 1초는 쿨다운 → VISIT_CERT_COOLDOWN")
    void registerGps_29DaysAnd59Minutes_throwsCooldown() {
        // Given
        Long memberId = 1L;
        String sido = "서울";
        String sigu = "강남구";
        Double latitude = 37.517235;
        Double longitude = 127.047739;

        // 기존 인증 (30일 - 1초 전)
        VisitCertification mockExistingCert = mock(VisitCertification.class);
        LocalDateTime lastCertified = LocalDateTime.now().minusDays(30).plusSeconds(1); // 29일 59분 59초 전
        when(mockExistingCert.getLastCertifiedAt()).thenReturn(lastCertified);
        
        when(repository.findByMemberIdAndSidoAndSigu(memberId, sido, sigu))
            .thenReturn(Optional.of(mockExistingCert));

        // When & Then
        assertThatThrownBy(() -> service.registerGps(memberId, sido, sigu, latitude, longitude))
            .isInstanceOf(VisitCertCooldownException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VISIT_CERT_COOLDOWN);
    }

    @Test
    @DisplayName("GPS 인증 - 지원하지 않는 지역 → VISIT_CERT_UNSUPPORTED_REGION")
    void registerGps_unsupportedRegion_throwsUnsupportedRegion() {
        // Given
        Long memberId = 1L;
        String sido = "제주";
        String sigu = "서귀포시";
        Double latitude = 33.254000;
        Double longitude = 126.560000;

        // 기존 인증 없음
        when(repository.findByMemberIdAndSidoAndSigu(memberId, sido, sigu))
            .thenReturn(Optional.empty());

        // 지역 중심 좌표 없음 (지원하지 않는 지역)
        when(regionCoordinateProvider.getCoordinate(sido, sigu))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.registerGps(memberId, sido, sigu, latitude, longitude))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VISIT_CERT_UNSUPPORTED_REGION);
    }

    @Test
    @DisplayName("댓글 최초 인증 - 지역 일치 → 인증 성공")
    void registerComment_firstTimeValidRegion_success() {
        // Given
        Long memberId = 1L;
        String sido = "서울";
        String sigu = "강남구";
        Long commentId = 15L;

        // 기존 인증 없음
        when(repository.findByMemberIdAndSidoAndSigu(memberId, sido, sigu))
            .thenReturn(Optional.empty());

        // 댓글 정보
        BattleCommentResponse comment = BattleCommentResponse.builder()
            .commentId(commentId)
            .battleId(42L)
            .memberId(678L)
            .createdAt(LocalDateTime.now())
            .build();
        when(battleClient.getComment(commentId)).thenReturn(comment);

        // Battle 정보 (지역 일치)
        BattleResponse battle = BattleResponse.builder()
            .battleId(42L)
            .title("성수 vs 연남")
            .sido(sido)
            .sigu(sigu)
            .status("ACTIVE")
            .build();
        when(battleClient.getBattle(42L)).thenReturn(battle);

        // 저장할 엔티티 모킹
        VisitCertification savedCert = VisitCertification.builder()
            .memberId(memberId)
            .sido(sido)
            .sigu(sigu)
            .method(VisitCertMethod.COMMENT)
            .commentContent("댓글 ID: 15 (작성자: 678)")
            .battleId(42L)
            .build();
        when(repository.save(any(VisitCertification.class))).thenReturn(savedCert);

        // When
        VisitCertificationResponse response = service.registerComment(memberId, sido, sigu, commentId);

        // Then
        assertThat(response.getSido()).isEqualTo(sido);
        assertThat(response.getSigu()).isEqualTo(sigu);
        assertThat(response.getMethod()).isEqualTo(VisitCertMethod.COMMENT);
        assertThat(response.getCertifiedAt()).isNotNull();
        assertThat(response.getLastCertifiedAt()).isNotNull();
        assertThat(response.getNextAvailableDate()).isNotNull();

        verify(repository).save(any(VisitCertification.class));
    }

    @Test
    @DisplayName("댓글 재인증 - 30일 경과 후 성공")
    void registerComment_recertificationAfter30Days_success() {
        // Given
        Long memberId = 1L;
        String sido = "서울";
        String sigu = "강남구";
        Long commentId = 20L;

        // 기존 인증 (31일 전)
        VisitCertification mockExistingCert = mock(VisitCertification.class);
        LocalDateTime lastCertified = LocalDateTime.now().minusDays(31);
        when(mockExistingCert.getLastCertifiedAt()).thenReturn(lastCertified);
        
        when(repository.findByMemberIdAndSidoAndSigu(memberId, sido, sigu))
            .thenReturn(Optional.of(mockExistingCert));

        // 댓글 정보
        BattleCommentResponse comment = BattleCommentResponse.builder()
            .commentId(commentId)
            .battleId(50L)
            .memberId(789L)
            .createdAt(LocalDateTime.now())
            .build();
        when(battleClient.getComment(commentId)).thenReturn(comment);

        // Battle 정보 (지역 일치)
        BattleResponse battle = BattleResponse.builder()
            .battleId(50L)
            .title("홍대 vs 이태원")
            .sido(sido)
            .sigu(sigu)
            .status("ACTIVE")
            .build();
        when(battleClient.getBattle(50L)).thenReturn(battle);

        // When
        VisitCertificationResponse response = service.registerComment(memberId, sido, sigu, commentId);

        // Then
        assertThat(response).isNotNull();
        
        // 기존 레코드 업데이트 확인
        verify(mockExistingCert).updateCommentCertification(
            eq("댓글 ID: 20 (작성자: 789)"), 
            eq(50L)
        );
        verify(repository, never()).save(any(VisitCertification.class));
    }

    @Test
    @DisplayName("댓글 인증 - 존재하지 않는 댓글 → VISIT_CERT_COMMENT_NOT_FOUND")
    void registerComment_commentNotFound_throwsCommentNotFound() {
        // Given
        Long memberId = 1L;
        String sido = "서울";
        String sigu = "강남구";
        Long commentId = 999L;

        // 기존 인증 없음
        when(repository.findByMemberIdAndSidoAndSigu(memberId, sido, sigu))
            .thenReturn(Optional.empty());

        // 댓글 없음
        when(battleClient.getComment(commentId))
            .thenThrow(new CustomException(ErrorCode.VISIT_CERT_COMMENT_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> service.registerComment(memberId, sido, sigu, commentId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VISIT_CERT_COMMENT_NOT_FOUND);

        verify(repository, never()).save(any(VisitCertification.class));
    }

    @Test
    @DisplayName("댓글 인증 - 지역 불일치 → VISIT_CERT_COMMENT_REGION_MISMATCH")
    void registerComment_regionMismatch_throwsRegionMismatch() {
        // Given
        Long memberId = 1L;
        String sido = "서울";
        String sigu = "강남구";
        Long commentId = 15L;

        // 기존 인증 없음
        when(repository.findByMemberIdAndSidoAndSigu(memberId, sido, sigu))
            .thenReturn(Optional.empty());

        // 댓글 정보
        BattleCommentResponse comment = BattleCommentResponse.builder()
            .commentId(commentId)
            .battleId(42L)
            .memberId(678L)
            .createdAt(LocalDateTime.now())
            .build();
        when(battleClient.getComment(commentId)).thenReturn(comment);

        // Battle 정보 (지역 불일치)
        BattleResponse battle = BattleResponse.builder()
            .battleId(42L)
            .title("성수 vs 연남")
            .sido("부산")  // 요청과 다른 지역
            .sigu("해운대구")  // 요청과 다른 지역
            .status("ACTIVE")
            .build();
        when(battleClient.getBattle(42L)).thenReturn(battle);

        // When & Then
        assertThatThrownBy(() -> service.registerComment(memberId, sido, sigu, commentId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VISIT_CERT_COMMENT_REGION_MISMATCH);

        verify(repository, never()).save(any(VisitCertification.class));
    }

    @Test
    @DisplayName("댓글 인증 - 30일 쿨다운 미경과 → VISIT_CERT_COOLDOWN")
    void registerComment_cooldownNotExpired_throwsCooldown() {
        // Given
        Long memberId = 1L;
        String sido = "서울";
        String sigu = "강남구";
        Long commentId = 15L;

        // 기존 인증 (29일 전 - 쿨다운 미경과)
        VisitCertification mockExistingCert = mock(VisitCertification.class);
        LocalDateTime lastCertified = LocalDateTime.now().minusDays(29);
        when(mockExistingCert.getLastCertifiedAt()).thenReturn(lastCertified);
        
        when(repository.findByMemberIdAndSidoAndSigu(memberId, sido, sigu))
            .thenReturn(Optional.of(mockExistingCert));

        // When & Then
        assertThatThrownBy(() -> service.registerComment(memberId, sido, sigu, commentId))
            .isInstanceOf(VisitCertCooldownException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VISIT_CERT_COOLDOWN);

        verify(battleClient, never()).getComment(any());
        verify(repository, never()).save(any(VisitCertification.class));
    }
}