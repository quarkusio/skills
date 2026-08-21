#!/usr/bin/env bash
# Analyze an mtool treesitter report and produce a compact migration plan.
# Cross-references found annotations AND imports with lookup files to output
# only actionable items, grouped by action.
#
# Usage: ./scripts/analyze.sh <report.json>
#
# Output:
#   1. Annotation analysis — what to replace/remove/review/keep
#   2. Import analysis — Spring imports that signal features needing migration
#   3. Files to read — deduplicated list of files the agent should deep-read

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ANN_LOOKUP="$SCRIPT_DIR/annotation-lookup.tsv"
IMP_LOOKUP="$SCRIPT_DIR/import-lookup.tsv"
REPORT="${1:-}"

if [[ -z "$REPORT" ]]; then
  echo "Usage: $0 <report.json>" >&2
  exit 1
fi

if [[ ! -f "$REPORT" ]]; then
  echo "ERROR: Report not found: $REPORT" >&2
  exit 1
fi

if ! command -v jq &>/dev/null; then
  echo "ERROR: jq not found in PATH" >&2
  exit 1
fi

ANN_KEY="Plan to analyze java code source ONLY :: find all java.annotation"
IMP_KEY="Plan to analyze java code source ONLY :: find all java.import"

# ── Annotation analysis ──────────────────────────────────────────────

declare -A ANN_FILES
while IFS=$'\t' read -r ann path; do
  if [[ -n "${ANN_FILES[$ann]+x}" ]]; then
    ANN_FILES[$ann]="${ANN_FILES[$ann]}, $path"
  else
    ANN_FILES[$ann]="$path"
  fi
done < <(jq -r --arg key "$ANN_KEY" \
  '.results[$key][]
   | .result | capture("Path: (?<path>[^,]+).*text: (?<ann>.+)$")
   | "\(.ann)\t\(.path)"' "$REPORT" | sort -t$'\t' -k1,1 -u)

for ann in "${!ANN_FILES[@]}"; do
  ANN_FILES[$ann]=$(echo "${ANN_FILES[$ann]}" | tr ',' '\n' | sed 's/^ //' | sort -u | paste -sd', ' -)
done

declare -A ANN_LOOKUP_ACTION
declare -A ANN_LOOKUP_REPLACEMENT
declare -A ANN_LOOKUP_CATEGORY
while IFS=$'\t' read -r spring action replacement category; do
  [[ "$spring" == \#* || -z "$spring" ]] && continue
  ANN_LOOKUP_ACTION[$spring]="$action"
  ANN_LOOKUP_REPLACEMENT[$spring]="$replacement"
  ANN_LOOKUP_CATEGORY[$spring]="$category"
done < "$ANN_LOOKUP"

declare -a ANN_REPLACE=()
declare -a ANN_REMOVE=()
declare -a ANN_REVIEW=()
declare -a ANN_KEEP=()
declare -a ANN_UNKNOWN=()

for ann in $(echo "${!ANN_FILES[@]}" | tr ' ' '\n' | sort); do
  files="${ANN_FILES[$ann]}"
  action="${ANN_LOOKUP_ACTION[$ann]:-unknown}"
  replacement="${ANN_LOOKUP_REPLACEMENT[$ann]:-?}"
  category="${ANN_LOOKUP_CATEGORY[$ann]:-?}"

  line="  @${ann} → ${replacement}  [${category}]"
  file_line="    files: ${files}"

  case "$action" in
    replace)  ANN_REPLACE+=("$line" "$file_line") ;;
    remove)   ANN_REMOVE+=("$line" "$file_line") ;;
    review)   ANN_REVIEW+=("$line" "$file_line") ;;
    keep)     ANN_KEEP+=("$line") ;;
    *)        ANN_UNKNOWN+=("  @${ann}  (not in lookup)" "$file_line") ;;
  esac
done

# ── Import analysis ──────────────────────────────────────────────────

