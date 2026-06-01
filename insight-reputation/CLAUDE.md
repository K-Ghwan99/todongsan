# CLAUDE.md — Insight-Reputation Service
 
> 루트 `CLAUDE.md`의 공통 지침을 상속한다. 이 파일은 Insight-Reputation Service에 특화된 컨텍스트를 추가한다.
> 루트 `CLAUDE.md`를 먼저 읽고 이 파일을 읽는다.
 
---
 
## 내 서비스 컨텍스트
 
- **서비스명:** Insight-Reputation Service
- **패키지 루트:** `com.todongsan.insightreputation`
- **담당 문서:**
  - `docs/insight-reputation/INSIGHT_ERD.md` (ERD 최신본)
  - `docs/insight-reputation/INSIGHT_API_SPEC.md` (API 명세 최신본)
  - `docs/insight-reputation/INSIGHT_ERROR_CODE.md` (도메인 에러 코드)
### 핵심 도메인 테이블
 
| 테이블 | 역할 | 주요 제약 |
|---|---|---|
| `reputation` | 회원별 신뢰도 점수 및 거주지역 선언 | `member_id` UNIQUE |
| `visit_certification` | GPS/댓글 기반 방문 인증 | `(member_id, sido, sigu)` UNIQUE |
| `insight_report` | AI 분석 리포트 상태 관리 | `(type, reference_id)` UNIQUE |
| `public_data_snapshot` | 공공 API 배치 적재 | `(source, data_type, reference_date, source_region_id)` UNIQUE |
 
### 핵심 비즈니스 제약 (코드 작성 전 반드시 숙지)
 
```
[reputation]
- activity_count 최대 3. activity_confirmed_at IS NOT NULL이면 증가 skip.
- 거주지역 변경 시 activity_count = 0, activity_confirmed_at = NULL 동시 초기화.
- 거주지역 변경 쿨다운: residence_changed_at + 30일. 최초 선언 시 skip.
 
[visit_certification]
- 동일 지역 재인증: INSERT가 아니라 UPDATE.
- 쿨다운: last_certified_at + 30일 > NOW() 이면 거부.
- GPS 반경: 지역 중심 좌표 기준 3km 초과 시 거부.
 
[insight_report]
- 상태 전이: PENDING → PROCESSING → DONE / FAILED (단방향, 역방향 불가).
- PROCESSING 전이 시 processing_started_at 기록.
- 스케줄러: processing_started_at + 10분 초과 시 PENDING으로 리셋.
- retry_count >= 3이면 영구 FAILED. 자동 재시도 없음.
 
[public_data_snapshot]
- 배치 재실행: ON DUPLICATE KEY UPDATE (멱등성 보장).
- 삭제 금지. 이력 보존 정책.
```
 
---
 
## 연계 서비스
 
### 내가 호출하는 서비스
 
| 서비스 | 엔드포인트 | 목적 | 주의사항 |
|---|---|---|---|
| Battle Service | `GET /api/v1/battles/comments/{commentId}` | COMMENT 방문 인증 시 댓글 지역 확인 | 응답에 sido/sigu 포함 여부 Battle 담당자 확인 필요 |
| Battle Service | Battle 투표/댓글 원본 데이터 조회 | AI 분석 리포트 생성 시 | 내부 API. 루트 API_SPEC.md 섹션 참조 |
| Market Service | Market 참여/정산 데이터 조회 | AI 분석 리포트 생성 시 | 내부 API. 루트 API_SPEC.md 섹션 참조 |
| Member-Point Service | `POST /api/v1/points/spend` | AI 리포트 80P 차감 | Idempotency-Key 필수 |
| Member-Point Service | Point 환불 API | GENERATION_FAILED / SOURCE_DATA_NOT_READY 시 | Member-Point 담당자가 환불 로직 소유 |
 
### 나를 호출하는 서비스
 
| 서비스 | 엔드포인트 | 목적 |
|---|---|---|
| Battle Service | `POST /internal/reputations/activity` (또는 루트 API_SPEC 정의 따름) | 투표/댓글 완료 시 activity_score 업데이트 |
| Market Service | `POST /internal/reputations/prediction-result` (또는 루트 API_SPEC 정의 따름) | 정산 완료 시 prediction_count/correct 업데이트 |
 
> **확인 필요:** 내부 연계 API 엔드포인트는 루트 `API_SPEC.md`를 기준으로 하며,
> Battle/Market 담당자와 필드 정합성을 사전에 확인한다.
 
---
 
## 공공 API 설정
 
```
한국부동산원 R-ONE (우선 사용)
- 인증키: 4178fe4edf654e8dad253d7d45db2ce4
- 엔드포인트: SttsApiTblData.do
- 주간 매매가격지수 STATBL_ID: T244183132827305 (WK)
- 월간 STATBL_ID: A_2024_00045 (MM)
 
배치 주기
- REB 주간 데이터: 매주 목요일 공표 후 실행
- REB 월간 데이터: 매월 15일 공표 후 실행
```
 
---
 
## 이 서비스의 성공 기준
 
