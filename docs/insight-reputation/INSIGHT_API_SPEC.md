# API_SPEC_v6.md - Insight-Reputation Service

> Insight-Reputation Service의 상세 API 명세이다.
> 사용자 신뢰도(Reputation) 조회 및 관리, 방문 인증, AI 분석 리포트 생성 기능을 제공한다.

---

## 변경 내역 (v7 → v8)

| 섹션 | 변경 내용 |
|---|---|
| 섹션 2 신규 | Market `basic-info` API 소비 명세 및 지역 매핑 규칙 추가 |
| 섹션 7-0 신규 | Market SETTLED 자동 트리거 내부 API 추가 (`POST /internal/api/v1/insights/markets/{marketId}/report`) |
| 섹션 7-4 신규 | Market 공공 데이터 참고 자료 조회 API 추가 (`GET /api/v1/insights/markets/{marketId}/public-data-reference`) |
| 섹션 3-4 신규 | 관리자 마켓 가격 이력 조회 API 추가 (`GET /api/v1/admin/insights/markets/{marketId}/price-history`) |
| `RebDataParser` 버그 수정 | `dtaCycleCd` 기반 `data_type` 결정 (`WK→WEEKLY_PRICE_INDEX`, `MM→MONTHLY_PRICE_INDEX`) |

---

## 변경 내역 (v6 → v7)

| 섹션 | 변경 내용 |
|---|---|
| 섹션 7-1~7-3 | Battle 사용자 facing API 제거 (내부 전용 전환) |
| 섹션 7-0 신규 | Battle 자동 트리거 내부 API 추가 |
| 섹션 3-3 신규 | Battle 리포트 관리자 조회 API 추가 |
| 섹션 7 번호 | Market 섹션 7-4~7-6 → 7-1~7-3으로 재번호 |

---

## 변경 내역 (v5 → v6)

| 섹션 | 변경 내용 |
|---|---|
| 섹션 2 | Market Service 아웃바운드 API 경로를 내부 연계 API에 맞춰 수정 (`/internal/api/v1/markets/{marketId}/insight-*`) |
| 섹션 2 | Market Service API 목적에 SETTLED 상태 제약 및 페이지네이션 명시 |

---

## 변경 내역 (v4 → v5)

| 섹션 | 변경 내용 |
|---|---|
| 섹션 3-2, 5-1, 6-1, 6-3 | 공통 에러 `NOT_FOUND` → `RESOURCE_NOT_FOUND` 통일 (ERROR_CODE.md 정합성) |
| 섹션 6-1 | `INSIGHT_REPORT_ALREADY_PROCESSING`, `INSIGHT_REPORT_SOURCE_DATA_NOT_READY` 누락 ErrorCode 추가 |
| 섹션 6-4 | `INSIGHT_REPORT_ALREADY_PROCESSING`, `INSIGHT_REPORT_SOURCE_DATA_NOT_READY` 누락 ErrorCode 추가 |
| 섹션 7 | `REPUTATION_NOT_FOUND`, `VISIT_CERT_COMMENT_NOT_FOUND`, `INSIGHT_REPORT_NOT_FOUND`, `INSIGHT_REPORT_ALREADY_PROCESSING`, `INSIGHT_REPORT_SOURCE_DATA_NOT_READY` 누락 5개 추가 |

---

## 변경 내역 (v3 → v4)

| 섹션 | 변경 내용 |
|---|---|
| 전체 | 도메인 ErrorCode 네이밍을 `ERROR_CODE.md` 가이드 기준(`{DOMAIN}_{REASON}`)으로 리네이밍 |
| 섹션 4-1 | `RESIDENCE_CHANGE_COOLDOWN` → `REPUTATION_RESIDENCE_CHANGE_COOLDOWN` |
| 섹션 5-1 | `ALREADY_CERTIFIED_RECENTLY` → `VISIT_CERT_COOLDOWN` |
| 섹션 5-1 | `TOO_FAR_FROM_REGION` → `VISIT_CERT_OUT_OF_RANGE` |
| 섹션 5-1 | `COMMENT_REGION_MISMATCH` → `VISIT_CERT_COMMENT_REGION_MISMATCH` |
| 섹션 6-1, 6-4 | `INSIGHT_REPORT_SOURCE_NOT_CLOSED` → `INSIGHT_REPORT_SOURCE_NOT_CLOSED` |
| 섹션 6-1, 6-4 | `INSIGHT_REPORT_GENERATION_FAILED` → `INSIGHT_REPORT_GENERATION_FAILED` |
| 섹션 7 | ErrorCode 목록 전체 리네이밍 반영 |

---

## 변경 내역 (v2 → v3)

