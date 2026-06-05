# Battle Service 구현 계획

## 구현 순서

```
Feature 1 (Global 설정) ✅
  └── Feature 2 (Battle CRUD) ✅
        ├── Feature 3 (Vote) ✅
        │     └── Feature 5 (Member-Point Client) ✅
        │           └── Feature 6 (RetryQueue) ✅
        │                 └── Feature 7 (Battle 마감·정산 배치) ✅
        ├── Feature 4 (Comment) ✅
        └── Feature 8 (내부 API) ✅
```

---

## Phase 1: 기본 구조 ✅ 완료

| 파일 | 역할 |
|---|---|
| `application.yml` | DB(localhost:3307/battle), JPA, 외부 서비스 URL |
| `global/entity/BaseEntity` | createdAt, updatedAt |
| `global/response/ApiResponse` | 공통 응답 포맷 |
| `global/exception/ErrorCode` | 에러 코드 Enum |
| `global/exception/CustomException` | 도메인 예외 |
| `global/exception/GlobalExceptionHandler` | 전역 예외 핸들러 (NoResourceFoundException 포함) |
| `global/config/JpaConfig` | JPA Auditing 활성화 |
| `global/config/AppConfig` | RestTemplate Bean, @EnableScheduling |
| 모든 Entity / Repository | ERD 기반 |
| 모든 Service / Controller | 완전 구현 |

---

## Phase 2: Feature별 구현 내용

### Feature 2: Battle CRUD ✅

**createBattle 흐름:**
1. `X-Member-Id` 헤더 → memberId 추출
2. `end_at > start_at` AND `end_at > NOW()` 검증 (실패 → `BATTLE_INVALID_PERIOD`)
3. Battle INSERT (`status=PENDING`) — 포인트 차감 없음
4. `BattleCreateResponse` 반환

**getBattle 분기:**
- `status = PENDING or CANCELLED` → `BATTLE_NOT_FOUND` (일반 사용자 비공개)
- `deleted_at IS NOT NULL` → `BATTLE_NOT_FOUND`
- 관리자 내부 API(`/api/v1/battles/{id}/info`)는 모든 상태 허용

**관리자 승인 (approve) 흐름:**
1. `X-Member-Role: ADMIN` 체크 (아니면 `FORBIDDEN`)
2. PENDING 상태 확인 (아니면 `BATTLE_INVALID_STATUS`)
3. `status = ACTIVE`로 UPDATE
4. 생성자(`created_by`)에게 `EARN_BATTLE_APPROVED` 20P 지급 → 실패 시 RetryQueue

---

### Feature 3: 투표 (Vote) ✅

**vote 흐름:**
1. Battle 조회 (PENDING이면 `BATTLE_NOT_FOUND`, CLOSED/CANCELLED이면 `BATTLE_CLOSED`)
2. `start_at > NOW()` → `BATTLE_CLOSED` (투표 시작 전)
3. option A/B 검증 (아니면 `BATTLE_INVALID_OPTION`)
4. `INSERT battle_vote` (UNIQUE 충돌 → `DataIntegrityViolationException` → `BATTLE_ALREADY_VOTED`)
5. 같은 트랜잭션에서 원자적 집계 UPDATE:
   ```sql
   UPDATE battle SET option_a_count = option_a_count + 1, vote_count = vote_count + 1 WHERE id = ?
   ```
6. Member-Point `EARN_VOTE` 10P 지급 → Timeout 시 RetryQueue 적재, 4xx는 로그만
7. 성공 응답 반환 (Point 지급 실패여도 투표 성공 응답)

**getResult 공개 정책:**
```
ACTIVE + 미투표/비회원  → 참여자 수만 노출
ACTIVE + 투표 완료     → 득표 비율 + 참여자 수
CLOSED + 투표 완료     → 전체 결과
CLOSED + 미투표 + 72h 미경과 → 비공개
CLOSED + 미투표 + 72h 경과  → 전체 결과
```

**getCrossResult / getCertifiedResult 흐름 (관리자 전용):**
1. `X-Member-Role: ADMIN` 체크 (아니면 `FORBIDDEN`) — Controller에서 처리
2. CLOSED 상태 확인 (아니면 `BATTLE_RESULT_NOT_AVAILABLE`)
3. 집계 통계 데이터 반환 (리포트 원문 미노출, 포인트 차감 없음)
4. 상세 필드 구현은 별도 Feature로 진행 예정

