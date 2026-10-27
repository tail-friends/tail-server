-- 재사용 감지: 로그인마다 토큰 묶음(family_id)을 두고, 쓴 토큰은 지우지 않고 쓴 시각(used_at)을 남긴다.
-- 이미 발급한 토큰은 각자 새 묶음으로 둔다.
ALTER TABLE refresh_tokens ADD COLUMN family_id UUID;
UPDATE refresh_tokens SET family_id = gen_random_uuid();
ALTER TABLE refresh_tokens ALTER COLUMN family_id SET NOT NULL;

ALTER TABLE refresh_tokens ADD COLUMN used_at TIMESTAMPTZ;

CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
