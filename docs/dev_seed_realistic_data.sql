SET NAMES utf8mb4;
-- DEV ONLY — 현실적인 Market/Battle 시드 데이터
-- 실제 사용자가 사용하는 것처럼 구성한 데이터. [DEV] 접두어 없음.
--
-- Prerequisites (반드시 먼저 실행):
--   dev_seed_all_frontend_scenarios.sql
--   dev_seed_supplement.sql
--   docs/market/sql/dev_seed_market_frontend_scenarios.sql
--
-- ID 범위 (기존 시드와 충돌 없음):
--   battle              : 801001~801015
--   market              : 902001~902012
--   market_option       : 912001~912026
--   market_prediction   : 922001~922027
--   market_price_history: 927001~927004
--   market_settlement   : 932001~932002
--   market_settle_detail: 942001~942004
--   market_comment      : 980001~980005
--   market_rep_update   : 962001~962004
--
-- 사용 멤버 (기존):
--   1:서울마포구, 2:서울강남구, 3:경기수원시, 4:부산해운대구, 5:대구중구
--   6:인천연수구, 7:광주서구,   8:대전유성구,  9:울산남구,     10:세종세종시
--   101~106: 마켓테스터
--
-- 실행 (bash / Git Bash):
--   cat docs/dev_seed_realistic_data.sql | docker exec -i todongsan-mysql mysql -uroot -p1234 --default-character-set=utf8mb4
--
-- 실행 (Windows PowerShell):
--   Get-Content .\docs\dev_seed_realistic_data.sql | docker exec -i todongsan-mysql mysql -uroot -p1234 --default-character-set=utf8mb4

-- ============================================================
-- 1. BATTLE DB
-- ============================================================
USE battle;

START TRANSACTION;

DELETE FROM comment     WHERE battle_id BETWEEN 801001 AND 801015;
DELETE FROM battle_vote WHERE battle_id BETWEEN 801001 AND 801015;
DELETE FROM battle      WHERE id        BETWEEN 801001 AND 801015;

INSERT INTO battle (id, title, option_a, option_b, status,
                    option_a_count, option_b_count, vote_count,
                    winning_option, reward_amount, settled_at,
                    sido, sigu, created_by, start_at, end_at,
                    deleted_at, created_at, updated_at)
VALUES
(801001, '한남동 카페 골목 vs 이태원 세계음식 거리, 주말 데이트는?',
 '한남동 카페 골목', '이태원 세계음식 거리', 'ACTIVE',
 4, 3, 7, NULL, 0.00, NULL,
 '서울', '용산구', 1,
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),

(801002, '성수동 핫플 vs 왕십리 야시장, 저녁 나들이 핫스팟은?',
 '성수동 핫플', '왕십리 야시장', 'ACTIVE',
 4, 2, 6, NULL, 0.00, NULL,
 '서울', '성동구', 2,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),

(801003, '일산 호수공원 산책 vs 행주산성 트레킹, 고양시 주말 코스는?',
 '일산 호수공원', '행주산성', 'ACTIVE',
 3, 1, 4, NULL, 0.00, NULL,
 '경기', '고양시', 3,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),

(801004, '광안대교 야경 vs 오륙도 스카이워크, 부산 필수 관광지는?',
 '광안대교 야경', '오륙도 스카이워크', 'ACTIVE',
 2, 2, 4, NULL, 0.00, NULL,
 '부산', '남구', 4,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),

(801005, '반포 한강공원 vs 잠원 한강공원, 주말 치맥 성지는?',
 '반포 한강공원', '잠원 한강공원', 'ACTIVE',
 4, 2, 6, NULL, 0.00, NULL,
 '서울', '서초구', 2,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),

(801006, '판교 테크노밸리 카페 vs 분당 정자역 로데오, 카공족 성지는?',
 '판교 테크노밸리', '분당 정자역 로데오', 'ACTIVE',
 3, 2, 5, NULL, 0.00, NULL,
 '경기', '성남시', 3,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),

(801007, '인천 차이나타운 짜장면 vs 신포국제시장 닭강정, 인천 대표 먹거리는?',
 '차이나타운 짜장면', '신포국제시장 닭강정', 'ACTIVE',
 2, 2, 4, NULL, 0.00, NULL,
 '인천', '중구', 6,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),

(801008, '망원시장 vs 공덕시장, 마포 주민 단골 전통시장은?',
 '망원시장', '공덕시장', 'ACTIVE',
 3, 2, 5, NULL, 0.00, NULL,
 '서울', '마포구', 1,
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),

(801009, '계룡산 국립공원 등산 vs 갑천 시민공원 자전거, 대전 힐링 코스는?',
 '계룡산 국립공원', '갑천 시민공원', 'ACTIVE',
 2, 2, 4, NULL, 0.00, NULL,
 '대전', '유성구', 8,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),

(801010, '북한산 둘레길 트레킹 vs 수유계곡 물놀이, 강북 자연 힐링은?',
 '북한산 둘레길', '수유계곡', 'ACTIVE',
 3, 2, 5, NULL, 0.00, NULL,
 '서울', '강북구', 1,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),

-- CLOSED 정산완료 A승리: 인사동 vs 익선동
(801011, '인사동 전통문화 거리 vs 익선동 한옥 카페촌, 종로 핫플은?',
 '인사동', '익선동', 'CLOSED',
 4, 3, 7, 'A', 10.00, DATE_SUB(NOW(), INTERVAL 2 DAY),
 '서울', '종로구', 1,
 DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 11 DAY), NOW()),

-- CLOSED 정산완료 B승리: 수원화성 vs 광교호수공원
(801012, '수원화성 역사투어 vs 광교호수공원 데이트, 수원 나들이 코스는?',
 '수원화성 역사투어', '광교호수공원 데이트', 'CLOSED',
 2, 3, 5, 'B', 10.00, DATE_SUB(NOW(), INTERVAL 1 DAY),
 '경기', '수원시', 3,
 DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),

