-- 이미 올린 사진의 weight_at_time을 채운다. 올린 날(KST)까지의 가장 최근 체중 기록이다(앞으로는 올릴 때 저장한다).
-- 체중 기록이 없으면 NULL로 둔다.
UPDATE photos AS p
SET weight_at_time = (
        SELECT l.weight_g
        FROM logs l
        WHERE l.animal_id = p.animal_id
          AND l.type = 'WEIGHT'
          AND l.log_date <= (p.created_at AT TIME ZONE 'Asia/Seoul')::date
        ORDER BY l.log_date DESC, l.id DESC
        LIMIT 1),
    updated_at = NOW()
WHERE p.weight_at_time IS NULL;
