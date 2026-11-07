# 운영 서버 배포 (CD)

`main`의 CI가 성공하면 `.github/workflows/deploy.yml`이 CI가 GHCR에 올린 이미지(`ghcr.io/tail-friends/tail-server:sha-<커밋 7자리>`)를 운영 서버에 배포한다.
이 문서는 **처음 한 번** 해 둘 서버·GitHub 설정과, 배포 뒤 확인·롤백 방법을 정리한다.

## 배포가 하는 일

1. `docker-compose.prod.yml`을 서버의 `/opt/tail-server/docker-compose.yml`로 올린다.
2. `deploy-remote.sh`를 서버에서 실행한다: GHCR 로그인 → `docker compose pull app` → `up -d` → `/api/v1/health-check`를 최대 3분 확인.
3. 실패하면 `/opt/tail-server/current_tag`에 적힌 이전 태그로 되돌리고, 이번 배포는 실패로 끝낸다.

서버에는 DB(PostgreSQL), 앱 컨테이너, 사진 볼륨이 docker compose로 함께 뜬다. DB 포트는 밖으로 열지 않는다.

## 1. 서버 준비

아래는 Ubuntu 기준이다. 서버에 SSH로 들어가서 한다.

### 1-1. Docker 설치

```bash
curl -fsSL https://get.docker.com | sudo sh
docker compose version      # Compose v2 플러그인이 보여야 한다
```

### 1-2. 배포용 사용자

배포 전용 사용자를 따로 두는 것을 권장한다. 이미 쓰는 사용자가 있으면 `docker` 그룹에만 넣으면 된다.

```bash
sudo adduser --disabled-password --gecos "" deploy
sudo usermod -aG docker deploy
```

`docker` 그룹은 사실상 서버 관리자 권한이므로, 이 사용자의 SSH 키는 GitHub 시크릿 외에는 공유하지 않는다.

### 1-3. 배포 폴더

```bash
sudo mkdir -p /opt/tail-server
sudo chown deploy:deploy /opt/tail-server
```

### 1-4. 운영 환경 변수 (`/opt/tail-server/.env`)

`deploy` 사용자로 `.env.example`을 참고해 만든다. 이 파일은 저장소에 올리지 않는다.

```bash
sudo -u deploy nano /opt/tail-server/.env
sudo chmod 600 /opt/tail-server/.env
```

운영에서 반드시 확인할 값:

| 변수 | 운영 값 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `POSTGRES_PASSWORD`, `DB_PASSWORD` | 같은 값. 강한 비밀번호 |
| `DB_USERNAME`, `POSTGRES_USER` | 같은 값 |
| `JWT_SECRET` | `openssl rand -base64 64` 로 만든 값 |
| `KAKAO_*`, `GOOGLE_*`, `APPLE_*` | 소셜 로그인 키. 리다이렉트 URI도 운영 도메인으로 등록 |
| `OAUTH_TOKEN_ENCRYPTION_KEY` | `openssl rand -base64 32`. **바꾸면 저장된 애플 토큰을 풀 수 없다** |
| `STORAGE_PUBLIC_URL` | 운영 도메인 기준 `/files` 주소 |
| `CORS_ALLOWED_ORIGINS` | 운영 앱·웹 origin |
| `AI_API_KEY` | Gemini API 키 |
| `FCM_CREDENTIALS` | Firebase 서비스 계정 키 JSON을 Base64로 한 줄. 비우면 푸시를 보내지 않는다 |
| `DEV_LOGIN_ENABLED` | **넣지 않는다.** 켜면 서버가 시작되지 않는다 |

`DB_URL`과 `STORAGE_LOCAL_PATH`는 `docker-compose.prod.yml`이 덮어쓰므로 적지 않아도 된다.

### 1-5. 방화벽

- SSH 포트와 앱이 쓰는 포트(8080, 또는 앞단 프록시를 쓰면 80/443)만 연다.
- 5432(DB)는 열지 않는다. compose가 이미 포트를 게시하지 않는다.
- HTTPS는 이 배포에 포함돼 있지 않다. 도메인과 리버스 프록시(Nginx, Caddy 등)를 앞에 두고 8080으로 넘기는 구성을 따로 준비한다.

## 2. SSH 키

배포 전용 키를 새로 만든다. 개인 키는 GitHub 시크릿에만 넣는다.

