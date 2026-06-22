# Market Debug Scripts

## 목적

Market 예측과 Member-Point 차감의 DB/API 상태를 읽기 전용으로 수집하고, 예측 POST 뒤 반영 시간을 재현 가능한 Markdown 보고서로 남긴다.

## 주의사항

- `measure-prediction-latency.sh`는 실제 예측 POST를 보내므로 실제 포인트가 차감될 수 있다. `CONFIRM_DEDUCT_POINTS=YES`를 명시해야만 실행되며 승인된 테스트 회원만 사용한다.
- `dev_seed_market_frontend_scenarios.sql`은 데이터 추가 전용이다. `INSERT IGNORE`만 사용하며 재실행해도 기존 Prediction을 삭제하지 않는다.
- `dev_cleanup_market_frontend_scenarios.sql.disabled`는 과거 사고 분석용 비활성 자료다. 실행 파일이나 운영 절차로 사용하지 않는다.
- dev test 범위 Market ID `900001~900099`는 기본 차단된다. add-only seed와 전체 volume reset 정책을 사용하는 로컬 실험 환경에서만 `ALLOW_DEV_TEST_RANGE=true`로 명시 승인할 수 있다.
- 일반 디버깅 중에는 volume 삭제를 실행하지 않는다. 완전 초기화가 명시적으로 필요한 경우에만 아래 전체 MySQL volume reset 절차를 사용한다.
- 테스트 중 DB 삭제·수정·삽입 SQL을 실행하지 않는다.
- 두 스크립트의 DB 접근은 SELECT뿐이다. 조회 실패도 보고서에 기록한다.
- Git Bash/WSL 등 Bash, `docker`, `mysql`이 든 `todongsan-mysql` 컨테이너가 필요하다. 지연 스크립트에는 `curl`도 필요하다.

## 테스트 데이터 생성과 로컬 전체 초기화

- `docs/market/sql/dev_seed_market_frontend_scenarios.sql`: Market 시나리오 데이터 추가만 담당한다. DML은 `INSERT IGNORE`이며 기존 행을 삭제하거나 갱신하지 않는다.
- 테스트 Market은 Admin API 또는 add-only seed로 생성한다. Market 데이터만 부분 삭제하는 SQL은 사용하지 않는다.
- AUTO_INCREMENT를 별도 고정 값으로 보정하지 않는다. 명시적 9000xx seed ID의 영향으로 Admin API 생성 Market도 `9000xx`가 될 수 있으며 로컬 실험 데이터로 허용한다.

### 로컬 MySQL 전체 초기화 절차

> 아래 절차는 명시적인 전체 초기화 작업이다. 이 문서 작성/검증 과정에서는 실행하지 않았다.

저장할 데이터가 없는지 먼저 확인한 뒤 저장소 루트에서 실행한다.

```bash
docker compose down
docker compose -f infra/docker-compose.yml down -v
docker compose -f infra/docker-compose.yml up -d
docker compose up -d
```

이 절차는 MySQL volume을 제거한 뒤 init 스크립트로 `market`, `memberpoint` 등 모든 스키마를 함께 다시 만든다. Market만 부분 삭제하는 방식보다 서비스 간 원장 정합성을 유지하기 쉽다.

전체 초기화의 영향:

- 카카오 로그인 회원, OAuth 저장 정보, 포인트 잔액과 `point_history`가 모두 초기화된다.
- Market, Prediction, Option, PriceHistory와 다른 서비스 스키마 데이터도 모두 사라진다.
- 초기화 후 카카오 재로그인이 필요하다.
- 기존 `memberId`를 재사용한다고 가정하지 말고 DB에서 새 `memberId`와 초기 포인트를 다시 확인한다.
- 초기화 전후 시각과 재검증에 사용할 memberId/marketId를 보고서에 기록한다.

## check-consistency.sh 사용법

환경변수 방식:

```bash
MEMBER_ID=1 MARKET_ID=123 ./scripts/market-debug/check-consistency.sh
```

인자 방식:

```bash
./scripts/market-debug/check-consistency.sh --member-id 1 --market-id 123
```

선택 환경변수와 기본값:

