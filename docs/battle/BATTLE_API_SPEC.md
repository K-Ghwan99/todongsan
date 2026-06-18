# API_SPEC.md - Battle Service

> Battle Service의 클라이언트 대상 API + 서비스 간 내부 연계 API 명세이다.
> 에러 코드 상세는 `docs/battle/ERROR_CODE.md` 참조.

---

## 1. 공통 사항

### 1-1. Base URL

```
https://todongsan.com/api/v1
```

### 1-2. 인증

JWT Bearer Token을 사용한다.

```
Authorization: Bearer {token}
```

내부 서비스 간 호출은 별도 내부 인증을 사용한다 (게이트웨이 차단 + 서비스 토큰).

### 1-3. 응답 포맷

모든 응답은 공통 `ApiResponse<T>` 포맷을 따른다. `CONVENTION.md` 섹션 2 참조.

### 1-4. 정책 상수

| 항목 | 값 |
|---|---|
| Battle title 최대 길이 | 255자 |
| Battle option 최대 길이 | 100자 |
| 댓글 content 최대 길이 | 500자 |
| 페이징 기본 size | Battle 20, 댓글 10 |
| 종료 후 결과 전체 공개 기간 | 72시간 |
| BattleStatus | `PENDING / ACTIVE / CLOSED / CANCELLED` (CONVENTION 6-1) |

> **정산 완료 표현**: BattleStatus에 `SETTLED`를 두지 않고 `battle.settled_at IS NOT NULL`로
> 판단한다. 정산 완료 후에도 `status`는 `CLOSED` 유지.

---

## 2. Battle 관리

### 2-1. Battle 주제 등록

```
POST /api/v1/battles
```

**인증**: 필요

**Request Body**

```json
{
  "title": "성수 vs 연남, 데이트하기 어디가 더 좋을까?",
  "optionA": "성수",
  "optionB": "연남",
  "description": "주말 데이트 코스로 어디가 더 매력적인지 투표해주세요!",
  "sido": "서울",
  "sigu": "성동구",
  "startAt": "2026-05-29T00:00:00",
  "endAt": "2026-06-05T00:00:00"
}
```

**Response**

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "battleId": 42,
    "title": "성수 vs 연남, 데이트하기 어디가 더 좋을까?",
    "optionA": "성수",
    "optionB": "연남",
    "sido": "서울",
    "sigu": "성동구",
    "status": "PENDING",
    "startAt": "2026-05-29T00:00:00",
    "endAt": "2026-06-05T00:00:00",
    "createdAt": "2026-05-28T10:00:00"
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

**Error Codes**

| ErrorCode | HTTP | 상황 |
|---|---:|---|
| `UNAUTHORIZED` | 401 | JWT 없음/만료 |
| `VALIDATION_FAILED` | 400 | 필수 필드 누락, 길이 초과 |
| `BATTLE_INVALID_PERIOD` | 400 | endAt이 startAt 이전이거나 과거 |
| `POINT_INSUFFICIENT` | 400 | Battle 생성권 부족 |

---

### 2-2. Battle 목록 조회

```
GET /api/v1/battles?status={status}&page={page}&size={size}
```

**인증**: 불필요

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| status | String | N | `ACTIVE`, `CLOSED` (기본: `ACTIVE`). `PENDING`/`CANCELLED`은 일반 사용자에게 노출 안 됨 |
| page | Integer | N | 페이지 번호 (기본: 0) |
| size | Integer | N | 페이지 크기 (기본: 20) |

**Response**: (기존 형식 유지)

**Error Codes**

| ErrorCode | HTTP | 상황 |
|---|---:|---|
| `VALIDATION_FAILED` | 400 | 잘못된 status 값 (`PENDING`/`CANCELLED` 요청 등) |

---

### 2-3. Battle 상세 조회

```
GET /api/v1/battles/{battleId}
```

**인증**: 불필요

**Response**: (기존 형식 유지, status는 `ACTIVE` 또는 `CLOSED`만 노출)

**Error Codes**

