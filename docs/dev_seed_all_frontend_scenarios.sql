-- DEV ONLY
-- Frontend local test seed data — all services.
-- Do not run in production.
--
-- Covers:
--   memberpoint : member (1 updated, 2~10 + 101~106 inserted), point_history
--   battle      : battle (800001~800010), battle_vote, comment
--   insight     : reputation, visit_certification, insight_report
--
-- Run examples (Windows PowerShell):
--   Get-Content .\docs\dev_seed_all_frontend_scenarios.sql | docker exec -i todongsan-mysql mysql -uroot -p1234
--
-- Prerequisites:
--   Market seed: docs\market\sql\dev_seed_market_frontend_scenarios.sql
--   (Market seed uses member_id 101~106 — run this file first)
--
-- Dev member mapping:
--   member_id=1  : 실제 로그인 계정 (devMember) — 포인트 5000P, 서울 마포구
--   member_id=2  : 테스트유저_강남 — 서울 강남구
--   member_id=3  : 테스트유저_경기 — 경기 수원시
--   member_id=4  : 테스트유저_부산 — 부산 해운대구
--   member_id=5  : 테스트유저_대구 — 대구 중구
--   member_id=6~10: 추가 투표/댓글 시나리오용 더미 유저
--   member_id=101~106: Market 시드 호환용

-- ============================================================
-- memberpoint DB
-- ============================================================
USE memberpoint;

START TRANSACTION;

-- ── 재실행 안전 정리 ──────────────────────────────────────────
DELETE FROM point_history WHERE idempotency_key LIKE 'DEV_SEED:%';
DELETE FROM point_history WHERE member_id BETWEEN 2 AND 10;
DELETE FROM member WHERE id BETWEEN 2 AND 10;
DELETE FROM member WHERE id BETWEEN 101 AND 106;

-- ── member 1 갱신 (실제 로그인 계정) ────────────────────────
UPDATE member
SET point_balance       = 5000.00,
    residence_sido      = '서울',
    residence_sigu      = '마포구',
    residence_changed_at = DATE_SUB(NOW(), INTERVAL 10 DAY),
    age_group           = 'AGE_20S',
    gender              = 'MALE',
    updated_at          = NOW()
WHERE id = 1;

-- ── 테스트 멤버 2~10 ─────────────────────────────────────────
INSERT INTO member (id, nickname, point_balance, role,
                    residence_sido, residence_sigu, residence_changed_at,
                    age_group, gender,
                    oauth_provider, oauth_id,
                    created_at, updated_at)
VALUES
(2,  '테스트유저_강남',   2000.00, 'USER', '서울',  '강남구',   DATE_SUB(NOW(), INTERVAL 8 DAY),  'AGE_30S',      'FEMALE', 'KAKAO', 'TEST_KAKAO_002', DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),
(3,  '테스트유저_경기',   1500.00, 'USER', '경기',  '수원시',   DATE_SUB(NOW(), INTERVAL 7 DAY),  'AGE_20S',      'MALE',   'KAKAO', 'TEST_KAKAO_003', DATE_SUB(NOW(), INTERVAL 19 DAY), NOW()),
(4,  '테스트유저_부산',   3000.00, 'USER', '부산',  '해운대구', DATE_SUB(NOW(), INTERVAL 6 DAY),  'AGE_40S',      'FEMALE', 'KAKAO', 'TEST_KAKAO_004', DATE_SUB(NOW(), INTERVAL 18 DAY), NOW()),
(5,  '테스트유저_대구',   800.00,  'USER', '대구',  '중구',     DATE_SUB(NOW(), INTERVAL 5 DAY),  'AGE_50S_ABOVE','MALE',   'KAKAO', 'TEST_KAKAO_005', DATE_SUB(NOW(), INTERVAL 17 DAY), NOW()),
(6,  '테스트유저_인천',   500.00,  'USER', '인천',  '연수구',   DATE_SUB(NOW(), INTERVAL 4 DAY),  'AGE_20S',      'FEMALE', 'KAKAO', 'TEST_KAKAO_006', DATE_SUB(NOW(), INTERVAL 16 DAY), NOW()),
(7,  '테스트유저_광주',   1200.00, 'USER', '광주',  '서구',     DATE_SUB(NOW(), INTERVAL 3 DAY),  'AGE_30S',      'MALE',   'KAKAO', 'TEST_KAKAO_007', DATE_SUB(NOW(), INTERVAL 15 DAY), NOW()),
(8,  '테스트유저_대전',   900.00,  'USER', '대전',  '유성구',   DATE_SUB(NOW(), INTERVAL 3 DAY),  'AGE_20S',      'FEMALE', 'KAKAO', 'TEST_KAKAO_008', DATE_SUB(NOW(), INTERVAL 14 DAY), NOW()),
(9,  '테스트유저_울산',   600.00,  'USER', '울산',  '남구',     DATE_SUB(NOW(), INTERVAL 2 DAY),  'AGE_30S',      'MALE',   'KAKAO', 'TEST_KAKAO_009', DATE_SUB(NOW(), INTERVAL 13 DAY), NOW()),
(10, '테스트유저_세종',   400.00,  'USER', '세종',  '세종시',   DATE_SUB(NOW(), INTERVAL 1 DAY),  'AGE_20S',      'FEMALE', 'KAKAO', 'TEST_KAKAO_010', DATE_SUB(NOW(), INTERVAL 12 DAY), NOW());

