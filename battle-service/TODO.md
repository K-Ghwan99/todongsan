# Battle Service TODO

## Feature 1: 기본 프로젝트 구조 (Global 설정) ✅ 완료

- [x] `application.yml` — DB(localhost:3307/battle), JPA, 외부 서비스 URL 설정
- [x] `global/entity/BaseEntity.java` — `createdAt`, `updatedAt` AuditingEntity
- [x] `global/response/ApiResponse.java` — 공통 응답 포맷
- [x] `global/exception/ErrorCode.java` — 공통 + Battle 도메인 에러 코드 Enum
- [x] `global/exception/CustomException.java`
- [x] `global/exception/GlobalExceptionHandler.java` — `NoResourceFoundException` 포함
- [x] `global/config/JpaConfig.java` — `@EnableJpaAuditing`
- [x] `global/config/AppConfig.java` — `RestTemplate` Bean, `@EnableScheduling`

---

## Feature 2: Battle CRUD + 관리자 상태 전환 ✅ 완료

- [x] `battle/entity/BattleStatus.java` — `PENDING / ACTIVE / CLOSED / CANCELLED`
- [x] `battle/entity/Battle.java`
- [x] `battle/repository/BattleRepository.java`
- [x] `battle/dto/request/BattleCreateRequest.java`
- [x] `battle/dto/response/BattleCreateResponse.java`
- [x] `battle/dto/response/BattleDetailResponse.java`
- [x] `battle/dto/response/BattleListResponse.java`
- [x] `battle/dto/response/BattleStatusResponse.java`
- [x] `battle/service/BattleService.java`
- [x] `battle/service/BattleServiceImpl.java`
  - [x] `createBattle` — PENDING 생성
  - [x] `getBattles` — ACTIVE/CLOSED만 노출
  - [x] `getBattle` — PENDING/CANCELLED는 BATTLE_NOT_FOUND
  - [x] `approveBattle` — PENDING → ACTIVE + EARN_BATTLE_APPROVED 20P
  - [x] `rejectBattle` — PENDING → CANCELLED
  - [x] `cancelBattle` — ACTIVE → CANCELLED
- [x] `battle/controller/BattleController.java`
- [x] `battle/controller/BattleAdminController.java` — `X-Member-Role: ADMIN` 체크

---

## Feature 3: 투표 (Vote) ✅ 완료

- [x] `vote/entity/BattleVote.java`
- [x] `vote/repository/BattleVoteRepository.java`
- [x] `vote/dto/request/VoteRequest.java`
- [x] `vote/dto/response/VoteResponse.java`
- [x] `vote/dto/response/VoteResultResponse.java`
- [x] `vote/dto/response/VoteRawResponse.java`
- [x] `vote/dto/response/CrossAnalysisResponse.java` — 필드 정의 미완 (아래 참고)
- [x] `vote/dto/response/CertifiedResultResponse.java` — 필드 정의 미완 (아래 참고)
- [x] `vote/service/VoteService.java`
- [x] `vote/service/VoteServiceImpl.java`
  - [x] `vote` — 투표 + 집계 UPDATE + EARN_VOTE 10P + RetryQueue
  - [x] `getResult` — 공개 정책 분기 (투표여부/72h)
  - [x] `getCrossResult` — 관리자 전용, CLOSED 체크 (집계 필드 구현 예정)
  - [x] `getCertifiedResult` — 관리자 전용, CLOSED 체크 (집계 필드 구현 예정)
  - [x] `getRawVotes` — 내부 Insight 전용
- [x] `vote/controller/VoteController.java` — cross/certified는 `X-Member-Role: ADMIN` 전용

### 미완 (집계 데이터 구현 필요)
- [ ] `CrossAnalysisResponse` 상세 필드 — 옵션별 비율, 성별/연령대 분포 등
- [ ] `CertifiedResultResponse` 상세 필드 — 방문 인증자 기준 집계 통계
- [ ] `VoteServiceImpl.getCrossResult` — 실제 집계 데이터 조회 로직
- [ ] `VoteServiceImpl.getCertifiedResult` — 실제 집계 데이터 조회 로직

---

## Feature 4: 댓글 (Comment) ✅ 완료

- [x] `comment/entity/Comment.java`
- [x] `comment/repository/CommentRepository.java`
- [x] `comment/dto/request/CommentCreateRequest.java`
- [x] `comment/dto/response/CommentResponse.java`
- [x] `comment/dto/response/CommentInternalResponse.java`
- [x] `comment/service/CommentService.java`
- [x] `comment/service/CommentServiceImpl.java`
  - [x] `createComment` — 저장 + EARN_COMMENT 2P + RetryQueue
  - [x] `getComments` — soft delete 제외 페이징
  - [x] `deleteComment` — 본인 확인 + soft delete
  - [x] `getCommentInternal` — 내부 Insight 전용
- [x] `comment/controller/CommentController.java`

---

## Feature 5: Member-Point 서비스 REST 클라이언트 ✅ 완료

- [x] `client/MemberPointClient.java`
  - [x] `earnPoint` — EARN_VOTE / EARN_COMMENT / EARN_BATTLE_APPROVED
  - [x] `spendPoint` — (현재 교차분석 0P 전환으로 미사용)
  - [x] `settlePoints` — 배치 정산 보상
- [x] `client/dto/PointEarnRequest.java`
- [x] `client/dto/PointSpendRequest.java`
- [x] `client/dto/PointSettleRequest.java`

---

## Feature 6: Point 지급 실패 재시도 큐 (RetryQueue) ✅ 완료

- [x] `retry/entity/RetryStatus.java`
- [x] `retry/entity/PointRewardRetryQueue.java`
- [x] `retry/repository/PointRewardRetryQueueRepository.java`
- [x] `retry/scheduler/RetryScheduler.java` — 1분 간격, 최대 3회 재시도

---

## Feature 7: Battle 마감 & 정산 배치 ✅ 완료

- [x] `battle/scheduler/BattleCloseScheduler.java` — ACTIVE → CLOSED + winning_option 확정
- [x] `battle/scheduler/BattleSettleScheduler.java`
  - [x] DRAW → settled_at 기록
  - [x] 승자 → is_rewarded=false 투표자에게 EARN_VOTE_WIN 지급
  - [x] 실패 → RetryQueue 적재 (existsByIdempotencyKey 중복 방지)

---

## Feature 8: 내부 API (Insight Service 전용) ✅ 완료

- [x] `battle/controller/BattleInternalController.java`
  - [x] `GET /api/v1/battles/{battleId}/votes/raw`
  - [x] `GET /api/v1/battles/comments/{commentId}`
  - [x] `GET /api/v1/battles/{battleId}/info`

---

## 공통 체크리스트

- [x] `X-Member-Id` 헤더로 memberId 추출 (Gateway 전달)
- [x] `X-Member-Role` 헤더로 관리자 체크 (ADMIN)
- [x] `X-Internal-Auth` 내부 서비스 인증 헤더 검증
- [x] `uq_battle_vote` 중복 투표 → `BATTLE_ALREADY_VOTED` 변환
- [x] 정산 완료 표현: `settled_at IS NOT NULL` (status는 CLOSED 유지)
- [x] idempotency_key 패턴 준수 (BATTLE_CONVENTION.md 1-2 기준)
- [x] 교차분석/인증자 필터 — 관리자 전용, 포인트 차감 없음, 집계 통계만 응답
- [ ] 단위 테스트 (Service 레이어) — CLAUDE.md 테스트 작성 기준 참고
