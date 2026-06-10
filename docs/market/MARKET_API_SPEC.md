# MARKET_API_SPEC_v4.md

> Market 서비스 API 명세서.  
> 본 버전은 다음 피드백을 반영한다.
>
> - 가격 갱신 동시성 제어는 선택한 option row가 아니라 **Market row + 해당 Market의 모든 option row**를 기준으로 한다.
> - Member-Point HTTP 호출은 DB 비관적 락을 잡은 트랜잭션 안에서 수행하지 않는다.
> - 예측 참여는 `POINT_PENDING` 선저장 → 포인트 차감 호출 → 가격 확정 트랜잭션 순서로 처리한다.
> - Member-Point 연동 시 `referenceType=MARKET_PREDICTION`, `referenceId=predictionId`를 전달한다.
> - 정산/환불은 batch API를 유지하되, item별 `idempotencyKey`로 멱등성을 보장한다.
> - Insight-Reputation Service 연계를 위해 SETTLED Market 기준 요약/Prediction 페이지 조회 내부 API를 제공한다.
> - 본 버전은 기존 Pool Share 모델을 **Pool Share 기반 즉시 참여형 예측시장**으로 명확히 정의한다.
> - `initialVirtualLiquidity`는 초기 가격뿐 아니라 시장 깊이 역할을 수행한다.
> - 예측 참여 전 예상 가격/계약 수량을 확인하는 Quote API를 추가한다.
> - 실제 예측 참여는 Quote가 아니라 체결 시점의 최신 Pool 상태를 기준으로 확정한다.
> - MVP에서는 가격 변동 허용 범위(slippage tolerance)를 필수로 두지 않고, 실제 체결 가격이 달라질 수 있음을 안내한다.

---

## 1. 공통 API 정책

### 1-1. 공통 응답 포맷

#### 성공 응답

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {},
  "timestamp": "2026-05-29T15:30:00"
}
```

#### 실패 응답

```json
{
  "success": false,
  "errorCode": "MARKET_CLOSED",
  "message": "이미 마감된 Market입니다.",
  "data": null,
  "timestamp": "2026-05-29T15:30:00"
}
```

---

### 1-2. Decimal 응답 정책

Market 서비스는 변동 배당률, 가격 스냅샷, 계약 수량, 풀 금액, 정산 금액 등 정밀도가 중요한 소수점 데이터를 다룬다.

따라서 Market API에서 소수점 정밀도가 중요한 값은 JSON Number가 아니라 **String**으로 응답한다.

| 필드 | 설명 | 예시 |
|---|---|---|
| `currentPrice` | 현재 선택지 가격 | `"0.18342111"` |
| `priceSnapshot` | 예측 확정 시점 가격 | `"0.18342111"` |
| `contractQuantity` | 구매 계약 수량 | `"12.50000000"` |
| `realPoolAmount` | 실제 참여 포인트 풀 | `"15000.00"` |
| `virtualPoolAmount` | 가격 안정화를 위한 가상 풀 | `"5000.00"` |
| `pointAmount` | 포인트 금액 | `"100.00"` |
| `settlementAmount` | 정산 지급 금액 | `"185.30"` |
| `refundAmount` | 환불 금액 | `"100.00"` |

구현 원칙:

```text
서버 내부 계산: BigDecimal
DB 저장: DECIMAL
API 응답 DTO 필드: BigDecimal 유지
JSON 직렬화: 기본적으로 BigDecimal을 String으로 출력
scientific notation 방지를 위해 정산 응답 등 일부 DTO는 BigDecimalPlainStringSerializer 사용
```

---

### 1-3. Member-Point 연동 referenceType 정책

Market이 Member-Point에 포인트 차감, 정산 지급, 환불 요청을 보낼 때는 포인트 거래의 원인 객체를 명확히 하기 위해 `referenceType`, `referenceId`를 함께 전달한다.

Market에서는 항상 다음 기준을 따른다.

```text
referenceType = MARKET_PREDICTION
referenceId = predictionId
```

| Market 상황 | Member-Point type | referenceType | referenceId |
|---|---|---|---|
| 예측 참여 포인트 차감 | `SPEND_MARKET` | `MARKET_PREDICTION` | predictionId |
| 정산 보상 지급 | `SETTLE_MARKET` | `MARKET_PREDICTION` | predictionId |
| 무효 처리 환불 | `REFUND_MARKET` | `MARKET_PREDICTION` | predictionId |

> `referenceType`, `referenceId`는 Member-Point의 `point_history`에 기록되는 값이다.  
> Market DB에는 `prediction_id`가 이미 있으므로 `referenceType`, `referenceId`를 중복 컬럼으로 저장하지 않는다.

---

### 1-4. Idempotency-Key 정책

포인트 차감, 정산 보상 지급, 환불 요청은 반드시 멱등성 키를 사용한다.

| 작업 | Idempotency-Key |
|---|---|
| 예측 참여 API 요청 헤더 | `MARKET_PREDICTION_SPEND:market:{marketId}:member:{memberId}` |
| 예측 참여 포인트 차감 | `MARKET_PREDICTION_SPEND:market:{marketId}:member:{memberId}:attempt:{attemptNo}` |
| 정산 batch 요청 추적 | `MARKET_SETTLEMENT_BATCH:market:{marketId}:settlement:{settlementId}:attempt:{attemptNo}` |
| 정산 보상 지급 item | `MARKET_SETTLEMENT_REWARD:market:{marketId}:prediction:{predictionId}:member:{memberId}` |
| 무효 처리 환불 | `MARKET_REFUND:market:{marketId}:prediction:{predictionId}:member:{memberId}` |

Gateway 경유 예측 참여 요청의 클라이언트용 `Idempotency-Key`는 아래 형식을 정확히 사용한다.

```text
MARKET_PREDICTION_SPEND:market:{marketId}:member:{memberId}
```

검증 규칙:

- `Idempotency-Key`의 `{marketId}`는 path variable `marketId`와 일치해야 한다.
- `Idempotency-Key`의 `{memberId}`는 Gateway/JWT가 주입한 `X-Member-Id`와 일치해야 한다.
- 둘 중 하나라도 일치하지 않으면 `VALIDATION_FAILED`를 반환한다.
- 클라이언트가 보내는 `Idempotency-Key`에는 `:attempt:{attemptNo}`를 포함하지 않는다.
- `:attempt:{attemptNo}`는 Market Service가 Member-Point 포인트 차감 요청을 만들 때 내부적으로만 붙인다.

예시:

```http
POST /api/v1/markets/6/predictions
X-Member-Id: 1
Idempotency-Key: MARKET_PREDICTION_SPEND:market:6:member:1
```

Future refactor TODO:

- 현재 엄격한 도메인 키 정책은 유지한다.
- 추후 외부 클라이언트가 UUID 기반 `Idempotency-Key`를 보내는 방식으로 바꿀 수 있다.
- 이 경우에도 Market Service는 path variable `marketId`와 Gateway/JWT 주입 `X-Member-Id`를 기준으로 예측 주체를 확정해야 한다.
- Member-Point 포인트 차감용 내부 키는 `MARKET_PREDICTION_SPEND:market:{marketId}:member:{memberId}:attempt:{attemptNo}` 형식을 유지한다.

예측 참여 포인트 차감 키에는 `optionId`를 포함하지 않는다.
클라이언트는 재시도 여부와 관계없이 attempt suffix가 없는 API 요청 헤더 키를 전달한다.
Market Service는 명확한 실패(`FAILED`) 후 재시도할 때 attemptNo를 증가시키고, Member-Point 차감 요청과 `market_prediction.point_spend_idempotency_key`에는 attempt별 키를 사용한다.
Prediction row와 `referenceId = predictionId`는 재사용한다.
`POINT_PENDING`, `POINT_UNKNOWN`, `CONFIRMED`, `SETTLED`, `REFUND_PENDING`, `REFUND_UNKNOWN`, `REFUNDED` 상태에서는 재시도하지 않고 `MARKET_ALREADY_PREDICTED`를 반환한다.

이유:

```text
같은 사용자가 YES와 NO를 동시에 요청한 경우,
optionId를 키에 포함하면 서로 다른 요청으로 인식되어 포인트가 중복 차감될 수 있다.
```

정산은 Member-Point batch 요청 전체를 추적하기 위한 Header `Idempotency-Key`와 실제 유저별 중복 지급을 방지하는 `items[].idempotencyKey`를 함께 사용한다.

```text
Header Idempotency-Key:
- Member-Point 정산 batch 요청 전체를 추적하기 위한 필수 헤더다.
- 실제 유저별 중복 지급 방지 기준은 아니다.
- 부분 실패 후 실패 item만 재시도할 경우 새 Header Idempotency-Key를 사용할 수 있다.

items[].idempotencyKey:
- 유저 1명, 즉 Prediction 1건의 정산 지급 멱등성 키다.
- 실제 중복 지급 방지는 이 키를 기준으로 한다.
- 같은 Prediction에 대한 재시도에서는 항상 같은 item.idempotencyKey를 사용한다.
```

환불은 Market batch 단위가 아니라 Prediction item 단위로 멱등성을 보장한다.

---

### 1-5. Client Timeout 처리 가이드

예측 참여 API에서 `502`, `503`, `504` 응답을 받은 경우 프론트엔드는 즉시 실패 화면을 표시하지 않는다.

대신 다음 메시지를 표시한다.

```text
예측 참여 처리 상태를 확인 중입니다. 잠시 후 결과가 반영됩니다.
```

이후 3~5초 간격으로 아래 API를 polling한다.

```http
GET /api/v1/markets/{marketId}/predictions/me
```

| PredictionStatus | Client 처리 |
|---|---|
| `CONFIRMED` | 예측 참여 성공 화면 표시 |
| `FAILED` | 예측 참여 실패 메시지 표시 |
| `POINT_PENDING` | 처리 중 표시 후 계속 polling |
| `POINT_UNKNOWN` | 처리 상태 확인 중 표시 후 계속 polling |
| `SETTLED` | 정산 완료 상태 표시 |
| `REFUND_PENDING` | 환불 처리 중 표시 |
| `REFUND_UNKNOWN` | 환불 상태 확인 중 표시 |
| `REFUNDED` | 환불 완료 상태 표시 |

---

### 1-6. 동시성 제어 원칙

Pool-Share 방식의 가격 계산은 선택한 option 하나만 보지 않는다.

```text
선택지 가격 = 해당 선택지 pool / 전체 선택지 pool 합
```

따라서 한 Market에 속한 여러 선택지의 pool 상태가 동시에 변경되면 전체 가격 정합성이 깨질 수 있다.

#### 잘못된 방식

```text
선택한 MarketOption row만 SELECT ... FOR UPDATE
```

이 방식은 다음 상황에서 문제가 된다.

```text
사용자 A → YES option lock
사용자 B → NO option lock

서로 다른 row이므로 동시에 처리 가능
→ 전체 option pool 합을 계산할 때 서로의 변경을 반영하지 못할 수 있음
→ currentPrice 불일치 발생
```

#### 권장 방식

예측 참여 확정 트랜잭션에서는 다음 순서로 락을 획득한다.

```sql
SELECT *
FROM market
WHERE id = :marketId
FOR UPDATE;
```

그 다음 해당 Market의 모든 선택지를 고정 순서로 락 조회한다.

```sql
SELECT *
FROM market_option
WHERE market_id = :marketId
ORDER BY id
FOR UPDATE;
```

처리 기준:

```text
1. 같은 Market 안의 예측 참여 확정은 순차 처리한다.
2. 선택한 option만 락 잡는 방식은 사용하지 않는다.
3. Market row를 먼저 락 잡고, 모든 MarketOption row를 optionId 오름차순으로 락 잡는다.
4. 고정 순서로 락을 잡아 데드락 가능성을 줄인다.
```

---

### 1-7. Transaction 경계 원칙

DB 비관적 락을 잡은 상태로 Member-Point HTTP API를 호출하지 않는다.

#### 금지 흐름

```text
트랜잭션 시작
→ Market row lock 획득
→ Member-Point HTTP 호출
→ 응답 대기
→ 가격 갱신
→ 커밋
```

문제점:

```text
외부 HTTP 응답이 지연되면 DB 커넥션과 락도 함께 오래 점유된다.
트래픽이 몰리면 DB 커넥션 고갈, lock wait 증가, timeout 전파가 발생할 수 있다.
```

#### 권장 흐름

```text
트랜잭션 A:
Prediction POINT_PENDING 생성
커밋

트랜잭션 밖:
Member-Point 포인트 차감 API 호출

