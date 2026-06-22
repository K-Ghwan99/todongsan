# Market Point Deduction Reconciliation Report

## 1. 검사 목적

사용자가 관찰한 “Market 참여와 pool 증가는 반영됐지만 10P가 차감되지 않은 것처럼 보임”이 실제 DB의 Type A 불일치인지 확인한다.

- 검사 시각: 2026-06-21T12:58:32+09:00
- 검사 방식: SELECT only
- DB 변경 및 seed/cleanup 실행: 없음
- 집중 회원: `memberId=2`

## 2. 방향별 결론

| 유형 | 전체 DB 존재 여부 | memberId=2의 10P 사례 | 결론 |
|---|---|---|---|
| Type A: Market 반영, Point 차감 없음 | 기술적으로 1건, `100P` | 없음 | 1건은 member `5001`이 실제 Member 테이블에도 없는 Boundary 실험 fixture |
| Type B: Point 차감, Market Prediction 없음 | 3건, 총 `30P` | 3건 | 별도 과거 DB 정합성 문제 |

따라서 사용자가 관찰한 **“10P가 안 빠졌는데 Market 반영”은 현재 DB가 뒷받침하지 않는다.** member 2의 현재 CONFIRMED Prediction은 모두 SUCCEEDED spend와 연결되며 balance에도 차감됐다.

## 3. CONFIRMED Prediction과 SUCCEEDED History 연결

### 3.1 reference_id 엄격 연결

- 불일치: 2건, 총 `110P`

| Prediction | Market | Member | 금액 | Strict history | 판단 |
|---:|---:|---:|---:|---|---|
| `5001` | `2300` | `5001` | `100P` | 없음 | Type A fixture 후보 |
| `920022` | `2300` | `2` | `10P` | 없음 | 아래 멱등성 키로 재연결됨 |

### 3.2 idempotency_key 재연결

- 재연결 후 불일치: 1건, 총 `100P`

Prediction `920022`의 key `MARKET_PREDICTION_SPEND:market:2300:member:2:attempt:1`은 Point History `3`의 key와 정확히 같다. History의 `reference_id`는 과거 Prediction `920017`을 가리키지만, 경제적으로는 현재 `920022 CONFIRMED`와 연결된다.

재연결 후 유일한 Type A row:

| Prediction | Market/Option | Member | 금액 | Market/Option 반영 | Member row | History |
|---:|---|---:|---:|---|---|---|
| `5001` | `2300/2301` | `5001` | `100P` | total pool `110`, option pool `100`, count `1` | 없음 | 없음 |

키 형식이 `BOUNDARY_EXPERIMENT_KEY_5001`이고 Member row도 없으므로 일반 사용자 참여가 아니라 Transaction Boundary 실험 데이터로 판단하는 것이 타당하다. 다만 순수 DB 원장 규칙상 Type A인 것은 맞다.

## 4. CONFIRMED 합계와 SUCCEEDED 합계 비교

멱등성 키 기준으로 현재 CONFIRMED Prediction과 SUCCEEDED spend를 연결했다.

| Market | CONFIRMED 합계 | SUCCEEDED 합계 | Market−Point | 원인 |
|---:|---:|---:|---:|---|
| `2300` | `110P` | `10P` | `+100P` | Prediction `5001` Boundary fixture |

다른 현재 CONFIRMED Market은 합계 차이가 없다. 특히 `900016`은 CONFIRMED `10P`와 SUCCEEDED `10P`가 일치한다.

## 5. memberId=2 Prediction별 연결 상태

| Prediction | Market | 금액 | 상태 | Strict 연결 | Key 연결 | Balance snapshot | 판정 |
|---:|---:|---:|---|---|---|---:|---|
| `920022` | `2300` | `10P` | CONFIRMED | 없음 | History `3` | `30P` | 차감됨, ID만 재연결 |
| `920023` | `900016` | `10P` | CONFIRMED | History `6` | History `6` | `0P` | 정상 차감 |

member 2 전체 Point 원장:

- SUCCEEDED earn 합계: `50P`
- SUCCEEDED spend 합계: `50P`
- 계산 잔액: `0P`
- 실제 `member.point_balance`: `0P`

따라서 member 2의 CONFIRMED Prediction에서 balance 차감이 누락된 사례는 없다.

