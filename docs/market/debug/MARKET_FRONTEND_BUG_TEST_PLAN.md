# Market Frontend Bug Test Plan

## 공통 안전 규칙

- dev test 범위 Market ID `900001~900099`는 기본적으로 피한다. 삭제 로직이 seed에서 분리된 로컬 환경에서 해당 범위를 사용할 때는 실행 시각과 memberId/marketId를 기록하고 `ALLOW_DEV_TEST_RANGE=true`를 명시한다.
- add-only seed 재실행 안전성은 격리된 TC-01에서 검증한다. Market 부분 삭제 SQL은 사용하지 않는다.
- 개별 테스트 중 volume reset과 DB 삭제 SQL은 금지한다. 완전 초기화가 필요하면 테스트를 종료하고 `scripts/market-debug/README.md`의 별도 전체 MySQL volume reset 절차를 따른다.
- 실행 전 회원/Market/옵션과 현재 잔액을 기록한다. 예측 POST는 실제 포인트를 차감하므로 승인된 테스트 계정만 쓴다.
- DB 조회는 `scripts/market-debug/check-consistency.sh`, 시간 측정은 `measure-prediction-latency.sh`를 우선 사용한다.

## TC-01. add-only dev seed 재실행 안전성 검증

### 목적

dev seed 재실행이 기존 Market/Prediction을 삭제하거나 갱신하지 않는지 검증한다.

### 사전 조건

- 로컬 전용 환경에서 seed 범위와 Admin API 생성 실험 Market의 기준 snapshot을 확보한다.
- 비활성화된 과거 cleanup 자료는 실행하지 않는다.

### 절차

1. seed 범위와 비범위의 Market/Prediction 수를 SELECT로 기록한다.
2. 테스트 회원의 balance와 관련 history를 기록한다.
3. seed 범위 Market의 실제 참여 이력이 있다면 Prediction ID와 spend 멱등성 키를 기록한다.
4. `dev_seed_market_frontend_scenarios.sql`을 한 번 재실행한다.
5. 같은 SELECT를 반복하고 Member-Point 결과와 비교한다.

### 기대 결과

누락된 고정 seed 행만 추가되고 기존 Market, Prediction 및 Member-Point balance/history는 바뀌지 않는다. 실제 참여 Prediction도 유지된다.

### 확인 API

- `GET /api/v1/markets/{marketId}`
- `GET /api/v1/markets/{marketId}/predictions/me`
- `GET /api/v1/points/balance`

### 확인 SQL

`market_prediction`, `market`, `market_option`, `market_price_history`를 seed 범위와 비범위로 COUNT/조회하고, `memberpoint.point_history.idempotency_key` 및 balance를 조회한다.

### 판단 기준

기존 행 수와 주요 값이 유지되면 통과다. 기존 Prediction 삭제/변경이 발생하면 add-only 정책 위반이다.

### 사람이 직접 확인해야 하는 부분

seed 실행 대상이 폐기 가능한 로컬 DB인지, UI에서 참여 상태가 사라지는지 확인한다.

## TC-02. 정상 예측 참여 DB 정합성 검증

### 목적

정상 POST 한 건이 두 DB와 Market 집계에 일관되게 반영되는지 확인한다.

### 사전 조건

- seed 범위 밖 ACTIVE Market, 참여 이력이 없는 회원, 유효 옵션, 충분한 잔액.

### 절차

1. 사전 consistency snapshot을 저장한다.
2. 고유한 유효 `Idempotency-Key`로 10P 예측 POST를 한 번 실행한다.
3. 즉시 및 30초 뒤 snapshot을 저장한다.
4. Prediction key와 point history key/reference를 교차 확인한다.

### 기대 결과

balance가 10P 감소하고 `SPEND_MARKET/SUCCEEDED` history 하나, `CONFIRMED` Prediction 하나가 존재한다. Market total pool, 선택 옵션 pool/계약/건수, 옵션별 price history가 같은 트랜잭션 시각대에 갱신된다.

