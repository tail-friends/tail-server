-- 개체와 1:1. 사진(photo_id)은 photos 테이블이 생길 때 컬럼을 추가한다.
CREATE TABLE characters (
    animal_id       BIGINT        PRIMARY KEY REFERENCES animals (id) ON DELETE CASCADE,
    render_style    VARCHAR(20)   NOT NULL DEFAULT 'ILLUSTRATION_2D',
    shape           VARCHAR(10)   NOT NULL,
    pattern_type    VARCHAR(10)   NOT NULL,
    color_body      VARCHAR(7)    NOT NULL,
    color_pattern   VARCHAR(7)    NOT NULL,
    color_belly     VARCHAR(7)    NOT NULL,
    color_eye       VARCHAR(7)    NOT NULL,
    head_size       NUMERIC(3, 2) CHECK (head_size BETWEEN 0 AND 1),
    tail_width      NUMERIC(3, 2) CHECK (tail_width BETWEEN 0 AND 1),
    pattern_density NUMERIC(3, 2) CHECK (pattern_density BETWEEN 0 AND 1),
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL
);
