# Todongsan Local Infrastructure Guide

## 1. 목적

본 문서는 Todongsan 프로젝트의 로컬 개발 환경 구성을 설명한다.

모든 개발자는 동일한 Docker Compose 설정을 사용하여 로컬 MySQL 환경을 구축한다.

---

## 2. 사전 준비

필수 설치 항목

- Git
- Docker Desktop
- JDK 17
- IntelliJ IDEA

Docker Desktop이 정상 실행 중인지 확인한다.

```bash
docker --version
docker compose version
```

---

## 3. 프로젝트 구조

```text
todongsan
│
├── infra
│   ├── docker-compose.yml
│   └── mysql
│       └── init
│           ├── 00-create-databases.sql
│           ├── 01-market-schema.sql
│           ├── 02-memberpoint-schema.sql
│           ├── 03-battle-schema.sql
│           └── 04-insight-schema.sql
│
├── market-service
├── member-point-service
├── battle-service
└── insight-reputation-service
```

---

## 4. Docker MySQL 실행

infra 디렉토리로 이동한다.

```bash
cd infra
```

Docker Compose 실행

```bash
docker compose up -d
```

실행 확인

```bash
docker ps
```

정상 실행 시 다음 컨테이너가 표시된다.

```text
todongsan-mysql
```

---

## 5. Docker MySQL 종료

```bash
cd infra

docker compose down
```

---

## 6. Docker MySQL 데이터 초기화 주의

EC2와 통합 로컬 검증 환경에서 MySQL 데이터는 Docker volume에 저장된다.

```text
compose volume key: todongsan_mysql_data
docker compose config output: infra_todongsan_mysql_data
```

일반 종료 또는 재기동 시에는 volume을 보존해야 한다.

```bash
cd infra
docker compose down
docker compose up -d
```

### 주의

아래 명령은 MySQL 데이터 volume 삭제 위험이 있으므로 일반 개발/배포 환경에서 사용하지 않는다.

```bash
docker compose down -v
docker volume rm
docker volume prune
```

스키마를 처음부터 다시 만들 필요가 있는 개발 초기 상황이라도, 먼저 백업 필요 여부를 확인한다.

---

## 7. MySQL 접속

컨테이너 접속

```bash
docker exec -it todongsan-mysql bash
```

MySQL 접속

```bash
mysql -u root -p
```

비밀번호

```text
1234
```

---

## 8. 생성되는 Database

초기 실행 시 다음 Database가 자동 생성된다.

```text
memberpoint
battle
market
insight
```

확인

```sql
SHOW DATABASES;
```

---

## 9. SQL 초기화 스크립트

초기 SQL 파일 위치

```text
infra/mysql/init
```

예시

```text
00-create-databases.sql
01-market-schema.sql
02-memberpoint-schema.sql
03-battle-schema.sql
04-insight-schema.sql
```

### 주의

docker-entrypoint-initdb.d 스크립트는 MySQL 컨테이너 최초 생성 시에만 실행된다.

SQL 수정 후 재반영하려면

```bash
docker compose down -v
docker compose up -d
```

를 수행해야 한다.

---

## 10. Market Service 로컬 실행

### application-local.yml

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3307/market?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: root
    password: 1234
```

### 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

또는 IntelliJ Run Configuration

```text
SPRING_PROFILES_ACTIVE=local
```

---

## 11. RDS 사용 정책

현재 비용 문제로 RDS는 사용하지 않는다.

현재 배포 기준은 다음과 같다.

```text
EC2 1대 + Docker Compose + Docker MySQL
```

기존 `.env.rds.example` 또는 `docker-compose.rds.yml` 파일은 향후 RDS 재도입 시 참고용으로 남겨둘 수 있다. 이번 EC2 Docker MySQL 배포에서는 사용하지 않는다.

---

## 12. 환경 변수 관리

다음 정보는 Git에 커밋하지 않는다.

```text
DB_HOST
DB_PORT
DB_USERNAME
DB_PASSWORD
```

권장 관리 방법

- .env
- GitHub Secret
- AWS Parameter Store

---

## 13. Troubleshooting

### 포트 충돌 확인

Windows

```bash
netstat -ano | findstr 3307
```

Mac / Linux

```bash
lsof -i :3307
```

### 컨테이너 상태 확인

```bash
docker ps
```

### 컨테이너 로그 확인

```bash
docker logs todongsan-mysql
```

### MySQL 접속 확인

```bash
docker exec -it todongsan-mysql mysql -u root -p
```

---

## 14. 개발 원칙

1. 모든 개발자는 동일한 Docker Compose를 사용한다.
2. Database 생성 및 변경은 SQL 스크립트로 관리한다.
3. 스키마 변경은 SQL 파일 수정 후 Pull Request로 공유한다.
4. 로컬 개발은 Docker MySQL을 사용한다.
5. AWS RDS는 통합 테스트 및 운영 검증 용도로 사용한다.
6. 환경 변수(.env)는 Git에 커밋하지 않는다.

---

## 15. Local MSA Docker Compose

Local integrated Docker execution uses the infra compose plus the root app compose.

```text
infra/docker-compose.yml
  - MySQL only
  - creates todongsan-network
  - runs todongsan-mysql