declare -A IMP_LOOKUP_ACTION
declare -A IMP_LOOKUP_NOTE
declare -A IMP_LOOKUP_CATEGORY
while IFS=$'\t' read -r spring action note category; do
  [[ "$spring" == \#* || -z "$spring" ]] && continue
  IMP_LOOKUP_ACTION[$spring]="$action"
  IMP_LOOKUP_NOTE[$spring]="$note"
  IMP_LOOKUP_CATEGORY[$spring]="$category"
done < "$IMP_LOOKUP"

declare -A IMP_FILES
while IFS=$'\t' read -r imp path; do
  [[ "$imp" != org.springframework* ]] && continue
  if [[ -n "${IMP_FILES[$imp]+x}" ]]; then
    IMP_FILES[$imp]="${IMP_FILES[$imp]}, $path"
  else
    IMP_FILES[$imp]="$path"
  fi
done < <(jq -r --arg key "$IMP_KEY" \
  '.results[$key][]
   | .result | capture("Path: (?<path>[^,]+).*text: (?<imp>.+)$")
   | "\(.imp)\t\(.path)"' "$REPORT" | sort -t$'\t' -k1,1 -u)

for imp in "${!IMP_FILES[@]}"; do
  IMP_FILES[$imp]=$(echo "${IMP_FILES[$imp]}" | tr ',' '\n' | sed 's/^ //' | sort -u | paste -sd', ' -)
done

declare -a IMP_REPLACE=()
declare -a IMP_REMOVE=()
declare -a IMP_REVIEW=()
declare -a IMP_KEEP=()
declare -a IMP_UNKNOWN=()

for imp in $(echo "${!IMP_FILES[@]}" | tr ' ' '\n' | sort); do
  files="${IMP_FILES[$imp]}"
  action="${IMP_LOOKUP_ACTION[$imp]:-unknown}"
  note="${IMP_LOOKUP_NOTE[$imp]:-?}"
  category="${IMP_LOOKUP_CATEGORY[$imp]:-?}"

  short="${imp#org.springframework.}"
  line="  ${short} → ${note}  [${category}]"
  file_line="    files: ${files}"

  case "$action" in
    replace)  IMP_REPLACE+=("$line" "$file_line") ;;
    remove)   IMP_REMOVE+=("$line" "$file_line") ;;
    review)   IMP_REVIEW+=("$line" "$file_line") ;;
    keep)     IMP_KEEP+=("$line") ;;
    *)        IMP_UNKNOWN+=("  ${short}  (not in lookup)" "$file_line") ;;
  esac
done

# ── Output ───────────────────────────────────────────────────────────

echo "=== Annotation Analysis ==="
echo ""

if [[ ${#ANN_REPLACE[@]} -gt 0 ]]; then
  echo "REPLACE (change annotation):"
  printf '%s\n' "${ANN_REPLACE[@]}"
  echo ""
fi

if [[ ${#ANN_REMOVE[@]} -gt 0 ]]; then
  echo "REMOVE (delete annotation, no replacement needed):"
  printf '%s\n' "${ANN_REMOVE[@]}"
  echo ""
fi

if [[ ${#ANN_REVIEW[@]} -gt 0 ]]; then
  echo "REVIEW (manual migration needed):"
  printf '%s\n' "${ANN_REVIEW[@]}"
  echo ""
fi

if [[ ${#ANN_KEEP[@]} -gt 0 ]]; then
  echo "KEEP (no change needed):"
  printf '%s\n' "${ANN_KEEP[@]}"
  echo ""
fi

if [[ ${#ANN_UNKNOWN[@]} -gt 0 ]]; then
  echo "UNKNOWN (not in annotation-lookup.tsv):"
  printf '%s\n' "${ANN_UNKNOWN[@]}"
  echo ""
fi

ann_total=${#ANN_FILES[@]}
ann_replace=${#ANN_REPLACE[@]}; ann_replace=$((ann_replace / 2))
ann_remove=${#ANN_REMOVE[@]}; ann_remove=$((ann_remove / 2))
ann_review=${#ANN_REVIEW[@]}; ann_review=$((ann_review / 2))
ann_keep=${#ANN_KEEP[@]}
ann_unknown=${#ANN_UNKNOWN[@]}; ann_unknown=$((ann_unknown / 2))

echo "--- Annotations: ${ann_total} found: ${ann_replace} replace, ${ann_remove} remove, ${ann_review} review, ${ann_keep} keep, ${ann_unknown} unknown ---"
echo ""

echo "=== Import Analysis (org.springframework.*) ==="
echo ""

if [[ ${#IMP_REPLACE[@]} -gt 0 ]]; then
  echo "REPLACE (change import):"
  printf '%s\n' "${IMP_REPLACE[@]}"
  echo ""
fi

if [[ ${#IMP_REMOVE[@]} -gt 0 ]]; then
  echo "REMOVE (delete import):"
  printf '%s\n' "${IMP_REMOVE[@]}"
  echo ""
fi

if [[ ${#IMP_REVIEW[@]} -gt 0 ]]; then
  echo "REVIEW (needs manual migration):"
  printf '%s\n' "${IMP_REVIEW[@]}"
  echo ""
fi

if [[ ${#IMP_KEEP[@]} -gt 0 ]]; then
  echo "KEEP (supported by compat extension):"
  printf '%s\n' "${IMP_KEEP[@]}"
  echo ""
fi

if [[ ${#IMP_UNKNOWN[@]} -gt 0 ]]; then
  echo "UNKNOWN (not in import-lookup.tsv):"
  printf '%s\n' "${IMP_UNKNOWN[@]}"
  echo ""
fi

imp_total=${#IMP_FILES[@]}
imp_replace=${#IMP_REPLACE[@]}; imp_replace=$((imp_replace / 2))
imp_remove=${#IMP_REMOVE[@]}; imp_remove=$((imp_remove / 2))
imp_review=${#IMP_REVIEW[@]}; imp_review=$((imp_review / 2))
imp_keep=${#IMP_KEEP[@]}
imp_unknown=${#IMP_UNKNOWN[@]}; imp_unknown=$((imp_unknown / 2))

echo "--- Imports: ${imp_total} Spring imports found: ${imp_replace} replace, ${imp_remove} remove, ${imp_review} review, ${imp_keep} keep, ${imp_unknown} unknown ---"
