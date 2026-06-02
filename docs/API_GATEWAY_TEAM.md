# API Gateway 팀 협업 문서

> 작성일: 2026-06-02
> 대상: 동네대전 MSA 전체 팀원 (Member-Point, Battle, Market, Insight 담당자)

---

## 1. 전체 아키텍처 흐름

```
외부 클라이언트 (모바일 앱 / 브라우저)
        │
        ▼
 ┌─────────────────────────────────────┐
 │       API Gateway (:9000)           │
 │   JWT 검증 → X-Member-Id 헤더 추가  │
 └─────────────────────────────────────┘
        │
        ├── /api/v1/members/**, /api/v1/points/** ──→ Member-Point Service (:8080)
        ├── /api/v1/battles/**                    ──→ Battle Service (:8081)
        ├── /api/v1/markets/**                    ──→ Market Service (:8082)
        └── /api/v1/insights/**                   ──→ Insight Service (:8083)
```

**핵심 원칙:**
- 외부 요청은 반드시 Gateway를 통해서만 내부 서비스에 도달한다.
- Gateway는 JWT를 검증하고, 검증된 회원 정보를 헤더로 전달한다.
- 내부 서비스는 JWT를 직접 파싱하지 않는다. `X-Member-Id`, `X-Member-Role` 헤더를 신뢰하여 사용한다.

---

## 2. 포트 번호 (확정)

| 역할 | 서비스 | 포트 |
|---|---|---|
| 외부 진입점 | API Gateway | `9000` |
| 회원/포인트 | Member-Point Service | `8080` |
| 배틀 | Battle Service | `8081` |
| 마켓 (예측) | Market Service | `8082` |
| 인사이트 (AI) | Insight Service | `8083` |

---

## 3. 각 서비스 담당자별 해야 할 일

### Gateway 담당 (Member-Point 담당자)

- `feature/gateway` 브랜치에서 별도 Spring Boot 프로젝트 생성
- `spring-cloud-starter-gateway` 의존성 추가 (WebFlux 기반)
- `JwtAuthenticationFilter` (GlobalFilter) 구현
- `application.yml`에 각 서비스 라우팅 설정 작성
- 공개 경로 / 보호 경로 구분 목록 관리 (4절 참고)

### 모든 서비스 담당자 공통

1. **`X-Member-Id` 헤더로 인증 처리**
   - JWT를 직접 파싱하지 않는다.
   - Controller에서 `@RequestHeader("X-Member-Id") Long memberId` 또는 Spring Security의 `X-Member-Id` 기반 principal로 수신한다.

2. **내부 API는 Gateway를 통하지 않는다**
   - 서비스 간 직접 REST 호출 (예: Market → Member-Point)은 내부 포트로 직접 호출한다.
   - 내부 API는 JWT 없이 호출되므로, 수신 측 서비스는 해당 API를 `permitAll`로 열어두어야 한다.

3. **서버 포트 설정 확인**
   - 각자 `application.yml`의 `server.port`가 위 포트 번호와 일치하는지 확인한다.

---

## 4. Gateway가 처리하는 경로 구분

### 공개 경로 (JWT 검증 없이 통과)

| 경로 | 설명 |
|---|---|
| `POST /api/v1/members/oauth/kakao` | 카카오 OAuth 로그인 |
| `POST /api/v1/members/token/refresh` | Access Token 재발급 |

### 내부 API 경로 (서비스 간 직접 호출 — Gateway 통과 안 함)

| 경로 | 호출 방향 | 설명 |
|---|---|---|
| `POST /api/v1/members/batch` | Insight → Member-Point | 회원 정보 배치 조회 |
| `POST /api/v1/points/earn` | Battle → Member-Point | 포인트 적립 |
| `POST /api/v1/points/spend` | Market/Insight → Member-Point | 포인트 차감 |
| `POST /api/v1/points/settlements` | Market → Member-Point | 포인트 정산 |
| `POST /api/v1/points/refunds` | Market/Insight → Member-Point | 포인트 환불 |
| `GET /api/v1/points/transactions` | Market → Member-Point | 거래 상태 조회 |

> 위 경로들은 Gateway를 통하지 않으므로 `X-Member-Id` 헤더가 없다.
> Member-Point는 이 경로들을 `permitAll`로 설정하고, 서비스 레이어에서 별도 검증한다.

### 보호 경로 (JWT 검증 필수)

