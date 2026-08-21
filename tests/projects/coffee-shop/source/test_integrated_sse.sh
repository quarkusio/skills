#!/usr/bin/env bash
set -euo pipefail

ok()   { printf "\033[32m✔ %s\033[0m\n" "$*"; }
warn() { printf "\033[33m⚠ %s\033[0m\n" "$*"; }
err()  { printf "\033[31m✘ %s\033[0m\n" "$*" >&2; }

require() { command -v "$1" >/dev/null 2>&1 || { err "Missing dependency: $1"; exit 1; }; }
require curl
require grep

wait_for_http() {
  local url="$1"; shift
  local timeout="${1:-30}"
  local start ts
  start=$(date +%s)
  while true; do
    if curl -sf -o /dev/null "$url"; then
      ok "Service is up: $url"
      return 0
    fi
    ts=$(date +%s)
    if [ $((ts-start)) -ge "$timeout" ]; then
      err "Timeout waiting for $url"
      return 1
    fi
    sleep 1
  done
}

check_health() {
  local base="$1"
  local ok_any=0
  for path in /actuator/health /q/health /health; do
    if curl -sf "${base}${path}" >/dev/null 2>&1; then
      ok "Health endpoint alive at ${base}${path}"
      ok_any=1
      break
    fi
  done
  if [ "$ok_any" -eq 0 ]; then
    warn "No standard health endpoint responded at ${base}{/actuator/health,/q/health,/health}. Continuing..."
  fi
}

HOST="${HOST:-localhost}"
WEB_PORT="${WEB_PORT:-8080}"
COUNTER_PORT="${COUNTER_PORT:-8081}"
WEB_BASE="http://${HOST}:${WEB_PORT}"
COUNTER_BASE="http://${HOST}:${COUNTER_PORT}"

echo "== Integrated SSE test =="

# Ensure both services respond
wait_for_http "${WEB_BASE}/" 30
check_health "${WEB_BASE}" || true
check_health "${COUNTER_BASE}" || true

ORDER_ID="${ORDER_ID:-order-$(date +%s)}"
NOW_ISO="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

# Start SSE capture in background (no GNU timeout; use curl --max-time)
TMP_SSE="$(mktemp)"
ok "Connecting to SSE at ${WEB_BASE}/api/dashboard/stream (capturing up to 20s)"
( curl -sN --max-time 20 "${WEB_BASE}/api/dashboard/stream" > "$TMP_SSE" ) &
SSE_PID=$!
sleep 1  # give it a moment to connect

# Send an order to trigger updates through the pipeline
read -r -d '' PAYLOAD <<JSON
{
  "id": "${ORDER_ID}",
  "orderSource": "WEB",
  "location": "ATLANTA",
  "timestamp": "${NOW_ISO}"
}
JSON

echo "Posting order ${ORDER_ID} to ${COUNTER_BASE}/api/order"
status=$(curl -s -o /dev/null -w "%{http_code}" -H "Content-Type: application/json" -d "$PAYLOAD" "${COUNTER_BASE}/api/order")
if [ "$status" -eq 202 ] || [ "$status" -eq 200 ]; then
  ok "Order accepted (${status})"
else
  err "Unexpected status ${status} from counter /api/order"
  kill $SSE_PID 2>/dev/null || true
  exit 1
fi

# Poll the captured SSE for our ORDER_ID while curl runs (max ~20s)
echo "Waiting for SSE containing ${ORDER_ID} ..."
found=0
for i in $(seq 1 20); do
  if grep -q "${ORDER_ID}" "$TMP_SSE"; then
    found=1
    break
  fi
  sleep 1
done

# Ensure background curl is done
wait $SSE_PID 2>/dev/null || true

if [ "$found" -eq 1 ]; then
  ok "Integrated SSE test passed (saw ${ORDER_ID} in stream)"
  exit 0
else
  warn "Did not see ${ORDER_ID} in SSE within timeout. Contents captured at: $TMP_SSE"
  echo "---- SSE captured (first 120 lines) ----"
  sed -n '1,120p' "$TMP_SSE" || true
  echo "----------------------------------------"
  exit 2
fi
