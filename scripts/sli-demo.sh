#!/usr/bin/env bash
# Generate traffic for SLI/SLO demo (success + failed logins + authenticated API calls).
# Prerequisites: app reachable at BASE_URL (default http://app.test:8090).
set -euo pipefail

BASE_URL="${BASE_URL:-http://app.test:8090}"
LOGIN="${LOGIN:-sliuser}"
PASSWORD="${PASSWORD:-sli-pass}"
MODE="${1:-mixed}" # mixed | success | fail | tasks
ROUNDS="${ROUNDS:-30}"

echo "BASE_URL=$BASE_URL MODE=$MODE ROUNDS=$ROUNDS"

register() {
  curl -sS -o /dev/null -w "register:%{http_code}\n" \
    -H "Content-Type: application/json" \
    -d "{\"login\":\"${LOGIN}\",\"password\":\"${PASSWORD}\"}" \
    "${BASE_URL}/api/auth/register" || true
}

login_ok() {
  curl -sS -H "Content-Type: application/json" \
    -d "{\"login\":\"${LOGIN}\",\"password\":\"${PASSWORD}\"}" \
    "${BASE_URL}/api/auth/login"
}

obtain_token() {
  local token=""
  local attempt
  for attempt in $(seq 1 20); do
    token="$(login_ok | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"
    if [[ -n "${token}" ]]; then
      echo "${token}"
      return 0
    fi
    sleep 0.3
  done
  return 1
}

login_fail() {
  curl -sS -o /dev/null -w "fail_login:%{http_code}\n" \
    -H "Content-Type: application/json" \
    -d "{\"login\":\"${LOGIN}\",\"password\":\"wrong-password\"}" \
    "${BASE_URL}/api/auth/login" || true
}

create_task() {
  local token="$1"
  curl -sS -o /dev/null -w "task:%{http_code}\n" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${token}" \
    -d '{"title":"sli-demo","description":"load"}' \
    "${BASE_URL}/api/tasks" || true
}

list_tasks() {
  local token="$1"
  curl -sS -o /dev/null -w "list:%{http_code}\n" \
    -H "Authorization: Bearer ${token}" \
    "${BASE_URL}/api/tasks" || true
}

register

case "$MODE" in
  success)
    for ((i = 1; i <= ROUNDS; i++)); do
      login_ok >/dev/null
      echo "ok_login $i"
      sleep 0.2
    done
    ;;
  fail)
    for ((i = 1; i <= ROUNDS; i++)); do
      login_fail
      sleep 0.2
    done
    ;;
  tasks)
    if ! TOKEN="$(obtain_token)"; then
      echo "failed to obtain token (is APP_DEMO_FAIL_RATE too high? try set env to 0)" >&2
      exit 1
    fi
    echo "got token"
    for ((i = 1; i <= ROUNDS; i++)); do
      list_tasks "$TOKEN"
      create_task "$TOKEN"
      sleep 0.2
    done
    ;;
  mixed|*)
    TOKEN="$(obtain_token || true)"
    for ((i = 1; i <= ROUNDS; i++)); do
      if (( i % 3 == 0 )); then
        login_fail
      else
        login_ok >/dev/null || true
        echo "ok_login $i"
      fi
      if [[ -n "${TOKEN}" ]]; then
        list_tasks "$TOKEN"
      fi
      sleep 0.2
    done
    ;;
esac

echo "done"
