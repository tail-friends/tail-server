# tail-server

꼬리친구의 백엔드 API 서버입니다.

꼬리친구는 내 파충류 사진으로 만든 캐릭터가 사육장에서 지내며, 실제 급여·탈피·체중 기록에 따라 상태를 알려주는 사육 기록 앱입니다. 사용자 앱(Flutter)과 관리자 웹이 이 서버의 API를 사용합니다.

## 서버가 하는 일

- 인증: 소셜 로그인, JWT 발급·재발급, 로그아웃
- 개체·종 관리, 종별 사육 계획 초기 데이터
- 급여·탈피·체중 기록. 기록이 바뀔 때마다 다음 급여일과 건강 경고를 다시 계산
- 사진 업로드와 AI 캐릭터 분석 (하루 분석 횟수 제한)
- 관리자 API: 종·사육 가이드 관리, 운영 통계

## 기술 스택

- Java 21, Spring Boot 3.5, Gradle (Groovy)
- Spring Web / Validation / Data JPA / Security, Lombok
- PostgreSQL 16 (로컬은 docker-compose로 실행)
- springdoc-openapi (Swagger UI), jjwt

> JDK 21이 설치되어 있지 않아도 됩니다. Gradle toolchain이 JDK 21을 자동으로 내려받습니다.

## 패키지 구조

```
com.tailfriends
├─ global    공통 설정, 보안, 예외, 응답 형식, 유틸
├─ auth      인증
├─ user      사용자
├─ animal    개체
├─ species   종
├─ log       사육 기록
├─ photo     사진, AI 분석
├─ health    건강 경고
└─ admin     관리자
```

도메인 패키지 안은 `controller / service / repository / domain / dto`로 나눕니다.

## 앱·웹 팀: 서버 띄우기 (Docker)

