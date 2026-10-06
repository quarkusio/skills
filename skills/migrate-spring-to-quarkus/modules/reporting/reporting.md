# Module: Reporting

Aggregate execution metrics, per-module results, and migration status into a consolidated report.

This module runs after the verification step, not as part of the module execution pipeline. It needs the verification results to produce an accurate report and record `finished_at`. All operations target `<target>`.

## What to do

- [ ] Collect execution metadata and write `execution-metadata.json`
- [ ] Read migration artifacts (spec, context, per-module reports, test logs)
- [ ] Generate `migration-summary.md`
- [ ] Write this module's own report

## 1. Collect execution metadata

Gather the following from the session context and write `<target>/migration-metadata/execution-metadata.json`.

| Data point | Where to find it |
|---|---|
| Agent (ACP) name | System context (e.g. "Claude Code", "opencode", "codex") |
| Model | System context model ID (e.g. "claude-sonnet-4-6") |
| Token usage | Session stats (input, output, thinking, cache read, cache write) |
| Estimated cost | From session stats if available, otherwise `null` |
| Start time | `migration-context.json` `generatedAt`, or earliest per-module report timestamp |
| End time | Current timestamp (record **after** verification completes) |
| Strategy | `migration-spec.yaml` `migration_strategy`, or the strategy chosen during planning |

### `execution-metadata.json` schema

```jsonc
{
  "schema_version": "1.0",
  "agent": {
    "name": "<string>",           // agent tool name
    "model": "<string>",          // model ID
    "version": "<string | null>"  // agent version if available
  },
  "execution": {
    "started_at": "<ISO-8601>",
    "finished_at": "<ISO-8601>",
    "duration_seconds": "<integer>",
    "strategy": "spring-compat | full-quarkus"
  },
  "tokens": {
    "input": "<integer>",
    "output": "<integer>",
    "thinking": "<integer | null>",
    "cache_read": "<integer | null>",
    "cache_write": "<integer | null>",
    "total": "<integer>"
  },
  "cost": {
    "estimated_usd": "<number | null>",  // null when not available from session
    "source": "<string>"                 // "session_stats" | "calculated" | "unavailable"
  },
  "modules": [
    {
      "name": "<string>",
      "status": "COMPLETED | SKIPPED | PARTIAL | FAILED",
      "gate_result": "PASS | SKIP | ALWAYS"
    }
  ],
  // SKILL.md verification checks: builds, no-spring-deps, has-quarkus, tests-pass, starts-up, no-thymeleaf
  "verification": {
    "checks_total": "<integer>",
    "checks_passed": "<integer>",
    "results": [
      {
        "check": "<string>",           // e.g. "builds", "no-spring-deps", "tests-pass"
        "result": "PASS | FAIL",
        "notes": "<string | null>"
      }
    ]
  }
}
```

If token usage or cost data is unavailable, set the values to `null` and add a note in the `notes` array of this module's report.

## 2. Read migration artifacts

Collect all available data for the report:

- [ ] `<target>/migration-spec.yaml` -- strategy, detected features, entities, services, repositories, controllers, decisions, unresolved issues
- [ ] `<target>/migration-metadata/migration-context.json` -- completed modules, timestamps, paths
- [ ] `<target>/migration-reports/*-report.json` -- per-module results (list directory, read each file)
- [ ] `<target>/migration-metadata/testlogs.txt` -- test output (if exists)

If a file does not exist, skip it. Note "data not available" in the corresponding section of the summary.

## 3. Generate migration-summary.md

Write `<target>/migration-summary.md` following the section structure below. If `migration-summary.md` already exists, overwrite it.

The sections below are normative and must appear in this order. Populate each section from the artifacts collected above.

### §1 -- Header

```markdown
# <Application Name> -> Quarkus Migration Summary

**Migration Date**: <date>
**Source**: <Framework> <version> (Java <version>)
**Target**: Quarkus <version> (Java <version>)
**Strategy**: <spring-compat | full-quarkus>
**Overall Status**: <emoji> **<N>% Complete** (Backend: <N>%, Frontend: <N>%)
```

- Source: `migration-spec.yaml` fields `source_technology` and `target_technology`
- Status emoji: checkmark when 100%, construction when partial, cross when failed
- Completion: ratio of COMPLETED modules to total enabled modules; split backend (build, code, testing, cleanup) and frontend

### §2 -- Executive Summary

Narrative paragraph: what completed, what is pending.

