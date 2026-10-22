-- 사용자와 1:1. 설정을 바꾼 적 없는 사용자는 행이 없고 기본값을 쓴다.
-- 원본 사진 보관·알림 수신은 consents에 있어 여기에 두지 않는다.
CREATE TABLE user_settings (
    user_id              BIGINT       PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    notify_time          TIME         NOT NULL DEFAULT '09:00',
    temp_unit            VARCHAR(1)   NOT NULL DEFAULT 'C',
    refusal_count        INTEGER      NOT NULL DEFAULT 3,
    weight_loss_percent  INTEGER      NOT NULL DEFAULT 10,
    weight_loss_days     INTEGER      NOT NULL DEFAULT 30,
    loose_stool_count    INTEGER      NOT NULL DEFAULT 2,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL
);
