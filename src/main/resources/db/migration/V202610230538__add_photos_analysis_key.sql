-- 원본을 보관하지 않는 캐릭터 사진의 분석용 임시 사본. 하루가 지나면 파일을 지우고 NULL로 바꾼다.
ALTER TABLE photos ADD COLUMN analysis_key VARCHAR(255);
CREATE INDEX idx_photos_analysis_key ON photos (created_at) WHERE analysis_key IS NOT NULL;
