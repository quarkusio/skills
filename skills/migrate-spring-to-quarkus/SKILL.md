---
name: migrate-spring-to-quarkus
description: Migrates Spring Boot applications to Quarkus using a modular, gate-driven approach. 
  Supports Spring compatibility extensions and native Quarkus migration paths. 
  Use when the user wants to migrate, convert, or port a Spring Boot app to Quarkus, mentions "spring to quarkus", 
  "quarkus migration", "replace spring", or asks about migrating "pom.xml", "Spring MVC", "Spring Data JPA", "Thymeleaf", "@SpringBootApplication".
license: Apache-2.0
metadata:
  author: Quarkus Team - https://github.com/quarkusio/quarkus
  version: "0.1.0"
---

# Spring Boot to Quarkus Migration

Modular, gate-driven migration of Spring Boot applications to Quarkus.

## Critical Rules

- **Never delete code you cannot migrate.** If you cannot fully migrate a piece of code, leave the original in place with a `// TODO: Migration required — <reason>` comment explaining what needs to change and why. This applies to:
    - Methods, classes, or annotations you don't know how to convert
    - Spring-specific patterns without a clear Quarkus equivalent
    - Configuration or wiring code whose purpose is unclear
      If you must remove code (e.g., a Spring-only base class), document what was removed and why in a `// REMOVED:` comment at the same location.
- **Don't break the build.** Run `mvn clean compile -DskipTests` after each phase. Never move to the next phase with a broken build.
- **Document every decision.** When choosing between migration approaches, explain the trade-off to the user.
- **No silent changes.** Every file modification must be intentional and traceable. If a check fails after a phase, diagnose and fix — don't skip the check or delete the failing code.

## Reference Files

Load the relevant reference file when working on a module:

| Reference | Use during |
|---|---|
| [references/dependency-map.md](references/dependency-map.md) | Build module: dependency and plugin mapping |
| [references/annotation-map.md](references/annotation-map.md) | Code module: annotation, DI, REST, Data, Security migration |
| [references/config-map.md](references/config-map.md) | Build module: configuration property migration |


## Step 1: Analyze & Choose Strategy

Scan the application to understand what needs to migrate:

- **Build system**: Read `pom.xml` — Spring Boot version, starters, plugins
- **Java code**: Search for Spring annotations (DI, REST, Data, Security, Scheduling)
- **Configuration**: Read `application.properties`/`application.yml`, check for profiles
- **UI / View layer**: Check for Thymeleaf/JSP templates, static resources, Model+View patterns
- **Tests**: Check for `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest`

Present a summary table with area, findings, and complexity. Then ask the user to choose a strategy:

- **Spring compat** (recommended): Use `quarkus-spring-web`, `quarkus-spring-data-jpa`, etc. Minimal code changes.
- **Native Quarkus**: Replace all Spring annotations with JAX-RS/CDI. More work, full Quarkus experience.

**Stop here and wait for the user's response before continuing.** Do not ask about git workflow or anything else in the same message.

## Step 2: Git branch (optional)

After the user has chosen a strategy, check if the target project is a git repository. If it is, propose the git workflow:

> **Migration workflow:** Each migration run can be isolated in its own branch (`migration/run-01`, `migration/run-02`, ...) created from `main`. The branch will contain a single commit with all changes plus a migration report. A draft PR against `main` will be created for review — it is never merged, it serves as a permanent diff and discussion record. **Would you like to use this workflow?**

- **User accepts** → follow [modules/git.md](modules/git.md) — **Pre-migration** section. Propose the branch name and wait for confirmation before creating it.
- **User declines** → skip git management entirely, proceed with migration in the current branch.
- **Not a git repo** → inform the user, skip git management, proceed normally.

## Step 3: Execute Modules

### Decision Gate Table 

For each module, evaluate whether it applies to this project. A module executes only when its gate is **PASS**. 
Inspect the project to determine the gate result — do not rely on blind grep commands; use your understanding of the codebase.

