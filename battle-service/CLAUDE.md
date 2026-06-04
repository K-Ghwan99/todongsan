# CLAUDE.md — Battle Service

> 루트 `CLAUDE.md`의 공통 지침을 상속한다. 이 파일은 Battle Service에 특화된 컨텍스트를 추가한다.

---

## 공통 코딩 원칙

**생각 먼저, 코딩 나중**
- 가정하지 않는다. 불확실하면 물어본다.
- 여러 해석이 가능하면 선택지를 제시한다.
- 더 단순한 방법이 있으면 먼저 말한다.

**최소한의 코드**
- 요청된 기능만 구현한다. 추측성 기능 추가 금지.
- 단일 사용 코드에 추상화 금지.
- 200줄로 쓸 수 있는 걸 50줄로 쓴다.

**외과적 수정**
- 변경해야 할 것만 건드린다.
- 기존 스타일을 유지한다 (내가 다르게 하고 싶어도).
- 내 변경으로 생긴 dead code만 제거한다.

---

## 내 서비스 컨텍스트

- **서비스명:** Battle Service
- **포트:** 8082
- **패키지 루트:** `com.todongsan.battle_service`
- **담당 문서:**
  - `docs/battle/BATTLE_ERD.md` (ERD 최신본)
  - `docs/battle/BATTLE_API_SPEC.md` (API 명세 최신본)
  - `docs/battle/BATTLE_ERROR_CODE.md` (도메인 에러 코드)
  - `battle-service/BATTLE_CONVENTION.md` (Battle 도메인 특수사항)

---

## 핵심 도메인 테이블

| 테이블 | 역할 | 주요 제약 |
|---|---|---|
| `battle` | Battle 주제, 상태, 투표 기간, 결과/보상 | soft delete(`deleted_at`), `idx_battle_status_end` |
| `battle_vote` | 개별 투표 기록 | `uq_battle_vote(battle_id, member_id)` UNIQUE |
| `comment` | Battle 댓글 (단일 depth) | soft delete(`deleted_at`), `idx_comment_battle` |
| `point_reward_retry_queue` | Point 지급 재시도 큐 | `idempotency_key` UNIQUE |

---

## 핵심 비즈니스 제약 (코드 작성 전 반드시 숙지)

```
[Battle 상태 전이]
- PENDING → ACTIVE: 관리자 승인만 가능
- PENDING → CANCELLED: 관리자 거절
- ACTIVE → CLOSED: end_at 도달 (BattleCloseScheduler, 1분 간격)
- ACTIVE → CANCELLED: 관리자 강제 취소
- SETTLED 상태는 없다. 정산 완료 = settled_at IS NOT NULL (status는 CLOSED 유지)

[일반 사용자 접근]
- PENDING/CANCELLED Battle 조회 → BATTLE_NOT_FOUND (상태 노출 금지)
- 조회 허용: ACTIVE, CLOSED 상태만
- start_at은 status 전이를 트리거하지 않는다.
  실제 투표 가능 여부는 service에서 start_at <= NOW() 체크

[투표]
- 1인 1표, 변경/취소 불가
- 중복 투표: uq_battle_vote DB 제약 → DataIntegrityViolationException → BATTLE_ALREADY_VOTED
- 집계: 원자적 UPDATE (option_a_count = option_a_count + 1) — 비관적 락 불필요

[댓글]
- 단일 depth (대댓글 없음)
- CLOSED/CANCELLED 상태에서 작성 불가 (BATTLE_CLOSED)
- soft delete만 사용, 실제 삭제 금지
- 본인 댓글만 삭제 가능

[교차분석 / 인증자 필터 조회]
- 관리자(ROLE_ADMIN) 전용. 내부 서비스도 접근 불가.
- 리포트 원문 반환 금지. 집계 통계(비율, 분포)만 응답.
- 포인트 차감 없음 (0P).

[정산]
- BattleSettleScheduler가 CLOSED + settled_at IS NULL인 Battle 순회
- 무승부(DRAW): 보상 없이 settled_at만 기록
- 승자 보상: is_rewarded = false인 투표자만 처리 (멱등성)
- idempotency_key = "battle:settle:{battleId}:member:{memberId}"
- 실패 시 point_reward_retry_queue 적재, existsByIdempotencyKey로 중복 방지
```

---

## 포인트 정책 요약

| 액션 | type | 금액 | 실패 처리 |
|---|---|---:|---|
| Battle 주제 승인 보상 | `EARN_BATTLE_APPROVED` | +20P | RetryQueue 적재 |
| 투표 참여 보상 | `EARN_VOTE` | +10P | RetryQueue 적재 |
| 댓글 작성 보상 | `EARN_COMMENT` | +2P | RetryQueue 적재 |
| 교차분석/인증자 필터 조회 | — | 0P | 일반 사용자 FORBIDDEN |
| 승리 보상 (정산) | `EARN_VOTE_WIN` | +10P | RetryQueue 적재 |

**idempotency_key 패턴:**

