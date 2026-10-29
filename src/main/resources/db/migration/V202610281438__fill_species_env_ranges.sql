-- 기본 10종 중 9종의 온습도 적정 범위를 노션 "종 원본 표" 값(출처는 표에 있음)으로 채운다.
-- 핫존은 바닥 표면(바스킹 자리) 온도다. 크레스티드·가고일 게코는 표에 "warm side"로 적혀 있어 따뜻한 쪽 온도에 가깝다.
-- 관리자가 이미 바꾼 값은 두고 비어 있는 값만 채운다. 육지 거북은 표에도 값이 없어 그대로 둔다.
UPDATE species AS s
SET hot_min      = COALESCE(s.hot_min, v.hot_min),
    hot_max      = COALESCE(s.hot_max, v.hot_max),
    cool_min     = COALESCE(s.cool_min, v.cool_min),
    cool_max     = COALESCE(s.cool_max, v.cool_max),
    humidity_min = COALESCE(s.humidity_min, v.humidity_min),
    humidity_max = COALESCE(s.humidity_max, v.humidity_max),
    updated_at   = NOW()
FROM (VALUES ('레오파드 게코', 34.0, 36.0, 21.0, 25.0, 30, 40),
             ('크레스티드 게코', 24.0, 26.5, 21.0, 24.0, 60, 80),
             ('가고일 게코', 24.0, 26.5, 21.0, 24.0, 60, 80),
             ('비어디드 드래곤', 40.5, 46.0, 25.0, 29.0, 30, 60),
             ('블루텅 스킨크', 40.5, 46.0, 21.0, 26.5, 40, 60),
             ('볼 파이톤', 30.0, 32.0, 22.0, 26.5, 60, 80),
             ('콘 스네이크', 29.5, 32.0, 22.0, 25.5, 40, 60),
             ('호그노즈 스네이크', 32.0, 35.0, 21.0, 24.0, 30, 50),
             ('킹 스네이크', 29.0, 32.0, 21.0, 26.0, 40, 60))
         AS v (name, hot_min, hot_max, cool_min, cool_max, humidity_min, humidity_max)
WHERE s.name = v.name;
