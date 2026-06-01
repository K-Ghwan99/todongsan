# AGENTS.md — Market Service

> 루트 `AGENTS.md`의 공통 지침을 상속한다. 이 파일은 Market Service에 특화된 컨텍스트를 추가한다.
> 루트 `AGENTS.md`를 먼저 읽고 이 파일을 읽는다.

---

## 내 서비스 컨텍스트

- **서비스명:** Market Service
- **패키지 루트:** `com.todongsan.marketservice`
- **담당 문서:**
  - `docs/market/MARKET_ERD.md` (ERD 최신본)
  - `docs/market/MARKET_API_SPEC.md` (API 명세 최신본)
  - `docs/market/MARKET_ERROR_CODE.md` (도메인 에러 코드)
  - `docs/market/MARKET_FAILURE_SCENARIO.md` (장애 시나리오)

### 핵심 도메인 테이블

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

### 핵심 비즈니스 제약 (코드 작성 전 반드시 숙지)

```
[예측 참여 흐름 - 순서 중요]
1. POINT_PENDING 예측 기록 생성 후 커밋
2. DB 락을 잡지 않은 상태에서 Member-Point spend 호출 (Point 차감)
3. 성공 시 별도 가격 확정 트랜잭션 시작
4. Market row를 먼저 락 조회
5. 해당 Market의 모든 MarketOption row를 optionId 오름차순으로 락 조회
6. pool 갱신, 가격 재계산, PriceHistory 저장, CONFIRMED 전환
7. 포인트 부족 등 확정 실패는 FAILED, 타임아웃 등 처리 여부 불명확 상태는 POINT_UNKNOWN

[가격 모델]
- POOL_SHARE만 사용한다.
- 선택지 가격 = 해당 선택지 pool / 전체 선택지 pool 합
- 가격 확정 트랜잭션은 한 Market 단위로 순차 처리한다.
- 외부 HTTP API 호출 중 DB 락을 유지하지 않는다.

[정산]
- 정산 시작은 Atomic Update로 SETTLEMENT_IN_PROGRESS 권한을 획득한다.
- CONFIRMED 예측만 정산 대상에 포함한다.
- 사용자별 정산 지급 detail item에 각각 멱등성 키를 사용한다.
- 일부 지급 실패 시 SETTLEMENT_IN_PROGRESS를 유지하고 실패 건만 재시도한다.

[무효 처리]
- SETTLEMENT_IN_PROGRESS, SETTLED 상태는 VOIDED 처리할 수 없다.
- 환불 대상 detail item에 각각 멱등성 키를 사용한다.
- 환불 타임아웃은 REFUND_UNKNOWN으로 기록하고 처리 이력을 대사한다.

[응답]
- Decimal 필드는 JSON String으로 응답한다.
```

---

## 연계 서비스

### 내가 호출하는 서비스

| 서비스 | 엔드포인트 | 목적 | 주의사항 |
|---|---|---|---|
| Member-Point Service | `POST /api/v1/points/spend` | 예측 참여 Point 차감 | `referenceType=MARKET_PREDICTION`, `referenceId=predictionId` |
| Member-Point Service | `GET /api/v1/points/transactions?idempotencyKey={key}` | 차감·환불 처리 상태 대사 | POINT_UNKNOWN 및 고착 상태 복구 |
| Member-Point Service | `POST /api/v1/points/settlements` | 정산 보상 지급 | detail item별 멱등성 키 |
| Member-Point Service | `POST /api/v1/points/refunds` | 무효 처리 환불 | detail item별 멱등성 키 |

### 나를 호출하는 서비스

| 서비스 | 엔드포인트 | 목적 |
|---|---|---|
| Insight-Reputation Service | `GET /internal/api/v1/markets/{marketId}/insight-summary` | SETTLED Market 요약 및 선택지 집계 조회 |
| Insight-Reputation Service | `GET /internal/api/v1/markets/{marketId}/insight-predictions` | SETTLED Market 예측 원본 페이지 조회 |

---

## 이 서비스의 성공 기준

```
[Market 예측 참여]
- POST /api/v1/markets/{marketId}/predictions
  → Prediction POINT_PENDING 선저장 후 포인트 차감
  → Point 차감 및 가격 확정 성공: CONFIRMED
  → 잔액 부족 등 확정 실패: FAILED
  → 타임아웃 등 처리 여부 불명확: POINT_UNKNOWN
  → 3분 이상 POINT_PENDING 고착 건도 대사 대상
  → 동일 Market 사용자 중복 참여 차단
  → 포인트 범위: 최소 10P, 최대 500P

[정산]
- POST /api/v1/admin/markets/{marketId}/settle
  → Atomic Update로 중복 정산 실행 차단
  → detail item별 멱등성 키로 중복 지급 방지
  → 일부 지급 실패 시 실패 건만 재시도

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

## 이 서비스에서 하지 않는 것

```
✗ Point를 직접 DB에서 관리하지 않는다. Member-Point Service REST 호출만.
✗ Battle 주제(사용자 투표 결정 주제)는 Market으로 등록하지 않는다.
✗ 공공 API 데이터가 없어도 임의로 결과를 판정하지 않는다.
✗ CONFIRMED가 아닌 예측은 정산 대상에 포함하지 않는다.
✗ DB 락을 잡은 트랜잭션 안에서 외부 HTTP API를 호출하지 않는다.
✗ Insight-Reputation용 분석 결과를 Market Service에 저장하지 않는다.
```

---

## 작업 전 체크리스트

```
[ ] 루트 AGENTS.md 읽었는가?
[ ] docs/market/MARKET_ERD.md (최신본) 읽었는가?
[ ] docs/market/MARKET_API_SPEC.md (최신본) 읽었는가?
[ ] docs/market/MARKET_FAILURE_SCENARIO.md 읽었는가?
[ ] 연계 작업이면 docs/member-point/MEMBER_POINT_API_SPEC.md를 읽었는가?
[ ] 예측 참여 흐름 순서 (POINT_PENDING 커밋 → spend → 가격 확정 트랜잭션)가 맞는가?
[ ] 외부 HTTP 호출 중 DB 락을 유지하고 있지 않은가?
[ ] Market row와 모든 MarketOption row를 고정 순서로 락 조회하는가?
[ ] POINT_UNKNOWN과 3분 이상 POINT_PENDING 고착 건을 대사하는가?
[ ] Member-Point 요청에 referenceType=MARKET_PREDICTION, referenceId=predictionId를 세팅하는가?
[ ] 정산·환불 detail item별 멱등성 키를 사용하고 있는가?
[ ] Decimal 필드를 JSON String으로 응답하는가?
[ ] Scheduler가 limit 기반 chunk 처리를 하는가?
[ ] Insight-Reputation 내부 조회는 SETTLED Market만 허용하고 memberId까지만 제공하는가?
```