트랜잭션 B:
Market row lock 획득
MarketOption 전체 row lock 획득
현재 가격 기준으로 priceSnapshot, contractQuantity 확정
pool 갱신
전체 선택지 가격 재계산
PriceHistory 저장
Prediction CONFIRMED 변경
커밋
```

---


### 1-8. Pool Share 기반 즉시 참여형 예측시장 정책

Market Service는 Polymarket과 같은 주문장 기반 CLOB(Central Limit Order Book) 모델을 사용하지 않는다.

본 MVP는 선택지별 유동성 풀을 기반으로 가격이 자동 조정되는 **Pool Share 기반 즉시 참여형 예측시장** 모델을 사용한다.

```text
optionEffectivePoolAmount = market_option.virtual_pool_amount + market_option.real_pool_amount
totalEffectivePoolAmount = sum(all optionEffectivePoolAmount)
currentPrice = optionEffectivePoolAmount / totalEffectivePoolAmount
```

처리 기준:

```text
1. 사용자는 지정가 주문을 등록하지 않는다.
2. 사용자는 현재 Pool Share 가격 기준으로 즉시 예측 참여한다.
3. 예측 참여가 확정되면 선택한 option의 realPoolAmount가 증가한다.
4. 모든 option의 currentPrice가 전체 effective pool 기준으로 재계산된다.
5. 예측 참여 확정 결과는 market_prediction과 market_price_history에 기록된다.
```

Polymarket식 CLOB과의 차이:

| 구분 | Polymarket식 CLOB | Market MVP |
|---|---|---|
| 가격 형성 | 사용자의 bid/ask 주문과 체결가 | 선택지별 Pool Share 공식 |
| 사용자 행위 | 지정가 주문, 매수/매도, 주문 취소 | 현재 가격 기준 즉시 참여 |
| 핵심 테이블 | order, trade, position | prediction, option, price_history |
| 정산 기준 | position | prediction |
| MVP 선택 이유 | 구현 범위 큼 | 정산 안정성과 구현 가능성 우선 |

현재 MVP에는 `order`, `trade`, `position`, `bid`, `ask`, `order cancel`, `matching engine`, `limit order` 개념을 두지 않는다.

pool 용어는 다음 기준으로 구분한다.

| 용어 | 의미 | 정산/환불 포함 여부 | 가격 계산 포함 여부 |
|---|---|---:|---:|
| `realPoolAmount` | 실제 사용자가 예측 참여에 사용한 포인트 누적합 | O | O |
| `virtualPoolAmount` | Market 생성 시 선택지별로 부여하는 가상 유동성 | X | O |
| `effectivePoolAmount` | `realPoolAmount + virtualPoolAmount` | X | O |
| `totalRealPoolAmount` | 모든 option의 `realPoolAmount` 합 | O | O |
| `totalVirtualPoolAmount` | 모든 option의 `virtualPoolAmount` 합 | X | O |
| `totalEffectivePoolAmount` | `totalRealPoolAmount + totalVirtualPoolAmount` | X | O |

주의:

```text
Pool Share 가격 계산에서 사용하는 totalEffectivePoolAmount와
정산/Insight에서 사용하는 totalPoolAmount는 같은 개념이 아니다.

정산/Insight의 totalPoolAmount는 실제 참여 포인트 총합이다.
가격 계산용 전체 pool은 totalEffectivePoolAmount라고 표현한다.
```

---

### 1-9. 초기 가격과 시장 깊이 정책

모든 Market이 50:50 또는 균등 가격으로 시작하지 않는다.

각 선택지의 초기 가격은 `market_option.virtual_pool_amount` 비율로 결정한다.

```text
initialPrice = option.virtualPoolAmount / sum(all option.virtualPoolAmount)
```

예시:

```text
상승 option virtualPoolAmount = 800
하락/보합 option virtualPoolAmount = 200

상승 초기 가격 = 800 / 1000 = 0.80000000
하락/보합 초기 가격 = 200 / 1000 = 0.20000000
```

`virtualPoolAmount`는 두 가지 역할을 가진다.

```text
1. 초기 사전 확률 반영
2. 초기 시장 깊이 제공
```

정배/역배가 명확한 주제는 `virtualPoolAmount`를 다르게 설정하여 초기 가격에 반영한다.
다만 결과가 이미 사실상 확정된 주제는 예측시장으로서 의미가 낮으므로 관리자 검수 단계에서 개설하지 않는 것을 원칙으로 한다.

`virtualPoolAmount`는 Market 생성 이후 수정하지 않는다.
Market 오픈 이후 가격은 실제 사용자 참여에 따른 `realPoolAmount` 변화로만 재계산한다.

---

### 1-10. Quote와 실제 체결 가격 정책

Quote API는 사용자가 예측 참여 전에 현재 가격, 예상 계약 수량, 참여 후 예상 가격, 가격 영향도를 확인하기 위한 **미리보기 기능**이다.

Quote API는 다음 작업을 수행하지 않는다.

```text
1. Prediction 생성 안 함
2. Member-Point 포인트 차감 안 함
3. 포인트 잔액 조회 안 함
4. market_option pool 변경 안 함
5. price_history 저장 안 함
```

Quote 결과는 확정 체결 결과가 아니다.

실제 예측 참여에서는 포인트 차감 성공 후 가격 확정 트랜잭션에서 Market row와 모든 option row의 lock을 획득한 시점의 최신 Pool 상태를 기준으로 `priceSnapshot`, `contractQuantity`를 확정한다.

```text
Quote 조회 시점 가격 != 실제 예측 참여 확정 시점 가격일 수 있음
```

MVP에서는 빠른 참여 경험을 우선하여 가격 변동 허용 범위(slippage tolerance)를 필수로 두지 않는다.
대신 클라이언트는 다음 안내 문구를 표시한다.

```text
현재 가격은 실시간으로 변동될 수 있으며, 실제 참여 시점의 가격 기준으로 계약 수량이 확정됩니다.
```

추후 필요하면 예측 참여 요청에 `maxAcceptedPrice` 또는 `minContractQuantity`를 선택 필드로 추가할 수 있다.

---

### 1-11. 내부 Scheduler Chunk 처리 원칙

대사, 정산 재시도, 환불 재시도 API는 한 번에 전체 대상을 처리하지 않는다.

모든 내부 Scheduler API는 `limit` 쿼리 파라미터를 받는다.

```text
기본값: limit = 100
```

처리 기준:

```text
1. 한 번의 Scheduler 실행에서 최대 limit건만 처리한다.
2. 각 chunk는 짧은 트랜잭션으로 처리한다.
3. 실패한 건은 다음 Scheduler 주기에 다시 처리한다.
4. 장시간 트랜잭션과 DB 커넥션 고갈을 방지한다.
```

---

## 2. Market 목록 조회

```http
GET /api/v1/markets?page=0&size=20&status=ACTIVE
```

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `page` | int | X | 페이지 번호. 기본값 0 |
| `size` | int | X | 페이지 크기. 기본값 20 |
| `status` | string | X | Market 상태 필터 |
| `keyword` | string | X | 제목 검색 |

### Response

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "content": [
      {
        "marketId": 1,
        "title": "이번 주 OO구 아파트 가격 변동률은?",
        "status": "ACTIVE",
        "closeAt": "2026-06-01T18:00:00",
        "totalRealPoolAmount": "25000.00",
        "totalVirtualPoolAmount": "10000.00",
        "totalEffectivePoolAmount": "35000.00",
        "options": [
          {
            "optionId": 1,
            "content": "0.0% 미만",
            "currentPrice": "0.31250000"
          },
          {
            "optionId": 2,
            "content": "0.0% 이상 ~ 0.3% 미만",
            "currentPrice": "0.68750000"
          }
        ]
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "timestamp": "2026-05-29T15:30:00"
}
```

---


## 3. Market 상세 조회

```http
GET /api/v1/markets/{marketId}
```

Market 상세 조회는 사용자가 예측 참여 전 현재 가격과 시장 흐름을 판단할 수 있도록 선택지별 가격 지표를 함께 제공한다.

`initialPrice`, `priceChangeRate`는 별도 컬럼이 아니라 기존 `virtualPoolAmount`, `currentPrice`를 이용해 계산한 응답 필드다.

```text
initialPrice = option.virtualPoolAmount / sum(all option.virtualPoolAmount)
priceChangeRate = (currentPrice - initialPrice) / initialPrice * 100
```

### Response

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "marketId": 1,
    "title": "이번 주 OO구 아파트 가격 변동률은?",
    "description": "한국부동산원 데이터를 기준으로 정산합니다.",
    "status": "ACTIVE",
    "priceModel": "POOL_SHARE",
    "closeAt": "2026-06-01T18:00:00",
    "judgeDate": "2026-06-04",
    "settleDueAt": "2026-06-04T18:00:00",
    "totalRealPoolAmount": "25000.00",
    "totalVirtualPoolAmount": "10000.00",
    "totalEffectivePoolAmount": "35000.00",
    "totalPredictionCount": 184,
    "options": [
      {
        "optionId": 1,
        "content": "0.0% 미만",
        "initialPrice": "0.50000000",
        "currentPrice": "0.31250000",
        "priceChangeRate": "-37.50000000",
        "realPoolAmount": "10000.00",
        "virtualPoolAmount": "5000.00",
        "effectivePoolAmount": "15000.00",
        "totalContractQuantity": "15272.72727272",
        "predictionCount": 64
      },
      {
        "optionId": 2,
        "content": "0.0% 이상 ~ 0.3% 미만",
        "initialPrice": "0.50000000",
        "currentPrice": "0.68750000",
        "priceChangeRate": "37.50000000",
        "realPoolAmount": "15000.00",
        "virtualPoolAmount": "5000.00",
        "effectivePoolAmount": "20000.00",
        "totalContractQuantity": "21818.18181818",
        "predictionCount": 120
      }
    ]
  },
  "timestamp": "2026-05-29T15:30:00"
}
```

pool 관련 조회 필드는 다음 의미로 사용한다.

| 필드 | 설명 |
|---|---|
| `totalRealPoolAmount` | 실제 유저 참여 포인트 총합. `market.total_pool`과 같은 의미 |
| `totalVirtualPoolAmount` | 선택지별 가상 유동성 총합. 정산/환불 금액에는 포함하지 않음 |
| `totalEffectivePoolAmount` | 가격 계산용 전체 유효 풀. `totalRealPoolAmount + totalVirtualPoolAmount` |
| `realPoolAmount` | 선택지별 실제 참여 포인트 누적합 |
| `virtualPoolAmount` | 선택지별 가상 유동성 |
| `effectivePoolAmount` | 선택지별 가격 계산용 유효 풀. `realPoolAmount + virtualPoolAmount` |
| `judgeDate` | 결과 판정 기준일 |
| `settleDueAt` | 정산 예정 또는 정산 마감 기준 시각 |
| `resultAnnounceAt` | 신규 응답 예시에서는 사용하지 않음. 필요한 경우 `settleDueAt` 기반 alias로만 취급 |

### 발생 가능한 ErrorCode

| ErrorCode | HTTP Status | 설명 |
|---|---:|---|
| `MARKET_NOT_FOUND` | 404 | Market 없음 |

---


## 4. 가격 이력 조회

가격 이력은 예측 참여, 풀 금액 변경, 가격 재계산 시 계속 누적되므로 반드시 페이징한다.

이 API는 프론트엔드의 배당률/가격 변화 그래프를 위한 조회 API다.
각 이력은 특정 예측 참여 전후의 가격과 pool 변화를 포함한다.

```http
GET /api/v1/markets/{marketId}/price-history?page=0&size=50&optionId=1
```

PriceHistory는 특정 Market의 선택지별 가격 변화 이력을 저장한다.
프론트엔드는 `priceAfter`를 시간순으로 연결하여 option별 가격 그래프를 그린다.
`priceBefore`, `priceAfter`, `priceChangeRate`, `realPoolBefore`, `realPoolAfter`, `contractQuantityBefore`, `contractQuantityAfter`는 사용자가 가격 변화 원인을 이해할 수 있도록 제공한다.

PriceHistory는 정산/환불 금액 계산의 원천 데이터가 아니다.
정산은 `market_prediction`, `market_settlement`, `market_settlement_detail`을 기준으로 하고, 환불은 `market_prediction.point_amount`, `market_refund_detail`을 기준으로 한다.

저장 단위:

```text
market_price_history row 1건 = 특정 Market의 특정 option에 대한 특정 가격 변경 이벤트 1건
```

Prediction CONFIRMED 시 해당 Market의 모든 option에 대해 `market_price_history` row를 생성한다.
Pool Share에서는 선택한 option의 참여만으로도 `totalEffectivePoolAmount`가 변하므로 선택되지 않은 option의 `currentPrice`도 함께 변할 수 있다.

```text
Market option 3개
사용자가 option B에 예측 참여
→ option B realPoolAmount 증가
→ totalEffectivePoolAmount 증가
→ A, B, C 모든 option currentPrice 재계산
→ market_price_history 3건 생성
```

MVP v4 eventType:

```text
PREDICTION_CONFIRMED
```

MVP v4에서는 `QUOTE_VIEWED`, `MARKET_CREATED`, `MARKET_ACTIVATED`, `RESULT_CONFIRMED`, `SETTLEMENT_STARTED`, `SETTLEMENT_COMPLETED`, `MARKET_VOIDED`, `REFUND_STARTED`, `REFUND_COMPLETED` 이벤트를 저장하지 않는다.
Quote, Market 생성/활성화, 결과 확정, 정산, 환불, 무효 처리는 PriceHistory를 생성하지 않는다.

### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `page` | int | X | 페이지 번호. 기본값 0 |
| `size` | int | X | 페이지 크기. 기본값 50 |
| `optionId` | long | X | 특정 선택지 이력만 조회 |

조회 조건:

```text
Market이 존재해야 한다.
optionId가 주어진 경우 해당 option은 marketId에 속해야 한다.
```

정렬:

```text
createdAt ASC, historyId ASC
```

가격 그래프는 시간순 연결이 필요하므로 기본 조회 순서는 오래된 이력부터 최신 이력 순서로 한다.

응답은 flat list를 유지한다.
`optionId` 파라미터가 없으면 모든 option history row를 시간순으로 반환하고, 프론트엔드는 `optionId` 기준으로 grouping해서 option별 라인을 그린다.
`optionId` 파라미터가 있으면 해당 option의 history만 반환한다.

### Response

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "content": [
      {
        "historyId": 1,
        "marketId": 1,
        "optionId": 1,
        "optionContent": "0.0% 미만",
        "predictionId": 1001,
        "eventType": "PREDICTION_CONFIRMED",
        "priceBefore": "0.50000000",
        "priceAfter": "0.31250000",
        "priceChangeRate": "-37.50000000",
        "realPoolBefore": "0.00",
        "realPoolAfter": "10000.00",
        "virtualPoolAmount": "5000.00",
        "contractQuantityBefore": "0.00000000",
        "contractQuantityAfter": "15272.72727272",
        "createdAt": "2026-05-29T15:30:00"
      }
    ],
    "page": 0,
    "size": 50,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "timestamp": "2026-05-29T15:30:00"
}
```

