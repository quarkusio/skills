#!/usr/bin/env bash
#
# report.sh — Generate an HTML dashboard from test run history.
#
# Usage:
#   ./report.sh                          # reads target/runs/, writes target/runs/report.html
#   ./report.sh /path/to/runs/           # custom runs directory
#   ./report.sh target/runs/ report.html # custom output path
#
# The report is a single self-contained HTML file with no external dependencies.
# Re-run after each test to update. Opens in any browser.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUNS_DIR="${1:-$SCRIPT_DIR/target/runs}"
OUTPUT="${2:-$RUNS_DIR/report.html}"
HISTORY="$RUNS_DIR/history.jsonl"

if [ ! -f "$HISTORY" ]; then
    echo "No history found at $HISTORY"
    echo "Run tests first: cd tests && mvn test"
    exit 1
fi

python3 - "$HISTORY" "$RUNS_DIR" "$OUTPUT" << 'PYTHON_SCRIPT'
import json, sys, os, html, re
from datetime import datetime
from collections import defaultdict
from pathlib import Path

history_file = sys.argv[1]
runs_dir = sys.argv[2]
output_file = sys.argv[3]

# Load all runs
runs = []
with open(history_file) as f:
    for line in f:
        line = line.strip()
        if not line:
            continue
        try:
            runs.append(json.loads(line))
        except json.JSONDecodeError:
            pass

if not runs:
    print("No runs found in history.")
    sys.exit(1)

# Group by (project, model, strategy) for trend tracking
groups = defaultdict(list)
for r in runs:
    key = (r.get("project", "?"), r.get("model", "?"), r.get("strategy", "?"))
    groups[key].append(r)

def get_run_name(run):
    """Get the run name from the JSON, or reconstruct it as fallback."""
    name = run.get("run_name", "")
    if name:
        return name
    # Fallback: reconstruct from fields (for old history entries)
    project = run.get("project", "unknown")
    model = run.get("model", "default")
    strategy = run.get("strategy", "full")
    import re
    model_clean = re.sub(r'[^a-zA-Z0-9-]', '-', model)
    return f"{project}_{model_clean}_{strategy}"

def load_file(run, suffix):
    """Try to load a run artifact file."""
    name = get_run_name(run)
    path = Path(runs_dir) / f"{name}{suffix}"
    if path.exists():
        return path.read_text()
    return ""

def load_review(run):
    """Load the review.md for a run, falling back to summary in JSON."""
    text = load_file(run, ".review.md")
    if text:
        return text
    return run.get("review", {}).get("summary", "")

def load_pretty_log(run):
    """Load the pretty.md for a run."""
    return load_file(run, ".pretty.md")

# Build HTML
def check_icon(passed):
    return "✅" if passed else "❌"

def format_cost(cost):
    return f"${cost:.4f}" if cost else "$0.00"

def format_tokens(tokens):
    if tokens >= 1_000_000:
        return f"{tokens/1_000_000:.1f}M"
    if tokens >= 1_000:
        return f"{tokens/1_000:.0f}K"
    return str(tokens)

def format_date(date_str):
    try:
        dt = datetime.fromisoformat(date_str.replace("Z", "+00:00"))
        return dt.strftime("%Y-%m-%d %H:%M")
    except:
        return date_str[:16] if date_str else "?"

def score_color(score_str):
    try:
        passed, total = map(int, score_str.split("/"))
        ratio = passed / total if total > 0 else 0
        if ratio >= 1.0: return "#22c55e"  # green
        if ratio >= 0.6: return "#eab308"  # yellow
        return "#ef4444"  # red
    except:
        return "#6b7280"

# Unique run id for linking
def run_id(run, idx):
    return f"run-{idx}"

html_parts = []

