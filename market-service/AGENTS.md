# AGENTS.md — Market Service

> 루트 `AGENTS.md`의 공통 지침을 상속한다.
> 이 파일은 Market Service에 특화된 구현 컨텍스트를 정의한다.
> 작업자는 반드시 루트 `AGENTS.md`를 먼저 읽고, 이후 이 파일을 읽는다.

---
## 0. 작업 우선순위

작업자는 반드시 아래 우선순위를 따른다.

1. 현재 사용자(팀장)의 작업 지시
2. `MARKET_API_SPEC.md`
3. `MARKET_ERROR_CODE.md`
4. `MARKET_ERD.md`
5. `MARKET_FAILURE_SCENARIO.md`
6. `AGENTS.md`

`AGENTS.md`는 구현 보조 가이드이다.

공식 문서(`MARKET_API_SPEC.md`, `MARKET_ERROR_CODE.md`, `MARKET_ERD.md`, `MARKET_FAILURE_SCENARIO.md`)와 충돌하는 경우 공식 문서를 우선한다.

공식 문서와 충돌하는 내용을 발견하면:

1. 임의로 새 정책을 만들지 않는다.
2. 공식 문서를 기준으로 구현한다.
3. 필요 시 AGENTS.md를 수정 대상으로 보고한다.

---

## 0-1. 작업 방식

새 기능을 구현하기 전에 반드시 아래 순서로 확인한다.

1. API 명세에서 endpoint, request, response, ErrorCode를 확인한다.
2. ERD에서 테이블, 컬럼, 제약조건을 확인한다.
3. ERROR_CODE에서 상태 전이와 실패 처리를 확인한다.
4. FAILURE_SCENARIO에서 타임아웃, 재시도, 멱등성 정책을 확인한다.
5. 공식 문서와 AGENTS.md가 충돌하면 공식 문서를 우선한다.

### 금지 사항

* 문서에 없는 MarketStatus를 새로 만들지 않는다.
* 문서에 없는 PredictionStatus를 새로 만들지 않는다.
* 문서에 없는 ErrorCode를 임의로 추가하지 않는다.
* 문서에 없는 DB 컬럼을 임의로 추가하지 않는다.
* 문서에 없는 API 응답 구조를 임의로 변경하지 않는다.
* 멱등성 키 형식을 임의로 축약하지 않는다.
* referenceType/referenceId와 idempotencyKey를 같은 개념으로 취급하지 않는다.
* 공식 문서에 정의된 정책을 AGENTS.md 기준으로 덮어쓰지 않는다.

### 애매한 경우

다음 상황에서는 임의 구현보다 문서 정합성을 우선한다.

* 문서 간 정의가 서로 다름
* 상태명(State)이 서로 다름
* ErrorCode 정의가 다름
* 멱등성 키 형식이 다름
* API 응답 필드가 다름

이 경우:

* 공식 문서를 기준으로 구현한다.
* 어떤 문서가 충돌했는지 작업 결과에 보고한다.

---

## 0-2. Codex 작업 보고 규칙

작업 완료 후 반드시 아래 형식으로 보고한다.

```text
수정 파일:
- ...

주요 변경:
- ...

추가/수정 테스트:
- ...

검증 결과:
- ./gradlew.bat test 결과
- git diff --check 결과
- Docker MySQL 수동 검증 여부

문서 충돌 발견:
- ...

변경하지 않은 정책:
- ...
```

---

## 0-3. AGENTS.md의 역할

AGENTS.md는 공식 정책 문서가 아니다.

역할:

* 구현 가이드
* 작업 체크리스트
* 실수 방지 규칙
* 구현 순서 안내

역할이 아닌 것:

* API 명세의 원본
* ERD의 원본
* ErrorCode 정의의 원본
* 상태(State) 정의의 원본

정책 변경은 반드시 공식 문서에서 먼저 수행한다.
그 후 AGENTS.md를 동기화한다.


---

## 1. 서비스 컨텍스트

- **서비스명:** Market Service
- **패키지 루트:** `com.todongsan.marketservice`
- **아키텍처:** REST 기반 MSA
- **DB 접근:** MyBatis
- **Point 관리:** 직접 관리하지 않고 Member-Point Service를 REST로 호출한다.

### 담당 문서