### 필드 설명

| 필드 | 설명 |
|---|---|
| `historyId` | 가격 이력 ID |
| `marketId` | Market ID |
| `optionId` | 선택지 ID |
| `optionContent` | 선택지 표시명 |
| `predictionId` | 가격 변경을 유발한 Prediction ID |
| `eventType` | 가격 변경 이벤트 타입. MVP v4에서는 `PREDICTION_CONFIRMED` |
| `priceBefore` | 이벤트 전 선택지 가격 |
| `priceAfter` | 이벤트 후 선택지 가격 |
| `priceChangeRate` | `(priceAfter - priceBefore) / priceBefore * 100` |
| `realPoolBefore` | 이벤트 전 선택지 실제 pool |
| `realPoolAfter` | 이벤트 후 선택지 실제 pool |
| `virtualPoolAmount` | 선택지 가상 pool. `market_option`에서 조회 |
| `contractQuantityBefore` | 이벤트 전 option 누적 계약 수량 |
| `contractQuantityAfter` | 이벤트 후 option 누적 계약 수량 |
| `createdAt` | 이력 생성 시각 |

`contractQuantityBefore`, `contractQuantityAfter`는 사용자 1명의 계약 수량이 아니라 option의 누적 `totalContractQuantity` snapshot이다.
한 사용자의 체결 계약 수량은 `market_prediction.contract_quantity`에 저장한다.

`priceChangeRate` 계산 시 `priceBefore <= 0`이면 데이터 정합성 오류이므로 0으로 나누지 않는다.
정상 생성된 Market에서는 `priceBefore > 0`이어야 한다.

Decimal scale:

| 필드 | Scale |
|---|---:|
| `priceBefore` | 8 |
| `priceAfter` | 8 |
| `priceChangeRate` | 8 |
| `realPoolBefore` | 2 |
| `realPoolAfter` | 2 |
| `virtualPoolAmount` | 2 |
| `contractQuantityBefore` | 8 |
| `contractQuantityAfter` | 8 |

### 그래프 표시 기준

프론트엔드는 `priceAfter`를 시간순으로 연결하여 선택지별 가격 변화 그래프를 그린다.
`priceChangeRate`는 해당 이력 한 건의 직전 가격 대비 변화율이다.
초기 가격 대비 변화율이 필요한 경우 Market 상세 조회의 `initialPrice`, `currentPrice`, `priceChangeRate`를 사용한다.

초기 가격 처리 정책:

```text
Market 생성/활성화 시 market_price_history row를 생성하지 않는다.
초기 가격은 Market 상세 조회의 initialPrice로 제공한다.
가격 이력 조회 API는 Prediction CONFIRMED 이후의 가격 변화 이벤트를 반환한다.
```

프론트엔드가 그래프 시작점을 초기 가격부터 보여주고 싶다면, Market 상세 조회의 `initialPrice`를 시작점으로 사용하고 이후 PriceHistory의 `priceAfter`를 시간순으로 연결한다.

### 발생 가능한 ErrorCode

| ErrorCode | HTTP Status | 설명 |
|---|---:|---|
| `MARKET_NOT_FOUND` | 404 | Market 없음 |
| `MARKET_OPTION_NOT_FOUND` | 404 | option 없음 또는 해당 Market 소속 아님 |

---


## 4-1. 예측 참여 Quote 조회

```http
POST /api/v1/markets/{marketId}/predictions/quote
```

Quote API는 사용자가 예측 참여 전에 현재 가격 기준 예상 결과를 확인하기 위한 미리보기 API다.

이 API는 Market 내부 데이터만 사용한다.
Member-Point Service를 호출하지 않으며, 포인트 차감/잔액 조회/Prediction 생성/가격 이력 저장을 수행하지 않는다.
또한 `market_option` pool과 `market_prediction` 상태를 변경하지 않는다.

Quote API 구현은 PriceHistory v4 schema/migration 작업과 독립적으로 진행할 수 있다.
Quote API는 `market`, `market_option` 조회 기반 계산 API이며, `market_price_history`를 읽거나 쓰지 않는다.
따라서 PriceHistory v4 저장 정책이 확정되지 않아도 Quote API는 구현 가능하다.

Quote 결과는 확정 견적이 아니다.
실제 예측 참여 API는 포인트 차감 성공 후 가격 확정 트랜잭션에서 Market row와 모든 option row lock을 획득한 시점의 최신 Pool 상태를 기준으로 `priceSnapshot`, `contractQuantity`를 확정한다.

### Request

```json
{
  "marketOptionId": 2,
  "pointAmount": "100.00"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `marketOptionId` | long | O | 예측하려는 선택지 ID |
| `pointAmount` | String decimal | O | 예측 참여에 사용할 포인트 금액 |

`pointAmount`는 Decimal 요청/응답 정책과 동일하게 문자열로 표현한다.

### 성공 조건

Quote API는 아래 조건을 모두 만족해야 성공한다.

```text
1. Market이 존재해야 한다.
2. Market.status = ACTIVE 여야 한다.
3. Market.closeAt > now 여야 한다.
4. marketOptionId가 존재해야 한다.
5. marketOptionId는 해당 marketId에 속해야 한다.
6. pointAmount가 유효해야 한다.
7. 선택지 currentPrice가 null이 아니고 0보다 커야 한다.
8. option realPoolAmount, virtualPoolAmount가 null이 아니어야 한다.
9. totalEffectivePoolAmount가 0보다 커야 한다.
```

상태 정책:

```text
Market.status != ACTIVE
→ MARKET_NOT_ACTIVE

Market.status = ACTIVE 이지만 closeAt <= now
→ MARKET_CLOSED
```

`pointAmount` 검증은 실제 예측 참여 API와 동일한 금액 검증 정책을 따른다.
Quote에서 허용된 금액이 실제 예측 참여에서 거부되거나, 실제 예측 참여에서 허용되는 금액이 Quote에서 거부되지 않도록 두 API의 검증 정책을 동기화한다.

```text
1. pointAmount > 0
2. 최소 예측 참여 금액 이상
3. 최대 예측 참여 금액 이하
4. 허용 소수점 자리수 준수
```

현재 정책:

```text
최소 10P
최대 500P
소수점 둘째 자리까지 허용
```

검증 실패 시 `MARKET_INVALID_BET_AMOUNT`를 반환한다.

구현 시 기존 예측 참여 API의 금액 검증 상수 또는 검증 로직이 있다면 Quote API도 이를 재사용한다.
문서와 코드가 다르면 기존 예측 참여 API 정책을 기준으로 문서를 다시 확인한다.

### 계산 기준

```text
currentPrice = 조회 시점 selected option의 currentPrice
estimatedContractQuantity = pointAmount / currentPrice

selectedOptionEffectivePoolBefore =
    selectedOption.realPoolAmount + selectedOption.virtualPoolAmount

totalEffectivePoolBefore =
    sum(all option.realPoolAmount + option.virtualPoolAmount)

selectedOptionEffectivePoolAfter = selectedOptionEffectivePoolBefore + pointAmount
totalEffectivePoolAfter = totalEffectivePoolBefore + pointAmount
estimatedAfterPrice = selectedOptionEffectivePoolAfter / totalEffectivePoolAfter

priceImpactRate = (estimatedAfterPrice - currentPrice) / currentPrice * 100
```

`selectedOptionEffectivePoolBefore`는 `virtualPoolAmount + realPoolAmount` 기준이다.
선택하지 않은 option의 `effectivePoolAmount`는 그대로 유지된다고 가정한다.
`currentPrice`는 Quote 조회 시점의 `market_option.current_price`를 사용한다.

방어 정책:

```text
currentPrice == null 또는 currentPrice <= 0
→ MARKET_INVALID_OPTION

option.realPoolAmount == null 또는 option.virtualPoolAmount == null
→ MARKET_INVALID_OPTION

totalEffectivePoolAmount <= 0
→ MARKET_INVALID_OPTION
```

정상 생성된 Market에서는 발생하면 안 되는 데이터 정합성 오류지만, 0 나눗셈 방지를 위해 문서상 방어 정책을 둔다.

구현 주의:

```text
Quote API는 currentPrice null/0 이하, option pool amount null, totalEffectivePoolAmount 0 이하인 데이터 정합성 오류 상황에서 MARKET_INVALID_OPTION을 사용한다.
만약 코드 enum에 MARKET_INVALID_OPTION이 없다면, 이는 새 정책 추가가 아니라 문서에 이미 정의된 ErrorCode의 구현 누락 보정으로 보고 동기화한다.
새로운 ErrorCode를 임의로 만들지 않는다.
```

### Decimal scale / RoundingMode

Quote API 계산 결과 scale은 아래 기준을 따른다.

| 필드 | Scale | 설명 |
|---|---:|---|
| `pointAmount` | 2 | 포인트 금액 |
| `currentPrice` | 8 | 가격 |
| `estimatedContractQuantity` | 8 | 예상 계약 수량 |
| `estimatedAfterPrice` | 8 | 예상 가격 |
| `priceImpactRate` | 8 | 가격 영향도 |
| `selectedOptionEffectivePoolBefore` | 2 | Pool 금액 |
| `selectedOptionEffectivePoolAfter` | 2 | Pool 금액 |
| `totalEffectivePoolBefore` | 2 | Pool 금액 |
| `totalEffectivePoolAfter` | 2 | Pool 금액 |

RoundingMode:

```text
나눗셈 계산은 RoundingMode.HALF_UP을 사용한다.
정산/환불 금액 지급처럼 실제 포인트 지급 금액을 계산하는 경우에는 기존 정산 정책대로 RoundingMode.DOWN을 유지한다.
```

Quote는 사용자 화면 표시용 예상값이다.
정산 지급 금액처럼 실제 지급 포인트를 확정하는 계산이 아니므로 HALF_UP을 사용한다.

### Response

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "marketId": 1,
    "selectedOptionId": 2,
    "pointAmount": "100.00",
    "currentPrice": "0.20000000",
    "estimatedContractQuantity": "500.00000000",
    "estimatedAfterPrice": "0.27272727",
    "priceImpactRate": "36.36363500",
    "selectedOptionEffectivePoolBefore": "200.00",
    "selectedOptionEffectivePoolAfter": "300.00",
    "totalEffectivePoolBefore": "1000.00",
    "totalEffectivePoolAfter": "1100.00",
    "notice": "현재 가격은 실시간으로 변동될 수 있으며, 실제 참여 시점의 가격 기준으로 계약 수량이 확정됩니다."
  },
  "timestamp": "2026-05-29T15:30:00"
}
```

| 필드 | 설명 |
|---|---|
| `marketId` | Market ID |
| `selectedOptionId` | 선택한 option ID |
| `pointAmount` | 요청 포인트 금액 |
| `currentPrice` | Quote 조회 시점의 현재 선택지 가격 |
| `estimatedContractQuantity` | 현재 가격 기준 예상 계약 수량 |
| `estimatedAfterPrice` | 해당 포인트가 반영된다고 가정했을 때의 예상 선택지 가격 |
| `priceImpactRate` | 현재 가격 대비 예상 가격 변화율 |
| `selectedOptionEffectivePoolBefore` | 선택 option의 참여 전 effective pool |
| `selectedOptionEffectivePoolAfter` | 선택 option의 참여 후 예상 effective pool |
| `totalEffectivePoolBefore` | 참여 전 전체 effective pool |
| `totalEffectivePoolAfter` | 참여 후 예상 전체 effective pool |
| `notice` | 실제 참여 시점 가격 변동 가능성 안내 |

Quote 응답에는 `totalPoolAmount`라는 모호한 필드를 사용하지 않는다.
가격 계산용 전체 pool은 `totalEffectivePoolBefore`, `totalEffectivePoolAfter`로 표현한다.

### DB lock 정책

Quote API는 상태 변경이 없는 미리보기 API다.

```text
Quote API는 SELECT ... FOR UPDATE를 사용하지 않는다.
Quote API는 일반 SELECT로 현재 Market과 option 상태를 조회한다.
Quote 결과는 확정 체결 결과가 아니므로, 조회 중 다른 사용자의 참여로 가격이 바뀔 수 있다.
실제 예측 참여 API에서만 Market row와 모든 option row를 FOR UPDATE로 잠근다.
```

### 주의 사항