-- ── Market 시드 호환 멤버 101~106 ────────────────────────────
INSERT INTO member (id, nickname, point_balance, role,
                    oauth_provider, oauth_id, created_at, updated_at)
VALUES
(101, '마켓테스터_1', 1000.00, 'USER', 'KAKAO', 'TEST_KAKAO_101', DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),
(102, '마켓테스터_2', 1000.00, 'USER', 'KAKAO', 'TEST_KAKAO_102', DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),
(103, '마켓테스터_3', 1000.00, 'USER', 'KAKAO', 'TEST_KAKAO_103', DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),
(104, '마켓테스터_4', 1000.00, 'USER', 'KAKAO', 'TEST_KAKAO_104', DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),
(105, '마켓테스터_5', 1000.00, 'USER', 'KAKAO', 'TEST_KAKAO_105', DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),
(106, '마켓테스터_6', 1000.00, 'USER', 'KAKAO', 'TEST_KAKAO_106', DATE_SUB(NOW(), INTERVAL 30 DAY), NOW());

-- ── 포인트 이력 (member 1) ───────────────────────────────────
INSERT INTO point_history (member_id, type, amount, balance_snapshot, reason,
                           reference_type, reference_id, idempotency_key, status,
                           created_at, updated_at)
VALUES
(1, 'EARN_SIGNUP',         1000.00, 1000.00, '신규 가입 보상',                    NULL, NULL, 'DEV_SEED:signup:member:1',           'SUCCEEDED', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY)),
(1, 'EARN_VOTE',             10.00, 1010.00, 'Battle 투표 참여 보상',             'BATTLE', 800001, 'DEV_SEED:vote:battle:800001:member:1', 'SUCCEEDED', DATE_SUB(NOW(), INTERVAL 5 DAY),  DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, 'EARN_COMMENT',           2.00, 1012.00, 'Battle 댓글 작성 보상',             'BATTLE', 800001, 'DEV_SEED:comment:1:member:1',          'SUCCEEDED', DATE_SUB(NOW(), INTERVAL 4 DAY),  DATE_SUB(NOW(), INTERVAL 4 DAY)),
(1, 'EARN_VOTE_WIN',          10.00, 1022.00, 'Battle 승리 진영 보상',            'BATTLE', 800005, 'DEV_SEED:settle:battle:800005:member:1','SUCCEEDED', DATE_SUB(NOW(), INTERVAL 3 DAY),  DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, 'SPEND_MARKET',         100.00, 922.00,  'Market 예측 참여 차감',             'MARKET', 900001, 'DEV_SEED:spend:market:900001:member:1', 'SUCCEEDED', DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY)),
(1, 'SETTLE_MARKET',        180.00, 1102.00, 'Market 정산 보상 (900006 승리)',    'MARKET', 900006, 'DEV_SEED:settle:market:900006:member:1','SUCCEEDED', DATE_SUB(NOW(), INTERVAL 2 DAY),  DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 'EARN_BATTLE_APPROVED',  20.00, 1122.00, 'Battle 주제 등록 승인 보상',        'BATTLE', 800001, 'DEV_SEED:approved:battle:800001:member:1','SUCCEEDED',DATE_SUB(NOW(), INTERVAL 6 DAY),  DATE_SUB(NOW(), INTERVAL 6 DAY)),
(1, 'SPEND_INSIGHT',         50.00, 1072.00, 'Insight 리포트 열람 차감',         NULL, NULL, 'DEV_SEED:insight:1:member:1',          'SUCCEEDED', DATE_SUB(NOW(), INTERVAL 1 DAY),  DATE_SUB(NOW(), INTERVAL 1 DAY));