| ErrorCode | HTTP | 상황 |
|---|---:|---|
| `BATTLE_NOT_FOUND` | 404 | 존재하지 않거나 soft delete됨. **`PENDING`/`CANCELLED` 상태도 일반 사용자에게는 이 에러로 응답** |

> 관리자 전용 조회는 별도 API로 분리 (MVP 이후).

---

### 2-4. Battle 승인 (관리자)

```
PATCH /api/v1/battles/{battleId}/approve
```

**인증**: 필요 (관리자)

**Response**: status가 `PENDING` → `ACTIVE`

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "battleId": 42,
    "status": "ACTIVE"
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

**Error Codes**

| ErrorCode | HTTP | 상황 |
|---|---:|---|
| `UNAUTHORIZED` | 401 | — |
| `FORBIDDEN` | 403 | 관리자 권한 없음 |
| `BATTLE_NOT_FOUND` | 404 | — |
| `BATTLE_INVALID_STATUS` | 409 | `PENDING` 상태가 아님 (이미 승인/거절/취소됨) |

---

### 2-5. Battle 거절 (관리자)

```
PATCH /api/v1/battles/{battleId}/reject
```

**인증**: 필요 (관리자)

**Response**: status가 `PENDING` → `CANCELLED`

**Error Codes**: 2-4와 동일

---

### 2-6. Battle 강제 취소 (관리자)

```
PATCH /api/v1/battles/{battleId}/cancel
```

**인증**: 필요 (관리자)

**Response**: status가 `ACTIVE` → `CANCELLED`

**Error Codes**

| ErrorCode | HTTP | 상황 |
|---|---:|---|
| `UNAUTHORIZED` | 401 | — |
| `FORBIDDEN` | 403 | 관리자 권한 없음 |
| `BATTLE_NOT_FOUND` | 404 | — |
| `BATTLE_INVALID_STATUS` | 409 | `ACTIVE`가 아님 (이미 종료/취소되었거나 PENDING) |

---

### 2-7. 내가 만든 배틀 목록 조회

```
GET /api/v1/battles/created/me?status={status}&page={page}&size={size}
```

**용도**: 마이페이지의 "내가 만든 배틀 목록/현황". `created_by = X-Member-Id`인 배틀을 조회한다.

**인증**: 필요 (게이트웨이 인증 후 `X-Member-Id` 기준 조회)

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| status | String | N | `PENDING`, `ACTIVE`, `CLOSED`, `CANCELLED` 중 하나 이상(콤마 구분 허용). 미지정 시 전체 |
| page | Integer | N | 페이지 번호 (기본: 0) |
| size | Integer | N | 페이지 크기 (기본: 20) |

> **본인 배틀은 모든 상태 노출**: 외부 목록/상세(2-2/2-3)는 `PENDING`/`CANCELLED`를 숨기지만, 생성자 본인에게는 검수 대기(`PENDING`)·반려/취소(`CANCELLED`) 상태도 그대로 내려준다.

