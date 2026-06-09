# API_GATEWAY_SPEC.md — API Gateway 구현 지침

> 작성 기준: API_GATEWAY_TEAM.md, CONVENTION.md, API_SPEC.md, 각 서비스 API 명세
> 대상: 동네대전 MSA 전체 팀원 (Gateway 구현 담당: Member-Point 담당자)

---

## 1. 전체 아키텍처 흐름도

```
외부 클라이언트 (모바일 앱 / 브라우저)
        │
        ▼
┌──────────────────────────────────────────┐
│         API Gateway (:9000)              │
│                                          │
│  1. 공개 경로 → JWT 검증 없이 통과       │
│  2. 보호 경로 → JWT 검증                 │
│              → X-Member-Id 헤더 추가     │
│              → X-Member-Role 헤더 추가   │
│  3. 인식 못한 경로 → 404                 │
└──────────────────────────────────────────┘
        │
        ├── /api/v1/members/**   ──→ member-point-service:8080
        ├── /api/v1/points/**    ──→ member-point-service:8080
        ├── /api/v1/battles/**   ──→ battle-service:8081
        ├── /api/v1/markets/**   ──→ market-service:8082
        └── /api/v1/insights/**  ──→ insight-reputation-service:8083

[서비스 간 직접 호출 — Gateway 통과 안 함 / /internal/** 경로 사용]

battle-service            ──→ member-point-service:8080         POST /internal/api/v1/points/earn
battle-service            ──→ insight-reputation-service:8083   POST /internal/api/v1/reputations/activity
battle-service            ──→ insight-reputation-service:8083   POST /internal/api/v1/insights/battles/{battleId}/report
market-service            ──→ member-point-service:8080         POST /internal/api/v1/points/spend|settlements|refunds
market-service            ──→ insight-reputation-service:8083   POST /internal/api/v1/reputations/prediction
insight-reputation-service ──→ member-point-service:8080        POST /internal/api/v1/members/batch
insight-reputation-service ──→ battle-service:8081              GET  /internal/api/v1/battles/{id}/votes/raw
insight-reputation-service ──→ market-service:8082              GET  /internal/api/v1/markets/{id}/insight-summary
insight-reputation-service ──→ market-service:8082              GET  /internal/api/v1/markets/{id}/insight-predictions

[DB — 모든 서비스가 외부 RDS에 직접 연결]

member-point-service      ──→ RDS (memberpoint DB)
battle-service            ──→ RDS (battle DB)
market-service            ──→ RDS (market DB)
insight-reputation-service ──→ RDS (insight DB)
```

---

## 2. 포트 번호 및 컨테이너 구성

| 컨테이너 이름 | 서비스 | 포트 | 비고 |
|---|---|---|---|
| `gateway` | API Gateway | `9000` | Spring Cloud Gateway (WebFlux) |
| `member-point-service` | Member-Point | `8080` | JWT 발급, 포인트 관리 |
| `battle-service` | Battle | `8081` | Battle 투표/댓글 |
| `market-service` | Market | `8082` | 예측 시장 |
| `insight-reputation-service` | Insight | `8083` | AI 분석, 신뢰도 |
| RDS | MySQL | - | 컨테이너 아님. 외부 AWS RDS |

> **⚠️ 포트 통합 시 주의**
> 현재 각자 개발 중이라 개인 환경의 `application.yml` 포트가 위와 다를 수 있다.
> 통합 배포 시에는 반드시 위 포트 번호를 사용하도록 각자 `server.port`를 수정하는 것을 권장한다.

---

## 3. JWT 검증 흐름