COMMIT;

-- ============================================================
-- battle DB
-- ============================================================
USE battle;

START TRANSACTION;

-- ── 재실행 안전 정리 ──────────────────────────────────────────
DELETE FROM point_reward_retry_queue WHERE reference_id BETWEEN 800001 AND 800010;
DELETE FROM comment WHERE battle_id BETWEEN 800001 AND 800010;
DELETE FROM battle_vote WHERE battle_id BETWEEN 800001 AND 800010;
DELETE FROM battle WHERE id BETWEEN 800001 AND 800010;

-- ── Battle ───────────────────────────────────────────────────
INSERT INTO battle (id, title, option_a, option_b, status,
                    option_a_count, option_b_count, vote_count,
                    winning_option, reward_amount, settled_at,
                    sido, sigu, created_by, start_at, end_at,
                    deleted_at, created_at, updated_at)
VALUES
-- 800001: ACTIVE - 홍대 vs 연남 (서울 마포구) 투표 활발, member=1 이미 투표
(800001, '홍대 vs 연남, 주말 나들이 어디가 더 좋아요?', '홍대', '연남', 'ACTIVE',
 12, 9, 21, NULL, 0.00, NULL,
 '서울', '마포구', 1,
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),

-- 800002: ACTIVE - 강남역 vs 선릉역 (서울 강남구) 투표 적음, member=1 미투표
(800002, '강남역 vs 선릉역, 점심 맛집은 어디가 더 많을까?', '강남역', '선릉역', 'ACTIVE',
 3, 5, 8, NULL, 0.00, NULL,
 '서울', '강남구', 2,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),

-- 800003: ACTIVE - 수원갈비 지역 배틀 (경기 수원시) member=3 생성
(800003, '수원 왕갈비 vs 수원 닭갈비, 수원 명물은?', '왕갈비', '닭갈비', 'ACTIVE',
 7, 4, 11, NULL, 0.00, NULL,
 '경기', '수원시', 3,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),

-- 800004: ACTIVE - 광안리 vs 해운대 (부산 해운대구) 투표 0
(800004, '광안리 해수욕장 vs 해운대 해수욕장, 여름 피서지는?', '광안리', '해운대', 'ACTIVE',
 0, 0, 0, NULL, 0.00, NULL,
 '부산', '해운대구', 4,
 DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 8 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),

-- 800005: CLOSED + 정산완료 - A 승리 (서울 종로구)
(800005, '경복궁 vs 창덕궁, 외국 친구 데려가기 좋은 곳은?', '경복궁', '창덕궁', 'CLOSED',
 18, 10, 28, 'A', 10.00, DATE_SUB(NOW(), INTERVAL 1 DAY),
 '서울', '종로구', 1,
 DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),

