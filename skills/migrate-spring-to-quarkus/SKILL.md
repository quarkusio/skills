---
name: migrate-spring-to-quarkus
description: Migrates Spring Boot applications to Quarkus using a modular, gate-driven approach. 
  Supports Spring compatibility extensions and full Quarkus migration paths. 
  Use when the user wants to migrate, convert, or port a Spring Boot app to Quarkus, mentions "spring to quarkus", 
  "quarkus migration", "replace spring", or asks about migrating "pom.xml", "build.gradle", "Spring MVC", "Spring Data JPA", "Thymeleaf", "@SpringBootApplication".
license: Apache-2.0
metadata:
  author: Quarkus Community - https://github.com/quarkusio/quarkus
---

# Spring Boot to Quarkus Migration

Modular, gate-driven migration of Spring Boot applications to Quarkus.

## Critical Rules

- **Never delete code you cannot migrate.** If you cannot fully migrate a piece of code, leave the original in place with a `// TODO: Migration required — <reason>` comment explaining what needs to change and why. This applies to:
    - Methods, classes, or annotations you don't know how to convert
    - Spring-specific patterns without a clear Quarkus equivalent
    - Configuration or wiring code whose purpose is unclear
      If you must remove code (e.g., a Spring-only base class), document what was removed and why in a `// REMOVED:` comment at the same location.
- **Don't break the build.** Run the compile command in `<target>` after each phase (`cd <target> && ./mvnw clean compile -DskipTests` for Maven, `cd <target> && ./gradlew clean compileJava -x test` for Gradle). Never move to the next phase with a broken build.
- **Source is read-only.** Never modify files in `<source>`. All changes go into `<target>`. The only exception is `<source>/migration-metadata/`, where extraction metadata may be written.
- **Document every decision.** When choosing between migration approaches, explain the trade-off to the user.
- **No silent changes.** Every file modification must be intentional and traceable. If a check fails after a phase, diagnose and fix — don't skip the check or delete the failing code.
- **Every invocation starts at STAGE 0, then STAGE 1.** This applies to the first run and to every later invocation: "continue", "resume", a restarted session, a new chat, or a compacted/summarized context. Never continue a migration from conversation memory or a summary. Run the prerequisite gate, then `Checkpoint.java status <target>` and act on its verdict, before touching any module. A module left `in_progress` means the previous run was interrupted; it must be recovered via `Checkpoint.java resume`, not by silently carrying on.

## Reference Files

Load the relevant reference file when working on a module:

| Reference | Use during |
|---|---|
| [references/dependency-map.md](references/dependency-map.md) | Build module: dependency and plugin mapping |
| [references/annotation-map.md](references/annotation-map.md) | Code module: annotation, DI, REST, Data, Security migration |
| [references/config-map.md](references/config-map.md) | Build module: configuration property migration |


## Step 1: Execute Modules

## Instructions

- Execute the instructions of the modules according to the following Decision Gate Table
- Always log which Module and Gate check is evaluated and the status using the format:
  Gate result: <STATUS> and <CONDITION_EVALUATED>

### Decision Gate Table 

- For each module, evaluate whether it applies by inspecting `<source>`. A module executes only when its gate status is: **PASS**.
- Inspect `<source>` to determine the gate result -- do not rely on blind grep commands; use your understanding of the codebase.

| Module                                        | Gate Check                                                                                                                | Gate Result                                                                              |
|-----------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------|
| [prerequisite](modules/prerequisite/prerequisite.md) | JDK version, build tool (Maven ≥ 3.6 recommended 3.9+ / Gradle ≥ 7 recommended 8.x), container runtime                           | **ALWAYS** — **ABORT** entire migration if any hard check fails; no subsequent module runs |
| [planning](modules/planning/planning.md)      | Prerequisite check passed                                                                                                 | **ALWAYS** — generates `<target>/migration-spec.yaml`                                    |
| [build](modules/build/build.md)               | Spring Boot parent/starters/`spring-boot-maven-plugin` in `pom.xml`, or Spring Boot/`io.spring.dependency-management` plugins in `build.gradle(.kts)` | **PASS** if Spring Boot build markers found; **SKIP** otherwise                          |
| [persistence](modules/code/persistence.md)    | `@Entity`, `JpaRepository`, `CrudRepository`, or `@Repository` in Java sources                                            | **PASS** if JPA/persistence annotations found; **SKIP** otherwise                        |
| [code](modules/code/code.md)                  | Spring annotations in Java sources (`@Component`, `@Service`, `@Controller`, `@Autowired`, etc.) | **PASS** if Spring annotations found; **SKIP** otherwise                                 |
| [messaging](modules/code/messaging.md)        | `@KafkaListener`, `@RabbitListener`, `@JmsListener`, `@SendTo`, `@EnableKafka`, `@EnableRabbit`, `KafkaTemplate`, `RabbitTemplate`, or `JmsTemplate` in Java sources | **PASS** if any found; **SKIP** otherwise                                                |
| [frontend](modules/frontend/frontend.md)      | Thymeleaf/JSP templates in `templates/` or `webapp/`, static resources in `static/`, JSF XHTML files in `webapp/` or `META-INF/resources/`, FreeMarker templates (`.ftl`, `.ftlh`, `.ftlx`), `freemarker.*` imports, or `FreeMarkerConfigurer` bean | **PASS** if view layer found; **SKIP** otherwise                                         |
| [testing](modules/testing/testing.md)         | Spring test annotations in test sources (`@SpringBootTest`, `@WebMvcTest`, `@MockBean`)                                   | **PASS** if Spring tests found; **SKIP** otherwise                                       |
| [cleanup](modules/cleanup/cleanup.md)         | Leftover Spring artifacts after all other modules                                                                          | **ALWAYS** -- runs after all other modules                                                |