- `docs/market/MARKET_ERD.md` — Market ERD 최신본
- `docs/market/MARKET_API_SPEC.md` — Market API 명세 최신본
- `docs/market/MARKET_ERROR_CODE.md` — Market 도메인 에러 코드
- `docs/market/MARKET_FAILURE_SCENARIO.md` — Market 장애 시나리오
- 연계 작업 시 `docs/member-point/MEMBER_POINT_API_SPEC.md`도 함께 확인한다.

---

## 2. 핵심 도메인 테이블

| 테이블 | 역할 | 주요 제약 |
|---|---|---|
| `market` | Market 주제, 판정 기준, 상태, 전체 풀 정보 | 상태·마감일 인덱스, `result_option_id`는 논리 참조 |
| `market_option` | 선택지, 수치 구간, 현재 가격, 선택지별 풀 정보 | `(market_id, option_code)` UNIQUE |
| `market_prediction` | 사용자별 예측 참여 기록 | `(market_id, member_id)` UNIQUE, `point_spend_idempotency_key` UNIQUE |
| `market_price_history` | 선택지 가격 변동 이력 | 예측 확정 등 가격 변경 이벤트 추적 |
| `market_settlement` | Market 단위 정산 결과 | `market_id` UNIQUE |
| `market_settlement_detail` | 사용자별 정산 지급 상세 | `prediction_id` UNIQUE, `idempotency_key` UNIQUE |
| `market_void` | Market 무효 처리 기록 | `market_id` UNIQUE |
| `market_refund_detail` | 사용자별 환불 상세 | `prediction_id` UNIQUE, `idempotency_key` UNIQUE |

---

## 3. 가격 모델 정책

### 3.1 기본 가격 모델

Market Service는 `POOL_SHARE` 가격 모델만 사용한다.

```text
각 선택지의 effectivePool = realPoolAmount + virtualPoolAmount
전체 effectivePool = 모든 선택지의 effectivePool 합

currentPrice = 해당 선택지 effectivePool / 전체 effectivePool
```

### 3.2 realPoolAmount

`realPoolAmount`는 사용자가 실제로 예측 참여에 사용한 포인트의 누적 합이다.

```text
realPoolAmount = 실제 유저 참여 포인트 풀
```

예측 참여가 확정되면 해당 선택지의 `realPoolAmount`가 증가한다.

### 3.3 virtualPoolAmount

`virtualPoolAmount`는 가격 안정화를 위한 가상 유동성이다.

```text
virtualPoolAmount = Market 생성 시 부여되는 가상 유동성
```

정책:

- Market 생성 시 선택지별로 설정한다.
- Market 생성 이후 수정할 수 없다.
- 사용자 정산·환불 대상 금액에 포함하지 않는다.
- 실제 포인트가 아니므로 Member-Point Service에 기록하지 않는다.
- 가격 계산에는 포함한다.
- 가격 이력 조회 시 `market_option.virtual_pool_amount`에서 조회한다.
- `market_price_history`에는 별도 저장하지 않는다.

`virtualPoolAmount`를 변경 가능하게 만들면 과거 가격 이력의 의미가 왜곡될 수 있다. 따라서 현재 프로젝트에서는 변경 불가 정책을 유지한다.

### 3.4 가격 이력 저장 기준

가격 변경 이벤트가 발생하면 `market_price_history`에 가격 변경 결과를 저장한다.

저장 대상:

- optionId
- price
- realPoolAmount
- contractQuantity
- createdAt

저장하지 않는 대상:

- virtualPoolAmount

이유:

```text
virtualPoolAmount는 생성 후 변경 불가이므로 market_option에 있는 원본 값을 참조하면 충분하다.
```

---

## 4. 예측 참여 흐름

예측 참여는 외부 Point 차감과 내부 가격 확정이 함께 엮이는 고위험 흐름이다.
반드시 아래 순서를 지킨다.

현재 2차 구현에서도 Member-Point 실제 HTTP 연동은 하지 않는다.
`MemberPointClient` 내부 예외 모델로 명확한 실패는 FAILED, 타임아웃·외부 오류·응답 불명확은 POINT_UNKNOWN으로 처리한다.
추후 실제 Member-Point 연동 시에도 아래 transaction boundary를 유지한다.

