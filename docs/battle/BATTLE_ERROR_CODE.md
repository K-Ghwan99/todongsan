# Battle Service ERROR_CODE.md

> Battle Service에서 발생하는 도메인 비즈니스 에러를 정의한다.
> 공통 기술 에러(`UNAUTHORIZED`, `VALIDATION_FAILED`, `EXTERNAL_SERVICE_TIMEOUT` 등)는
> 루트 `ERROR_POLICY.md`를 참조한다.

---

## 1. 문서 목적

이 문서는 Battle Service의 다음 영역에서 발생할 수 있는 비즈니스 실패 상황과
해당 실패 후 데이터 상태, 재시도/보상 정책을 정의한다.

- Battle 등록 / 조회 / 관리자 상태 전환
- 투표 참여 / 결과 조회
- 댓글 작성 / 삭제 / 단건 조회
- 투표/댓글 성공 후 Point 지급 실패 처리

---

## 2. 에러 코드 목록

| ErrorCode | HTTP | 메시지 | 발생 조건 | Retry | 실패 후 상태 |
|---|---:|---|---|:---:|---|
| `BATTLE_NOT_FOUND` | 404 | 존재하지 않는 Battle입니다. | 존재하지 않거나 soft delete된 Battle 조회·투표·댓글 요청. **`PENDING` 상태 Battle에 대한 일반 사용자 접근도 이 에러로 처리** | X | 변경 없음 |
| `BATTLE_CLOSED` | 409 | 종료된 Battle입니다. | `CLOSED` 또는 `CANCELLED` 상태에서 투표·댓글 요청 | X | 변경 없음 |
| `BATTLE_ALREADY_VOTED` | 409 | 이미 투표한 Battle입니다. | 동일 회원이 같은 Battle에 중복 투표 요청 | X | `battle_vote` 생성 안 함, 집계 변경 없음 |
| `BATTLE_INVALID_OPTION` | 400 | 올바르지 않은 선택지입니다. | `option`이 `A`/`B`가 아님 | X | 변경 없음 |
| `BATTLE_INVALID_PERIOD` | 400 | Battle 기간이 올바르지 않습니다. | `end_at`이 `start_at` 이전이거나 과거 시점 | X | Battle 생성 안 함 |
| `BATTLE_INVALID_STATUS` | 409 | 현재 상태에서 처리할 수 없습니다. | 관리자 승인/거절/취소 시 상태 전이 불가 (예: `PENDING`이 아닌데 승인 요청, 이미 `CANCELLED`인데 다시 취소 등) | X | 변경 없음 |
| `BATTLE_RESULT_NOT_AVAILABLE` | 409 | 진행 중인 Battle은 상세 결과를 볼 수 없습니다. | 포인트 소비 결과 조회(교차분석/인증자 필터) 요청 시 Battle이 `CLOSED`가 아님 | X | Point 차감 안 함 |
| `BATTLE_COMMENT_NOT_FOUND` | 404 | 존재하지 않는 댓글입니다. | 존재하지 않거나 soft delete된 댓글 삭제·조회 요청 | X | 변경 없음 |
| `BATTLE_COMMENT_FORBIDDEN` | 403 | 본인 댓글만 삭제할 수 있습니다. | 다른 회원의 댓글에 대한 삭제 요청 | X | 변경 없음 |
| `BATTLE_COMMENT_TOO_LONG` | 400 | 댓글 길이가 너무 깁니다. (최대 500자) | `content` 길이가 정책 한도 초과 | X | 댓글 생성 안 함 |
| `BATTLE_POINT_REWARD_FAILED` | (내부 기록) | — | 투표/댓글 성공 후 Member-Point 지급 호출 실패 (Timeout/5xx) | O | 활동 유지, `point_reward_retry_queue` 적재 |

> **이전 버전 대비 변경 사항**
> - `BATTLE_NOT_ACTIVE` 폐기 → `PENDING` 상태 Battle은 일반 사용자에게 노출되지 않으므로 접근 시 `BATTLE_NOT_FOUND`로 처리 (검수 중인 Battle 보호)
> - `BATTLE_CLOSED` 발생 조건에서 `SETTLED` 제거 → BattleStatus enum에서 `SETTLED`가 없어졌기 때문 (정산은 `settled_at` 컬럼으로 표현)

