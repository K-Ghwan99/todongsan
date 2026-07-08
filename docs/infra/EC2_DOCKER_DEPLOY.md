# EC2 Docker MySQL Deploy Guide

Todongsan current deployment baseline is one EC2 instance running Docker Compose with Docker MySQL.

RDS is not used for the current deployment because of cost. Existing RDS example files may remain as references for a future RDS reintroduction, but they are not part of the current deployment path.

## 1. Deployment Topology

```text
EC2 instance
├─ Docker network: todongsan-network
├─ MySQL container: todongsan-mysql
├─ api-gateway: 9000
├─ member-point-service: 8080
├─ battle-service: 8081
├─ market-service: 8082
└─ insight-reputation-service: 8083
```

Database schemas in Docker MySQL:

```text
Docker MySQL
├─ memberpoint
├─ battle
├─ market
└─ insight
```

The MSA structure remains unchanged. Each application runs as an independent container and communicates by REST URLs. Each service accesses only its own schema.

## 2. Environment Files

Root environment file roles:

```text
.env.example
  - Local integrated Docker Compose placeholder.
  - Copy to .env for local compose execution.

.env.ec2.example
  - EC2 Docker Compose deployment placeholder.
  - Copy to .env on EC2 and fill real values manually.

.env
  - Actual runtime environment file.
  - Must not be committed.
```

Prepare EC2 environment:

```bash
cp .env.ec2.example .env
vi .env
```

Do not put real secrets in example files. Do not commit actual `.env`, `.env.local`, `.env.rds`, or `.env.ec2` files.

## 3. Run Order

Run MySQL first from the infra compose file, then run app containers from the root compose file.

```bash
# 1. Prepare env
cp .env.ec2.example .env

# 2. Start infra MySQL
docker compose -f infra/docker-compose.yml up -d

# 3. Build app images
docker compose build

# 4. Start app containers
docker compose up -d

# 5. Check status and logs
docker compose ps
docker compose logs --tail=100
```

The root `docker-compose.yml` does not include MySQL. It expects `todongsan-mysql` and the external `todongsan-network` created by `infra/docker-compose.yml`.

## 4. Smoke Tests

Run basic checks after startup:

```bash
curl http://localhost:8082/actuator/health
curl http://localhost:8082/api/v1/markets
curl http://localhost:9000/api/v1/markets
curl http://localhost:8083/actuator/health
```

Gateway-routed APIs may require an `Authorization` header because `api-gateway` has a JWT global filter. If a gateway smoke test returns `401`, verify JWT and downstream header injection before treating it as a service failure.

## 5. Stop Commands

Stop application containers:

```bash
docker compose down
```

Stop infra MySQL container while preserving data volume:

```bash
docker compose -f infra/docker-compose.yml down
```

## 6. Forbidden Commands

Do not run these commands on EC2 unless you intentionally want to remove MySQL data and have a verified backup:

```bash
docker compose down -v
docker volume rm
docker volume prune
```

MySQL data is stored in the compose volume key `todongsan_mysql_data`.
With the current `infra/docker-compose.yml` project name, `docker compose config` may render the actual Docker volume name as `infra_todongsan_mysql_data`.
Preserve the actual volume shown by `docker volume ls`. Recreating the `todongsan-mysql` container is acceptable only if this volume is preserved.

## 7. MySQL Backup And Restore

Create a backup directory:

```bash
mkdir -p ~/todongsan-backups
```

Backup all Todongsan schemas:

```bash
docker exec todongsan-mysql \
  mysqldump -uroot -p --databases memberpoint battle market insight \
  > ~/todongsan-backups/todongsan-all.sql
```

Restore from a backup:

```bash
docker exec -i todongsan-mysql \
  mysql -uroot -p < ~/todongsan-backups/todongsan-all.sql
```

Prefer interactive password input instead of writing the password directly in the command. Backup files may contain personal or production data, so do not share them externally. Take a dump backup before demonstrations or risky deployment changes.

## 8. EC2 Security Group And Ports

Recommended external open ports:

```text
22    SSH
9000  API Gateway
```

Do not expose these ports publicly:

```text
3306  MySQL
8080  member-point-service
8081  battle-service
8082  market-service
8083  insight-reputation-service
```

External clients should access only `api-gateway:9000`. Service ports `8080` through `8083` are for Docker network communication. MySQL `3306` must not be opened to the public internet.

## 9. EC2 Size Guidance

Baseline guidance:

```text
Minimum: 2 vCPU / 8 GB RAM
Recommended: 4 vCPU / 16 GB RAM
EBS: minimum 30 GB, recommended 50 GB
```

Instance types are examples only. Actual cost depends on current AWS pricing and usage.

## 10. JVM Memory Options

One EC2 instance runs MySQL plus five Spring Boot services, so JVM memory caps may be useful. Do not apply these blindly. Check `docker stats` after deployment and then decide.

Example compose environment:

```yaml
environment:
  JAVA_TOOL_OPTIONS: "-Xms256m -Xmx768m"
```

Suggested starting points:

```text
api-gateway: -Xms256m -Xmx512m
member-point-service: -Xms256m -Xmx768m
battle-service: -Xms256m -Xmx768m
market-service: -Xms256m -Xmx1024m
insight-reputation-service: -Xms256m -Xmx1024m
```

## 11. Docker Disk Management

Check disk and Docker usage:

```bash
df -h
docker system df
docker stats
```

Safe cleanup candidates:

```bash
docker image prune
docker builder prune
```

Do not use `docker volume prune` on EC2 because it can remove the MySQL data volume.

## 12. RDS Status

Current status:

```text
RDS is not used in the current deployment.
The deployment baseline is EC2 + Docker Compose + Docker MySQL.
Existing .env.rds.example or docker-compose.rds.yml files are retained only as future RDS reference material.
Do not use them for the current EC2 Docker MySQL deployment.
```
