# market-service 실험 결과

실행일: 2026-06-16 / 커밋: 34ae195 / MySQL: mysql:8.4 / Java: Temurin 21.0.10

## ① 동시성 정합성

- CONCURRENCY 50: CONFIRMED 50/50, 가격합 1, real pool 5000.00, history 100, 소요 1656ms
- CONCURRENCY 100: 미실행
- CONCURRENCY 200: 미실행

```text
================ [실험1] 동시성 정합성 결과 ================
 동시 요청 수(CONCURRENCY) : 50
 확정 성공(success)        : 50
 확정 실패/충돌(failure)   : 0
 DB CONFIRMED 건수          : 50 / 50
 선택지 가격 합(=1.0 기대)  : 1
 real pool 합(기대 5000.00) : 5000.00
 market.total_pool          : 5000.00
 price_history 행수(기대 100) : 100
 전체 소요(ms)              : 1656
==========================================================
```

## ② 정산/환불 멱등성

- 전체 4 / 1차 성공 2 / 실패 1 / 재시도 후 SETTLED 4 / 중복지급 0

```text
[실험2] 멱등성: 전체 4 / 1차 성공 2 / 1차 실패 1 / 재시도 후 SETTLED 4 / 중복지급 0
```

## ③ 대사 복구

- 대상 4 / CONFIRMED 1 / FAILED 2 / UNKNOWN유지 1 / 분류정확 4/4 / 고착 0

```text
[실험3] 대사: 대상 4 / CONFIRMED 1 / FAILED 2 / UNKNOWN유지 1 / 분류정확 4/4 / 고착 0
```

## ④ 트랜잭션 경계 A/B

- 외부지연 2000ms: A 53ms vs B 2018ms (개선 97%)

```text
[실험4] 외부지연 2000ms 주입: 락구간 A=53ms vs B=2018ms (개선 97%)
```

## ①-B 대조군

- 잘못된 락 가격합 미실행 / lost update 미실행
