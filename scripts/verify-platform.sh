#!/usr/bin/env bash
# 平台端到端最小验收：须先启动 backend-java（local profile）与 frontend-vite。
# 用法：API_BASE=http://127.0.0.1:8083/race-api ./scripts/verify-platform.sh
set -euo pipefail

API_BASE="${API_BASE:-http://127.0.0.1:8083/race-api}"
CURL="${CURL:-/usr/bin/curl}"
NODE="${NODE:-/opt/homebrew/bin/node}"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

json() { "$NODE" -e "$1"; }
request() { "$CURL" -fsS "$@" -H "Authorization: Bearer $TOKEN"; }
assert_success() {
  local payload="$1" label="$2"
  printf '%s' "$payload" | json 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{const x=JSON.parse(s);if(!x.success){console.error(x.message||"request failed");process.exit(1)}})'
  printf 'PASS  %s\n' "$label"
}

LOGIN="$($CURL -fsS -X POST "$API_BASE/auth/login" -H 'Content-Type: application/json' --data '{"username":"admin","password":"admin123"}')"
TOKEN="$(printf '%s' "$LOGIN" | json 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>process.stdout.write(JSON.parse(s).data.accessToken))')"
printf 'PASS  登录与 JWT\n'

STAMP="$(date +%s)"
NAME="verify-skill-$STAMP"
CREATE="$(request -X POST "$API_BASE/skills" -H 'Content-Type: application/json' --data "{\"name\":\"$NAME\",\"description\":\"自动验收临时 Skill\",\"runtime\":\"python\",\"visibility\":\"public\",\"version\":\"0.0.0\"}")"
assert_success "$CREATE" '创建 Skill（公共请求会被服务端降级为私有）'
SKILL_ID="$(printf '%s' "$CREATE" | json 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>process.stdout.write(String(JSON.parse(s).data.id)))')"

save_file() {
  local path="$1" content="$2" revision="$3"
  local body
  body="$($NODE -e 'console.log(JSON.stringify({entryFile:process.argv[1],code:process.argv[2],expectedRevision:Number(process.argv[3])}))' "$path" "$content" "$revision")"
  request -X POST "$API_BASE/skills/$SKILL_ID/code" -H 'Content-Type: application/json' --data "$body"
}

REV=0
for file in 'SKILL.md' 'requirements.txt' 'references/test-data.json' 'references/implementation-notes.md' 'scripts/validators.py' 'scripts/mock_data.py' 'scripts/main.py'; do
  case "$file" in
    SKILL.md) CONTENT=$'---\nname: verify-skill\nname_zh: 自动验收\ndescription: 平台端到端最小验收使用的临时 Skill，用于校验保存、调试、评审和定版链路。\nversion: 0.0.0\ntags: 验收,临时\nrunEnv: all\ndigestValue: pending\n---\n# 自动验收 Skill\n\n## 版本\n- 当前版本：0.0.0\n' ;;
    requirements.txt) CONTENT=$'# 标准库实现\n' ;;
    references/test-data.json) CONTENT='{"testCases":[{"id":"normal","name":"正常查询","input":{"product_code":"000001"},"expected":{"contains":{"product_code":"000001"}}}]}' ;;
    references/implementation-notes.md) CONTENT=$'# 实现说明\n' ;;
    scripts/validators.py) CONTENT=$'def validate_input(data):\n    return None if data.get("product_code") else "product_code 不能为空"\n' ;;
    scripts/mock_data.py) CONTENT=$'def build(data):\n    return {"product_code": data["product_code"]}\n' ;;
    scripts/main.py) CONTENT=$'from scripts.validators import validate_input\nfrom scripts.mock_data import build\n\ndef handle(input_data):\n    error = validate_input(input_data)\n    return {"error": error} if error else build(input_data)\n' ;;
  esac
  SAVED="$(save_file "$file" "$CONTENT" "$REV")"
  assert_success "$SAVED" "保存 $file"
  REV="$(printf '%s' "$SAVED" | json 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>process.stdout.write(String(JSON.parse(s).data.draftRevision)))')"
done

DEBUG="$(request -X POST "$API_BASE/skills/$SKILL_ID/debug" -H 'Content-Type: application/json' --data '{"inputJson":"{\"product_code\":\"000001\"}"}')"
printf '%s' "$DEBUG" | json 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{const x=JSON.parse(s);if(x.data.status!=="PASS"||!x.data.stdout.includes("000001"))process.exit(1)})'
printf 'PASS  Skill 调试成功路径\n'

