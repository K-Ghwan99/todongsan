# Market Frontend Actual Verification Report

## 1. 테스트 환경 및 데이터

- 실행 시각: 2026-06-21 11:07~11:09 KST
- MySQL container: `todongsan-mysql`
- Market service: `http://localhost:8082`
- Point service: `http://localhost:8080`
- memberId: `2`
- 테스트 전 잔액: `10.00P`
- marketId: `900016`
- optionId: `910033` (`YES`)
- pointAmount: `10.00P`
- dev test range 승인: `ALLOW_DEV_TEST_RANGE=true`
- cleanup/seed SQL 실행: 없음

## 2. 테스트 Market 준비 결과

- 기존 Market 조회: `GET /api/v1/markets/900016` → HTTP 200
- 최초 상태: `PENDING`, `canPredict=false`
- 활성화: `PATCH /api/v1/admin/markets/900016/activate` + `X-Member-Role: ADMIN` → HTTP 200
- 활성화 응답: `marketId=900016`, `status=ACTIVE`
- 활성화 후 상세: HTTP 200, `status=ACTIVE`, `canPredict=true`
- 최초 Market total pool: `0.00`
- 최초 옵션:
  - `910033 YES`: real pool `0.00`, price `0.50000000`, prediction count `0`
  - `910034 NO`: real pool `0.00`, price `0.50000000`, prediction count `0`

## 3. 실행한 명령

```bash
MEMBER_ID=2 MARKET_ID=900016 \
./scripts/market-debug/check-consistency.sh

CONFIRM_DEDUCT_POINTS=YES \
ALLOW_DEV_TEST_RANGE=true \
MEMBER_ID=2 \
MARKET_ID=900016 \
OPTION_ID=910033 \
POINT_AMOUNT=10 \
API_BASE_URL=http://localhost:8082 \
POINT_BASE_URL=http://localhost:8080 \
./scripts/market-debug/measure-prediction-latency.sh

MEMBER_ID=2 MARKET_ID=900016 \
./scripts/market-debug/check-consistency.sh
```

## 4. 생성된 보고서 경로

- 사전 consistency: `reports/market-debug/consistency-member-2-market-900016-20260621-110807.md`
- latency: `reports/market-debug/latency-member-2-market-900016-20260621-110823.md`
- 사후 consistency: `reports/market-debug/consistency-member-2-market-900016-20260621-110930.md`

## 5. 사전 DB 상태

- member balance: `10.00`
- 해당 member/Market Prediction: 없음
- Market status/total pool: `ACTIVE / 0.00`
- 선택 옵션 real pool/price/count: `0.00 / 0.50000000 / 0`
- price history: 없음

## 6. 예측 POST 결과

- API: `POST /api/v1/markets/900016/predictions`
- HTTP status: `200`
- success/errorCode/message: `true / null / null`
- Idempotency-Key: `MARKET_PREDICTION_SPEND:market:900016:member:2`
- predictionId: `920023`
- selectedOptionId: `910033`
- pointAmount: `10.00`
- priceSnapshot: `0.50000000`
- contractQuantity: `20.00000000`
- 응답 Prediction status: `CONFIRMED`

## 7. 시점별 반영 결과

| 시점 | Market detail 반영 | My prediction | Point balance API | DB member balance | DB Prediction | DB total pool | 판단 |
|---|---|---|---|---:|---|---:|---|
| T+0 | 200, pool `10.00`, 가격 반영 | 200, `CONFIRMED` | 403 | `0.00` | `CONFIRMED` | `10.00` | 즉시 반영 |
| T+1 | 반영 유지 | `CONFIRMED` | 403 | `0.00` | `CONFIRMED` | `10.00` | 정상 |
| T+3 | 반영 유지 | `CONFIRMED` | 403 | `0.00` | `CONFIRMED` | `10.00` | 정상 |
| T+5 | 반영 유지 | `CONFIRMED` | 403 | `0.00` | `CONFIRMED` | `10.00` | 정상 |
| T+10 | 반영 유지 | `CONFIRMED` | 403 | `0.00` | `CONFIRMED` | `10.00` | 정상 |
| T+30 | 반영 유지 | `CONFIRMED` | 403 | `0.00` | `CONFIRMED` | `10.00` | 정상 |

Point balance API의 403은 직접 호출 시 인증이 없기 때문으로 보이며, 모든 시점의 DB snapshot에서 잔액 `0.00`과 성공 이력을 확인했다. 따라서 포인트 차감 실패로 판정하지 않는다.

## 8. 사후 DB 상태

- `member.point_balance`: `10.00 → 0.00`, 정확히 10P 차감
- `point_history`: id `6`, `SPEND_MARKET`, amount `10.00`, balance snapshot `0.00`, status `SUCCEEDED`
- Point reference: `MARKET_PREDICTION / 920023`
- 내부 spend key: `MARKET_PREDICTION_SPEND:market:900016:member:2:attempt:1`
- `market_prediction`: id `920023`, status `CONFIRMED`, attempt `1`, fail reason 없음
- `market.total_pool`: `0.00 → 10.00`
- 선택 옵션 `910033`:
  - real pool: `0.00 → 10.00`
  - total contract quantity: `0 → 20.00000000`
  - current price: `0.50000000 → 0.52380952`
  - prediction count: `0 → 1`
- 반대 옵션 `910034`:
  - real pool: `0.00` 유지
  - current price: `0.50000000 → 0.47619048`
  - prediction count: `0` 유지
- `market_price_history`: Prediction `920023`에 대해 옵션별 2행 생성
  - YES: `0.50000000 → 0.52380952`, real pool `0.00 → 10.00`
  - NO: `0.50000000 → 0.47619048`, real pool `0.00 → 0.00`

## 9. 원인 판정

### 판정: 백엔드 정합성 정상, 프론트 캐시 문제 후보

POST가 HTTP 200과 `CONFIRMED`를 반환했고 T+0 구간부터 다음 값이 모두 일치했다.

- Member-Point 잔액 차감 및 `SUCCEEDED` history
- Market Prediction `CONFIRMED`
- Market total pool
- 양 옵션 가격과 선택 옵션 pool/count
- 옵션별 price history
- Market detail API와 My Prediction API

따라서 이번 정상 경로에서는 `POINT_PENDING`, `POINT_UNKNOWN`, Prediction 소실 또는 DB 간 불일치가 재현되지 않았다. DB/API가 이미 T+0부터 정상인데 브라우저에서 잔액이나 참여 상태가 이전 값으로 남는다면 프론트의 point balance/member profile query invalidate·refetch 누락이 우선 후보다.

브라우저 UI 자체는 이번 CLI 측정에 포함되지 않았으므로 프론트 stale 현상을 확정한 것은 아니며, 백엔드 정상 기준선을 확보한 결과다.

## 10. 다음 조치 제안

- Frontend: 예측 성공 후 point balance, member profile, point history query invalidate/refetch를 브라우저 Network 및 Query Devtools로 검증한다.
- Market-service: 이번 정상 경로에 대한 수정 필요성은 확인되지 않았다. timeout/프로세스 중단 경로는 별도 장애 주입 테스트가 필요하다.
- Member-Point: 직접 point balance API 403은 인증 조건에 따른 것으로 보인다. Gateway/JWT 경유 테스트에서 API 잔액 `0.00`을 추가 확인할 수 있다.
- Seed/cleanup: 실제 참여 데이터가 있는 Market `900016`, member `2`, Prediction `920023`을 기록한다. cleanup 실행 시 Member-Point 잔액/history가 복구되지 않으므로 별도 정합성 판단 없이 삭제하지 않는다.
