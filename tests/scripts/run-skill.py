#!/usr/bin/env python3
"""
run-skill.py — Run a migration skill against a Spring Boot project and
               export a markdown report with tokens, cost, and duration.

Usage:
    ./scripts/run-skill.py <skill-folder-name> [project-name]

Examples:
    ./scripts/run-skill.py migrate-simple-with-mtool
    ./scripts/run-skill.py migrate-simple-with-mtool spring-petclinic
    ./scripts/run-skill.py migrate-simple-without-mtool spring-rest-api

Override the model:
    MODEL=google-vertex-anthropic/claude-opus-4-6@default ./scripts/run-skill.py migrate-simple-with-mtool
"""

import json
import os
import shutil
import sqlite3
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_DIR = SCRIPT_DIR.parent
SKILLS_DIR = PROJECT_DIR / "skills"
OPENCODE_DIR = PROJECT_DIR / ".opencode"
PROJECTS_DIR = PROJECT_DIR / "projects"
REPORTS_DIR = PROJECT_DIR / "reports"
OPENCODE_DB = Path.home() / ".local" / "share" / "opencode" / "opencode.db"

DEFAULT_PROJECT = "spring-boot-todo-app"
MODEL = os.environ.get("MODEL", "google-vertex-anthropic/claude-opus-4-6@default")
PROMPT = (
    "Migrate this Spring Boot project to Quarkus. "
    "Follow the loaded skill instructions exactly — do only what the skill says, nothing more."
)


def list_skills():
    print("Available skills:")
    for d in sorted(SKILLS_DIR.iterdir()):
        if not d.is_dir() or d.name in ("shared", "dummy"):
            continue
        skill_md = d / "SKILL.md"
        desc = ""
        if skill_md.exists():
            for line in skill_md.read_text().splitlines():
                if line.startswith("description:"):
                    desc = line.removeprefix("description:").strip()[:80]
                    break
        print(f"  {d.name}")
        if desc:
            print(f"    {desc}")


def list_projects():
    print("Available projects:")
    for d in sorted(PROJECTS_DIR.iterdir()):
        if not d.is_dir() or d.name == "dummy":
            continue
        source = d / "source"
        marker = "✓" if (source / "pom.xml").exists() or (source / "build.gradle").exists() else "✗"
        print(f"  {marker} {d.name}")


def read_skill_field(path: Path, field: str) -> str:
    for line in path.read_text().splitlines():
        if line.startswith(f"{field}:"):
            return line.removeprefix(f"{field}:").strip()
    return ""


def read_session_usage(title: str) -> dict | None:
    """Read token usage and cost from the opencode SQLite database."""
    if not OPENCODE_DB.exists():
        return None
    try:
        conn = sqlite3.connect(str(OPENCODE_DB))
        conn.row_factory = sqlite3.Row
        row = conn.execute(
            "SELECT cost, tokens_input, tokens_output, tokens_reasoning, "
            "tokens_cache_read, tokens_cache_write, model "
            "FROM session WHERE title = ? ORDER BY time_created DESC LIMIT 1",
            (title,),
        ).fetchone()
        conn.close()
        if not row:
            return None
        model_info = row["model"]
        model_name = MODEL
        if model_info:
            try:
                m = json.loads(model_info)
                model_name = f"{m.get('providerID', '')}/{m.get('id', '')}"
            except (json.JSONDecodeError, TypeError):
                pass
        total = (
            row["tokens_input"] + row["tokens_output"]
            + row["tokens_cache_read"] + row["tokens_cache_write"]
        )
        return {
            "input": row["tokens_input"],
            "output": row["tokens_output"],
            "reasoning": row["tokens_reasoning"],
            "cache_read": row["tokens_cache_read"],
            "cache_write": row["tokens_cache_write"],
            "total": total,
            "cost": row["cost"],
            "model": model_name,
        }
    except sqlite3.Error:
        return None


