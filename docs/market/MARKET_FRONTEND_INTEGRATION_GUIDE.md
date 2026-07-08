# Market 프론트 연동 가이드

> Market API 필드의 UI 표시 의미와 상태별 처리 기준을 설명한다. 상세 API 계약은 `MARKET_API_SPEC.md`를 따른다.

## 1. 핵심 모델

Market은 **Pool Share Pricing + Contract-weighted Pari-mutuel Settlement** 방식이다.

```text
contractQuantity = pointAmount / priceSnapshot
feeAmount = floor2(losingPool * feeRate / 100)
rewardPool = losingPool - feeAmount
winner settlementAmount = floor2(pointAmount + rewardPool * contractQuantity / totalWinningContractQuantity)
loser settlementAmount = 0.00
settlementPool = winningPrincipalPool + rewardPool
```

`contractQuantity`는 고정 지급권이 아니다. 정답 선택지 안에서 `rewardPool`을 나누는 가중치이며, 오답 선택지에서는 만기 시 가치가 0이다. `virtualPoolAmount`는 가격 계산에만 사용하고 정산·환불·Insight `totalPoolAmount`에는 포함하지 않는다.

## 2. 프론트용 용어 사전

| 백엔드 필드 | 프론트 표시 권장명 | 의미 | 주의 |
|---|---|---|---|
| `currentPrice` | 현재 가격 / 현재 확률성 가격 | 선택지의 현재 Pool Share 가격 | 확정 확률이 아님 |
| `priceSnapshot` | 체결 가격 | 예측 확정 시점 가격 | Quote 가격과 다를 수 있음 |
| `contractQuantity` | 체결 계약 수량 / 보상 가중치 | 정답 선택지 내 rewardPool 분배 가중치 | 고정 지급권이 아님 |
| `pointAmount` | 참여 포인트 | 사용자가 사용한 실제 포인트 | 정산 원금 기준 |
| `currentPayoutPerContract` | 계약당 예상 추가 보상 | 현재 기준 예상 rewardPerContract | 원금 포함 아님 |
| `payoutPerContract` | 계약당 확정 추가 보상 | 정산 시 확정된 rewardPerContract | 원금 포함 아님 |
| `estimatedPayoutIfWin` | 적중 시 예상 정산금 | 원금 + 예상 reward share | 확정값이 아님 |
| `estimatedProfitIfWin` | 적중 시 예상 순이익 | 예상 정산금 - 원금 | 확정값이 아님 |
| `settlementAmount` / `settledAmount` | 정산금 | 실제 지급 금액 | 패자는 `0.00` |
| `profitAmount` | 손익 | 정산금 - 참여 포인트 | 패자는 `-pointAmount` |
| `totalRealPoolAmount` | 실제 참여금 | 실제 사용자 참여 포인트 총합 | 정산 재원 |
| `totalVirtualPoolAmount` | 가상 유동성 | 가격 안정화용 가상 풀 | 정산 재원 아님 |
| `totalEffectivePoolAmount` | 가격 계산용 유효 풀 | real + virtual | 실제 참여금으로 표시 금지 |
| `settlementPool` / `settlementPoolAmount` | 정산 지급 가능 풀 | winningPrincipalPool + rewardPool | totalPool - feeAmount로 설명 금지 |

### contractQuantity

정답 선택지의 `contractQuantity`만 reward 분배 비율 계산에 사용한다. 오답 선택지의 수량은 지급 계산에 사용하지 않는다.

- 권장: `체결 계약 수량`, `보상 분배 가중치`, `적중 시 보상 비율에 반영되는 수량`
- 비추천: `받을 계약 수량`, `확정 지급 수량`, `계약 1개당 1P 지급`, `계약 수량만큼 지급`

### currentPayoutPerContract / payoutPerContract

두 필드는 원금을 포함한 계약당 정산금이 아니라 **계약 1개당 추가 reward**다.

```text
currentPayoutPerContract = 현재 예상 rewardPool / 현재 선택지 총 contractQuantity
payoutPerContract = 확정 rewardPool / totalWinningContractQuantity
```

