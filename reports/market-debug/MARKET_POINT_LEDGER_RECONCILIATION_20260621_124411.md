# Market Point Ledger Reconciliation Report

## 1. 검사 개요

- 검사 시각: 2026-06-21T12:44:11+09:00
- 대상 스키마: `memberpoint`, `market`
- 검사 방식: SELECT only
- 집중 회원: `memberId=2`
- 집중 Market: `900013`, `900015`, `900016`, `2300`, `2400`
- seed/cleanup 실행 및 DB 변경: 없음

연결은 먼저 `point_history.reference_id = market_prediction.id`로 엄격 검사하고, Prediction 삭제 후 재생성 가능성을 구분하기 위해 `point_history.idempotency_key = market_prediction.point_spend_idempotency_key`도 추가 검사했다.

## 2. 발견된 불일치 건수

| 검사 | reference_id 기준 | 멱등성 키 보정 후 | 해석 |
|---|---:|---:|---|
| CONFIRMED Prediction인데 SUCCEEDED Point History 없음 | 2 | 1 | `920022`는 이전 history와 키가 같아 ID만 단절됨 |
| SUCCEEDED Point History인데 Prediction 없음 | 4 | 3 | history id `3`은 새 Prediction `920022`와 키로 재연결됨 |
| `market.total_pool` vs 현재 CONFIRMED 합계 | 0 | 해당 없음 | 현재 남은 Market 내부 합계는 일치 |
| `market_option.real_pool_amount` vs 현재 CONFIRMED 합계 | 0 | 해당 없음 | 현재 남은 Option 내부 합계는 일치 |

경제적 연결까지 고려한 핵심 불일치는 member 2의 10P 지출 3건, 총 `30P`다.

## 3. 불일치 Row 상세

### 3.1 CONFIRMED인데 reference_id로 SUCCEEDED History가 없는 Prediction

| Prediction | Market | Member | 금액 | Spend key | 판단 |
|---:|---:|---:|---:|---|---|
| `5001` | `2300` | `5001` | `100.00` | `BOUNDARY_EXPERIMENT_KEY_5001` | exact key로도 history 없음. Transaction Boundary 실험 fixture 성격이나 원장상 불일치 |
| `920022` | `2300` | `2` | `10.00` | `...market:2300:member:2:attempt:1` | history id `3`과 exact key 일치. history reference는 삭제된 `920017`을 가리켜 ID 연결만 단절 |

### 3.2 SUCCEEDED History인데 reference_id Prediction이 없는 Row

| History | Member | 금액 | Balance snapshot | 누락 Prediction | 추론 Market | Exact key Prediction | 판단 |
|---:|---:|---:|---:|---:|---|---|
| `2` | `2` | `10.00` | `40.00` | `920016` | `900013` | 없음 | 진짜 유령 10P 후보 |
| `3` | `2` | `10.00` | `30.00` | `920017` | `2300` | `920022` | 새 ID로 경제적 재연결, 참조 무결성은 깨짐 |
| `4` | `2` | `10.00` | `20.00` | `920018` | `2400` | 없음 | 진짜 유령 10P 후보 |
| `5` | `2` | `10.00` | `10.00` | `920021` | `900015` | 없음 | 진짜 유령 10P 후보 |

## 4. memberId=2 이력 타임라인

| 시각 | History | Market | 차감 | 차감 후 잔액 | 현재 연결 상태 |
|---|---:|---:|---:|---:|---|
| 2026-06-18 00:16:37 | `2` | `900013` | `10P` | `40P` | Market/Prediction 모두 없음 |
| 2026-06-18 14:28:00 | `3` | `2300` | `10P` | `30P` | 원 Prediction `920017` 없음, 같은 key의 `920022 CONFIRMED` 존재 |
| 2026-06-18 17:17:20 | `4` | `2400` | `10P` | `20P` | Market은 ACTIVE지만 Prediction 없음, pool `0` |
| 2026-06-19 00:15:18 | `5` | `900015` | `10P` | `10P` | Market/Prediction 모두 없음 |
| 2026-06-21 11:08:33 | `6` | `900016` | `10P` | `0P` | Prediction `920023 CONFIRMED`와 정상 직접 연결 |

회원 원장은 가입 적립 `50P`, 성공한 Market 차감 `50P`, 현재 balance `0P`로 Member-Point 내부 계산은 정확히 일치한다. 반면 현재 member 2의 CONFIRMED Prediction 합계는 `20P`뿐이므로 `30P`는 Market 경제 상태에 대응하지 않는다.

## 5. 지정 Market별 상태

