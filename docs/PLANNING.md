# 동네대전 서비스 기획 및 정책 방향

> 이 문서는 동네대전의 서비스 기획 배경과 정책 방향을 설명합니다. 실제 구현 기준의 API, 상태 전이, 정산/환불 정책은 각 도메인 문서를 기준으로 합니다.

## 1. 서비스 개요

동네대전은 지역 선택에 필요한 집단지성을 수집하는 플랫폼입니다. 사용자는 지역에 대한 선호, 경험, 예측을 남기고, 서비스는 이를 투표·예측·분석 데이터로 축적해 더 합리적인 지역 선택을 돕습니다.

서비스는 두 종류의 질문을 분리해서 다룹니다.

| 구분 | 역할 |
| --- | --- |
| Battle | 데이트, 여행, 자취, 생활권처럼 주관적 선호가 중요한 지역 질문을 블라인드 투표로 수집 |
| Market | 가격지수, 거래량, 정책 발표처럼 외부 데이터로 판정 가능한 질문을 포인트 예측 시장으로 운영 |

## 2. 문제의식

기존 부동산·지역 정보 서비스는 매매가, 전세가, 월세, 평수, 역세권, 주변 편의시설 같은 정량 데이터에 강합니다. 하지만 실제 사용자는 이런 질문도 자주 합니다.

```text
실제로 살기 좋은 곳은 어디인가?
데이트하기 좋은 곳은 어디인가?
자취 초보에게 더 나은 동네는 어디인가?
앞으로 가격이 오를 가능성이 높은 지역은 어디인가?
사람들은 지금 어떤 지역을 더 선호하고 있는가?
```

동네대전은 정량 지표만으로 답하기 어려운 선호 질문은 Battle로, 객관적 결과가 필요한 예측 질문은 Market으로 분리해 수집합니다.

## 3. 사용자 흐름

```text
1. 사용자는 Battle에서 지역 선택 투표에 참여한다.
2. 서비스 내부 Point를 획득하거나 보유 Point를 확인한다.
3. 객관적 판정이 가능한 Market 예측에 Point로 참여한다.
4. Market은 공공데이터 또는 외부 지표로 결과를 확정한다.
5. 정답 선택지 참여자는 정책에 따라 정산을 받는다.
6. 누적된 투표, 예측, 댓글, 인증 데이터는 Insight 분석 재료가 된다.
```

Battle, Member-Point, Insight-Reputation의 세부 정책은 담당 도메인 구현과 문서를 기준으로 확인해야 합니다. 이 문서에서는 구현 완료 여부를 단정하지 않고 서비스 기획 방향만 설명합니다.

## 4. Battle과 Market 분리 원칙

참여자가 결과를 직접 바꿀 수 있는 주제에는 포인트 예측 참여를 허용하지 않는 것이 기본 원칙입니다.

예를 들어 “성수 vs 연남, 주말에 놀기 어디가 더 좋은가?” 같은 주제는 사용자의 투표 자체가 결과입니다. 이런 주제에 포인트 예측을 허용하면 사용자가 실제 선호가 아니라 “이길 것 같은 선택지”에 투표할 수 있어 데이터가 왜곡됩니다.

따라서 주관적 선호와 경험을 모으는 질문은 Battle에서 다루고, Market은 외부 데이터로 결과를 판정할 수 있는 질문만 다룹니다.

## 5. Market 운영 정책 방향

Market은 사용자의 투표가 아니라 공공데이터 또는 외부 지표로 결과가 확정되는 주제만 허용합니다.

좋은 Market 주제의 조건은 다음과 같습니다.

- 결과를 객관적으로 판정할 수 있어야 한다.
- 판정 데이터 출처가 명확해야 한다.
- 예측 마감 시점과 결과 확정 기준이 명확해야 한다.
- 사용자가 직접 결과를 조작하기 어려워야 한다.
- 선택지가 명확해야 한다.
- 지역 비하, 혐오, 정치적 선동으로 흐르지 않아야 한다.

예시 주제는 다음과 같은 방향입니다.

```text
다음 주 서울 아파트 매매가격지수는 상승할까?
이번 달 서울 아파트 거래량은 지난달보다 증가할까?
특정 기간 공개된 실거래가가 사전 기준을 넘을까?
```

실거래가, 정책·개발 이벤트처럼 데이터 공개 지연이나 판정 기준 모호성이 큰 주제는 관리자 검수와 무효 조건을 함께 정의해야 합니다.

## 6. Point 경제 구조

Point는 서비스 참여를 유도하는 내부 재화이며 현금성 자산이 아닙니다.

- 현금 환전 불가
- 코인 전환 불가
- 상품권·기프티콘 교환 불가
- 사용자 간 양도 불가
- 외부 거래 불가

기획상 Point 경제는 다음 흐름을 가집니다.

```text
Battle = Point 생성
Market = Point 사용 / 이동 / 일부 소각
Insight = Point 소비 또는 분석 가치 제공
```

Market에서는 사용자가 보유 Point로 예측에 참여하고, 결과 확정 후 정답자에게 정산하는 구조를 지향합니다. 수수료, 소각, 정산 산식, 소수점 처리 같은 구현 기준은 [Market API Spec](market/MARKET_API_SPEC.md)과 [Market Failure Scenario](market/MARKET_FAILURE_SCENARIO.md)를 기준으로 합니다.

