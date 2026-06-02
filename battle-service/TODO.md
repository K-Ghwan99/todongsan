# Battle Service TODO

## Feature 1: 기본 프로젝트 구조 (Global 설정)
> 모든 기능의 공통 기반. 이걸 먼저 완성해야 나머지 기능 개발 가능.

- [ ] `application.yml` — DB, JPA, 외부 서비스 URL 설정
- [ ] `global/entity/BaseEntity.java` — `createdAt`, `updatedAt` AuditingEntity
- [ ] `global/response/ApiResponse.java` — 공통 응답 포맷 (`success`, `errorCode`, `message`, `data`, `timestamp`)
- [ ] `global/exception/ErrorCode.java` — 공통 + Battle 도메인 에러 코드 Enum
- [ ] `global/exception/CustomException.java` — ErrorCode를 담는 RuntimeException
- [ ] `global/exception/GlobalExceptionHandler.java` — `@RestControllerAdvice`
- [ ] `global/config/JpaConfig.java` — `@EnableJpaAuditing`
- [ ] `global/config/AppConfig.java` — `RestTemplate` Bean

---

## Feature 2: Battle CRUD + 관리자 상태 전환
> Battle 등록 → 관리자 승인/거절 → ACTIVE 전환 흐름.

### 엔티티 & 저장소
- [ ] `battle/entity/BattleStatus.java` — `PENDING / ACTIVE / CLOSED / CANCELLED` Enum
- [ ] `battle/entity/Battle.java` — ERD 3-1 기준 (status, optionACount, rewardAmount, settledAt 등)
- [ ] `battle/repository/BattleRepository.java` — `findByIdAndDeletedAtIsNull`, status 조회, 배치 조회용 쿼리

### DTO
- [ ] `battle/dto/request/BattleCreateRequest.java` — title, optionA, optionB, startAt, endAt
- [ ] `battle/dto/response/BattleCreateResponse.java` — 등록 직후 응답
- [ ] `battle/dto/response/BattleDetailResponse.java` — 상세 조회 응답
- [ ] `battle/dto/response/BattleListResponse.java` — 목록 조회 응답 (Page)
- [ ] `battle/dto/response/BattleStatusResponse.java` — 승인/거절/취소 후 응답 (`battleId`, `status`)

### 서비스 & 컨트롤러
- [ ] `battle/service/BattleService.java` — 인터페이스
- [ ] `battle/service/BattleServiceImpl.java` — 구현체
  - `createBattle(Long memberId, BattleCreateRequest)` — PENDING 생성, Member-Point SPEND_BATTLE_CREATE 호출
  - `getBattles(String status, int page, int size)` — ACTIVE/CLOSED만 노출
  - `getBattle(Long battleId)` — PENDING/CANCELLED는 BATTLE_NOT_FOUND 처리
  - `approveBattle(Long battleId)` — PENDING → ACTIVE, 생성자에게 EARN_BATTLE_APPROVED 보상
  - `rejectBattle(Long battleId)` — PENDING → CANCELLED
  - `cancelBattle(Long battleId)` — ACTIVE → CANCELLED
- [ ] `battle/controller/BattleController.java` — 일반 사용자 API
- [ ] `battle/controller/BattleAdminController.java` — 관리자 API (approve/reject/cancel)

---

## Feature 3: 투표 (Vote)
> 1인 1표, 변경 불가. 투표 성공 후 EARN_VOTE 보상 지급.

### 엔티티 & 저장소
- [ ] `vote/entity/BattleVote.java` — battle_id, member_id, selectedOption, isRewarded
- [ ] `vote/repository/BattleVoteRepository.java` — `uq_battle_vote` 충돌 처리, member의 투표 여부 조회

### DTO
- [ ] `vote/dto/request/VoteRequest.java` — option("A" or "B")
- [ ] `vote/dto/response/VoteResponse.java` — 투표 완료 응답
- [ ] `vote/dto/response/VoteResultResponse.java` — 결과 조회 응답 (투표 전/후, 72h 분기)
- [ ] `vote/dto/response/VoteRawResponse.java` — 내부 API 원본 데이터 응답 (votes 배열 포함)
- [ ] `vote/dto/response/CrossAnalysisResponse.java` — 교차분석 결과 (30P)
- [ ] `vote/dto/response/CertifiedResultResponse.java` — 방문 인증자 필터 (30P)

### 서비스 & 컨트롤러
- [ ] `vote/service/VoteService.java` — 인터페이스
- [ ] `vote/service/VoteServiceImpl.java` — 구현체
  - `vote(Long battleId, Long memberId, VoteRequest)` — 투표 + 집계 UPDATE + EARN_VOTE 보상
  - `getResult(Long battleId, Long memberId)` — 공개 정책 분기
  - `getCrossResult(Long battleId, Long memberId, String idempotencyKey)` — 30P 차감 후 교차분석
  - `getCertifiedResult(Long battleId, Long memberId, String idempotencyKey)` — 30P 차감 후 인증자 필터
  - `getRawVotes(Long battleId)` — 내부 Insight 전용
- [ ] `vote/controller/VoteController.java` — 투표/결과 API

