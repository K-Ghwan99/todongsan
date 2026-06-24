-- DEV ONLY — Supplement seed (run AFTER dev_seed_all_frontend_scenarios.sql)
-- Fixes: insight_report status 'COMPLETED'→'DONE'
-- Adds:  ADMIN member, missing point_history types,
--        insight_report PENDING/FAILED, CLOSED unsettled battle,
--        market_prediction_result, point_history for members 2~5

-- ============================================================
-- 1. insight_report status 오기 수정 (COMPLETED → DONE)
-- ============================================================
USE insight;
UPDATE insight_report SET status = 'DONE' WHERE id IN (700001, 700002);

-- PENDING 리포트 (AI 생성 대기 중 — 배틀 800007 DRAW)
INSERT INTO insight_report (id, type, reference_id, status, summary,
                             analysis_data, raw_prompt, failed_reason,
                             processing_started_at, retry_count,
                             requested_by, report_content, generated_at,
                             created_at, updated_at)
VALUES
(700003, 'BATTLE', 800007, 'PENDING',
 NULL, NULL, NULL, NULL,
 NULL, 0, 1, NULL, NULL,
 NOW(), NOW()),

-- FAILED 리포트 (재시도 3회 초과 — 마켓 900005 데이터 없음)
(700004, 'MARKET', 900005, 'FAILED',
 NULL, NULL, 'market_id=900005 분석 요청', 'Claude API timeout after 3 retries',
 DATE_SUB(NOW(), INTERVAL 11 MINUTE), 3,
 1, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 30 MINUTE), NOW()),

-- PROCESSING 리포트 (AI 호출 진행 중 — 배틀 800006)
(700005, 'BATTLE', 800006, 'PROCESSING',
 NULL, NULL, 'battle_id=800006 분석 요청', NULL,
 DATE_SUB(NOW(), INTERVAL 2 MINUTE), 0,
 1, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 3 MINUTE), NOW());

-- market_prediction_result (Market 정산 완료 후 reputation 반영 기록)
DELETE FROM market_prediction_result WHERE member_id IN (1, 106);
INSERT INTO market_prediction_result (member_id, market_id, prediction_id, is_correct,
                                       processed_at, created_at, updated_at)
