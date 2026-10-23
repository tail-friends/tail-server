#!/usr/bin/env bash
# GitHub 이벤트를 Discord 웹훅으로 알린다.
#
# 사용법: discord-notify.sh <pr-opened | pr-merged | review | ci-failed>
#
# 공통 환경 변수
#   DISCORD_WEBHOOK_URL       Discord 채널 웹훅 주소 (시크릿)
#   DISCORD_USER_MAP          GitHub 아이디 → Discord 사용자 ID JSON (변수). 예: {"dx2eun":"123..."}
#   DISCORD_BACKEND_MENTIONS  리뷰어를 모를 때 태그할 멘션 (변수). 예: "<@123> <@456>"
#   DRY_RUN=1이면 보내지 않고 JSON만 출력한다.
#
# pr-opened: PR_TITLE, PR_URL, PR_NUMBER, PR_AUTHOR, PR_BASE, PR_BODY, PR_REVIEWERS(공백 구분 아이디)
# pr-merged: PR_TITLE, PR_URL, PR_NUMBER, PR_AUTHOR, PR_BASE, PR_BODY, PR_MERGED_BY
# review:    PR_TITLE, PR_URL, PR_NUMBER, PR_AUTHOR, REVIEW_STATE, REVIEW_AUTHOR, REVIEW_BODY, REVIEW_URL
# ci-failed: CI_BRANCH, CI_ACTOR, CI_COMMIT_SHA, CI_COMMIT_MESSAGE, CI_RUN_URL
set -euo pipefail

kind=${1:?"알림 종류가 필요합니다: pr-opened | pr-merged | review | ci-failed"}

# 붙여 넣을 때 섞인 공백·줄바꿈을 지운다
webhook_url=$(printf '%s' "${DISCORD_WEBHOOK_URL:-}" | tr -d '[:space:]')

if [ "${DRY_RUN:-}" != "1" ]; then
  if [ -z "$webhook_url" ]; then
    echo "DISCORD_WEBHOOK_URL 시크릿이 없어 알림을 건너뜁니다."
    exit 0
  fi
  if [[ "$webhook_url" != https://discord.com/api/webhooks/* ]]; then
    echo "::error::DISCORD_WEBHOOK_URL이 Discord 웹훅 주소 형식(https://discord.com/api/webhooks/...)이 아닙니다. 시크릿 값을 다시 등록하세요."
    exit 1
  fi
fi

user_map=${DISCORD_USER_MAP:-}
if [ -z "$user_map" ] || ! printf '%s' "$user_map" | jq -e 'type == "object"' > /dev/null 2>&1; then
  user_map='{}'
fi

# GitHub 아이디를 Discord 멘션으로. 매핑이 없으면 아이디를 글자로 쓴다.
mention() {
  local id
  id=$(printf '%s' "$user_map" | jq -r --arg login "$1" '.[$login] // empty')
  if [ -n "$id" ]; then
    printf '<@%s>' "$id"
  else
    printf '@%s' "$1"
  fi
}

# 매핑된 사람만 멘션으로 (매핑이 없으면 빈 문자열)
mention_if_mapped() {
  local id
  id=$(printf '%s' "$user_map" | jq -r --arg login "$1" '.[$login] // empty')
  if [ -n "$id" ]; then
    printf '<@%s>' "$id"
  fi
}

send() {
  local payload=$1
  if [ "${DRY_RUN:-}" = "1" ]; then
    printf '%s\n' "$payload"
    return
  fi
  curl -fsS -H "Content-Type: application/json" -d "$payload" "$webhook_url"
  echo "Discord 알림을 보냈습니다. ($kind)"
}

# content와 embed 하나로 메시지를 만든다
message() {
  local content=$1 title=$2 url=$3 description=$4 color=$5 fields=$6
  jq -n \
    --arg content "$content" \
    --arg title "$title" \
    --arg url "$url" \
    --arg description "${description:0:3500}" \
    --argjson color "$color" \
    --argjson fields "$fields" \
    '{
      content: $content,
      allowed_mentions: { parse: ["users", "roles"] },
      embeds: [{ title: $title, url: $url, description: $description, color: $color, fields: $fields }]
    }'
}

field() {
  jq -n --arg name "$1" --arg value "$2" '{ name: $name, value: $value, inline: true }'
}

# PR 본문에서 "## 제목" 아래부터 다음 "## " 전까지
pr_section() {
  printf '%s\n' "${PR_BODY:-}" | tr -d '\r' | sed '/<!--.*-->/d' \
    | awk -v h="## $1" '$0 == h {f=1; next} /^## /{f=0} f'
}

pr_overview() {
  pr_section "개요" | grep -viE '^(closes|fixes|resolves) #' | sed '/^[[:space:]]*$/d' || true
}

pr_opened() {
  local overview changes description mentions="" reviewer author_mention
  overview=$(pr_overview)
  changes=$(pr_section "변경 사항" | grep -vE '^[[:space:]]*-?[[:space:]]*$' | head -n 10 || true)

  description="${overview:-PR 본문에 개요가 없습니다.}"
  if [ -n "$changes" ]; then
    description="${description}"$'\n\n'"**변경 사항**"$'\n'"${changes}"
  fi

  for reviewer in ${PR_REVIEWERS:-}; do
    mentions="$mentions $(mention "$reviewer")"
  done
  if [ -z "${mentions// /}" ]; then
    # 리뷰어를 모르면 백엔드 팀 전체. 작성자 본인은 빼고 태그한다.
    mentions=${DISCORD_BACKEND_MENTIONS:-}
    author_mention=$(mention_if_mapped "$PR_AUTHOR")
    if [ -n "$author_mention" ]; then
      mentions=${mentions//$author_mention/}
    fi
  fi
  mentions=$(printf '%s' "$mentions" | xargs)

  send "$(message \
    "${mentions:+$mentions }리뷰 요청: 새 PR이 올라왔어요" \
    "#${PR_NUMBER} ${PR_TITLE}" \
    "$PR_URL" \
    "$description" \
    5814783 \
    "$(jq -n --argjson a "$(field "작성자" "$PR_AUTHOR")" --argjson b "$(field "대상 브랜치" "$PR_BASE")" '[$a, $b]')")"
}

pr_merged() {
  local overview
  overview=$(pr_overview)

  send "$(message \
    "🎉 #${PR_NUMBER} PR이 ${PR_BASE}에 머지됐어요." \
    "#${PR_NUMBER} ${PR_TITLE}" \
    "$PR_URL" \
    "${overview:-PR 본문에 개요가 없습니다.}" \
    10181046 \
    "$(jq -n \
      --argjson a "$(field "작성자" "$PR_AUTHOR")" \
      --argjson b "$(field "머지한 사람" "${PR_MERGED_BY:-알 수 없음}")" \
      --argjson c "$(field "브랜치" "$PR_BASE")" \
      '[$a, $b, $c]')")"
}