html_parts.append("""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Quarkus Migration Skills — Test Report</title>
<style>
  :root {
    --bg: #0f172a; --surface: #1e293b; --surface2: #334155;
    --text: #e2e8f0; --text2: #94a3b8; --accent: #38bdf8;
    --green: #22c55e; --red: #ef4444; --yellow: #eab308;
    --border: #475569;
  }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, monospace;
    background: var(--bg); color: var(--text);
    padding: 2rem; max-width: 1400px; margin: 0 auto;
  }
  h1 { color: var(--accent); margin-bottom: 0.5rem; font-size: 1.5rem; }
  h2 { color: var(--accent); margin: 2rem 0 1rem; font-size: 1.2rem; border-bottom: 1px solid var(--border); padding-bottom: 0.5rem; }
  h3 { color: var(--text); margin: 1.5rem 0 0.5rem; font-size: 1rem; }
  .subtitle { color: var(--text2); margin-bottom: 2rem; }
  .stats-grid {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 1rem; margin-bottom: 2rem;
  }
  .stat-card {
    background: var(--surface); border-radius: 8px; padding: 1rem;
    border: 1px solid var(--border);
  }
  .stat-card .label { color: var(--text2); font-size: 0.75rem; text-transform: uppercase; }
  .stat-card .value { font-size: 1.5rem; font-weight: bold; margin-top: 0.25rem; }
  table { width: 100%; border-collapse: collapse; margin-bottom: 1.5rem; }
  th, td {
    padding: 0.5rem 0.75rem; text-align: left;
    border-bottom: 1px solid var(--border); font-size: 0.85rem;
  }
  th { background: var(--surface); color: var(--text2); font-weight: 600; position: sticky; top: 0; }
  tr:hover { background: var(--surface); }
  .score-badge {
    display: inline-block; padding: 0.15rem 0.5rem; border-radius: 4px;
    font-weight: bold; font-size: 0.85rem; color: #000;
  }
  .trend { display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; }
  .trend .score-badge { font-size: 0.75rem; }
  .trend .arrow { color: var(--text2); }
  .checks { display: flex; gap: 0.25rem; flex-wrap: wrap; }
  .check { font-size: 0.8rem; }
  .details-toggle {
    background: var(--surface2); border: 1px solid var(--border); color: var(--accent);
    padding: 0.25rem 0.75rem; border-radius: 4px; cursor: pointer; font-size: 0.8rem;
  }
  .details-toggle:hover { background: var(--border); }
  .details-panel {
    display: none; background: var(--surface); border: 1px solid var(--border);
    border-radius: 8px; padding: 1rem; margin: 0.5rem 0 1rem;
    white-space: pre-wrap; font-family: monospace; font-size: 0.8rem;
    max-height: 600px; overflow-y: auto; line-height: 1.5;
  }
  .details-panel.open { display: block !important; }
  .tab-bar { display: flex; gap: 0; border-bottom: 2px solid var(--border); margin-bottom: 1rem; }
  .tab {
    padding: 0.5rem 1rem; cursor: pointer; color: var(--text2);
    border-bottom: 2px solid transparent; margin-bottom: -2px; font-size: 0.85rem;
    user-select: none;
  }
  .tab:hover { color: var(--text); }
  .tab.active { color: var(--accent); border-bottom-color: var(--accent); }
  .chart-bar-container { display: flex; align-items: center; gap: 0.5rem; margin: 0.25rem 0; }
  .chart-bar {
    height: 20px; border-radius: 3px; min-width: 2px;
    transition: width 0.3s ease;
  }
  .chart-label { font-size: 0.75rem; color: var(--text2); min-width: 120px; }
  .chart-value { font-size: 0.75rem; color: var(--text); min-width: 60px; text-align: right; }
  footer { margin-top: 3rem; color: var(--text2); font-size: 0.75rem; text-align: center; }
</style>
</head>
<body>
<h1>🔄 Quarkus Migration Skills — Test Report</h1>
""")

html_parts.append(f'<p class="subtitle">Generated {datetime.now().strftime("%Y-%m-%d %H:%M")} · {len(runs)} run(s) · {len(groups)} configuration(s)</p>')

# === Summary stats ===
total_tokens = sum(r.get("usage", {}).get("total_tokens", 0) for r in runs)
total_cost = sum(r.get("usage", {}).get("total_cost", 0) for r in runs)
total_review_cost = sum(r.get("review", {}).get("cost", 0) for r in runs)
total_duration = sum(r.get("duration_seconds", 0) for r in runs)
all_scores = [r.get("score", "0/0") for r in runs]
perfect_runs = sum(1 for s in all_scores if s.split("/")[0] == s.split("/")[1])