```text
[예측 참여 흐름]

1. 요청 검증
   - marketId 유효성 확인
   - optionId 유효성 확인
   - amount 범위 확인
   - Market 상태 및 마감 여부 확인
   - `(market_id, member_id)` 기준 기존 Prediction row 락 조회
   - 기존 row가 있으면 FAILED 상태인지 확인

2. POINT_PENDING 예측 기록 생성 또는 FAILED row 재사용 후 커밋
   - market_prediction.status = POINT_PENDING
   - 신규 row는 attemptNo = 1
   - FAILED row 재시도는 같은 predictionId를 유지하고 attemptNo 증가
   - FAILED row 재시도 시 optionId, pointAmount를 새 요청 값으로 갱신
   - FAILED 이외 상태는 MARKET_ALREADY_PREDICTED
   - attempt별 point_spend_idempotency_key 저장

3. DB 락을 잡지 않은 상태에서 Member-Point spend 호출
   - 외부 HTTP 호출 중 DB 락을 유지하지 않는다.

4. Member-Point spend 성공 시 별도 가격 확정 트랜잭션 시작

5. 가격 확정 트랜잭션
   - Market row를 먼저 락 조회한다.
   - 해당 Market의 모든 MarketOption row를 optionId 오름차순으로 락 조회한다.
   - 선택지 realPoolAmount를 증가시킨다.
   - 모든 선택지 currentPrice를 재계산한다.
   - market_price_history를 저장한다.
   - market_prediction.status를 CONFIRMED로 전환한다.

6. 포인트 부족 등 명확한 실패
   - market_prediction.status = FAILED

7. 타임아웃 등 처리 여부 불명확
   - market_prediction.status = POINT_UNKNOWN
   - 대사 대상에 포함한다.
```

### 중요한 금지 사항

```text
외부 HTTP API 호출 중 DB 락을 유지하지 않는다.
```

Member-Point Service 호출은 반드시 DB 락 없는 상태에서 수행한다.
가격 확정 트랜잭션은 Point 차감 성공 이후 별도로 시작한다.

---

## 5. 예측 참여 멱등성 정책

예측 참여에서는 아래 세 개를 구분한다.

1. DB 중복 참여 방지
2. Member-Point 차감 멱등성 키
3. Member-Point 거래 참조값

### 5.1 DB 중복 참여 방지

사용자는 하나의 Market에 한 번만 참여할 수 있다.

```text
UNIQUE(market_id, member_id)
```

이 제약으로 동일 사용자의 동일 Market 중복 참여를 차단한다.

### 5.2 Point 차감 멱등성 키

예측 참여 API 요청 헤더의 멱등성 키는 Market과 Member 기준으로 생성한다.

```text
MARKET_PREDICTION_SPEND:market:{marketId}:member:{memberId}
```

예시:

```text
MARKET_PREDICTION_SPEND:market:10:member:5
```

Market Service가 Member-Point 차감 요청에 사용하는 키에는 attemptNo를 추가한다.

```text
MARKET_PREDICTION_SPEND:market:{marketId}:member:{memberId}:attempt:{attemptNo}
```

주의:

- `optionId`를 포함하지 않는다.
- `predictionId`를 멱등성 키로 사용하지 않는다.
- 클라이언트는 재시도 시에도 attempt suffix 없는 요청 헤더 키를 사용한다.
- 명확한 실패(`FAILED`) 후 재시도에서만 attemptNo를 증가시킨다.
- `POINT_PENDING`, `POINT_UNKNOWN`, `CONFIRMED`, `SETTLED`, `REFUND_PENDING`, `REFUND_UNKNOWN`, `REFUNDED` 상태에서는 재시도하지 않는다.

### 5.3 Member-Point referenceType / referenceId

Member-Point Service에는 거래의 출처를 추적할 수 있도록 아래 값을 전달한다.

```text
referenceType = MARKET_PREDICTION
referenceId = predictionId
```

정리하면 다음과 같다.

| 구분 | 값 | 목적 |
|---|---|---|
| DB Unique | `(market_id, member_id)` | 동일 Market 중복 참여 차단 |
| API 요청 헤더 idempotencyKey | `MARKET_PREDICTION_SPEND:market:{marketId}:member:{memberId}` | Market, Member 기준 요청 검증 |
| Point 차감 idempotencyKey | `MARKET_PREDICTION_SPEND:market:{marketId}:member:{memberId}:attempt:{attemptNo}` | attempt별 Point 차감 중복 요청 방지 |
| referenceType | `MARKET_PREDICTION` | Member-Point 거래가 Market Prediction에서 발생했음을 표시 |
| referenceId | `predictionId` | Market 예측 기록과 Point 거래 연결 |

---

## 6. 예측 상태 정책

