---
name: release
description: develop을 main에 올리는 릴리즈 PR을 release/날짜 브랜치로 만든다. 포함된 PR, 새 마이그레이션, 환경 변수를 본문에 정리한다. "릴리즈 PR 올려줘", "main에 반영", "배포 PR" 같은 요청에 사용한다.
---

# 릴리즈 PR

AGENTS.md 2부의 "릴리즈"를 따른다. push와 PR 생성은 사람이 요청했을 때만 한다.

## 1. 브랜치

```bash
git status --short                                   # 깨끗해야 한다
git fetch origin
git checkout -b release/$(date +%Y-%m-%d) origin/develop
git merge -s ours --no-edit origin/main             # 내용은 develop 그대로, main 이력만 잇는다
```

머지 결과가 develop과 같은지 확인한다. 두 값이 다르면 멈추고 사람에게 알린다(main에만 있는 변경이 있다는 뜻이다).

```bash
git merge-tree --write-tree origin/main HEAD
git rev-parse 'origin/develop^{tree}'
```

## 2. 본문에 넣을 것

```bash
git log --oneline --first-parent origin/main..origin/develop     # 포함된 PR
git diff --name-only --diff-filter=A origin/main origin/develop -- src/main/resources/db/migration
git diff origin/main origin/develop -- .env.example              # 새 환경 변수
```

- 포함된 PR 목록(번호와 제목)
- 새 마이그레이션 파일. 운영 DB에 자동 적용된다는 점
- 운영 서버에 넣을 환경 변수(이름만, 값은 쓰지 않는다). 없으면 "없음"
- 앱·웹 팀이 알아야 할 API 변화

## 3. PR

```bash
git push -u origin release/<날짜>
gh pr create --base main --title "chore: 기능 릴리즈 (develop → main)" --body "<본문>" --reviewer dx2eun --assignee @me
gh pr checks --watch
```

- 리뷰어는 같은 파트의 다른 담당자다. 본인 계정은 `gh api user -q .login`으로 확인한다.
- 머지는 **Create a merge commit**으로 한다. 사람이 요청하면 승인과 Build를 확인한 뒤 `gh pr merge <번호> --merge`로 머지한다.
- 머지 후 release 브랜치를 지우고, 노션 "백엔드 진행 기록"의 main 최신 릴리즈와 "릴리즈와 작업 방식" 목록을 고친다.
