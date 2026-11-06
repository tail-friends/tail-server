#!/usr/bin/env python3
"""PostToolUse(Edit|Write) 훅. 고친 Java 파일에 120자를 넘는 줄이 있으면 Claude에게 알려 고치게 한다.

글자 수는 바이트가 아니라 문자로 센다(한글 주석도 한 글자).
"""
import json
import sys

MAX_LENGTH = 120
MAX_REPORTED = 10


def main():
    payload = json.load(sys.stdin)
    path = payload.get("tool_input", {}).get("file_path") or payload.get("tool_response", {}).get("filePath")
    if not path or not path.endswith(".java"):
        return 0
    try:
        with open(path, encoding="utf-8") as file:
            long_lines = [(number, len(line.rstrip("\n")))
                          for number, line in enumerate(file, 1) if len(line.rstrip("\n")) > MAX_LENGTH]
    except OSError:
        return 0
    if not long_lines:
        return 0
    lines = ", ".join("%d행(%d자)" % line for line in long_lines[:MAX_REPORTED])
    more = " 외 %d줄" % (len(long_lines) - MAX_REPORTED) if len(long_lines) > MAX_REPORTED else ""
    print(json.dumps({
        "decision": "block",
        "reason": "%s에 %d자를 넘는 줄이 있어요: %s%s. 줄을 나눠 주세요." % (path, MAX_LENGTH, lines, more),
    }, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