`market_prediction.status`는 다음 의미를 가진다.

| 상태 | 의미 | 후속 처리 |
|---|---|---|
| `POINT_PENDING` | 예측 기록은 생성됐지만 Point 차감 결과가 아직 확정되지 않음 | 3분 이상 고착 시 대사 대상 |
| `CONFIRMED` | Point 차감과 가격 확정이 모두 완료됨 | 정산 대상 |
| `FAILED` | Point 부족 등 명확한 실패 | 정산 대상 아님 |
| `POINT_UNKNOWN` | 타임아웃 등으로 Point 처리 여부 불명확 | Member-Point 거래 조회로 대사 |

정산 대상은 반드시 `CONFIRMED` 예측만 포함한다.

---

## 7. 정산 정책

정산 시작은 Atomic Update로 권한을 획득한다.

```text
CLOSED 또는 DATA_PENDING
→ SETTLEMENT_IN_PROGRESS
```

정산 시작 Atomic Update:

```sql
UPDATE market
SET status = 'SETTLEMENT_IN_PROGRESS'
WHERE id = :marketId
  AND status IN ('CLOSED', 'DATA_PENDING');
```

정산 기준:

- `CONFIRMED` 예측만 정산 대상에 포함한다.
- 정답 선택지에 참여한 사용자만 보상 대상이다.
- 사용자별 정산 지급 detail item마다 별도의 멱등성 키를 사용한다.
- 일부 지급 실패 시 Market 상태를 `SETTLEMENT_IN_PROGRESS`로 유지한다.
- 재시도 시 실패한 detail item만 다시 처리한다.
- 탈퇴 회원도 시스템 정산 대상에 포함한다.

정산 detail 멱등성 키 예시:

```text
MARKET_SETTLEMENT_REWARD:market:{marketId}:prediction:{predictionId}:member:{memberId}
```

---

## 8. 무효 처리 및 환불 정책

무효 처리 가능 조건:

- 아직 정산 완료되지 않은 Market
- `SETTLEMENT_IN_PROGRESS`, `SETTLED` 상태는 VOIDED 처리할 수 없다.

환불 기준:

- `CONFIRMED` 예측만 환불 대상이다.
- 사용자별 환불 detail item마다 별도의 멱등성 키를 사용한다.
- 환불 타임아웃은 `REFUND_UNKNOWN`으로 기록한다.
- `REFUND_UNKNOWN`은 Member-Point 거래 조회로 대사한다.
- 탈퇴 회원도 시스템 환불 대상에 포함한다.

환불 detail 멱등성 키 예시:

```text
MARKET_REFUND:market:{marketId}:prediction:{predictionId}:member:{memberId}
```

---

## 9. 연계 서비스

### 9.1 내가 호출하는 서비스

| 서비스 | 엔드포인트 | 목적 | 주의사항 |
|---|---|---|---|
| Member-Point Service | `POST /api/v1/points/spend` | 예측 참여 Point 차감 | `referenceType=MARKET_PREDICTION`, `referenceId=predictionId`, idempotencyKey 필수 |
| Member-Point Service | `GET /api/v1/points/transactions?idempotencyKey={key}` | 차감·환불 처리 상태 대사 | `POINT_UNKNOWN`, `REFUND_UNKNOWN`, 고착 상태 복구 |
| Member-Point Service | `POST /api/v1/points/settlements` | 정산 보상 지급 | detail item별 멱등성 키 |
| Member-Point Service | `POST /api/v1/points/refunds` | 무효 처리 환불 | detail item별 멱등성 키 |

### 9.2 나를 호출하는 서비스

| 서비스 | 엔드포인트 | 목적 |
|---|---|---|
| Insight-Reputation Service | `GET /internal/api/v1/markets/{marketId}/insight-summary` | SETTLED Market 요약 및 선택지 집계 조회 |
| Insight-Reputation Service | `GET /internal/api/v1/markets/{marketId}/insight-predictions` | SETTLED Market 예측 원본 페이지 조회 |

Insight-Reputation 내부 조회는 반드시 `SETTLED` Market만 허용한다.
회원 정보는 `memberId`까지만 제공한다.

---

## 10. 응답 정책