docker-compose.yml
  - app services only
  - api-gateway
  - member-point-service
  - battle-service
  - market-service
  - insight-reputation-service
  - uses external todongsan-network
```

`docker-compose.local.yml` is no longer used. The standard local integrated app compose file is root `docker-compose.yml`.

Run order:

```bash
cp .env.example .env
docker compose -f infra/docker-compose.yml up -d
docker network inspect todongsan-network
docker compose config
docker compose build
docker compose up -d
docker compose ps
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Root Docker Compose uses `.env` for variable substitution. The `.env` file is local-only and must not be committed.

Step-by-step startup:

```bash
docker compose -f infra/docker-compose.yml up -d
docker network inspect todongsan-network
docker ps

docker compose up -d member-point-service market-service
docker logs --tail=100 member-point-service
docker logs --tail=100 market-service
curl http://localhost:8082/actuator/health
curl http://localhost:8082/api/v1/markets

docker compose up -d --no-deps api-gateway
docker logs --tail=100 api-gateway
curl http://localhost:9000/api/v1/markets

docker compose up -d battle-service insight-reputation-service
docker logs --tail=100 battle-service
docker logs --tail=100 insight-reputation-service
docker compose ps
```

Smoke test examples:

```bash
curl http://localhost:9000/api/v1/markets
curl http://localhost:8082/actuator/health
curl http://localhost:8082/api/v1/markets
```

Network rules:

```text
Inside Docker Compose, localhost means the current container itself.
Container-to-container calls must use Docker Compose service names.
DB host inside app containers is todongsan-mysql.
Use localhost:3307 only from the host PC when connecting to MySQL.
```

Service URL examples for integrated compose:

```text
MEMBER_POINT_SERVICE_URL=http://member-point-service:8080
BATTLE_SERVICE_URL=http://battle-service:8081
MARKET_SERVICE_URL=http://market-service:8082
INSIGHT_SERVICE_URL=http://insight-reputation-service:8083

MEMBER_POINT_SERVICE_BASE_URL=http://member-point-service:8080
BATTLE_SERVICE_BASE_URL=http://battle-service:8081
MARKET_SERVICE_BASE_URL=http://market-service:8082
```

Local environment files:

Linux/macOS/Git Bash:

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Actual `.env`, `.env.local`, and `.env.rds` files are local-only files and must not be committed.
Use `.env.example` as the shared placeholder template for root `docker-compose.yml`.
If you test external API features, fill local values such as `REB_API_KEY` and `CLAUDE_API_KEY` in `.env`.
Simple routing smoke tests can use placeholder values.

Environment file roles:

```text
root .env
  - Used only for root docker-compose.yml integrated local app execution.
  - Create it from .env.example and fill local values when needed.
  - Do not commit.

service .env.local
  - Used for running one service independently.
  - Kept separate from root .env.
  - Do not delete and do not commit.

service .env.rds
  - Currently unused for EC2 Docker MySQL deployment.
  - Retained only as future RDS reference material.
  - Do not merge into root .env for local integrated execution.
  - Do not delete and do not commit.
```

JPA local policy:

```text
battle-service and insight-reputation-service are JPA-based services.
In local Docker MySQL integration tests, ddl-auto=update may create or adjust their schema.
This is allowed only for local integration testing.
EC2 Docker MySQL deployment ddl-auto policy must be reviewed before deployment.
```

Forbidden commands for normal local verification and EC2 Docker MySQL deployment:

```bash
docker compose down -v
docker volume rm
DROP DATABASE
TRUNCATE
DELETE
```

Use plain `docker compose down` only when containers need to be stopped without deleting volumes.

## 16. EC2 Docker MySQL 배포 안내

현재 EC2 배포 기준은 RDS가 아니라 Docker MySQL이다.

자세한 절차는 다음 문서를 따른다.

```text
docs/infra/EC2_DOCKER_DEPLOY.md
```

핵심 원칙:

```text
1. infra/docker-compose.yml로 todongsan-mysql을 먼저 실행한다.
2. root docker-compose.yml로 app service 5개를 실행한다.
3. root .env는 .env.ec2.example을 복사해 EC2에서 수동 작성한다.
4. 외부 진입점은 api-gateway:9000으로 제한한다.
5. MySQL 3306과 내부 서비스 포트 8080~8083은 외부에 공개하지 않는다.
6. docker compose down -v와 docker volume rm/prune은 사용하지 않는다.
7. 시연 또는 위험 작업 전 mysqldump 백업을 남긴다.
```