Quote 결과는 확정 체결 결과가 아니다.
실제 예측 참여 API는 포인트 차감 성공 후 가격 확정 트랜잭션에서 lock을 획득한 시점의 최신 가격 기준으로 `priceSnapshot`, `contractQuantity`를 확정한다.

MVP에서는 별도의 slippage tolerance를 필수로 받지 않는다.
빠른 참여 경험을 우선하며, 클라이언트 안내 문구로 가격 변동 가능성을 고지한다.

### 발생 가능한 ErrorCode

| ErrorCode | HTTP Status | 설명 |
|---|---:|---|
| `MARKET_NOT_FOUND` | 404 | Market 없음 |
| `MARKET_NOT_ACTIVE` | 409 | Quote 조회 가능한 상태가 아님 |
| `MARKET_CLOSED` | 409 | 마감된 Market |
| `MARKET_OPTION_NOT_FOUND` | 404 | 선택지 없음 |
| `MARKET_INVALID_BET_AMOUNT` | 400 | 예측 금액 오류 |
| `MARKET_INVALID_OPTION` | 400 | currentPrice 또는 pool amount가 null이거나 currentPrice/totalEffectivePoolAmount가 0 이하 |

---

## 5. 예측 참여

```http
POST /api/v1/markets/{marketId}/predictions
```

### Headers

| 이름 | 필수 | 설명 |
|---|---:|---|
| `X-Member-Id` | O | 임시 회원 식별자. 추후 Gateway/Auth 연동 시 인증 정보에서 주입 |
| `Idempotency-Key` | O | 예측 참여 포인트 차감 멱등성 키 |
| `Authorization` | 추후 | JWT Access Token. 현재 1차 구현에서는 사용하지 않음 |

`X-Member-Id`는 MVP 단계의 임시 인증 정책이다.

Idempotency-Key 예시:

```text
MARKET_PREDICTION_SPEND:market:1:member:10
```

검증 규칙:

- key의 market id는 path variable `marketId`와 같아야 한다.
- key의 member id는 `X-Member-Id`와 같아야 한다.
- Gateway/Auth 연동 시 `X-Member-Id`는 JWT subject에서 주입되므로, 클라이언트는 다른 member id를 key에 사용할 수 없다.
- `:attempt:{attemptNo}`가 포함된 key는 공개 예측 참여 API 요청 헤더로 사용할 수 없다.

요청 예시:

```http
POST /api/v1/markets/1/predictions
X-Member-Id: 10
Idempotency-Key: MARKET_PREDICTION_SPEND:market:1:member:10
Content-Type: application/json
```

### Request

```json
{
  "marketOptionId": 2,
  "pointAmount": "100.00"
}
```

---

### 처리 흐름

예측 참여는 외부 HTTP 호출과 DB 가격 확정 트랜잭션을 분리한다.

> 현재 2차 구현에서도 Member-Point 실제 HTTP 연동은 하지 않는다.
> `MemberPointClient` 내부 예외 모델을 통해 명확한 실패는 `FAILED`, 타임아웃·외부 오류·응답 불명확은 `POINT_UNKNOWN`으로 처리한다.
> 추후 실제 Member-Point 연동 시에도 동일한 transaction boundary를 유지한다.

```text
[트랜잭션 A]
1. Market 조회
2. Market ACTIVE 상태 검증
3. 선택지 검증
4. `(market_id, member_id)` 기준 기존 Prediction row 락 조회
5. 기존 Prediction이 없으면 POINT_PENDING Prediction 저장
   - attemptNo = 1
   - pointAmount 저장
   - selectedOptionId 저장
   - memberId 저장
   - priceSnapshot, contractQuantity는 아직 NULL 가능
6. 기존 Prediction이 FAILED이면 같은 row를 POINT_PENDING으로 변경
   - 요청의 pointAmount, selectedOptionId로 갱신
   - priceSnapshot, contractQuantity, 예상값 snapshot, failReason 초기화
   - attemptNo 증가
7. 기존 Prediction이 FAILED 이외 상태이면 MARKET_ALREADY_PREDICTED
8. 트랜잭션 A 커밋

[트랜잭션 밖]
9. Member-Point 포인트 차감 API 호출
   - type = SPEND_MARKET
   - referenceType = MARKET_PREDICTION
   - referenceId = predictionId
   - idempotencyKey = MARKET_PREDICTION_SPEND:market:{marketId}:member:{memberId}:attempt:{attemptNo}

[트랜잭션 B]
10. 포인트 차감 성공 시 새 트랜잭션 시작
11. Market row 비관적 락 획득
12. 해당 Market의 모든 MarketOption row를 optionId 오름차순으로 비관적 락 획득
13. 현재 선택지 가격 기준으로 priceSnapshot 확정
14. contractQuantity 계산
15. 선택한 option의 realPoolAmount, totalContractQuantity 증가
16. 전체 선택지 currentPrice 재계산
17. PriceHistory 저장
18. Prediction CONFIRMED 변경
19. 트랜잭션 B 커밋
20. 응답 반환
```

---

### Member-Point 포인트 차감 요청 예시

```http
POST /api/v1/points/spend
Idempotency-Key: MARKET_PREDICTION_SPEND:market:1:member:10:attempt:1
```

```json
{
  "memberId": 10,
  "type": "SPEND_MARKET",
  "amount": "100.00",
  "referenceType": "MARKET_PREDICTION",
  "referenceId": 100,
  "reason": "Market 예측 참여"
}
```

---

### 가격 확정 트랜잭션 Lock 정책

트랜잭션 B에서는 선택한 option만 락 잡지 않는다.

```sql
SELECT *
FROM market
WHERE id = :marketId
FOR UPDATE;
```

```sql
SELECT *
FROM market_option
WHERE market_id = :marketId
ORDER BY id
FOR UPDATE;
```

이유:

```text
Pool-Share 가격 계산은 전체 선택지 pool 합에 의존한다.
따라서 YES/NO 또는 다중 선택지에서 서로 다른 option에 동시 참여하더라도
한 Market 안의 가격 확정은 순차 처리되어야 한다.
```

---

### 가격 확정 기준

`priceSnapshot`과 `contractQuantity`는 사용자가 버튼을 누른 순간이 아니라, **포인트 차감 성공 후 가격 확정 트랜잭션에서 Market lock을 획득한 시점**의 `currentPrice`를 기준으로 확정한다.

```text
priceSnapshot = lock 획득 후 selected option의 currentPrice
contractQuantity = pointAmount / priceSnapshot
```

MVP에서는 가격 변동 허용 범위(slippage tolerance) 기능을 필수로 두지 않는다.
Quote API를 통해 확인한 예상 가격과 실제 예측 참여 확정 가격은 다를 수 있다.

클라이언트는 예측 참여 화면에 다음 안내를 표시한다.

```text
현재 가격은 실시간으로 변동될 수 있으며, 실제 참여 시점의 가격 기준으로 계약 수량이 확정됩니다.
```

추후 필요하면 요청값에 `maxAcceptedPrice` 또는 `minContractQuantity`를 선택 필드로 추가할 수 있다.

---

### 성공 Response

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "predictionId": 100,
    "marketId": 1,
    "selectedOptionId": 2,
    "pointAmount": "100.00",
    "priceSnapshot": "0.68750000",
    "contractQuantity": "145.45454545",
    "status": "CONFIRMED"
  },
  "timestamp": "2026-05-29T15:30:00"
}
```

---

### 처리 불명확 Response

포인트 차감 타임아웃, 5xx, 응답 불명확 상황에서는 `POINT_UNKNOWN` 상태로 남긴다.

```json
{
  "success": true,
  "errorCode": null,
  "message": "예측 참여 처리 상태를 확인 중입니다.",
  "data": {
    "predictionId": 100,
    "marketId": 1,
    "selectedOptionId": 2,
    "pointAmount": "100.00",
    "status": "POINT_UNKNOWN"
  },
  "timestamp": "2026-05-29T15:30:00"
}
```

포인트 차감은 성공했지만 트랜잭션 B에서 가격 확정/CONFIRMED 변경에 실패한 경우, `POINT_PENDING` 또는 `POINT_UNKNOWN` 상태로 남을 수 있다.  
Scheduler는 해당 Prediction을 대사 대상으로 조회하고, Member-Point 거래 상태 조회 API로 차감 성공 여부를 확인한 뒤 가격 확정 트랜잭션을 재시도한다.

---

### Client 처리 가이드

예측 참여 API에서 `502`, `503`, `504` 응답을 받거나 응답 본문의 상태가 `POINT_UNKNOWN`인 경우 프론트엔드는 실패 화면을 표시하지 않는다.

대신 다음 API를 3~5초 간격으로 polling한다.

```http
GET /api/v1/markets/{marketId}/predictions/me
```

---

### 발생 가능한 ErrorCode

| ErrorCode | HTTP Status | 설명 |
|---|---:|---|
| `MARKET_NOT_FOUND` | 404 | Market 없음 |
| `MARKET_NOT_ACTIVE` | 409 | 참여 가능한 상태가 아님 |
| `MARKET_CLOSED` | 409 | 마감된 Market |
| `MARKET_ALREADY_PREDICTED` | 409 | 이미 예측 참여함 |
| `MARKET_OPTION_NOT_FOUND` | 404 | 선택지 없음 |
| `MARKET_INVALID_BET_AMOUNT` | 400 | 예측 금액 오류 |
| `POINT_INSUFFICIENT` | 409 | 포인트 부족 |
| `MEMBER_ALREADY_DELETED` | 409 | 탈퇴 회원의 일반 사용자 요청 |
| `IDEMPOTENCY_KEY_REQUIRED` | 400 | 멱등성 키 누락 |
| `IDEMPOTENCY_KEY_CONFLICT` | 409 | 멱등성 키 충돌 |
| `EXTERNAL_SERVICE_TIMEOUT` | 504 | 포인트 차감 요청 타임아웃 |
| `EXTERNAL_SERVICE_UNAVAILABLE` | 503 | 포인트 서비스 연결 실패 |
| `EXTERNAL_SERVICE_ERROR` | 502 | 포인트 서비스 5xx |
| `MARKET_PRICE_UPDATE_CONFLICT` | 409 | 가격 갱신 동시성 충돌 |

---

## 6. 내 예측 상태 조회

```http
GET /api/v1/markets/{marketId}/predictions/me
```

`POINT_UNKNOWN`, `POINT_PENDING` 상태를 클라이언트가 확인하기 위한 API이다.

### Headers

| 이름 | 필수 | 설명 |
|---|---:|---|
| `X-Member-Id` | O | 임시 회원 식별자. 추후 Gateway/Auth 연동 시 인증 정보에서 주입 |
| `Idempotency-Key` | X | 읽기 API이므로 사용하지 않음 |

`X-Member-Id`는 MVP 단계의 임시 인증 정책이다.

### Response

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "predictionId": 100,
    "marketId": 1,
    "selectedOptionId": 2,
    "pointAmount": "100.00",
    "priceSnapshot": "0.68750000",
    "contractQuantity": "145.45454545",
    "status": "POINT_UNKNOWN",
    "createdAt": "2026-05-29T15:30:00",
    "updatedAt": "2026-05-29T15:31:00"
  },
  "timestamp": "2026-05-29T15:31:00"
}
```

`POINT_PENDING`, `POINT_UNKNOWN` 상태에서는 `priceSnapshot`, `contractQuantity`가 아직 `null`일 수 있다.

### 발생 가능한 ErrorCode

| ErrorCode | HTTP Status | 설명 |
|---|---:|---|
| `MARKET_NOT_FOUND` | 404 | Market 없음 |
| `MARKET_PREDICTION_NOT_FOUND` | 404 | 내 예측 없음 |

---

## 6-3. 관리자 API 권한 정책

관리자 API는 Gateway가 JWT를 검증한 뒤 주입한 `X-Member-Role` 헤더를 사용한다.
Market Service는 JWT를 직접 파싱하지 않고, JWT_SECRET을 사용하지 않는다.

### Headers

| 이름 | 필수 | 설명 |
|---|---:|---|
| `X-Member-Role` | O | Gateway가 주입한 회원 역할. 관리자 API는 `ADMIN`만 허용 |

`X-Member-Role`이 없거나 `ADMIN`이 아니면 `403 FORBIDDEN`을 반환한다.

---

## 7. 관리자 Market 생성

```http
POST /api/v1/admin/markets
```

### Request

```json
{
  "title": "이번 주 OO구 아파트 가격 변동률은?",
  "description": "한국부동산원 데이터를 기준으로 정산합니다.",
  "category": "PRICE_INDEX",
  "answerType": "NUMERIC_RANGE",
  "metricUnit": "PERCENT",
  "judgeDataSource": "한국부동산원",
  "judgeCriteria": "지정된 판정일의 변동률 기준",
  "judgeDate": "2026-06-30",
  "closeAt": "2026-06-20T23:59:59",
  "settleDueAt": "2026-07-01T23:59:59",
  "feeRate": "5.00",
  "createdBy": 1,
  "options": [
    {
      "optionCode": "A",
      "optionText": "0.0% 이상 ~ 0.3% 미만",
      "displayOrder": 1,
      "rangeMin": "0.0000",
      "rangeMax": "0.3000",
      "minInclusive": true,
      "maxInclusive": false,
      "virtualPoolAmount": "100.00"
    },
    {
      "optionCode": "B",
      "optionText": "0.3% 이상 ~ 0.6% 미만",
      "displayOrder": 2,
      "rangeMin": "0.3000",
      "rangeMax": "0.6000",
      "minInclusive": true,
      "maxInclusive": false,
      "virtualPoolAmount": "100.00"
    }
  ]
}
```