| 섹션 | 변경 내용 |
|---|---|
| 섹션 3-2 | 본인 ID 입력 시 타인 조회 응답 그대로 반환으로 확정. `[팀 결정 필요]` 제거 |
| 섹션 5-1 | COMMENT 인증 Battle 댓글 조회 API 요청 완료. `[팀 결정 필요]` 제거 |
| 섹션 6-1, 6-4 | 비동기 처리 확정. `[팀 결정 필요]` 제거 및 응답 명세 정리 |
| 섹션 6-1, 6-4 | `INSIGHT_REPORT_GENERATION_FAILED` 확정: Member-Point 담당자가 환불 로직 추가, 이 서비스는 예외 발생만 담당. `[팀 결정 필요]` 제거 |
| 섹션 6-3, 6-6 | 폴링 수치 확정 (2초 간격, 30초 타임아웃). `[팀 결정 필요]` 제거 |
| 섹션 8 | 미결 사항 전체 확정. 섹션 삭제 |

---

## 변경 내역 (v1 → v2)

| 섹션 | 변경 내용 |
|---|---|
| 섹션 4-1 | 최초 선언 시 쿨다운 skip 조건 명시 (`residenceChangedAt IS NULL`) |
| 섹션 5-1 | COMMENT 인증 흐름에 Battle Service 댓글 조회 API 의존성 명시. `[팀 결정 필요] commentId vs battleId` 추가 |
| 섹션 6-1 | POST 응답 구조를 비동기(PENDING 즉시 반환) 기준으로 변경. `[팀 결정 필요]` 표시 |
| 섹션 6-4 | 6-1과 동일하게 "기존 DONE 리포트 존재 시 Point 미차감" 주석 추가. POST 응답 비동기 구조로 변경 |
| 섹션 6-1/6-4 | `INSIGHT_REPORT_GENERATION_FAILED` 환불 API 명시 (`[팀 결정 필요]`) |
| 섹션 6-3/6-6 | 프론트엔드 폴링 가이드 추가 (`[팀 결정 필요]` 수치 포함) |
| 섹션 3-2 | 본인 ID 입력 시 처리 방침 `[팀 결정 필요]` 추가 |
| 섹션 8 (신규) | 미결 사항 및 팀 결정 필요 항목 목록 추가 |

---

## 1. 공통 사항

### 1-1. Base URL

```
https://api.todongsan.com/api/v1       (외부, API Gateway 경유)
http://insight-reputation-service/api/v1  (내부 서비스 간 직접 호출)
```

### 1-2. 인증

모든 외부 API는 JWT 인증이 필요하다. API Gateway가 검증 후 헤더로 전달한다.

```
X-Member-Id: {memberId}
X-Member-Role: USER | ADMIN
```

### 1-3. 응답 포맷