### 기본 기동
 
```
1. 애플리케이션 기동 → 검증: GET /actuator/health → 200 OK
2. DB 스키마 생성 → 검증: 4개 테이블 존재 확인
3. Enum 매핑 → 검증: JPA Enum 컬럼 오류 없이 기동
```
 
### API별 성공 기준
 
```
[Reputation]
- GET /api/v1/reputations/me → 200, visitCertifications 배열 포함
- PUT /api/v1/reputations/me/residence → 쿨다운 30일 적용 확인
  (최초 선언 시 쿨다운 skip 확인)
 
[Visit Certification]
- POST /api/v1/reputations/visit-certifications (GPS)
  → 반경 내: 200, 반경 외: 400 VISIT_CERT_OUT_OF_RANGE
- POST /api/v1/reputations/visit-certifications (COMMENT)
  → 지역 일치: 200, 불일치: 400 VISIT_CERT_COMMENT_REGION_MISMATCH
- 30일 내 재시도: 400 VISIT_CERT_COOLDOWN
- 재인증: DB UPDATE 확인 (INSERT 아님)
 
[Insight Report]
- POST /api/v1/insights/battles/{id}/report
  → PENDING 상태 즉시 반환, pointCharged: 80
  → Battle 미종료 시: 400 INSIGHT_REPORT_SOURCE_NOT_CLOSED (Point 차감 없음)
  → 이미 PENDING/PROCESSING: 409 INSIGHT_REPORT_ALREADY_PROCESSING (Point 차감 없음)
  → 기존 DONE 존재 시: 즉시 리포트 반환, pointCharged: 0
- GET /api/v1/insights/battles/{id}/report/status → 폴링 2초 간격 가이드 준수
```
 
### 비즈니스 로직 검증
 
```
- activity_count: 3 도달 시 activity_confirmed_at 동일 트랜잭션 업데이트
- prediction_accuracy: FLOOR(correct/count * 100 * 100) / 100 (버림)
- public_data_snapshot 배치: ON DUPLICATE KEY UPDATE 확인
```
 
---
 
## 이 서비스에서 하지 않는 것
 
```
✗ Point를 직접 DB에서 관리하지 않는다. 반드시 Member-Point Service REST 호출.
✗ battle, market 테이블을 직접 JOIN하지 않는다.
✗ GPS 인증에서 행정구역 경계 판별하지 않는다. 중심 좌표 반경 계산만.
✗ AI 분석에서 특정 선택지를 추천하지 않는다. 정보 요약만.
✗ 방문 인증 이력 로그 테이블은 MVP에서 구현하지 않는다.
✗ MCP 직접 구현하지 않는다. Claude API 직접 호출만.
```
 
---
 
## 작업 전 체크리스트
 
```
[ ] 루트 CLAUDE.md 읽었는가?
[ ] INSIGHT_ERD.md (최신본) 읽었는가?
[ ] INSIGHT_API_SPEC.md (최신본) 읽었는가?
[ ] 연계 서비스 API 호출이 필요한 경우 해당 서비스 API_SPEC 확인했는가?
[ ] Point 관련 로직이라면 Idempotency-Key 처리를 고려했는가?
[ ] 비즈니스 제약 (쿨다운, 상태 전이, activity_count 상한)을 코드에 반영했는가?
```
 
---
 
## 코드 작성 원칙
 
### 레이어 책임 경계
 
```
Controller
- HTTP 요청/응답 변환만 담당한다.
- 비즈니스 로직을 포함하지 않는다.
- @Valid로 입력 검증, Service 호출, DTO 반환이 전부다.
 
Service
- 트랜잭션 경계를 소유한다. (@Transactional)
- 비즈니스 제약(쿨다운, 상태 전이, 상한 등)은 반드시 여기서 처리한다.
- @Transactional 블록 안에서 외부 REST 호출 금지.
  → 외부 호출은 트랜잭션 밖에서 먼저 처리하고, 결과로 DB를 업데이트한다.
 
Repository
- JPA 쿼리 정의만 담당한다.
- 비즈니스 판단 로직을 포함하지 않는다.
 
외부 REST 호출
- xxxClient 클래스로 분리한다. (예: BattleClient, MemberPointClient)
- Service에서 직접 RestTemplate/WebClient를 호출하지 않는다.
```
 
### 네이밍 규칙
 
```
클래스:     PascalCase    BattleClient, VisitCertificationService
메서드:     camelCase     registerVisitCertification, findReputationByMemberId
변수:       camelCase     lastCertifiedAt, activityCount
상수:       UPPER_SNAKE   MAX_ACTIVITY_COUNT, VISIT_CERT_RADIUS_KM
테스트:     메서드명_상황_기대결과
            예) register_withinRadius_success
                register_cooldownNotExpired_throwsException
```
 
### 금지 패턴
 