- 권장: `계약당 예상 추가 보상`, `계약당 reward`, `계약당 보상 단가`
- 비추천: `계약당 정산금`, `계약당 지급액`, `계약 1개당 받을 금액`

### estimatedPayoutIfWin / estimatedProfitIfWin

```text
estimatedPayoutIfWin = 내 원금 + 현재 예상 reward share
estimatedProfitIfWin = estimatedPayoutIfWin - pointAmount
```

`estimatedPayoutIfWin`은 원금 포함 예상 총액이고, `estimatedProfitIfWin`은 원금을 제외한 예상 순이익이다. 다른 사용자의 추가 참여에 따라 가격, 계약 수량, losingPool과 rewardPool이 달라질 수 있으므로 `확정 수령액`으로 표시하지 않는다.

### settlementAmount / profitAmount

정산 완료 후에는 예상 필드가 아닌 실제 값을 사용한다.

| 결과 | settlementAmount | profitAmount |
|---|---:|---:|
| winner | 원금 + reward share | settlementAmount - pointAmount |
| loser | `0.00` | `-pointAmount` |

### settlementPool / settlementPoolAmount

```text
settlementPool = winningPrincipalPool + rewardPool
rewardPool = losingPool - feeAmount
feeAmount = floor2(losingPool * feeRate / 100)
```

`전체 참여금에서 수수료를 뺀 금액`으로 설명하지 않는다. 표시가 필요하면 `정산 지급 가능 풀` 또는 `정산 대상 지급 풀`을 사용한다.

## 3. 화면별 처리 기준

### Market 목록·상세

| 필드 | 용도 |
|---|---|
| `status` | DB 생명주기 상태 |
| `displayStatus` | 화면에 표시할 상태 |
| `canPredict` | Quote·예측 참여 버튼 활성화 기준 |

```text
canPredict == true  -> Quote / 예측 참여 버튼 활성화
canPredict == false -> 예측 참여 버튼 비활성화
displayStatus == CLOSED_BY_TIME -> "마감" 표시
```

`status == ACTIVE`만 보고 버튼을 활성화하지 않는다. 마감 시각이 지난 ACTIVE Market이 있을 수 있으므로 반드시 `canPredict`를 사용한다.

### Quote 화면

Quote는 확정 견적이 아니다. 권장 안내 문구:

> 현재 가격은 실시간으로 변동될 수 있으며, 실제 참여 시점의 가격 기준으로 계약 수량이 확정됩니다.

현재 Quote API가 제공하는 값은 현재 가격, 참여 포인트, 예상 계약 수량, 참여 후 예상 가격, 가격 영향도와 유효 풀이다. 참여 전 `estimatedPayoutIfWin`과 `estimatedProfitIfWin`은 현재 Quote 응답 필드가 아니므로 프론트에서 임의 계산하지 않는다. 해당 미리보기가 필요하면 별도 API 계약이 선행되어야 한다.

모든 Quote 값에는 `예상`이라는 표현을 사용하고 확정 정산금으로 표시하지 않는다.

### 내 예측 화면

| PredictionStatus | UI 문구 | 금액 표시 기준 |
|---|---|---|
| `POINT_PENDING` | 처리 중 | 예상값 숨김 |
| `POINT_UNKNOWN` | 처리 상태 확인 중 | 예상값 숨김 |
| `FAILED` | 예측 참여 실패 | 예상값 숨김 |
| `CONFIRMED` | 예측 참여 완료 | `estimatedPayoutIfWin`, `estimatedProfitIfWin` |
| `SETTLED` | 정산 완료 | 실제 `settledAmount`와 실제 손익 |
| `REFUND_PENDING` | 환불 처리 중 | 예상값 숨김 |
| `REFUND_UNKNOWN` | 환불 상태 확인 중 | 예상값 숨김 |
| `REFUNDED` | 환불 완료 | `refundAmount` |

`SETTLED`에서는 예상값을 사용하지 않는다. 목록 응답에 별도 `profitAmount`가 없다면 실제 손익은 Decimal 연산으로 `settledAmount - pointAmount`를 계산한다.

### 502·503·504 처리

예측 참여 API의 502·503·504는 즉시 참여 실패로 확정하지 않는다.