```
요청 도착 (Gateway :9000)
    │
    ▼
공개 경로인가?  ──YES──→ JWT 검증 없이 하위 서비스로 라우팅
    │
    NO
    │
    ▼
Authorization: Bearer {token} 헤더 있는가?
    │
    NO ──→ 401 UNAUTHORIZED 반환 (하위 서비스 미도달)
    │
    YES
    │
    ▼
JWT 서명 검증 (Member-Point와 동일한 JWT_SECRET 사용)
    │
    ├── 만료 ──→ 401 UNAUTHORIZED
    ├── 위조 ──→ 401 UNAUTHORIZED
    │
    └── 유효 ──→ memberId, role 추출
                    │
                    ▼
              클라이언트가 보낸 X-Member-Id, X-Member-Role 헤더 제거 (스푸핑 차단)
                    │
                    ▼
              X-Member-Id: {memberId}    헤더 추가 (JWT claim 기준)
              X-Member-Role: {role}      헤더 추가 (JWT claim 기준)
                    │
                    ▼
              하위 서비스로 라우팅
```

> **[보안 정책]** 클라이언트가 임의로 보낸 `X-Member-Id`, `X-Member-Role` 헤더는 반드시 제거 후
> JWT claim 기준으로 덮어써야 한다. 클라이언트가 `X-Member-Role: ADMIN`을 임의로 설정하는
> 스푸핑 공격을 차단한다.
> `Idempotency-Key`는 건드리지 않고 원본 그대로 downstream 전달한다.

**Gateway GlobalFilter 구현 핵심:**
```java
// Spring Cloud Gateway는 WebFlux 기반 → GlobalFilter 사용 (OncePerRequestFilter 아님)
public class JwtAuthenticationFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 공개 경로 체크 → JWT 검증 → 헤더 추가 → chain.filter(exchange)
    }
}
```

---

## 4. 경로별 라우팅 규칙

### 4-1. 공개 경로 (JWT 검증 없이 통과)

| 경로 | 메서드 | 설명 |
|---|---|---|
| `/api/v1/members/oauth/kakao` | POST | 카카오 OAuth 로그인 |
| `/api/v1/members/token/refresh` | POST | Access Token 재발급 |

### 4-2. 보호 경로 (JWT 검증 필수 → X-Member-Id 헤더 추가)

공개 경로와 내부 API 외 모든 경로. 예시:

| 경로 | 메서드 | 설명 |
|---|---|---|
| `/api/v1/members/me` | GET | 내 정보 조회 |
| `/api/v1/members/me` | PATCH | 내 정보 수정 |
| `/api/v1/members/me` | DELETE | 회원 탈퇴 |
| `/api/v1/members/logout` | POST | 로그아웃 |
| `/api/v1/points/balance` | GET | 포인트 잔액 조회 |
| `/api/v1/points/history` | GET | 포인트 내역 조회 |
| `/api/v1/battles/**` | 전체 | Battle 관련 외부 API |
| `/api/v1/admin/markets/**` | 전체 | Market 관리자 API (Market 팀 요청) |
| `/api/v1/markets/**` | 전체 | Market 관련 외부 API |
| `/api/v1/insights/**` | 전체 | Insight 관련 외부 API |

> ⚠️ `/api/v1/admin/markets/**`는 `/api/v1/markets/**`보다 **먼저** 등록해야 한다.
> Spring Cloud Gateway는 등록 순서대로 매칭하므로 더 구체적인 경로를 위에 둔다.

### 4-3. 내부 연계 API (Gateway를 통하지 않음)

서비스 간 직접 호출. JWT 없이 내부 포트로 직접 호출한다.
**각 서비스는 이 경로들을 `permitAll`로 설정해야 한다.**

> **[내부 API 경로 전략]** 내부 연계 API는 `/internal/` 접두사를 붙인다.
> Gateway 라우팅 규칙에 `/internal/**` 경로가 없으므로, 외부에서 Gateway를 통해 이 경로에
> 접근하면 자동으로 404가 반환된다.
> 별도 인증 차단 없이도 외부 접근이 구조적으로 막힌다.