### 검증 규칙

```text
1. 선택지는 최소 2개 이상이어야 한다.
2. 선택지 범위가 서로 겹치면 안 된다.
3. 선택지 범위 사이에 빈 구간이 있으면 안 된다.
4. 경계값 포함 여부가 명확해야 한다.
5. 선택지 범위가 커버하는 값에 대해서는 실제 정산 값이 정확히 하나의 선택지에만 매칭되어야 한다.
   단, 유한 구간만으로 구성된 Market에서 실제 정산 값이 모든 선택지 범위 밖에 있으면 결과 확정 시 `MARKET_WINNING_OPTION_NOT_FOUND`로 처리한다.
6. Market 생성 시 초기 가격은 선택지별 virtualPoolAmount 비율로 계산한다.
7. virtualPoolAmount는 Market 생성 후 변경하지 않는다.
```

### 발생 가능한 ErrorCode

| ErrorCode | HTTP Status | 설명 |
|---|---:|---|
| `FORBIDDEN` | 403 | 관리자 권한 없음 |
| `VALIDATION_FAILED` | 400 | 요청 값 검증 실패 |
| `MARKET_INVALID_OPTION` | 400 | 선택지 구성 오류 |
| `MARKET_INVALID_OPTION_RANGE` | 400 | 선택지 범위 오류 |
| `MARKET_INVALID_FEE_RATE` | 400 | 수수료율 범위 오류 |

#### NUMERIC_RANGE 열린 구간 정책

```text
rangeMin = null은 -무한대에서 시작하는 구간을 의미한다.
rangeMax = null은 +무한대까지 이어지는 구간을 의미한다.
rangeMin과 rangeMax가 모두 null인 구간은 허용하지 않는다.
rangeMin = null인 구간과 rangeMax = null인 구간은 각각 최대 1개만 허용한다.
rangeMin과 rangeMax에는 음수 값을 사용할 수 있다.
정렬된 인접 구간 사이에는 빈 구간이나 겹침이 없어야 한다.
인접 경계값은 정확히 하나의 구간에만 포함되어야 한다.
첫 구간이 -무한대에서 시작하거나 마지막 구간이 +무한대까지 이어져야 하는 것은 아니다.
```

### 7-1. 관리자 Market 활성화

```http
PATCH /api/v1/admin/markets/{marketId}/activate
```

생성 직후 `PENDING` 상태인 Market을 예측 참여 가능한 `ACTIVE` 상태로 전환한다.

허용 상태 전이:

```text
PENDING → ACTIVE
```

활성화 검증:

```text
1. Market이 존재해야 한다.
2. 현재 상태가 PENDING이어야 한다.
3. closeAt이 현재 시각보다 미래여야 한다.
4. 선택지가 최소 2개 이상 존재해야 한다.
5. initialVirtualLiquidity가 0보다 커야 한다.
6. 활성화 API는 Member-Point Service를 호출하지 않는다.
7. 활성화 API는 가격을 재계산하지 않고 기존 MarketOption currentPrice를 유지한다.
```

### Response

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "marketId": 1,
    "status": "ACTIVE"
  },
  "timestamp": "2026-06-02T15:30:00"
}
```

### 발생 가능한 ErrorCode

| ErrorCode | HTTP Status | 설명 |
|---|---:|---|
| `MARKET_NOT_FOUND` | 404 | Market 없음 |
| `MARKET_INVALID_STATUS` | 409 | PENDING 상태가 아님 |
| `MARKET_CLOSED` | 409 | 활성화 전에 마감 시각이 지남 |
| `MARKET_INVALID_OPTION` | 400 | 선택지 수 또는 초기 가상 유동성이 유효하지 않음 |

---

## 8. 관리자 결과 확정

```http
PATCH /api/v1/admin/markets/{marketId}/result
```

관리자가 공공 데이터 또는 수동 확인 결과를 바탕으로 Market 결과를 확정한다.

이 API는 결과를 확정할 뿐, 실제 포인트 정산을 실행하지 않는다.  
정산 실행은 별도의 정산 API 또는 Scheduler가 담당한다.
`CLOSED`는 단순 시간 마감 상태가 아니라 결과 확정 완료 및 정산 준비 완료 상태를 의미한다.
결과 확정 전에 `POINT_PENDING`, `POINT_UNKNOWN` Prediction이 남아 있으면 `MARKET_INVALID_STATUS`로 실패한다.
미해결 포인트 차감 건이 있는 Market은 아직 정산 준비 상태가 아니다.

허용 상태 전이:

```text
ACTIVE + closeAt <= now → CLOSED
DATA_PENDING → CLOSED
```

### Request

```json
{
  "resultOptionId": 2,
  "resultValue": "0.1834",
  "resultText": "한국부동산원 2026-06-30 기준 OO구 아파트 가격 변동률"
}
```

처리 기준:

```text
YES_NO, MULTIPLE_CHOICE:
- resultOptionId 필수
- resultOptionId가 해당 Market의 option인지 검증

NUMERIC_RANGE:
- resultValue 필수
- 서버가 resultValue와 option range를 비교하여 정답 option 계산
- request.resultOptionId가 있으면 서버 계산 결과와 일치 여부 검증
- 매칭 option 0개: MARKET_WINNING_OPTION_NOT_FOUND
- 매칭 option 2개 이상: MARKET_INVALID_SETTLEMENT_DATA
```

정답 option이 존재하지만 해당 option을 선택한 사용자가 없는 경우에도 결과 확정은 성공한다.
정답자 존재 여부와 지급 처리는 후속 정산 API의 책임이다.

### Response

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "marketId": 1,
    "resultOptionId": 2,
    "resultValue": "0.1834",
    "resultText": "한국부동산원 2026-06-30 기준 OO구 아파트 가격 변동률",
    "status": "CLOSED"
  },
  "timestamp": "2026-06-02T16:00:00"
}
```

이 API는 Member-Point를 호출하지 않고, Prediction 상태 및 PriceHistory를 변경하지 않는다.

### 발생 가능한 ErrorCode

| ErrorCode | HTTP Status | 설명 |
|---|---:|---|
| `MARKET_NOT_FOUND` | 404 | Market 없음 |
| `MARKET_INVALID_STATUS` | 409 | 결과 확정 가능한 상태가 아니거나 미해결 포인트 차감 건이 남아 있음 |
| `MARKET_OPTION_NOT_FOUND` | 404 | 해당 Market의 선택지가 아님 |
| `MARKET_WINNING_OPTION_NOT_FOUND` | 409 | 정답 선택지 계산 실패 |
| `MARKET_INVALID_SETTLEMENT_DATA` | 409 | 정산 데이터 비정상 |
| `VALIDATION_FAILED` | 400 | AnswerType별 필수 결과 값 누락 |
| `FORBIDDEN` | 403 | 관리자 권한 없음 |

---

## 9. 관리자 정산 실행

```http
POST /api/v1/admin/markets/{marketId}/settlements
```

정산 실행 결과로 `market_settlement` 리소스가 생성되므로 복수형 리소스 endpoint를 사용한다.

정산은 결과 확정 이후 실행한다. 정산 시작 조건은 `Market.status = CLOSED`이다.

`CLOSED`는 단순 시간 마감 상태가 아니라 다음 의미다.

```text
CLOSED = 결과 확정 완료 + 정산 준비 완료 상태
```

상태 전이:

```text
CLOSED
→ SETTLEMENT_IN_PROGRESS
→ SETTLED
```

### 처리 흐름

```text
1. Market 조회
2. Market.status = CLOSED 검증
3. Atomic Update로 SETTLEMENT_IN_PROGRESS 획득
4. CONFIRMED Prediction 조회
5. 정산 금액 계산
6. market_settlement 생성
7. 승자 Prediction에 대해서만 market_settlement_detail 생성
8. 승자가 있으면 Member-Point 정산 batch API 호출
   - Header Idempotency-Key는 batch 요청 전체 추적용 필수 헤더다.
   - 각 item은 predictionId 기준 idempotencyKey를 가진다.
   - 각 item은 referenceType=MARKET_PREDICTION, referenceId=predictionId를 가진다.
9. results[]를 기준으로 성공/실패/불명확 detail 상태 반영
10. 패자 Prediction은 status = SETTLED, settledAmount = 0.00 처리
11. 성공한 승자 Prediction은 status = SETTLED, settledAmount = 지급액 처리
12. 모든 승자 detail이 SUCCESS이면 Market SETTLED
13. 일부 detail이 FAILED 또는 UNKNOWN이면 SETTLEMENT_IN_PROGRESS 유지
```

### 정산 완료 후 Insight reputation update 후속 처리

정산 성공 기준은 다음 세 가지가 완료된 상태다.

```text
1. Member-Point 정산 지급 완료
2. Prediction SETTLED 처리 완료
3. Market SETTLED 처리 완료
```

Insight-Reputation Service로의 prediction accuracy update는 Market 정산 성공 여부와 분리된 후속 처리다.

정산이 완료되어 `Market.status = SETTLED`로 전환되면 Market Service는 `market_reputation_update` outbox task를 생성한다. Scheduler는 이 task를 읽어 Insight-Reputation Service의 내부 API를 호출한다.

```text
1. Admin이 Market 정산 요청
2. Member-Point batch settlement 성공
3. winner/loser Prediction SETTLED 처리
4. Market SETTLED 처리
5. market_reputation_update task 생성
6. Scheduler가 Insight-Reputation Service로 prediction accuracy update 전송
```

Market이 호출하는 Insight-Reputation 내부 API:

```http
POST /internal/api/v1/reputations/prediction
```

Request Body:

```json
{
  "memberId": 1,
  "marketId": 7,
  "predictionId": 123,
  "isCorrect": true
}
```

전송 값:

```text
memberId = prediction.memberId
marketId = market.id
predictionId = prediction.id
isCorrect = prediction.optionId == market.resultOptionId
```

주의:

```text
이 endpoint는 Market API가 아니라 Market Service가 호출하는 Insight-Reputation Service의 내부 API다.
Market API_SPEC에서는 서비스 간 연동 흐름 설명으로만 기록한다.
Insight update 실패는 Market.status = SETTLED 상태를 변경하지 않는다.
Insight update 실패로 Prediction.status를 변경하지 않는다.
Insight update 실패로 Member-Point 정산 결과를 되돌리지 않는다.
```

### Atomic Update

```sql
UPDATE market
SET status = 'SETTLEMENT_IN_PROGRESS'
WHERE id = :marketId
  AND status = 'CLOSED';
```

`affected row = 1`이면 정산 시작 권한을 획득한다.
`affected row = 0`이면 이미 정산 중이거나 정산 가능한 상태가 아니므로 중단한다.

### 정산 대상과 보상 대상

```text
정산 생명주기 종료 대상:
- 해당 Market의 모든 CONFIRMED Prediction

보상 지급 대상:
- CONFIRMED 이면서 option_id = market.result_option_id 인 Prediction
```

패자 처리:

```text
패자는 Member-Point 지급 대상이 아니다.
패자는 market_settlement_detail을 생성하지 않는다.
패자 Prediction은 status = SETTLED, settledAmount = 0.00 으로 처리한다.
```

승자 처리:

```text
승자는 market_settlement_detail을 생성한다.
승자는 Member-Point batch 정산 지급 item으로 전송한다.
성공 시 Prediction status = SETTLED, settledAmount = 지급액으로 처리한다.
```

### 정산 금액 계산

```text
totalPool = CONFIRMED Prediction의 pointAmount 합

feeAmount = floor2(totalPool * feeRate / 100)

settlementPool = totalPool - feeAmount

winningContractQuantity =
  정답 option을 선택한 CONFIRMED Prediction의 contractQuantity 합

payoutPerContract =
  winningContractQuantity > 0
    ? settlementPool / winningContractQuantity
    : 0

각 승자 settledAmount =
  floor2(winner.contractQuantity * payoutPerContract)

profitAmount =
  settledAmount - originalPointAmount

burnedPointAmount =
  settlementPool - sum(승자 settledAmount)
```

`floor2`는 Java 기준 아래 처리를 의미한다.

```java
setScale(2, RoundingMode.DOWN)
```

정산 지급 금액은 소수점 둘째 자리까지 사용한다. 셋째 자리 이하는 버림 처리하며 반올림하지 않는다. 소수점 버림으로 인해 남은 잔여 금액은 `burnedPointAmount`에 기록한다.

예시:

```text
33.339 → 33.33
33.335 → 33.33
33.330 → 33.33
```

### 승자 없는 경우

정답 option 없음:

```text
결과 확정 실패
```

정답자는 없음:

```text
결과 확정 성공
정산 API도 실패하지 않음
```

승자 없는 정산 처리:

```text
Member-Point 정산 batch API 호출 없음
market_settlement 생성
market_settlement_detail 생성 0건
winningContractQuantity = 0
payoutPerContract = 0
burnedPointAmount = settlementPool
모든 CONFIRMED Prediction은 status = SETTLED, settledAmount = 0.00
Market.status = SETTLED
market_settlement.status = COMPLETED
```

### Member-Point 정산 요청 예시