def read_phase_costs(title: str) -> dict | None:
    """Break down token usage into analysis vs migration phases.

    Walks the step-start/tool/text/step-finish parts of the session.
    Steps before the first mention of "Step 2" or "Migrate" are analysis;
    everything after is migration.
    """
    if not OPENCODE_DB.exists():
        return None
    try:
        conn = sqlite3.connect(str(OPENCODE_DB))
        session_row = conn.execute(
            "SELECT id FROM session WHERE title = ? ORDER BY time_created DESC LIMIT 1",
            (title,),
        ).fetchone()
        if not session_row:
            conn.close()
            return None

        parts = conn.execute(
            "SELECT data FROM part WHERE session_id = ? ORDER BY time_created",
            (session_row[0],),
        ).fetchall()
        conn.close()

        analysis = {"tokens": 0, "cost": 0.0, "steps": 0}
        migration = {"tokens": 0, "cost": 0.0, "steps": 0}
        step_texts = []
        analysis_done = False

        for (raw,) in parts:
            d = json.loads(raw)
            ptype = d.get("type", "")

            if ptype == "step-start":
                step_texts = []
            elif ptype == "text":
                txt = d.get("text", "").strip()
                if txt:
                    step_texts.append(txt)
            elif ptype == "step-finish":
                for txt in step_texts:
                    if any(k in txt for k in ("Step 2", "Migrate pom", "Migrate App", "Delete App")):
                        analysis_done = True

                tokens = d.get("tokens", {})
                total = tokens.get("total", 0)
                cost = d.get("cost", 0)

                bucket = migration if analysis_done else analysis
                bucket["tokens"] += total
                bucket["cost"] += cost
                bucket["steps"] += 1

        return {"analysis": analysis, "migration": migration}
    except (sqlite3.Error, json.JSONDecodeError):
        return None


def format_tokens(n: int) -> str:
    if n >= 1_000_000:
        return f"{n / 1_000_000:.1f}M"
    if n >= 1_000:
        return f"{n:,}"
    return str(n)


def usage_to_markdown(usage: dict) -> str:
    """Format session usage as a markdown table."""
    return (
        "| Metric | Value |\n"
        "| --- | --- |\n"
        f"| Model | `{usage['model']}` |\n"
        f"| Input tokens | {format_tokens(usage['input'])} |\n"
        f"| Output tokens | {format_tokens(usage['output'])} |\n"
        f"| Cache read | {format_tokens(usage['cache_read'])} |\n"
        f"| Cache write | {format_tokens(usage['cache_write'])} |\n"
        f"| Total tokens | {format_tokens(usage['total'])} |\n"
        f"| Cost | ${usage['cost']:.2f} |"
    )


def extract_migration_summary(log: str) -> str:
    """Extract the migration report section from opencode output."""
    for marker in ("## Migration Report", "# Migration Report"):
        idx = log.find(marker)
        if idx >= 0:
            return log[idx:]
    return "_(No migration summary found in opencode output)_"


