#!/usr/bin/env bash
set -euo pipefail

# Colors
ok()   { printf "\033[32m✔ %s\033[0m\n" "$*"; }
warn() { printf "\033[33m⚠ %s\033[0m\n" "$*"; }
err()  { printf "\033[31m✘ %s\033[0m\n" "$*" >&2; }

require() { command -v "$1" >/dev/null 2>&1 || { err "Missing dependency: $1"; exit 1; }; }
require curl

# Helper: wait_for_http <url> <timeout_seconds>
wait_for_http() {
  local url="$1"; shift
  local timeout="${1:-30}"
  local start ts code
  start=$(date +%s)
  while true; do
    code=$(curl -s -o /dev/null -w "%{http_code}" "$url" || true)
    # Consider any response code (even 404/405) as "up"
    if [ -n "$code" ]; then
      ok "HTTP reachable (${code}) at $url"
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


# Helper: try multiple health endpoints (Spring Actuator / Quarkus)
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

# Defaults (override via env): COUNTER_PORT=8081
HOST="${HOST:-localhost}"
COUNTER_PORT="${COUNTER_PORT:-8081}"
BASE="http://${HOST}:${COUNTER_PORT}"

echo "== Counter Service smoke test =="

# Optional: wait for the service root to answer HTTP before continuing
wait_for_http "${BASE}" 30 || true
check_health "${BASE}" || true

ORDER_ID="${ORDER_ID:-order-$(date +%s)}"
NOW_ISO="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

# FIX: use a here-doc instead of `read -d ''` which expects a NUL.
# CHANGED: field name storeId -> location (your Spring API expects 'location')
PAYLOAD="$(cat <<JSON
{
  "id": "${ORDER_ID}",
  "orderSource": "WEB",
  "location": "ATLANTA",
  "timestamp": "${NOW_ISO}"
}
JSON
)"

echo "Posting order ${ORDER_ID} to ${BASE}/api/order"
status=$(curl -s -o /dev/null -w "%{http_code}" -H "Content-Type: application/json" -d "$PAYLOAD" "${BASE}/api/order")
if [ "$status" -eq 202 ] || [ "$status" -eq 200 ]; then
  ok "Order accepted (${status})"
else
  err "Unexpected status ${status} from /api/order"
  exit 1
fi

ok "Counter Service smoke test passed."