### Execution Protocol

```
STAGE 0 — PREREQUISITE GATE (mandatory, runs before everything else)
  1. LOAD modules/prerequisite/prerequisite.md and execute all checks
  2. IF prerequisite gate == FAIL
       → log "Migration aborted — prerequisite gate failed: <failing check(s)>"
       → STOP immediately. Do not evaluate or execute any further module.
  3. IF prerequisite gate == PASS (warnings are allowed)
       → log "Prerequisite gate: PASS — proceeding with migration"
       → continue to STAGE 1

STAGE 1 — RESUME CHECK (see "Resumable Migration" below; MANDATORY on EVERY invocation, before the module loop)
  "Once" means once per invocation, NOT once per migration. A NOT_FOUND result in an earlier
  conversation, or any summary of earlier work, does not satisfy this stage. The first
  command of this stage is always `Checkpoint.java status <target>`; its output, not memory,
  decides what runs next. If the verdict is RESUMABLE, `Checkpoint.java resume <target>` MUST
  be run (resumeCount increments) before any module is started.

STAGE 2 — MODULE LOOP
FOR module IN [planning, build, persistence, code, messaging, frontend, testing, cleanup]
    (on a resumed run: only the modules in `pendingModules`):

  1. EVALUATE — inspect <source> for the gate condition
  2. DECIDE
     IF gate == ALWAYS → proceed to step 3
     IF gate == PASS   → proceed to step 3
     IF gate == SKIP   → log "Module {name}: SKIPPED — {reason}", run
                         `Checkpoint.java complete <target> <module> skipped --gate SKIP --reason "<reason>"`, continue
  3. CHECKPOINT — run `Checkpoint.java start <target> <module>`
                  (for `planning` on a fresh run, <target> has no context file yet; skip this call —
                   planning creates the file in its Step 5 and records its own completion)
  4. LOAD — read the module file and relevant reference files
  5. EXECUTE — read from <source>, write to <target>. Follow the module instructions, adapting to the chosen strategy
  6. COMPILE — run the compile command in <target> (`cd <target> && ./mvnw clean compile -DskipTests` for Maven, `cd <target> && ./gradlew clean compileJava -x test` for Gradle)
     Fails → load [modules/compile-fix.md](modules/compile-fix.md) and follow the retry procedure.
             If compile-fix reports MANUAL_REVIEW_REQUIRED (unresolved errors after 3 retries),
             ask the user whether to continue with the next module or stop the migration.
             If continuing → `Checkpoint.java complete <target> <module> completed --gate <G> --reason "<gate reason>" --compile FAIL --compile-attempts <n> [--report ...]`, go to the next module.
             (The module has run; a failing compile is recorded in `modules.<module>.compile`, not as a separate status.)
  7. CHECKPOINT — after the module's report JSON is written and compile passes, run
                  `Checkpoint.java complete <target> <module> completed --gate <PASS|ALWAYS> --reason "<gate reason>" --compile PASS --compile-attempts <n> --report migration-reports/<module>-report.json`
                  (`<n>` = compile-fix retries used, 0 if the first compile passed; omit `--report` if the module writes none).
                  Never run this earlier.
  8. LOG — mark checkbox as done
```

### Resumable Migration

