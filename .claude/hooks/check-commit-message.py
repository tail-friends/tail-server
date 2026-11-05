#!/usr/bin/env python3
"""PreToolUse(Bash) 훅. git commit의 -m 메시지가 AGENTS.md 커밋 규칙에 맞지 않으면 막는다.

- 첫 줄: `타입: 한글 설명 (#이슈번호)`
- 메시지 전체에 AI 도구 표시(Co-Authored-By: Claude, Generated with Claude Code 등)가 없어야 한다

-m으로 메시지를 넘기지 않은 커밋(-F, 편집기, --amend --no-edit 등)은 읽을 수 없어 검사하지 않는다.
"""
import json
import re
import shlex
import sys

TYPES = ("feat", "fix", "refactor", "docs", "test", "chore", "style")
SUBJECT = re.compile(r"^(%s): .+ \(#\d+\)$" % "|".join(TYPES))
# 트레일러와 서명은 줄 맨 앞에 온다. .github/scripts/check-pr.sh와 같은 기준이다
AI_MARKS = re.compile(
    r"^\s*co-authored-by:.*(claude|anthropic|codex|openai|copilot)"
    r"|^\s*generated (with|by) \[?(claude|codex|chatgpt|copilot)"
    r"|🤖",
    re.IGNORECASE | re.MULTILINE,
)


def messages(command):
    """명령에 있는 git commit마다 -m 값들을 이어 붙인 메시지를 돌려준다."""
    try:
        tokens = shlex.split(command, posix=True)
    except ValueError:
        return []
    result = []
    i = 0
    while i < len(tokens):
        if tokens[i] == "git" and "commit" in tokens[i + 1:i + 4]:
            j = tokens.index("commit", i + 1) + 1
            parts = []
            while j < len(tokens) and tokens[j] not in ("&&", "||", ";", "|"):
                token = tokens[j]
                if token == "--message" and j + 1 < len(tokens):
                    parts.append(tokens[j + 1])
                    j += 2
                    continue
                if token.startswith("--message="):
                    parts.append(token.split("=", 1)[1])
                elif re.match(r"^-[a-zA-Z]*m", token):
                    # -m, -qm, -qam처럼 짧은 옵션을 붙여 쓴 경우. m 뒤에 글자가 있으면 그게 메시지다
                    rest = token[token.index("m") + 1:]
                    if rest:
                        parts.append(rest)
                    elif j + 1 < len(tokens):
                        parts.append(tokens[j + 1])
                        j += 2
                        continue
                j += 1
            if parts:
                result.append("\n\n".join(parts))
            i = j
        else:
            i += 1
    return result


def problems(message):
    found = []
    subject = message.splitlines()[0] if message else ""
    if not SUBJECT.match(subject):
        found.append(
            "첫 줄이 `타입: 한글 설명 (#이슈번호)` 형식이 아니에요 (타입: %s): %s" % (", ".join(TYPES), subject))
    if AI_MARKS.search(message):
        found.append("커밋 메시지에 AI 도구 표시를 넣지 않아요 (AGENTS.md AI 도구 사용 규칙)")
    return found


def main():
    payload = json.load(sys.stdin)
    command = payload.get("tool_input", {}).get("command", "")
    found = [problem for message in messages(command) for problem in problems(message)]
    if not found:
        return 0
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": "커밋 규칙 위반: " + " / ".join(found),
        }
    }, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