### 확인 API

- `POST /api/v1/markets/{marketId}/predictions`
- `GET /api/v1/markets/{marketId}`
- `GET /api/v1/markets/{marketId}/predictions/me`
- `GET /api/v1/points/balance`

### 확인 SQL

`check-consistency.sh`의 전체 조회와 `point_history.idempotency_key = market_prediction.point_spend_idempotency_key` 비교.

### 판단 기준

두 DB와 API가 모두 일치하면 정상. point 성공/Market 미확정은 대사 후보, DB 정상/API 불일치는 조회 계층 후보다.

### 사람이 직접 확인해야 하는 부분

POST 중복 클릭 여부와 브라우저가 보낸 헤더/본문을 확인한다.

## TC-03. 예측 참여 직후 API 반영 속도 검증

### 목적

POST 뒤 Market detail, 내 예측, 잔액 및 DB가 언제 바뀌는지 측정한다.

### 사전 조건

TC-02와 동일하며 `measure-prediction-latency.sh`의 실제 차감 경고를 확인한다.

### 절차

1. 지연 측정 스크립트를 실행한다.
2. 사전, POST, 0/1/3/5/10/30초 보고서를 비교한다.
3. POST status가 pending/unknown이면 3분 이후 별도 snapshot과 scheduler 로그를 확인한다.

### 기대 결과

POST가 `CONFIRMED`를 반환했다면 0초 조회부터 detail/my prediction/DB가 반영되어야 한다. 잔액 API도 Member-Point 커밋 직후 변경돼야 한다.

### 확인 API

지연 측정 스크립트가 호출하는 Market detail, my prediction, point balance.

### 확인 SQL

각 시점의 balance, point history, Prediction status/updated_at, Market total pool, 옵션 가격.

### 판단 기준

DB와 API 반영 시각 차이, API와 화면 반영 시각 차이를 각각 분리한다. 30초 내 미확정이면 대사 추적 TC-05로 넘긴다.

### 사람이 직접 확인해야 하는 부분

브라우저 Network timing과 서버/DB 시계 차이를 기록한다.

## TC-04. 로그아웃/재로그인 후 포인트/예측 상태 비교

### 목적

인증 전환 시 TanStack Query cache가 이전 회원 데이터를 재사용하는지 확인한다.

### 사전 조건

서로 잔액/예측이 다른 테스트 회원 A/B와 seed 범위 밖 Market.

### 절차

1. A로 로그인해 잔액, 프로필, 내 예측을 열고 Network 요청을 기록한다.
2. 로그아웃 후 B로 로그인해 같은 화면을 즉시 연다.
3. 새 API 요청 전 잠깐이라도 A 데이터가 보이는지, B API가 호출되는지 확인한다.
4. 강제 새로고침 전후를 비교한다.

### 기대 결과

B 로그인 뒤 A의 회원별 데이터가 표시되지 않고 B 기준 API가 다시 호출되어야 한다.

### 확인 API

`GET /api/v1/points/balance`, `GET /api/v1/members/me`, Market my prediction API.

### 확인 SQL

A/B 각각 `check-consistency.sh`로 기준값을 확보한다.

### 판단 기준

DB/API는 B인데 화면만 A면 cache 격리/clear 문제다.

### 사람이 직접 확인해야 하는 부분

Query Devtools의 key/dataUpdatedAt, logout 직후 cache 잔존 여부.

## TC-05. POINT_PENDING / POINT_UNKNOWN 대사 검증

### 목적

불확실 상태의 선별 시점과 Member-Point 결과별 전이를 검증한다.

### 사전 조건

안전한 로컬 장애 주입 환경 또는 이미 존재하는 pending/unknown 행. DB 직접 상태 조작은 금지한다.

### 절차