| 경로 | 메서드 | 호출 방향 | 설명 |
|---|---|---|---|
| `/internal/api/v1/members/batch` | POST | Insight → Member-Point | 회원 배치 조회 |
| `/internal/api/v1/points/earn` | POST | Battle → Member-Point | 포인트 적립 |
| `/internal/api/v1/points/spend` | POST | Market/Insight → Member-Point | 포인트 차감 |
| `/internal/api/v1/points/settlements` | POST | Market → Member-Point | 포인트 정산 |
| `/internal/api/v1/points/refunds` | POST | Market/Insight → Member-Point | 포인트 환불 |
| `/internal/api/v1/points/transactions` | GET | Market → Member-Point | 거래 상태 조회 |
| `/internal/api/v1/reputations/activity` | POST | Battle → Insight | 활동 점수 업데이트 |
| `/internal/api/v1/insights/battles/{battleId}/report` | POST | Battle → Insight | Battle 종료 AI 분석 트리거 |
| `/internal/api/v1/reputations/prediction` | POST | Market → Insight | 예측 정확도 업데이트 |
| `/internal/api/v1/battles/{battleId}/votes/raw` | GET | Insight → Battle | 투표 원본 데이터 조회 |
| `/internal/api/v1/markets/{marketId}/insight-summary` | GET | Insight → Market | 예측/정산 요약 조회 |
| `/internal/api/v1/markets/{marketId}/insight-predictions` | GET | Insight → Market | 예측 상세 조회 |

---

## 5. 서비스 간 통신 방법 (컨테이너 이름 사용)

Docker 네트워크 내에서는 컨테이너 이름으로 직접 접근한다.
`localhost`나 IP 대신 서비스 이름을 사용해야 한다.

| 접근 대상 | Docker 내부 URL | 로컬 개발 URL |
|---|---|---|
| Member-Point | `http://member-point-service:8080` | `http://localhost:8080` |
| Battle | `http://battle-service:8081` | `http://localhost:8081` |
| Market | `http://market-service:8082` | `http://localhost:8082` |
| Insight | `http://insight-reputation-service:8083` | `http://localhost:8083` |

**각 서비스 application.yml에서 환경변수로 분리해서 관리한다:**

```yaml
# 예시: battle-service가 member-point를 호출할 때
client:
  member-point:
    base-url: ${MEMBER_POINT_SERVICE_URL:http://localhost:8080}
```

Docker 환경에서는 `MEMBER_POINT_SERVICE_URL=http://member-point-service:8080` 환경변수를 주입한다.

---

## 6. JWT Secret Key 공유 방법

- JWT Secret은 **Gateway와 Member-Point Service만 공유**한다.
- Battle, Market, Insight는 JWT를 직접 파싱하지 않으므로 Secret 불필요.
- 코드에 하드코딩 금지. 반드시 환경변수로 관리한다.

```
JWT_SECRET=your-jwt-secret-must-be-at-least-32-characters
```

```yaml
# gateway/application.yml
jwt:
  secret: ${JWT_SECRET}

# member-point-service/application.yml
jwt:
  secret: ${JWT_SECRET}
  access-token-expiry: 21600000     # 6시간 (ms)
  refresh-token-expiry: 5184000000  # 60일 (ms)
```

**두 서비스에 동일한 `JWT_SECRET` 환경변수가 주입되어야 한다.**

---

## 7. application.yml 예시 (Gateway)

```yaml
spring:
  application:
    name: gateway-service
  cloud:
    gateway:
      routes:
        - id: member-point
          uri: http://member-point-service:8080
          predicates:
            - Path=/api/v1/members/**, /api/v1/points/**

        - id: battle
          uri: http://battle-service:8081
          predicates:
            - Path=/api/v1/battles/**

        - id: market
          uri: http://market-service:8082
          predicates:
            - Path=/api/v1/markets/**

        - id: insight
          uri: http://insight-reputation-service:8083
          predicates:
            - Path=/api/v1/insights/**

server:
  port: 9000

jwt:
  secret: ${JWT_SECRET}

management:
  endpoints:
    web:
      exposure:
        include: health
```

---

## 8. docker-compose.yml 예시 (RDS 연결 포함)