| Module                          | Gate Check                                                                                                                | Gate Result                                                                              |
|---------------------------------|---------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------|
| [jdk](modules/jdk.md)           | JDK 21+ required                                   | **ALWAYS** -- stop migration if < 21 |
| [build](modules/build.md)       | Spring Boot parent, starters, or spring-boot-maven-plugin in pom.xml                                                      | **PASS** if Spring Boot build markers found; **SKIP** otherwise                          |
| [code](modules/code.md)         | Spring annotations in Java sources (`@Component`, `@Service`, `@Controller`, `@Repository`, `@Entity`, `@Autowired`, etc.) | **PASS** if Spring annotations found; **SKIP** otherwise                                 |
| [frontend](modules/frontend.md) | Thymeleaf/JSP templates in `templates/` or static resources in `static/`                                                  | **PASS** if view layer found; **SKIP** otherwise                                         |
| [testing](modules/testing.md)   | Spring test annotations in test sources (`@SpringBootTest`, `@WebMvcTest`, `@MockBean`)                                   | **PASS** if Spring tests found; **SKIP** otherwise                                       |
| [cleanup](modules/cleanup.md)   | Leftover Spring artifacts after all other modules                                                                          | **ALWAYS** — runs after all other modules                                                |

### Execution Protocol

```
FOR module IN [build, code, frontend, testing, cleanup]:

  1. EVALUATE — inspect the project for the gate condition
  2. DECIDE
     IF gate == ALWAYS → proceed to step 3
     IF gate == PASS   → proceed to step 3
     IF gate == SKIP   → log "Module {name}: SKIPPED — {reason}", mark checkbox, continue
  3. LOAD — read the module file and relevant reference files
  4. EXECUTE — follow the module instructions, adapting to the chosen strategy
  5. COMPILE — run `mvn clean compile -DskipTests`
     Fails → diagnose and fix before proceeding
  6. LOG — mark checkbox as done
```

### Running Individual Modules

To run a single module outside the full migration flow, read its file directly:

- "Read `modules/build.md` and execute it"
- "Run only the frontend module"
- "Re-run the cleanup module"

The module will use the current project state and the chosen strategy (if already decided). If no strategy has been chosen, the module will ask.

## Step 4: Verify the Migration

Run each check in order. A check fails = stop and fix before continuing.

| # | Check | Command | Pass criteria |
|---|-------|---------|---------------|
| 1 | **Builds** | `mvn clean package -DskipTests` | Exit code 0, no compilation errors |
| 2 | **No Spring deps** | Search `pom.xml` for `org.springframework` | Zero Spring deps (except Spring compat extensions if using that strategy) |
| 3 | **Has Quarkus** | Search `pom.xml` for `io.quarkus` | Quarkus BOM and at least one extension present |
| 4 | **Tests pass** | `mvn test` | All tests pass using `@QuarkusTest` |
| 5 | **Starts up** | `mvn quarkus:dev` | App starts, `curl http://localhost:8080/q/health` returns UP |
| 6 | **No leftover templates** | Search for Thymeleaf/JSP references | None remaining (unless intentionally kept) |

## Step 5: Migration Review (Self-Reflection)

Answer each question honestly:

1. **What migrated cleanly?** Patterns that mapped 1:1.
2. **What required manual judgment?** Non-obvious decisions made.
3. **What was left as TODO?** Every `// TODO: Migration required` comment and why.
4. **Was any code removed?** What, where, justification. Flag runtime risks.
5. **What checks failed initially?** Failures from Step 4 and how you fixed them.
6. **What's missing from the skill references?** Mappings you had to figure out.

### Migration Report

Present the review as a structured report:

```
## Migration Report: [app-name]

### Summary
- Strategy: [Full Migration / Spring Compatibility]
- Agent: Claude
- Model: [model name — e.g. claude-sonnet-4-6, check system context]
- Modules completed: [X/4]
- Checks passed: [X/6]
- Token usage: [input tokens / output tokens — check session stats]
- Estimated cost: [~$X.XX — token counts × per-model pricing from anthropic.com/pricing]

### Changes by Module
| Module | Files changed | Key changes |
|--------|--------------|-------------|
| build | pom.xml, application.properties | ... |
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

## Step 6: Commit and PR (only if git workflow was accepted)

Follow [modules/git.md](modules/git.md) — **Post-migration** section. Ask the user for confirmation before committing, and again before pushing / creating the draft PR. Do not proceed with either action without explicit user approval.
