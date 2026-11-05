#!/usr/bin/env bash
# PR 제목·본문·커밋 메시지가 AGENTS.md의 커밋·PR 규칙을 지키는지 검사한다.
#   1. 제목은 `타입: 설명 (#이슈번호)`이다. Squash 머지하면 제목이 그대로 develop의 커밋 메시지가 된다.
#      main으로 가는 릴리즈 PR은 `chore: 기능 릴리즈 (develop → main)`처럼 끝을 `(develop → main)`으로 쓴다.
#   2. 제목·본문·커밋 메시지에 AI 도구 표시(Co-Authored-By: Claude, Generated with Claude Code 등)가 없다.
# Claude Code 훅(.claude/hooks/check-commit-message.py)은 Claude Code 안에서만 돌아서, 사람이 직접 쓴 것도 여기서 확인한다.
#
# 사용: PR_TITLE, PR_BODY, BASE_REF, PR_NUMBER, GITHUB_REPOSITORY, GH_TOKEN 환경 변수를 넣고 실행한다.
set -euo pipefail

: "${PR_TITLE:?PR_TITLE이 필요합니다}"
: "${BASE_REF:?BASE_REF가 필요합니다}"
: "${PR_NUMBER:?PR_NUMBER가 필요합니다}"
: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY가 필요합니다}"

types='feat|fix|refactor|docs|test|chore|style'
ai_marks='co-authored-by:.*(claude|anthropic|codex|openai|copilot)|generated with [[]?claude|generated (by|with) (codex|chatgpt|copilot)'

failed=0

has_ai_mark() {
  grep -iqE "$ai_marks" <<< "$1" || grep -qF '🤖' <<< "$1"
}

if [ "$BASE_REF" = main ]; then
  title_pattern="^($types): .+ \((#[0-9]+|develop → main)\)$"
else
  title_pattern="^($types): .+ \(#[0-9]+\)$"
fi
if ! grep -qE "$title_pattern" <<< "$PR_TITLE"; then
  echo "::error::PR 제목이 '타입: 설명 (#이슈번호)' 형식이 아닙니다: $PR_TITLE"
  failed=1
fi

if has_ai_mark "$PR_TITLE"$'\n'"${PR_BODY:-}"; then
  echo "::error::PR 제목이나 본문에 AI 도구 표시가 있습니다. 지워 주세요."
  failed=1
fi

# 커밋 목록 응답에 메시지가 들어 있어 jq로 한 번에 거른다
marked=$(gh api --paginate "repos/$GITHUB_REPOSITORY/pulls/$PR_NUMBER/commits" -q "
  .[] | select(.commit.message | test(\"$ai_marks|🤖\"; \"i\"))
  | .sha[0:7] + \" \" + (.commit.message | split(\"\\n\")[0])")
if [ -n "$marked" ]; then
  while IFS= read -r line; do
    echo "::error::커밋 메시지에 AI 도구 표시가 있습니다: $line"
  done <<< "$marked"
  failed=1
fi

exit "$failed"