---

## 3. API별 발생 가능 에러

### 3-1. Battle 등록 — `POST /api/v1/battles`

| ErrorCode | 메모 |
|---|---|
| `UNAUTHORIZED` (공통) | JWT 없음/만료 |
| `VALIDATION_FAILED` (공통) | title/optionA/optionB 빈 값, 길이 초과 |
| `BATTLE_INVALID_PERIOD` | end_at < start_at 또는 과거 시점 |

### 3-2. Battle 목록/상세 조회 — `GET /api/v1/battles`, `GET /api/v1/battles/{id}`

| ErrorCode | 메모 |
|---|---|
| `BATTLE_NOT_FOUND` | 상세 조회 시. `PENDING` 상태 Battle도 일반 사용자에게는 이 에러로 응답 |

### 3-3. 관리자 상태 전환 — `PATCH /api/v1/battles/admin/{battleId}/{approve|reject|cancel}`

| ErrorCode | 메모 |
|---|---|
| `UNAUTHORIZED` (공통) | — |
| `FORBIDDEN` (공통) | 관리자 권한 없음 |
| `BATTLE_NOT_FOUND` | 관리자는 `PENDING` 상태도 조회 가능. 진짜로 없을 때만 |
| `BATTLE_INVALID_STATUS` | 현재 상태에서 해당 전이 불가 (예: PENDING이 아닌데 승인 요청) |

### 3-4. Battle 취소 (본인) — `PATCH /api/v1/battles/{battleId}/cancel`

| ErrorCode | 메모 |
|---|---|
| `UNAUTHORIZED` (공통) | — |
| `FORBIDDEN` (공통) | 본인이 등록한 Battle이 아님 (`createdBy ≠ memberId`) |
| `BATTLE_NOT_FOUND` | 존재하지 않거나 soft delete됨 |
| `BATTLE_INVALID_STATUS` | `PENDING` 상태가 아님 (이미 승인·취소·종료됨) |

### 3-5. 투표 참여 — `POST /api/v1/battles/{id}/votes`

| ErrorCode | 메모 |
|---|---|
| `UNAUTHORIZED` (공통) | — |
| `VALIDATION_FAILED` (공통) | option 필드 누락 |
| `BATTLE_INVALID_OPTION` | option이 A/B가 아님 |
| `BATTLE_NOT_FOUND` | 존재하지 않거나 PENDING 상태 |
| `BATTLE_CLOSED` | CLOSED/CANCELLED 상태 |
| `BATTLE_ALREADY_VOTED` | 중복 투표 |
| `EXTERNAL_SERVICE_TIMEOUT` (공통) | Member-Point 지급 호출 Timeout → 투표는 유지, Retry Queue 적재 |

> **start_at 미도달 시**: ACTIVE 상태이지만 service 레이어에서 `BATTLE_CLOSED` 응답.
> (사용자 메시지는 "투표가 시작되지 않았습니다"로 처리 가능하나 ErrorCode는 같음)

### 3-6. 결과 조회 — `GET /api/v1/battles/{id}/result`

| ErrorCode | 메모 |
|---|---|
| `BATTLE_NOT_FOUND` | — |

### 3-7. 관리자 전용 분석 결과 — `GET /api/v1/battles/{id}/result/{cross|certified}`

| ErrorCode | 메모 |
|---|---|
| `UNAUTHORIZED` (공통) | — |
| `FORBIDDEN` (공통) | 관리자(`ROLE_ADMIN`) 권한 없음 |
| `BATTLE_NOT_FOUND` | — |
| `BATTLE_RESULT_NOT_AVAILABLE` | `CLOSED` 상태가 아님 (포인트 차감 없음, 관리자도 동일) |

### 3-8. 댓글 작성 — `POST /api/v1/battles/{id}/comments`

| ErrorCode | 메모 |
|---|---|
| `UNAUTHORIZED` (공통) | — |
| `VALIDATION_FAILED` (공통) | content 빈 값 |
| `BATTLE_COMMENT_TOO_LONG` | 500자 초과 |
| `BATTLE_NOT_FOUND` | 존재하지 않거나 PENDING 상태 |
| `BATTLE_CLOSED` | CLOSED/CANCELLED 상태 |