```http
POST /api/v1/points/settlements
Idempotency-Key: MARKET_SETTLEMENT_BATCH:market:7:settlement:1:attempt:1
Content-Type: application/json
```

```json
{
  "marketId": 7,
  "settlementId": "settle-market-7-settlement-1-attempt-1",
  "items": [
    {
      "predictionId": 1001,
      "memberId": 1,
      "amount": "190.00",
      "referenceType": "MARKET_PREDICTION",
      "referenceId": 1001,
      "reason": "Market 정산 보상",
      "idempotencyKey": "MARKET_SETTLEMENT_REWARD:market:7:prediction:1001:member:1"
    },
    {
      "predictionId": 1002,
      "memberId": 2,
      "amount": "95.00",
      "referenceType": "MARKET_PREDICTION",
      "referenceId": 1002,
      "reason": "Market 정산 보상",
      "idempotencyKey": "MARKET_SETTLEMENT_REWARD:market:7:prediction:1002:member:2"
    }
  ]
}
```

Header와 item key의 역할 차이:

```text
Header Idempotency-Key:
- Member-Point 정산 batch 요청 전체를 추적하기 위한 필수 헤더다.
- 실제 유저별 중복 지급 방지 기준은 아니다.
- 부분 실패 후 실패 item만 재시도할 경우 새 Header Idempotency-Key를 사용할 수 있다.

items[].idempotencyKey:
- 유저 1명, 즉 Prediction 1건의 정산 지급 멱등성 키다.
- 실제 중복 지급 방지는 이 키를 기준으로 한다.
- 같은 Prediction에 대한 재시도에서는 항상 같은 item.idempotencyKey를 사용한다.
```

### Member-Point 정산 응답 예시

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "marketId": 7,
    "results": [
      {
        "predictionId": 1001,
        "memberId": 1,
        "status": "PROCESSED",
        "errorCode": null,
        "amount": "190.00",
        "balanceSnapshot": "340.00"
      },
      {
        "predictionId": 1002,
        "memberId": 2,
        "status": "FAILED",
        "errorCode": "MEMBER_NOT_FOUND",
        "amount": "95.00",
        "balanceSnapshot": null
      }
    ]
  },
  "timestamp": "2026-05-29T15:30:00"
}
```

### results status 처리

| status | Market 처리 |
|---|---|
| `PROCESSED` | detail `SUCCESS`, Prediction `SETTLED` |
| `ALREADY_PROCESSED` | 성공으로 간주. detail `SUCCESS`, Prediction `SETTLED` |
| `FAILED` | detail `FAILED`, Market `SETTLEMENT_IN_PROGRESS` 유지 |
| status null / 알 수 없는 status / result 누락 / batch timeout | 요청 대상 detail `UNKNOWN`, Market `SETTLEMENT_IN_PROGRESS` 유지 |

모든 승자 detail이 `SUCCESS`이면 `Market.status = SETTLED`로 전환한다.
일부 detail이 `FAILED` 또는 `UNKNOWN`이면 `Market.status = SETTLEMENT_IN_PROGRESS`를 유지한다.
실패 또는 `UNKNOWN` detail은 후속 재시도 API 또는 Scheduler의 대상이 된다.

### 발생 가능한 ErrorCode

| ErrorCode | HTTP Status | 설명 |
|---|---:|---|
| `MARKET_NOT_FOUND` | 404 | Market 없음 |
| `MARKET_INVALID_STATUS` | 409 | 정산 가능한 상태가 아님 |
| `MARKET_ALREADY_SETTLED` | 409 | 이미 정산 완료 |
| `MARKET_NO_PREDICTIONS` | 409 | 정산 대상 없음 |
| `MARKET_INVALID_SETTLEMENT_DATA` | 409 | resultOptionId 없음 등 정산 전제 데이터 비정상 |
| `EXTERNAL_SERVICE_TIMEOUT` | 504 | 포인트 지급 요청 타임아웃 |
| `EXTERNAL_SERVICE_ERROR` | 502 | 포인트 서비스 오류 |

부분 실패는 전체 API 실패로 단정하지 않는다. 성공한 item은 확정하고 실패한 item만 재시도 대상으로 남긴다.

---

## 9-1. 관리자 정산 재시도

```http
POST /api/v1/admin/markets/{marketId}/settlements/retry
```

이미 생성된 `market_settlement`와 `market_settlement_detail` 중 실패 또는 처리 불명확 상태만 재시도한다.
이 API는 새 정산 row를 만들지 않고, 정산 금액을 다시 계산하지 않는다.

### 실행 조건

```text
market.status = SETTLEMENT_IN_PROGRESS
market_settlement.status = IN_PROGRESS
market_settlement_detail.status IN ('FAILED', 'UNKNOWN')
```

재시도 대상 detail이 없을 때:

- 모든 detail이 `SUCCESS`이면 정산 완료 보정으로 `market_settlement.status = COMPLETED`, `market.status = SETTLED`로 전환한다.
- `PENDING` 등 `SUCCESS`가 아닌 detail이 남아 있으면 `MARKET_INVALID_SETTLEMENT_DATA`를 반환한다.

### 재시도 요청 규칙

Header `Idempotency-Key`와 요청 본문의 `settlementId`는 동일한 새 batch 추적 키를 사용한다.

```text
MARKET_SETTLEMENT_BATCH:market:{marketId}:settlement:{settlementId}:retry:{uuid}
```

item별 멱등성 키는 기존 `market_settlement_detail.idempotency_key`를 그대로 사용한다.

```json
{
  "predictionId": 1001,
  "memberId": 1,
  "amount": "190.00",
  "referenceType": "MARKET_PREDICTION",
  "referenceId": 1001,
  "reason": "Market 정산 보상 재시도",
  "idempotencyKey": "MARKET_SETTLEMENT_REWARD:market:7:prediction:1001:member:1"
}
```

### 처리 결과

| Member-Point result.status | Market 처리 |
|---|---|
| `PROCESSED` | detail `SUCCESS`, Prediction `SETTLED` |
| `ALREADY_PROCESSED` | 성공으로 간주. detail `SUCCESS`, Prediction `SETTLED` |
| `FAILED` | detail `FAILED`, Prediction `CONFIRMED` 유지 |
| status null / 알 수 없는 status / result 누락 / timeout 또는 응답 불명확 | 요청 대상 detail `UNKNOWN`, Prediction `CONFIRMED` 유지 |

재시도 후 완료 판단은 해당 retry batch만 보지 않고, 같은 settlement의 모든 detail 기준으로 판단한다.

```sql
SELECT COUNT(*)
FROM market_settlement_detail
WHERE settlement_id = :settlementId
  AND status <> 'SUCCESS';
```

위 count가 `0`이면 `market_settlement.status = COMPLETED`, `market.status = SETTLED`, `market.settled_at = now`로 전환한다.
남아 있으면 `IN_PROGRESS` / `SETTLEMENT_IN_PROGRESS`를 유지한다.

### 발생 가능한 ErrorCode

| ErrorCode | HTTP Status | 설명 |
|---|---:|---|
| `MARKET_NOT_FOUND` | 404 | Market 없음 |
| `MARKET_INVALID_STATUS` | 409 | 재시도 가능한 Market 상태가 아님 |
| `MARKET_ALREADY_SETTLED` | 409 | 이미 정산 완료 |
| `MARKET_INVALID_SETTLEMENT_DATA` | 409 | 진행 중 settlement 없음, PENDING 잔존 등 정산 데이터 비정상 |

재시도 API는 별도 Market ErrorCode를 추가하지 않는다.

---

## 10. 관리자 Market 무효 처리

```http
PATCH /api/v1/admin/markets/{marketId}/void
```

관리자가 Market을 무효 처리한다.
무효 처리 API는 Market 상태와 무효 사유를 확정할 뿐, Member-Point 환불 API를 호출하지 않는다.
실제 환불은 별도의 환불 실행 API가 담당한다.

Request 예시:

```json
{
  "reasonCode": "DATA_UNAVAILABLE",
  "reason": "공공 데이터 제공 중단으로 결과 판정이 불가능합니다."
}
```

요청의 `reasonCode`는 `market_void.reason_type`, `reason`은 `market_void.reason_detail`에 저장한다.

Response 예시:

```json
{
  "marketId": 7,
  "voidId": 1,
  "status": "VOIDED",
  "refundRequired": true,
  "refundablePredictionCount": 12,
  "reasonCode": "DATA_UNAVAILABLE",
  "reason": "공공 데이터 제공 중단으로 결과 판정이 불가능합니다."
}
```

### VOIDED 가능 상태

```text
PENDING
ACTIVE
CLOSED
DATA_PENDING
```

### VOIDED 불가능 상태

```text
SETTLEMENT_IN_PROGRESS
SETTLED
VOIDED
```

정산이 이미 시작된 Market은 관리자도 VOIDED 처리할 수 없다.  
정산 시작 이후 문제가 발생하면 관리자 수동 보정 대상으로 남긴다.

무효 처리 전에 다음 Prediction이 남아 있으면 차단한다.

```text
Prediction.status IN ('POINT_PENDING', 'POINT_UNKNOWN')
```

`POINT_PENDING` 또는 `POINT_UNKNOWN` Prediction은 차감 여부가 불명확하므로 환불 대상 여부를 판단할 수 없다.
먼저 예측 차감 대사 API 또는 Scheduler로 해당 Prediction을 `CONFIRMED` 또는 `FAILED`로 정리해야 한다.

### 처리 흐름

```text
1. Market FOR UPDATE 조회
2. Market 존재 여부 확인
3. Market.status가 PENDING / ACTIVE / CLOSED / DATA_PENDING 중 하나인지 확인
4. SETTLEMENT_IN_PROGRESS / SETTLED / VOIDED이면 차단
5. POINT_PENDING / POINT_UNKNOWN Prediction 존재 여부 확인
6. 존재하면 차단
7. market_void row 생성
8. Market.status = VOIDED 변경
9. CONFIRMED Prediction 수를 조회하여 refundRequired / refundablePredictionCount 반환
10. Member-Point 호출 없음
```

무효 처리 API는 환불 대상 Prediction의 포인트를 직접 환불하지 않는다.

### 발생 가능한 ErrorCode

| ErrorCode | HTTP Status | 설명 |
|---|---:|---|
| `MARKET_NOT_FOUND` | 404 | Market 없음 |
| `MARKET_CANNOT_VOID` | 409 | 정산 중/정산 완료/이미 무효 등 무효 처리 불가능 |
| `MARKET_INVALID_STATUS` | 409 | 미해결 Prediction 존재 등 상태 전제 불충족 |
| `FORBIDDEN` | 403 | 관리자 권한 없음 |

---

## 10-1. 관리자 환불 실행

```http
POST /api/v1/admin/markets/{marketId}/refunds
```

VOIDED Market의 `CONFIRMED` Prediction에 대해 원금 환불을 실행한다.
환불 대상은 실제 포인트 차감과 가격 확정이 완료된 `CONFIRMED` Prediction이다.

시작 조건:

```text
Market.status = VOIDED
```

환불 대상:

```text
Prediction.status = CONFIRMED
```

환불 대상 아님:

| PredictionStatus | 처리 |
|---|---|
| `POINT_PENDING` | 차감 여부 불명확. 먼저 대사 필요 |
| `POINT_UNKNOWN` | 차감 여부 불명확. 먼저 대사 필요 |
| `FAILED` | 포인트 미차감 또는 실패. 환불 대상 아님 |
| `SETTLED` | 이미 정산 완료. 환불 대상 아님 |
| `REFUND_PENDING` | 이미 환불 요청 중 |
| `REFUND_UNKNOWN` | 환불 여부 불명확. 재시도 대상 |
| `REFUNDED` | 이미 환불 완료 |

환불 금액:

```text
refundAmount = prediction.pointAmount
```

무효 처리는 Market 자체가 성립하지 않는 상황이므로 `CONFIRMED` Prediction 참여자에게 예측 참여 원금을 환불한다.
수수료를 차감하지 않고, 정산 금액 계산처럼 비율 계산을 하지 않는다.

### 처리 흐름

트랜잭션 A - 환불 준비:

```text
1. VOIDED Market 조회
2. market_void 조회
3. CONFIRMED Prediction 조회
4. 환불 대상 Prediction을 REFUND_PENDING으로 변경
5. market_refund_detail 생성
   - marketVoidId
   - predictionId
   - memberId
   - refundAmount = prediction.pointAmount
   - status = PENDING
   - idempotencyKey = MARKET_REFUND:market:{marketId}:prediction:{predictionId}:member:{memberId}
6. commit
```

트랜잭션 A에서는 Member-Point를 호출하지 않는다.

트랜잭션 밖 - Member-Point refund batch 호출:

```text
1. PENDING refund_detail 기준으로 items 생성
2. Header Idempotency-Key 생성
3. body refundId 또는 batchId 생성
4. Member-Point refund batch API 호출
```

Header Idempotency-Key:

```text
MARKET_REFUND_BATCH:market:{marketId}:void:{voidId}:attempt:1
```

Item idempotencyKey:

```text
MARKET_REFUND:market:{marketId}:prediction:{predictionId}:member:{memberId}
```

트랜잭션 B - 환불 결과 반영:

```text
PROCESSED / ALREADY_PROCESSED:
- market_refund_detail.status = SUCCESS
- Prediction.status = REFUNDED
- Prediction.refundAmount = refundAmount

