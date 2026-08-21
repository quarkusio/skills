#!/usr/bin/env bash
# Run mtool treesitter scanner and print the report path.
#
# Usage: ./scripts/scan.sh [project-dir]
#   project-dir  Path to the project to scan (default: current directory)
#
# Environment:
#   MTOOL_PLAN   Path to the plan file (default: $HOME/.mtool/plans/java-min-app.yml)

set -euo pipefail

PROJECT_DIR="${1:-.}"
MTOOL_PLAN="${MTOOL_PLAN:-$HOME/.mtool/plans/java-min-app.yml}"

if ! command -v mtool &>/dev/null; then
  echo "ERROR: mtool not found in PATH" >&2
  exit 1
fi

if [[ ! -f "$MTOOL_PLAN" ]]; then
  echo "ERROR: Plan file not found: $MTOOL_PLAN" >&2
  exit 1
fi

cd "$PROJECT_DIR"

mtool scan --plan "$MTOOL_PLAN" . --scanner treesitter -o json >/dev/null 2>&1

REPORT=$(ls -t scanning-treesitter-report_*.json 2>/dev/null | head -1)

if [[ -z "$REPORT" ]]; then
  echo "ERROR: No report file generated" >&2
  exit 1
fi

echo "$REPORT"