---

## Feature 4: 댓글 (Comment)
> 단일 depth, soft delete. 작성 후 EARN_COMMENT 보상.

### 엔티티 & 저장소
- [ ] `comment/entity/Comment.java` — battle_id, member_id, content, deletedAt
- [ ] `comment/repository/CommentRepository.java` — `idx_comment_battle` 기반 페이징 조회

### DTO
- [ ] `comment/dto/request/CommentCreateRequest.java` — content (500자 제한)
- [ ] `comment/dto/response/CommentResponse.java` — 목록/작성 응답
- [ ] `comment/dto/response/CommentInternalResponse.java` — 내부 단건 조회 응답 (commentId, battleId, memberId, createdAt)

### 서비스 & 컨트롤러
- [ ] `comment/service/CommentService.java` — 인터페이스
- [ ] `comment/service/CommentServiceImpl.java` — 구현체
  - `createComment(Long battleId, Long memberId, CommentCreateRequest)` — 댓글 저장 + EARN_COMMENT 보상
  - `getComments(Long battleId, int page, int size)` — 삭제되지 않은 댓글 페이징
  - `deleteComment(Long battleId, Long commentId, Long memberId)` — 본인 댓글 soft delete
  - `getComment(Long commentId)` — 내부 Insight 전용 (soft delete된 것은 BATTLE_COMMENT_NOT_FOUND)
- [ ] `comment/controller/CommentController.java` — 댓글 API

---

## Feature 5: Member-Point 서비스 REST 클라이언트
> Battle → Member-Point 보상/차감 호출. 실패 시 RetryQueue 적재.

- [ ] `client/MemberPointClient.java` — RestTemplate 기반 HTTP 클라이언트
  - `earnPoint(PointEarnRequest, String idempotencyKey)` — 보상 지급
  - `spendPoint(PointSpendRequest, String idempotencyKey)` — 포인트 차감
  - `settlePoints(List<PointSettleRequest>)` — 배치 정산 보상 지급
- [ ] `client/dto/PointEarnRequest.java`
- [ ] `client/dto/PointSpendRequest.java`
- [ ] `client/dto/PointSettleRequest.java`

---

## Feature 6: Point 지급 실패 재시도 큐 (RetryQueue)
> 투표/댓글 후 Member-Point 호출 실패 시 큐에 적재, 1분 간격 최대 3회 재시도.

- [ ] `retry/entity/RetryStatus.java` — `PENDING / SUCCESS / FAILED` Enum
- [ ] `retry/entity/PointRewardRetryQueue.java` — ERD 3-4 기준 (idempotency_key UK, retry_count, status)
- [ ] `retry/repository/PointRewardRetryQueueRepository.java` — `idx_retry_status` 기반 PENDING 조회
- [ ] `retry/scheduler/RetryScheduler.java`
  - `@Scheduled(fixedDelay = 60000)` — PENDING 건 조회 → Member-Point 재호출 → 성공/실패 처리
  - 3회 초과 시 `status = FAILED` 처리

---

## Feature 7: Battle 마감 & 정산 배치
> end_at 도달 → ACTIVE → CLOSED 전환 + 승자 확정 + 보상 지급.

- [ ] `battle/scheduler/BattleCloseScheduler.java`
  - `@Scheduled(fixedDelay = 60000)` — `status=ACTIVE AND end_at <= NOW()` 조회 → CLOSED 전환 + winning_option 확정
- [ ] `battle/scheduler/BattleSettleScheduler.java`
  - CLOSED이고 `settled_at IS NULL` 인 Battle 조회
  - winning_option이 A or B면 → 승자 측 `is_rewarded=false` 투표자 조회 → 배치 보상 지급
  - DRAW면 → 바로 `settled_at` 기록
  - 성공 시 `battle_vote.is_rewarded = true`, 전원 완료 시 `battle.settled_at` 기록

---

## Feature 8: 내부 API (Insight Service 전용)
> Gateway 외부 차단 + `X-Internal-Auth` 헤더 검증.

- [ ] `battle/controller/BattleInternalController.java`
  - `GET /api/v1/battles/{battleId}/votes/raw` — VoteRawResponse (votes 배열 포함)
  - `GET /api/v1/battles/comments/{commentId}` — CommentInternalResponse
  - `GET /api/v1/battles/{battleId}/info` — BattleDetailResponse (PENDING/CANCELLED도 그대로 노출)

---

## 공통 체크리스트
- [ ] `X-Member-Id` 헤더로 memberId 추출 (Gateway가 검증 후 전달)
- [ ] `Idempotency-Key` 헤더 처리 (교차분석/인증자 필터 30P 차감)
- [ ] `X-Internal-Auth` 내부 서비스 인증 헤더 검증
- [ ] `uq_battle_vote` 중복 투표 → `DataIntegrityViolationException` → `BATTLE_ALREADY_VOTED` 변환
- [ ] 정산 완료 표현: `settled_at IS NOT NULL` (status는 CLOSED 유지)
- [ ] idempotency_key 패턴 준수 (ERROR_CODE.md 5-4 기준)
