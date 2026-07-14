---
name: migrate-simple-with-mtool
description: Migrates Spring Boot applications to Quarkus using a modular, gate-driven approach and mtool.
  Analyze the code source using mtool. Use when the user wants to migrate, convert, or port a Spring Boot app to Quarkus, mentions "spring to quarkus", "quarkus migration", "replace spring", or asks about migrating "pom.xml", "build.gradle", "Spring MVC", "Spring Data JPA", "@SpringBootApplication".
license: Apache-2.0
metadata:
  author: Quarkus Community
---

# Spring Boot to Quarkus — Code Migration (with mtool)

Migrate Spring Boot Java source code to Quarkus using `mtool` to analyze the project.

## Critical Rules

- **Analysis uses mtool ONLY — no exploration.** During Step 1 you MUST NOT call Read, glob, grep, find, or ls on any `.java`, `.properties`, `.yml`, `.html`, `.js`, or `.css` file. You MUST NOT list, scan, or browse the `src/` directory, `resources/`, `templates/`, or `static/`. The ONLY Read allowed is `pom.xml` or `build.gradle`. Do NOT "get a full picture" or "explore the project" — the mtool report IS the full picture. After Step 1c, proceed immediately to Step 2. Any file read not listed in these instructions is a violation.
- **Scope: code migration only.** Do NOT migrate tests, frontend templates, static assets, or build plugins. Only migrate Java source files under `src/main/java/` and the build file (`pom.xml` or `build.gradle`). Leave everything else untouched.
- **Never delete code you cannot migrate.** Leave the original in place with a `// TODO: Migration required — <reason>` comment.
- **Don't compile the code** using maven or gradle.

## Step 1: Analyze the Project

### 1a. Run the scanner

Run `mtool` FIRST, ALONE — do not batch it with any file reads:

```bash
mtool scan --plan "$MTOOL_PLAN" . --scanner treesitter -o json
```

- `MTOOL_PLAN` — path to the plan file (default: `$HOME/.mtool/plans/java-app.yml`).

After running the command, locate the generated report:

```bash
REPORT=$(ls -t scanning-treesitter-report_*.json 2>/dev/null | head -1)
```

### 1b. Extract findings

Run exactly these 2 commands. Do not add any other tool calls.

**Command 1 — Build system:** Read `pom.xml` (Maven) or `build.gradle(.kts)` (Gradle) to identify the build tool.

**Command 2 — All code findings (single query):**
```bash
jq '{
  dependencies: [.results["Plan to analyze java code source :: find all pom.dependencies"][]?.result],
  classes:      [.results[][] | select(.id? // "" | test("^java-class")) | .result],
  annotations:  [.results[][] | select(.id? // "" | test("^java-annotation")) | .result],
  interfaces:   [.results[][] | select(.id? // "" | test("^java-interface")) | .result],
  imports:      [.results[][] | select(.id? // "" | test("^java-import")) | .result]
}' "$REPORT"
```

### 1c. Identify what to migrate

From Command 2 output, identify:
- Which Java files contain Spring annotations (from the `annotations` array)
- Which Spring annotations are used and which files they appear in
- Whether there is a `@SpringBootApplication` main class to remove
- Which Spring imports it has (`org.springframework.*`)

Present a brief summary of what was found and what needs to change.

**Step 1 is now COMPLETE. Do NOT read any more files. Proceed directly to Step 2.**

## Step 2: Migrate Code — using the mtool report

You already have the complete list of classes, annotations, and dependencies from the jq report output above. Use this data to determine exactly which files need which annotation replacements — do NOT re-read files to discover annotations.

For each file that the report shows has Spring annotations:
1. **Read** the file ONLY because you need its current content to produce the edited version
2. **Apply** the annotation mapping and **update** the imports using the tables in the shared migration steps below
3. **Write** the modified file

Load [../shared/migration-steps.md](../shared/migration-steps.md) for the annotation mapping tables (Step 2) and report template (Step 3).