html_parts.append(f"""
<div class="stats-grid">
  <div class="stat-card"><div class="label">Total Runs</div><div class="value">{len(runs)}</div></div>
  <div class="stat-card"><div class="label">Perfect Scores</div><div class="value" style="color:var(--green)">{perfect_runs}/{len(runs)}</div></div>
  <div class="stat-card"><div class="label">Total Tokens</div><div class="value">{format_tokens(total_tokens)}</div></div>
  <div class="stat-card"><div class="label">Migration Cost</div><div class="value">{format_cost(total_cost)}</div></div>
  <div class="stat-card"><div class="label">Review Cost</div><div class="value">{format_cost(total_review_cost)}</div></div>
  <div class="stat-card"><div class="label">Total Time</div><div class="value">{total_duration // 60}m {total_duration % 60}s</div></div>
</div>
""")

# === Score Trends ===
html_parts.append("<h2>📈 Score Trends</h2>")
html_parts.append("<table><thead><tr>")
html_parts.append("<th>Project</th><th>Model</th><th>Strategy</th><th>Trend</th><th>Last Run</th><th>Tokens</th><th>Cost</th><th>Time</th>")
html_parts.append("</tr></thead><tbody>")

for (project, model, strategy), group_runs in sorted(groups.items()):
    scores_html = []
    for i, r in enumerate(group_runs):
        score = r.get("score", "?")
        color = score_color(score)
        scores_html.append(f'<span class="score-badge" style="background:{color}">{score}</span>')
        if i < len(group_runs) - 1:
            scores_html.append('<span class="arrow">→</span>')

    last = group_runs[-1]
    usage = last.get("usage", {})
    date = format_date(last.get("date", ""))

    html_parts.append(f"""<tr>
      <td><strong>{html.escape(project)}</strong></td>
      <td>{html.escape(model)}</td>
      <td>{html.escape(strategy)}</td>
      <td><div class="trend">{''.join(scores_html)}</div></td>
      <td>{date}</td>
      <td>{format_tokens(usage.get('total_tokens', 0))}</td>
      <td>{format_cost(usage.get('total_cost', 0))}</td>
      <td>{last.get('duration_seconds', 0)}s</td>
    </tr>""")

html_parts.append("</tbody></table>")

# === All Runs Detail ===
html_parts.append("<h2>📋 All Runs</h2>")

for idx, run in enumerate(reversed(runs)):  # newest first
    run_index = len(runs) - 1 - idx
    rid = run_id(run, run_index)
    project = run.get("project", "?")
    model = run.get("model", "?")
    strategy = run.get("strategy", "?")
    score = run.get("score", "?")
    date = format_date(run.get("date", ""))
    duration = run.get("duration_seconds", 0)
    usage = run.get("usage", {})
    checks = run.get("checks", {})
    exit_code = run.get("pi_exit_code", "?")

    checks_html = " ".join(
        f'<span class="check">{check_icon(v)} {k}</span>'
        for k, v in checks.items()
    )

    color = score_color(score)

    # Load review and pretty log
    review_text = load_review(run)
    pretty_text = load_pretty_log(run)

    html_parts.append(f"""
    <div style="background:var(--surface);border:1px solid var(--border);border-radius:8px;padding:1rem;margin-bottom:1rem;">
      <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:0.5rem;">
        <div>
          <strong>{html.escape(project)}</strong>
          <span class="score-badge" style="background:{color};margin-left:0.5rem;">{score}</span>
          <span style="color:var(--text2);margin-left:0.5rem;font-size:0.8rem;">{date}</span>
        </div>
        <div style="font-size:0.8rem;color:var(--text2);">
          {html.escape(model)} · {strategy} · {duration}s · {format_tokens(usage.get('total_tokens', 0))} tokens · {format_cost(usage.get('total_cost', 0))}
        </div>
      </div>
      <div class="checks" style="margin-top:0.5rem;">{checks_html}</div>
    """)

    # Tabs for details
    has_review = bool(review_text and review_text.strip())
    has_pretty = bool(pretty_text and pretty_text.strip())

    if has_review or has_pretty:
        html_parts.append(f"""
      <div style="margin-top:0.75rem;">
        <div class="tab-bar" id="tabs-{rid}">
    """)
        if has_pretty:
            html_parts.append(f'<div class="tab" onclick="switchTab(\'{rid}\',\'log\',this)">Migration Log</div>')
        if has_review:
            html_parts.append(f'<div class="tab" onclick="switchTab(\'{rid}\',\'review\',this)">Skill Review</div>')
        html_parts.append("</div>")

        if has_pretty:
            html_parts.append(f'<div class="details-panel" id="{rid}-log">{html.escape(pretty_text)}</div>')
        if has_review:
            html_parts.append(f'<div class="details-panel" id="{rid}-review">{html.escape(review_text)}</div>')

        html_parts.append("</div>")

    html_parts.append("</div>")

