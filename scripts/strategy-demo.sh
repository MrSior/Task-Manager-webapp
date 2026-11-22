#!/usr/bin/env bash
# Demo deployment strategies via Envoy Gateway HTTPRoute / RollingUpdate.
# Prerequisites: EG installed, gateway+backends applied, BASE_URL reachable.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NS="${NS:-application}"
BASE_URL="${BASE_URL:-http://app.test:8088}"
GATEWAY_DIR="${ROOT}/k8s/gateway"
STRAT_DIR="${GATEWAY_DIR}/strategies"
ARTIFACT_DIR="${ROOT}/artifacts/strategies"
ROUNDS="${ROUNDS:-40}"
PAUSE="${PAUSE:-3}"

mkdir -p "${ARTIFACT_DIR}"

usage() {
  cat <<EOF
Usage: $0 <rolling|bluegreen|canary|ab|shadow|probe>
Env: BASE_URL=${BASE_URL} NS=${NS} ROUNDS=${ROUNDS}
EOF
}

ts() { date -u +"%Y-%m-%dT%H:%M:%SZ"; }

log() { echo "[$(ts)] $*"; }

probe_versions() {
  local label="$1"
  local extra_header="${2:-}"
  local out="${ARTIFACT_DIR}/${label}-versions.txt"
  local tmp
  tmp="$(mktemp)"
  log "Probing ${ROUNDS} requests → ${BASE_URL}/api/auth/login (${label})"
  local i
  for i in $(seq 1 "${ROUNDS}"); do
    if [[ -n "${extra_header}" ]]; then
      curl -sS -D - -o /dev/null \
        -H "Content-Type: application/json" \
        -H "${extra_header}" \
        -d '{"login":"strategy-probe","password":"x"}' \
        "${BASE_URL}/api/auth/login" 2>/dev/null \
        | awk 'tolower($0) ~ /^x-app-version:/{print $2}' | tr -d '\r' \
        || echo "ERROR"
    else
      curl -sS -D - -o /dev/null \
        -H "Content-Type: application/json" \
        -d '{"login":"strategy-probe","password":"x"}' \
        "${BASE_URL}/api/auth/login" 2>/dev/null \
        | awk 'tolower($0) ~ /^x-app-version:/{print $2}' | tr -d '\r' \
        || echo "ERROR"
    fi
  done | tee "${tmp}"
  {
    echo "# ${label} @ $(ts) BASE_URL=${BASE_URL}"
    echo "# counts:"
    sort "${tmp}" | uniq -c | sort -nr
  } | tee "${out}"
  rm -f "${tmp}"
}

apply_route() {
  kubectl apply -f "$1"
  sleep "${PAUSE}"
}

ensure_versioned_backends() {
  kubectl apply -f "${GATEWAY_DIR}/backend-v1.yaml"
  kubectl apply -f "${GATEWAY_DIR}/backend-v2.yaml"
  kubectl -n "${NS}" scale deployment/task-manager-backend-v1 --replicas=2
  kubectl -n "${NS}" scale deployment/task-manager-backend-v2 --replicas=2
  kubectl -n "${NS}" rollout status deployment/task-manager-backend-v1 --timeout=240s
  kubectl -n "${NS}" rollout status deployment/task-manager-backend-v2 --timeout=240s
}

capture_pods() {
  local label="$1"
  kubectl -n "${NS}" get pods -l 'app in (task-manager-backend,task-manager-backend-v1,task-manager-backend-v2)' -o wide \
    | tee "${ARTIFACT_DIR}/${label}-pods.txt"
}

capture_logs_snippet() {
  local label="$1"
  local deploy="$2"
  kubectl -n "${NS}" logs "deploy/${deploy}" --tail=30 2>/dev/null \
    | tee "${ARTIFACT_DIR}/${label}-logs-${deploy}.txt" || true
}

demo_rolling() {
  local logf="${ARTIFACT_DIR}/rolling.log"
  {
    log "=== Rolling: scale down versioned backends; primary on v1 ==="
    kubectl -n "${NS}" scale deployment/task-manager-backend-v1 --replicas=0 || true
    kubectl -n "${NS}" scale deployment/task-manager-backend-v2 --replicas=0 || true
    kubectl apply -f "${GATEWAY_DIR}/httproute.yaml"
    kubectl apply -f "${ROOT}/k8s/deployments/backend-deploymet.yaml"
    kubectl -n "${NS}" scale deployment/task-manager-backend --replicas=2
    kubectl -n "${NS}" set image deployment/task-manager-backend backend=task-manager-backend:v1
    kubectl -n "${NS}" set env deployment/task-manager-backend APP_VERSION=v1
    kubectl -n "${NS}" rollout status deployment/task-manager-backend --timeout=240s
    capture_pods rolling-before
    probe_versions rolling-before

    log "=== Rolling: set image v1 → v2 (watch pods) ==="
    kubectl -n "${NS}" set image deployment/task-manager-backend backend=task-manager-backend:v2
    kubectl -n "${NS}" set env deployment/task-manager-backend APP_VERSION=v2
    # sample during rollout
    (
      for _ in $(seq 1 15); do
        curl -sS -D - -o /dev/null \
          -H "Content-Type: application/json" \
          -d '{"login":"strategy-probe","password":"x"}' \
          "${BASE_URL}/api/auth/login" 2>/dev/null \
          | awk 'tolower($0) ~ /^x-app-version:/{print $2}' | tr -d '\r' || true
        sleep 1
      done
    ) | tee "${ARTIFACT_DIR}/rolling-during.txt"
    kubectl -n "${NS}" rollout status deployment/task-manager-backend --timeout=240s
    capture_pods rolling-after
    probe_versions rolling-after
    kubectl -n "${NS}" rollout history deployment/task-manager-backend | tee "${ARTIFACT_DIR}/rolling-history.txt"
    log "Rolling done"
  } 2>&1 | tee "${logf}"
}