```yaml
version: '3.8'

networks:
  todongsan-network:
    driver: bridge

services:

  gateway:
    build: ./api-gateway
    container_name: gateway
    ports:
      - "9000:9000"
    environment:
      JWT_SECRET: ${JWT_SECRET}
    networks:
      - todongsan-network
    depends_on:
      - member-point-service
      - battle-service
      - market-service
      - insight-reputation-service

  member-point-service:
    build: ./member-point-service
    container_name: member-point-service
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:mysql://${RDS_HOST}:3306/memberpoint?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      CRYPTO_AES_KEY: ${CRYPTO_AES_KEY}
    networks:
      - todongsan-network

  battle-service:
    build: ./battle-service
    container_name: battle-service
    ports:
      - "8081:8081"
    environment:
      DB_URL: jdbc:mysql://${RDS_HOST}:3306/battle?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      MEMBER_POINT_SERVICE_URL: http://member-point-service:8080
      INSIGHT_SERVICE_URL: http://insight-reputation-service:8083
    networks:
      - todongsan-network

  market-service:
    build: ./market-service
    container_name: market-service
    ports:
      - "8082:8082"
    environment:
      DB_URL: jdbc:mysql://${RDS_HOST}:3306/market?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      MEMBER_POINT_SERVICE_URL: http://member-point-service:8080
      INSIGHT_SERVICE_URL: http://insight-reputation-service:8083
    networks:
      - todongsan-network

  insight-reputation-service:
    build: ./insight-reputation
    container_name: insight-reputation-service
    ports:
      - "8083:8083"
    environment:
      DB_URL: jdbc:mysql://${RDS_HOST}:3306/insight?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      BATTLE_SERVICE_BASE_URL: http://battle-service:8081
      MEMBER_POINT_SERVICE_BASE_URL: http://member-point-service:8080
      MARKET_SERVICE_BASE_URL: http://market-service:8082
      CLAUDE_API_KEY: ${CLAUDE_API_KEY}
      CLAUDE_API_URL: ${CLAUDE_API_URL}
      CLAUDE_MODEL: ${CLAUDE_MODEL}
      REB_API_KEY: ${REB_API_KEY}
      REB_API_BASE_URL: ${REB_API_BASE_URL}
    networks:
      - todongsan-network

# DB는 컨테이너 아님 — 외부 RDS 사용
# RDS_HOST 환경변수로 주입
```

**.env 파일 예시 (Git 커밋 금지):**

```
RDS_HOST=your-rds-endpoint.ap-northeast-2.rds.amazonaws.com
DB_USERNAME=admin
DB_PASSWORD=your-db-password
JWT_SECRET=your-jwt-secret-must-be-at-least-32-characters
CRYPTO_AES_KEY=your-aes-key-must-be-exactly-32chars
CLAUDE_API_KEY=your-claude-api-key
CLAUDE_API_URL=https://api.anthropic.com/v1/messages
CLAUDE_MODEL=claude-sonnet-4-6
REB_API_KEY=your-reb-api-key
REB_API_BASE_URL=https://...
```

---

## 9. 팀원별 체크리스트

> 현재 각자 개발 중이라 개인 환경의 포트 번호가 위 통합 포트와 다를 수 있다.
> **통합 배포 시** 아래 지정된 포트를 사용하는 것을 권장한다.

### 공통 (모든 팀원)

```
[ ] server.port를 통합 포트 번호로 수정 (아래 각 서비스 참고)
[ ] JWT 직접 파싱 코드 없음 (Gateway/Member-Point만 JWT 라이브러리 사용)
[ ] X-Member-Id, X-Member-Role 헤더로 인증 처리
[ ] 서비스 간 호출 URL을 환경변수로 분리 (하드코딩 금지)
[ ] 내부 API 경로 permitAll 설정 확인
[ ] 외부 테스트는 Gateway 포트(9000) 사용
[ ] .env 파일 .gitignore에 등록 확인
```

### Gateway 담당 (Member-Point 담당자)

```
[ ] feature/gateway 브랜치 생성
[ ] Spring Boot 프로젝트 생성 (spring-cloud-starter-gateway 의존성)
      ⚠️ spring-boot-starter-web과 함께 쓰면 충돌 — 사용 금지
[ ] JwtAuthenticationFilter (GlobalFilter) 구현
      ⚠️ OncePerRequestFilter 아님. Mono<Void> 반환
[ ] 공개 경로 목록 누락 없는지 확인 (섹션 4-1)
[ ] application.yml 라우팅 설정 (4개 서비스)
[ ] server.port: 9000
[ ] JWT_SECRET 환경변수 → Member-Point와 동일한 값 사용
```