-- 800006: CLOSED + 정산완료 - B 승리 (서울 강남구)
(800006, '강남 치킨 vs 강남 피자, 야식 최강자는?', '치킨', '피자', 'CLOSED',
 6, 14, 20, 'B', 10.00, DATE_SUB(NOW(), INTERVAL 2 DAY),
 '서울', '강남구', 2,
 DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 11 DAY), NOW()),

-- 800007: CLOSED + 정산완료 - DRAW (대구 중구)
(800007, '동성로 vs 반월당, 대구 중심가는 어디?', '동성로', '반월당', 'CLOSED',
 8, 8, 16, 'DRAW', 0.00, DATE_SUB(NOW(), INTERVAL 3 DAY),
 '대구', '중구', 5,
 DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 13 DAY), NOW()),

-- 800008: ACTIVE - 성수동 카페 배틀 (서울 성동구) start_at 미래 → 투표 불가
(800008, '성수동 카페 vs 뚝섬 카페, 인스타 감성은?', '성수동 카페', '뚝섬 카페', 'ACTIVE',
 0, 0, 0, NULL, 0.00, NULL,
 '서울', '성동구', 1,
 DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 9 DAY),
 NULL, NOW(), NOW()),

-- 800009: PENDING - 관리자 승인 대기
(800009, '[검수 대기] 한강 공원 vs 북한산, 주말 산책 코스는?', '한강 공원', '북한산', 'PENDING',
 0, 0, 0, NULL, 0.00, NULL,
 '서울', '은평구', 1,
 DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 8 DAY),
 NULL, NOW(), NOW()),

-- 800010: CANCELLED
(800010, '[취소됨] 인천 차이나타운 vs 신포국제시장', '차이나타운', '신포국제시장', 'CANCELLED',
 0, 0, 0, NULL, 0.00, NULL,
 '인천', '미추홀구', 6,
 DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW());