## 7. 구현 기준 상태값 요약

상태 전이의 상세 규칙은 Market 도메인 문서를 기준으로 하며, 이 문서에는 코드에서 확인한 enum 값만 요약합니다.

| 구분 | 실제 코드 기준 값 |
| --- | --- |
| PredictionStatus | `POINT_PENDING`, `CONFIRMED`, `FAILED`, `POINT_UNKNOWN`, `SETTLED`, `REFUND_PENDING`, `REFUND_UNKNOWN`, `REFUNDED` |
| MarketStatus | `PENDING`, `ACTIVE`, `CLOSED`, `DATA_PENDING`, `SETTLEMENT_IN_PROGRESS`, `SETTLED`, `VOIDED` |
| SettlementStatus | `PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED` |
| RefundStatus | `PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED` |
| MarketVoidReasonType | `DATA_UNAVAILABLE`, `ADMIN_ERROR`, `MARKET_CANCELLED`, `NO_TRANSACTION`, `ETC` |

예측 참여는 `POINT_PENDING` 상태로 생성된 뒤 Member-Point 차감 결과에 따라 `CONFIRMED`, `FAILED`, `POINT_UNKNOWN` 등으로 관리됩니다. 포인트 부족처럼 명확한 실패는 실패 상태로 처리하고, 타임아웃·통신 장애·응답 불확실 상황은 대사와 재시도로 보정할 수 있는 상태로 남기는 방향입니다.

정산 대상과 환불 대상, 상태별 전환 조건은 [Market API Spec](market/MARKET_API_SPEC.md), [Market ERD](market/MARKET_ERD.md), [Market Failure Scenario](market/MARKET_FAILURE_SCENARIO.md)를 기준으로 합니다.

## 8. 서비스 간 연계 방향

MVP는 REST 기반 MSA로 구현합니다. 이벤트 브로커 기반 비동기 처리는 현재 문서 범위의 필수 구현으로 다루지 않습니다.

```text
Client
  └── API Gateway
        ├── Member-Point Service
        ├── Battle Service
        ├── Market Service
        └── Insight-Reputation Service
```

코드 기준으로 Gateway는 JWT를 검증한 뒤 `X-Member-Id`, `X-Member-Role` 헤더를 하위 서비스에 전달합니다. Market은 Member-Point 및 Insight-Reputation 내부 API를 OpenFeign 기반 REST 호출로 연동합니다. Battle은 Member-Point 및 Insight-Reputation 연동에 RestTemplate 기반 REST 호출을 사용합니다.

Market과 Member-Point 연동에서는 포인트 차감, 정산 지급, 환불 요청을 재시도 가능한 외부 호출로 보고 Header `Idempotency-Key`와 항목별 멱등성 키 정책으로 중복 차감·지급·환불을 방지합니다. 현재 구현 기준으로 Member-Point 기록에는 `referenceType=MARKET_PREDICTION`, `referenceId=predictionId`를 사용합니다. 상세 형식은 [Market API Spec](market/MARKET_API_SPEC.md)을 기준으로 합니다.

## 9. Insight 분석 방향

Insight-Reputation은 Battle, Market, Reputation에서 발생한 데이터를 분석해 사용자의 지역 선택을 보조하는 방향의 도메인입니다.

기획 방향은 다음과 같습니다.

- Battle 결과와 댓글을 요약해 선호 이유를 보여주는 방향을 검토합니다.
- Market 데이터와 외부 지표를 함께 요약해 사용자가 판단할 수 있는 정보를 제공하는 방향을 지향합니다.
- 특정 선택지를 추천하기보다 근거, 반대 근거, 데이터 출처, 판정 기준을 정리하는 것을 목표로 합니다.
- 사용자 활동 신뢰도나 예측 정확도 같은 지표는 분석 재료로 활용할 수 있습니다.

세부 API와 구현 범위는 [Insight-Reputation 문서](insight-reputation/)를 기준으로 확인합니다.

## 10. 상세 문서 기준

구현 기준의 API, 상태 전이, 에러 코드, 정산·환불 실패 시나리오는 아래 문서를 따릅니다.

| 문서 | 역할 |
| --- | --- |
| [Market API Spec](market/MARKET_API_SPEC.md) | Market API, 상태 전이, Member-Point 연동, 멱등성 정책 |
| [Market ERD](market/MARKET_ERD.md) | Market 데이터 모델 |
| [Market Error Code](market/MARKET_ERROR_CODE.md) | Market 에러 코드 |
| [Market Failure Scenario](market/MARKET_FAILURE_SCENARIO.md) | 정산·환불·외부 서비스 실패 대응 |
| [Battle 문서](battle/) | Battle 도메인 API·ERD·에러 코드 |
| [Member-Point 문서](member-point/) | 회원·포인트 도메인 API·ERD·에러 코드 |
| [Insight-Reputation 문서](insight-reputation/) | Insight/Reputation API·ERD·관리 기능 |

에러 코드와 API 응답 형식은 각 도메인 API 문서를 기준으로 합니다.
