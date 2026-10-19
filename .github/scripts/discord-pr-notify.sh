#!/usr/bin/env bash
# PR 정보를 Discord 웹훅으로 보낸다. 값은 모두 환경 변수로 받는다.
#   DISCORD_WEBHOOK_URL       Discord 채널 웹훅 주소 (시크릿)
#   DISCORD_MENTIONS          메시지 앞에 붙일 멘션. 예: "<@123> <@456>" 또는 "<@&역할ID>"
#   PR_TITLE, PR_URL, PR_NUMBER, PR_AUTHOR, PR_BASE, PR_BODY
#   DRY_RUN=1이면 보내지 않고 JSON만 출력한다.
set -euo pipefail

# 붙여 넣을 때 섞인 공백·줄바꿈을 지운다
webhook_url=$(printf '%s' "${DISCORD_WEBHOOK_URL:-}" | tr -d '[:space:]')

if [ -z "$webhook_url" ] && [ "${DRY_RUN:-}" != "1" ]; then
  echo "DISCORD_WEBHOOK_URL 시크릿이 없어 알림을 건너뜁니다."
  exit 0
fi

if [ "${DRY_RUN:-}" != "1" ] && [[ "$webhook_url" != https://discord.com/api/webhooks/* ]]; then
  echo "::error::DISCORD_WEBHOOK_URL이 Discord 웹훅 주소 형식(https://discord.com/api/webhooks/...)이 아닙니다. 시크릿 값을 다시 등록하세요."
  exit 1
fi

body=$(printf '%s\n' "${PR_BODY:-}" | tr -d '\r' | sed '/<!--.*-->/d')

# "## 제목" 아래부터 다음 "## " 전까지
section() {
  printf '%s\n' "$body" | awk -v h="## $1" '$0 == h {f=1; next} /^## /{f=0} f'
}

overview=$(section "개요" | grep -viE '^(closes|fixes|resolves) #' | sed '/^[[:space:]]*$/d' || true)
changes=$(section "변경 사항" | grep -vE '^[[:space:]]*-?[[:space:]]*$' | head -n 10 || true)

description="${overview:-PR 본문에 개요가 없습니다.}"
if [ -n "$changes" ]; then
  description="${description}"$'\n\n'"**변경 사항**"$'\n'"${changes}"
fi
# Discord embed 설명은 4096자까지
description=${description:0:3500}

payload=$(jq -n \
  --arg content "${DISCORD_MENTIONS:-} 리뷰 요청: 새 PR이 올라왔어요" \
  --arg title "#${PR_NUMBER} ${PR_TITLE}" \
  --arg url "$PR_URL" \
  --arg description "$description" \
  --arg author "$PR_AUTHOR" \
  --arg base "$PR_BASE" \
  '{
    content: $content,
    allowed_mentions: { parse: ["users", "roles"] },
    embeds: [{
      title: $title,
      url: $url,
      description: $description,
      color: 5814783,
      fields: [
        { name: "작성자", value: $author, inline: true },
        { name: "대상 브랜치", value: $base, inline: true }
      ]
    }]
  }')

if [ "${DRY_RUN:-}" = "1" ]; then
  printf '%s\n' "$payload"
  exit 0
fi

curl -fsS -H "Content-Type: application/json" -d "$payload" "$webhook_url"
echo "Discord 알림을 보냈습니다."