VALUES
(1,   900006, 920008, TRUE,  DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(106, 900006, 920009, FALSE, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NOW());

-- ============================================================
-- 2. memberpoint — ADMIN 멤버 + 누락 point_history 타입
-- ============================================================
USE memberpoint;

-- ADMIN 멤버
DELETE FROM member WHERE id = 999;
INSERT INTO member (id, nickname, point_balance, role,
                    residence_sido, residence_sigu, residence_changed_at,
                    age_group, gender, oauth_provider, oauth_id,
                    created_at, updated_at)
VALUES
(999, '관리자_ADMIN', 99999.00, 'ADMIN',
 '서울', '종로구', DATE_SUB(NOW(), INTERVAL 60 DAY),
 'AGE_30S', 'MALE', 'KAKAO', 'TEST_KAKAO_ADMIN',
 DATE_SUB(NOW(), INTERVAL 60 DAY), NOW());

-- 잔액 0 테스터 (포인트 부족 시나리오용)
DELETE FROM member WHERE id = 11;
INSERT INTO member (id, nickname, point_balance, role,
                    oauth_provider, oauth_id, created_at, updated_at)
VALUES
(11, '잔액없음_테스터', 0.00, 'USER', 'KAKAO', 'TEST_KAKAO_011',
 DATE_SUB(NOW(), INTERVAL 1 DAY), NOW());

-- 거주지 변경 쿨다운 테스터 (29일 전 변경 → 아직 쿨다운)
DELETE FROM member WHERE id = 12;
INSERT INTO member (id, nickname, point_balance, role,
                    residence_sido, residence_sigu,
                    residence_changed_at,
                    oauth_provider, oauth_id, created_at, updated_at)
VALUES
(12, '쿨다운_테스터', 500.00, 'USER',
 '서울', '강북구',
 DATE_SUB(NOW(), INTERVAL 29 DAY),
 'KAKAO', 'TEST_KAKAO_012',
 DATE_SUB(NOW(), INTERVAL 30 DAY), NOW());

-- 거주지 미설정 테스터 (최초 선언 플로우)
DELETE FROM member WHERE id = 13;
INSERT INTO member (id, nickname, point_balance, role,
                    oauth_provider, oauth_id, created_at, updated_at)
VALUES
(13, '거주지미설정_테스터', 1000.00, 'USER', 'KAKAO', 'TEST_KAKAO_013',
 DATE_SUB(NOW(), INTERVAL 1 DAY), NOW());

-- member 1 — 누락 point_history 타입 추가 (272P 이후 체인)
DELETE FROM point_history WHERE idempotency_key IN (
  'DEV_SEED:battle_create:800009:member:1',
  'DEV_SEED:slot_expand:member:1',
  'DEV_SEED:refund_market:900007:member:1',
  'DEV_SEED:refund_insight:member:1',
  'DEV_SEED:burn:member:1'
);
INSERT INTO point_history (member_id, type, amount, balance_snapshot, reason,
                           reference_type, reference_id, idempotency_key, status,
                           created_at, updated_at)
VALUES
(1, 'SPEND_BATTLE_CREATE', 20.00,  252.00, 'Battle 주제 생성권',          'BATTLE', 800009, 'DEV_SEED:battle_create:800009:member:1', 'SUCCEEDED', '2026-06-22 13:10:00', '2026-06-22 13:10:00'),
(1, 'SPEND_SLOT',         100.00,  152.00, '관심 지역 슬롯 확장',          NULL,    NULL,   'DEV_SEED:slot_expand:member:1',          'SUCCEEDED', '2026-06-22 13:20:00', '2026-06-22 13:20:00'),
(1, 'REFUND_MARKET',      100.00,  252.00, 'Market 무효 환불 (900007)',    'MARKET', 900007, 'DEV_SEED:refund_market:900007:member:1', 'SUCCEEDED', '2026-06-22 13:30:00', '2026-06-22 13:30:00'),
(1, 'REFUND_INSIGHT',      80.00,  332.00, 'AI 리포트 생성 실패 환불',     NULL,    NULL,   'DEV_SEED:refund_insight:member:1',       'SUCCEEDED', '2026-06-22 13:40:00', '2026-06-22 13:40:00'),
(1, 'BURN',                 0.01,  331.99, '소수점 소각',                  NULL,    NULL,   'DEV_SEED:burn:member:1',                 'SUCCEEDED', '2026-06-22 13:50:00', '2026-06-22 13:50:00');

-- members 2~5 point_history (타 멤버도 이력 보여야 함)
DELETE FROM point_history WHERE member_id BETWEEN 2 AND 5;
INSERT INTO point_history (member_id, type, amount, balance_snapshot, reason,
                           reference_type, reference_id, idempotency_key, status,
                           created_at, updated_at)
VALUES
(2, 'EARN_SIGNUP',         1000.00, 1000.00, '신규 가입 보상',             NULL,   NULL,   'DEV_SEED:signup:member:2',             'SUCCEEDED', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),
(2, 'EARN_VOTE',             10.00, 1010.00, 'Battle 투표 참여 보상',      'BATTLE',800001,'DEV_SEED:vote:battle:800001:member:2', 'SUCCEEDED', DATE_SUB(NOW(), INTERVAL 2 DAY),  DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 'SPEND_MARKET',         100.00,  910.00, 'Market 예측 참여 차감',      'MARKET',900001,'DEV_SEED:spend:market:900001:member:2','SUCCEEDED', DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY)),
(3, 'EARN_SIGNUP',         1000.00, 1000.00, '신규 가입 보상',             NULL,   NULL,   'DEV_SEED:signup:member:3',             'SUCCEEDED', DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY)),
(3, 'EARN_VOTE',             10.00, 1010.00, 'Battle 투표 참여 보상',      'BATTLE',800001,'DEV_SEED:vote:battle:800001:member:3', 'SUCCEEDED', DATE_SUB(NOW(), INTERVAL 1 DAY),  DATE_SUB(NOW(), INTERVAL 1 DAY)),
(4, 'EARN_SIGNUP',         1000.00, 1000.00, '신규 가입 보상',             NULL,   NULL,   'DEV_SEED:signup:member:4',             'SUCCEEDED', DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY)),
(4, 'EARN_VOTE',             10.00, 1010.00, 'Battle 투표 참여 보상',      'BATTLE',800005,'DEV_SEED:vote:battle:800005:member:4', 'SUCCEEDED', DATE_SUB(NOW(), INTERVAL 7 DAY),  DATE_SUB(NOW(), INTERVAL 7 DAY)),
(4, 'EARN_VOTE_WIN',         10.00, 1020.00, 'Battle 승리 진영 보상',      'BATTLE',800005,'DEV_SEED:settle:battle:800005:member:4','SUCCEEDED',DATE_SUB(NOW(), INTERVAL 1 DAY),  DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 'EARN_SIGNUP',         1000.00, 1000.00, '신규 가입 보상',             NULL,   NULL,   'DEV_SEED:signup:member:5',             'SUCCEEDED', DATE_SUB(NOW(), INTERVAL 17 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY)),
(5, 'EARN_VOTE',             10.00, 1010.00, 'Battle 투표 참여 보상',      'BATTLE',800007,'DEV_SEED:vote:battle:800007:member:5', 'SUCCEEDED', DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY));

