-- 관리자 웹의 가이드 작성 현황. 가이드(섹션 전체)를 마지막으로 바꾼 시각과 운영자.
-- 운영자가 탈퇴하면 운영자만 비우고 시각은 남긴다.
ALTER TABLE species
    ADD COLUMN guide_updated_at TIMESTAMPTZ,
    ADD COLUMN guide_updated_by BIGINT REFERENCES users (id) ON DELETE SET NULL;

-- 이미 작성된 가이드는 섹션의 마지막 수정 시각으로 채운다. 수정한 운영자는 알 수 없어 비워 둔다.
UPDATE species AS s
SET guide_updated_at = g.updated_at
FROM (SELECT species_id, max(updated_at) AS updated_at FROM species_guides GROUP BY species_id) AS g
WHERE s.id = g.species_id;
