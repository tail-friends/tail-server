---
name: merge-pr
description: 기능 PR을 Squash로 머지하고 뒷정리(이슈 닫기, 로컬 브랜치 정리, 노션 반영)를 한다. 사람이 이미 머지했다고 하면 뒷정리만 한다. "머지해줘", "머지했어", "머지함" 같은 요청에 사용한다.
---

# PR 머지와 뒷정리

AGENTS.md의 "PR"과 "작업 흐름"을 따른다. 머지는 사람이 요청했을 때만 한다. 릴리즈 PR(develop → main)은 `release` 스킬을 쓴다.

## 1. 머지 (사람이 "머지해줘"라고 했을 때)

1. PR 상태를 확인한다. 승인(`APPROVED`)이 없거나 Build가 통과하지 않았으면 머지하지 않고 알린다.
   ```bash
   gh pr view <번호> --json state,reviewDecision,mergeStateStatus,baseRefName,headRefName
   gh pr checks <번호>
   ```
2. base가 `develop`인 기능 PR이면 Squash로 머지한다.
   ```bash
   gh pr merge <번호> --squash --delete-branch
   ```

## 2. 머지 방식 확인 (사람이 직접 머지했을 때도)

머지 커밋의 부모 수로 머지 방식을 확인한다. 1이면 Squash, 2면 Create a merge commit이다.

```bash
oid=$(gh pr view <번호> --json mergeCommit -q .mergeCommit.oid)
gh api repos/tail-friends/tail-server/commits/$oid -q '.parents | length'
```

- 기능 PR인데 2가 나오면 사람에게 알린다. 이미 들어간 것은 되돌리지 않는다(force push 금지). 다음 PR부터 머지 버튼 옆 화살표에서 **Squash and merge**를 고르도록 안내한다.
- 상태가 `MERGED`가 아니면 뒷정리를 하지 않고 알린다.

## 3. 뒷정리

1. 연결된 이슈를 닫는다. `develop`은 기본 브랜치가 아니라 `closes #N`으로 자동으로 닫히지 않는다.
   ```bash
   gh issue close <이슈번호> --reason completed --comment "#<PR번호> 에서 develop에 머지됨"
   ```
2. 로컬을 정리한다. 커밋하지 않은 변경이 있으면 멈추고 알린다.
   ```bash
   git status --short
   git checkout develop && git pull
   git branch -D <작업 브랜치>   # Squash 머지는 이력이 달라 -d로 지워지지 않는다
   ```
3. 노션을 반영한다.
   - 작업 보드 카드: 그 카드의 범위가 다 끝났으면 상태를 `완료`로, 남았으면 본문 진행 상황에 끝난 부분과 PR 번호를 적는다.
   - "백엔드 진행 기록" 페이지: 기준일, "한눈에" 표, 단계별 끝난 것(PR 번호), 앱·웹 팀에 전할 것.
   - 노션에 접근할 수 없으면 사람에게 알리고 넘어간다.
4. 사람에게 알린다: 머지 방식, 닫은 이슈, 바꾼 노션, `main`에 아직 안 들어간 PR(릴리즈할 때가 됐는지).
