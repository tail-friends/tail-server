-- 사용자당 하루 사진 업로드 수를 센다
CREATE INDEX idx_photos_user_id_created_at ON photos (user_id, created_at);
