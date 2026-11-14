#!/usr/bin/env bash
# 백업이 실제로 복원되는지 확인한다. 운영 DB와 사진 볼륨은 건드리지 않는다.
#
# 가장 최근 DB 덤프를 임시 postgres 컨테이너에 복원해 주요 테이블 행 수와 마이그레이션 버전을 보여 주고,
# 가장 최근 사진 백업이 있으면 압축을 풀지 않고 파일 수를 센다. 끝나면 임시 컨테이너를 지운다.
#
# 사용법: restore-check.sh [덤프 파일]   (비우면 backups/의 가장 최근 db-*.dump)
# 환경 변수: BACKUP_BASE_DIR (기본 /opt/tail-server)
set -euo pipefail

BASE=${BACKUP_BASE_DIR:-/opt/tail-server}
DIR="$BASE/backups"
CONTAINER=tailfriends-restore-check

dump=${1:-$(ls -t "$DIR"/db-*.dump 2>/dev/null | head -n 1 || true)}
if [ -z "$dump" ] || [ ! -f "$dump" ]; then
  echo "FAIL: 복원할 DB 덤프가 없습니다 ($DIR/db-*.dump)"
  exit 1
fi

cleanup() { docker rm -f "$CONTAINER" > /dev/null 2>&1 || true; }
trap cleanup EXIT
cleanup

echo "덤프: $dump"
docker run -d --name "$CONTAINER" -e POSTGRES_PASSWORD=restore-check postgres:16 > /dev/null
for _ in $(seq 1 30); do
  docker exec "$CONTAINER" pg_isready -U postgres > /dev/null 2>&1 && break
  sleep 1
done

# 운영 DB의 역할 이름이 임시 DB에는 없으므로 소유자·권한은 복원하지 않는다
docker exec -i "$CONTAINER" pg_restore -U postgres -d postgres --no-owner --no-privileges --exit-on-error < "$dump"

docker exec "$CONTAINER" psql -U postgres -d postgres -v ON_ERROR_STOP=1 -P pager=off -c "
  select 'flyway 최신 버전' as 항목, max(version) as 값 from flyway_schema_history where success
  union all select 'users', count(*)::text from users
  union all select 'animals', count(*)::text from animals
  union all select 'logs', count(*)::text from logs
  union all select 'photos', count(*)::text from photos;"

photos=$(ls -t "$DIR"/photos-*.tar.gz 2>/dev/null | head -n 1 || true)
if [ -n "$photos" ]; then
  echo "사진 백업: $photos ($(tar -tzf "$photos" | grep -cv '/$') 개 파일)"
else
  echo "사진 백업: 아직 없음"
fi

echo "OK: 복원 확인 완료"