-- ── Battle Vote ──────────────────────────────────────────────
-- 800001 ACTIVE - member=1 A 투표, 여러 멤버 투표
INSERT INTO battle_vote (battle_id, member_id, selected_option, is_rewarded, created_at, updated_at) VALUES
(800001, 1, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(800001, 2, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(800001, 3, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(800001, 4, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(800001, 5, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(800001, 6, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(800001, 7, 'A', TRUE,  NOW(), NOW()),
(800001, 8, 'A', TRUE,  NOW(), NOW()),
(800001, 9, 'A', TRUE,  NOW(), NOW()),
(800001, 10,'A', TRUE,  NOW(), NOW()),
(800001, 101,'A', TRUE, NOW(), NOW()),
(800001, 102,'B', TRUE, NOW(), NOW()),
(800001, 103,'B', TRUE, NOW(), NOW()),
(800001, 104,'B', TRUE, NOW(), NOW()),
(800001, 105,'B', TRUE, NOW(), NOW()),
(800001, 106,'B', TRUE, NOW(), NOW()),
-- 800002 ACTIVE - member=1 미투표
(800002, 2, 'B', TRUE,  DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(800002, 3, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(800002, 4, 'B', TRUE,  NOW(), NOW()),
(800002, 5, 'A', TRUE,  NOW(), NOW()),
(800002, 6, 'B', TRUE,  NOW(), NOW()),
(800002, 7, 'B', TRUE,  NOW(), NOW()),
(800002, 8, 'B', TRUE,  NOW(), NOW()),
(800002, 9, 'A', TRUE,  NOW(), NOW()),
-- 800003 ACTIVE
(800003, 1, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(800003, 3, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(800003, 4, 'A', TRUE,  NOW(), NOW()),
(800003, 6, 'A', TRUE,  NOW(), NOW()),
(800003, 7, 'A', TRUE,  NOW(), NOW()),
(800003, 8, 'A', TRUE,  NOW(), NOW()),
(800003, 9, 'A', TRUE,  NOW(), NOW()),
(800003, 2, 'B', TRUE,  NOW(), NOW()),
(800003, 5, 'B', TRUE,  NOW(), NOW()),
(800003, 10,'B', TRUE,  NOW(), NOW()),
(800003, 101,'B', TRUE, NOW(), NOW()),
-- 800005 CLOSED A 승리 - member=1 A 투표
(800005, 1, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(800005, 2, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(800005, 3, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(800005, 4, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(800005, 5, 'B', TRUE,  DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(800005, 6, 'B', TRUE,  DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
-- 800006 CLOSED B 승리 - member=1 A 투표 (패배)
(800006, 1, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(800006, 2, 'B', TRUE,  DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(800006, 3, 'B', TRUE,  DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(800006, 4, 'B', TRUE,  DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(800006, 5, 'B', TRUE,  DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
-- 800007 CLOSED DRAW
(800007, 1, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(800007, 2, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(800007, 3, 'B', TRUE,  DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(800007, 4, 'B', TRUE,  DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));

-- ── Comment ──────────────────────────────────────────────────
-- battle 800001
INSERT INTO comment (battle_id, member_id, content, deleted_at, created_at, updated_at) VALUES
(800001, 1, '홍대는 클럽 문화랑 감성 카페가 정말 독특하죠! 연남은 요즘 트렌디한 식당이 많아요.', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(800001, 2, '연남동은 조용하고 골목 산책하기 딱 좋아요. 홍대보다 덜 북적여서 저는 연남파!', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(800001, 3, '홍대 근처 독립서점이랑 빈티지샵이 너무 좋아서 홍대에 한 표요.', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(800001, 4, '저는 해산물 좋아해서 연남동 해산물 거리 자주 가는데, 정말 맛있어요!', NULL, DATE_SUB(NOW(), INTERVAL 12 HOUR), NOW()),
(800001, 5, '두 곳 다 좋지만 지하철 접근성은 홍대가 훨씬 낫죠.', NULL, DATE_SUB(NOW(), INTERVAL 6 HOUR), NOW()),
(800001, 1, '이 댓글은 삭제된 댓글 테스트용입니다.', NOW(), DATE_SUB(NOW(), INTERVAL 5 HOUR), NOW()),
-- battle 800002
(800002, 2, '선릉역 주변에 숨은 맛집들이 의외로 많아요. 회사원들이 많아서 점심 퀄리티가 높음!', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(800002, 3, '강남역은 프랜차이즈가 너무 많아서 개인 맛집 찾기가 오히려 선릉이 낫더라고요.', NULL, DATE_SUB(NOW(), INTERVAL 12 HOUR), NOW()),
-- battle 800003
(800003, 3, '수원 왕갈비는 진짜 두꺼운 갈비살이 일품! 수원 여행 왔다면 무조건 왕갈비죠.', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(800003, 1, '닭갈비도 맵고 짭조름한 게 맥주랑 진짜 잘 어울려요. 둘 다 맛있어서 고민되네요.', NULL, DATE_SUB(NOW(), INTERVAL 6 HOUR), NOW()),
-- battle 800005 CLOSED
(800005, 1, '경복궁은 규모가 압도적이고 야경도 진짜 아름다워요. 외국 친구들이 다들 감탄했어요!', NULL, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),
(800005, 3, '창덕궁 후원은 사전 예약이 필요하지만 그 고즈넉한 분위기는 정말 특별해요.', NULL, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW());

COMMIT;

-- ============================================================
-- insight DB
-- ============================================================
USE insight;

START TRANSACTION;

-- ── 재실행 안전 정리 ──────────────────────────────────────────
DELETE FROM insight_report WHERE id BETWEEN 700001 AND 700010;
DELETE FROM visit_certification WHERE member_id BETWEEN 1 AND 10;
DELETE FROM reputation WHERE member_id BETWEEN 1 AND 10;

-- ── Reputation ───────────────────────────────────────────────
INSERT INTO reputation (member_id, activity_score, prediction_count, prediction_correct,
                        prediction_accuracy, residence_sido, residence_sigu,
                        residence_declared_at, residence_changed_at,
                        activity_count, activity_confirmed_at, created_at, updated_at)
VALUES
(1, 85, 6, 4, 66.67, '서울', '마포구',  DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), 12, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),
(2, 62, 4, 2, 50.00, '서울', '강남구',  DATE_SUB(NOW(), INTERVAL 8 DAY),  DATE_SUB(NOW(), INTERVAL 8 DAY),  8,  DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),
(3, 40, 3, 1, 33.33, '경기', '수원시',  DATE_SUB(NOW(), INTERVAL 7 DAY),  DATE_SUB(NOW(), INTERVAL 7 DAY),  5,  DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY), NOW()),
(4, 75, 5, 4, 80.00, '부산', '해운대구',DATE_SUB(NOW(), INTERVAL 6 DAY),  DATE_SUB(NOW(), INTERVAL 6 DAY),  10, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY), NOW()),
(5, 30, 2, 0, 0.00,  '대구', '중구',    DATE_SUB(NOW(), INTERVAL 5 DAY),  DATE_SUB(NOW(), INTERVAL 5 DAY),  3,  DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY), NOW()),
(6, 15, 1, 0, 0.00,  '인천', '연수구',  DATE_SUB(NOW(), INTERVAL 4 DAY),  DATE_SUB(NOW(), INTERVAL 4 DAY),  2,  DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY), NOW()),
(7, 50, 3, 2, 66.67, '광주', '서구',    DATE_SUB(NOW(), INTERVAL 3 DAY),  DATE_SUB(NOW(), INTERVAL 3 DAY),  7,  DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), NOW()),
(8, 20, 1, 1, 100.00,'대전', '유성구',  DATE_SUB(NOW(), INTERVAL 3 DAY),  DATE_SUB(NOW(), INTERVAL 3 DAY),  3,  DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY), NOW()),
(9, 10, 0, 0, 0.00,  '울산', '남구',    DATE_SUB(NOW(), INTERVAL 2 DAY),  DATE_SUB(NOW(), INTERVAL 2 DAY),  1,  DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY), NOW()),
(10, 5, 0, 0, 0.00,  '세종', '세종시',  DATE_SUB(NOW(), INTERVAL 1 DAY),  DATE_SUB(NOW(), INTERVAL 1 DAY),  0,  NULL,                            DATE_SUB(NOW(), INTERVAL 12 DAY), NOW());

-- ── Visit Certification ──────────────────────────────────────
INSERT INTO visit_certification (member_id, sido, sigu, method,
                                  latitude, longitude, comment_content, battle_id,
                                  last_certified_at, certified_at, created_at, updated_at)
VALUES
-- member 1: GPS 인증 (마포구), 댓글 인증 (종로구)
(1, '서울', '마포구',   'GPS',     37.55695,  126.92426, NULL,                              NULL,   DATE_SUB(NOW(), INTERVAL 5 DAY),  DATE_SUB(NOW(), INTERVAL 5 DAY),  DATE_SUB(NOW(), INTERVAL 5 DAY),  NOW()),
(1, '서울', '종로구',   'COMMENT', NULL,       NULL,      '경복궁 야경 진짜 아름다워요!',    800005, DATE_SUB(NOW(), INTERVAL 7 DAY),  DATE_SUB(NOW(), INTERVAL 7 DAY),  DATE_SUB(NOW(), INTERVAL 7 DAY),  NOW()),
-- member 2: GPS 인증 (강남구)
(2, '서울', '강남구',   'GPS',     37.49794,  127.02759, NULL,                              NULL,   DATE_SUB(NOW(), INTERVAL 4 DAY),  DATE_SUB(NOW(), INTERVAL 4 DAY),  DATE_SUB(NOW(), INTERVAL 4 DAY),  NOW()),
-- member 3: GPS 인증 (수원시)
(3, '경기', '수원시',   'GPS',     37.26337,  127.02856, NULL,                              NULL,   DATE_SUB(NOW(), INTERVAL 3 DAY),  DATE_SUB(NOW(), INTERVAL 3 DAY),  DATE_SUB(NOW(), INTERVAL 3 DAY),  NOW()),
-- member 4: 댓글 인증 (해운대구)
(4, '부산', '해운대구', 'GPS',     35.15875,  129.16019, NULL,                              NULL,   DATE_SUB(NOW(), INTERVAL 2 DAY),  DATE_SUB(NOW(), INTERVAL 2 DAY),  DATE_SUB(NOW(), INTERVAL 2 DAY),  NOW()),
-- member 5: GPS 인증 (대구 중구)
(5, '대구', '중구',     'GPS',     35.86884,  128.60648, NULL,                              NULL,   DATE_SUB(NOW(), INTERVAL 1 DAY),  DATE_SUB(NOW(), INTERVAL 1 DAY),  DATE_SUB(NOW(), INTERVAL 1 DAY),  NOW());

-- ── Insight Report ───────────────────────────────────────────
INSERT INTO insight_report (id, type, reference_id, status, summary, analysis_data,
                             raw_prompt, failed_reason,
                             processing_started_at, retry_count,
                             requested_by, report_content, generated_at,
                             created_at, updated_at)
VALUES
-- Battle 800005 리포트 (A 승리 — 경복궁 vs 창덕궁)
(700001, 'BATTLE', 800005, 'COMPLETED',
 '경복궁 66.7% 승리. 접근성·랜드마크 인지도가 주요 요인. 20~30대 비율 79%.',
 JSON_OBJECT(
     'totalVotes', 28,
     'optionAVotes', 18, 'optionBVotes', 10,
     'winningOption', 'A',
     'ageDistribution', JSON_OBJECT('AGE_20S', 12, 'AGE_30S', 10, 'AGE_40S', 4, 'AGE_50S_ABOVE', 2),
     'genderDistribution', JSON_OBJECT('MALE', 15, 'FEMALE', 13),
     'regionDistribution', JSON_OBJECT('서울', 16, '경기', 5, '부산', 4, '기타', 3)
 ),
 'battle_id=800005 분석 요청',
 NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), 0,
 1,
 '경복궁 66.7%(18표) vs 창덕궁 33.3%(10표). 3·5호선 접근성과 랜드마크 인지도가 결정적. 20~30대 79% 차지.',
 DATE_SUB(NOW(), INTERVAL 1 DAY),
 DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),

-- Market 900006 리포트 (SETTLED — 서울 아파트 매매가격지수)
(700002, 'MARKET', 900006, 'COMPLETED',
 '서울 아파트 매매가격지수 상승(YES) 판정. YES 예측자 180P 정산. 예측 정확도 50%.',
 JSON_OBJECT(
     'resultOption', 'YES',
     'totalPool', 200.00,
     'settlementPool', 180.00,
     'feeRate', 10.0,
     'participantCount', 2,
     'yesCount', 1, 'noCount', 1,
     'winnerSettledAmount', 180.00
 ),
 'market_id=900006 분석 요청',
 NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), 0,
 1,
 '서울 아파트 매매가격지수 +0.08% 상승 판정. YES 100P 참여자 180P 수령(수수료 10%). 강남·서초·송파 매수세 유지.',
 DATE_SUB(NOW(), INTERVAL 2 DAY),
 DATE_SUB(NOW(), INTERVAL 3 DAY), NOW());

COMMIT;

-- ============================================================
-- 확인 쿼리 (실행 후 검증)
-- ============================================================
-- SELECT id, nickname, point_balance, residence_sido, residence_sigu FROM memberpoint.member ORDER BY id;
-- SELECT id, title, status, vote_count, sido, sigu FROM battle.battle WHERE id BETWEEN 800001 AND 800010;
-- SELECT battle_id, COUNT(*) AS votes FROM battle.battle_vote WHERE battle_id BETWEEN 800001 AND 800010 GROUP BY battle_id;
-- SELECT member_id, activity_score, prediction_count, prediction_accuracy FROM insight.reputation ORDER BY member_id;
-- SELECT id, type, reference_id, status FROM insight.insight_report WHERE id BETWEEN 700001 AND 700010;
