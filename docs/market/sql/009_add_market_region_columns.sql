ALTER TABLE market
    ADD COLUMN region_sido VARCHAR(50) NULL AFTER metric_unit,
    ADD COLUMN region_sigu VARCHAR(50) NULL AFTER region_sido;

UPDATE market
SET region_sido = '전국'
WHERE region_sido IS NULL;