```text
MYSQL_CONTAINER=todongsan-mysql
MYSQL_USER=root
MYSQL_PASSWORD=1234
MARKET_DB=market
MEMBERPOINT_DB=memberpoint
REPORT_DIR=reports/market-debug
```

보고서 첫 부분의 `Environment diagnostics`에는 DB 시각과 Market/Prediction/Member/Point History 전체 건수 및 seed 범위 Market 건수가 기록된다. 이어서 회원 잔액/최근 포인트 이력, 회원의 최근 Prediction 및 해당 Market Prediction, Market/옵션/가격 이력을 저장한다.

진단 SELECT는 값이 0이어도 `COUNT(*)` 한 행을 반환해야 하고 `SELECT NOW()`도 반드시 한 행을 반환한다. 진단까지 전부 `(no rows)`이거나 결과가 없다면 단순히 대상 회원/Market이 없는 상황으로 보지 말고 다음을 확인한다.

- 잘못된 `MYSQL_CONTAINER`, `MYSQL_USER`, `MYSQL_PASSWORD` 또는 Docker context
- 예상과 다른 MySQL 컨테이너/빈 로컬 인스턴스에 연결
- `MARKET_DB`, `MEMBERPOINT_DB` 스키마 이름 오설정 또는 schema 초기화 미완료
- MySQL이 아직 기동 중이거나 권한/연결 오류가 보고서에 함께 기록된 경우
- 스크립트 실행 환경에서 Docker CLI 또는 Docker socket에 접근하지 못한 경우

## measure-prediction-latency.sh 사용법

```bash
CONFIRM_DEDUCT_POINTS=YES \
MEMBER_ID=1 \
MARKET_ID=123 \
OPTION_ID=456 \
POINT_AMOUNT=10 \
API_BASE_URL=http://localhost:8082 \
POINT_BASE_URL=http://localhost:8080 \
./scripts/market-debug/measure-prediction-latency.sh
```

### Market 직접 호출 모드

Market API는 8082로 직접 호출하고 point balance API는 Member-Point의 실제 호스트 노출 포트로 호출한다.

```text
API_BASE_URL=http://localhost:8082
POINT_BASE_URL=http://localhost:8080
```

Docker Compose의 호스트 포트가 다르면 `POINT_BASE_URL`을 실제 매핑에 맞춘다. Market 직접 호출에는 스크립트가 `X-Member-Id`와 `X-Member-Role`을 보낸다. Member-Point의 point balance endpoint는 직접 호출에서도 JWT 인증을 요구할 수 있으므로 `AUTH_TOKEN`이 필요할 수 있다.

### Gateway 경유 모드

Market과 point balance를 모두 Gateway 9000으로 호출한다.

```bash
CONFIRM_DEDUCT_POINTS=YES \
MEMBER_ID=1 MARKET_ID=123 OPTION_ID=456 \
API_BASE_URL=http://localhost:9000 \
POINT_BASE_URL=http://localhost:9000 \
AUTH_TOKEN='test-jwt' \
./scripts/market-debug/measure-prediction-latency.sh
```

Gateway의 인증 정책에 따라 `AUTH_TOKEN`이 필요하다. point balance API가 `401` 또는 `403`이면 API 응답만으로 잔액을 판단하지 말고 같은 시점에 보고서가 수집한 `memberpoint.member.point_balance`와 `point_history.balance_snapshot` DB snapshot을 기준으로 판단한다.

추가 환경변수:

```text
POINT_AMOUNT=10
API_BASE_URL=http://localhost:8082
GATEWAY_BASE_URL=(선택)
POINT_BASE_URL=GATEWAY_BASE_URL 또는 http://localhost:8080
AUTH_TOKEN=(선택, point balance/gateway 인증용 JWT)
MEMBER_ROLE=USER
CONFIRM_DEDUCT_POINTS=YES (필수 실행 승인)
ALLOW_DEV_TEST_RANGE=false (기본값, dev test 범위 차단)
MYSQL_CONTAINER=todongsan-mysql
MYSQL_USER=root
MYSQL_PASSWORD=1234
MARKET_DB=market
MEMBERPOINT_DB=memberpoint
REPORT_DIR=reports/market-debug
```