JDK 없이 Docker만 있으면 서버와 DB를 한 번에 띄울 수 있습니다. ([Docker Desktop](https://www.docker.com/products/docker-desktop/) 설치)

```bash
git clone https://github.com/tail-friends/tail-server.git
cd tail-server
cp .env.example .env                        # 처음 한 번. 로컬은 값을 바꾸지 않아도 됩니다
docker compose --profile app up -d --build  # 서버 + DB 실행 (처음엔 빌드 때문에 몇 분 걸림)
```

- Swagger UI: http://localhost:8080/swagger-ui
- 최신 코드로 다시 띄우기: `git pull` 후 같은 명령을 다시 실행
- 끄기: `docker compose --profile app down` (DB 데이터와 올린 사진은 남음)
- DB와 사진까지 처음부터: `docker compose --profile app down -v`
- 서버 로그 보기: `docker compose --profile app logs -f app`

### 토큰 받기 (테스트 로그인)

로컬에서는 소셜 로그인 대신 테스트 로그인으로 토큰을 받습니다. 같은 `name`이면 같은 사용자입니다.

```bash
curl -X POST http://localhost:8080/api/v1/dev/login \
  -H 'Content-Type: application/json' \
  -d '{ "name": "app-tester" }'
```

- 관리자 API를 쓰려면 `{ "name": "admin", "role": "ADMIN" }`
- 응답의 `accessToken`을 `Authorization: Bearer {accessToken}` 헤더에 넣습니다. Swagger에서는 오른쪽 위 **Authorize**에 토큰만 붙여 넣습니다.
- 테스트 로그인은 `docker compose --profile app`으로 띄울 때만 켜집니다. 운영(`prod` 프로필)에서는 항상 꺼져 있습니다.

### 샘플 데이터 만들기

빈 화면 대신 기록이 채워진 화면으로 개발하려면, 테스트 로그인으로 받은 토큰으로 샘플 데이터를 만듭니다.

```bash
curl -X POST http://localhost:8080/api/v1/dev/sample-data \
  -H "Authorization: Bearer {accessToken}"
```

로그인한 사용자에게 개체 3마리와 캐릭터, 오늘 기준 최근 60일 기록(급여·탈피·체중·청소·온습도·메모)이 생깁니다.

| 개체 | 종 | 보이는 상태 |
|---|---|---|
| 망고 | 레오파드 게코 | 급여 D-2, 체중 증가, 3주 전 탈피 완료 |
| 콩이 | 크레스티드 게코 | 급여 2일 지남, 최근 이틀 거부, 체중 감소, 묽은 변 |
| 단비 | 볼 파이톤 | 오늘 급여, 이틀 전부터 탈피 준비(눈 파래짐) |

- 부를 때마다 3마리가 새로 추가됩니다. 처음부터 다시 하려면 테스트 로그인의 `name`을 바꿉니다.
- 테스트 로그인과 같이 로컬에서만 켜지고 운영에는 없습니다.

## 로컬 실행 (백엔드 개발)

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

| 프로필 | 용도 |
|---|---|
| `local` (기본값) | 로컬 개발 (SQL 로그 출력) |
| `dev` | 개발 서버 |
| `prod` | 운영 서버 (Swagger 비활성화) |

`SPRING_PROFILES_ACTIVE` 환경 변수로 프로필을 바꿉니다.

## DB 마이그레이션 (Flyway)

테이블은 Flyway 마이그레이션 SQL로만 만들고 바꿉니다. 모든 프로필에서 JPA `ddl-auto`는 `validate`라서, 엔티티와 테이블이 다르면 앱이 뜨지 않습니다.

- 위치: `src/main/resources/db/migration`
- 파일 이름: `V{yyyyMMddHHmm}__{설명}.sql`
  - 예: `V202610201530__create_animals.sql`
  - 백엔드 두 명이 동시에 만들어도 번호가 겹치지 않도록 버전은 작성 시각으로 씁니다.
- 앱이 시작될 때 아직 적용되지 않은 파일이 자동으로 실행됩니다.
- **이미 머지된 마이그레이션 파일은 고치지 않습니다.** 바꿀 내용이 있으면 새 파일을 추가합니다. (적용된 파일을 고치면 체크섬이 달라져 앱이 뜨지 않습니다.)
- 다른 사람의 마이그레이션이 먼저 머지됐다면, 내 파일의 버전(시각)이 그보다 뒤가 되도록 이름을 바꾼 뒤 머지합니다. (Flyway는 이미 적용된 버전보다 앞선 버전을 실행하지 않습니다.)
- 엔티티를 만들 때 `BaseTimeEntity`를 상속하면 `created_at`, `updated_at` 컬럼이 필요합니다. 타입은 `timestamp(6) with time zone`입니다.

로컬 DB를 처음부터 다시 만들고 싶으면 볼륨을 지우고 다시 띄웁니다.

```bash
docker compose down -v
docker compose up -d
```

## 환경 변수

전체 목록은 [`.env.example`](.env.example)을 참고하세요. DB 주소, JWT 시크릿, AI 키 같은 비밀값은 코드나 yml에 직접 쓰지 않습니다.

## 빌드

```bash
./gradlew build
```

## 컨벤션

### 브랜치

| 브랜치 | 용도 |
|---|---|
| `main` | 배포본. develop에서 릴리즈할 때만 병합 |
| `develop` | 개발 통합본. 개발 서버에 배포되며 PR로만 병합 |
| `feature/#이슈번호-설명` | 기능 개발. develop에서 따고 develop으로 PR |
| `fix/#이슈번호-설명` | 버그 수정 |
| `hotfix/설명` | 배포본 긴급 수정. main과 develop에 모두 병합 |

### 커밋 메시지

형식: `타입: 한글로 한 줄 설명 (#이슈번호)`

| 타입 | 언제 |
|---|---|
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변화 없는 구조 변경 |
| `docs` | 문서 |
| `test` | 테스트 |
| `chore` | 빌드·설정·의존성 |
| `style` | 포맷·오타 |

### 코드

- 이름: `AnimalController`, `AnimalService`, `AnimalRepository`, 요청·응답 DTO는 `AnimalCreateRequest`, `AnimalResponse`
- 모든 응답은 `ApiResponse<T>`, 예외는 `BusinessException(ErrorCode)`로 던집니다
- 엔티티를 컨트롤러 밖으로 내보내지 않고 DTO로 바꿉니다
- 비밀값은 환경 변수로만 관리합니다
- 급여일·경고 계산처럼 규칙이 있는 로직은 단위 테스트를 같이 올립니다
- API를 바꿀 때는 API 명세서를 먼저 고치고, 이미 쓰는 필드는 지우지 않고 추가만 합니다
- PR은 Squash and merge로 병합합니다