---

### Feature 4: 댓글 (Comment) ✅

**createComment 흐름:**
1. Battle 조회 (PENDING이면 `BATTLE_NOT_FOUND`, CLOSED/CANCELLED이면 `BATTLE_CLOSED`)
2. content 길이 > 500자 → `BATTLE_COMMENT_TOO_LONG`
3. Comment INSERT
4. Member-Point `EARN_COMMENT` 2P 지급 → Timeout 시 RetryQueue 적재, 4xx는 로그만
5. 성공 응답 반환

**deleteComment 흐름:**
1. Comment 조회 (`deleted_at IS NULL`)
2. 없으면 `BATTLE_COMMENT_NOT_FOUND`
3. `member_id != 요청자` → `BATTLE_COMMENT_FORBIDDEN`
4. `deleted_at = NOW()` UPDATE (soft delete)

---

### Feature 5: Member-Point Client ✅

**사용 중인 엔드포인트:**
- `POST {member-point-url}/api/v1/points/earn` — EARN_VOTE / EARN_COMMENT / EARN_BATTLE_APPROVED
- `POST {member-point-url}/api/v1/points/settlements` — 배치 정산 보상 (EARN_VOTE_WIN)

**미사용:**
- `POST {member-point-url}/api/v1/points/spend` — 교차분석이 관리자 전용 0P로 전환되어 현재 미사용

**실패 처리:**
- Timeout (`ResourceAccessException`) → `EXTERNAL_SERVICE_TIMEOUT` throw → 호출자에서 RetryQueue 적재
- 4xx (`HttpClientErrorException`) → `POINT_INSUFFICIENT` throw → EARN이면 로그만, SPEND이면 전파

**idempotency_key 패턴:**
```
투표 보상:    battle:vote:{battleId}:member:{memberId}
댓글 보상:    battle:comment:{commentId}:member:{memberId}
승리 보상:    battle:settle:{battleId}:member:{memberId}
승인 보상:    battle:approved:{battleId}:member:{memberId}
```

---

### Feature 6: Retry Queue ✅

```java
@Scheduled(fixedDelay = 60000)
public void retryPendingRewards() {
    // retryCount < 3인 PENDING 항목 조회
    // earnPoint 재호출
    // 성공: status = SUCCESS
    // 실패: retryCount++ (3 이상이면 status = FAILED)
}
```

---

### Feature 7: 마감 & 정산 배치 ✅

**BattleCloseScheduler (1분 간격):**
```
status=ACTIVE AND end_at <= NOW() 조회
→ status = CLOSED
→ winning_option = A/B/DRAW 확정
```

**BattleSettleScheduler (1분 간격):**
```
status=CLOSED AND settled_at IS NULL 조회
→ DRAW → settled_at 기록
→ A or B → is_rewarded=false 투표자 순회
  → EARN_VOTE_WIN 지급 (idempotency_key: battle:settle:{battleId}:member:{memberId})
  → 성공: is_rewarded = true
  → 실패: RetryQueue 적재 (existsByIdempotencyKey 중복 방지)
→ 전원 처리 후 settled_at 기록 (status는 CLOSED 유지)
```

---

### Feature 8: 내부 API ✅

**인증:** `X-Internal-Auth` 헤더 값으로 내부 서비스 확인. Gateway에서 외부 요청 차단.

**엔드포인트:**
- `GET /api/v1/battles/{battleId}/votes/raw` → VoteRawResponse (votes 배열 포함)
- `GET /api/v1/battles/comments/{commentId}` → CommentInternalResponse (soft delete이면 NOT_FOUND)
- `GET /api/v1/battles/{battleId}/info` → BattleDetailResponse (PENDING/CANCELLED도 노출)

---

## 남은 작업

| 항목 | 우선순위 | 비고 |
|---|---|---|
| 교차분석/인증자 필터 집계 필드 구현 | 중 | CrossAnalysisResponse, CertifiedResultResponse 상세화 |
| Service 레이어 단위 테스트 | 중 | CLAUDE.md 테스트 기준 참고 |
