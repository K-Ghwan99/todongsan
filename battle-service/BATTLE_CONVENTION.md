# BATTLE_CONVENTION.md

> Battle Service 개발자 전용 규칙. `CONVENTION.md`와 `BATTLE_ERROR_CODE.md`를 베이스로 Battle 도메인 특수사항을 정리한다.

---

## 1. 포인트 정책

### 1-1. 액션별 포인트

| 액션 | PointHistoryType |        금액 | 처리 방식 |
|---|---|----------:|---|
| Battle 주제 생성 | — | 포인트 차감 없음 | — |
| Battle 주제 승인 보상 | `EARN_BATTLE_APPROVED` |    20P 지급 | 실패 시 RetryQueue 적재 |
| 투표 참여 보상 | `EARN_VOTE` |    10P 지급 | 실패 시 RetryQueue 적재 |
| 댓글 작성 보상 | `EARN_COMMENT` |     2P 지급 | 실패 시 RetryQueue 적재 |
| 교차분석 / 인증자 필터 조회 | — | 0P (관리자/내부 전용, 포인트 차감 없음) | 일반 사용자 접근 불가 (`FORBIDDEN`) |
| 승리 진영 보상 (정산) | `EARN_VOTE_WIN` |    10P 지급 | BattleSettleScheduler에서 처리 |

> `SPEND_*`는 RetryQueue 대상이 아니다. 실패 시 작ㅊ업 자체를 거부해야 한다.

### 1-2. idempotency_key 패턴

`BATTLE_ERROR_CODE.md` 5-4 기준. 모두 소문자 콜론 구분 형식.

| 상황 | 패턴 |
|---|---|
| 투표 보상 | `battle:vote:{battleId}:member:{memberId}` |
| 댓글 보상 | `battle:comment:{commentId}:member:{memberId}` |
| 승인 보상 | `battle:approved:{battleId}:member:{memberId}` |
| 승리 보상 | `battle:settle:{battleId}:member:{memberId}` |
| 주제 생성권 차감 | 미사용 (포인트 차감 없음) |

---

## 2. Member-Point 연계 처리 원칙

### 2-1. EARN (보상 지급)

- `MemberPointClient.earnPoint()` 호출
- `EXTERNAL_SERVICE_TIMEOUT`: `PointRewardRetryQueue`에 적재, 활동(투표/댓글/승인)은 성공 응답 유지
- 4xx (`POINT_INSUFFICIENT` 등): 로그만 남김, 관리자 수동 보정 대상 (RetryQueue 미적재)

```java
try {
    memberPointClient.earnPoint(request);
} catch (CustomException e) {
    if (e.getErrorCode() == ErrorCode.EXTERNAL_SERVICE_TIMEOUT) {
        retryQueueRepository.save(PointRewardRetryQueue.builder()
                .memberId(...)
                .referenceType("BATTLE")
                .referenceId(battleId)
                .type("EARN_VOTE") // 타입에 맞게
                .amount(...)
                .idempotencyKey(idempotencyKey)
                .build());
    }
}
```

### 2-2. SPEND (포인트 차감)

- `MemberPointClient.spendPoint()` 호출
- 실패 시 예외를 그대로 전파한다 (RetryQueue 적재 없음)
- 검증 실패 전에는 절대 호출하지 않는다 (비즈니스 검증 → 포인트 차감 순서 준수)

---

## 3. RetryQueue 처리 규칙

- `RetryScheduler`가 1분 간격으로 `status=PENDING`, `retry_count < 3`인 항목 재시도
- 3회 초과 시 `status=FAILED`, 관리자 수동 보정 대상
- `idempotency_key`가 이미 있는 경우 RetryQueue에 중복 적재하지 않는다 (`existsByIdempotencyKey` 체크)
- 재시도 시 `earnPoint`로 통일한다 (EARN_VOTE_WIN은 BattleSettleScheduler가 별도 처리)

