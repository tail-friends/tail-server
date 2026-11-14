#!/usr/bin/env bash
# 운영 DB와 사진을 백업한다. 운영 서버에서 cron으로 매일 실행된다(deploy-remote.sh가 deploy 사용자 crontab에 등록).
#
# - DB: 매일 pg_dump(custom 형식) → backups/db-<UTC 시각>.dump, DB_KEEP_DAYS일 보관
# - 사진 볼륨: 일요일(UTC)마다, 또는 --photos를 주면 → backups/photos-<UTC 시각>.tar.gz, PHOTO_KEEP_DAYS일 보관
# - 실패하면 .env의 BACKUP_DISCORD_WEBHOOK_URL로 알린다(없으면 로그에만 남긴다).
#
# 복원과 복구 확인은 docs/backup.md, scripts/backup/restore-check.sh 참고.
#
# 환경 변수(모두 선택. CI는 BACKUP_BASE_DIR만 바꿔서 실행한다)
#   BACKUP_BASE_DIR    .env가 있는 폴더 (기본 /opt/tail-server). 백업은 그 아래 backups/에 둔다
#   POSTGRES_CONTAINER (기본 tailfriends-postgres)
#   PHOTO_VOLUME       (기본 tail-server_photo-data)
set -eEuo pipefail

BASE=${BACKUP_BASE_DIR:-/opt/tail-server}
DIR="$BASE/backups"
POSTGRES_CONTAINER=${POSTGRES_CONTAINER:-tailfriends-postgres}
PHOTO_VOLUME=${PHOTO_VOLUME:-tail-server_photo-data}
DB_KEEP_DAYS=14
PHOTO_KEEP_DAYS=28

log() { echo "$(date -u +%FT%TZ) $*"; }

# .env는 여러 줄 값(APPLE_PRIVATE_KEY 등)이 있어 source하지 않고 필요한 키만 읽는다
env_value() { grep -m1 "^$1=" "$BASE/.env" 2>/dev/null | cut -d= -f2- || true; }

step="준비"
notify_failure() {
  local url
  url=$(env_value BACKUP_DISCORD_WEBHOOK_URL | tr -d '[:space:]')
  [ -n "$url" ] || return 0
  curl -fsS -m 10 -H 'Content-Type: application/json' \
    -d "{\"content\":\"🚨 운영 백업이 실패했어요. 단계: $step. 서버의 $DIR/backup.log를 확인하세요.\"}" \
    "$url" > /dev/null || log "WARN: Discord 알림을 보내지 못했습니다"
}
trap 'log "FAIL: $step"; notify_failure' ERR

umask 077   # 백업에는 사용자 데이터가 있으므로 deploy 사용자만 읽는다
mkdir -p "$DIR"
ts=$(date -u +%Y%m%dT%H%M%SZ)

step="DB 덤프"
db_file="$DIR/db-$ts.dump"
docker exec "$POSTGRES_CONTAINER" sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$db_file.part"
# 덤프 목록을 읽어 파일이 깨지지 않았는지 확인한 뒤에만 정식 이름으로 바꾼다
docker exec -i "$POSTGRES_CONTAINER" pg_restore --list < "$db_file.part" > /dev/null
mv "$db_file.part" "$db_file"
log "OK: DB $(du -h "$db_file" | cut -f1) $db_file"

if [ "${1:-}" = "--photos" ] || [ "$(date -u +%u)" = 7 ]; then
  step="사진 백업"
  photo_file="$DIR/photos-$ts.tar.gz"
  # tar만 쓰면 되므로 서버에 이미 있는 postgres 이미지를 빌려 쓴다(따로 pull하지 않는다)
  docker run --rm -v "$PHOTO_VOLUME:/data:ro" --entrypoint tar postgres:16 -czf - -C /data . > "$photo_file.part"
  gzip -t "$photo_file.part"
  mv "$photo_file.part" "$photo_file"
  log "OK: 사진 $(du -h "$photo_file" | cut -f1) $photo_file"
fi

step="오래된 백업 정리"
find "$DIR" -maxdepth 1 -name 'db-*.dump' -mtime +"$DB_KEEP_DAYS" -delete
find "$DIR" -maxdepth 1 -name 'photos-*.tar.gz' -mtime +"$PHOTO_KEEP_DAYS" -delete
find "$DIR" -maxdepth 1 -name '*.part' -mmin +60 -delete

date -u +%FT%TZ > "$DIR/last_success"
log "OK: 백업 완료"