모든 API는 공통 응답 포맷을 따른다.

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {},
  "timestamp": "2026-06-01T13:52:00"
}
```

실패 응답 예시:

```json
{
  "success": false,
  "errorCode": "MARKET_NOT_FOUND",
  "message": "Market을 찾을 수 없습니다.",
  "data": null,
  "timestamp": "2026-06-01T13:52:00"
}
```

Decimal 필드는 응답 DTO에서 `BigDecimal`로 유지한다.
JSON 직렬화 시 `@JsonSerialize(using = ToStringSerializer.class)`를 사용하여 JSON Number가 아니라 JSON String으로 응답한다.

예시:

```json
{
  "currentPrice": "0.31250000",
  "totalPoolAmount": "25000.00",
  "realPoolAmount": "15000.00",
  "virtualPoolAmount": "10000.00"
}
```

---

## 11. 이 서비스의 성공 기준

```text
[관리자 Market 생성]
- POST /api/v1/admin/markets
- 생성 직후 MarketStatus는 PENDING이다.
- Market 기본 정보와 MarketOption을 일괄 저장한다.
- NUMERIC_RANGE, 열린 구간, optionCode 중복, virtualPoolAmount를 검증한다.
- 초기 currentPrice는 virtualPoolAmount 비율로 계산한다.
- totalPool, feeAmount, settlementPool을 초기화하고 initialVirtualLiquidity를 저장한다.
- 생성 시 ACTIVE로 전환하지 않는다.
- 생성 시 예측 참여를 허용하거나 Member-Point Service를 호출하지 않는다.
- 생성 시 PriceHistory를 만들지 않는다.

[관리자 Market 활성화]
- PATCH /api/v1/admin/markets/{marketId}/activate
- PENDING 상태만 ACTIVE로 전환한다.
- ACTIVE, CLOSED, DATA_PENDING, SETTLEMENT_IN_PROGRESS, SETTLED, VOIDED 상태는 활성화할 수 없다.
- Market 존재 여부, PENDING 상태, 미래 closeAt, 최소 2개 선택지, 양수 initialVirtualLiquidity를 검증한다.
- 활성화 시 Member-Point Service를 호출하지 않는다.
- 활성화 시 가격을 재계산하지 않고 기존 MarketOption currentPrice를 유지한다.

[Market 목록 조회]
- GET /api/v1/markets
- page, size, status, keyword 필터 지원
- 목록에서는 상세 전용 풀 금액을 제외한다.
- Market별 옵션 N+1 조회를 피한다.


[Market 상세 조회]
- GET /api/v1/markets/{marketId}
- 선택지별 realPoolAmount, virtualPoolAmount를 제공한다.
- 존재하지 않는 Market은 MARKET_NOT_FOUND를 반환한다.

[Market 가격 이력 조회]
- GET /api/v1/markets/{marketId}/price-history
- optionId 필터를 지원한다.
- created_at DESC, id DESC 정렬을 유지한다.
- virtualPoolAmount는 market_option에서 조회한다.

[Market 예측 참여]
- POST /api/v1/markets/{marketId}/predictions
- POINT_PENDING 선저장 후 포인트 차감
- Point 차감 및 가격 확정 성공: CONFIRMED
- 잔액 부족 등 확정 실패: FAILED
- 타임아웃 등 처리 여부 불명확: POINT_UNKNOWN
- 3분 이상 POINT_PENDING 고착 건은 대사 대상
- FAILED는 같은 Prediction row의 attemptNo를 증가시켜 재시도 허용
- FAILED 이외 기존 Prediction은 중복 참여 차단
- 포인트 범위: 최소 10P, 최대 500P

[내 예측 상태 조회]
- GET /api/v1/markets/{marketId}/predictions/me
- X-Member-Id 임시 헤더로 해당 회원의 Prediction만 조회한다.
- POINT_PENDING, POINT_UNKNOWN 상태에서는 priceSnapshot, contractQuantity가 null일 수 있다.

[정산]
- POST /api/v1/admin/markets/{marketId}/settle
- Atomic Update로 중복 정산 실행 차단
- detail item별 멱등성 키로 중복 지급 방지
- 일부 지급 실패 시 실패 건만 재시도

[DATA_PENDING 상태]
- 정산일 API 미수신 → DATA_PENDING 전환
- 예상 수집일로부터 최대 3일간 유지
- 이후 관리자 확인 대상으로 처리