### Member-Point 담당자

```
[ ] server.port: 8080 (현재 일치)
[ ] 내부 연계 API 컨트롤러 경로 앞에 /internal 추가
      @RequestMapping: /internal/api/v1/members/batch
      @RequestMapping: /internal/api/v1/points/earn, /spend, /settlements, /refunds, /transactions
[ ] SecurityConfig에서 /internal/** permitAll 설정
[ ] DB_URL → RDS 엔드포인트로 변경
```

### Battle 담당자

```
[ ] server.port: 8081 권장
      (현재 application.yml: 8082 → 통합 시 8081로 변경 권장)
[ ] 내부 연계 API 컨트롤러 경로 앞에 /internal 추가
      @RequestMapping: /internal/api/v1/battles/{battleId}/votes/raw
[ ] SecurityConfig에서 /internal/** permitAll 설정
[ ] 서비스 간 호출 URL 환경변수 분리
      MEMBER_POINT_SERVICE_URL, INSIGHT_SERVICE_URL
[ ] Battle 종료(CLOSED) 시 Insight AI 분석 트리거 호출 구현
      POST /internal/api/v1/insights/battles/{battleId}/report (BattleCloseScheduler)
[ ] DB_URL → RDS 엔드포인트로 변경
```

### Market 담당자

```
[ ] server.port: 8082 권장
      (현재 application.yml: 8081 → 통합 시 8082로 변경 권장)
[ ] 내부 연계 API 컨트롤러 경로 앞에 /internal 추가
      @RequestMapping: /internal/api/v1/markets/{marketId}/insight-summary
      @RequestMapping: /internal/api/v1/markets/{marketId}/insight-predictions
[ ] SecurityConfig에서 /internal/** permitAll 설정
[ ] 서비스 간 호출 URL 환경변수 분리
      MEMBER_POINT_SERVICE_URL, INSIGHT_SERVICE_URL
[ ] DB_URL → RDS 엔드포인트로 변경
```

### Insight 담당자

```
[ ] server.port: 8083 권장
      (현재 application.yml: 8084 → 통합 시 8083으로 변경 권장)
[ ] 내부 연계 API 컨트롤러 경로 앞에 /internal 추가
      @RequestMapping: /internal/api/v1/reputations/activity
      @RequestMapping: /internal/api/v1/reputations/prediction
[ ] SecurityConfig에서 /internal/** permitAll 설정
[ ] 서비스 간 호출 URL 환경변수 분리
      BATTLE_SERVICE_BASE_URL, MEMBER_POINT_SERVICE_BASE_URL (현재 적용 중)
[ ] DB_URL → RDS 엔드포인트로 변경 (현재 환경변수 사용 중)
```

---

## 10. 서비스 간 호출 흐름 요약

### Battle 투표 완료
```
Client → Gateway(:9000) → battle-service(:8081)
battle-service → member-point-service(:8080)         POST /internal/api/v1/points/earn
battle-service → insight-reputation-service(:8083)   POST /internal/api/v1/reputations/activity
```

### Battle 종료
```
battle-service → insight-reputation-service(:8083)   POST /internal/api/v1/insights/battles/{battleId}/report
```

### Market 예측 참여
```
Client → Gateway(:9000) → market-service(:8082)
market-service → member-point-service(:8080)          POST /internal/api/v1/points/spend
```

### Market 정산
```
market-service → member-point-service(:8080)          POST /internal/api/v1/points/settlements
market-service → insight-reputation-service(:8083)    POST /internal/api/v1/reputations/prediction
```

### Insight AI 분석
```
insight-reputation-service → battle-service(:8081)          GET  /internal/api/v1/battles/{id}/votes/raw
insight-reputation-service → market-service(:8082)           GET  /internal/api/v1/markets/{id}/insight-summary
insight-reputation-service → market-service(:8082)           GET  /internal/api/v1/markets/{id}/insight-predictions
insight-reputation-service → member-point-service(:8080)     POST /internal/api/v1/members/batch
```