1. `예측 참여 처리 상태를 확인 중입니다`를 표시한다.
2. 3~5초 간격으로 `GET /api/v1/markets/{marketId}/predictions/me`를 polling한다.
3. 조회된 PredictionStatus에 따라 화면을 갱신한다.

### Market 상세 댓글 영역

댓글 영역은 Market 상세 페이지 하단에 배치한다. 목록은 비로그인 사용자도 조회할 수 있고, 작성 폼과 삭제 기능만 로그인 여부를 사용한다. 댓글은 단일 depth이며 답글, 좋아요, 신고, 수정 UI를 제공하지 않는다.

#### 상태별 UI

| Market 상태 | 목록 조회 | 댓글 작성 | 권장 UI |
|---|---:|---:|---|
| `ACTIVE` | O | O | 목록과 작성 폼 표시 |
| `CLOSED` | O | O | 목록과 작성 폼 표시 |
| `DATA_PENDING` | O | O | 목록과 작성 폼 표시 |
| `SETTLEMENT_IN_PROGRESS` | O | O | 목록과 작성 폼 표시 |
| `SETTLED` | O | O | 목록과 작성 폼 표시 |
| `PENDING` | O | X | 목록만 표시하고 공개 전 안내 |
| `VOIDED` | O | X | 목록만 표시하고 무효 Market 안내 |
| 존재하지 않거나 삭제된 Market | X | X | Market 404 화면 |

비로그인 상태에서는 목록을 그대로 보여주고 입력 폼 대신 `로그인 후 댓글을 작성할 수 있습니다` CTA를 표시한다. 로그인 상태에서도 `PENDING`, `VOIDED`이면 작성 폼을 숨기거나 비활성화한다. 서버의 `MARKET_COMMENT_NOT_ALLOWED` 응답을 최종 기준으로 삼는다.

#### 목록과 페이징

```text
GET /api/v1/markets/{marketId}/comments?page={page}&size=10
정렬: created_at ASC, id ASC (서버 고정)
```

- 댓글 목록 응답의 `data.content`가 빈 배열이면 정상적인 빈 목록으로 렌더링하고 `아직 댓글이 없습니다.`를 표시한다. 이 경우 응답은 `200 OK`다.
- 댓글 본문 `content`가 blank인 작성 요청은 `VALIDATION_FAILED`, `400 Bad Request`로 처리하므로 입력 오류를 표시한다.
- 댓글의 `memberId`와 현재 로그인 회원 id가 같을 때만 삭제 버튼을 노출한다.
- soft delete된 댓글은 서버 목록에 포함되지 않으므로 `삭제된 댓글입니다` placeholder를 만들지 않는다.
- 동일 시각 댓글의 순서는 `id ASC`로 안정화되므로 클라이언트에서 재정렬하지 않는다.

#### 작성 성공 후 갱신

오름차순 정렬에서는 새 댓글이 마지막에 추가된다. 작성 성공 시 댓글 목록 query 전체를 invalidate하고 `totalElements`로 계산한 마지막 page를 조회해 그 page로 이동하는 방식을 권장한다. 현재 page에 응답 데이터를 임의 append하면 새 댓글이 다른 page에 속하거나 동시 작성 댓글을 놓칠 수 있다.

```text
1. POST 성공
2. 해당 marketId의 comment-list query root invalidate
3. 최신 totalElements 확인
4. lastPage = max(0, ceil(totalElements / size) - 1)
5. lastPage refetch 및 이동
```

삭제 성공 시 응답의 `data.commentId`와 `data.deleted == true`를 확인하고 현재 page를 invalidate/refetch한다. 삭제 후 현재 page가 비었고 `page > 0`이면 이전 page로 이동해 다시 조회한다. 중복 삭제가 `MARKET_COMMENT_NOT_FOUND`이면 이미 삭제된 상태로 보고 목록을 refetch하되 성공 toast를 중복 표시하지 않는다. 관리자 삭제 UI는 이번 MVP 범위에 포함하지 않는다.

#### 오류 표시