| 상황 | 패턴 |
|---|---|
| 투표 보상 | `battle:vote:{battleId}:member:{memberId}` |
| 댓글 보상 | `battle:comment:{commentId}:member:{memberId}` |
| 승인 보상 | `battle:approved:{battleId}:member:{memberId}` |
| 승리 보상 | `battle:settle:{battleId}:member:{memberId}` |

---

## 연계 서비스

### 내가 호출하는 서비스

| 서비스 | 목적 | 주의사항 |
|---|---|---|
| Member-Point | `earnPoint()` — 투표/댓글/승인 보상 지급 | Timeout 시 RetryQueue 적재, 4xx는 로그만 |
| Member-Point | `settlePoints()` — 정산 보상 배치 지급 | is_rewarded 플래그로 멱등성 관리 |
| Member-Point | `spendPoint()` — 포인트 차감 | 실패 시 즉시 예외 전파, RetryQueue 없음 |

### 나를 호출하는 서비스

| 서비스 | 엔드포인트 | 목적 |
|---|---|---|
| Insight Service | `GET /api/v1/battles/{battleId}/votes/raw` | AI 분석용 투표 원본 데이터 |
| Insight Service | `GET /api/v1/battles/comments/{commentId}` | COMMENT 방문 인증 검증 |
| Insight Service | `GET /api/v1/battles/{battleId}/info` | Battle 메타데이터 조회 |

> 내부 API는 `X-Internal-Auth` 헤더로 인증한다. 게이트웨이에서 외부 요청은 차단된다.

---

## 헤더 처리

| 헤더 | 용도 | 세팅 주체 |
|---|---|---|
| `X-Member-Id` | memberId 추출 | Gateway (JWT 검증 후 전달) |
| `X-Internal-Auth` | 내부 서비스 인증 | Insight 등 내부 서비스 |
| `Idempotency-Key` | 포인트 차감 중복 방지 | 클라이언트 |

---

## 이 서비스의 성공 기준

### 기본 기동

```
1. 애플리케이션 기동 → 검증: GET /swagger-ui/index.html → 200 OK
2. DB 연결 → 검증: MySQL localhost:3307/battle (docker-compose)
3. JPA 스키마 → 검증: 4개 테이블 존재 (battle, battle_vote, comment, point_reward_retry_queue)
```

### API별 성공 기준

```
[Battle]
- POST /api/v1/battles → 201, status=PENDING
- GET /api/v1/battles?status=ACTIVE → ACTIVE 목록만 반환 (PENDING/CANCELLED 미포함)
- GET /api/v1/battles/{id} (PENDING 상태) → 404 BATTLE_NOT_FOUND
- PATCH /api/v1/battles/{id}/approve → PENDING → ACTIVE 전이 확인

[투표]
- POST /api/v1/battles/{id}/votes → battle_vote 생성 + option_a_count +1 원자적 확인
- POST 중복 → 409 BATTLE_ALREADY_VOTED
- CLOSED 상태 투표 → 409 BATTLE_CLOSED
- start_at 미도달 → 409 BATTLE_CLOSED

[댓글]
- POST /api/v1/battles/{id}/comments → 201, 보상 RetryQueue 적재 확인
- DELETE (본인 아님) → 403 BATTLE_COMMENT_FORBIDDEN
- soft delete 확인: deleted_at IS NOT NULL

[교차분석]
- GET /api/v1/battles/{id}/result/cross (일반 사용자) → 403 FORBIDDEN
- GET /api/v1/battles/{id}/result/cross (관리자) → 200, 집계 통계 응답

[정산]
- BattleSettleScheduler 실행 → is_rewarded=true, settled_at 기록 확인
- 무승부(DRAW) → 보상 없이 settled_at만 기록
- 재실행 시 is_rewarded=true인 건 skip 확인 (멱등성)
```

---

## 이 서비스에서 하지 않는 것

```
✗ Point를 직접 DB에서 관리하지 않는다. 반드시 Member-Point Service REST 호출.
✗ battle, comment 테이블을 hard delete하지 않는다. 항상 soft delete.
✗ 비관적 락(pessimistic lock)을 사용하지 않는다. 원자적 UPDATE로 처리.
✗ SETTLED 상태를 만들지 않는다. settled_at IS NOT NULL로 판단.
✗ 교차분석/인증자 필터 API에서 개별 투표 데이터(원문)를 반환하지 않는다.
✗ start_at을 상태 전이 트리거로 사용하지 않는다. 서비스 레이어에서 체크.
✗ RetryQueue를 SPEND(차감) 액션에 사용하지 않는다. EARN(지급)만 해당.
```

---

## 코드 작성 원칙

### 레이어 책임 경계