# === Per-Check Pass Rate Chart ===
all_check_names = []
check_counts = defaultdict(lambda: {"pass": 0, "total": 0})
for r in runs:
    for k, v in r.get("checks", {}).items():
        if k not in all_check_names:
            all_check_names.append(k)
        check_counts[k]["total"] += 1
        if v:
            check_counts[k]["pass"] += 1

html_parts.append("<h2>📊 Check Pass Rates</h2>")
for check_name in all_check_names:
    c = check_counts[check_name]
    pct = (c["pass"] / c["total"] * 100) if c["total"] > 0 else 0
    color = "#22c55e" if pct >= 80 else "#eab308" if pct >= 50 else "#ef4444"
    html_parts.append(f"""
    <div class="chart-bar-container">
      <div class="chart-label">{html.escape(check_name)}</div>
      <div style="flex:1;background:var(--surface2);border-radius:3px;">
        <div class="chart-bar" style="width:{pct}%;background:{color};"></div>
      </div>
      <div class="chart-value">{c['pass']}/{c['total']} ({pct:.0f}%)</div>
    </div>""")

# === Cost Comparison Chart ===
if len(groups) > 1:
    html_parts.append("<h2>💰 Cost Comparison (Last Run)</h2>")
    max_cost = max(g[-1].get("usage", {}).get("total_cost", 0) for g in groups.values()) or 1
    for (project, model, strategy), group_runs in sorted(groups.items()):
        last = group_runs[-1]
        cost = last.get("usage", {}).get("total_cost", 0)
        pct = (cost / max_cost * 100) if max_cost > 0 else 0
        label = f"{project} ({model.rsplit('/', 1)[-1][:20]})"
        html_parts.append(f"""
        <div class="chart-bar-container">
          <div class="chart-label">{html.escape(label)}</div>
          <div style="flex:1;background:var(--surface2);border-radius:3px;">
            <div class="chart-bar" style="width:{pct}%;background:var(--accent);"></div>
          </div>
          <div class="chart-value">{format_cost(cost)}</div>
        </div>""")

# === JS for tabs ===
html_parts.append("""
<footer>
  Generated by <a href="https://github.com/quarkusio/quarkus-migration-skills" style="color:var(--accent);">quarkus-migration-skills</a> test harness
</footer>

<script>
function switchTab(runId, tabName, el) {
  const panel = document.getElementById(`${runId}-${tabName}`);
  if (!panel) return;

  const wasOpen = panel.classList.contains('open');

  // Close all panels for this run
  document.querySelectorAll(`[id^="${runId}-"]`).forEach(p => {
    if (p.classList.contains('details-panel')) p.classList.remove('open');
  });
  // Deactivate all tabs for this run
  document.getElementById(`tabs-${runId}`).querySelectorAll('.tab').forEach(t => t.classList.remove('active'));

  // Toggle: if it was already open, leave it closed; otherwise open
  if (!wasOpen) {
    panel.classList.add('open');
    el.classList.add('active');
  }
}
</script>
</body>
</html>
""")

# Write output
output_path = Path(output_file)
output_path.parent.mkdir(parents=True, exist_ok=True)
output_path.write_text("".join(html_parts))
print(f"Report written to: {output_path}")
print(f"  {len(runs)} run(s), {len(groups)} configuration(s)")
PYTHON_SCRIPT
