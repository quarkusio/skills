---
name: migrate-simple-with-mtool
description: Migrates a Spring Boot application to Quarkus.
  Analyze the code source using mtool. Use when the user wants to migrate, convert, or port a Spring Boot app to Quarkus, mentions "spring to quarkus", "quarkus migration", "replace spring", or asks about migrating "pom.xml", "build.gradle", "Spring MVC", "Spring Data JPA", "@SpringBootApplication".
---

# Spring Boot to Quarkus — Code Migration (with mtool)

Simulate to migrate Spring Boot Java source code to Quarkus using `mtool` to analyze the project.

## Critical Rules

- **Non-interactive.** Do NOT ask the user any questions. Do NOT ask for strategy choices. Just run the steps below and report the results.
- **Use only the mtool analysis output.** Do NOT read Java source files — the scan report already contains every annotation, import, and file path you need.
- **Scope: `src/main/java/` only.** Ignore annotations/imports from test files (`src/test/`), frontend templates, static assets, or build plugins.
- **Don't compile the code** using maven or gradle.
- **Script location.** The helper scripts are in the `scripts/` subdirectory of this skill's directory, NOT in the project directory. Step 0 below locates them automatically — run it first.

## Step 0: Locate the skill scripts

Run this **before** any other step to find the scripts directory:

```bash
SKILL_DIR=$(find "$(git rev-parse --show-toplevel 2>/dev/null || pwd)" -type f -name "scan.sh" -path "*/migrate-simple-with-mtool/scripts/*" 2>/dev/null -exec dirname {} \; | head -1)
echo "SKILL_DIR=$SKILL_DIR"
```

If `SKILL_DIR` is empty, the scripts are missing — stop and tell the user.

## Step 1: Scan the project with mtool

```bash
REPORT=$($SKILL_DIR/scan.sh .)
```

## Step 2: Analyze annotations and imports

```bash
$SKILL_DIR/analyze.sh "$REPORT"
```

This outputs:

1. **Annotation Analysis** — grouped by action (REPLACE, REMOVE, REVIEW, KEEP, UNKNOWN) with affected file paths
2. **Import Analysis** — Spring imports grouped by action, including non-annotation APIs, with affected file paths

**Use this output directly as your migration report.** Do not read the source files — the annotation names, import paths, and file locations from the scan are sufficient. Only act on REPLACE, REMOVE, and REVIEW items.

## Step 3: Report

Present a brief summary based on the analyze.sh output:
- What annotations/imports need to change (REPLACE, REMOVE)
- What needs manual migration (REVIEW)
- Which files are affected

## Step 4: Stop

Stop the migration process as we are simulating and wanted to get the token usage