```bash
# 내 PC에서
ssh-keygen -t ed25519 -f tail-deploy -N "" -C "tail-server-deploy"

# 공개 키를 서버의 deploy 사용자에게 등록
ssh-copy-id -i tail-deploy.pub deploy@<서버 주소>
```

서버 호스트 키(`SSH_KNOWN_HOSTS` 값)는 내 PC에서 받아 **지문을 서버 콘솔과 대조한 뒤** 쓴다.

```bash
ssh-keyscan -p <포트> <서버 주소>
```

시크릿에 넣은 뒤 PC에 남은 `tail-deploy` 개인 키 파일은 지운다.

## 3. GitHub 설정

저장소 Settings → Environments → **New environment** → 이름 `prod`.
(권장) Deployment protection rules에서 `main` 브랜치만 허용한다.

`prod` environment의 **Secrets**에 등록한다.

| 이름 | 값 |
|---|---|
| `SSH_HOST` | 서버 주소 |
| `SSH_PORT` | SSH 포트 (보통 22) |
| `SSH_USER` | `deploy` |
| `SSH_PRIVATE_KEY` | `tail-deploy` 개인 키 전체 (BEGIN/END 줄 포함) |
| `SSH_KNOWN_HOSTS` | `ssh-keyscan` 결과. 비워 두면 접속 시점의 키를 그대로 신뢰해 경고가 뜬다 |

GHCR 로그인은 워크플로의 `GITHUB_TOKEN`(`packages: read`)으로 하므로 따로 등록할 것이 없다.
패키지가 다른 저장소에 연결돼 있으면 Packages → tail-server → Package settings에서 이 저장소에 읽기 권한을 준다.

## 4. 첫 배포

1. 준비가 끝나면 `main`에 반영한다(릴리즈 PR). CI가 성공하면 Deploy가 자동으로 시작된다.
2. 또는 Actions → Deploy → **Run workflow**. `image_tag`를 비우면 `main` 최신 커밋을 배포한다.
3. 로그에 `OK: sha-xxxxxxx 배포 완료`가 나오면 성공이다.

첫 배포는 `current_tag`가 없어서 실패해도 롤백할 이전 버전이 없다. 실패하면 로그의 컨테이너 로그를 보고 `.env`부터 확인한다.

## 5. 확인

```bash
ssh deploy@<서버 주소>
cd /opt/tail-server
cat current_tag                          # 지금 떠 있는 태그
docker compose ps                        # postgres, app 모두 running/healthy
docker compose logs --tail 100 app
curl -i http://localhost:8080/api/v1/health-check
```

## 6. 롤백·재배포

- 이전 버전으로 되돌리기: Actions → Deploy → Run workflow → `image_tag`에 이전 태그(예: `sha-1a2b3c4`). 태그는 GHCR 패키지 목록이나 이전 Deploy 실행 로그에서 찾는다.
- 배포가 헬스 체크에 실패하면 스크립트가 직전 태그로 자동 복구한다. 이 경우에도 Deploy 실행은 실패로 표시되니 원인을 고친 뒤 다시 배포한다.
- DB 마이그레이션은 앱이 뜰 때 자동 적용된다. 롤백해도 이미 적용된 마이그레이션은 되돌아가지 않으므로, 컬럼 삭제 같은 변경은 한 번에 올리지 않는다.

## 문제 해결

| 증상 | 확인할 것 |
|---|---|
| `Permission denied (publickey)` | 공개 키가 `deploy` 사용자의 `~/.ssh/authorized_keys`에 있는지, 시크릿의 개인 키가 짝이 맞는지 |
| `Host key verification failed` | `SSH_KNOWN_HOSTS`가 현재 서버의 호스트 키와 같은지(서버를 새로 만들었다면 다시 받는다) |
| `permission denied ... docker.sock` | `deploy` 사용자가 `docker` 그룹인지 (그룹 추가 후 재접속 필요) |
| `pull access denied` / `denied` | 패키지 읽기 권한, Deploy 잡의 `packages: read` |
| 헬스 체크 실패, 서버가 안 뜸 | `docker compose logs app`. 대부분 `.env`의 누락·오타, DB 비밀번호 불일치, `DEV_LOGIN_ENABLED=true` |
| DB 비밀번호를 바꿨는데 접속 실패 | Postgres는 처음 만들 때의 비밀번호를 볼륨에 저장한다. `.env`만 바꾸면 반영되지 않는다 |