| ErrorCode | 권장 처리 |
|---|---|
| `UNAUTHORIZED` | 로그인 CTA 표시. Gateway 인증 상태 재확인 |
| `VALIDATION_FAILED` | 공백 입력 또는 잘못된 페이징 값 수정 안내 |
| `MARKET_COMMENT_TOO_LONG` | `댓글은 500자까지 입력할 수 있습니다` 표시 |
| `MARKET_COMMENT_NOT_FOUND` | 목록 refetch 후 대상 제거 |
| `MARKET_COMMENT_FORBIDDEN` | 삭제 권한 없음 안내 후 로그인 회원 정보 재확인 |
| `MARKET_COMMENT_NOT_ALLOWED` | 작성 폼 비활성화 및 Market 상태 안내 |
| `MARKET_NOT_FOUND` | Market 404 화면으로 전환 |

댓글 요청은 포인트나 Prediction 상태와 무관하다. 댓글 실패 시 포인트 잔액 polling, Prediction polling, 정산/환불 retry UI를 시작하지 않는다.

## 4. Decimal String 처리

Market API의 금액·가격·계약 수량은 JSON Number가 아니라 String이다.

```text
currentPrice, priceSnapshot, contractQuantity, pointAmount,
estimatedPayoutIfWin, estimatedProfitIfWin, settlementAmount,
settledAmount, refundAmount, realPoolAmount, virtualPoolAmount
```

- 화면 표시만 한다면 문자열 기반으로 포맷팅한다.
- 계산이 필요하면 `decimal.js`, `big.js` 같은 Decimal 라이브러리를 사용한다.
- `Number(value)`로 금액·가격·계약 수량을 계산하지 않는다. JavaScript 부동소수점 오차가 발생할 수 있다.

## 5. 표시 예시

### 예시 A — 혼자 정답인 경우

```text
NO에 10P 참여
priceSnapshot = 0.50000000
contractQuantity = 20.00000000
다른 참여자 없음, 정답 = NO

winningPrincipalPool = 10.00
losingPool = 0.00
feeAmount = 0.00
rewardPool = 0.00
settlementAmount = 10.00
profitAmount = 0.00
```

프론트 표시: `정산금 10.00P`, `손익 0.00P`. 상대편 패자 풀이 없으므로 추가 보상 없이 원금만 반환된다. 오류가 아니다.

### 예시 B — 초반 참여자가 더 많은 reward를 받는 경우

```text
사용자 A: NO 가격 0.50000000, 10P, contractQuantity 20.00000000
사용자 B: NO 가격 0.90000000, 10P, contractQuantity 11.11111111
YES losingPool = 100.00
feeRate = 5.00, rewardPool = 95.00
totalWinningContractQuantity = 31.11111111

A settlementAmount = 71.07
B settlementAmount = 43.92
```

같은 10P를 참여해도 낮은 가격에 먼저 참여한 사용자는 더 많은 계약 수량을 확보하므로 적중 시 더 많은 reward를 받는다.

## 6. UI 문구 체크리스트

권장 문구:

- `체결 계약 수량`
- `계약당 예상 추가 보상`
- `적중 시 예상 정산금`
- `적중 시 예상 순이익`
- `정산금`
- `정산 지급 가능 풀`

사용 금지 또는 주의 문구:

- `계약 수량만큼 지급`
- `계약 1개당 정산금`
- `계약당 확정 지급액`
- `확정 수령액` — 예상값에 사용 금지
- `totalEffectivePoolAmount = 실제 참여금`
- `virtualPoolAmount = 정산 재원`

댓글 수동 확인 체크리스트:

- [ ] 비로그인 상태에서 댓글 목록이 조회되고 작성 대신 로그인 CTA가 보인다.
- [ ] 로그인 상태에서 작성 성공 후 comment-list query가 invalidated되고 마지막 page가 refetch된다.
- [ ] 새 댓글이 `createdAt ASC, commentId ASC` 순서의 마지막 위치에 보인다.
- [ ] 본인 댓글에만 삭제 버튼이 보인다.
- [ ] 삭제 성공 후 현재 page가 refetch되고 빈 page라면 이전 page로 이동한다.
- [ ] `PENDING`, `VOIDED`에서는 작성할 수 없고 나머지 허용 상태에서는 작성할 수 있다.
- [ ] 댓글 동작이 point/member/prediction/settlement query를 invalidate하거나 polling하지 않는다.