State is kept in `<target>/migration-metadata/migration-context.json` and managed **only** through [`scripts/Checkpoint.java`](scripts/Checkpoint.java), a single-file Java program run with the JDK the prerequisite gate already guarantees (no build step, no dependencies). Never edit the context file by hand or infer progress from which files exist in `<target>`.

The file records, for the whole run: `currentModule` and `lastCheckpointAt`. For every module it records a `modules.<name>` entry, which is the only place a module's outcome is stored: `status` (`in_progress` / `completed` / `skipped`), `gate`, `gateReason`, `startedAt`, `finishedAt`, `compile` (`PASS` / `FAIL` / `NOT_RUN`), and `compileAttempts`.

In this document `Checkpoint.java <command>` is shorthand for `java <skill>/scripts/Checkpoint.java <command>`:

```
Checkpoint.java status   <target>
Checkpoint.java resume   <target> [--accept-drift] [--max-resumes <n>]
Checkpoint.java start    <target> <module>
Checkpoint.java complete <target> <module> <completed|skipped>
        [--gate PASS|SKIP|ALWAYS] [--reason "<text>"] [--compile PASS|FAIL|NOT_RUN] [--compile-attempts <n>] [--description "<text>"] [--report <relpath>]
Checkpoint.java reset    <target> <module>
```

`--description` is optional free text stored as `modules.<name>.statusDescription`; use it to explain a non-obvious outcome, such as why a module was accepted with `--compile FAIL`. Omit it otherwise.

Every command prints JSON. A `verdict` of `ERROR` or `RESUME_LIMIT_REACHED` (exit code 1) is a hard stop: report it to the user, do not continue.

**Resume check (STAGE 1: once per invocation, after the prerequisite gate, before any module; never skipped, including for "continue" / "resume" / restarted sessions / compacted context):**

1. Resolve `<target>` as in planning Step 0, then run `Checkpoint.java status <target>`. If the user supplied a custom `<target>` on the first run, they must supply the same path again (skill argument or prompt); the default is `<source-name>-quarkus/` next to `<source>`. The context file also records `sourceRepo` and `targetRepo` for reference.
2. Act on `verdict`:

| Verdict | Action |
|---|---|
| `NOT_FOUND` | Fresh run. Log `Resume: no checkpoint found`, go to STAGE 2 (module loop). |
| `CORRUPT` | Tell the user. Ask: start fresh (delete `<target>`) or abort. Never guess a resume point. |
| `COMPLETE` | Log `Resume: migration already complete`, skip STAGE 2, go to Step 2 (Verify). |
| `RESUMABLE` | Continue below. |

3. **Decide resume vs. fresh** — first match wins: skill argument `resume: true\|false`, then `resume` in `.quarkus-migration.yml`; otherwise interactive → show `completedModules`, `skippedModules`, `currentModule`, `lastCheckpointAt` and ask "resume or start fresh (deletes `<target>`)?"; `mode: non-interactive` → resume. Delete `<target>` only after explicit confirmation or `resume: false`.
4. **Handle warnings from `status`:**
   - `drift: true` (source changed since the last run, so completed modules may be stale) → interactive: ask whether to resume anyway (then pass `--accept-drift` to `resume`) or start fresh; non-interactive: start fresh.
   - `specWarning` or non-empty `missingReports` → run `Checkpoint.java reset <target> <module>` for the affected module (`planning` for the spec) so it re-runs.
5. Run `Checkpoint.java resume <target>`. If the verdict is `RESUME_LIMIT_REACHED` (`resumeCount` reached `maxResumes`, default 5; `status` shows this as `resumeLimitReached: true`), **STOP**: tell the user the migration has been resumed too many times, ask them to inspect `<target>`, and do not run any module. Pass `--max-resumes <n>` only after the user explicitly approves a higher limit.
   Otherwise it increments `resumeCount` and clears the interrupted `currentModule`; that module is re-run from its start (safe, because modules read `<source>` and write `<target>`).
6. **Load prior decisions** from `<target>/migration-spec.yaml`. Do not re-ask strategy, versions, or any `decisions.*`; planning is not re-run unless it is in `pendingModules`. If planning is pending, it keeps the existing `<target>` (no overwrite prompt) and does not call `init` again.
7. **Baseline compile** in `<target>`; on failure run [modules/compile-fix.md](modules/compile-fix.md) before the first pending module.
8. Log `Resume: continuing from <first pending module> (completed: <list>)` and run STAGE 2 over `pendingModules` only. Completed and skipped modules are not re-evaluated, re-loaded, or re-compiled.

