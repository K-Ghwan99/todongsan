# SQL Migration Policy

## 1. 목적

DB 스키마 변경 이력을 추적하고 신규 개발 환경 구성 및 운영 DB 재구성을 가능하게 하기 위함.

---

## 2. 공통 규칙

* 기존 migration SQL 파일은 수정하지 않는다.
* DB 스키마 변경 시 반드시 새로운 migration SQL 파일을 추가한다.
* DROP TABLE 기반 migration을 금지한다.
* migration SQL은 재실행 가능(idempotent)하게 작성한다.
* migration SQL은 버전 순서대로 적용한다.

---

## 3. 파일명 규칙

형식:

001_xxx.sql
002_xxx.sql
003_xxx.sql

예시:

001_market_init.sql
002_market_prediction.sql
003_market_prediction_attempt_no.sql

---

## 4. 적용 순서

신규 환경 구성 시 낮은 번호부터 순서대로 적용한다.

예시:

001
↓
002
↓
003

---

## 5. 도메인별 관리

각 서비스는 자신의 docs/sql 디렉터리에서 migration SQL을 관리한다.

예시:

market-service/docs/sql
member-point-service/docs/sql
battle-service/docs/sql

다른 도메인의 migration SQL을 수정하지 않는다.

## 6. 주의사항

* 공용 DB 또는 RDS에는 PR merge 후 적용한다.
* 새 migration 번호는 develop 브랜치의 최신 파일 기준으로 정한다.
* 위험한 DDL(DROP COLUMN, 대량 DELETE, 컬럼 타입 변경 등)은 팀 합의 후 진행한다.