demo_bluegreen() {
  local logf="${ARTIFACT_DIR}/bluegreen.log"
  {
    kubectl -n "${NS}" scale deployment/task-manager-backend --replicas=0 || true
    ensure_versioned_backends
    log "=== Blue/Green: 100% blue (v1) ==="
    apply_route "${STRAT_DIR}/blue-green-blue.yaml"
    probe_versions bluegreen-blue
    capture_pods bluegreen-blue

    log "=== Blue/Green: instant switch → green (v2) ==="
    apply_route "${STRAT_DIR}/blue-green-green.yaml"
    probe_versions bluegreen-green

    log "=== Blue/Green: rollback → blue (v1) ==="
    apply_route "${STRAT_DIR}/blue-green-blue.yaml"
    probe_versions bluegreen-rollback
    log "Blue/Green done"
  } 2>&1 | tee "${logf}"
}

demo_canary() {
  local logf="${ARTIFACT_DIR}/canary.log"
  {
    kubectl -n "${NS}" scale deployment/task-manager-backend --replicas=0 || true
    ensure_versioned_backends
    log "=== Canary 90/10 ==="
    apply_route "${STRAT_DIR}/canary-90-10.yaml"
    probe_versions canary-90-10

    log "=== Canary 50/50 ==="
    apply_route "${STRAT_DIR}/canary-50-50.yaml"
    probe_versions canary-50-50

    log "=== Canary 0/100 ==="
    apply_route "${STRAT_DIR}/canary-0-100.yaml"
    probe_versions canary-0-100
    log "Canary done"
  } 2>&1 | tee "${logf}"
}

demo_ab() {
  local logf="${ARTIFACT_DIR}/ab.log"
  {
    kubectl -n "${NS}" scale deployment/task-manager-backend --replicas=0 || true
    ensure_versioned_backends
    apply_route "${STRAT_DIR}/ab-header.yaml"
    log "=== A/B: default audience → expect v1 ==="
    probe_versions ab-default
    log "=== A/B: X-Audience: beta → expect v2 ==="
    probe_versions ab-beta "X-Audience: beta"
    log "A/B done"
  } 2>&1 | tee "${logf}"
}

demo_shadow() {
  local logf="${ARTIFACT_DIR}/shadow.log"
  {
    kubectl -n "${NS}" scale deployment/task-manager-backend --replicas=0 || true
    ensure_versioned_backends
    apply_route "${STRAT_DIR}/shadow-mirror.yaml"
    log "=== Shadow: client should see only v1; both backends get traffic ==="
    # clear recent noise by noting start time
    local start
    start="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
    probe_versions shadow-client
    sleep 2
    capture_logs_snippet shadow task-manager-backend-v1
    capture_logs_snippet shadow task-manager-backend-v2
    {
      echo "# shadow started ${start}"
      echo "# client versions:"
      cat "${ARTIFACT_DIR}/shadow-client-versions.txt"
      echo "# v1 log lines mentioning /api:"
      grep -E '"http.path"|/api/' "${ARTIFACT_DIR}/shadow-logs-task-manager-backend-v1.txt" | tail -20 || true
      echo "# v2 log lines mentioning /api (mirrored):"
      grep -E '"http.path"|/api/' "${ARTIFACT_DIR}/shadow-logs-task-manager-backend-v2.txt" | tail -20 || true
    } | tee "${ARTIFACT_DIR}/shadow-summary.txt"
    log "Shadow done — client X-App-Version must be v1; v2 logs should show mirrored requests"
  } 2>&1 | tee "${logf}"
}

cmd="${1:-}"
case "${cmd}" in
  rolling) demo_rolling ;;
  bluegreen) demo_bluegreen ;;
  canary) demo_canary ;;
  ab) demo_ab ;;
  shadow) demo_shadow ;;
  probe) probe_versions probe-manual "${2:-}" ;;
  *) usage; exit 1 ;;
esac
