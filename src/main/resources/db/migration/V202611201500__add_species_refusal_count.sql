-- 종별 거식 경고 기준. 비어 있으면 사용자 설정만 쓰고, 값이 있으면 사용자 설정과 둘 중 큰 값으로 판정한다.
-- 볼 파이톤은 건강해도 몇 주에서 몇 달씩 먹지 않기도 하고, 호그노즈 스네이크도 계절에 따라 오래 굶는다.
-- 이런 종은 거식 경고를 늦게 띄우고, 실제 위험은 체중 감소 경고로 잡는다.
ALTER TABLE species
    ADD COLUMN refusal_count INTEGER CHECK (refusal_count >= 1);

UPDATE species AS s
SET refusal_count = v.refusal_count,
    updated_at    = NOW()
FROM (VALUES ('볼 파이톤', 8),
             ('호그노즈 스네이크', 6))
         AS v (name, refusal_count)
WHERE s.name = v.name
  AND s.refusal_count IS NULL;