```
✗ Entity를 Controller 응답으로 직접 노출 → DTO로 변환 필수
✗ Service에서 다른 도메인 Service를 직접 주입 (순환 의존)
  → 같은 서비스 내 도메인 간 호출은 Repository를 통해 처리
✗ 매직 넘버/문자열 → 상수 또는 Enum으로 정의
  나쁨: if (retryCount >= 3)
  좋음: if (retryCount >= InsightReport.MAX_RETRY_COUNT)
✗ 빈 catch 블록 → 최소한 로그 기록
✗ null 반환 → Optional 또는 커스텀 예외
```
 
### 예외 처리
 
```
비즈니스 예외: CustomException(ErrorCode) throw
  → GlobalExceptionHandler가 ApiResponse 에러 형태로 변환
 
외부 서비스 장애: EXTERNAL_SERVICE_UNAVAILABLE (ERROR_POLICY.md)
  → 재시도 대상과 즉시 실패 대상을 구분한다
  → 4xx: 즉시 실패, 5xx/타임아웃: 재시도 가능
 
모든 에러 응답은 errorCode + message 포함 필수 (CONVENTION.md 기준)
```
 
---
 
## 개발 워크플로우
 
### 기능 하나 = 브랜치 하나 = 커밋 하나
 
```
1. feature 브랜치 생성
   git checkout -b feature/visit-cert-gps
 
2. 기능 구현
   - Entity/DTO → Repository → Service → Controller 순서
   - 각 레이어 완성 후 다음 레이어로
 
3. 단위 테스트 작성 (커밋 전 필수)
   - 아래 "테스트 작성 기준" 참고
 
4. 테스트 전체 통과 확인
   ./gradlew test
 
5. 커밋
   git commit -m "feat: GPS 기반 방문 인증 등록"
 
6. develop으로 PR
```
 
### 테스트 작성 기준
 
기능 구현이 끝나면 **커밋 전** 반드시 단위 테스트를 작성한다.
테스트가 없는 기능은 완료로 간주하지 않는다.
 
**Service 레이어 테스트 (최우선)**
 
각 Service 메서드에 대해 아래 케이스를 커버한다.
 
```
[정상 케이스]
- 입력이 유효하고 모든 조건이 충족될 때 기대 결과를 반환하는가
 
[비즈니스 예외 케이스]
- 도메인 제약 위반 시 올바른 ErrorCode로 CustomException이 발생하는가
  예) 쿨다운 미경과 → VISIT_CERT_COOLDOWN
      반경 초과 → VISIT_CERT_OUT_OF_RANGE
      잔액 부족 → POINT_INSUFFICIENT
 
[경계값 케이스]
- 경계 조건에서 정확히 동작하는가
  예) activity_count = 2 → 3 전환 시 activity_confirmed_at 세팅
      retry_count = 2 → 3 도달 시 영구 FAILED
      쿨다운 정확히 30일 경과 → 통과
      쿨다운 29일 → 거부
```
 
**테스트 작성 방식**
 
```java
// Mockito로 의존성 격리. DB, 외부 API 호출 없이 순수 비즈니스 로직만 검증.
@ExtendWith(MockitoExtension.class)
class VisitCertificationServiceTest {
 
    @InjectMocks VisitCertificationService service;
    @Mock VisitCertificationRepository repository;
    @Mock BattleClient battleClient;
 
    @Test
    @DisplayName("GPS 인증 - 반경 내 좌표 → 인증 성공")
    void register_withinRadius_success() { ... }
 
    @Test
    @DisplayName("GPS 인증 - 반경 외 좌표 → VISIT_CERT_OUT_OF_RANGE")
    void register_outsideRadius_throwsOutOfRange() { ... }
 
    @Test
    @DisplayName("GPS 인증 - 30일 쿨다운 미경과 → VISIT_CERT_COOLDOWN")
    void register_cooldownNotExpired_throwsCooldown() { ... }
 
    @Test
    @DisplayName("재인증 - 기존 레코드 UPDATE, INSERT 아님")
    void register_recertification_updatesExistingRecord() { ... }
}
```
 
**커밋 전 체크리스트**
 
```
[ ] 구현한 Service 메서드마다 테스트 클래스가 존재하는가?
[ ] 정상/예외/경계값 케이스가 모두 포함되어 있는가?
[ ] ./gradlew test 전체 통과했는가?
[ ] 테스트 메서드명이 메서드명_상황_기대결과 형식인가?
[ ] @DisplayName에 한국어로 의도가 명확히 적혀 있는가?
```
 
### 기능 단위 구분 (이 서비스 기준)
 
아래 단위로 브랜치와 커밋을 나눈다.
 
```
feat/project-init          - 프로젝트 기본 구조, Entity, Enum, 공통 클래스
feat/reputation-crud       - Reputation 조회, 거주지역 선언/변경
feat/visit-cert-gps        - GPS 방문 인증
feat/visit-cert-comment    - COMMENT 방문 인증
feat/insight-report-battle - Battle AI 리포트 생성/조회/상태
feat/insight-report-market - Market AI 정보 요약 생성/조회/상태
feat/public-data-batch     - 공공 API 배치 수집 스케줄러
feat/internal-api          - 타 서비스에서 호출받는 내부 연계 API
```