review() {
  local state headline color review_body
  state=$(printf '%s' "${REVIEW_STATE:-}" | tr '[:upper:]' '[:lower:]')
  case "$state" in
    approved)
      headline="✅ 승인됐어요. Build가 통과했으면 머지할 수 있어요."
      color=5763719
      ;;
    changes_requested)
      headline="🔁 변경 요청이 왔어요."
      color=15548997
      ;;
    *)
      echo "리뷰 상태가 ${REVIEW_STATE}라 알림을 보내지 않습니다."
      return
      ;;
  esac

  review_body=$(printf '%s' "${REVIEW_BODY:-}" | tr -d '\r')
  if [ -z "${review_body// /}" ]; then
    review_body="(리뷰 코멘트 없음)"
  fi

  send "$(message \
    "$(mention "$PR_AUTHOR") $headline" \
    "#${PR_NUMBER} ${PR_TITLE}" \
    "${REVIEW_URL:-$PR_URL}" \
    "${review_body:0:1000}" \
    "$color" \
    "$(jq -n --argjson a "$(field "리뷰어" "$REVIEW_AUTHOR")" '[$a]')")"
}

ci_failed() {
  local first_line
  first_line=$(printf '%s\n' "${CI_COMMIT_MESSAGE:-}" | head -n 1)

  send "$(message \
    "$(mention "$CI_ACTOR") 🚨 ${CI_BRANCH} 빌드가 실패했어요." \
    "${CI_BRANCH} Build 실패" \
    "$CI_RUN_URL" \
    "${first_line:-커밋 메시지 없음}" \
    15548997 \
    "$(jq -n --argjson a "$(field "커밋" "${CI_COMMIT_SHA:0:7}")" --argjson b "$(field "푸시한 사람" "$CI_ACTOR")" '[$a, $b]')")"
}

case "$kind" in
  pr-opened) pr_opened ;;
  pr-merged) pr_merged ;;
  review) review ;;
  ci-failed) ci_failed ;;
  *)
    echo "::error::알 수 없는 알림 종류: $kind"
    exit 1
    ;;
esac
