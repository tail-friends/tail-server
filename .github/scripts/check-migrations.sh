#!/usr/bin/env bash
# PR의 Flyway 마이그레이션을 대상 브랜치와 비교한다.
#   1. 새로 추가한 마이그레이션 버전은 대상 브랜치의 마지막 버전보다 커야 한다.
#      CI는 빈 DB에서 시작해 통과하지만, 이미 마이그레이션을 적용한 DB는 낮은 버전이 끼어들면 서버가 뜨지 않는다.
#   2. 대상 브랜치에 이미 있는 마이그레이션은 고치거나 지우지 않는다.
#
# 사용: check-migrations.sh <대상 브랜치 ref>   예: check-migrations.sh origin/develop
# 현재 작업 트리(HEAD)를 PR 결과로 보고 비교한다.
set -euo pipefail

base_ref=${1:?대상 브랜치 ref가 필요합니다. 예: origin/develop}
dir=src/main/resources/db/migration

version_of() {
  sed -n 's/^V\([0-9][0-9]*\)__.*\.sql$/\1/p' <<< "$1"
}

base_files=$(git ls-tree --name-only "$base_ref" "$dir/" | xargs -n1 basename | sort)
head_files=$(ls "$dir" | sort)
latest=$(for f in $base_files; do version_of "$f"; done | sort -n | tail -1)

failed=0

for f in $(comm -13 <(echo "$base_files") <(echo "$head_files")); do
  version=$(version_of "$f")
  if [ -z "$version" ]; then
    echo "::error file=${dir}/${f}::파일 이름이 V{yyyyMMddHHmm}__{설명}.sql 형식이 아닙니다."
    failed=1
  elif [ -n "$latest" ] && [ "$version" -le "$latest" ]; then
    echo "::error file=${dir}/${f}::버전 ${version}이 ${base_ref}의 마지막 마이그레이션(${latest})보다 크지 않습니다. 파일 이름의 시각을 지금 시각으로 바꾸세요."
    failed=1
  fi
done

for f in $base_files; do
  if ! git diff --quiet "$base_ref" -- "$dir/$f"; then
    echo "::error file=${dir}/${f}::이미 ${base_ref}에 있는 마이그레이션이 바뀌었거나 지워졌습니다. 바꿀 내용은 새 파일로 추가하세요."
    failed=1
  fi
done

if [ "$failed" -eq 0 ]; then
  echo "마이그레이션 확인 통과 (${base_ref} 마지막 버전: ${latest:-없음})"
fi
exit "$failed"
