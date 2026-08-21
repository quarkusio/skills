#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:9080/daytrader}"

echo "Health check: ${BASE_URL}/"
HTTP_STATUS=$(curl -sL -o /dev/null -w "%{http_code}" "${BASE_URL}/")

if [ "$HTTP_STATUS" -eq 200 ]; then
  echo "PASS - got HTTP ${HTTP_STATUS}"
  exit 0
else
  echo "FAIL - got HTTP ${HTTP_STATUS}"
  exit 1
fi