**Response**: Spring `Page<T>` 직렬화 형태(`content`, `number`, `size`, `totalElements`, `totalPages`, `first`, `last`).

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "content": [
      {
        "battleId": 42,
        "title": "성수 vs 연남, 데이트하기 어디가 더 좋을까?",
        "optionA": "성수",
        "optionB": "연남",
        "sido": "서울",
        "sigu": "성동구",
        "status": "PENDING",
        "voteCount": 0,
        "winningOption": null,
        "settledAt": null,
        "startAt": "2026-05-29T00:00:00",
        "endAt": "2026-06-05T00:00:00",
        "createdAt": "2026-05-28T10:00:00"
      }
    ],
    "number": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-06-18T10:00:00"
}
```

**응답 item 필드**

| 필드 | 타입 | 설명 |
|---|---|---|
| battleId | Long | 배틀 ID |
| title / optionA / optionB | String | 배틀 제목·선택지 |
| sido / sigu | String\|null | 지역 |
| status | String | `PENDING` / `ACTIVE` / `CLOSED` / `CANCELLED` |
| voteCount | Integer | 총 참여(투표) 수 |
| winningOption | String\|null | 정산 결과 `A`/`B`/`DRAW`. 미정산이면 null |
| settledAt | String\|null | 정산 시각. null이면 미정산 |
| startAt / endAt | String | 배틀 기간 |
| createdAt | String | 생성 시각 |

**설계 메모**

- **생성 내역이 없으면 404가 아니라 `200 + content=[]`** 로 응답한다.
- 상태 필터는 **서버 쿼리 파라미터로 처리**한다.

**Error Codes**

| ErrorCode | HTTP | 상황 |
|---|---:|---|
| `UNAUTHORIZED` | 401 | JWT 없음/만료 |
| `VALIDATION_FAILED` | 400 | 잘못된 status 값 |

---

## 3. 투표

### 3-1. 투표 참여

```
POST /api/v1/battles/{battleId}/votes
```

**인증**: 필요

**Request Body**

```json
{
  "option": "A"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| option | String | Y | "A" 또는 "B" |

**Response**: (기존 형식 유지)

**상태 변화 메모**

- 정상: `battle_vote` 생성 + `battle.option_a_count`/`vote_count` +1, 그 후 Member-Point 보상 호출
- Point 호출 Timeout: 투표 유지, `point_reward_retry_queue` 적재 (사용자에게는 투표 성공 응답)
- Member-Point 호출 페이로드 규칙은 `docs/battle/ERROR_CODE.md` 5-4 참조

**Error Codes**

| ErrorCode | HTTP | 상황 |
|---|---:|---|
| `UNAUTHORIZED` | 401 | — |
| `VALIDATION_FAILED` | 400 | option 필드 누락 |
| `BATTLE_INVALID_OPTION` | 400 | A/B 외의 값 |
| `BATTLE_NOT_FOUND` | 404 | 존재하지 않거나 `PENDING` 상태 |
| `BATTLE_CLOSED` | 409 | `CLOSED`/`CANCELLED` 상태 또는 `start_at` 미도달 |
| `BATTLE_ALREADY_VOTED` | 409 | 중복 투표 |

---

### 3-2. 투표 결과 조회

```
GET /api/v1/battles/{battleId}/result
```

**인증**: 선택

**Response**: (기존 형식 유지 - 상태/투표여부/72h 경과 분기. status 값은 `ACTIVE`/`CLOSED`만 사용)

**Error Codes**

| ErrorCode | HTTP | 상황 |
|---|---:|---|
| `BATTLE_NOT_FOUND` | 404 | — |

---

### 3-3. 교차분석 조회 (관리자 전용)

```
GET /api/v1/battles/{battleId}/result/cross
```

**인증**: 필요 (관리자 `ROLE_ADMIN`)

**응답 정책**

- 리포트 원문(개별 투표 데이터)은 반환하지 않는다.
- 옵션별 비율, 성별/연령대 분포 등 **집계 통계값만** 응답한다.
- 포인트 차감 없음 (0P).

**Response**: (상세 필드 구현 예정 — Feature 3)

**Error Codes**

| ErrorCode | HTTP | 상황 |
|---|---:|---|
| `UNAUTHORIZED` | 401 | — |
| `FORBIDDEN` | 403 | 관리자 권한 없음 |
| `BATTLE_NOT_FOUND` | 404 | — |
| `BATTLE_RESULT_NOT_AVAILABLE` | 409 | `CLOSED` 상태가 아님 |

---

### 3-4. 방문 인증자 필터 결과 (관리자 전용)

```
GET /api/v1/battles/{battleId}/result/certified
```

**인증**: 필요 (관리자 `ROLE_ADMIN`)

**응답 정책**

- 방문 인증자 기준 필터링된 **집계 통계값만** 응답한다.
- 포인트 차감 없음 (0P).

**Error Codes**: 3-3과 동일

---

### 3-5. 내 참여 배틀 목록 조회

```
GET /api/v1/battles/votes/me?status={status}&page={page}&size={size}
```

**용도**: 마이페이지의 "내가 참여(투표)한 배틀 목록/현황". Market의 `GET /api/v1/markets/predictions/me`와 대응한다.

**인증**: 필요 (게이트웨이 인증 후 `X-Member-Id` 기준 조회)

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| status | String | N | `ACTIVE`, `CLOSED` 중 하나 이상(콤마 구분 허용). 미지정 시 전체. `PENDING`/`CANCELLED`는 사용자 노출 대상 아님 |
| page | Integer | N | 페이지 번호 (기본: 0) |
| size | Integer | N | 페이지 크기 (기본: 20) |

**Response**: Spring `Page<T>` 직렬화 형태(다른 목록 API와 동일: `content`, `number`, `size`, `totalElements`, `totalPages`, `first`, `last`).

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "content": [
      {
        "battleId": 42,
        "title": "성수 vs 연남, 데이트하기 어디가 더 좋을까?",
        "optionA": "성수",
        "optionB": "연남",
        "sido": "서울",
        "sigu": "성동구",
        "status": "CLOSED",
        "selectedOption": "A",
        "winningOption": "A",
        "isWin": true,
        "rewardAmount": "50.00",
        "settledAt": "2026-06-05T10:00:00",
        "votedAt": "2026-05-30T14:23:00",
        "startAt": "2026-05-29T00:00:00",
        "endAt": "2026-06-05T00:00:00"
      }
    ],
    "number": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-06-18T10:00:00"
}
```

**응답 item 필드**

| 필드 | 타입 | 설명 |
|---|---|---|
| battleId | Long | 배틀 ID |
| title / optionA / optionB | String | 배틀 제목·선택지 |
| sido / sigu | String\|null | 지역 |
| status | String | `ACTIVE` / `CLOSED` |
| selectedOption | String | 내가 투표한 선택지 `A`/`B` |
| winningOption | String\|null | 정산 결과 `A`/`B`/`DRAW`. 미정산이면 null |
| isWin | Boolean\|null | 내 승리 여부. 미정산이면 null |
| rewardAmount | String(Decimal)\|null | 내가 받은 승리 보상. 없으면 null |
| settledAt | String\|null | 정산 시각. null이면 미정산 |
| votedAt | String | 내가 투표한 시각 |
| startAt / endAt | String | 배틀 기간 |

**설계 메모**

- **참여 내역이 없으면 404가 아니라 `200 + content=[]`** 로 응답한다.
- 상태 필터는 **서버 쿼리 파라미터로 처리**한다(프론트에서 현재 페이지 content만 거르는 방식 금지).
- `battle_vote`를 `member_id`로 조회 후 `battle`을 조인해 한 번에 내려준다(배틀 전체 조회 후 단건 API N회 호출 금지). 내부 API(`votes/raw`)는 사용하지 않는다.

**Error Codes**

| ErrorCode | HTTP | 상황 |
|---|---:|---|
| `UNAUTHORIZED` | 401 | JWT 없음/만료 |
| `VALIDATION_FAILED` | 400 | 잘못된 status 값 |

---

## 4. 댓글

### 4-1. 댓글 작성

```
POST /api/v1/battles/{battleId}/comments
```

**인증**: 필요

**Request Body**

```json
{
  "content": "성수는 감성 카페가 많고, 연남은 독특한 맛집들이 많아서 고민되네요!"
}
```

**Response**: (기존 형식 유지)

**Error Codes**

| ErrorCode | HTTP | 상황 |
|---|---:|---|
| `UNAUTHORIZED` | 401 | — |
| `VALIDATION_FAILED` | 400 | content 빈 값 |
| `BATTLE_COMMENT_TOO_LONG` | 400 | 500자 초과 |
| `BATTLE_NOT_FOUND` | 404 | 존재하지 않거나 `PENDING` 상태 |
| `BATTLE_CLOSED` | 409 | `CLOSED`/`CANCELLED` 상태 |

---

### 4-2. 댓글 목록 조회

```
GET /api/v1/battles/{battleId}/comments?page={page}&size={size}
```

**인증**: 불필요

**Response**: (기존 형식 유지)

**Error Codes**

| ErrorCode | HTTP | 상황 |
|---|---:|---|
| `BATTLE_NOT_FOUND` | 404 | — |

---

### 4-3. 댓글 삭제

```
DELETE /api/v1/battles/{battleId}/comments/{commentId}
```

**인증**: 필요

**Response**

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": null,
  "timestamp": "2026-05-28T10:00:00"
}
```