FAILED:
- market_refund_detail.status = FAILED
- Prediction.status = REFUND_PENDING 또는 REFUND_UNKNOWN 유지

batch timeout / 응답 불명확:
- market_refund_detail.status = UNKNOWN
- Prediction.status = REFUND_UNKNOWN

status null, 알 수 없는 status, result 누락도 실패로 단정하지 않고 UNKNOWN 흐름으로 처리한다.
```

일부 item 실패는 전체 환불 실패로 단정하지 않는다.
성공한 item은 `REFUNDED`로 확정하고, 실패/UNKNOWN item만 재시도 대상으로 남긴다.

환불 대상이 없는 경우:

```text
Market.status = VOIDED
CONFIRMED Prediction = 0
```

환불 실행 API는 성공으로 응답한다.
Member-Point refund batch API를 호출하지 않고, `market_refund_detail`은 0건 생성된다.

### Member-Point 환불 요청 예시

```http
POST /api/v1/points/refunds
Idempotency-Key: MARKET_REFUND_BATCH:market:7:void:1:attempt:1
Content-Type: application/json
```

```json
{
  "marketId": 7,
  "refundId": "MARKET_REFUND_BATCH:market:7:void:1:attempt:1",
  "items": [
    {
      "predictionId": 1001,
      "memberId": 1,
      "amount": "100.00",
      "referenceType": "MARKET_PREDICTION",
      "referenceId": 1001,
      "reason": "Market 무효 처리 환불",
      "idempotencyKey": "MARKET_REFUND:market:7:prediction:1001:member:1"
    }
  ]
}
```

Header와 item key 역할:

```text
Header Idempotency-Key:
- Member-Point 환불 batch 요청 전체를 추적하기 위한 필수 헤더다.
- 실제 유저별 중복 환불 방지 기준은 아니다.
- 부분 실패 후 실패 item만 재시도할 경우 새 Header Idempotency-Key를 사용할 수 있다.

items[].idempotencyKey:
- Prediction 1건의 환불 멱등성 키다.
- 실제 중복 환불 방지는 이 키를 기준으로 한다.
- 같은 Prediction에 대한 재시도에서는 항상 같은 item.idempotencyKey를 사용한다.
```

Response 예시:

```json
{
  "marketId": 7,
  "voidId": 1,
  "refundTargetCount": 12,
  "successCount": 10,
  "failedCount": 2,
  "unknownCount": 0,
  "marketStatus": "VOIDED",
  "refundStatus": "IN_PROGRESS"
}
```

환불 완료 기준:

```text
모든 refund_detail.status = SUCCESS이면 refundStatus = COMPLETED
하나라도 FAILED 또는 UNKNOWN이면 refundStatus = IN_PROGRESS
```

Market.status는 계속 `VOIDED`로 유지한다.

### 발생 가능한 ErrorCode

| ErrorCode | HTTP Status | 설명 |
|---|---:|---|
| `MARKET_NOT_FOUND` | 404 | Market 없음 |
| `MARKET_INVALID_STATUS` | 409 | VOIDED가 아닌 Market 환불 실행 |
| `MARKET_REFUND_NOT_ALLOWED` | 409 | 환불 대상이 아닌 Prediction |
| `MARKET_ALREADY_REFUNDED` | 409 | 이미 환불 완료 |
| `MARKET_REFUND_FAILED` | 500 | 환불 처리 실패 |
| `EXTERNAL_SERVICE_TIMEOUT` | 504 | 환불 요청 타임아웃 |
| `EXTERNAL_SERVICE_ERROR` | 502 | 포인트 서비스 오류 |
| `EXTERNAL_SERVICE_UNAVAILABLE` | 503 | 포인트 서비스 연결 실패 |

---

## 10-2. 관리자 환불 재시도

```http
POST /api/v1/admin/markets/{marketId}/refunds/retry
```

기존 환불 실행 중 `FAILED`, `UNKNOWN` 또는 3분 이상 `PENDING`으로 남은 `market_refund_detail`만 재시도한다.

재시도 대상:

```text
market_refund_detail.status IN ('FAILED', 'UNKNOWN')
OR (
    market_refund_detail.status = 'PENDING'
    AND market_refund_detail.updated_at <= now - 3 minutes
)
```

재시도 금지:

```text
새 market_void 생성 금지
새 market_refund_detail 생성 금지
refundAmount 재계산 금지
성공한 SUCCESS detail 재요청 금지
items[].idempotencyKey 재생성 금지
```

Header Idempotency-Key:

```text
MARKET_REFUND_BATCH:market:{marketId}:void:{voidId}:retry:{uuid}
```

items[].idempotencyKey:

```text
기존 market_refund_detail.idempotency_key 그대로 사용
```

처리:

```text
PROCESSED / ALREADY_PROCESSED:
- refund_detail SUCCESS
- Prediction REFUNDED

FAILED:
- refund_detail FAILED
- Prediction REFUND_PENDING 유지

timeout / 응답 불명확:
- refund_detail UNKNOWN
- Prediction REFUND_UNKNOWN

status null, 알 수 없는 status, result 누락도 실패로 단정하지 않고 UNKNOWN 흐름으로 처리한다.
```

---

## 10-3. 내부 환불 재시도 API / Scheduler 대상

```http
POST /api/v1/internal/markets/refunds/retry?limit=100
```

VOIDED Market 중 `FAILED`, `UNKNOWN` 또는 3분 이상 `PENDING` refund_detail이 남아 있는 Market을 limit 단위로 조회하여 환불 재시도를 수행한다.

대상:

```text
market.status = VOIDED
market_refund_detail.status IN ('FAILED', 'UNKNOWN')
OR (
    market_refund_detail.status = 'PENDING'
    AND market_refund_detail.updated_at <= now - 3 minutes
)
```

처리:

```text
기존 관리자 환불 재시도 Service 로직을 marketId별로 호출한다.
Controller를 HTTP로 자기 호출하지 않는다.
Scheduler는 Service를 직접 호출한다.
```

이번 문서 작업에서는 실제 Scheduler 구현은 하지 않는다.
추후 환불 API 구현 이후 Scheduler 자동화 작업에서 처리한다.

---

## 11. Insight-Reputation 내부 연계 API

Insight-Reputation Service가 Market AI 리포트 생성을 위해 Market 원본 참여 데이터를 조회하는 내부 API이다.

### 기본 정책

```text
1. Insight 분석용 내부 API는 읽기 전용 API이다.
2. Market Service는 Market 원본 참여 데이터만 제공한다.
3. Market Service는 회원 프로필 정보(성별, 나이, 거주지역, 이메일, 이름 등)를 제공하지 않는다.
4. Market Service는 memberId까지만 제공한다.
5. 회원 프로필 정보는 Insight-Reputation 또는 Member-Point Service에서 별도 조회한다.
6. 분석 대상 Market은 SETTLED 상태만 허용한다.
7. 응답 크기 증가를 방지하기 위해 Market 요약/선택지 집계 API와 Prediction 페이지 조회 API를 분리한다.
8. Decimal 필드는 기존 정책과 동일하게 JSON String으로 응답한다.
```

분석 가능 상태:

```text
MarketStatus = SETTLED
```

SETTLED가 아닌 Market에 대해 Insight 데이터 조회를 요청하면 `MARKET_INVALID_STATUS`를 반환한다.

---

### 11-1. Market Insight 요약 및 선택지 집계 조회

```http
GET /internal/api/v1/markets/{marketId}/insight-summary
```

#### 사용 목적

```text
Insight-Reputation Service가 Market AI 리포트 생성을 위해
Market 기본 정보와 선택지별 집계 데이터를 조회한다.
```

#### Path Variable

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `marketId` | Long | O | 조회할 Market ID |

#### Response

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "market": {
      "marketId": 7,
      "title": "다음 주 서울 아파트 매매가격지수는 상승할까?",
      "category": "PRICE_INDEX",
      "answerType": "NUMERIC_RANGE",
      "status": "SETTLED",
      "closeAt": "2026-05-27T23:59:59",
      "judgeDate": "2026-06-01",
      "judgeDataSource": "REB",
      "judgeCriteria": "서울 아파트 매매가격지수 주간 변동률",
      "resultOptionId": 2,
      "resultValue": "0.3400",
      "resultText": null,
      "totalPredictionCount": 79,
      "totalPoolAmount": "25000.00",
      "settlementPoolAmount": "23750.00",
      "settledAt": "2026-06-02T10:00:00"
    },
    "optionStatistics": [
      {
        "optionId": 1,
        "optionCode": "RANGE_1",
        "optionLabel": "0.4% 이상",
        "rangeMin": "0.4000",
        "rangeMax": null,
        "minInclusive": true,
        "maxInclusive": false,
        "predictionCount": 32,
        "participantCount": 32,
        "poolAmount": "10500.00",
        "finalPrice": "0.42100000",
        "totalContractQuantity": "24940.61757720",
        "isResult": false
      },
      {
        "optionId": 2,
        "optionCode": "RANGE_2",
        "optionLabel": "0.3% 이상 ~ 0.4% 미만",
        "rangeMin": "0.3000",
        "rangeMax": "0.4000",
        "minInclusive": true,
        "maxInclusive": false,
        "predictionCount": 47,
        "participantCount": 47,
        "poolAmount": "14500.00",
        "finalPrice": "0.57900000",
        "totalContractQuantity": "25043.17789291",
        "isResult": true
      }
    ]
  },
  "timestamp": "2026-06-02T10:00:00"
}
```

#### 필드 설명

| 필드 | 설명 |
|---|---|
| `market` | Market 기본 정보와 정산 결과 요약 |
| `optionStatistics` | 선택지별 참여 수, Pool 금액, 최종 가격, 정답 여부 집계 |
| `optionLabel` | `market_option.option_text`를 응답 DTO에서 사용하는 이름 |
| `participantCount` | 해당 선택지를 선택한 회원 수. 현재 정책상 한 회원은 한 Market에 하나의 Prediction만 가지므로 `predictionCount`와 동일할 수 있다. |
| `totalPoolAmount` | Insight 문맥에서는 실제 참여 포인트 총합. 가격 계산용 `totalEffectivePoolAmount`가 아님 |
| `poolAmount` | 선택지별 실제 참여 포인트 합. `virtualPoolAmount`를 포함하지 않음 |
| `finalPrice` | SETTLED 시점의 선택지 최종 가격. `market_option.current_price` 기준 |
| `isResult` | 정답 선택지 여부 |

#### 발생 가능한 ErrorCode

| ErrorCode | HTTP Status | 설명 |
|---|---:|---|
| `MARKET_NOT_FOUND` | 404 | Market 없음 |
| `MARKET_INVALID_STATUS` | 409 | SETTLED 상태가 아닌 Market |
| `MARKET_NO_PREDICTIONS` | 409 | 분석할 예측 참여 데이터가 없음 |

---

### 11-2. Market Insight Prediction 페이지 조회

```http
GET /internal/api/v1/markets/{marketId}/insight-predictions?page=0&size=500
```

#### 사용 목적

```text
Insight-Reputation Service가 회원별 예측 참여 원본 데이터를 페이지 단위로 조회한다.
회원의 성별, 나이대, 거주지역 등 프로필 정보는 포함하지 않는다.
```

#### Query Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `page` | int | X | 페이지 번호. 기본값 0 |
| `size` | int | X | 페이지 크기. 기본값 500, 최대 1000 |