[Insight-Reputation 내부 조회]
- SETTLED Market만 조회 허용
- summary와 prediction page API를 분리
- 회원 정보는 memberId까지만 제공
```

---

## 12. 이 서비스에서 하지 않는 것

```text
✗ Point를 직접 DB에서 관리하지 않는다. Member-Point Service REST 호출만 사용한다.
✗ Battle 주제, 사용자 투표 결정 주제는 Market으로 등록하지 않는다.
✗ 공공 API 데이터가 없어도 임의로 결과를 판정하지 않는다.
✗ CONFIRMED가 아닌 예측은 정산 대상에 포함하지 않는다.
✗ DB 락을 잡은 트랜잭션 안에서 외부 HTTP API를 호출하지 않는다.
✗ Insight-Reputation용 분석 결과를 Market Service에 저장하지 않는다.
✗ virtualPoolAmount를 정산·환불 금액에 포함하지 않는다.
✗ Market 생성 이후 virtualPoolAmount를 수정하지 않는다.
```

---

## 13. 작업 전 체크리스트

```text
[ ] 루트 AGENTS.md를 읽었는가?
[ ] docs/market/MARKET_ERD.md 최신본을 읽었는가?
[ ] docs/market/MARKET_API_SPEC.md 최신본을 읽었는가?
[ ] docs/market/MARKET_ERROR_CODE.md를 읽었는가?
[ ] docs/market/MARKET_FAILURE_SCENARIO.md를 읽었는가?
[ ] 연계 작업이면 docs/member-point/MEMBER_POINT_API_SPEC.md를 읽었는가?

[가격]
[ ] 가격 모델은 POOL_SHARE만 사용하고 있는가?
[ ] currentPrice 계산에 realPoolAmount와 virtualPoolAmount를 모두 반영했는가?
[ ] virtualPoolAmount를 생성 후 수정 불가로 유지하고 있는가?
[ ] virtualPoolAmount를 정산·환불 금액에 포함하지 않았는가?
[ ] Decimal 필드를 JSON String으로 응답하는가?

[NUMERIC_RANGE]
- `rangeMin = null`은 -무한대에서 시작하는 구간으로 간주한다.
- `rangeMax = null`은 +무한대까지 이어지는 구간으로 간주한다.
- `rangeMin`, `rangeMax`가 모두 null인 구간은 금지한다.
- `rangeMin = null`인 구간과 `rangeMax = null`인 구간은 각각 최대 1개만 허용한다.
- `rangeMin`, `rangeMax`에는 음수 값을 허용한다.
- 인접 구간의 경계값은 정확히 하나의 구간에만 포함되어야 한다.
- 정렬된 인접 구간 사이의 빈 구간은 금지한다.
- 첫 구간의 `rangeMin = null`, 마지막 구간의 `rangeMax = null`을 강제하지 않는다.

[예측 참여]
[ ] 예측 참여 흐름 순서가 POINT_PENDING 커밋 → spend 호출 → 가격 확정 트랜잭션 순서인가?
[ ] 외부 HTTP 호출 중 DB 락을 유지하고 있지 않은가?
[ ] 동일 Market 사용자 중복 참여를 `(market_id, member_id)`로 차단하는가?
[ ] Point 차감 idempotencyKey가 `MARKET_PREDICTION_SPEND:market:{marketId}:member:{memberId}:attempt:{attemptNo}`인가?
[ ] Member-Point 요청에 `referenceType=MARKET_PREDICTION`, `referenceId=predictionId`를 세팅하는가?
[ ] Market row와 모든 MarketOption row를 고정 순서로 락 조회하는가?
[ ] POINT_UNKNOWN과 3분 이상 POINT_PENDING 고착 건을 대사하는가?

[정산/환불]
[ ] CONFIRMED 예측만 정산·환불 대상에 포함하는가?
[ ] 정산·환불 detail item별 멱등성 키를 사용하는가?
[ ] 일부 실패 시 전체를 성공 처리하지 않고 재시도 가능 상태를 유지하는가?
[ ] 탈퇴 회원도 시스템 정산·환불 대상에 포함하는가?

[조회/연계]
[ ] Market 목록 조회에서 옵션 N+1 조회가 발생하지 않는가?
[ ] 가격 이력 조회 정렬이 `created_at DESC, id DESC`인가?
[ ] Insight-Reputation 내부 조회는 SETTLED Market만 허용하는가?
[ ] Insight-Reputation에 memberId까지만 제공하는가?

[운영]
[ ] Scheduler가 limit 기반 chunk 처리를 하는가?
[ ] H2 테스트 스키마와 실제 MySQL DDL 컬럼명이 일치하는가?
```