**Error Codes**

| ErrorCode | HTTP | 상황 |
|---|---:|---|
| `UNAUTHORIZED` | 401 | — |
| `BATTLE_COMMENT_FORBIDDEN` | 403 | 본인 댓글 아님 |
| `BATTLE_COMMENT_NOT_FOUND` | 404 | 존재하지 않거나 이미 삭제됨 |

---

## 5. 내부 연계 API

> 외부 노출 차단. 게이트웨이에서 외부 요청 차단 + 서비스 간 인증 토큰 검증 후 라우팅.

### 5-0. 아웃바운드 호출 (Battle → 외부 서비스)

Battle Service가 직접 호출하는 내부 엔드포인트 목록.

| 대상 서비스 | 경로 | 메서드 | 호출 시점 | 실패 처리 |
|---|---|---|---|---|
| Insight | `/internal/api/v1/insights/battles/{battleId}/report` | POST | Battle CLOSED 전환 직후 | 로그만 (RetryQueue 없음) |
| Member-Point | `/internal/api/v1/points/earn` | POST | 투표/댓글/승인 보상 | Timeout/5xx → RetryQueue |
| Member-Point | `/internal/api/v1/points/settlements` | POST | 정산 보상 배치 | Timeout/5xx → RetryQueue |

### 5-1. Battle 투표 원본 데이터 조회 (Insight Service 전용)

