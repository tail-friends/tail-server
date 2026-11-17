# 운영 백업과 복원

운영 DB(PostgreSQL)와 사진은 운영 서버의 도커 볼륨(`postgres-data`, `photo-data`)에만 있다.
`scripts/backup/backup.sh`가 매일 이를 같은 서버의 `/opt/tail-server/backups`에 백업한다.

> 같은 서버에 두므로 **실수로 지운 데이터, 잘못된 마이그레이션, 컨테이너·볼륨 손상**은 복구할 수 있지만, **서버 자체(디스크)를 잃으면 백업도 같이 잃는다.** 외부 보관은 나중에 `backup.sh` 끝에 업로드를 더한다.

## 무엇을, 언제

| 대상 | 주기 | 파일 | 보관 |
|---|---|---|---|
| DB | 매일 (cron, 아래 시각) | `backups/db-<UTC 시각>.dump` (pg_dump custom 형식) | 14일 |
| 사진 볼륨 | 일요일(UTC)마다 | `backups/photos-<UTC 시각>.tar.gz` | 28일 |

- 덤프는 `pg_restore --list`로, 사진은 `gzip -t`로 깨지지 않았는지 확인한 뒤에만 정식 이름이 된다. 실패한 중간 파일(`*.part`)은 다음 실행 때 지운다.
- 마지막 성공 시각: `backups/last_success`. 실행 기록: `backups/backup.log`.
- `backups/`는 `deploy` 사용자만 읽을 수 있다(700). 사용자 데이터가 들어 있으므로 서버 밖으로 옮길 때도 조심한다.

## 설치 (자동)

Deploy가 실행될 때마다 한다. 따로 할 것은 없다.

1. `backup.sh`, `restore-check.sh`를 `/opt/tail-server/`에 올린다.
2. `deploy-remote.sh`가 `deploy` 사용자 crontab에 아래 줄을 등록한다(있으면 바꿔 적는다).
   ```
   30 18 * * * bash /opt/tail-server/backup.sh >> /opt/tail-server/backups/backup.log 2>&1
   ```
   서버 시간대가 UTC이므로 한국 시각 매일 03:30이다. 시각을 바꾸려면 `deploy-remote.sh`의 `install_backup_cron`을 고친다.

### 실패 알림 (선택, 권장)

서버 `/opt/tail-server/.env`에 Discord 웹훅 주소를 넣으면 백업이 실패할 때 알린다.

```bash
echo "BACKUP_DISCORD_WEBHOOK_URL=<웹훅 주소>" | sudo -u deploy tee -a /opt/tail-server/.env > /dev/null
```

## 확인

```bash
ssh deploy@<서버 주소>     # 다른 계정이면 아래 명령 앞에 sudo -u deploy
cd /opt/tail-server
crontab -l                       # 백업 줄이 있는지
cat backups/last_success         # 마지막 성공 시각 (UTC)
tail -n 20 backups/backup.log
ls -lh backups/
```

처음 설치한 뒤나 큰 변경 뒤에는 바로 한 번 돌려 본다.

```bash
bash backup.sh --photos          # 사진까지 지금 백업
```

## 복원 확인 (분기마다 한 번 이상)

복원해 본 적 없는 백업은 믿지 않는다. 아래는 **운영 DB를 건드리지 않고** 가장 최근 덤프를 임시 컨테이너에 복원해 주요 테이블 행 수를 보여 준다. CI의 Docker 작업도 매번 같은 스크립트로 백업·복원을 확인한다.

```bash
cd /opt/tail-server
bash restore-check.sh                          # 가장 최근 덤프
bash restore-check.sh backups/db-<시각>.dump   # 특정 덤프
```

`OK: 복원 확인 완료`와 함께 `users`, `animals` 수가 운영과 비슷하게 나오면 된다.

## 실제 복원 (운영 데이터를 덮어쓴다)

먼저 지금 상태를 한 번 더 백업하고(`bash backup.sh`), 팀에 알린 뒤 한다.

### DB

```bash
cd /opt/tail-server
docker compose stop app                        # 복원하는 동안 쓰기를 막는다
docker exec -i tailfriends-postgres sh -c \
  'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner --exit-on-error' \
  < backups/db-<시각>.dump
docker compose start app
curl -fsS http://localhost:8080/api/v1/health-check
```

- 덤프보다 최신 버전의 서버가 떠 있으면, 서버가 뜰 때 Flyway가 빠진 마이그레이션을 다시 적용한다. 덤프 시점의 버전으로 되돌리려면 Deploy를 그 시점 태그로 실행한다.

### 사진

```bash
cd /opt/tail-server
docker compose stop app
docker run --rm -v tail-server_photo-data:/data --entrypoint tar postgres:16 -xzf - -C /data \
  < backups/photos-<시각>.tar.gz
docker compose start app
```

압축을 기존 볼륨 위에 푼다. 백업 뒤에 올라온 사진은 그대로 남는다.

## 문제 해결

| 증상 | 확인할 것 |
|---|---|
| `last_success`가 하루 넘게 안 바뀜 | `backups/backup.log`의 `FAIL:` 줄, `crontab -l`, `systemctl is-active cron` |
| `FAIL: DB 덤프` | `docker ps`에 `tailfriends-postgres`가 떠 있는지 |
| `FAIL: 사진 백업` | 서버에 `postgres:16` 이미지가 있는지(`docker images postgres`), 볼륨 이름(`docker volume ls`) |
| 디스크가 찬다 | `df -h`, `du -sh backups/*`. 보관 기간은 `backup.sh`의 `DB_KEEP_DAYS`, `PHOTO_KEEP_DAYS` |