**Limits:** granularity is the module (an interrupted module restarts from its beginning). The prerequisite gate, the resume check (STAGE 1), verification (Step 2), and reporting (Step 3) re-run on every invocation because they reflect the current environment and `<target>`.

**Self-check before starting any module:** confirm that `Checkpoint.java status <target>` was executed in THIS invocation and, if its verdict was `RESUMABLE`, that `Checkpoint.java resume <target>` was executed too. If not, stop and run them now. A `status` showing a module `in_progress` with `resumeCount: 0` means this step was skipped after an interruption.

### Running Individual Modules

To run a single module outside the full migration flow, read all the files in the module folder directly:

- "Read `modules/build/build.md` and execute it"
- "Run only the frontend module"
- "Re-run the cleanup module"

The module will use the current `<source>` and `<target>` paths and the chosen strategy (if already decided). If no strategy has been chosen, the module will ask.

When re-running a module that already completed, first run `Checkpoint.java reset <target> <module>`. This also clears every later module in the order above, since they may depend on its output; tell the user and let them keep later modules by re-marking them `completed`.

## Step 2: Verify the Migration

All verification checks run against `<target>`. Run each check in order. A check fails = stop and fix before continuing.

| # | Check | Command (run in `<target>`) | Pass criteria |
|---|-------|---------|---------------|
| 1 | **Builds** | `cd <target> && ./mvnw clean package -DskipTests` / `cd <target> && ./gradlew clean build -x test` | Exit code 0, no compilation errors |
| 2 | **No Spring deps** | Search `<target>` build file for `org.springframework` | Zero Spring deps (except Spring compat extensions if using that strategy) |
| 3 | **Has Quarkus** | Search `<target>` build file for `io.quarkus` | Quarkus BOM and at least one extension present |
| 4 | **Tests pass** | `cd <target> && ./mvnw test` / `cd <target> && ./gradlew test` | All tests pass using `@QuarkusTest` |
| 5 | **Starts up** | `cd <target> && ./mvnw quarkus:dev` / `cd <target> && ./gradlew quarkusDev` | App starts, `curl http://localhost:8080/q/health` returns UP |
| 6 | **No leftover templates** | Search `<target>` for Thymeleaf/JSP references | None remaining (unless intentionally kept) |

## Step 3: Reporting

After verification completes, run the [reporting](modules/reporting/reporting.md) module. It reads the verification results and all per-module data to produce:

- `<target>/migration-metadata/execution-metadata.json` -- agent, model, tokens, cost, timing, verification results
- `<target>/migration-summary.md` -- full 13-section migration report

The reporting module must run **after** verification so that it can include check results and record an accurate `finished_at` timestamp.

### Console Summary

After the reporting module completes, present the following directly to the user so they get an immediate overview without opening the generated files.

Answer each question honestly:

1. **What migrated cleanly?** Patterns that mapped 1:1.
2. **What required manual judgment?** Non-obvious decisions made.
3. **What was left as TODO?** Every `// TODO: Migration required` comment and why.
4. **Was any code removed?** What, where, justification. Flag runtime risks.
5. **What checks failed initially?** Failures from verification and how you fixed them.
6. **What's missing from the skill references?** Mappings you had to figure out.

Then present the review as a structured report:

```
## Migration Report: [app-name]

### Summary
- Source: [source directory path]
- Target: [target directory path]
- Strategy: [Full Migration / Spring Compatibility]
- Agent: [AI agent name - e.g claude, pi, opencode, gemini, etc]
- Model: [model name — e.g. claude-sonnet-4-6, check system context]
- Modules completed: [X/9]
- Checks passed: [X/6]
- Token usage: [input tokens / output tokens — check session stats]
- Estimated cost: [~$X.XX — token counts × per-model pricing from anthropic.com/pricing]

### Changes by Module
| Module | Files changed | Key changes |
|--------|--------------|-------------|
| build | pom.xml or build.gradle(.kts), application.properties | ... |
| code | ... | ... |
| frontend | ... | ... |
| testing | ... | ... |

### Validation Results
| Check | Result | Notes |
|-------|--------|-------|
| Builds | PASS/FAIL | |
| No Spring deps | PASS/FAIL | |
| Has Quarkus | PASS/FAIL | |
| Tests pass | PASS/FAIL | |
| Starts up | PASS/FAIL | |
| No leftover templates | PASS/FAIL | |

### Unmigrated Code (TODOs)
| File | Line | What | Why not migrated |
|------|------|------|-----------------|

### Removed Code
| File | What was removed | Justification |
|------|-----------------|---------------|

### Skill Improvement Suggestions
- [Any missing mappings, unclear instructions, or edge cases discovered]
```