```
GET /api/v1/battles/{battleId}/votes/raw
```

**인증**: 내부 서비스 인증

**Response**

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "battleId": 42,
    "title": "성수 vs 연남, 데이트하기 어디가 더 좋을까?",
    "optionA": "성수",
    "optionB": "연남",
    "totalVotes": 320,
    "optionACount": 195,
    "optionBCount": 125,
    "status": "CLOSED",
    "winningOption": "A",
    "settledAt": "2026-06-05T10:00:00",
    "votes": [
      { "memberId": 1, "selectedOption": "A", "votedAt": "2026-05-30T14:23:00" },
      { "memberId": 2, "selectedOption": "B", "votedAt": "2026-05-31T09:11:00" }
    ]
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

> **응답 보강 (v2)**: Insight의 교차분석/AI 분석을 위해 개별 투표 데이터(`votes` 배열)를 포함한다.
> 이전 버전은 집계 데이터만 제공해서 Insight 측이 member별 통계를 산출할 수 없었음.
> `INSIGHT_API_SPEC.md` 2번 아웃바운드 호출 표의 "투표 목록 조회(member_id, selected_option)" 요구사항을 충족.

**Error Codes**

| ErrorCode | HTTP | 상황 |
|---|---:|---|
| `FORBIDDEN` | 403 | 내부 서비스 인증 실패 |
| `BATTLE_NOT_FOUND` | 404 | — |

---

### 5-2. 댓글 단건 조회 (방문 인증용, Insight Service 전용)

```
GET /api/v1/battles/comments/{commentId}
```

**용도**: Insight-Reputation Service의 댓글 기반 방문 인증(`visit_certification.method=COMMENT`) 검증.
사용자가 "이 댓글로 지역 방문을 인증한다"고 요청하면 Insight가 이 API를 호출하여
댓글 존재 여부와 작성자/Battle 매핑을 확인한다.

**인증**: 내부 서비스 인증

**Path Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| commentId | Long | Y | 댓글 ID |

**Response**

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "commentId": 15,
    "battleId": 42,
    "memberId": 678,
    "createdAt": "2026-05-28T10:00:00"
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

**응답 필드 정책**

- `content` (본문)와 닉네임은 응답에 포함하지 않음. 인증 검증에 불필요하고
  응답 비대화·개인정보 노출 최소화를 위함.
- soft delete된 댓글은 `BATTLE_COMMENT_NOT_FOUND`로 응답 (인증 무효 처리).
  → 인증 후 댓글을 지우는 우회를 방지.