-- ============================================================
-- 3. battle — CLOSED 미정산 배틀 추가
-- ============================================================
USE battle;

DELETE FROM battle_vote WHERE battle_id = 800011;
DELETE FROM battle WHERE id = 800011;

-- 800011: CLOSED + winning_option 확정, settled_at IS NULL (정산 대기 중)
INSERT INTO battle (id, title, option_a, option_b, status,
                    option_a_count, option_b_count, vote_count,
                    winning_option, reward_amount, settled_at,
                    sido, sigu, created_by, start_at, end_at,
                    deleted_at, created_at, updated_at)
VALUES
(800011, '한강 뚝섬 vs 반포, 피크닉 성지는?', '뚝섬', '반포', 'CLOSED',
 5, 3, 8, 'A', 0.00, NULL,
 '서울', '영등포구', 1,
 DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 1 HOUR),
 NULL, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW());

INSERT INTO battle_vote (battle_id, member_id, selected_option, is_rewarded, created_at, updated_at)
VALUES
(800011, 1, 'A', FALSE, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),
(800011, 2, 'A', FALSE, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(800011, 3, 'A', FALSE, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(800011, 4, 'A', FALSE, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),
(800011, 5, 'A', FALSE, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(800011, 6, 'B', FALSE, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(800011, 7, 'B', FALSE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(800011, 8, 'B', FALSE, DATE_SUB(NOW(), INTERVAL 12 HOUR), NOW());

-- 800012: 소프트 삭제된 배틀 (deleted_at IS NOT NULL)
DELETE FROM battle WHERE id = 800012;
INSERT INTO battle (id, title, option_a, option_b, status,
                    option_a_count, option_b_count, vote_count,
                    winning_option, reward_amount, settled_at,
                    sido, sigu, created_by, start_at, end_at,
                    deleted_at, created_at, updated_at)
VALUES
(800012, '[삭제됨] 관리자가 삭제한 배틀', '옵션A', '옵션B', 'CANCELLED',
 0, 0, 0, NULL, 0.00, NULL,
 '서울', '강남구', 999,
 DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY),
 NOW(), DATE_SUB(NOW(), INTERVAL 5 DAY), NOW());