#### Response

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "content": [
      {
        "predictionId": 100,
        "memberId": 10,
        "optionId": 2,
        "optionCode": "RANGE_2",
        "optionLabel": "0.3% 이상 ~ 0.4% 미만",
        "pointAmount": "100.00",
        "priceSnapshot": "0.68750000",
        "contractQuantity": "145.45454545",
        "status": "SETTLED",
        "isCorrect": true,
        "participatedAt": "2026-05-21T15:30:00"
      },
      {
        "predictionId": 101,
        "memberId": 15,
        "optionId": 1,
        "optionCode": "RANGE_1",
        "optionLabel": "0.4% 이상",
        "pointAmount": "300.00",
        "priceSnapshot": "0.40120000",
        "contractQuantity": "747.75822431",
        "status": "SETTLED",
        "isCorrect": false,
        "participatedAt": "2026-05-22T11:20:00"
      }
    ],
    "page": 0,
    "size": 500,
    "totalElements": 1200,
    "totalPages": 3,
    "last": false
  },
  "timestamp": "2026-06-02T10:00:00"
}
```

#### 필드 설명

| 필드 | 설명 |
|---|---|
| `predictionId` | Market Prediction ID |
| `memberId` | 참여 회원 ID. Market Service가 제공하는 유일한 회원 식별 정보 |
| `optionId` | 선택한 Market Option ID |
| `optionCode` | 선택지 코드 |
| `optionLabel` | 선택지 표시명 |
| `pointAmount` | 사용 포인트 |
| `priceSnapshot` | 예측 확정 시점 1계약 가격 |
| `contractQuantity` | 구매 계약 수량 |
| `status` | Prediction 상태. Insight 조회 대상은 SETTLED 기준 |
| `isCorrect` | `prediction.optionId == market.resultOptionId` 기준으로 계산한 적중 여부 |
| `participatedAt` | 예측 참여 시각 |

#### 발생 가능한 ErrorCode

| ErrorCode | HTTP Status | 설명 |
|---|---:|---|
| `MARKET_NOT_FOUND` | 404 | Market 없음 |
| `MARKET_INVALID_STATUS` | 409 | SETTLED 상태가 아닌 Market |
| `MARKET_NO_PREDICTIONS` | 409 | 분석할 예측 참여 데이터가 없음 |

---

### 11-3. Insight 연계 책임 범위

| 항목 | Market Service | Insight-Reputation Service |
|---|---|---|
| Market 원본 정보 | 제공 | 사용 |
| 선택지별 집계 데이터 | 제공 | 사용 |
| Prediction 원본 데이터 | 제공 | 사용 |
| memberId | 제공 | 기준 키로 사용 |
| 회원 이름/이메일 | 제공하지 않음 | 필요 시 Member-Point 조회 |
| 성별/나이대/거주지역 | 제공하지 않음 | Insight/Reputation 또는 Member-Point 조회 |
| AI 분석/Claude 호출 | 수행하지 않음 | 수행 |
| insight_report 저장 | 수행하지 않음 | 수행 |


## 12. 내부 Scheduler API

> 내부 Scheduler API는 외부 클라이언트에 공개하지 않는다.  
> Gateway 또는 내부 네트워크에서만 접근 가능하도록 제한한다.
> 자동 Scheduler는 Controller를 HTTP로 호출하지 않고, 기존 Service를 직접 호출한다.
> 단일 인스턴스 중복 실행은 `AtomicBoolean` guard로 방지하며, 분산락은 MVP 범위에서 제외한다.

자동 Scheduler 기본 설정:

| Scheduler | 호출 Service | 기본 주기 | 기본 limit |
|---|---|---:|---:|
| 예측 차감 대사 | `PredictionSpendReconciliationService.reconcile(limit)` | 60초 | 100 |
| 정산 재시도 | `MarketSettlementService.retryFailedSettlements(limit)` | 180초 | 50 |
| 환불 재시도 | `MarketRefundService.retryFailedRefunds(limit)` | 180초 | 50 |
| Insight prediction accuracy update | `MarketReputationUpdateService.processPendingOrUnknownUpdates(limit)` | 180초 | 50 |

테스트 환경에서는 Scheduler를 비활성화한다.

### 12-1. 포인트 차감 상태 대사

```http
POST /api/v1/internal/markets/predictions/reconcile?limit=100
```

`POINT_PENDING` 또는 `POINT_UNKNOWN` 상태로 남은 예측 참여 포인트 차감 건을 Member-Point 거래 상태 조회 API로 대사한다.
이 API는 사용자용 API가 아니라 내부 운영/보정용 API다. 추후 Scheduler가 주기적으로 호출할 수 있다.

Query Parameter:

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `limit` | int | X | 100 | 한 번에 처리할 최대 Prediction 수 |

limit 정책:

```text
기본값: 100
최대값: 500
0 이하 값은 VALIDATION_FAILED
```

대상:

```text
1. status = POINT_UNKNOWN

2. status = POINT_PENDING
   AND updated_at <= now - 3 minutes
```

`POINT_UNKNOWN`은 포인트 차감 여부가 불명확한 상태이므로 대사 대상이다.
`POINT_PENDING`은 방금 생성된 정상 처리 중 상태일 수 있으므로 즉시 대사하지 않는다.
`updated_at` 기준 3분 이상 지난 `POINT_PENDING`만 고착 상태로 보고 대사한다.

Member-Point 거래 상태 조회 API:

```http
GET /api/v1/points/transactions?idempotencyKey={pointSpendIdempotencyKey}
```

조회 key:

```text
market_prediction.point_spend_idempotency_key
```

key 형식:

```text
MARKET_PREDICTION_SPEND:market:{marketId}:member:{memberId}:attempt:{attemptNo}
```

Member-Point 문서의 과거 예시에 `predictionId`가 포함된 spend key가 있더라도,
Market의 현재 정책은 `marketId + memberId + attemptNo` 형식이다.

Member-Point 조회 결과별 Market 처리:

| Member-Point transaction status | Market 처리 |
|---|---|
| `PROCESSED` | 포인트 차감 성공. 가격 확정 트랜잭션 재시도 후 Prediction `CONFIRMED` |
| `FAILED` | 포인트 차감 실패. Prediction `FAILED` |
| `NOT_FOUND` | point_history 없음. 자동 재차감하지 않고 Prediction `FAILED` |
| `UNKNOWN` | 처리 여부 불명확. Prediction `POINT_UNKNOWN` 유지 |
| 조회 timeout / 5xx | 처리 여부 불명확. Prediction `POINT_UNKNOWN` 유지 |

`NOT_FOUND`는 point_history가 존재하지 않는다는 뜻이다.
Member-Point 담당자 확인 결과, `POINT_INSUFFICIENT` 같은 명확한 실패도 거래 조회 시 `NOT_FOUND`로 나올 수 있다.
따라서 Market은 `NOT_FOUND`를 보고 자동으로 다시 spend 요청을 보내지 않는다.
자동 재차감은 사용자의 이후 잔액 상태로 예측을 뒤늦게 성공시키는 문제가 생길 수 있다.

```text
예측 시점에는 포인트 부족
→ Member-Point에서 POINT_INSUFFICIENT
→ Market은 응답 유실로 POINT_UNKNOWN 유지
→ 이후 거래 조회 결과 NOT_FOUND
→ 그 사이 유저가 포인트를 충전
→ Market이 자동 재차감하면 원래 실패했어야 할 예측이 뒤늦게 성공할 수 있음
```

정책:

```text
NOT_FOUND → Prediction FAILED
```

따라서 대사 API는 자동 spend 재시도를 하지 않는다.

가격 확정 트랜잭션 재시도 시에도 `Market row lock + 모든 option row lock` 규칙을 동일하게 적용한다.
`PROCESSED`일 때는 단순히 Prediction 상태만 `CONFIRMED`로 바꾸면 안 된다.
반드시 예측 참여 성공 시 사용하는 가격 확정 트랜잭션을 재사용해야 한다.

처리 내용:

```text
1. Market row lock
2. 해당 Market의 모든 MarketOption row lock
3. selected option의 현재 가격 기준 priceSnapshot 확정
4. contractQuantity 계산
5. selected option realPoolAmount 증가
6. 전체 option currentPrice 재계산
7. PriceHistory 저장
8. Prediction CONFIRMED 전환
```

Prediction `CONFIRMED`만 UPDATE하고 pool, priceHistory, contractQuantity를 반영하지 않는 구현은 금지한다.

사용자가 `closeAt` 이전에 예측 참여 요청을 했고,
Prediction이 `POINT_PENDING` 또는 `POINT_UNKNOWN`으로 남은 경우,
대사 API는 `closeAt` 이후에도 `PROCESSED` 보정을 허용한다.
사용자의 예측 요청과 포인트 차감은 `closeAt` 이전에 시작되었을 수 있기 때문이다.

대사 가격 확정 허용 Market.status:

```text
ACTIVE
DATA_PENDING
```

대사 가격 확정 차단 Market.status:

```text
CLOSED
SETTLEMENT_IN_PROGRESS
SETTLED
VOIDED
```

결과 확정 API는 `POINT_PENDING` / `POINT_UNKNOWN` Prediction이 남아 있으면 `CLOSED`로 전환하지 않는다.
따라서 정상 흐름에서는 대사 대상이 있는 Market이 `CLOSED` 이상 상태일 수 없다.
`CLOSED` 이상 상태에서 대사 대상이 발견되면 데이터 정합성 오류로 보고 skip 또는 로그 대상으로 처리한다.

응답 예시:

```json
{
  "success": true,
  "errorCode": null,
  "message": null,
  "data": {
    "requestedLimit": 100,
    "scannedCount": 15,
    "processedCount": 15,
    "confirmedCount": 8,
    "failedCount": 4,
    "notFoundCount": 3,
    "unknownCount": 2,
    "skippedCount": 1
  },
  "timestamp": "2026-06-04T16:00:00"
}
```

| 필드 | 설명 |
|---|---|
| `requestedLimit` | 요청 limit |
| `scannedCount` | DB에서 조회한 대사 대상 Prediction 수 |
| `processedCount` | Member-Point 거래 상태 조회를 시도한 수 |
| `confirmedCount` | `PROCESSED` 확인 후 `CONFIRMED` 처리 성공 수 |
| `failedCount` | `FAILED` 또는 `NOT_FOUND`로 `FAILED` 처리한 수 |
| `notFoundCount` | `NOT_FOUND` 응답 수 |
| `unknownCount` | `UNKNOWN` 또는 조회 실패로 `POINT_UNKNOWN` 유지한 수 |
| `skippedCount` | Market 상태 불일치, 데이터 불일치 등으로 처리하지 않고 넘긴 수 |

---

### 12-2. 정산 실패 건 재시도

```http
POST /internal/api/v1/markets/settlements/retry-failed?limit=100
```

자동 Scheduler는 내부 HTTP endpoint를 호출하지 않고, `MarketSettlementService.retryFailedSettlements(limit)`를 직접 호출한다.

대상:

```text
MarketStatus = SETTLEMENT_IN_PROGRESS
market_settlement_detail.status IN ('FAILED', 'UNKNOWN')
```

처리:

```text
limit 건만 marketId 조회
→ marketId별 기존 정산 재시도 Service 호출
→ PROCESSED/ALREADY_PROCESSED면 detail SUCCESS, Prediction SETTLED
→ 모든 대상 성공 시 Market SETTLED
```

---

### 12-3. 환불 실패 건 재시도

```http
POST /api/v1/internal/markets/refunds/retry?limit=100
```

대상:

```text
market_refund_detail.status IN ('FAILED', 'UNKNOWN')
OR (
    market_refund_detail.status = 'PENDING'
    AND market_refund_detail.updated_at <= now - 3 minutes
)
```

처리:

```text
limit 건만 조회
→ Member-Point 환불 재시도
→ PROCESSED/ALREADY_PROCESSED면 detail SUCCESS, Prediction REFUNDED
```

---

### 12-4. 공공 데이터 수집 재시도

```http
POST /internal/api/v1/markets/settlement-data/retry-fetch?limit=100
```

대상:

```text
MarketStatus = DATA_PENDING
예상 수집일로부터 3일 이내
```

처리:

```text
limit 건만 조회
→ 공공 데이터 수집 재시도
→ 데이터 수집 성공 시 결과 계산 단계로 이동
→ 3일 초과 시 관리자 확인 대상
```

---

## 13. Market API 완료 기준

- [ ] Decimal 필드는 JSON String으로 응답한다.
- [ ] 가격 이력 조회 API는 page/size 페이징을 지원한다.
- [ ] Market 상세 조회는 `initialPrice`, `currentPrice`, `priceChangeRate`, pool/참여 지표를 제공한다.
- [ ] Quote API는 예상 계약 수량, 참여 후 예상 가격, 가격 영향도를 제공한다.
- [ ] Quote API는 Prediction 생성, 포인트 차감, 잔액 조회, price_history 저장을 수행하지 않는다.
- [ ] 실제 예측 참여는 Quote가 아니라 최신 Pool 상태 기준으로 `priceSnapshot`, `contractQuantity`를 확정한다.
- [ ] MVP에서는 slippage tolerance를 필수로 두지 않고 가격 변동 안내 문구를 제공한다.
- [ ] 예측 참여 API는 `Idempotency-Key`를 필수로 받는다.
- [ ] 예측 참여 API는 Prediction을 먼저 `POINT_PENDING`으로 저장하고 커밋한다.
- [ ] DB 비관적 락을 잡은 상태로 Member-Point HTTP API를 호출하지 않는다.
- [ ] Member-Point 포인트 차감 요청에 `referenceType=MARKET_PREDICTION`, `referenceId=predictionId`를 전달한다.
- [ ] 포인트 차감 타임아웃 시 `POINT_UNKNOWN`으로 처리한다.
- [ ] 가격 확정 트랜잭션은 Market row와 해당 Market의 모든 option row를 고정 순서로 락 잡는다.
- [ ] 가격 확정 트랜잭션에서 `priceSnapshot`, `contractQuantity`를 확정한다.
- [ ] 한 사용자는 하나의 Market에 하나의 Prediction만 가질 수 있다.
- [ ] 정산/환불 item에는 `referenceType=MARKET_PREDICTION`, `referenceId=predictionId`를 전달한다.
- [ ] 정산/환불 item별 `idempotencyKey`를 사용한다.
- [ ] 정산 시작은 Atomic Update로 `SETTLEMENT_IN_PROGRESS`를 획득한다.
- [ ] 정산 완료 후 Insight prediction accuracy update는 outbox task로 분리 처리한다.
- [ ] Insight update 실패는 Market SETTLED 상태와 Prediction SETTLED 상태를 변경하지 않는다.
- [ ] 내부 Scheduler API는 `limit` 파라미터로 chunk 처리한다.
- [ ] Insight-Reputation 내부 API는 SETTLED Market만 조회 가능하게 한다.
- [ ] Insight-Reputation 내부 API는 요약/선택지 집계와 Prediction 페이지 조회를 분리한다.
- [ ] Insight-Reputation 내부 API는 memberId까지만 제공하고 회원 프로필 정보는 제공하지 않는다.
- [ ] `SETTLEMENT_IN_PROGRESS`, `SETTLED` 상태는 VOIDED 처리할 수 없다.