| Market | Market 상태 | Prediction/History 상태 | Pool 상태 | 판단 |
|---:|---|---|---|---|
| `900013` | Market 없음 | 성공 history `2`, Prediction `920016` 없음 | 확인 불가 | Market과 Prediction 소실 |
| `900015` | Market 없음 | 성공 history `5`, Prediction `920021` 없음 | 확인 불가 | Market과 Prediction 소실 |
| `900016` | ACTIVE | `920023 CONFIRMED` ↔ history `6 SUCCEEDED` 정상 | total `10`, option YES `10` | 정상 |
| `2300` | ACTIVE | `5001` 및 `920022` CONFIRMED. history `3`은 old id `920017` 참조, key는 `920022`와 일치 | total `110`, option 합계 `110` | 경제 합계 정상, ledger reference 단절 |
| `2400` | ACTIVE | 성공 history `4`, Prediction `920018` 없음. 현재 `5002 FAILED`만 존재 | total/option pool `0` | 차감 10P가 Market에 없음 |

## 6. Pool 정합성

현재 DB에 남아 있는 모든 Market에 대해 다음 두 검사는 불일치 `0건`이다.

- `market.total_pool = SUM(CONFIRMED market_prediction.point_amount)`
- `market_option.real_pool_amount = SUM(CONFIRMED market_prediction.point_amount by option)`

이는 현재 Market 내부 집계가 자기 일관적이라는 뜻이지, 과거 성공 차감이 모두 보존됐다는 뜻은 아니다. 삭제된 Prediction과 함께 pool이 초기화됐거나 Market 자체가 삭제된 경우 이 두 검사만으로는 유령 포인트를 발견하지 못한다.

## 7. 진짜 유령 10P 가능성

### 결론: 있음 — 3건, 총 30P

- Market `900013`: 10P
- Market `2400`: 10P
- Market `900015`: 10P

세 history 모두 `SPEND_MARKET/SUCCEEDED`이고 balance snapshot에 순차 반영됐지만, reference_id와 exact idempotency key 어느 기준으로도 현재 Prediction이 없다.

Market `2300`의 10P는 엄격 reference 기준으로는 불일치지만 같은 회원·Market·attempt의 exact key를 가진 `920022 CONFIRMED`가 있고 pool에도 10P가 포함되어 있어 경제적 유령으로 보지 않는다. 다만 `point_history.reference_id=920017`과 현재 Prediction ID가 달라 감사 추적 링크는 깨져 있다.

## 8. seed/cleanup 삭제 흔적 가능성

가능성이 매우 높지만, 실행 로그가 없으므로 이 SELECT 검사만으로 실행 주체와 시각까지 확정할 수는 없다.

근거:

- 누락 Prediction ID `920016`, `920017`, `920018`, `920021`은 모두 과거 seed/현재 cleanup의 광범위한 `prediction_id 920001~920399` 범위에 포함된다.
- `900013`, `900015`는 Market cleanup 범위 `900001~900099` 안이며 현재 Market 자체가 없다.
- `2300`, `2400`은 Market ID 범위 밖이지만 Prediction ID 조건만으로 삭제 대상이 될 수 있다.
- `2300`은 이전 ID `920017`이 사라진 뒤 동일 spend key의 새 Prediction `920022`가 생성된 형태여서 삭제 후 재생성 흔적과 부합한다.
- `900016/920023`은 최근 검증에서 생성됐고 현재 정상 보존돼 있어, 그 이후 destructive cleanup이 실행된 흔적은 없다.

현재 add-only seed 재실행은 삭제 원인이 아니다. 다만 별도 cleanup의 Prediction ID 범위는 비-seed Market `2300`, `2400`의 자동 생성 Prediction까지 삭제할 수 있으므로 실행 전 대상 검토가 필수다.

## 9. 최종 판정

### DB 정합성 문제와 프론트 캐시 문제는 별개로 모두 존재 가능

- 정상 경로 `900016`: DB balance/history, Prediction, Market/Option pool과 API가 모두 정상이다. 이 경로에서 브라우저만 stale하면 프론트 cache invalidation/refetch 문제로 보는 것이 맞다.
- 과거 이력 `900013`, `2400`, `900015`: 성공 차감은 남고 Prediction이 없으므로 실제 DB 원장 정합성 문제다. 프론트 캐시만으로 설명할 수 없다.
- `2300`: 현재 경제 상태는 맞지만 Point History가 과거 Prediction ID를 가리키는 참조 단절 문제다.

따라서 “현재 정상 참여 후 UI가 늦는 증상”의 우선 후보는 프론트 캐시지만, member 2의 과거 30P 소실은 별도의 Market 데이터 삭제/초기화 계열 DB 문제로 판정한다.

## 10. 후속 제안

- cleanup 실행 전 실제 대상 Market/Prediction ID를 SELECT로 미리 출력하고 operator 확인을 받는 절차가 필요하다.
- cleanup의 전역 Prediction ID 범위가 비-seed Market Prediction까지 포함하는 점을 재검토해야 한다.
- member 2의 30P 보정 여부는 Market/Member-Point 담당자가 실험 기록과 삭제 실행 로그를 확인한 뒤 결정해야 한다. 이 보고서는 어떠한 보정 SQL도 실행하지 않았다.
- 브라우저 캐시 검증은 정상 데이터가 남아 있는 별도 회원/Market 조합으로 수행해야 한다.
