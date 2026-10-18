# tail-server

꼬리친구(파충류 사육 기록 + 사진으로 만든 캐릭터 앱)의 백엔드 서버입니다.

## 기술 스택

- Java 21, Spring Boot 3.5, Gradle (Groovy)
- Spring Web / Validation / Data JPA / Security, Lombok
- PostgreSQL 16 (로컬은 docker-compose로 실행)
- springdoc-openapi (Swagger UI), jjwt

> JDK 21이 설치되어 있지 않아도 됩니다. Gradle toolchain이 JDK 21을 자동으로 내려받습니다.

## 로컬 실행

### 1. 환경 변수 파일 만들기

```bash
cp .env.example .env
```

`.env`에서 `POSTGRES_PASSWORD`, `DB_PASSWORD`, `JWT_SECRET` 값을 바꿔 주세요. `.env`는 커밋하지 않습니다.

### 2. PostgreSQL 실행

```bash
docker compose up -d
```

docker-compose가 같은 폴더의 `.env`를 자동으로 읽습니다.

### 3. 애플리케이션 실행

```bash
set -a && source .env && set +a
./gradlew bootRun
```

IntelliJ에서 실행할 때는 Run Configuration의 Environment variables에 `.env` 값을 넣어 주세요.
(EnvFile 플러그인을 쓰면 `.env`를 그대로 불러올 수 있습니다.)

### 4. 확인

- Swagger UI: http://localhost:8080/swagger-ui

## 프로필

| 프로필 | 용도 | ddl-auto |
|---|---|---|
| `local` (기본값) | 로컬 개발 | `update` |
| `dev` | 개발 서버 | `validate` |
| `prod` | 운영 서버 (Swagger 비활성화) | `validate` |

`SPRING_PROFILES_ACTIVE` 환경 변수로 프로필을 바꿉니다.

## 환경 변수

전체 목록은 [`.env.example`](.env.example)을 참고하세요. DB 주소, JWT 시크릿, AI 키 같은 비밀값은 코드나 yml에 직접 쓰지 않습니다.

## 빌드

```bash
./gradlew build
```
