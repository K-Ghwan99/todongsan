# Battle Service 구현 계획

## 구현 순서

기능 간 의존성 때문에 아래 순서대로 구현한다.

```
Feature 1 (Global 설정)
  └── Feature 2 (Battle CRUD)
        ├── Feature 3 (Vote)
        │     └── Feature 5 (Member-Point Client)
        │           └── Feature 6 (RetryQueue)
        │                 └── Feature 7 (Battle 마감·정산 배치)
        ├── Feature 4 (Comment)
        │     └── Feature 5 (Member-Point Client)
        └── Feature 8 (내부 API)
```

---

## Phase 1: 기본 구조 (현재 완료)

아래 파일들은 이미 생성됨.

| 파일 | 역할 |
|---|---|
| `application.yml` | DB, JPA, 외부 서비스 URL |
| `global/entity/BaseEntity` | createdAt, updatedAt |
| `global/response/ApiResponse` | 공통 응답 포맷 |
| `global/exception/ErrorCode` | 에러 코드 Enum |
| `global/exception/CustomException` | 도메인 예외 |
| `global/exception/GlobalExceptionHandler` | 전역 예외 핸들러 |
| `global/config/JpaConfig` | JPA Auditing 활성화 |
| `global/config/AppConfig` | RestTemplate Bean |
| 모든 Entity | ERD 기반 JPA 엔티티 |
| 모든 Repository | JPA Repository 인터페이스 |
| 모든 Service 인터페이스 | 메서드 시그니처 정의 |
| 모든 ServiceImpl (stub) | 미구현 메서드만 선언 |
| 모든 Controller (stub) | 라우팅만 정의 |

---

## Phase 2: Feature별 구현 가이드

### Feature 2: Battle CRUD

**createBattle 흐름:**
1. 요청 헤더 `X-Member-Id` → memberId 추출
2. `end_at > start_at` AND `end_at > NOW()` 검증 (실패 → `BATTLE_INVALID_PERIOD`)
3. Battle INSERT (`status=PENDING`) — 포인트 차감 없음
4. `BattleCreateResponse` 반환

**getBattle 분기:**
- `status = PENDING or CANCELLED` → `BATTLE_NOT_FOUND` 반환 (일반 사용자 비공개)
- `deleted_at IS NOT NULL` → `BATTLE_NOT_FOUND`
- 관리자 전용 내부 API는 모든 상태 허용 (`/api/v1/battles/{id}/info`)

**관리자 승인 (approve) 흐름:**
1. PENDING 상태 확인 (아니면 `BATTLE_INVALID_STATUS`)
2. `status = ACTIVE`로 UPDATE
3. 생성자(`created_by`)에게 `EARN_BATTLE_APPROVED` 20P 지급
4. `BattleStatusResponse` 반환

---

### Feature 3: 투표 (Vote)

**vote 흐름:**
1. Battle 조회 (PENDING이면 `BATTLE_NOT_FOUND`, CLOSED/CANCELLED이면 `BATTLE_CLOSED`)
2. `start_at > NOW()` → `BATTLE_CLOSED` (투표 시작 전)
3. option A/B 검증 (아니면 `BATTLE_INVALID_OPTION`)
4. `INSERT battle_vote` (UNIQUE 충돌 → `BATTLE_ALREADY_VOTED`)
5. 같은 트랜잭션에서:
   ```sql
   UPDATE battle SET option_a_count = option_a_count + 1, vote_count = vote_count + 1 WHERE id = ?
   ```
6. Member-Point `EARN_VOTE` 10P 지급 (실패 → RetryQueue 적재)
7. 성공 응답 반환 (Point 지급 실패여도 투표 성공 응답)

**getResult 공개 정책:**
```
ACTIVE + 미투표 + 비회원  → 참여자 수만
ACTIVE + 투표 완료       → 득표 비율 + 참여자 수
CLOSED + 투표 완료       → 전체 결과 + 기본 교차분석
CLOSED + 미투표/비회원 + 72h 미경과 → 비공개
CLOSED + 미투표/비회원 + 72h 경과  → 전체 결과 비율만
```

**getCrossResult / getCertifiedResult 흐름:**
1. CLOSED 상태 확인 (아니면 `BATTLE_RESULT_NOT_AVAILABLE`)
2. `Idempotency-Key` 헤더 확인 (없으면 `IDEMPOTENCY_KEY_REQUIRED`)
3. Member-Point `SPEND_INSIGHT` 30P 차감 (실패 → `POINT_INSUFFICIENT`)
4. 교차분석 or 인증자 필터 데이터 조회 및 반환

---

### Feature 4: 댓글 (Comment)

**createComment 흐름:**
1. Battle 조회 (PENDING이면 `BATTLE_NOT_FOUND`, CLOSED/CANCELLED이면 `BATTLE_CLOSED`)
2. content 길이 검증 > 500자 → `BATTLE_COMMENT_TOO_LONG`
3. Comment INSERT
4. Member-Point `EARN_COMMENT` 2P 지급 (실패 → RetryQueue 적재)
5. 성공 응답 반환

**deleteComment 흐름:**
1. Comment 조회 (`deleted_at IS NULL`)
2. 없으면 `BATTLE_COMMENT_NOT_FOUND`
3. `member_id != 요청자` → `BATTLE_COMMENT_FORBIDDEN`
4. `deleted_at = NOW()` UPDATE (soft delete)

---

### Feature 5: Member-Point Client

**주요 엔드포인트:**
- `POST {member-point-url}/api/v1/points/earn` — EARN_VOTE, EARN_COMMENT, EARN_BATTLE_APPROVED
- `POST {member-point-url}/api/v1/points/spend` — SPEND_BATTLE_CREATE, SPEND_INSIGHT
- `POST {member-point-url}/api/v1/points/settlements` — 배치 정산 보상

