-- 종 선택 화면의 분류 탭, 검색(영문명·학명·별칭), 인기 종 먼저 보여주기
ALTER TABLE species
    ADD COLUMN category        VARCHAR(20)   NOT NULL DEFAULT 'REPTILE',
    ADD COLUMN english_name    VARCHAR(60),
    ADD COLUMN scientific_name VARCHAR(80),
    ADD COLUMN aliases         VARCHAR(40)[] NOT NULL DEFAULT '{}',
    ADD COLUMN display_order   INTEGER       NOT NULL DEFAULT 1000;

-- 초기 10종. 학명은 한 종을 가리킬 때만 넣고, 여러 종을 묶어 부르는 이름은 비워 둔다.
UPDATE species
SET display_order   = v.display_order,
    english_name    = v.english_name,
    scientific_name = v.scientific_name
FROM (VALUES ('레오파드 게코', 10, 'Leopard gecko', 'Eublepharis macularius'),
             ('크레스티드 게코', 20, 'Crested gecko', 'Correlophus ciliatus'),
             ('가고일 게코', 30, 'Gargoyle gecko', 'Rhacodactylus auriculatus'),
             ('비어디드 드래곤', 40, 'Bearded dragon', 'Pogona vitticeps'),
             ('블루텅 스킨크', 50, 'Blue-tongued skink', NULL),
             ('볼 파이톤', 60, 'Ball python', 'Python regius'),
             ('콘 스네이크', 70, 'Corn snake', 'Pantherophis guttatus'),
             ('호그노즈 스네이크', 80, 'Hognose snake', NULL),
             ('킹 스네이크', 90, 'Kingsnake', NULL),
             ('육지 거북', 100, 'Tortoise', NULL))
         AS v(name, display_order, english_name, scientific_name)
WHERE species.name = v.name;