`GATEWAY_BASE_URL`은 하위 호환 편의 변수이며 `POINT_BASE_URL`을 명시하지 않았을 때 point API 주소로 사용된다. 혼동을 피하려면 위 예시처럼 `API_BASE_URL`과 `POINT_BASE_URL`을 명시하는 것을 권장한다.

### Idempotency-Key 정책 확인 결과

HTTP 요청 키는 임의 고유 값이 아니다. `MarketPredictionService.validateHeaders()`가 정확히 `MARKET_PREDICTION_SPEND:market:{marketId}:member:{memberId}`인지 비교하고 다르면 `VALIDATION_FAILED`를 반환한다. 따라서 스크립트는 현재 고정 형식을 유지한다. `MarketPredictionTransactionService.pointSpendIdempotencyKey()`는 Member-Point에 보낼 내부 키에 `:attempt:{n}`을 추가하므로 HTTP 키와 내부 spend 키는 동일 문자열은 아니지만, HTTP 키가 내부 키의 고정 prefix 역할을 한다.

같은 회원/Market 조합으로 스크립트를 재실행하면 새 HTTP 키를 만들 수 없고, `(market_id, member_id)` Prediction 유일성 및 기존 상태에 따라 이미 참여 오류가 발생하거나 `FAILED` Prediction 재시도로 처리될 수 있다. 재실행으로 새 측정을 만들려면 참여 이력이 없는 다른 테스트 회원 또는 다른 Market을 사용한다. 데이터 삭제로 재실행 조건을 만들지 않는다.

Market 직접 호출에는 `X-Member-Id`, `X-Member-Role`, 위 고정 `Idempotency-Key`를 보낸다. point balance가 인증 실패해도 HTTP 상태와 응답이 보고서에 남고 나머지 측정은 계속된다.

스크립트는 실행 전 조회와 POST 응답을 저장한 뒤 T+0/1/3/5/10/30초에 Market detail, my prediction, point balance 및 간략 DB snapshot을 수집한다. 한 회원은 Market당 Prediction 하나만 허용되므로 참여 이력이 없는 Market을 사용한다.

## 결과 보고서 위치

기본 위치는 `reports/market-debug`다.

- `consistency-member-{memberId}-market-{marketId}-{timestamp}.md`
- `latency-member-{memberId}-market-{marketId}-{timestamp}.md`

민감한 token은 보고서에 출력하지 않지만 API 응답에 개인정보가 있을 수 있으므로 공유 전 검토한다.

## 결과 해석 기준

### 케이스 A

`point_history` 성공 + `market_prediction` 없음

→ 과거 부분 삭제, 전체 volume 초기화 또는 저장 데이터 소실 후보. add-only dev seed 재실행 자체는 기존 Prediction을 삭제하지 않아야 한다.

### 케이스 B

`point_history` 성공 + `market_prediction POINT_PENDING`

→ 차감 성공 뒤 confirm 전 중단/충돌 또는 pending stale 대사 대기 후보. 생성 후 3분과 scheduler 주기를 확인한다.

### 케이스 C

`point_history` 성공 + `market_prediction POINT_UNKNOWN`

→ Member-Point 응답 timeout/외부 오류 후 대사 대기 후보. 같은 멱등성 키의 transaction 결과를 확인한다.

### 케이스 D

`point_history` 성공 + `market_prediction CONFIRMED` + member balance 차감됨

→ 백엔드 정합성은 정상. API도 정상인데 화면만 stale하면 프론트 cache invalidation/refetch 문제 후보다.

### 케이스 E

`point_history` 성공 + `market_prediction CONFIRMED` + member balance 미차감

→ Member-Point 내부 정합성의 심각한 버그 후보. 단, “미차감” 판단 전에 시작 잔액, 이후 적립/환불 이력과 `balance_snapshot`을 함께 확인한다.

추가로 point history가 실패인데 Prediction이 `CONFIRMED`라면 즉시 별도 장애 보고가 필요하다. DB/API/UI 중 어디에서 처음 불일치하는지를 시간순으로 비교한다.