- **지역 정보(sido/sigu)는 이 응답에 포함하지 않음.** Insight가 지역 검증이 필요한 경우
  `battleId`로 `GET /api/v1/battles/{battleId}/info` (5-3)를 별도 호출하여 `sido`/`sigu`를 확인한다.

**Error Codes**

| ErrorCode | HTTP | 상황 |
|---|---:|---|
| `FORBIDDEN` | 403 | 내부 서비스 인증 실패 |
| `BATTLE_COMMENT_NOT_FOUND` | 404 | 존재하지 않거나 soft delete됨 |

---

### 5-3. Battle 기본 정보 조회 (Insight Service 등 내부용)

```
GET /api/v1/battles/{battleId}/info
```

**용도**: Insight Service가 AI 분석 시 Battle 메타데이터 조회. 외부 API(`GET /api/v1/battles/{battleId}`)와 분리하여 내부 인증 채널로 노출.

**인증**: 내부 서비스 인증

**Response**

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "battleId": 42,
    "title": "성수 vs 연남, 데이트하기 어디가 더 좋을까?",
    "optionA": "성수",
    "optionB": "연남",
    "sido": "서울",
    "sigu": "성동구",
    "status": "CLOSED",
    "isClosed": true,
    "createdBy": 7,
    "startAt": "2026-05-29T00:00:00",
    "endAt": "2026-06-05T00:00:00",
    "settledAt": "2026-06-05T10:00:00"
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

> 외부 API(2-3)는 `PENDING`/`CANCELLED`를 노출하지 않지만, 내부 API는 모든 상태를 그대로 노출한다.

**Error Codes**

| ErrorCode | HTTP | 상황 |
|---|---:|---|
| `FORBIDDEN` | 403 | 내부 서비스 인증 실패 |
| `BATTLE_NOT_FOUND` | 404 | — |

---

## 6. 변경 이력

| 일자 | 내용 |
|---|---|
| 2026-05-28 | 초안 작성 |
| 2026-05-29 | 에러 코드 가이드 기준 정비 (도메인 prefix, HTTP 상태 코드, 추가 케이스) |
| 2026-05-29 | 5-2 댓글 단건 조회 내부 API 추가 (Insight 요청) |
| 2026-06-01 | BattleStatus를 CONVENTION.md 6-1 기준(`PENDING/ACTIVE/CLOSED/CANCELLED`)으로 통일. `ONGOING`/`SETTLED` 제거 |
| 2026-06-01 | `BATTLE_NOT_ACTIVE` 폐기 → `BATTLE_NOT_FOUND`로 통합 (PENDING Battle은 일반 사용자에게 비공개) |
| 2026-06-01 | 5-1 응답에 `votes[]` 배열 추가 (Insight 교차분석 지원) |
| 2026-06-01 | 5-3 Battle 기본 정보 조회 내부 API 신설 (외부 API와 채널 분리) |
| 2026-06-01 | 5-2 댓글 단건 조회 응답에 sido/sigu 미포함 정책 명시 (Battle 도메인은 지역 비종속) |
| 2026-06-04 | 3-3/3-4 교차분석·인증자 필터 조회를 관리자 전용으로 변경. 포인트 차감 제거, 집계 통계만 응답 |
| 2026-06-05 | 5-0 아웃바운드 호출 목록 신설. Battle 종료 시 Insight AI 분석 트리거(`POST /internal/api/v1/insights/battles/{battleId}/report`) 추가 |
| 2026-06-11 | 2-1 요청/응답에 `sido`, `sigu` 추가. 5-1 `totalVoteCount` → `totalVotes` 변경, votes 항목에 `votedAt` 추가. 5-2 sido/sigu 정책 설명 수정. 5-3 응답에 `sido`, `sigu`, `isClosed` 추가 |
| 2026-06-18 | 3-5 내 참여 배틀 목록 조회(`GET /api/v1/battles/votes/me`) 신설. 마이페이지 "내가 참여한 배틀" 용도, Market `/markets/predictions/me` 대응 |
| 2026-06-18 | 2-7 내가 만든 배틀 목록 조회(`GET /api/v1/battles/created/me`) 신설. 생성자 본인에게는 PENDING/CANCELLED 포함 전체 상태 노출 |