### 3-9. 댓글 목록 — `GET /api/v1/battles/{id}/comments`

| ErrorCode | 메모 |
|---|---|
| `BATTLE_NOT_FOUND` | — |

### 3-10. 댓글 삭제 — `DELETE /api/v1/battles/{id}/comments/{commentId}`

| ErrorCode | 메모 |
|---|---|
| `UNAUTHORIZED` (공통) | — |
| `BATTLE_COMMENT_NOT_FOUND` | 없거나 이미 삭제됨 |
| `BATTLE_COMMENT_FORBIDDEN` | 본인 댓글 아님 |

### 3-11. 내부 — 댓글 단건 조회 — `GET /api/v1/battles/comments/{commentId}`

| ErrorCode | 메모 |
|---|---|
| `FORBIDDEN` (공통) | 내부 서비스 인증 실패 |
| `BATTLE_COMMENT_NOT_FOUND` | 없거나 soft delete됨 (인증 무효 처리) |

### 3-12. 내부 — 투표 원본 데이터 — `GET /api/v1/battles/{id}/votes/raw`

| ErrorCode | 메모 |
|---|---|
| `FORBIDDEN` (공통) | 내부 서비스 인증 실패 |
| `BATTLE_NOT_FOUND` | — |

---

## 4. 실패 시 상태 변화

### 4-1. 투표 단계별 데이터 영향

| 단계 | 실패 원인 | `battle_vote` | `battle` 집계 | Member-Point | 사용자 응답 |
|---|---|---|---|---|---|
| 검증 | `BATTLE_NOT_FOUND`/`BATTLE_CLOSED`/`BATTLE_ALREADY_VOTED`/`BATTLE_INVALID_OPTION` | 생성 안 함 | 변경 없음 | 호출 안 함 | 4xx 에러 |
| 저장 | DB INSERT 실패 (`uq_battle_vote` 충돌은 위 케이스로 흡수) | 생성 안 함 | 변경 없음 | 호출 안 함 | 500 에러 |
| Point 지급 호출 | `EXTERNAL_SERVICE_TIMEOUT` / 5xx | 유지 (`is_rewarded=false`) | 유지 | 미확정 | **투표 성공 응답**, 백그라운드 재시도 |
| Point 지급 호출 | `POINT_INSUFFICIENT` 등 4xx (정상 응답) | 유지 (`is_rewarded=false`) | 유지 | 변경 없음 | 정책 결정 필요 (아래 4-3) |

### 4-2. 댓글 단계별 데이터 영향

| 단계 | 실패 원인 | `comment` | Member-Point | 사용자 응답 |
|---|---|---|---|---|
| 검증 | `BATTLE_NOT_FOUND`/`BATTLE_CLOSED`/`BATTLE_COMMENT_TOO_LONG` | 생성 안 함 | 호출 안 함 | 4xx 에러 |
| 저장 후 Point 지급 | Timeout/5xx | 유지 | 미확정 | **작성 성공 응답**, 재시도 |

### 4-3. 합의 필요 — Point 지급 호출이 4xx로 명확히 실패한 경우

투표/댓글은 성공했는데 보상 지급 단계에서 Member-Point가 `POINT_INSUFFICIENT`처럼
4xx로 명확히 거절하는 경우는 시스템 정합성 문제(보상 대상자에게 줄 포인트가 없는 상황)일
가능성이 높다. 다음 중 어느 정책으로 갈지 팀 합의 필요.

- (a) 투표·댓글은 유지, 관리자 수동 보정 대상으로 기록만 남김 (권장)
- (b) 보상이 실패하면 활동도 롤백 (트랜잭션 일관성 우선)

→ **MVP는 (a) 권장.** 사용자 입장에서 "투표는 됐는데 포인트는 안 들어옴"이
"투표 자체가 안 됨"보다 회복하기 쉽다.

---

## 5. Retry / 보상 처리

### 5-1. Retry 원칙

| 응답 유형 | Retry 여부 | 처리 |
|---|:---:|---|
| 4xx (비즈니스 거절) | X | 사용자에게 즉시 에러 응답 |
| 5xx (서버 오류) | O | 1분 간격, 최대 3회 |
| Timeout | O | 1분 간격, 최대 3회 |

