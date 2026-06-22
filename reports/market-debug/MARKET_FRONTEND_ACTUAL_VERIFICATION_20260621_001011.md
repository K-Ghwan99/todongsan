# Market Frontend Actual Verification Report

## 1. 테스트 환경

- 실행 시각: 2026-06-21T00:10:11+09:00
- MySQL container: `todongsan-mysql` (실행 중, host `127.0.0.1:3307`)
- Market service URL: `http://localhost:8082` (`/actuator/health` 200, `UP`)
- Point base URL: `http://localhost:8080` (`/actuator/health` 403; 보안 정책 가능성이 있어 장애로 판정하지 않음)
- Gateway URL: `http://localhost:9000` (`/actuator/health` 404; endpoint 미노출 가능성)
- memberId: `2`
- 계정 식별자: `rl***@naver.com`
- marketId: `900016`
- optionId: 확인하지 않음(안전 중단)
- seed 범위 여부: **해당 (`900001~900099`)**
- 테스트 전 포인트 잔액: `10.00P`

## 2. 테스트 Market 생성 결과

- 생성 API: `POST http://localhost:8082/api/v1/admin/markets`
- 활성화 API: 실행하지 않음
- 생성 제목: `DEBUG_LATENCY_TEST_20260621_000954`
- 생성 HTTP status: `200`
- 생성 응답: `success=true`, `marketId=900016`
- marketId: `900016`
- optionId: 확인하지 않음
- Market 상태: API 계약상 생성 직후 `PENDING`; 별도 detail 조회 전 안전 중단
- 생성 성공 여부: 성공
- 활성화 성공 여부: 실행하지 않음

생성된 ID가 작업 지시서의 즉시 중단 조건인 seed 범위에 포함되어 활성화와 후속 테스트를 수행하지 않았다. DB 직접 수정이나 삭제로 ID를 조정·정리하지 않았다.

## 3. 사전 DB 상태 요약

- member.point_balance: `10.00P`
- point_history 최근 이력: 조회하지 않음(테스트 Market 확정 전 중단)
- market_prediction 존재 여부: 조회하지 않음
- market.total_pool: 조회하지 않음
- option 상태: 조회하지 않음

## 4. 예측 POST 결과

- HTTP status: 실행하지 않음
- 응답 success/errorCode/message: 해당 없음
- 응답 data의 prediction status: 해당 없음
- Idempotency-Key: 생성/전송하지 않음
- 실패 시 원인: POST 실패가 아니라, 생성된 Market ID가 seed 범위여서 사전 안전 조건에서 중단

## 5. T+0/1/3/5/10/30초 반영 결과

| 시점 | Market detail 반영 | My prediction 상태 | Point balance API | DB member balance | DB prediction status | DB market total_pool | 판단 |
|---|---|---|---|---|---|---|---|
| T+0 | 미실행 | 미실행 | 미실행 | 미실행 | 미실행 | 미실행 | seed 범위 안전 중단 |
| T+1 | 미실행 | 미실행 | 미실행 | 미실행 | 미실행 | 미실행 | seed 범위 안전 중단 |
| T+3 | 미실행 | 미실행 | 미실행 | 미실행 | 미실행 | 미실행 | seed 범위 안전 중단 |
| T+5 | 미실행 | 미실행 | 미실행 | 미실행 | 미실행 | 미실행 | seed 범위 안전 중단 |
| T+10 | 미실행 | 미실행 | 미실행 | 미실행 | 미실행 | 미실행 | seed 범위 안전 중단 |
| T+30 | 미실행 | 미실행 | 미실행 | 미실행 | 미실행 | 미실행 | seed 범위 안전 중단 |

## 6. 사후 DB 상태 요약

- member.point_balance: 예측 POST를 하지 않았으므로 최초 확인값 `10.00P`에서 차감 없음으로 예상하나 사후 snapshot은 실행하지 않음
- point_history 성공/실패 여부: 예측 요청 미실행
- market_prediction status: 생성되지 않음
- market.total_pool: 측정하지 않음
- market_option 변경: 측정하지 않음
- price_history 생성 여부: 예측 요청 미실행

## 7. 원인 판정

### 판정: 케이스 A~F 분류 전 안전 중단

예측 POST 자체를 호출하지 않았으므로 A~F 중 하나로 정합성 원인을 판정할 데이터가 없다. 중단 원인은 Admin API의 자동 생성 ID `900016`이 seed 삭제 범위 `900001~900099`와 충돌한 것이다.

이는 실제 참여 테스트에 해당 Market을 사용하면 향후 dev seed 재실행으로 Prediction이 사라지고 Member-Point 차감만 남을 수 있다는 기존 분석의 위험 조건이다. 안전 규칙에 따라 `ALLOW_SEED_MARKET` 우회도 사용하지 않았다.

## 8. 다음 조치 제안

- Frontend 수정 필요 여부: 이번 실행으로 판단 불가
- Market-service 수정 필요 여부: 실제 정합성은 판단 불가. 테스트 Market ID를 seed 범위 밖에서 발급할 수 있는 안전한 개발 절차가 먼저 필요
- Member-Point 담당자 전달 필요 여부: 현재 없음. 계정 잔액 `10.00P`만 확인됨
- seed 운영 규칙 수정 필요 여부: 필요. seed의 높은 명시적 ID가 auto-increment 다음 값도 seed 범위에 위치시키므로, Admin API 생성 Market이 seed cleanup 대상이 되는 충돌을 해소해야 함
- 후속 테스트 조건: DB 직접 수정 없이 Admin API가 `900100` 이상 또는 `900000` 이하의 비-seed ID를 발급하는 환경/절차를 준비한 뒤 새 Market으로 처음부터 재실행

## 9. 첨부 보고서 경로

- 사전 consistency: 미생성(중단 조건 발생)
- latency: 미생성(실제 포인트 차감 미실행)
- 사후 consistency: 미생성(중단 조건 발생)

## 10. 안전 중단 기록

- DB 변경 SQL: 실행하지 않음
- Market 활성화 API: 실행하지 않음
- Prediction API: 실행하지 않음
- 실제 포인트 차감: 없음
- 생성된 PENDING Market 정리: 금지된 DB 변경을 피하기 위해 수행하지 않음
