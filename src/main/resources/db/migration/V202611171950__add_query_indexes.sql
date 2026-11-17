-- 오늘 남긴 기록 조회(user_id + log_date). 지금은 user_id로 시작하는 인덱스가 uk_logs_user_id_client_id뿐이라 사용자 기록을 전부 훑는다.
CREATE INDEX idx_logs_user_id_log_date ON logs (user_id, log_date);

-- 개체별 가장 최근 캐릭터용 사진(DISTINCT ON animal_id, ORDER BY id DESC).
CREATE INDEX idx_photos_animal_id_purpose_id ON photos (animal_id, purpose, id DESC);

-- ON DELETE SET NULL 외래 키. 기록·개체를 지울 때 참조하는 행을 인덱스로 찾는다.
CREATE INDEX idx_point_histories_log_id ON point_histories (log_id);
CREATE INDEX idx_breedings_male_id ON breedings (male_id);
CREATE INDEX idx_breedings_female_id ON breedings (female_id);