### 5-2. 투표/댓글 성공 후 Point 지급 실패 (`BATTLE_POINT_REWARD_FAILED`)

`CONVENTION.md` 8-4와 동일한 흐름이다.

1. 투표/댓글 자체는 유지 (`battle_vote.is_rewarded = false`)
2. `point_reward_retry_queue`에 적재 (멱등성 키는 5-4 참조)
3. Scheduler가 1분 간격으로 최대 3회 재시도
4. 3회 실패 시 `status = FAILED` 처리, 관리자 보정 대상

### 5-3. 정산 보상 지급 실패 (다수 선택자 보상)

`docs/battle/ERD.md` 5번 흐름과 동일.

1. `winning_option` 확정 후 승자 측 투표자(`is_rewarded=false`) 순회
2. Member-Point 호출 (`POST /api/v1/points/settlements` 배치) 실패 시 `point_reward_retry_queue` 적재
3. 전원 처리 완료 시점에 `battle.settled_at` 기록 (`status`는 `CLOSED` 유지)
4. 실패 건이 남아 있어도 `settled_at` 기록은 진행 (재시도 큐로 처리되므로)

### 5-4. Member-Point 호출 정책 (reference / idempotency_key)

Battle Service가 Member-Point를 호출할 때 페이로드 규칙이다.
모든 보상 호출에서 `referenceType = "BATTLE"`, `referenceId = battleId`로 통일한다.
(`MEMBER_POINT_ERD.md` 4-3 `PointReferenceType` 규칙 준수)

| 상황 | type | amount | reason | referenceType | referenceId | idempotency_key |
|---|---|---|---|---|---|---|
| 투표 보상 | `EARN_VOTE` | +10P | Battle 투표 참여 보상 | `BATTLE` | `battleId` | `battle:vote:{battleId}:member:{memberId}` |
| 댓글 보상 | `EARN_COMMENT` | +2P | Battle 댓글 작성 보상 | `BATTLE` | `battleId` | `battle:comment:battle:{battleId}:member:{memberId}` |
| 승리 보상 (정산) | `EARN_VOTE_WIN` | +10P | 배틀 승리 보상 | `BATTLE` | `battleId` | `battle:settle:{battleId}:member:{memberId}` |
| 주제 승인 보상 | `EARN_BATTLE_APPROVED` | +20P | Battle 주제 등록 승인 보상 | `BATTLE` | `battleId` | `battle:approved:{battleId}:member:{memberId}` |

> **댓글 보상 idempotency_key**: `battle:comment:battle:{battleId}:member:{memberId}` 형태로, 사용자당 배틀 1개에 최초 1회만 보상이 지급된다. commentId를 키에 포함하지 않으므로 같은 사용자가 동일 Battle에 댓글을 여러 개 작성해도 두 번째 댓글부터는 보상 없음.

---

## 6. CONVENTION.md / README.md와의 충돌 정리

가이드 1-1 표 기준으로 정리하면서 기존 CONVENTION.md / README.md의 ErrorCode와 차이가 발생한다.
CONVENTION 담당자(Insight)와 협의해서 통일 필요.

| 현재 CONVENTION/README | 가이드 기준 권장 | 비고 |
|---|---|---|
| `ALREADY_VOTED` | `BATTLE_ALREADY_VOTED` | 도메인 prefix 누락. ERROR_POLICY 3-3 예시와 본 문서는 후자 사용 |
| `BATTLE_CLOSED` (HTTP 400) | `BATTLE_CLOSED` (HTTP 409) | 상태 충돌은 409가 적절 (ERROR_POLICY 2-6) |
| `ALREADY_PREDICTED` | `MARKET_ALREADY_PREDICTED` | 도메인 prefix 누락 |
| `INVALID_SETTLE` | `MARKET_SETTLEMENT_NOT_ALLOWED` | 도메인 prefix + 의미 명확화 |
| `NOT_FOUND` | `RESOURCE_NOT_FOUND` | ERROR_POLICY 4-3 기준. Insight는 이미 전환 완료 |
| `POINT_INSUFFICIENT` HTTP | 400 (CONVENTION) vs 409 (MEMBER_POINT) | Member-Point만 409. 400으로 통일 권장 |
| PointHistoryType `REFUND_INSIGHT` 누락 | 추가 필요 | MEMBER_POINT_ERD v4에 이미 추가됨 |