[CONVENTION.md 섹션 2](../CONVENTION.md#2-공통-응답-포맷) 참조

---

## 2. 서비스 간 내부 연계 API (아웃바운드)

Insight-Reputation Service가 다른 서비스를 호출하는 API 목록이다.

| 호출 대상 | 엔드포인트 | 목적 |
|---|---|---|
| Battle Service | GET /internal/api/v1/battles/{battleId}/info | Battle 기본 정보 조회 (**⚠️ Battle 담당자 구현 필요 — COORDINATION_ISSUES.md 참조**) |
| Battle Service | GET /internal/api/v1/battles/{battleId}/votes/raw | 투표 목록 조회 (member_id, selected_option) (**⚠️ Battle 담당자 경로 변경 필요 — COORDINATION_ISSUES.md 참조**) |
| Battle Service | GET /internal/api/v1/battles/comments/{commentId} | 댓글 단건 조회 (방문 인증용) (**⚠️ Battle 담당자 경로 변경 필요 — COORDINATION_ISSUES.md 참조**) |
| Market Service | GET /internal/api/v1/markets/{marketId}/basic-info | 마켓 기본 정보 조회 (상태 무관. regionSido/regionSigu 포함. MARKET_NOT_FOUND → RESOURCE_NOT_FOUND 변환) |
| Market Service | GET /internal/api/v1/markets/{marketId}/insight-summary | Market 기본 정보 + 옵션별 통계 조회 (SETTLED 전용. Market spec 섹션 11-1) |
| Market Service | GET /internal/api/v1/markets/{marketId}/insight-predictions | 예측 참여 목록 조회 (member_id, selected_option, 페이지네이션. Market spec 섹션 11-2) |
| Member-Point Service | POST /internal/api/v1/members/batch | 회원 정보 배치 조회 (ageGroup, gender, residenceSido/Sigu) |
| Member-Point Service | POST /internal/api/v1/points/refunds | AI 리포트 생성 실패 시 Point 환불 (`INSIGHT_REPORT_SOURCE_DATA_NOT_READY`, `INSIGHT_REPORT_GENERATION_FAILED`) |

[루트 API_SPEC.md 섹션 3](../API_SPEC.md#3-insight-reputation-service-내부-연계-api) 참조

---

## 3. 서비스 간 내부 연계 API (인바운드)

다른 서비스가 Insight-Reputation Service를 호출하는 API 목록이다.

| 호출 주체 | 엔드포인트 | 목적 |
|---|---|---|
| Battle Service | POST /internal/api/v1/reputations/activity | 투표/댓글/Battle 승인 시 activity_score 업데이트 |
| Battle Service | POST /internal/api/v1/insights/battles/{battleId}/report | Battle 종료 시 AI 리포트 자동 트리거 |
| Market Service | POST /internal/api/v1/reputations/prediction | 정산 완료 시 prediction_count/correct 업데이트 |
| Market Service | POST /internal/api/v1/insights/markets/{marketId}/report | Market SETTLED 시 AI 리포트 자동 트리거 (v8 신규, 섹션 7-0-M 참조) |

### 3-1. Activity Score 업데이트

```
POST /internal/api/v1/reputations/activity
```

**인증 필요:** X (내부 서비스 간 호출)
**Point 소비:** X

**호출 주체:** Battle Service (투표 완료, 댓글 작성, Battle 승인 시)

**Request Body**

```json
{
  "memberId": 1,
  "activityType": "VOTE",
  "region": {
    "sido": "서울",
    "sigu": "성동구"
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| memberId | Long | Y | 회원 ID |
| activityType | String | Y | `VOTE`, `COMMENT`, `BATTLE_APPROVED` |
| region | Object | Y | 활동 지역 정보 |
| region.sido | String | Y | 시/도 |
| region.sigu | String | Y | 시/구 |

**Response**

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "memberId": 1,
    "activityScore": 36,
    "activityCount": 2,
    "activityConfirmed": false
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

> 현재 거주 선언 지역과 활동 지역이 일치할 때만 `activityCount` 증가 (최대 3).
> `activityCount = 3` 달성 시 `activityConfirmed = true`, `activityConfirmedAt` 설정.

**Error Codes**

| 에러 코드 | HTTP | 상황 |
|---|---:|---|
| RESOURCE_NOT_FOUND | 404 | 존재하지 않는 회원 |
| VALIDATION_FAILED | 400 | 잘못된 `activityType` 또는 지역 정보 |

---

### 3-2. Prediction Accuracy 업데이트

```
POST /internal/api/v1/reputations/prediction
```

**인증 필요:** X (내부 서비스 간 호출)
**Point 소비:** X

**호출 주체:** Market Service (Market 정산 완료 시)

**Request Body**

```json
{
  "memberId": 1,
  "marketId": 7,
  "predictionId": 123,
  "isCorrect": true
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| memberId | Long | Y | 회원 ID |
| marketId | Long | Y | Market ID |
| predictionId | Long | N | Prediction ID (Market Service에서 제공, 추적 정확성 향상) |
| isCorrect | Boolean | Y | 예측 정확성 (true: 정답, false: 오답) |

**Response**

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "memberId": 1,
    "predictionCount": 11,
    "predictionCorrect": 8,
    "predictionAccuracy": 72.73
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

> `predictionCount` 증가 및 `predictionCorrect` 업데이트.
> `predictionAccuracy = (predictionCorrect / predictionCount) * 100` 계산.
> 
> **멱등성 보장**: 동일 `memberId + marketId` 재시도 시 중복 처리 없이 기존 결과 반환.
> `market_prediction_result` 테이블의 `UNIQUE KEY (member_id, market_id)` 제약으로 보장.

**Error Codes**

| 에러 코드 | HTTP | 상황 |
|---|---:|---|
| RESOURCE_NOT_FOUND | 404 | 존재하지 않는 회원 |
| VALIDATION_FAILED | 400 | 잘못된 Market ID, Prediction ID 또는 `isCorrect` 값 |

---

### 3-3. Battle AI 분석 리포트 관리자 조회

```
GET /api/v1/admin/insights/battles/{battleId}/report
```

**인증 필요:** X-Member-Role: ADMIN 필수
**Point 소비:** X

**Response**

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "battleId": 42,
    "reportId": 1,
    "status": "DONE",
    "title": "성수 vs 연남, 데이트하기 어디가 더 좋을까?",
    "summary": "전체 투표에서는 성수가 61%로 우세했습니다...",
    "analysisData": {},
    "generatedAt": "2026-05-25T15:30:00",
    "retryCount": 0,
    "failedReason": null
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

**Error Codes**

| 에러 코드 | HTTP | 상황 |
|---|---:|---|
| RESOURCE_NOT_FOUND | 404 | Battle 없거나 리포트 없음 |
| FORBIDDEN | 403 | ADMIN 권한 없음 |

---

## 4. Reputation 조회

### 4-1. 내 신뢰도 조회

```
GET /api/v1/reputations/me
```

**인증 필요:** O
**Point 소비:** X

**Response**

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "memberId": 1,
    "activityScore": 35,
    "predictionCount": 10,
    "predictionCorrect": 7,
    "predictionAccuracy": 70.00,
    "residenceSido": "서울",
    "residenceSigu": "성동구",
    "residenceDeclaredAt": "2026-04-01T10:00:00",
    "residenceChangedAt": "2026-04-01T10:00:00",
    "activityCount": 3,
    "activityConfirmed": true,
    "activityConfirmedAt": "2026-05-01T12:00:00",
    "visitCertifications": [
      {
        "sido": "서울",
        "sigu": "성동구",
        "method": "GPS",
        "certifiedAt": "2026-05-20T14:30:00",
        "lastCertifiedAt": "2026-05-20T14:30:00",
        "nextAvailableDate": "2026-06-19T14:30:00"
      },
      {
        "sido": "서울",
        "sigu": "마포구",
        "method": "COMMENT",
        "certifiedAt": "2026-05-15T16:45:00",
        "lastCertifiedAt": "2026-05-15T16:45:00",
        "nextAvailableDate": "2026-06-14T16:45:00"
      }
    ]
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

> `activityCount`: 현재 거주 선언 지역 기준 활동 누적 횟수. 최대 3.
> `activityConfirmed`: `activityConfirmedAt IS NOT NULL` 여부.
> `visitCertifications`: 해당 회원의 방문 인증 전체 목록.
> `nextAvailableDate`: `lastCertifiedAt + 30일` 계산값.

---

### 4-2. 특정 회원 신뢰도 조회

```
GET /api/v1/reputations/{memberId}
```

**인증 필요:** O
**Point 소비:** X

**Path Variable**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| memberId | Long | Y | 조회할 회원 ID |

**Response**

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "memberId": 2,
    "activityScore": 28,
    "predictionCount": 8,
    "predictionAccuracy": 62.50,
    "residenceSido": "서울",
    "residenceSigu": "마포구",
    "activityConfirmed": true,
    "visitCertificationCount": 3
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

> 타인 조회이므로 `predictionCorrect`, 인증 상세 목록, `nextAvailableDate` 등 민감 정보는 제외한다.
> 본인 ID를 `{memberId}`에 입력한 경우에도 타인 조회 응답을 그대로 반환한다. 본인 전체 정보는 `GET /api/v1/reputations/me`를 사용한다.

**Error Codes**

| 에러 코드 | HTTP | 상황 |
|---|---:|---|
| RESOURCE_NOT_FOUND | 404 | 존재하지 않는 회원 또는 Reputation 미생성 회원 |

---

## 5. 거주지역 관리

### 5-1. 거주지역 선언 및 변경

```
PUT /api/v1/reputations/me/residence
```

**인증 필요:** O
**Point 소비:** X

**Request Body**

```json
{
  "sido": "서울",
  "sigu": "성동구"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| sido | String | Y | 시/도 (예: 서울, 경기) |
| sigu | String | Y | 시/구 (예: 성동구, 수원시 영통구) |

**Response - 최초 선언**

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "sido": "서울",
    "sigu": "성동구",
    "residenceDeclaredAt": "2026-05-28T10:00:00",
    "residenceChangedAt": null,
    "nextChangeAvailableDate": null
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

**Response - 변경 성공**

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "sido": "서울",
    "sigu": "마포구",
    "residenceDeclaredAt": "2026-04-01T10:00:00",
    "residenceChangedAt": "2026-05-28T10:00:00",
    "nextChangeAvailableDate": "2026-06-27T10:00:00"
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

> 변경 성공 시 `activity_count = 0`, `activity_confirmed_at = NULL` 동시 초기화.
> `nextChangeAvailableDate`: `residenceChangedAt + 30일` 계산값.
> **최초 선언 시(`residenceChangedAt IS NULL`) `REPUTATION_RESIDENCE_CHANGE_COOLDOWN` 체크를 skip한다.** 쿨다운은 변경 이력이 존재하는 경우에만 적용된다.

**Error Codes**

| 에러 코드 | HTTP | 상황 |
|---|---:|---|
| REPUTATION_RESIDENCE_CHANGE_COOLDOWN | 400 | 거주지역 변경 후 30일 미경과 |

**REPUTATION_RESIDENCE_CHANGE_COOLDOWN 응답 예시**

```json
{
  "success": false,
  "errorCode": "REPUTATION_RESIDENCE_CHANGE_COOLDOWN",
  "message": "거주지역은 2026-06-20일부터 변경 가능합니다.",
  "data": {
    "nextChangeAvailableDate": "2026-06-20T00:00:00"
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

---

## 6. 방문 인증

### 6-1. 방문 인증 등록

```
POST /api/v1/reputations/visit-certifications
```

**인증 필요:** O
**Point 소비:** X

**Request Body - GPS 방식**

```json
{
  "sido": "서울",
  "sigu": "성동구",
  "method": "GPS",
  "data": {
    "latitude": 37.544876,
    "longitude": 127.055678
  }
}
```

**Request Body - COMMENT 방식**

```json
{
  "sido": "서울",
  "sigu": "성동구",
  "method": "COMMENT",
  "data": {
    "commentId": 42
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| sido | String | Y | 인증할 지역 시/도 |
| sigu | String | Y | 인증할 지역 시/구 |
| method | String | Y | `GPS` 또는 `COMMENT` |
| data | Object | Y | 인증 방법별 데이터 |

**GPS 방식 data**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| latitude | Double | Y | 위도 |
| longitude | Double | Y | 경도 |

**COMMENT 방식 data**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| commentId | Long | Y | 해당 지역 Battle 댓글 ID |

> `sido` + `sigu`를 직접 받는다. ERD `visit_certification` 테이블의 `sido`, `sigu` 컬럼에 직접 매핑된다.
> GPS 인증 시 지역 중심 좌표 기준 3km 초과 시 거부한다.
> 재인증 성공 시 기존 레코드를 UPDATE한다 (INSERT 아님).
>
> **COMMENT 인증 처리 흐름:**
> 1. `commentId`로 Battle Service에 댓글 단건 조회
>    (`GET /internal/api/v1/battles/comments/{commentId}` → `memberId`, `battleId` 추출)
> 2. 댓글 작성자 `memberId`로 Member-Point Service에 회원 거주지 조회
>    (`POST /internal/api/v1/members/batch` → `residenceSido`, `residenceSigu` 추출)
> 3. 작성자 `residenceSido`/`residenceSigu`와 요청 `sido`/`sigu` 불일치 시
>    `VISIT_CERT_COMMENT_REGION_MISMATCH` 반환
> 4. ERD `visit_certification.battle_id` 저장
>    (`comment_content` 미저장 — Battle API 응답에 `content` 필드 없음)

**Response**

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "sido": "서울",
    "sigu": "성동구",
    "method": "GPS",
    "certifiedAt": "2026-05-20T14:30:00",
    "lastCertifiedAt": "2026-05-28T10:00:00",
    "nextAvailableDate": "2026-06-27T10:00:00"
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

> `certifiedAt`: 최초 인증 시점. 재인증 시 변경되지 않는다.
> `lastCertifiedAt`: 이번 인증 시점.
> `nextAvailableDate`: `lastCertifiedAt + 30일` 계산값.

**Error Codes**

| 에러 코드 | HTTP | 상황 |
|---|---:|---|
| RESOURCE_NOT_FOUND | 404 | COMMENT 방식에서 존재하지 않는 댓글 ID |
| FORBIDDEN | 403 | GPS 사용 불가 환경 (HTTP 환경) |
| VISIT_CERT_OUT_OF_RANGE | 400 | GPS 좌표가 지역 중심에서 3km 초과 |
| VISIT_CERT_COMMENT_REGION_MISMATCH | 400 | 댓글이 해당 지역(sido+sigu) Battle이 아님 |
| VISIT_CERT_COOLDOWN | 400 | 동일 지역 30일 내 재인증 시도 |

**VISIT_CERT_COOLDOWN 응답 예시**

```json
{
  "success": false,
  "errorCode": "VISIT_CERT_COOLDOWN",
  "message": "서울 성동구는 2026-06-20일부터 재인증 가능합니다.",
  "data": {
    "nextAvailableDate": "2026-06-20T00:00:00"
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

---

### 6-2. 내 방문 인증 내역 조회

```
GET /api/v1/reputations/visit-certifications/mine
```

**인증 필요:** O
**Point 소비:** X

**Response**

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": [
    {
      "sido": "서울",
      "sigu": "성동구",
      "method": "GPS",
      "certifiedAt": "2026-05-20T14:30:00",
      "lastCertifiedAt": "2026-05-20T14:30:00",
      "nextAvailableDate": "2026-06-19T14:30:00"
    },
    {
      "sido": "서울",
      "sigu": "마포구",
      "method": "COMMENT",
      "certifiedAt": "2026-05-15T16:45:00",
      "lastCertifiedAt": "2026-05-15T16:45:00",
      "nextAvailableDate": "2026-06-14T16:45:00"
    }
  ],
  "timestamp": "2026-05-28T10:00:00"
}
```

---

## 7. AI 분석 리포트

### 7-0. Battle AI 분석 자동 트리거 (내부)

```
POST /internal/api/v1/insights/battles/{battleId}/report
```

**인증 필요:** 없음 (내부 서비스 간 호출)
**Point 소비:** 없음
**호출 주체:** Battle Service (Battle 종료 시)

**처리 흐름:**
1. (type=BATTLE, reference_id=battleId) 기존 리포트 조회
   - 이미 PENDING/PROCESSING/DONE 존재 → 중복 트리거 무시 (200 반환)
2. insight_report INSERT (status=PENDING)
3. 즉시 200 응답 반환
4. @Async로 분석 실행:
   - BattleClient → /internal/api/v1/battles/{battleId}/votes/raw (투표 원본)
   - MemberPointClient → /internal/api/v1/members/batch (회원 인구통계)
   - ClaudeApiClient → AI 분석
   - insight_report UPDATE (DONE/FAILED)

**Request Body:** 없음

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

| 에러 코드 | HTTP | 상황 |
|---|---:|---|
| RESOURCE_NOT_FOUND | 404 | 존재하지 않는 battleId |
| INSIGHT_REPORT_GENERATION_FAILED | 500 | Claude API 호출 실패 (비동기, 로그만 기록) |

---

### 7-1. Market AI 정보 요약 생성

```
POST /api/v1/insights/markets/{marketId}/report
```

**인증 필요:** O
**Point 소비:** 80P (즉시 차감)

**Headers**

```
Idempotency-Key: {uuid}
```

> 동일 `Idempotency-Key`로 재요청 시 첫 번째 응답을 그대로 반환한다.
> 이미 `DONE` 상태의 리포트가 존재하면 Point를 차감하지 않고 기존 리포트를 반환한다.
> **비동기 처리로 확정**: POST는 즉시 `PENDING` 상태를 반환하고, 클라이언트가 섹션 7-3 상태 조회 API로 폴링한다.
>
> **Market AI 분석 처리 흐름:**
> 1. Market Service `/internal/api/v1/markets/{marketId}/insight-summary` 호출하여 Market 기본 정보 및 상태 확인
> 2. Market 상태가 `SETTLED`가 아니면 `INSIGHT_REPORT_SOURCE_DATA_NOT_READY` 반환
> 3. Market Service `/internal/api/v1/markets/{marketId}/insight-predictions?page=0&size=500` 호출하여 예측 데이터 수집
> 4. Member-Point Service `/internal/api/v1/members/batch` 호출하여 회원 정보 배치 조회
> 5. Claude API를 통한 AI 분석 수행

**Response - 생성 요청 수락 (비동기 기준)**

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "reportId": 2,
    "status": "PENDING",
    "reportContent": null,
    "generatedAt": null,
    "pointCharged": 80
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

> 폴링: `GET /api/v1/insights/markets/{marketId}/report/status` (2초 간격, 30초 타임아웃)

**Response - 기존 리포트 존재 (DONE, pointCharged=0)**

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "reportId": 2,
    "status": "DONE",
    "reportContent": "title: 서울 강남구 아파트 가격 전망\nsummary: ...\ncontent: |...",
    "generatedAt": "2026-05-25T15:30:00",
    "pointCharged": 0
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

> `reportContent`: Claude가 생성한 YAML 형식 문자열 (`title:` / `summary:` / `content:` 구조). 클라이언트가 직접 파싱해 렌더링한다.

**Error Codes**

| 에러 코드 | HTTP | 상황 |
|---|---:|---|
| RESOURCE_NOT_FOUND | 404 | 존재하지 않는 Market |
| INSIGHT_REPORT_ALREADY_PROCESSING | 409 | 이미 PENDING/PROCESSING 상태의 리포트가 존재함. Point 차감 없음 |
| POINT_INSUFFICIENT | 400 | 보유 Point 80P 미만 |
| INSIGHT_REPORT_SOURCE_DATA_NOT_READY | 409 | Market이 아직 SETTLED 상태가 아니거나 분석 데이터 부족. Point 차감 후 환불 처리됨 |
| INSIGHT_REPORT_GENERATION_FAILED | 500 | Claude API 호출 실패. Point 차감 후 환불 처리됨 |

---

### 7-2. Market AI 정보 요약 조회

```
GET /api/v1/insights/markets/{marketId}/report
```

**인증 필요:** O
**Point 소비:** X

**Response**

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "reportId": 2,
    "status": "DONE",
    "reportContent": "title: 서울 강남구 아파트 가격 전망\nsummary: ...\ncontent: |...",
    "generatedAt": "2026-05-25T15:30:00",
    "pointCharged": 0
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

> `reportContent`: YAML 형식 문자열. `status`가 `DONE`일 때만 값이 있고 그 외 `null`.

**Error Codes**

| 에러 코드 | HTTP | 상황 |
|---|---:|---|
| RESOURCE_NOT_FOUND | 404 | Market이 존재하지 않거나 리포트가 아직 없음 |

---

### 7-3. Market AI 정보 요약 상태 조회

```
GET /api/v1/insights/markets/{marketId}/report/status
```

**인증 필요:** O
**Point 소비:** X

> `PENDING` 또는 `PROCESSING` 상태일 때 클라이언트 폴링용으로 사용한다.
> 폴링 가이드는 섹션 7-0과 동일하다. 폴링 간격 2초, 클라이언트 최대 대기 30초.

**Response**

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "marketId": 7,
    "reportId": 2,
    "status": "PENDING",
    "retryCount": 0,
    "processingStartedAt": null
  },
  "timestamp": "2026-05-28T10:00:00"
}
```

**Error Codes**

| 에러 코드 | HTTP | 상황 |
|---|---:|---|
| RESOURCE_NOT_FOUND | 404 | 리포트 자체가 없음 |

---

## 8. 서비스 도메인 ErrorCode 목록

> [CONVENTION.md 섹션 3](../CONVENTION.md#3-에러-코드) 공통 ErrorCode에 아래를 추가한다.

| 에러 코드 | HTTP | 설명 |
|---|---:|---|
| REPUTATION_NOT_FOUND | 404 | 해당 회원의 Reputation 레코드 없음 |
| REPUTATION_RESIDENCE_CHANGE_COOLDOWN | 400 | 거주지역 변경 후 30일 미경과 |
| VISIT_CERT_COOLDOWN | 400 | 동일 지역 30일 내 재인증 시도 |
| VISIT_CERT_OUT_OF_RANGE | 400 | GPS 좌표가 지역 중심에서 3km 초과 |
| VISIT_CERT_COMMENT_NOT_FOUND | 404 | COMMENT 인증에서 댓글 ID 없음 |
| VISIT_CERT_COMMENT_REGION_MISMATCH | 400 | 댓글이 해당 지역 Battle이 아님 |
| INSIGHT_REPORT_NOT_FOUND | 404 | 분석 리포트 레코드 없음 |
| INSIGHT_REPORT_ALREADY_PROCESSING | 409 | 동일 Battle/Market 리포트가 이미 PENDING/PROCESSING 상태 |
| INSIGHT_REPORT_SOURCE_NOT_CLOSED | 400 | Battle이 아직 종료되지 않음 |
| INSIGHT_REPORT_SOURCE_DATA_NOT_READY | 409 | Battle/Market이 종료되지 않았거나 분석에 필요한 데이터 부족. Point 차감 후 환불 처리됨 |
| INSIGHT_REPORT_GENERATION_FAILED | 500 | Claude API 호출 실패. Point 차감 후 환불 처리됨 |

---

> **[v8 신규 섹션]** 아래 섹션은 v8에서 추가된 Market 분석 기능 관련 명세다.
> 문서 재구성 시 섹션 2, 3-4, 7-0-M, 7-4 위치로 이동할 것.

## 2. Market Service 연계 — `basic-info` API 소비 규칙 (v8 신규)

> Insight 서비스가 Market Service의 `GET /internal/api/v1/markets/{marketId}/basic-info`를 호출할 때의 계약 및 지역 매핑 규칙.

### 2-1. Market Service 응답 계약

```json
{
  "success": true,
  "data": {
    "marketId": 123,
    "title": "강남구 아파트 2024년 하반기 매매가격 변동률",
    "optionLabels": ["0~5% 상승", "5~10% 상승"],
    "regionSido": "서울",
    "regionSigu": "강남구"
  }
}
```

| 필드 | 타입 | Nullable | 설명 |
|---|---|---|---|
| `marketId` | Long | N | 마켓 ID |
| `title` | String | N | 마켓 제목 |
| `optionLabels` | List\<String\> | N | 옵션 레이블 목록 |
| `regionSido` | String | **Y** | 시도명. 전국 마켓은 `"전국"`, 미설정은 null |
| `regionSigu` | String | **Y** | 시군구명. 시도 전체·전국 마켓은 null |

**지역 단위별 값 계약**

| 마켓 유형 | regionSido | regionSigu |
|---|---|---|
| 시군구 마켓 | `"서울"` | `"강남구"` |
| 시도 전체 마켓 | `"경기"` | `null` |
| 전국 마켓 | `"전국"` | `null` |

**Market Service 에러 코드**

| HTTP | Market errorCode | Insight 변환 |
|---|---|---|
| 404 | `MARKET_NOT_FOUND` | `RESOURCE_NOT_FOUND` |

### 2-2. Insight DB 지역 필터 매핑

`basic-info` 응답의 `regionSido`/`regionSigu`로 `public_data_snapshot` 조회 시 적용 규칙.
`public_data_snapshot`에 `region_sigu` 컬럼이 없으므로 `regionSigu`는 `region_fullpath LIKE` 조건으로 대체한다.

```sql
-- 전국 마켓 (regionSido = "전국")
WHERE region_sido = '전국'

-- 시도 전체 마켓 (regionSido = "서울", regionSigu = null)
WHERE region_sido = :regionSido

-- 시군구 마켓 (regionSido = "서울", regionSigu = "강남구")
WHERE region_sido = :regionSido
  AND region_fullpath LIKE CONCAT('%', :regionSigu, '%')
```

---

## 3-4. 관리자 마켓 가격 이력 조회 (v8 신규)

**관리자 전용.** 마켓 지역의 공공 매매가격지수 시계열과 최종 예측 분포를 반환한다.

```
GET /api/v1/admin/insights/markets/{marketId}/price-history
Headers:
  X-Member-Id: {memberId}
```

**응답 (200 OK)**

```json
{
  "success": true,
  "data": {
    "regionSido": "서울",
    "regionSigu": "강남구",
    "dataType": "WEEKLY_PRICE_INDEX",
    "priceHistory": [
      { "referenceDate": "2026-04-28", "value": 103.08 },
      { "referenceDate": "2026-05-05", "value": 103.29 },
      { "referenceDate": "2026-06-16", "value": 104.71 }
    ],
    "latestPredictionDistribution": [
      { "optionLabel": "0~5% 상승", "ratio": 0.657, "isResult": true },
      { "optionLabel": "5~10% 상승", "ratio": 0.343, "isResult": false }
    ]
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `regionSido` | String\|null | 마켓 지역 시도 |
| `regionSigu` | String\|null | 마켓 지역 시군구 |
| `dataType` | String | `WEEKLY_PRICE_INDEX` 또는 `MONTHLY_PRICE_INDEX` |
| `priceHistory` | Array | `referenceDate`(오름차순) + `value` 목록. 데이터 없으면 빈 배열 |
| `latestPredictionDistribution` | Array | SETTLED 마켓만 채워짐, 그 외 빈 배열 |

**데이터 조회 우선순위:** 주간(최근 8주) → 없으면 월간(최근 6개월) 폴백

**Error Codes**

| 에러 코드 | HTTP | 상황 |
|---|---:|---|
| `RESOURCE_NOT_FOUND` | 404 | 존재하지 않는 marketId |
| `EXTERNAL_SERVICE_UNAVAILABLE` | 503 | Market Service 연결 불가 |

---

## 7-0-M. Market SETTLED 자동 트리거 (v8 신규, 내부 API)

> Market Service가 Market 정산 완료(SETTLED) 시 호출.

```
POST /internal/api/v1/insights/markets/{marketId}/report
```

- Point 차감 없음
- 이미 리포트 존재 시 상태 무관 skip (멱등성)
- SETTLED 상태가 아니면 `INSIGHT_REPORT_SOURCE_DATA_NOT_READY` 반환

**응답 (200 OK)**

```json
{ "success": true, "data": null }
```

**Error Codes**

| 에러 코드 | HTTP | 상황 |
|---|---:|---|
| `INSIGHT_REPORT_SOURCE_DATA_NOT_READY` | 409 | Market이 SETTLED 아님 |
| `RESOURCE_NOT_FOUND` | 404 | 존재하지 않는 marketId |

---

## 7-4. Market 공공 데이터 참고 자료 조회 (v8 신규)

진행 중(ACTIVE) 마켓에서 베팅 전 공공 데이터 기반 AI 참고 요약을 제공한다.
포인트 차감 없음. 결과 저장 없음(실시간 생성). Claude extended thinking 적용.

```
GET /api/v1/insights/markets/{marketId}/public-data-reference
Headers:
  X-Member-Id: {memberId}
```

**응답 (200 OK)**

```json
{
  "success": true,
  "data": {
    "title": "서울 강남구 아파트 매매가격지수 최근 동향",
    "summary": "강남구 매매가격지수는 최근 8주간 103.08에서 104.71로 완만한 상승세를 보이고 있습니다.",
    "content": "## 최근 시장 지표\n...\n## 시장 해석\n...\n## 베팅 참고 포인트\n1. ...",
    "dataAsOf": "2026-06-16T00:00:00"
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `title` | String | AI 생성 제목 |
| `summary` | String | 2~3문장 핵심 요약 |
| `content` | String | Markdown 본문 (`## 최근 시장 지표` / `## 시장 해석` / `## 베팅 참고 포인트`) |
| `dataAsOf` | String\|null | 가장 최근 공공 데이터 기준일 (ISO 8601). 공공 데이터 없으면 null |

**동작 규칙**

- Market 상태(ACTIVE/SETTLED 등) **무관하게** 조회 가능
- Market Service 연결 실패 시 지역 필터 없이 전체 공공 데이터로 자동 fallback
- 공공 데이터가 없으면 200 OK + 안내 메시지 반환 (에러 아님)
- 응답 시간 3~5초 예상 (Claude API 호출) → 클라이언트 로딩 처리 권장

**Error Codes**

| 에러 코드 | HTTP | 상황 |
|---|---:|---|
| `RESOURCE_NOT_FOUND` | 404 | 존재하지 않는 marketId (Market Service 확인된 경우) |
| `EXTERNAL_SERVICE_UNAVAILABLE` | 503 | Market Service 연결 불가 (fallback 실패 시) |