**실패 처리:**
- 5xx / Timeout → `EXTERNAL_SERVICE_TIMEOUT` throw → 호출자에서 RetryQueue 적재
- 4xx → 즉시 예외 전파 (RetryQueue 적재 안 함)

**idempotency_key 패턴 (ERROR_CODE.md 5-4):**
```
투표 보상:    battle:vote:{battleId}:member:{memberId}
댓글 보상:    battle:comment:{commentId}:member:{memberId}
승리 보상:    battle:settle:{battleId}:member:{memberId}
승인 보상:    battle:approved:{battleId}:member:{memberId}
```

---

### Feature 6: Retry Queue

**RetryScheduler 구현:**
```java
@Scheduled(fixedDelay = 60000)
public void retry() {
    List<PointRewardRetryQueue> pending = repo.findByStatusAndRetryCountLessThan(PENDING, 3);
    for (PointRewardRetryQueue q : pending) {
        try {
            memberPointClient.earnPoint(...);
            q.setStatus(SUCCESS);
        } catch (Exception e) {
            q.incrementRetryCount();
            if (q.getRetryCount() >= 3) q.setStatus(FAILED);
        }
        repo.save(q);
    }
}
```

---

### Feature 7: 마감 & 정산 배치

**BattleCloseScheduler:**
```
1분마다 실행
SELECT * FROM battle WHERE status = 'ACTIVE' AND end_at <= NOW()
→ status = 'CLOSED'
→ winning_option = option_a_count > option_b_count ? 'A' : option_b_count > option_a_count ? 'B' : 'DRAW'
```

**BattleSettleScheduler:**
```
1분마다 실행
SELECT * FROM battle WHERE status = 'CLOSED' AND settled_at IS NULL
→ winning_option = 'DRAW' → settled_at 기록, 종료
→ winning_option = 'A' or 'B' → 승자 투표자 (is_rewarded=false) 조회
  → Member-Point EARN_VOTE_WIN 2P 지급
  → 성공: is_rewarded = true
  → 실패: RetryQueue 적재
  → 전원 처리 완료 시 settled_at 기록
```

---

### Feature 8: 내부 API

**인증:** `X-Internal-Auth` 헤더 존재 여부로 내부 서비스 확인 (MVP 수준).
Gateway에서 외부 요청은 해당 경로 차단.

**엔드포인트:**
- `GET /api/v1/battles/{battleId}/votes/raw` → VoteRawResponse (모든 vote 레코드 포함)
- `GET /api/v1/battles/comments/{commentId}` → CommentInternalResponse (soft delete됐으면 NOT_FOUND)
- `GET /api/v1/battles/{battleId}/info` → BattleDetailResponse (PENDING/CANCELLED도 노출)

---

## 패키지 구조 (최종)

```
com.todongsan.battle_service
├── BattleServiceApplication.java
├── global
│   ├── config
│   │   ├── AppConfig.java          # RestTemplate Bean
│   │   └── JpaConfig.java          # @EnableJpaAuditing
│   ├── entity
│   │   └── BaseEntity.java         # createdAt, updatedAt
│   ├── exception
│   │   ├── CustomException.java
│   │   ├── ErrorCode.java
│   │   └── GlobalExceptionHandler.java
│   └── response
│       └── ApiResponse.java
├── battle
│   ├── controller
│   │   ├── BattleAdminController.java
│   │   ├── BattleController.java
│   │   └── BattleInternalController.java
│   ├── dto
│   │   ├── request
│   │   │   └── BattleCreateRequest.java
│   │   └── response
│   │       ├── BattleCreateResponse.java
│   │       ├── BattleDetailResponse.java
│   │       ├── BattleListResponse.java
│   │       └── BattleStatusResponse.java
│   ├── entity
│   │   ├── Battle.java
│   │   └── BattleStatus.java
│   ├── repository
│   │   └── BattleRepository.java
│   ├── scheduler
│   │   ├── BattleCloseScheduler.java
│   │   └── BattleSettleScheduler.java
│   └── service
│       ├── BattleService.java
│       └── BattleServiceImpl.java
├── vote
│   ├── controller
│   │   └── VoteController.java
│   ├── dto
│   │   ├── request
│   │   │   └── VoteRequest.java
│   │   └── response
│   │       ├── CertifiedResultResponse.java
│   │       ├── CrossAnalysisResponse.java
│   │       ├── VoteRawResponse.java
│   │       ├── VoteResponse.java
│   │       └── VoteResultResponse.java
│   ├── entity
│   │   └── BattleVote.java
│   ├── repository
│   │   └── BattleVoteRepository.java
│   └── service
│       ├── VoteService.java
│       └── VoteServiceImpl.java
├── comment
│   ├── controller
│   │   └── CommentController.java
│   ├── dto
│   │   ├── request
│   │   │   └── CommentCreateRequest.java
│   │   └── response
│   │       ├── CommentInternalResponse.java
│   │       └── CommentResponse.java
│   ├── entity
│   │   └── Comment.java
│   ├── repository
│   │   └── CommentRepository.java
│   └── service
│       ├── CommentService.java
│       └── CommentServiceImpl.java
├── retry
│   ├── entity
│   │   ├── PointRewardRetryQueue.java
│   │   └── RetryStatus.java
│   ├── repository
│   │   └── PointRewardRetryQueueRepository.java
│   └── scheduler
│       └── RetryScheduler.java
└── client
    ├── MemberPointClient.java
    └── dto
        ├── PointEarnRequest.java
        ├── PointSpendRequest.java
        └── PointSettleRequest.java
```
