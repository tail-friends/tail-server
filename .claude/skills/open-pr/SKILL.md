---
name: open-pr
description: 작업 브랜치를 push하고 develop으로 PR을 올린다. 리뷰어와 담당자를 지정하고 CI 결과를 확인한다. "PR 올려줘", "푸시하고 PR" 같은 요청에 사용한다.
---

# PR 올리기

AGENTS.md의 "PR"과 "AI 도구 사용 규칙"을 따른다. 사람이 PR을 요청했을 때만 실행한다.

## 순서

1. 현재 브랜치가 `feature/#N-...` 또는 `fix/#N-...`인지 확인한다. main·develop이면 멈춘다.
2. `./gradlew build`가 통과하는지 확인한다. 실패하면 PR을 올리지 않고 원인을 알린다.
3. 커밋 목록을 확인한다. 커밋 메시지가 `타입: 설명 (#N)` 형식이고 AI 도구 표시가 없는지 본다.
   ```bash
   git log --format='%s%n%b' develop..HEAD
   ```
4. push한다.
   ```bash
   git push -u origin "$(git branch --show-current)"
   ```
5. 리뷰어를 정한다. 백엔드 담당은 `kimseongchan0914`, `dx2eun`이다. 본인이 아닌 사람을 리뷰어로 지정한다.
   ```bash
   gh api user -q .login
   ```
6. `.github/pull_request_template.md` 형식으로 본문을 쓰고 PR을 만든다.
   ```bash
   gh pr create --base develop --title "<타입: 설명 (#N)>" --body "<본문>" --reviewer <리뷰어> --assignee @me
   ```
   - `closes #N`을 넣는다.
   - 확인하지 못한 항목은 체크하지 않고 이유를 적는다.
   - 노션 API 명세 변경, 마이그레이션 추가, 팀원 영향이 있으면 "참고"에 적는다.
7. CI 결과를 기다려 알려준다.
   ```bash
   gh pr checks --watch
   ```

## 머지

- 이 스킬은 머지하지 않는다.
- 사람이 머지를 요청하면 승인(`reviewDecision`이 `APPROVED`)과 Build 통과를 확인한 뒤 Squash로 머지하고, 연결된 이슈를 닫는다.
  ```bash
  gh pr view <번호> --json reviewDecision,mergeStateStatus
  gh pr merge <번호> --squash --delete-branch
  gh issue close <이슈번호> --reason completed --comment "#<PR번호> 에서 develop에 머지됨"
  ```