위 두 분류(공개 경로 / 내부 API)에 해당하지 않는 모든 경로. 예시:
- `GET /api/v1/members/me`
- `PATCH /api/v1/members/me`
- `DELETE /api/v1/members/me`
- `POST /api/v1/members/logout`
- `GET /api/v1/points/balance`
- `GET /api/v1/points/history`
- `/api/v1/battles/**` (GET, POST 등 모든 메서드)
- `/api/v1/markets/**` (GET, POST 등 모든 메서드)
- `/api/v1/insights/**` (GET, POST 등 모든 메서드)

---

## 5. Gateway가 추가하는 헤더

JWT 검증 성공 시 Gateway는 아래 헤더를 내부 서비스로 전달한다.

| 헤더 이름 | 타입 | 예시 | 설명 |
|---|---|---|---|
| `X-Member-Id` | `Long` | `"42"` | JWT subject에서 추출한 회원 ID |
| `X-Member-Role` | `String` | `"USER"` | JWT claim에서 추출한 역할 |

**각 서비스에서 사용하는 방법:**

```java
// Controller에서 직접 수신
@GetMapping("/api/v1/battles/{id}")
public ApiResponse<?> getBattle(
    @RequestHeader("X-Member-Id") Long memberId,
    @PathVariable Long id
) { ... }
```

또는 Spring Security `AuthenticationPrincipal`로 연동 (Member-Point 방식 참고).

---

## 6. JWT 시크릿 공유

- JWT 시크릿 키는 **Member-Point Service와 Gateway가 동일한 값을 사용**한다.
- 다른 서비스는 JWT를 직접 파싱하지 않으므로 시크릿 키가 필요 없다.
- 시크릿 키는 코드에 하드코딩하지 않는다. `application.yml`의 환경변수로 관리한다.

```yaml
# Member-Point & Gateway 공통
jwt:
  secret: ${JWT_SECRET}
  access-token-expiry-ms: 21600000    # 6시간
  refresh-token-expiry-ms: 5184000000 # 60일
```

---

## 7. 로컬 테스트 방법

### 서비스 기동 순서

```
1. Docker MySQL 기동 (포트 3307)
   cd infra && docker-compose up -d

2. 각 서비스 기동 (순서 무관)
   Member-Point: ./gradlew bootRun  → :8080
   Battle:       ./gradlew bootRun  → :8081
   Market:       ./gradlew bootRun  → :8082
   Insight:      ./gradlew bootRun  → :8083
   Gateway:      ./gradlew bootRun  → :9000

3. 외부 요청은 반드시 Gateway 포트(9000)로 보낸다
```

### 헬스 체크

```bash
# Gateway
curl http://localhost:9000/actuator/health

# 각 서비스 직접 확인
curl http://localhost:8080/actuator/health   # Member-Point
curl http://localhost:8081/actuator/health   # Battle
curl http://localhost:8082/actuator/health   # Market
curl http://localhost:8083/actuator/health   # Insight
```

### API 호출 예시

```bash
# 카카오 로그인 (공개 경로 — JWT 불필요)
curl -X POST http://localhost:9000/api/v1/members/oauth/kakao \
  -H "Content-Type: application/json" \
  -d '{"accessToken": "kakao_access_token_here"}'

# 내 정보 조회 (보호 경로 — JWT 필요)
curl http://localhost:9000/api/v1/members/me \
  -H "Authorization: Bearer {jwt_access_token}"

# 서비스 간 내부 호출 (Gateway 통하지 않고 직접 호출)
curl -X POST http://localhost:8080/api/v1/points/earn \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: {idempotency_key}" \
  -d '{ "memberId": 1, "amount": 10, "type": "EARN_VOTE", ... }'
```

---

## 8. Swagger (각 서비스별)

각 서비스는 자체 Swagger UI를 노출한다. **Gateway를 통하지 않고 직접 서비스 포트로 접근**한다.

| 서비스 | Swagger UI URL |
|---|---|
| Member-Point | `http://localhost:8080/swagger-ui.html` |
| Battle | `http://localhost:8081/swagger-ui.html` |
| Market | `http://localhost:8082/swagger-ui.html` |
| Insight | `http://localhost:8083/swagger-ui.html` |

---

## 9. 체크리스트 (각 서비스 담당자)

```
[ ] server.port 확인 (위 포트 번호와 일치)
[ ] X-Member-Id, X-Member-Role 헤더로 인증 처리
[ ] JWT 직접 파싱 코드 없음 (JWT 라이브러리 의존성 불필요)
[ ] 내부 API 경로 permitAll 설정
[ ] 서비스 간 호출 시 내부 포트(8080~8083) 직접 호출
[ ] 외부 테스트는 Gateway 포트(9000) 사용
```