def run(skill_name: str, project_name: str):
    skill_dir = SKILLS_DIR / skill_name
    skill_md = skill_dir / "SKILL.md"
    shared_md = SKILLS_DIR / "shared" / "migration-steps.md"
    source_dir = PROJECTS_DIR / project_name / "source"

    if not skill_md.exists():
        print(f"ERROR: Skill not found: {skill_md}")
        print(f"Run '{sys.argv[0]}' without arguments to see available skills.")
        sys.exit(1)

    if not source_dir.exists():
        print(f"ERROR: Project source not found: {source_dir}")
        print()
        list_projects()
        sys.exit(1)

    for cmd in ("opencode", "git"):
        if not shutil.which(cmd):
            print(f"ERROR: '{cmd}' not found in PATH")
            sys.exit(1)

    date_stamp = datetime.now().strftime("%Y-%m-%d_%H-%M")
    title = f"{project_name}_{skill_name}_{date_stamp}"
    skill_report_dir = REPORTS_DIR / skill_name
    skill_report_dir.mkdir(parents=True, exist_ok=True)
    report_file = skill_report_dir / f"{title}.md"

    print("═" * 60)
    print(f"  Skill:   {skill_name}")
    print(f"  Project: {project_name}")
    print(f"  Model:   {MODEL}")
    print(f"  Source:  {source_dir}")
    print(f"  Title:   {title}")
    print(f"  Report:  {report_file}")
    print("═" * 60)

    # Step 1: Restore project source
    print(f"\n→ Restoring {project_name} source to git-clean state...")
    subprocess.run(
        ["git", "-C", str(PROJECT_DIR), "restore", f"projects/{project_name}/source/"],
        check=True,
    )

    # Step 2: Clear opencode session data
    print("→ Clearing opencode session data...")
    oc_data = Path.home() / ".local" / "share" / "opencode"
    if oc_data.exists():
        shutil.rmtree(oc_data)
        oc_data.mkdir(parents=True, exist_ok=True)

    # Step 3: Copy the chosen skill + shared content
    print(f"→ Copying skill files for: {skill_name}")
    skills_target = OPENCODE_DIR / "skills"
    shared_target = OPENCODE_DIR / "shared"
    skills_target.mkdir(parents=True, exist_ok=True)
    shared_target.mkdir(parents=True, exist_ok=True)

    dest_skill = skills_target / "SKILL.md"
    dest_shared = shared_target / "migration-steps.md"
    dest_skill.unlink(missing_ok=True)
    dest_shared.unlink(missing_ok=True)

    shutil.copy2(skill_md, dest_skill)
    print(f"  skills/SKILL.md ← {skill_md.resolve()}")

    if shared_md.exists():
        shutil.copy2(shared_md, dest_shared)
        print(f"  shared/migration-steps.md ← {shared_md.resolve()}")

    loaded_name = read_skill_field(dest_skill, "name")
    print(f"  Loaded skill: {loaded_name}")

    # Step 4: Run opencode and capture output + duration
    print("\n→ Running opencode...")
    print("─" * 60)

    start = time.time()
    proc = subprocess.Popen(
        [
            "opencode", "run",
            "--dir", str(source_dir),
            "--title", title,
            "-m", MODEL,
            PROMPT,
        ],
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
    )
    output_lines = []
    for line in proc.stdout:
        sys.stdout.write(line)
        sys.stdout.flush()
        output_lines.append(line)
    proc.wait()
    duration_secs = int(time.time() - start)
    duration_min = duration_secs // 60
    duration_sec = duration_secs % 60

    opencode_output = "".join(output_lines)
    print("─" * 60)
    print(f"  Duration: {duration_min}m {duration_sec}s ({duration_secs}s)")

    # Step 5: Read token usage from opencode DB
    print("\n→ Reading token usage from opencode database...")
    usage = read_session_usage(title)
    if usage:
        print(f"  Input:      {format_tokens(usage['input'])}")
        print(f"  Output:     {format_tokens(usage['output'])}")
        print(f"  Cache read: {format_tokens(usage['cache_read'])}")
        print(f"  Total:      {format_tokens(usage['total'])}")
        print(f"  Cost:       ${usage['cost']:.2f}")
        usage_table = usage_to_markdown(usage)
    else:
        print("  WARNING: Could not read session from opencode database")
        usage_table = "_(Could not read token usage from opencode database)_"

    # Step 5b: Read analysis vs migration cost breakdown
    phases = read_phase_costs(title)
    if phases:
        a = phases["analysis"]
        m = phases["migration"]
        print(f"\n  Phase breakdown:")
        print(f"    Analysis:  {a['steps']} steps, {format_tokens(a['tokens'])} tokens, ${a['cost']:.4f}")
        print(f"    Migration: {m['steps']} steps, {format_tokens(m['tokens'])} tokens, ${m['cost']:.4f}")
        phase_table = (
            "| Phase | Steps | Tokens | Cost |\n"
            "| --- | --- | --- | --- |\n"
            f"| Analysis | {a['steps']} | {format_tokens(a['tokens'])} | ${a['cost']:.4f} |\n"
            f"| Migration | {m['steps']} | {format_tokens(m['tokens'])} | ${m['cost']:.4f} |\n"
            f"| **Total** | **{a['steps'] + m['steps']}** | **{format_tokens(a['tokens'] + m['tokens'])}** | **${a['cost'] + m['cost']:.4f}** |"
        )
    else:
        phase_table = "_(Could not read phase breakdown from opencode database)_"

    # Step 6: Extract migration summary
    migration_summary = extract_migration_summary(opencode_output)

    # Step 7: Write markdown report
    skill_description = read_skill_field(skill_md, "description")
    report_date = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    report = f"""# Migration Run Report

## Run Info

| Field | Value |
|-------|-------|
| Skill | `{skill_name}` |
| Project | {project_name} |
| Model | `{MODEL}` |
| Title | `{title}` |
| Date | {report_date} |
| Duration | {duration_min}m {duration_sec}s ({duration_secs}s) |

## Token Usage

{usage_table}

## Cost Breakdown: Analysis vs Migration

{phase_table}

## Migration Summary

{migration_summary}

## Skill Description

{skill_description}

## Reproduction

```bash
MODEL="{MODEL}" ./scripts/run-skill.py {skill_name} {project_name}
```
"""
    report_file.write_text(report)

    print()
    print("═" * 60)
    print(f"  Done: {skill_name} / {project_name}")
    print(f"  Duration: {duration_min}m {duration_sec}s")
    print(f"  Report:   {report_file}")
    print("═" * 60)


if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] in ("-h", "--help"):
        print(f"Usage: {sys.argv[0]} <skill-folder-name> [project-name]\n")
        print(f"Default project: {DEFAULT_PROJECT}\n")
        list_skills()
        print()
        list_projects()
        sys.exit(1 if len(sys.argv) < 2 else 0)

    skill = sys.argv[1]
    project = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_PROJECT
    run(skill, project)