INVALID="$(request -X POST "$API_BASE/skills/$SKILL_ID/debug" -H 'Content-Type: application/json' --data '{"inputJson":"not-json"}')"
printf '%s' "$INVALID" | json 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{const x=JSON.parse(s);if(x.data.status!=="FAILED"||!x.data.stderr.includes("不是合法 JSON"))process.exit(1)})'
printf 'PASS  Skill 调试失败日志\n'

REPO_PATH="$(printf '%s' "$CREATE" | json 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>process.stdout.write(JSON.parse(s).data.gitRepoPath))')"
TAG_COUNT_BEFORE="$(/opt/homebrew/bin/git --git-dir="$REPO_PATH" tag | /usr/bin/wc -l | /usr/bin/tr -d ' ')"
REVIEW="$(request -X POST "$API_BASE/skills/$SKILL_ID/reviews")"
assert_success "$REVIEW" '提交 Skill 评审（不提前定版）'
REVIEW_ID="$(printf '%s' "$REVIEW" | json 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>process.stdout.write(String(JSON.parse(s).data.id)))')"
STATUS_AFTER_SUBMIT="$(request "$API_BASE/skills/$SKILL_ID" | json 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>process.stdout.write(JSON.parse(s).data.status))')"
TAG_COUNT_AFTER_SUBMIT="$(/opt/homebrew/bin/git --git-dir="$REPO_PATH" tag | /usr/bin/wc -l | /usr/bin/tr -d ' ')"
if [[ "$STATUS_AFTER_SUBMIT" != "reviewing" || "$TAG_COUNT_BEFORE" != "$TAG_COUNT_AFTER_SUBMIT" ]]; then
  echo '提交评审错误：不应在提交时定版' >&2; exit 1
fi
APPROVE="$(request -X PUT "$API_BASE/skills/reviews/$REVIEW_ID" -H 'Content-Type: application/json' --data '{"decision":"approved","comment":"自动验收通过"}')"
assert_success "$APPROVE" '审核通过后定版与 Git Tag'
STATUS_AFTER_APPROVE="$(request "$API_BASE/skills/$SKILL_ID" | json 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>process.stdout.write(JSON.parse(s).data.status))')"
TAG_COUNT_AFTER_APPROVE="$(/opt/homebrew/bin/git --git-dir="$REPO_PATH" tag | /usr/bin/wc -l | /usr/bin/tr -d ' ')"
if [[ "$STATUS_AFTER_APPROVE" != "released" || "$TAG_COUNT_AFTER_APPROVE" -le "$TAG_COUNT_AFTER_SUBMIT" ]]; then
  echo '审核通过错误：应完成定版并创建 Git Tag' >&2; exit 1
fi

AGENT="$(request -X POST "$API_BASE/agents" -H 'Content-Type: application/json' --data "{\"name\":\"verify-agent-$STAMP\",\"description\":\"自动验收\",\"systemPrompt\":\"test\"}")"
assert_success "$AGENT" '创建智能体'
AGENT_ID="$(printf '%s' "$AGENT" | json 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>process.stdout.write(String(JSON.parse(s).data.id)))')"
MOUNT="$(request -X POST "$API_BASE/agents/$AGENT_ID/mountings" -H 'Content-Type: application/json' --data "{\"skillId\":$SKILL_ID,\"skillAlias\":\"verify\",\"enabled\":true}")"
assert_success "$MOUNT" 'Skill 挂载到智能体'

EVAL="$(request -X POST "$API_BASE/eval-tasks" -H 'Content-Type: application/json' --data "{\"name\":\"verify-eval-$STAMP\",\"agentId\":$AGENT_ID,\"datasetKey\":\"product-mock\"}")"
assert_success "$EVAL" '创建评测任务'
EVAL_ID="$(printf '%s' "$EVAL" | json 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>process.stdout.write(String(JSON.parse(s).data.id)))')"
EVAL_RUNNING="$(request -X PUT "$API_BASE/eval-tasks/$EVAL_ID/status?status=running")"
assert_success "$EVAL_RUNNING" '更新评测任务状态'

PIPELINE="$(request -X POST "$API_BASE/pipelines" -H 'Content-Type: application/json' --data "{\"name\":\"verify-pipeline-$STAMP\",\"type\":\"factory\",\"stages\":\"[{\\\"name\\\":\\\"build\\\"}]\"}")"
assert_success "$PIPELINE" '创建流水线'

printf 'PASS  批量接口验收完成\n'