1. 대상 Prediction의 status, updated_at, spend key를 기록한다.
2. Member-Point transaction 조회 결과를 기록한다.
3. `POINT_PENDING`은 생성 후 3분 전/후, `POINT_UNKNOWN`은 즉시 대상 여부를 비교한다.
4. scheduler 실행 또는 승인된 내부 reconcile API 호출 전후 snapshot을 비교한다.

### 기대 결과

`PROCESSED → CONFIRMED`, `FAILED/NOT_FOUND → FAILED`, 조회 불확실 → `POINT_UNKNOWN`이다. 최근 pending은 선별되지 않는다.

### 확인 API

- `GET /internal/api/v1/points/transactions` (내부 접근 가능한 경우)
- `POST /api/v1/internal/markets/predictions/reconcile?limit=100` (승인된 로컬 환경만)

### 확인 SQL

Prediction status/key/updated_at, 해당 point history, Market/옵션/price history.

### 판단 기준

Member-Point 성공인데 대사 후에도 확정되지 않으면 Market lock/state/데이터 누락 또는 scheduler 미동작을 조사한다.

### 사람이 직접 확인해야 하는 부분

scheduler enabled 설정, 실행 로그, 서버 재시작 시각.

## TC-06. 프론트 캐시 invalidate/refetch 검증

### 목적

예측 성공/불확실 오류 뒤 필요한 query가 실제 refetch되는지 검증한다.

### 사전 조건

Query Devtools와 브라우저 Network 탭을 열고 TC-02 조건을 준비한다.

### 절차

1. Market detail, my prediction, Market list, price history, point balance, profile, point history를 미리 cache한다.
2. 예측 참여를 실행한다.
3. 각 query의 invalidated/fetching 상태와 HTTP 요청을 기록한다.
4. UI 값과 직접 API 응답을 비교한다.

### 기대 결과

현재 코드를 기준으로 Market detail/list/price history와 my prediction/list는 refetch되지만 point balance/history 및 profile은 자동 refetch되지 않을 것으로 예상한다(검증 대상).

### 확인 API

모든 Market/Prediction 조회와 point balance/history, member me.

### 확인 SQL

`check-consistency.sh`로 UI 비교 기준값을 확보한다.

### 판단 기준

DB/API 정상 + stale UI + 관련 Network 요청 없음이면 프론트 cache invalidation 누락으로 판단한다.

### 사람이 직접 확인해야 하는 부분

Query Devtools와 Network 기록, 컴포넌트 mount 여부.

## TC-07. 로그인 응답 지연 후보 측정

### 목적

전체 로그인 시간과 gateway/Member-Point/카카오 구간 후보를 분리한다.

### 사전 조건

유효한 테스트용 카카오 token, 브라우저 Network와 Member-Point 로그 접근 권한.

### 절차

1. gateway 경유 로그인 POST의 DNS/connect/TTFB/total을 3회 기록한다.
2. 가능한 로컬 환경에서 Member-Point 직접 호출과 비교한다.
3. `카카오 API 호출 시작/실패` 로그와 요청 완료 시각을 대조한다.
4. DB connection 지표와 transaction 지속 시간을 확인한다.

### 기대 결과

카카오 응답 대기가 길면 전체 로그인 TTFB가 함께 늘어난다. gateway 추가 지연과 Member-Point 내부 지연을 분리할 수 있어야 한다.

### 확인 API

`POST /api/v1/members/oauth/kakao`, 각 서비스 `/actuator/health`.

### 확인 SQL

읽기 전용으로 기존 회원 조회 시간과 DB connection 상태를 확인한다. 로그인 검증을 위해 데이터를 수정하지 않는다.

### 판단 기준

직접/경유 호출이 모두 느리고 카카오 호출 구간과 일치하면 blocking 외부 API 후보가 강하다. 코드상 timeout 부재는 별도 위험으로 기록한다.

### 사람이 직접 확인해야 하는 부분

유효 token 관리, 카카오/네트워크 상태, 운영 로그의 민감정보 마스킹. 수정은 Member-Point 담당 범위다.
