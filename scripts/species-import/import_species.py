#!/usr/bin/env python3
"""노션 "종 원본 표"에서 내보낸 CSV를 관리자 API로 일괄 등록한다.

상태가 "검수 완료"인 행만 보내고, 서버에 이미 있는 이름은 건너뛴다. 그래서 여러 번 돌려도 안전하다.
표준 라이브러리만 쓴다 (Python 3.9 이상). 사용법은 같은 폴더의 README.md.
"""

import argparse
import csv
import json
import os
import sys
import urllib.error
import urllib.request

READY_STATUS = "검수 완료"
TOKEN_ENV = "TAIL_ADMIN_TOKEN"

# 노션 열 이름 → API 필드. 숫자 열은 (필드, 정수 여부)
TEXT_COLUMNS = {
    "분류": "category",
    "체형": "shape",
    "기본 사육장": "defaultHome",
    "영문명": "englishName",
    "학명": "scientificName",
}
NUMBER_COLUMNS = {
    "급여 주기(일)": ("feedIntervalDays", True),
    "탈피 주기(일)": ("shedIntervalDays", True),
    "핫존 최소(℃)": ("hotMin", False),
    "핫존 최대(℃)": ("hotMax", False),
    "쿨존 최소(℃)": ("coolMin", False),
    "쿨존 최대(℃)": ("coolMax", False),
    "습도 최소(%)": ("humidityMin", True),
    "습도 최대(%)": ("humidityMax", True),
    "노출 순서": ("displayOrder", True),
}
REQUIRED_COLUMNS = ["이름", "상태", "체형", "급여 주기(일)", "기본 사육장"]
REQUIRED_FIELDS = {"shape": "체형", "feedIntervalDays": "급여 주기(일)", "defaultHome": "기본 사육장"}


class RowError(Exception):
    pass


def main():
    parser = argparse.ArgumentParser(description="종 원본 표 CSV를 관리자 API로 등록한다.")
    parser.add_argument("csv_path", help="노션에서 내보낸 CSV 파일")
    parser.add_argument("--base-url", default="http://localhost:8080", help="서버 주소 (기본 http://localhost:8080)")
    parser.add_argument("--dev-login", metavar="NAME",
                        help="로컬 테스트 로그인으로 관리자 토큰을 받는다. 없으면 환경 변수 %s를 쓴다" % TOKEN_ENV)
    parser.add_argument("--dry-run", action="store_true", help="서버에 보내지 않고 보낼 내용만 출력한다")
    args = parser.parse_args()

    rows = read_rows(args.csv_path)
    ready = [row for row in rows if row.get("상태", "").strip() == READY_STATUS and row.get("이름", "").strip()]
    print("CSV %d행 중 '%s' %d행" % (len(rows), READY_STATUS, len(ready)))

    api = Api(args.base_url.rstrip("/"), token(args))
    existing = {species["name"] for species in api.get("/api/v1/admin/species")["species"]}

    created, skipped, failed = 0, 0, 0
    for row in ready:
        name = row["이름"].strip()
        if name in existing:
            print("  건너뜀  %s (이미 있음)" % name)
            skipped += 1
            continue
        try:
            body = to_request(row)
        except RowError as e:
            print("  실패    %s: %s" % (name, e))
            failed += 1
            continue
        if args.dry_run:
            print("  보낼 값 %s" % json.dumps(body, ensure_ascii=False))
            continue
        try:
            api.post("/api/v1/admin/species", body)
        except ApiError as e:
            print("  실패    %s: %s" % (name, e))
            failed += 1
            continue
        existing.add(name)
        print("  등록    %s" % name)
        created += 1

    if args.dry_run:
        print("미리 보기만 했어요. 서버에는 보내지 않았어요.")
    else:
        print("등록 %d, 건너뜀 %d, 실패 %d" % (created, skipped, failed))
        if created:
            print("노션 표에서 등록한 행의 상태를 '등록됨'으로 바꿔 주세요.")
    sys.exit(1 if failed else 0)


def read_rows(path):
    # 노션 CSV는 BOM이 붙어 있을 수 있다
    with open(path, encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        missing = [column for column in REQUIRED_COLUMNS if column not in (reader.fieldnames or [])]
        if missing:
            sys.exit("CSV에 필요한 열이 없어요: %s" % ", ".join(missing))
        return list(reader)


def to_request(row):
    body = {"name": row["이름"].strip(), "spiky": row.get("가시", "").strip().lower() in ("yes", "true")}
    for column, field in TEXT_COLUMNS.items():
        value = row.get(column, "").strip()
        if value:
            body[field] = value
    for column, (field, integer) in NUMBER_COLUMNS.items():
        value = row.get(column, "").strip()
        if value:
            body[field] = number(column, value, integer)
    aliases = [alias.strip() for alias in row.get("별칭", "").split(",") if alias.strip()]
    if aliases:
        body["aliases"] = aliases
    missing = [label for field, label in REQUIRED_FIELDS.items() if field not in body]
    if missing:
        raise RowError("필수 값이 비어 있어요: %s" % ", ".join(missing))
    return body


def number(column, value, integer):
    try:
        parsed = float(value.replace(",", ""))
    except ValueError:
        raise RowError("%s 값 '%s'이 숫자가 아니에요" % (column, value))
    if integer:
        if not parsed.is_integer():
            raise RowError("%s 값 '%s'은 정수여야 해요" % (column, value))
        return int(parsed)
    return parsed


def token(args):
    if args.dev_login:
        api = Api(args.base_url.rstrip("/"), None)
        return api.post("/api/v1/dev/login", {"name": args.dev_login, "role": "ADMIN"})["accessToken"]
    value = os.environ.get(TOKEN_ENV)
    if not value:
        sys.exit("관리자 토큰이 없어요. 환경 변수 %s를 넣거나, 로컬이면 --dev-login을 쓰세요." % TOKEN_ENV)
    return value


class ApiError(Exception):
    pass


class Api:

    def __init__(self, base_url, access_token):
        self.base_url = base_url
        self.access_token = access_token

    def get(self, path):
        return self._request("GET", path, None)

    def post(self, path, body):
        return self._request("POST", path, body)

    def _request(self, method, path, body):
        headers = {"Content-Type": "application/json"}
        if self.access_token:
            headers["Authorization"] = "Bearer " + self.access_token
        data = None if body is None else json.dumps(body).encode("utf-8")
        request = urllib.request.Request(self.base_url + path, data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=10) as response:
                return json.loads(response.read().decode("utf-8"))["data"]
        except urllib.error.HTTPError as e:
            raise ApiError(error_message(e))
        except urllib.error.URLError as e:
            sys.exit("서버에 연결하지 못했어요 (%s): %s" % (self.base_url, e.reason))


def error_message(e):
    try:
        error = json.loads(e.read().decode("utf-8"))["error"]
        return "%d %s %s" % (e.code, error["code"], error["message"])
    except (ValueError, KeyError, TypeError):
        return "%d %s" % (e.code, e.reason)


if __name__ == "__main__":
    main()
