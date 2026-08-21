#!/usr/bin/env bash
# Query an mtool treesitter JSON report.
#
# Usage:
#   ./scripts/query-report.sh extract   <report.json>              Extract all findings grouped by category
#   ./scripts/query-report.sh find-ann  <report.json> <annotation> Find files containing a specific annotation
#   ./scripts/query-report.sh list-ann  <report.json>              List all annotation → file pairs
#   ./scripts/query-report.sh unique    <report.json>              List unique annotations (sorted)

set -euo pipefail

COMMAND="${1:-}"
REPORT="${2:-}"

usage() {
  echo "Usage: $0 <command> <report.json> [annotation]"
  echo ""
  echo "Commands:"
  echo "  extract   Extract all findings grouped by category (classes, annotations, interfaces, imports)"
  echo "  find-ann  Find files containing a specific annotation (requires annotation argument)"
  echo "  list-ann  List all annotation → file pairs"
  echo "  unique    List unique annotations (sorted)"
  exit 1
}

if [[ -z "$COMMAND" || -z "$REPORT" ]]; then
  usage
fi

if [[ ! -f "$REPORT" ]]; then
  echo "ERROR: Report file not found: $REPORT" >&2
  exit 1
fi

if ! command -v jq &>/dev/null; then
  echo "ERROR: jq not found in PATH" >&2
  exit 1
fi

ANN_KEY="Plan to analyze java code source ONLY :: find all java.annotation"

case "$COMMAND" in
  extract)
    jq '{
      classes:      [.results[][] | select(.id? // "" | test("^java-class")) | .result],
      annotations:  [.results[][] | select(.id? // "" | test("^java-annotation")) | .result],
      interfaces:   [.results[][] | select(.id? // "" | test("^java-interface")) | .result],
      imports:      [.results[][] | select(.id? // "" | test("^java-import")) | .result]
    }' "$REPORT"
    ;;

  find-ann)
    ANNOTATION="${3:-}"
    if [[ -z "$ANNOTATION" ]]; then
      echo "ERROR: annotation argument required for find-ann" >&2
      echo "Usage: $0 find-ann <report.json> <annotation>" >&2
      exit 1
    fi
    jq -r --arg key "$ANN_KEY" --arg ann "$ANNOTATION" \
      '.results[$key][]
       | select(.result | test("text: " + $ann + "$"))
       | .result | capture("Path: (?<path>[^,]+)").path' "$REPORT"
    ;;

  list-ann)
    jq -r --arg key "$ANN_KEY" \
      '.results[$key][]
       | .result | capture("Path: (?<path>[^,]+).*text: (?<ann>.+)$")
       | "\(.ann) → \(.path)"' "$REPORT"
    ;;

  unique)
    jq -r --arg key "$ANN_KEY" \
      '[.results[$key][]
       | .result | capture("text: (?<a>.+)$").a] | unique | .[]' "$REPORT"
    ;;

  *)
    echo "ERROR: Unknown command: $COMMAND" >&2
    usage
    ;;
esac