Then two bullet lists:
- **Key Achievements** -- one bullet per completed milestone
- **Remaining Work** -- one bullet per pending item

Source data: module statuses, `unresolved_issues` from migration-spec.yaml, TODO comments:
```bash
grep -rn "TODO.*[Mm]igration\|TODO.*[Qq]uarkus" <target>/src/ 2>/dev/null || true
```

### §3 -- Migration Modules

| Module | Name | Status | Details |
|--------|------|--------|---------|

One row per module from `execution-metadata.json`. Status values: Complete, Skipped, Pending, Failed.

### §4 -- Component Migration Status

One sub-section per component category. Heading includes the completion ratio:

```markdown
### JPA Entities (6/6 - 100%)
### Controllers (0/6 - 0%)
```

Source data: `migration-spec.yaml` sections `entities`, `repositories`, `services`, `rest_controllers`, `configurations`. Cross-reference with per-module reports for actual status.

Sub-section tables:

| Category | Columns |
|---|---|
| Entities | Entity, Table, Status, Notes |
| Repositories | Repository, Entity, Status, Notes |
| Services | Service, Status, Notes |
| Controllers | Controller, Endpoints, Status, Notes |
| Templates | Bulleted file list (no table) |
| Configuration | Configuration, Status, Notes |

End each sub-section with a **Migration Pattern** line describing the transformation approach.

### §5 -- Technology Mapping

Mandatory sub-sections: Framework, Persistence, Web Layer, Dependency Injection, Configuration.
Optional (include only when detected): Caching, Messaging, Security.

Each lists `before -> after` pairs as bullets.
Source data: `migration-spec.yaml` `migration_strategy`, `compat_mode`.

### §6 -- Build & Deployment

Build command outcomes, artifacts under `target/`, running instructions (dev mode, production, Docker).
Source data: `build-report.json`, verification results.

### §7 -- Database

Schema info, JDBC URL, relevant `application.properties` snippet in a fenced code block.
Source data: `migration-spec.yaml` `database`, config files in `<target>/src/main/resources/`.

### §8 -- Migration Reports

Numbered list of all `migration-reports/*-report.json` files with a one-line validator summary (rules passed / total) for each.

### Test Logs Appendix

If `<target>/migration-metadata/testlogs.txt` exists, include its full contents verbatim as a fenced code block appendix at the end of §8.

### §9 -- Next Steps

Three sub-sections: **Immediate**, **Short Term**, **Long Term**. Each as a numbered list.
Source data: `unresolved_issues`, TODO comments, verification failures.

### §10 -- Key Migration Patterns

Before/after Java code block pairs for the most important transformations. Include at minimum: entity migration, repository migration. Add caching, messaging, and web layer patterns when relevant.

### §11 -- Lessons Learned

Two sub-sections: **What Went Well**, **Challenges**. Each as a numbered list.
Source data: compilation failures encountered, retries, manual interventions during the run.

### §12 -- Resources

Links to relevant Quarkus guides based on the technologies used. Always include:
- https://quarkus.io/guides/
- Extension-specific guides matching `target_technology.quarkus_extensions`

### §13 -- Conclusion

One paragraph summarizing overall migration status and readiness.

### Footer

Last lines of the file:

```markdown
---
**Generated by**: <agent-name> (<model>)
**Date**: <date>
**Version**: 1.0
**Token usage**: <input> input / <output> output (~$<cost> estimated)
```

## 4. Write this module's report

Write `<target>/migration-reports/reporting-report.json`:

```jsonc
{
  "module": "reporting",
  "status": "SUCCESS",
  "timestamp": "<ISO-8601>",
  "summary": "Generated migration-summary.md and execution-metadata.json",
  "actions_performed": [
    { "action": "collect_execution_metadata", "description": "Gathered agent, model, token, and timing data" },
    { "action": "aggregate_module_reports", "description": "Read per-module reports from migration-reports/" },
    { "action": "generate_migration_summary", "description": "Produced migration-summary.md (13 sections)" }
  ],
  "files_created": [
    "migration-metadata/execution-metadata.json",
    "migration-summary.md",
    "migration-reports/reporting-report.json"
  ],
  "next_module": null,
  "notes": []
}
```

## 5. Update migration-context.json

If `<target>/migration-metadata/migration-context.json` exists:

- Add `"reporting"` to `completedModules`
- Set `currentModule` to `null` only if all verification checks passed; otherwise leave it as `"reporting"`
- Set `moduleReports.reporting` to `"migration-reports/reporting-report.json"`