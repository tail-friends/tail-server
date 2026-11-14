#!/usr/bin/env bash
# 운영 서버에서 실행된다 (deploy.yml이 /opt/tail-server/deploy.sh 로 올려 실행).
# 환경 변수: IMAGE_TAG(배포할 태그), GHCR_USER. 표준 입력 첫 줄: GHCR 읽기 토큰.
set -uo pipefail
cd /opt/tail-server

DOCKER_CONFIG=$(mktemp -d)   # 로그인 정보를 서버에 남기지 않는다
export DOCKER_CONFIG
trap 'rm -rf "$DOCKER_CONFIG"' EXIT

read -r GHCR_TOKEN
printf '%s' "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin || exit 1

# 부팅에 20~40초 걸리므로 최대 3분까지 기다린다
wait_healthy() {
  for _ in $(seq 1 60); do
    if curl -fsS http://localhost:8080/api/v1/health-check > /dev/null 2>&1; then
      return 0
    fi
    sleep 3
  done
  return 1
}

# 띄울 태그를 .env의 IMAGE_TAG에도 적는다. 서버에서 IMAGE_TAG 없이 docker compose ps/logs/up을 바로 쓸 수 있다.
# .env는 app의 env_file이기도 해서, up 뒤에 바꾸면 다음 수동 up 때 app이 다시 만들어진다. 그래서 up 전에 적는다.
set_image_tag() {
  if grep -q '^IMAGE_TAG=' .env; then
    sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=$1/" .env
  else
    printf '\n# 배포 스크립트가 적는다. 직접 고치지 않는다\nIMAGE_TAG=%s\n' "$1" >> .env
  fi
}

PREVIOUS_TAG=$(cat current_tag 2>/dev/null || true)

export IMAGE_TAG
docker compose pull app || exit 1
set_image_tag "$IMAGE_TAG" || exit 1
docker compose up -d --remove-orphans

if wait_healthy; then
  echo "$IMAGE_TAG" > current_tag
  # 바뀐 프로메테우스 설정을 다시 읽는다. 모니터링 문제로 배포를 실패시키지는 않는다
  docker compose kill -s SIGHUP prometheus > /dev/null 2>&1 || echo "WARN: 프로메테우스 설정을 다시 읽지 못했습니다"
  echo "OK: $IMAGE_TAG 배포 완료"
  docker image prune -f > /dev/null
  exit 0
fi

echo "FAIL: 서버가 정상 기동하지 않았습니다"
docker compose logs --tail 80 app || true

if [ -n "$PREVIOUS_TAG" ]; then
  echo "이전 버전($PREVIOUS_TAG)으로 롤백합니다"
  export IMAGE_TAG="$PREVIOUS_TAG"
  set_image_tag "$PREVIOUS_TAG"
  docker compose up -d
  if wait_healthy; then
    echo "ROLLBACK OK: 이전 버전으로 복구했습니다. 이번 배포는 실패입니다"
  else
    echo "ROLLBACK FAIL: 이전 버전도 기동하지 않습니다. 서버를 직접 확인하세요"
  fi
fi
exit 1
