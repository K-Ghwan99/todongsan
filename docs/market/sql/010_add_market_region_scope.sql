ALTER TABLE market
ADD COLUMN region_scope VARCHAR(20) NULL;

UPDATE market
SET region_scope = 'NON_REGIONAL'
WHERE region_sido IS NULL;

UPDATE market
SET region_scope = 'NATIONAL'
WHERE region_sido = '전국';

UPDATE market
SET region_scope = 'REGIONAL'
WHERE region_sido IS NOT NULL
  AND region_sido <> '전국';

ALTER TABLE market
MODIFY region_scope VARCHAR(20) NOT NULL;
