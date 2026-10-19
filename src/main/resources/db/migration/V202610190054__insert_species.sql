-- 기능명세서 4-2 종별 사육 계획 (성체 기준 기본 급여 주기)
-- 탈피 주기와 온습도 범위는 사육 자료 확인 후 채운다.
INSERT INTO species (name, shape, spiky, feed_interval_days, default_home, created_at, updated_at)
VALUES ('레오파드 게코', 'GECKO', FALSE, 4, 'DESERT', NOW(), NOW()),
       ('크레스티드 게코', 'GECKO', FALSE, 3, 'FOREST', NOW(), NOW()),
       ('가고일 게코', 'GECKO', FALSE, 3, 'FOREST', NOW(), NOW()),
       ('비어디드 드래곤', 'LIZARD', TRUE, 2, 'DESERT', NOW(), NOW()),
       ('블루텅 스킨크', 'LIZARD', FALSE, 5, 'DESERT', NOW(), NOW()),
       ('볼 파이톤', 'SNAKE', FALSE, 10, 'ROCK', NOW(), NOW()),
       ('콘 스네이크', 'SNAKE', FALSE, 7, 'ROCK', NOW(), NOW()),
       ('호그노즈 스네이크', 'SNAKE', FALSE, 7, 'ROCK', NOW(), NOW()),
       ('킹 스네이크', 'SNAKE', FALSE, 7, 'ROCK', NOW(), NOW()),
       ('육지 거북', 'TURTLE', FALSE, 1, 'POND', NOW(), NOW());
