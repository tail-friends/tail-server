---
name: start-task
description: 새 작업을 시작할 때 GitHub 이슈를 만들고 develop에서 이슈 번호로 브랜치를 딴다. "작업 시작", "이슈 만들고 브랜치", "새 기능 시작" 같은 요청에 사용한다.
---

# 작업 시작

AGENTS.md의 "작업 흐름"과 "브랜치" 규칙을 따른다.

## 순서

1. 작업 내용을 확인한다. 이슈 제목과 할 일이 분명하지 않으면 사람에게 묻는다.
2. 이슈를 만든다. 담당자는 본인이다.
   ```bash
   gh issue create --title "<제목>" --body "<## 작업 내용 목록>" --assignee @me
   ```
3. 작업 트리가 깨끗한지 확인한다. 커밋하지 않은 변경이 있으면 멈추고 사람에게 알린다.
   ```bash
   git status --short
   ```
4. develop을 최신으로 받고 브랜치를 딴다.
   ```bash
   git checkout develop && git pull
   git checkout -b "feature/#<이슈번호>-<영문-설명>"   # 버그 수정은 fix/
   ```
5. 이슈 링크와 브랜치 이름을 알려준다.

## 주의

- 브랜치 설명은 영문 소문자와 `-`만 쓴다. 예: `feature/#12-animal-api`
- main에서 브랜치를 따지 않는다. (hotfix 제외)