---

## 4. 상태 전이 요약

```
PENDING → ACTIVE: 관리자 승인 (approve)
PENDING → CANCELLED: 관리자 거절 (reject)
ACTIVE → CLOSED: end_at 도달 (BattleCloseScheduler, 1분 간격)
ACTIVE → CANCELLED: 관리자 강제 취소 (cancel)
```

- `SETTLED` 상태는 없다. 정산 완료는 `settled_at IS NOT NULL`로 판단
- `start_at`은 status 전이를 트리거하지 않는다. service 레이어에서 `start_at <= NOW()` 체크

### 관련 에러 코드

| 상황 | ErrorCode |
|---|---|
| PENDING/CANCELLED Battle에 일반 사용자 접근 | `BATTLE_NOT_FOUND` |
| 투표 시 CLOSED/CANCELLED 또는 start_at 미도달 | `BATTLE_CLOSED` |
| 댓글 시 CLOSED/CANCELLED | `BATTLE_CLOSED` |
| 상태 전이 불가 (예: 이미 ACTIVE인 Battle 승인 시도) | `BATTLE_INVALID_STATUS` |

---

## 5. 일반 사용자 vs 관리자 API 접근 차이

| 구분 | 일반 사용자 | 관리자 | 내부 서비스 |
|---|---|---|---|
| PENDING Battle 조회 | `BATTLE_NOT_FOUND` | 정상 응답 | 정상 응답 |
| CANCELLED Battle 조회 | `BATTLE_NOT_FOUND` | 정상 응답 | 정상 응답 |
| 교차분석 / 인증자 필터 조회 | `FORBIDDEN` | 집계 통계만 응답 (리포트 원문 미노출) | 접근 불가 |
| 인증 | `X-Member-Id` 헤더 | `X-Member-Id` 헤더 + ROLE_ADMIN | `X-Internal-Auth` 헤더 |

### 5-1. 교차분석 / 인증자 필터 조회 규칙

- **관리자 전용**: `ROLE_ADMIN`이 없으면 `FORBIDDEN` 반환, 내부 서비스(`X-Internal-Auth`)도 접근 불가
- **집계 데이터만 노출**: 리포트 원문(개별 응답 데이터)은 반환하지 않고, 분석 결과를 가공한 통계값만 응답
- **노출 위치**: Battle / Market 상세 화면의 통계 섹션에만 표시 (관리자 상세 화면 한정)
- **응답 형태**: 옵션별 비율, 연령대별/성별 분포 등 집계 수치만 포함

---

## 6. 특수 DB 규칙

- `uq_battle_vote(battle_id, member_id)`: 중복 투표 방지. `DataIntegrityViolationException` → `GlobalExceptionHandler`에서 `BATTLE_ALREADY_VOTED`로 변환
- 댓글, Battle은 soft delete (`deleted_at`). 조회 시 항상 `deleted_at IS NULL` 조건 포함
- 투표 집계: `UPDATE battle SET option_a_count = option_a_count + 1` 원자적 UPDATE 사용 (비관적 락 불필요)
- 정산 멱등성: `battle_vote.is_rewarded = true` 플래그로 중복 지급 방지

---

## 7. 헤더 처리

| 헤더 | 용도 | 누가 세팅 |
|---|---|---|
| `X-Member-Id` | memberId 추출 | Gateway (JWT 검증 후 전달) |
| `X-Internal-Auth` | 내부 서비스 인증 | Insight Service 등 내부 서비스 |
| `Idempotency-Key` | 포인트 차감 중복 방지 | 클라이언트 |

---

## 8. 변경 이력

| 일자 | 내용 |
|---|---|
| 2026-06-02 | 초안 작성 |
| 2026-06-04 | 교차분석/인증자 필터 조회를 관리자 전용으로 변경. 리포트 원문 미노출, 집계 통계만 응답. 내부 서비스 접근도 불가 |
