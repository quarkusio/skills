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
| [code](modules/code/code.md)                  | Spring annotations in Java sources (`@Component`, `@Service`, `@Controller`, `@Repository`, `@Entity`, `@Autowired`, etc.) | **PASS** if Spring annotations found; **SKIP** otherwise                                 |
| [messaging](modules/code/messaging.md)        | `@KafkaListener`, `@RabbitListener`, `@JmsListener`, `@SendTo`, `@EnableKafka`, `@EnableRabbit`, `KafkaTemplate`, `RabbitTemplate`, or `JmsTemplate` in Java sources | **PASS** if any found; **SKIP** otherwise                                                |
| [frontend](modules/frontend/frontend.md)      | Thymeleaf/JSP templates in `templates/` or `webapp/`, static resources in `static/`, JSF XHTML files in `webapp/` or `META-INF/resources/`, FreeMarker templates (`.ftl`, `.ftlh`, `.ftlx`), `freemarker.*` imports, or `FreeMarkerConfigurer` bean | **PASS** if view layer found; **SKIP** otherwise                                         |
| [testing](modules/testing/testing.md)         | Spring test annotations in test sources (`@SpringBootTest`, `@WebMvcTest`, `@MockBean`)                                   | **PASS** if Spring tests found; **SKIP** otherwise                                       |
| [cleanup](modules/cleanup/cleanup.md)         | Leftover Spring artifacts after all other modules                                                                          | **ALWAYS** — runs after all other modules                                                |

### Execution Protocol

```
STEP 0 — PREREQUISITE GATE (mandatory, runs before everything else)
  1. LOAD modules/prerequisite/prerequisite.md and execute all checks
  2. IF prerequisite gate == FAIL
       → log "Migration aborted — prerequisite gate failed: <failing check(s)>"
       → STOP immediately. Do not evaluate or execute any further module.
  3. IF prerequisite gate == PASS (warnings are allowed)
       → log "Prerequisite gate: PASS — proceeding with migration"
       → continue to the module loop below

FOR module IN [planning, build, code, messaging, frontend, testing, cleanup]:

  1. EVALUATE — inspect <source> for the gate condition
  2. DECIDE
     IF gate == ALWAYS → proceed to step 3
     IF gate == PASS   → proceed to step 3
     IF gate == SKIP   → log "Module {name}: SKIPPED — {reason}", mark checkbox, continue
  3. LOAD — read the module file and relevant reference files
  4. EXECUTE — read from <source>, write to <target>. Follow the module instructions, adapting to the chosen strategy
  5. COMPILE — run the compile command in <target> (`cd <target> && ./mvnw clean compile -DskipTests` for Maven, `cd <target> && ./gradlew clean compileJava -x test` for Gradle)
     Fails → load [modules/compile-fix.md](modules/compile-fix.md) and follow the retry procedure.
             If compile-fix reports MANUAL_REVIEW_REQUIRED (unresolved errors after 3 retries),
             ask the user whether to continue with the next module or stop the migration.
  6. LOG — mark checkbox as done
```

### Running Individual Modules

To run a single module outside the full migration flow, read all the files in the module folder directly:

- "Read `modules/build/build.md` and execute it"
- "Run only the frontend module"
- "Re-run the cleanup module"

The module will use the current `<source>` and `<target>` paths and the chosen strategy (if already decided). If no strategy has been chosen, the module will ask.

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

## Step 3: Migration Review (Self-Reflection)

Answer each question honestly:

1. **What migrated cleanly?** Patterns that mapped 1:1.
2. **What required manual judgment?** Non-obvious decisions made.
3. **What was left as TODO?** Every `// TODO: Migration required` comment and why.
4. **Was any code removed?** What, where, justification. Flag runtime risks.
5. **What checks failed initially?** Failures from Step 2 and how you fixed them.
6. **What's missing from the skill references?** Mappings you had to figure out.

### Migration Report

Present the review as a structured report:

```
## Migration Report: [app-name]

### Summary
- Source: [source directory path]
- Target: [target directory path]
- Strategy: [Full Migration / Spring Compatibility]
- Agent: [AI agent name - e.g claude, pi, opencode, gemini, etc]
- Model: [model name — e.g. claude-sonnet-4-6, check system context]
- Modules completed: [X/8]
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

