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
├─ animal    개체
├─ species   종
├─ log       사육 기록
├─ photo     사진, AI 분석
├─ health    건강 경고
└─ admin     관리자
```

도메인 패키지 안은 `controller / service / repository / domain / dto`로 나눕니다.

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

## 배포 (개발 서버)

`develop`에 머지되면 GitHub Actions(`.github/workflows/cd-dev.yml`)가 자동으로 배포합니다.

1. Docker 이미지를 빌드해 GHCR(`ghcr.io/tail-friends/tail-server`)에 올립니다.
2. 학교 서버에 SSH로 접속해 `deploy/docker-compose.yml`을 복사하고, 새 이미지로 컨테이너를 다시 띄웁니다.
3. `/api/v1/health-check`가 응답하는지 확인합니다. 실패하면 앱 로그를 출력하고 워크플로가 실패합니다.

Actions 탭에서 `CD (dev)` 워크플로를 직접 실행(Run workflow)할 수도 있습니다.

### 서버 준비 (처음 한 번)

1. Docker와 Docker Compose 플러그인을 설치합니다.
2. 배포용 계정이 `docker` 명령을 쓸 수 있게 합니다. (`sudo usermod -aG docker <계정>`)
3. 배포 폴더를 만들고 환경 변수 파일을 둡니다.

```bash
mkdir -p ~/tail-server
# deploy/.env.example을 참고해 ~/tail-server/.env 작성
```

4. 배포용 SSH 키를 만들고, 공개 키를 서버의 `~/.ssh/authorized_keys`에 추가합니다.

```bash
ssh-keygen -t ed25519 -f tail-deploy -C "github-actions"
```

### GitHub 설정

Settings → Environments에서 `dev` 환경을 만들고 아래 Secrets를 추가합니다.

| 이름 | 값 |
|---|---|
| `SSH_HOST` | 서버 주소 |
| `SSH_PORT` | SSH 포트 (비우면 22) |
| `SSH_USER` | 배포용 계정 |
| `SSH_PRIVATE_KEY` | 배포용 SSH 개인 키 (`tail-deploy` 파일 내용) |
| `DEPLOY_PATH` | 배포 폴더 절대 경로 (예: `/home/<계정>/tail-server`) |

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