## 6. market.total_pool과 SUCCEEDED spend 비교

Point History key에서 Market ID를 추출해 모든 성공 spend를 포함했다.

| Market | total_pool | SUCCEEDED spend | 차이 | 방향 |
|---:|---:|---:|---:|---|
| `2300` | `110P` | `10P` | `+100P` | Type A fixture |
| `2400` | `0P` | `10P` | `-10P` | Type B |
| `900016` | `10P` | `10P` | `0P` | 정상 |
| `900013` | Market 없음 | `10P` | 산정 불가 | Type B |
| `900015` | Market 없음 | `10P` | 산정 불가 | Type B |

사용자 실제 검증 Market `900016`에는 “pool 10P, Point spend 10P”가 동시에 존재한다.

## 7. option.real_pool_amount와 SUCCEEDED spend 비교

현재 Prediction의 exact spend key로 Option에 배분 가능한 성공 이력을 비교했다.

| Option | Market | real_pool | 연결된 SUCCEEDED spend | 차이 | 원인 |
|---:|---:|---:|---:|---:|---|
| `2301` | `2300` | `100P` | `0P` | `+100P` | Boundary fixture `5001` |

Option `2302`는 pool `10P`와 key 재연결 History `3`의 `10P`가 일치한다. Market `900016`의 선택 Option `910033`도 pool `10P`와 History `6`의 `10P`가 일치한다.

Prediction이 사라진 Type B History는 Option ID를 더 이상 복원할 수 없어 Option별 합계에 배분하지 않았다.

## 8. Type B 별도 결과

멱등성 키로도 현재 Prediction이 없는 성공 spend는 3건, 총 `30P`다.

| History | Member | Market | 금액 | 누락 Prediction | Balance snapshot |
|---:|---:|---:|---:|---:|---:|
| `2` | `2` | `900013` | `10P` | `920016` | `40P` |
| `4` | `2` | `2400` | `10P` | `920018` | `20P` |
| `5` | `2` | `900015` | `10P` | `920021` | `10P` |

이 결과는 Point 차감은 있으나 Market 데이터가 없는 반대 방향 문제다. 이번 사용자가 관찰한 Type A 증상의 증거가 아니다.

## 9. 사용자 증상 판정

### 판정: DB Type A보다 프론트 캐시 표시 문제 가능성이 높음

근거:

- 실제 검증 Prediction `920023/Market 900016`은 POST 직후 CONFIRMED였다.
- Market total/Option pool은 각각 `10P` 반영됐다.
- Point History `6`은 `SPEND_MARKET/SUCCEEDED/10P`, balance snapshot `0P`다.
- 실제 member balance도 `0P`다.
- member 2의 다른 현재 CONFIRMED 10P Prediction `920022`도 exact key로 SUCCEEDED History `3`과 연결된다.
- member 2에 “CONFIRMED 10P + Point History 없음” 또는 “CONFIRMED 10P + balance 미차감” row가 없다.

따라서 DB에서는 포인트가 차감됐는데 브라우저가 이전 `10P`를 표시한 것으로 보는 편이 근거에 맞다. 예측 성공 후 point balance/member profile query invalidate·refetch와 로그아웃/재로그인 cache 격리를 우선 확인해야 한다.

## 10. cleanup/delete와 방향성 구분

cleanup/delete는 성공 Point History를 남기고 Prediction을 제거할 수 있으므로 **Type B 원인 후보**다. 하지만 이미 존재하는 Market Prediction/pool에 대응하는 Point 차감만 없게 만드는 **Type A 원인으로는 부족하다.**

Type A를 설명하려면 Point spend 호출 미실행·실패인데 Market confirm이 수행됐거나, 실험 fixture를 직접 구성한 별도 경로가 필요하다. 현재 일반 사용자 member 2의 10P 데이터에서는 그런 경로가 확인되지 않았다.

## 11. 최종 결론

- Type A 전체 DB: 존재하나 Boundary 실험 fixture 100P 한 건
- Type A member 2 / 10P: 없음
- Type B: 존재, 3건 30P
- 사용자의 “10P 미차감처럼 보임”: DB로 뒷받침되지 않음
- 정상 사용자 경로 결론: 백엔드 Point/Market 정합성 정상, 프론트 stale cache 후보가 더 강함