```
Controller
- HTTP 요청/응답 변환만 담당한다.
- 헤더(X-Member-Id, X-Internal-Auth)에서 값 추출 → Service 전달.
- @Valid로 입력 검증, Service 호출, DTO 반환이 전부다.

Service
- 트랜잭션 경계를 소유한다. (@Transactional)
- 비즈니스 제약(상태 전이, 기간 검증 등)은 반드시 여기서 처리한다.
- @Transactional 블록 안에서 외부 REST 호출 금지.
  → 외부 호출은 트랜잭션 밖에서 먼저 처리하고, 결과로 DB를 업데이트한다.

Repository
- JPA 쿼리 정의만 담당한다.
- 비즈니스 판단 로직을 포함하지 않는다.

외부 REST 호출
- MemberPointClient 클래스로 분리. Service에서 직접 호출하지 않는다.
```

### 금지 패턴

```
✗ Entity를 Controller 응답으로 직접 노출 → DTO로 변환 필수
✗ Service에서 다른 도메인 Service를 직접 주입 (순환 의존)
✗ 매직 넘버/문자열 → 상수 또는 Enum으로 정의
✗ 빈 catch 블록 → 최소한 log.warn/error 기록
✗ null 반환 → Optional 또는 CustomException
```

### 예외 처리

```
비즈니스 예외: throw new CustomException(ErrorCode.XXX)
  → GlobalExceptionHandler가 ApiResponse 에러 형태로 변환

외부 서비스 장애:
  - EARN: catch → RetryQueue 적재 (Timeout/5xx), 로그만 (4xx)
  - SPEND: 예외 그대로 전파

DataIntegrityViolationException:
  - uq_battle_vote 충돌 → GlobalExceptionHandler에서 BATTLE_ALREADY_VOTED 변환
```

---

## 테스트 작성 기준

기능 구현 후 **커밋 전** 반드시 Service 레이어 단위 테스트를 작성한다.

**커버해야 할 케이스:**

```
[정상 케이스]
- 유효한 입력 + 모든 조건 충족 시 기대 결과 반환

[비즈니스 예외 케이스]
- 상태 전이 불가 → BATTLE_INVALID_STATUS
- PENDING Battle 일반 사용자 접근 → BATTLE_NOT_FOUND
- CLOSED 상태 투표 → BATTLE_CLOSED
- start_at 미도달 투표 → BATTLE_CLOSED
- 권한 없음 교차분석 → FORBIDDEN

[멱등성 케이스]
- 정산 재실행 시 is_rewarded=true 건 skip 확인
- existsByIdempotencyKey 중복 방지 확인
```

**테스트 작성 방식:**

```java
@ExtendWith(MockitoExtension.class)
class BattleServiceImplTest {

    @InjectMocks BattleServiceImpl service;
    @Mock BattleRepository battleRepository;
    @Mock MemberPointClient memberPointClient;

    @Test
    @DisplayName("PENDING 상태 Battle 일반 사용자 조회 → BATTLE_NOT_FOUND")
    void getBattle_pendingStatus_throwsBattleNotFound() {
        // given / when / then
    }

    @Test
    @DisplayName("start_at 미도달 투표 → BATTLE_CLOSED")
    void vote_beforeStartAt_throwsBattleClosed() {
        // given / when / then
    }
}
```

**네이밍 규칙:**

```
메서드명: 메서드명_상황_기대결과
  예) approveBattle_notPendingStatus_throwsInvalidStatus
      vote_alreadyVoted_throwsAlreadyVoted
      settle_draw_noRewardSettledAtRecorded

@DisplayName: 한국어로 의도 명확히 작성
```

---

## 개발 워크플로우

### 기능 단위 브랜치

```
feat/battle-crud         - Battle 등록/조회/관리자 상태 전환
feat/vote                - 투표 참여, 결과 조회
feat/comment             - 댓글 작성/조회/삭제
feat/cross-analysis      - 교차분석/인증자 필터 (관리자 전용)
feat/internal-api        - Insight 연계 내부 API (votes/raw, comments/{id}, info)
feat/settle-scheduler    - BattleSettleScheduler, BattleCloseScheduler
feat/retry-scheduler     - RetryScheduler (포인트 재시도)
feat/point-reward        - Member-Point 연동 (EARN/SPEND)
```

### 구현 순서

```
1. Entity / Enum 정의
2. Repository (JPA 메서드)
3. Service (비즈니스 로직 + 트랜잭션)
4. Controller (HTTP 매핑 + 헤더 처리)
5. 단위 테스트 작성
6. ./gradlew test 전체 통과 확인
7. 커밋
```

### 커밋 전 체크리스트

```
[ ] docs/battle/ 문서와 구현이 일치하는가?
[ ] PENDING/CANCELLED Battle 접근 시 BATTLE_NOT_FOUND 처리했는가?
[ ] soft delete 조회에 deleted_at IS NULL 조건 포함했는가?
[ ] 외부 REST 호출이 @Transactional 블록 밖에 있는가?
[ ] EARN 실패 시 RetryQueue 적재, SPEND 실패 시 예외 전파인가?
[ ] idempotency_key 패턴이 BATTLE_CONVENTION.md 1-2 기준과 일치하는가?
[ ] Service 레이어 테스트가 작성되어 있는가?
[ ] ./gradlew test 전체 통과했는가?
```