또한 가이드에서 공통으로 둬야 한다고 명시한 항목 중 CONVENTION.md에 누락된 것:

- `VALIDATION_FAILED` (400)
- `TOKEN_EXPIRED`, `INVALID_TOKEN` (401, `UNAUTHORIZED`와 분리)
- `IDEMPOTENCY_KEY_REQUIRED` / `IDEMPOTENCY_KEY_CONFLICT` (400/409)
- `EXTERNAL_SERVICE_TIMEOUT` / `EXTERNAL_SERVICE_UNAVAILABLE` (504/503)

→ 루트 `ERROR_POLICY.md`에는 모두 정의되어 있으므로, CONVENTION.md를 ERROR_POLICY 기준으로 일괄 업데이트 요청 예정 (CONVENTION 담당자).

---

## 7. 완료 체크리스트

- [x] 각 API별 발생 가능한 ErrorCode를 `docs/battle/API_SPEC.md`에 연결했다.
- [x] 중복 투표, 마감 투표, 없는 배틀 조회 케이스를 모두 작성했다.
- [x] Point 지급 실패 시 투표 데이터 유지 여부와 Retry Queue 적재 여부를 작성했다.
- [x] 공통 ErrorCode로 처리 가능한 `VALIDATION_FAILED`, `UNAUTHORIZED`, `FORBIDDEN` 등을
      도메인 코드로 중복 생성하지 않았다.
- [x] 도메인 ErrorCode는 `BATTLE_{REASON}` / `BATTLE_{ACTION}_{REASON}` 형식을 따른다.
- [x] 4xx는 Retry 대상에서 제외하고 5xx/Timeout만 Retry 대상으로 분리했다.
- [x] 정산 보상의 멱등성 키 패턴을 명시했다.
- [x] BattleStatus를 `CONVENTION.md` 6-1 기준(`PENDING/ACTIVE/CLOSED/CANCELLED`)으로 통일했다.
- [x] `BATTLE_NOT_ACTIVE`를 폐기하고 `BATTLE_NOT_FOUND`로 흡수했다.
- [x] Member-Point 호출 시 `referenceType=BATTLE`, `referenceId=battleId` 통일 정책을 명시했다.
- [ ] (팀 합의 후) CONVENTION.md의 `ALREADY_VOTED` → `BATTLE_ALREADY_VOTED` 리네이밍 반영.

---

## 8. 변경 이력

| 버전 | 변경 내용 |
|---|---|
| v1 | 초안 작성 (가이드 PDF 기준) |
| v2 | BattleStatus를 `PENDING/ACTIVE/CLOSED/CANCELLED`로 정정 (CONVENTION 6-1 일치) |
| v2 | `BATTLE_NOT_ACTIVE` 폐기 → `BATTLE_NOT_FOUND`로 통합 |
| v2 | `BATTLE_CLOSED` 발생 조건에서 `SETTLED` 제거 (Status enum에 SETTLED 없음) |
| v2 | 5-4 섹션 추가: Member-Point 호출 시 referenceType/referenceId/idempotency_key 정책 명확화 |
| v2 | 6번 충돌 항목 확장: NOT_FOUND, POINT_INSUFFICIENT HTTP 코드 등 추가 |
| v3 | 3-6 교차분석·인증자 필터 에러 코드를 관리자 전용 기준으로 변경. POINT_INSUFFICIENT/IDEMPOTENCY_KEY_REQUIRED 제거, FORBIDDEN 추가 |
| v4 | 3-3 관리자 전환 API 경로를 `/admin/` 하위로 정정. 3-4 사용자 Battle 취소 에러 섹션 신설. 이하 번호 이동 (3-4→3-5 ~ 3-11→3-12) |
| v5 | 3-1 미구현 `POINT_INSUFFICIENT` 제거. 5-4 테이블에 amount/reason 컬럼 추가. 댓글 idempotency_key 실제 구현 패턴으로 정정(`{commentId}` → `battle:{battleId}`). 미구현 `SPEND_BATTLE_CREATE` 행 제거 |
