-- 회원 탈퇴 시 사용자 데이터를 함께 지운다. (개체를 지우면 기록·건강 경고·캐릭터도 함께 지워진다)
ALTER TABLE animals
    DROP CONSTRAINT animals_user_id_fkey,
    ADD CONSTRAINT animals_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE logs
    DROP CONSTRAINT logs_user_id_fkey,
    ADD CONSTRAINT logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE refresh_tokens
    DROP CONSTRAINT refresh_tokens_user_id_fkey,
    ADD CONSTRAINT refresh_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
