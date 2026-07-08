# Market Service Docker Run Guide

This document is a preparation guide for running `market-service` as a Docker container.
It does not replace Gateway E2E verification and does not require changing Java business logic or DB schema.

## Files

- `market-service/Dockerfile`
- `market-service/.dockerignore`
- `market-service/.env.local.example`
- `market-service/.env.rds.example`
- `market-service/docker-compose.market-local.yml`
- `market-service/docker-compose.market-rds.yml`

## Dockerfile

- Base image: `gradle:8.14.5-jdk17`
- Runtime image: `eclipse-temurin:17-jre`
- Build method: multi-stage build
- Build command: `./gradlew clean bootJar --no-daemon`
- Jar copy path: `/app/build/libs/*.jar` to `/app/app.jar`
- Exposed port: `8082`

The Dockerfile does not contain DB passwords, RDS endpoints, JWT secrets, or profile-specific secrets.
Runtime configuration must be injected through environment variables.

## Environment Files

Only example files are committed.

```powershell
Copy-Item .env.local.example .env.local
Copy-Item .env.rds.example .env.rds
```

Do not commit `.env.local`, `.env.rds`, or any real secret file.

### Local Mode

Local mode assumes the containerized `market-service` calls services running on the host machine.

```env
SPRING_PROFILES_ACTIVE=prod
DB_HOST=host.docker.internal
DB_PORT=3307
DB_MARKET=market
MEMBER_POINT_SERVICE_URL=http://host.docker.internal:8080
INSIGHT_SERVICE_URL=http://host.docker.internal:8083
```

Use this mode when Docker MySQL, Member-Point, and Insight-Reputation are already running on the host.

### RDS Mode

RDS mode does not include a MySQL container.
`market-service` connects directly to AWS RDS MySQL using values from `.env.rds`.

`market-service/.env.rds.example` contains placeholders only.
Never commit a real RDS endpoint, username, password, or security token.

RDS checklist:

- RDS security group inbound rules must allow the runtime environment.
- For local PC execution, allow the current public IP if the RDS instance is publicly reachable.
- For EC2 execution, allow the EC2 security group in the RDS security group.
- Private subnet RDS may not be reachable directly from a local PC.
- Do not run repeated E2E tests against RDS without checking data impact.
- Do not run `DROP`, `TRUNCATE`, or `DELETE` against RDS as part of container smoke tests.

## Compose Commands

Static config check:

```powershell
docker compose -f docker-compose.market-local.yml config
docker compose -f docker-compose.market-rds.yml config
```

Optional image build check:

```powershell
docker compose -f docker-compose.market-local.yml build
```

This preparation task intentionally does not run:

- `docker compose up`
- Gateway E2E
- Member-Point HTTP E2E
- Insight-Reputation HTTP E2E
- RDS data mutation
- DB migration
- `docker compose down -v`

## Container URL Rules

Docker Compose 안에서 `localhost`는 현재 컨테이너 자신을 의미한다.
다른 컨테이너로 통신할 때는 Docker Compose service name을 사용해야 한다.
host 머신에서 실행 중인 서비스를 호출할 때는 `host.docker.internal`을 사용한다.

Current market-only compose files assume Member-Point and Insight-Reputation are running on the host:

```text
MEMBER_POINT_SERVICE_URL=http://host.docker.internal:8080
INSIGHT_SERVICE_URL=http://host.docker.internal:8083
```

When all services move into one Docker Compose network, change the URLs to service names:

```text
MEMBER_POINT_SERVICE_URL=http://member-point-service:8080
INSIGHT_SERVICE_URL=http://insight-reputation-service:8083
MARKET_SERVICE_URL=http://market-service:8082
```

Gateway owners should use the following URL in a full compose environment:

```text
MARKET_SERVICE_URL=http://market-service:8082
```

## Scheduler Environment

`application.yml` supports Spring Boot relaxed binding.
For prediction reconciliation scheduler override, use:

```env
MARKET_SCHEDULER_PREDICTION_RECONCILIATION_ENABLED=true
```

For reputation update scheduler override, use the existing placeholders:

```env
MARKET_REPUTATION_UPDATE_SCHEDULER_ENABLED=true
MARKET_REPUTATION_UPDATE_SCHEDULER_FIXED_DELAY_MS=5000
MARKET_REPUTATION_UPDATE_SCHEDULER_LIMIT=50
```

## Idempotency-Key Reminder

Public prediction API key:

```text
MARKET_PREDICTION_SPEND:market:{marketId}:member:{memberId}
```

The public key must not include `:attempt:{attemptNo}`.
Attempt suffix keys are generated internally by Market Service when calling Member-Point.

## Safety Notes

- `.env.local` and `.env.rds` must stay untracked.
- `.env.rds.example` must contain placeholders only.
- `docker compose down -v` removes volumes and can delete local DB data. Do not use it casually.
- This document is a preparation guide. Actual container run, RDS smoke test, and Gateway E2E should be done in a separate verification task.