-- CLOSED DRAW: 서면 vs 부전시장
(801013, '서면 쇼핑몰 vs 부전시장 먹자골목, 부산진구 대표 상권은?',
 '서면 쇼핑몰', '부전시장 먹자골목', 'CLOSED',
 2, 2, 4, 'DRAW', 0.00, DATE_SUB(NOW(), INTERVAL 3 DAY),
 '부산', '부산진구', 4,
 DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),

-- PENDING 검수 대기: 은평구
(801014, '은평 한옥마을 문화체험 vs 불광천 산책로, 은평구 힐링 명소는?',
 '은평 한옥마을', '불광천 산책로', 'PENDING',
 0, 0, 0, NULL, 0.00, NULL,
 '서울', '은평구', 1,
 DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 8 DAY),
 NULL, NOW(), NOW()),

-- ACTIVE: 제주 동문 vs 서문
(801015, '제주 동문시장 야시장 vs 서문시장 오메기떡, 제주 전통시장 맛집은?',
 '동문시장 야시장', '서문시장 오메기떡', 'ACTIVE',
 2, 1, 3, NULL, 0.00, NULL,
 '제주', '제주시', 4,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY),
 NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW());

-- ── 투표 ─────────────────────────────────────────────────────

-- 801001 한남동(A) 4 : 이태원(B) 3
INSERT INTO battle_vote (battle_id, member_id, selected_option, is_rewarded, created_at, updated_at) VALUES
(801001, 1,   'A', TRUE, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(801001, 2,   'A', TRUE, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(801001, 7,   'A', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801001, 101, 'A', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801001, 4,   'B', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801001, 102, 'B', TRUE, NOW(), NOW()),
(801001, 103, 'B', TRUE, NOW(), NOW());

-- 801002 성수동(A) 4 : 왕십리(B) 2
INSERT INTO battle_vote (battle_id, member_id, selected_option, is_rewarded, created_at, updated_at) VALUES
(801002, 1, 'A', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801002, 2, 'A', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801002, 6, 'A', TRUE, NOW(), NOW()),
(801002, 8, 'A', TRUE, NOW(), NOW()),
(801002, 3, 'B', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801002, 5, 'B', TRUE, NOW(), NOW());

-- 801003 일산(A) 3 : 행주(B) 1
INSERT INTO battle_vote (battle_id, member_id, selected_option, is_rewarded, created_at, updated_at) VALUES
(801003, 3,  'A', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801003, 7,  'A', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801003, 10, 'A', TRUE, NOW(), NOW()),
(801003, 9,  'B', TRUE, NOW(), NOW());

-- 801004 광안대교(A) 2 : 오륙도(B) 2
INSERT INTO battle_vote (battle_id, member_id, selected_option, is_rewarded, created_at, updated_at) VALUES
(801004, 4, 'A', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801004, 2, 'A', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801004, 5, 'B', TRUE, NOW(), NOW()),
(801004, 6, 'B', TRUE, NOW(), NOW());

-- 801005 반포(A) 4 : 잠원(B) 2
INSERT INTO battle_vote (battle_id, member_id, selected_option, is_rewarded, created_at, updated_at) VALUES
(801005, 2,   'A', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801005, 4,   'A', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801005, 101, 'A', TRUE, NOW(), NOW()),
(801005, 102, 'A', TRUE, NOW(), NOW()),
(801005, 1,   'B', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801005, 103, 'B', TRUE, NOW(), NOW());

-- 801006 판교(A) 3 : 분당(B) 2
INSERT INTO battle_vote (battle_id, member_id, selected_option, is_rewarded, created_at, updated_at) VALUES
(801006, 3,  'A', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801006, 6,  'A', TRUE, NOW(), NOW()),
(801006, 10, 'A', TRUE, NOW(), NOW()),
(801006, 8,  'B', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801006, 9,  'B', TRUE, NOW(), NOW());

-- 801007 차이나타운(A) 2 : 닭강정(B) 2
INSERT INTO battle_vote (battle_id, member_id, selected_option, is_rewarded, created_at, updated_at) VALUES
(801007, 6, 'A', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801007, 4, 'A', TRUE, NOW(), NOW()),
(801007, 1, 'B', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801007, 2, 'B', TRUE, NOW(), NOW());

-- 801008 망원시장(A) 3 : 공덕시장(B) 2
INSERT INTO battle_vote (battle_id, member_id, selected_option, is_rewarded, created_at, updated_at) VALUES
(801008, 1, 'A', TRUE, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(801008, 2, 'A', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801008, 7, 'A', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801008, 3, 'B', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801008, 8, 'B', TRUE, NOW(), NOW());

-- 801009 계룡산(A) 2 : 갑천(B) 2
INSERT INTO battle_vote (battle_id, member_id, selected_option, is_rewarded, created_at, updated_at) VALUES
(801009, 8, 'A', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801009, 3, 'A', TRUE, NOW(), NOW()),
(801009, 5, 'B', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801009, 9, 'B', TRUE, NOW(), NOW());

-- 801010 북한산(A) 3 : 수유계곡(B) 2
INSERT INTO battle_vote (battle_id, member_id, selected_option, is_rewarded, created_at, updated_at) VALUES
(801010, 1,  'A', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801010, 2,  'A', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801010, 3,  'A', TRUE, NOW(), NOW()),
(801010, 7,  'B', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801010, 10, 'B', TRUE, NOW(), NOW());

-- 801011 인사동(A) 4 승리 : 익선동(B) 3
INSERT INTO battle_vote (battle_id, member_id, selected_option, is_rewarded, created_at, updated_at) VALUES
(801011, 1, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(801011, 2, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(801011, 5, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(801011, 7, 'A', TRUE,  DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(801011, 3, 'B', FALSE, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(801011, 4, 'B', FALSE, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(801011, 6, 'B', FALSE, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY));

-- 801012 수원화성(A) 2 : 광교(B) 3 승리
INSERT INTO battle_vote (battle_id, member_id, selected_option, is_rewarded, created_at, updated_at) VALUES
(801012, 1, 'A', FALSE, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(801012, 4, 'A', FALSE, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(801012, 3, 'B', TRUE,  DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(801012, 8, 'B', TRUE,  DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(801012, 9, 'B', TRUE,  DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 801013 DRAW 서면(A) 2 : 부전(B) 2
INSERT INTO battle_vote (battle_id, member_id, selected_option, is_rewarded, created_at, updated_at) VALUES
(801013, 4, 'A', TRUE, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(801013, 5, 'A', TRUE, DATE_SUB(NOW(), INTERVAL 9 DAY),  DATE_SUB(NOW(), INTERVAL 3 DAY)),
(801013, 6, 'B', TRUE, DATE_SUB(NOW(), INTERVAL 9 DAY),  DATE_SUB(NOW(), INTERVAL 3 DAY)),
(801013, 9, 'B', TRUE, DATE_SUB(NOW(), INTERVAL 8 DAY),  DATE_SUB(NOW(), INTERVAL 3 DAY));

-- 801015 동문(A) 2 : 서문(B) 1
INSERT INTO battle_vote (battle_id, member_id, selected_option, is_rewarded, created_at, updated_at) VALUES
(801015, 4,  'A', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801015, 10, 'A', TRUE, NOW(), NOW()),
(801015, 7,  'B', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW());

-- ── 댓글 ─────────────────────────────────────────────────────

INSERT INTO comment (battle_id, member_id, content, deleted_at, created_at, updated_at) VALUES
-- 801001 한남동 vs 이태원
(801001, 1,  '한남동 테라스 카페 뷰가 정말 압도적이에요. 루프탑에서 한강 보이는 카페들이 모여있어서 데이트 코스로는 최고예요.', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(801001, 2,  '이태원은 세계 각국 요리를 한 자리에서 즐길 수 있어서 매력이 달라요. 미국식 버거부터 중동 요리까지 다 있으니까요.', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801001, 4,  '저는 이태원 골목 숨어있는 로컬 바들이 너무 좋더라고요. 글로벌 분위기는 한남동보다 이태원이 확실해요.', NULL, DATE_SUB(NOW(), INTERVAL 12 HOUR), NOW()),

-- 801002 성수동 vs 왕십리
(801002, 1,  '성수동 수제화 골목에서 카페 한 잔하고 갤러리 구경하는 코스가 서울에서 제일 힙해요. 강추합니다!', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801002, 6,  '왕십리 광장시장 닭발이 야식으로는 진짜 최고예요. 저녁 먹방 투어는 왕십리가 완승 아닌가요?', NULL, DATE_SUB(NOW(), INTERVAL 6 HOUR), NOW()),

-- 801005 반포 vs 잠원
(801005, 2,  '반포 분수대 쇼 보면서 치맥 먹는 게 서울 여름 버킷리스트죠. 야경도 너무 예뻐서 외국 친구들한테도 자주 데려가요.', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801005, 7,  '잠원 쪽은 주말에도 의외로 자리가 남아 있어서 돗자리 치고 여유롭게 피크닉하기 훨씬 편해요. 반포는 너무 사람 많음.', NULL, DATE_SUB(NOW(), INTERVAL 6 HOUR), NOW()),

-- 801006 판교 vs 분당
(801006, 3,  '판교는 IT 기업들이 많아서 평일 점심에 카페 자리 잡기가 경쟁이에요. 그래도 커피 퀄리티는 확실히 높죠.', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801006, 8,  '분당 정자역 로데오는 주말에 걷기 좋고, 분위기 있는 카페들이 즐비해서 여유로운 카공하기 딱 좋아요.', NULL, DATE_SUB(NOW(), INTERVAL 6 HOUR), NOW()),

-- 801008 망원시장 vs 공덕시장 (member 1 거주지)
(801008, 1,  '망원시장은 수제 음식들이 정말 다양하고 신선해요. 마포 살면서 주말마다 거의 들리는데, 갈 때마다 새로운 가게가 생겨있어요.', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(801008, 3,  '공덕 쪽도 직장인들 입맛 맞게 다양한 메뉴가 있어요. 가성비 국밥이나 순대국 골목이 특히 훌륭해요.', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),

-- 801011 인사동 vs 익선동 (CLOSED)
(801011, 1,  '인사동은 전통 공예품이나 한국화 갤러리가 많아서 외국 친구들 데려가기 진짜 좋아요. 한국 문화 느끼기엔 최고죠.', NULL, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(801011, 2,  '익선동은 낡은 한옥에 현대 감성이 섞인 게 정말 매력적이에요. 인스타 사진 찍기는 익선동 완승인데, 전통 문화 체험은 인사동이죠.', NULL, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(801011, 4,  '두 곳 다 종로에서 도보권인데 분위기가 완전 달라요. 조용하게 쇼핑하고 싶다면 인사동, 힙하게 카페 투어하고 싶다면 익선동이죠.', NULL, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),

-- 801012 수원화성 vs 광교 (CLOSED)
(801012, 3,  '광교호수공원 저녁 산책로가 정말 잘 조성되어 있어요. 수변 카페들도 많아서 데이트 코스로 가성비 최고예요.', NULL, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(801012, 1,  '수원화성은 유네스코 세계문화유산이라 역사적 가치가 남다르죠. 외국인한테 한국 역사 알려주기엔 화성이 압도적이에요.', NULL, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),

-- 801015 제주 동문 vs 서문
(801015, 4,  '동문시장 야시장은 제주 여행 온 사람이라면 무조건 한 번은 가야 해요. 오메기떡부터 흑돼지 구이까지 다 있어요!', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(801015, 7,  '서문시장 오메기떡은 제주도 정겨움이 그대로 담겨있어요. 관광객 많은 동문보다 서문이 더 로컬 느낌 나서 좋아요.', NULL, DATE_SUB(NOW(), INTERVAL 6 HOUR), NOW());

COMMIT;

-- ============================================================
-- 2. MARKET DB
-- ============================================================
USE market;

START TRANSACTION;

DELETE FROM market_reputation_update WHERE id BETWEEN 962001 AND 962010;
DELETE FROM market_settlement_detail  WHERE id BETWEEN 942001 AND 942010;
DELETE FROM market_settlement         WHERE id BETWEEN 932001 AND 932010;
DELETE FROM market_price_history      WHERE id BETWEEN 927001 AND 927010;
DELETE FROM market_comment            WHERE id BETWEEN 980001 AND 980010;
DELETE FROM market_prediction         WHERE id BETWEEN 922001 AND 922030;
DELETE FROM market_option             WHERE id BETWEEN 912001 AND 912030;
DELETE FROM market                    WHERE id BETWEEN 902001 AND 902015;

-- ── Market ───────────────────────────────────────────────────
-- pool math 요약
--   ACTIVE  : fee_amount=0, settlement_pool=0 (아직 정산 전)
--   SETTLED : fee_rate=5%, fee_amount=total_pool*0.05, settlement_pool=total_pool-fee_amount
--   CLOSED  : fee_amount=0 (정산 대기)

INSERT INTO market (
    id, title, description, category, answer_type, metric_unit,
    judge_data_source, judge_criteria, judge_date, status,
    close_at, settle_due_at, settled_at, result_option_id, result_value, result_text,
    total_pool, fee_rate, fee_amount, settlement_pool,
    initial_virtual_liquidity, price_model, created_by, deleted_at, created_at, updated_at
) VALUES
-- 902001: 강남구 YES/NO ACTIVE - 가장 활발한 마켓 (6명 참여)
(902001, '이번 주 서울 강남구 아파트 매매가격지수, 상승할까요?',
 '한국부동산원 주간 데이터 기준. 강남3구 중 강남구 단독 지수 기준입니다.',
 'PRICE_INDEX', 'YES_NO', 'PERCENT',
 '한국부동산원 주간 아파트 가격동향',
 '서울 강남구 아파트 매매가격지수 주간 변동률이 0보다 크면 YES',
 DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'ACTIVE',
 DATE_ADD(NOW(), INTERVAL 6 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY),
 NULL, NULL, NULL, NULL,
 1000.00, 5.00, 0.00, 0.00, 500.00, 'POOL_SHARE', 2, NULL,
 DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),

-- 902002: 경기도 MULTIPLE_CHOICE ACTIVE
(902002, '이번 달 경기도 아파트 매매가격지수 변동률, 어느 구간일까요?',
 '3개 구간 중 하나를 예측하는 마켓입니다. 한국부동산원 월간 데이터 기준.',
 'PRICE_INDEX', 'MULTIPLE_CHOICE', 'PERCENT',
 '한국부동산원 월간 아파트 가격동향',
 '경기도 아파트 매매가격지수 월간 변동률 구간 판정',
 DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'ACTIVE',
 DATE_ADD(NOW(), INTERVAL 14 DAY), DATE_ADD(NOW(), INTERVAL 15 DAY),
 NULL, NULL, NULL, NULL,
 200.00, 5.00, 0.00, 0.00, 400.00, 'POOL_SHARE', 3, NULL,
 DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),

-- 902003: 서울 전체 YES/NO ACTIVE (3명 참여)
(902003, '이번 주 서울 전체 아파트 매매가격지수, 상승세 이어갈까요?',
 '서울 전체 기준. 강남·강북 구분 없이 서울 평균 매매가격지수 변동률로 판정.',
 'PRICE_INDEX', 'YES_NO', 'PERCENT',
 '한국부동산원 주간 아파트 가격동향',
 '서울 아파트 매매가격지수 주간 변동률이 0보다 크면 YES',
 DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'ACTIVE',
 DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY),
 NULL, NULL, NULL, NULL,
 400.00, 5.00, 0.00, 0.00, 300.00, 'POOL_SHARE', 1, NULL,
 DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),

-- 902004: 서울 마포구 YES/NO ACTIVE (member 1 참여)
(902004, '이번 주 서울 마포구 아파트 매매가격지수, 오를까요?',
 '마포구 거주 또는 관심 있는 분들의 예측을 모읍니다.',
 'PRICE_INDEX', 'YES_NO', 'PERCENT',
 '한국부동산원 주간 아파트 가격동향',
 '서울 마포구 아파트 매매가격지수 주간 변동률이 0보다 크면 YES',
 DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'ACTIVE',
 DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY),
 NULL, NULL, NULL, NULL,
 250.00, 5.00, 0.00, 0.00, 300.00, 'POOL_SHARE', 1, NULL,
 DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),

-- 902005: 부산 해운대구 YES/NO ACTIVE
(902005, '이번 주 부산 해운대구 아파트 매매가격지수, 상승할까요?',
 '해운대구 아파트 시장 동향. 수영구·남구 인접 영향도 반영됩니다.',
 'PRICE_INDEX', 'YES_NO', 'PERCENT',
 '한국부동산원 주간 아파트 가격동향',
 '부산 해운대구 아파트 매매가격지수 주간 변동률이 0보다 크면 YES',
 DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'ACTIVE',
 DATE_ADD(NOW(), INTERVAL 6 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY),
 NULL, NULL, NULL, NULL,
 300.00, 5.00, 0.00, 0.00, 300.00, 'POOL_SHARE', 4, NULL,
 DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),

-- 902006: 인천 YES/NO ACTIVE
(902006, '이번 주 인천 아파트 매매가격지수, 상승할까요?',
 '인천 전체 기준. 연수구·남동구·부평구 등 전 구군 평균.',
 'PRICE_INDEX', 'YES_NO', 'PERCENT',
 '한국부동산원 주간 아파트 가격동향',
 '인천 아파트 매매가격지수 주간 변동률이 0보다 크면 YES',
 DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'ACTIVE',
 DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY),
 NULL, NULL, NULL, NULL,
 250.00, 5.00, 0.00, 0.00, 300.00, 'POOL_SHARE', 6, NULL,
 DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),

-- 902007: 경기 성남시 SETTLED YES 승리
--   total_pool=300, fee=15(5%), settlement_pool=285
--   YES(912014) 승리: member3 150P → 285P 수령 (수익 135P)
--   NO(912015) 패배:  member106 150P → 0P
(902007, '지난 주 경기 성남시 아파트 매매가격지수, 상승했을까요?',
 '이미 종료된 마켓입니다. 판교·분당 수요 견인 여부가 관건이었습니다.',
 'PRICE_INDEX', 'YES_NO', 'PERCENT',
 '한국부동산원 주간 아파트 가격동향',
 '경기 성남시 아파트 매매가격지수 주간 변동률이 0보다 크면 YES',
 DATE_SUB(CURDATE(), INTERVAL 7 DAY), 'SETTLED',
 DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY),
 DATE_SUB(NOW(), INTERVAL 5 DAY), 912014, NULL, 'YES',
 300.00, 5.00, 15.00, 285.00, 300.00, 'POOL_SHARE', 3, NULL,
 DATE_SUB(NOW(), INTERVAL 14 DAY), NOW()),

-- 902008: 서울 노원구 SETTLED NO(하락/보합) 승리
--   total_pool=300, fee=15(5%), settlement_pool=285
--   UP(912016) 패배:    member1 150P → 0P
--   DOWN_FLAT(912017) 승리: member5 150P → 285P 수령
(902008, '지난 주 서울 노원구 아파트 매매가격지수, 상승했을까요?',
 '이미 종료된 마켓입니다. 노원구는 실수요 중심으로 전세가 비율이 높은 지역입니다.',
 'PRICE_INDEX', 'YES_NO', 'PERCENT',
 '한국부동산원 주간 아파트 가격동향',
 '서울 노원구 아파트 매매가격지수 주간 변동률이 0보다 크면 YES(상승), 아니면 NO(하락/보합)',
 DATE_SUB(CURDATE(), INTERVAL 5 DAY), 'SETTLED',
 DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY),
 DATE_SUB(NOW(), INTERVAL 3 DAY), 912017, NULL, 'NO',
 300.00, 5.00, 15.00, 285.00, 300.00, 'POOL_SHARE', 1, NULL,
 DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),

-- 902009: 서울 서초구 MULTIPLE_CHOICE CLOSED (결과 확정, 정산 대기)
--   LOW(912019) 결과 확정. 아직 정산 미완료.
(902009, '지난 주 서울 서초구 아파트 매매가격지수 변동률, 어느 구간이었을까요?',
 '이미 마감된 마켓입니다. 결과가 확정되어 정산을 기다리고 있습니다.',
 'PRICE_INDEX', 'MULTIPLE_CHOICE', 'PERCENT',
 '한국부동산원 주간 아파트 가격동향',
 '서울 서초구 아파트 매매가격지수 주간 변동률 구간 판정',
 DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'CLOSED',
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY),
 NULL, 912019, NULL, 'LOW',
 350.00, 5.00, 0.00, 0.00, 300.00, 'POOL_SHARE', 2, NULL,
 DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),

-- 902010: 전국 YES/NO ACTIVE (2명 참여, 균형)
(902010, '이번 주 전국 아파트 매매가격지수, 상승할까요?',
 '전국 평균 기준. 수도권·지방 포함 종합 지수.',
 'PRICE_INDEX', 'YES_NO', 'PERCENT',
 '한국부동산원 주간 아파트 가격동향',
 '전국 아파트 매매가격지수 주간 변동률이 0보다 크면 YES',
 DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'ACTIVE',
 DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY),
 NULL, NULL, NULL, NULL,
 200.00, 5.00, 0.00, 0.00, 500.00, 'POOL_SHARE', 1, NULL,
 DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),

-- 902011: 대구 수성구 YES/NO ACTIVE (1명 참여)
(902011, '이번 주 대구 수성구 아파트 매매가격지수, 상승할까요?',
 '대구 대표 부촌 수성구 아파트 시장 예측.',
 'PRICE_INDEX', 'YES_NO', 'PERCENT',
 '한국부동산원 주간 아파트 가격동향',
 '대구 수성구 아파트 매매가격지수 주간 변동률이 0보다 크면 YES',
 DATE_ADD(CURDATE(), INTERVAL 6 DAY), 'ACTIVE',
 DATE_ADD(NOW(), INTERVAL 6 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY),
 NULL, NULL, NULL, NULL,
 100.00, 5.00, 0.00, 0.00, 300.00, 'POOL_SHARE', 5, NULL,
 DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),

-- 902012: 서울 은평구 YES/NO ACTIVE (1명 참여)
(902012, '이번 주 서울 은평구 아파트 매매가격지수, 상승할까요?',
 '은평구 신축 공급 및 재개발 영향 반영 예정.',
 'PRICE_INDEX', 'YES_NO', 'PERCENT',
 '한국부동산원 주간 아파트 가격동향',
 '서울 은평구 아파트 매매가격지수 주간 변동률이 0보다 크면 YES',
 DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'ACTIVE',
 DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY),
 NULL, NULL, NULL, NULL,
 100.00, 5.00, 0.00, 0.00, 300.00, 'POOL_SHARE', 1, NULL,
 DATE_SUB(NOW(), INTERVAL 2 DAY), NOW());

-- ── Market Option ─────────────────────────────────────────────
-- 가격 = (virtual + real_해당) / 전체합
-- 계약수 = point_amount / 0.5 (예측 시점 기준 가격 0.5 가정)

INSERT INTO market_option (
    id, market_id, option_code, option_text, display_order,
    range_min, range_max, min_inclusive, max_inclusive,
    virtual_pool_amount, real_pool_amount, total_contract_quantity,
    current_price, prediction_count, is_result, created_at, updated_at
) VALUES
-- 902001 (YES real=750, NO real=250; virtual=500 each; total=2000)
-- price_YES = 1250/2000 = 0.625, price_NO = 750/2000 = 0.375
(912001, 902001, 'YES', 'YES', 1, NULL, NULL, TRUE, FALSE,
 500.00, 750.00, 1500.00000000, 0.62500000, 4, FALSE, DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),
(912002, 902001, 'NO',  'NO',  2, NULL, NULL, TRUE, FALSE,
 500.00, 250.00,  500.00000000, 0.37500000, 2, FALSE, DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),

-- 902002 MULTIPLE_CHOICE (경기도, 참여 없음)
(912003, 902002, 'NEGATIVE', '0% 미만',             1, NULL,   0.0000, TRUE,  FALSE, 400.00, 0.00, 0.00, 0.33333333, 0, FALSE, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
(912004, 902002, 'LOW',      '0% 이상 ~ 0.3% 미만', 2, 0.0000, 0.3000, TRUE,  FALSE, 400.00, 0.00, 0.00, 0.33333333, 0, FALSE, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
(912005, 902002, 'HIGH',     '0.3% 이상',           3, 0.3000, NULL,   TRUE,  FALSE, 400.00, 0.00, 0.00, 0.33333333, 0, FALSE, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),

-- 902003 (YES real=300, NO real=100; virtual=300 each; total=1000)
-- price_YES = 600/1000 = 0.6, price_NO = 400/1000 = 0.4
(912006, 902003, 'YES', 'YES', 1, NULL, NULL, TRUE, FALSE,
 300.00, 300.00, 600.00000000, 0.60000000, 2, FALSE, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(912007, 902003, 'NO',  'NO',  2, NULL, NULL, TRUE, FALSE,
 300.00, 100.00, 200.00000000, 0.40000000, 1, FALSE, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),

-- 902004 (YES real=150, NO real=100; virtual=300 each; total=850)
-- price_YES = 450/850 = 0.52941176, price_NO = 400/850 = 0.47058824
(912008, 902004, 'YES', 'YES', 1, NULL, NULL, TRUE, FALSE,
 300.00, 150.00, 300.00000000, 0.52941176, 2, FALSE, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),
(912009, 902004, 'NO',  'NO',  2, NULL, NULL, TRUE, FALSE,
 300.00, 100.00, 200.00000000, 0.47058824, 1, FALSE, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),

-- 902005 (YES real=200, NO real=100; virtual=300 each; total=900)
-- price_YES = 500/900 = 0.55555556, price_NO = 400/900 = 0.44444444
(912010, 902005, 'YES', 'YES', 1, NULL, NULL, TRUE, FALSE,
 300.00, 200.00, 400.00000000, 0.55555556, 1, FALSE, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(912011, 902005, 'NO',  'NO',  2, NULL, NULL, TRUE, FALSE,
 300.00, 100.00, 200.00000000, 0.44444444, 1, FALSE, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),

-- 902006 (YES real=100, NO real=150; virtual=300 each; total=850)
-- price_YES = 400/850 = 0.47058824, price_NO = 450/850 = 0.52941176
(912012, 902006, 'YES', 'YES', 1, NULL, NULL, TRUE, FALSE,
 300.00, 100.00, 200.00000000, 0.47058824, 1, FALSE, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(912013, 902006, 'NO',  'NO',  2, NULL, NULL, TRUE, FALSE,
 300.00, 150.00, 300.00000000, 0.52941176, 1, FALSE, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),

-- 902007 SETTLED YES 승리 (real=150 each; virtual=300 each; price 0.5 고정)
(912014, 902007, 'YES', 'YES', 1, NULL, NULL, TRUE, FALSE,
 300.00, 150.00, 300.00000000, 0.50000000, 1, TRUE,  DATE_SUB(NOW(), INTERVAL 14 DAY), NOW()),
(912015, 902007, 'NO',  'NO',  2, NULL, NULL, TRUE, FALSE,
 300.00, 150.00, 300.00000000, 0.50000000, 1, FALSE, DATE_SUB(NOW(), INTERVAL 14 DAY), NOW()),

-- 902008 SETTLED DOWN_FLAT 승리 (real=150 each; virtual=300 each; price 0.5 고정)
(912016, 902008, 'UP',        '상승',     1, NULL, NULL, TRUE, FALSE,
 300.00, 150.00, 300.00000000, 0.50000000, 1, FALSE, DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),
(912017, 902008, 'DOWN_FLAT', '하락/보합', 2, NULL, NULL, TRUE, FALSE,
 300.00, 150.00, 300.00000000, 0.50000000, 1, TRUE,  DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),

-- 902009 CLOSED 서초구 MULTIPLE_CHOICE (LOW 결과 확정)
-- NEGATIVE real=100, LOW real=100, HIGH real=150; virtual=300 each; total=1250
-- price_NEG = 400/1250=0.32, price_LOW = 400/1250=0.32, price_HIGH = 450/1250=0.36
(912018, 902009, 'NEGATIVE', '0% 미만',             1, NULL,   0.0000, TRUE,  FALSE,
 300.00, 100.00, 200.00000000, 0.32000000, 1, FALSE, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(912019, 902009, 'LOW',      '0% 이상 ~ 0.3% 미만', 2, 0.0000, 0.3000, TRUE,  FALSE,
 300.00, 100.00, 200.00000000, 0.32000000, 1, TRUE,  DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(912020, 902009, 'HIGH',     '0.3% 이상',           3, 0.3000, NULL,   TRUE,  FALSE,
 300.00, 150.00, 300.00000000, 0.36000000, 1, FALSE, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),

-- 902010 전국 균형 (YES real=100, NO real=100; virtual=500 each; total=1200)
-- price = 600/1200 = 0.5 균형
(912021, 902010, 'YES', 'YES', 1, NULL, NULL, TRUE, FALSE,
 500.00, 100.00, 200.00000000, 0.50000000, 1, FALSE, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),
(912022, 902010, 'NO',  'NO',  2, NULL, NULL, TRUE, FALSE,
 500.00, 100.00, 200.00000000, 0.50000000, 1, FALSE, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW()),

-- 902011 대구수성구 (YES real=100; virtual=300 each; total=700)
-- price_YES = 400/700 = 0.57142857, price_NO = 300/700 = 0.42857143
(912023, 902011, 'YES', 'YES', 1, NULL, NULL, TRUE, FALSE,
 300.00, 100.00, 200.00000000, 0.57142857, 1, FALSE, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(912024, 902011, 'NO',  'NO',  2, NULL, NULL, TRUE, FALSE,
 300.00,   0.00,   0.00000000, 0.42857143, 0, FALSE, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),

-- 902012 은평구 (YES real=100; virtual=300 each; total=700)
(912025, 902012, 'YES', 'YES', 1, NULL, NULL, TRUE, FALSE,
 300.00, 100.00, 200.00000000, 0.57142857, 1, FALSE, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(912026, 902012, 'NO',  'NO',  2, NULL, NULL, TRUE, FALSE,
 300.00,   0.00,   0.00000000, 0.42857143, 0, FALSE, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW());

-- ── Market Prediction ─────────────────────────────────────────
-- contract_quantity = point_amount / 0.5 (예측 시점 price=0.5 기준)

INSERT INTO market_prediction (
    id, market_id, option_id, member_id, point_amount,
    price_snapshot, contract_quantity,
    expected_payout_per_contract_snapshot, expected_multiplier_snapshot,
    status, point_spend_idempotency_key, attempt_no,
    settled_amount, refund_amount, fail_reason, created_at, updated_at
) VALUES
-- ── 902001 강남구 (6명) ──────────────────────────────────────
(922001, 902001, 912001, 2,   200.00, 0.50000000, 400.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902001:member:2:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY)),
(922002, 902001, 912001, 1,   100.00, 0.52173913, 200.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902001:member:1:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
(922003, 902001, 912002, 4,   150.00, 0.47826087, 300.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902001:member:4:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),
(922004, 902001, 912001, 101, 300.00, 0.55172414, 600.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902001:member:101:attempt:1', 1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
(922005, 902001, 912002, 102, 100.00, 0.44827586, 200.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902001:member:102:attempt:1', 1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
(922006, 902001, 912001, 103, 150.00, 0.62857143, 300.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902001:member:103:attempt:1', 1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),

-- ── 902003 서울전체 (3명) ────────────────────────────────────
(922007, 902003, 912006, 1,   100.00, 0.50000000, 200.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902003:member:1:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
(922008, 902003, 912006, 2,   200.00, 0.50000000, 400.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902003:member:2:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(922009, 902003, 912007, 3,   100.00, 0.50000000, 200.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902003:member:3:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),

-- ── 902004 마포구 (3명, member 1 포함) ──────────────────────
(922010, 902004, 912008, 1,   100.00, 0.50000000, 200.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902004:member:1:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(922011, 902004, 912009, 7,   100.00, 0.50000000, 200.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902004:member:7:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(922012, 902004, 912008, 8,    50.00, 0.52941176, 100.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902004:member:8:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- ── 902005 부산해운대구 (2명) ────────────────────────────────
(922013, 902005, 912010, 4,   200.00, 0.50000000, 400.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902005:member:4:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(922014, 902005, 912011, 9,   100.00, 0.50000000, 200.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902005:member:9:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- ── 902006 인천 (2명) ────────────────────────────────────────
(922015, 902006, 912012, 6,   100.00, 0.50000000, 200.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902006:member:6:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
(922016, 902006, 912013, 3,   150.00, 0.50000000, 300.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902006:member:3:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),

-- ── 902007 성남시 SETTLED YES 승리 ──────────────────────────
-- member3 YES → 285P 수령, member106 NO → 0P
(922017, 902007, 912014, 3,   150.00, 0.50000000, 300.00000000, NULL, NULL,
 'SETTLED', 'REAL_SPEND:market:902007:member:3:attempt:1',   1, 285.00, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(922018, 902007, 912015, 106, 150.00, 0.50000000, 300.00000000, NULL, NULL,
 'SETTLED', 'REAL_SPEND:market:902007:member:106:attempt:1', 1,   0.00, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),

-- ── 902008 노원구 SETTLED DOWN_FLAT 승리 ────────────────────
-- member1 UP → 0P 패배, member5 DOWN_FLAT → 285P 수령
(922019, 902008, 912016, 1,   150.00, 0.50000000, 300.00000000, NULL, NULL,
 'SETTLED', 'REAL_SPEND:market:902008:member:1:attempt:1',   1,   0.00, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(922020, 902008, 912017, 5,   150.00, 0.50000000, 300.00000000, NULL, NULL,
 'SETTLED', 'REAL_SPEND:market:902008:member:5:attempt:1',   1, 285.00, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),

-- ── 902009 서초구 CLOSED (정산 대기) ────────────────────────
(922021, 902009, 912019, 2,   100.00, 0.32000000, 200.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902009:member:2:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
(922022, 902009, 912020, 1,   150.00, 0.36000000, 300.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902009:member:1:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
(922023, 902009, 912018, 104, 100.00, 0.32000000, 200.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902009:member:104:attempt:1', 1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),

-- ── 902010 전국 (2명) ────────────────────────────────────────
(922024, 902010, 912021, 1,   100.00, 0.50000000, 200.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902010:member:1:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(922025, 902010, 912022, 5,   100.00, 0.50000000, 200.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902010:member:5:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- ── 902011 대구수성구 (1명) ──────────────────────────────────
(922026, 902011, 912023, 5,   100.00, 0.50000000, 200.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902011:member:5:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- ── 902012 은평구 (1명) ──────────────────────────────────────
(922027, 902012, 912025, 1,   100.00, 0.50000000, 200.00000000, NULL, NULL,
 'CONFIRMED', 'REAL_SPEND:market:902012:member:1:attempt:1',   1, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ── Market Price History (902001 가격 변동 이력) ─────────────

INSERT INTO market_price_history (
    id, market_id, option_id, prediction_id, event_type,
    price_before, price_after, real_pool_before, real_pool_after,
    contract_quantity_before, contract_quantity_after, created_at, updated_at
) VALUES
-- member 101 (300P YES) 예측 직후 가격 변화
(927001, 902001, 912001, 922004, 'PREDICTION_CONFIRMED',
 0.55172414, 0.62857143, 300.00, 600.00, 600.00000000, 1200.00000000,
 DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
(927002, 902001, 912002, 922004, 'PREDICTION_CONFIRMED',
 0.44827586, 0.37142857, 150.00, 150.00, 300.00000000,  300.00000000,
 DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
-- member 103 (150P YES) 예측 직후 가격 변화
(927003, 902001, 912001, 922006, 'PREDICTION_CONFIRMED',
 0.60000000, 0.62500000, 600.00, 750.00, 1200.00000000, 1500.00000000,
 DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(927004, 902001, 912002, 922006, 'PREDICTION_CONFIRMED',
 0.40000000, 0.37500000, 250.00, 250.00,  500.00000000,  500.00000000,
 DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY));

-- ── Market Settlement ─────────────────────────────────────────

INSERT INTO market_settlement (
    id, market_id, result_option_id, total_pool, fee_rate, fee_amount,
    settlement_pool, winning_contract_quantity, payout_per_contract,
    burned_point_amount, status, settled_by, settled_at, created_at, updated_at
) VALUES
-- 902007 YES 승리: 300P pool, 5% fee, payout = 285/300 = 0.95
(932001, 902007, 912014, 300.00, 5.00, 15.00, 285.00, 300.00000000, 0.95000000,
 0.00, 'COMPLETED', 999, DATE_SUB(NOW(), INTERVAL 5 DAY),
 DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- 902008 DOWN_FLAT 승리
(932002, 902008, 912017, 300.00, 5.00, 15.00, 285.00, 300.00000000, 0.95000000,
 0.00, 'COMPLETED', 999, DATE_SUB(NOW(), INTERVAL 3 DAY),
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));

-- ── Market Settlement Detail ──────────────────────────────────

INSERT INTO market_settlement_detail (
    id, settlement_id, prediction_id, member_id,
    original_point_amount, contract_quantity, payout_per_contract,
    settled_amount, profit_amount, status, idempotency_key, fail_reason,
    created_at, updated_at
) VALUES
-- 902007 YES 승리: member3 (150P → 285P, 수익 135P)
(942001, 932001, 922017, 3,
 150.00, 300.00000000, 0.95000000, 285.00, 135.00,
 'SUCCESS', 'MARKET_SETTLEMENT_REWARD:market:902007:prediction:922017:member:3', NULL,
 DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- 902007 NO 패배: member106 (150P → 0P, 손실 150P)
(942002, 932001, 922018, 106,
 150.00, 300.00000000, 0.95000000, 0.00, -150.00,
 'SUCCESS', 'MARKET_SETTLEMENT_REWARD:market:902007:prediction:922018:member:106', NULL,
 DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
-- 902008 UP 패배: member1 (150P → 0P, 손실 150P)
(942003, 932002, 922019, 1,
 150.00, 300.00000000, 0.95000000, 0.00, -150.00,
 'SUCCESS', 'MARKET_SETTLEMENT_REWARD:market:902008:prediction:922019:member:1', NULL,
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
-- 902008 DOWN_FLAT 승리: member5 (150P → 285P, 수익 135P)
(942004, 932002, 922020, 5,
 150.00, 300.00000000, 0.95000000, 285.00, 135.00,
 'SUCCESS', 'MARKET_SETTLEMENT_REWARD:market:902008:prediction:922020:member:5', NULL,
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));

-- ── Market Reputation Update ──────────────────────────────────

INSERT INTO market_reputation_update (
    id, market_id, prediction_id, member_id, is_correct,
    status, attempt_no, last_error_code, last_error_message, created_at, updated_at
) VALUES
(962001, 902007, 922017, 3,   TRUE,  'SUCCESS', 1, NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(962002, 902007, 922018, 106, FALSE, 'SUCCESS', 1, NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(962003, 902008, 922019, 1,   FALSE, 'SUCCESS', 1, NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(962004, 902008, 922020, 5,   TRUE,  'SUCCESS', 1, NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW());

-- ── Market Comment ────────────────────────────────────────────

INSERT INTO market_comment (id, market_id, member_id, content, deleted_at, created_at, updated_at) VALUES
(980001, 902001, 2,   '강남구 최근 거래량도 늘고 있어서 이번 주도 소폭 상승할 것 같아요. YES로 참여했습니다.', NULL, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
(980002, 902001, 4,   '금리 동결 기조가 유지되는 한 강남 아파트가 내리긴 어렵죠. 저도 YES 입장이에요.', NULL, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(980003, 902001, 103, '국내외 경기 불확실성이 있어서 저는 NO로 갔어요. 하락 전환 가능성도 열어둬야 한다고 봐요.', NULL, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(980004, 902004, 1,   '마포구 공덕 쪽 신축 분양 수요가 꾸준한 것 같아서 YES로 참여했어요. 같이 응원해봐요!', NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(980005, 902007, 3,   '성남시는 판교 IT 수요가 버팀목이 돼서 YES가 맞을 거라고 봤는데 결국 맞았네요. 다음에도 도전!', NULL, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW());

COMMIT;

-- ============================================================
-- 3. INSIGHT DB — market_prediction_result
-- ============================================================
USE insight;

START TRANSACTION;

DELETE FROM market_prediction_result WHERE member_id IN (3, 106, 5) AND market_id IN (902007, 902008);
DELETE FROM market_prediction_result WHERE member_id = 1 AND market_id = 902008;

INSERT INTO market_prediction_result (member_id, market_id, prediction_id, is_correct,
                                       processed_at, created_at, updated_at)
VALUES
-- 902007 성남시: member3 맞춤, member106 틀림
(3,   902007, 922017, TRUE,  DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(106, 902007, 922018, FALSE, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
-- 902008 노원구: member1 틀림, member5 맞춤
(1,   902008, 922019, FALSE, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(5,   902008, 922020, TRUE,  DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NOW());

COMMIT;

-- ============================================================
-- 검증 쿼리
-- ============================================================
-- USE battle;
-- SELECT id, title, status, option_a_count, option_b_count, vote_count, winning_option
--   FROM battle WHERE id BETWEEN 801001 AND 801015 ORDER BY id;
-- SELECT battle_id, COUNT(*) votes FROM battle_vote
--   WHERE battle_id BETWEEN 801001 AND 801015 GROUP BY battle_id ORDER BY battle_id;
-- SELECT battle_id, COUNT(*) comments FROM comment
--   WHERE battle_id BETWEEN 801001 AND 801015 GROUP BY battle_id ORDER BY battle_id;
--
-- USE market;
-- SELECT id, title, status, total_pool, result_option_id
--   FROM market WHERE id BETWEEN 902001 AND 902012 ORDER BY id;
-- SELECT market_id, COUNT(*) preds, SUM(point_amount) pool FROM market_prediction
--   WHERE market_id BETWEEN 902001 AND 902012 GROUP BY market_id ORDER BY market_id;
--
-- USE insight;
-- SELECT member_id, market_id, is_correct FROM market_prediction_result
--   WHERE market_id IN (902007, 902008) ORDER BY market_id, member_id;
