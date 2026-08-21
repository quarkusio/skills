#!/usr/bin/env bash
set -euo pipefail

# --- helpers ---
ok()   { printf "\033[32m✔ %s\033[0m\n" "$*"; }
warn() { printf "\033[33m⚠ %s\033[0m\n" "$*"; }
err()  { printf "\033[31m✘ %s\033[0m\n" "$*" >&2; }

require() { command -v "$1" >/dev/null 2>&1 || { err "Missing dependency: $1"; exit 1; }; }
require curl
require grep
require awk

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

# Probe SSE without hanging forever:
# 1) Validate headers (Content-Type: text/event-stream)
# 2) Read just the first two lines of the stream (typically "event:" and "data:")
probe_sse() {
  local url="$1"
  # --- headers check ---
  local ct
  ct="$(curl -sI "$url" | awk -F': ' 'tolower($1)=="content-type"{print tolower($2)}' | tr -d '\r')"
  if echo "$ct" | grep -q 'text/event-stream'; then
    ok "SSE content-type confirmed at $url"
  else
    err "Expected text/event-stream at $url, got: ${ct:-<none>}"
    return 1
  fi

  # --- read a tiny bit of body and quit ---
  # We rely on --max-time to stop curl after a short window,
  # and capture whatever came first (often the init event).
  # Exit code 28 (operation timed out) is OK as long as we saw some output.
  local out
  set +e
  out="$(curl -sN --max-time 5 "$url" | head -n 2)"
  local rc=$?
  set -e
  if [ -n "$out" ]; then
    ok "Received initial SSE chunk:"
    printf '%s\n' "$out" | sed 's/^/   /'
    return 0
  fi
  # If no output and curl timed out, it's still a failure to receive events.
  if [ $rc -eq 28 ]; then
    err "No SSE data received within 5s from $url"
  else
    err "SSE probe failed (curl rc=$rc) for $url"
  fi
  return 1
}

# --- config ---
HOST="${HOST:-localhost}"
WEB_PORT="${WEB_PORT:-8080}"
BASE="http://${HOST}:${WEB_PORT}"

echo "== Web Service smoke test =="

# 1) server up + health
wait_for_http "${BASE}/" 30
check_health "${BASE}"

# 2) HTML index returns
html="$(curl -s "${BASE}/")"
if echo "$html" | grep -qi "<html"; then
  ok "HTML returned from /"
else
  err "Expected HTML at ${BASE}/"
  exit 1
fi

# Optional: sanity check that the page has some app markup
if echo "$html" | grep -qi "coffeeshop"; then
  ok "Coffeeshop page content detected"
fi

# 3) SSE probe (WebFlux controller publishes at /api/dashboard/stream)
probe_sse "${BASE}/api/dashboard/stream"

ok "Web Service smoke test passed."
