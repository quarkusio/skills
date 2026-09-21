# Expand skill modules: prerequisite, discovery, planning and reporting

- Status: proposed
- Date: 2026-08-28
- Issue: [#59](https://github.com/quarkusio/skills/issues/59)

## Context and Problem Statement

The `migrate-spring-to-quarkus` skill currently organises migration into six modules:
JDK, Build, Code, Frontend, Testing, and Cleanup. As the skill evolves to keep the
user in the loop, reduce LLM token consumption, and produce richer output, four
responsibilities that today either live ad-hoc inside `SKILL.md` or are scattered
across modules have become hard to extend: environment pre-flight checks, source-app
metadata extraction, migration-strategy decisioning, and end-of-run reporting.

## Decision Drivers

- **Pre-flight environment gaps.** JDK validation is isolated in its own module, but
  Maven/Gradle availability, Docker/Podman presence, and other toolchain requirements
  have no dedicated home.
- **Token consumption.** Feeding raw source files into the LLM for every run is
  expensive and slow. Structured metadata captured once can be reused across runs and
  dramatically reduce context size.
- **User confirmation and strategy binding.** Asking the user to choose a migration
  strategy (Spring compat vs. full Quarkus) is currently a single prompt inside
  `SKILL.md`. There is no dedicated place to collect and persist all migration
  parameters, verify them with the user, and make them the single source of truth for
  downstream modules.
- **Reporting completeness.** The current migration report is produced at the end of
  `SKILL.md` with no dedicated module. This makes it hard to extend, test, or reuse
  across multiple migration runs.

## Considered Options

### Keep the current six-module structure

Expand the existing modules in-place to absorb the new concerns. Simpler to
communicate, no structural change to `SKILL.md`.

However, the modules would grow large and unfocused; environment checks, metadata
extraction, and planning logic mixed into `build.md` and `code.md` become hard to
maintain independently. The gate table and execution protocol in `SKILL.md` would have
to carry responsibilities that do not belong there.

### Introduce four dedicated modules

Add `prerequisite`, `discovery`, `planning`, and `reporting` as first-class modules
with their own folders under `modules/`. The Decision Gate Table in `SKILL.md` is
updated to include these modules, and the execution protocol runs them in the defined
sequence.

The principle remains: the gate contract (the "What") stays in `SKILL.md`; the verbose
operational mechanics (the "How") move into the module file.

## Decision

The skill adds four new modules to the `modules/` directory:

### 1. `prerequisite` (`modules/prerequisite/`)

Performs pre-flight environment and toolchain validation before any migration work
begins. It consolidates and replaces the current standalone `jdk` module.

Responsibilities:
- JDK version check (minimum derived from the target Quarkus version: 17 for Quarkus 3,
  21 for Quarkus 4).
- Maven/Gradle availability and version check.
- Container runtime detection (Docker / Podman) where relevant.
- Hard-stop semantics: if a required prerequisite fails, migration is aborted
  immediately with a clear remediation message.

Gate: **ALWAYS** — runs as the very first module.

### 2. `discovery` (`modules/discovery/`)

Analyzes the source application using deterministic scanners and metadata extractors
to produce compact, structured metadata before any LLM-driven transformation begins.

Responsibilities:
- Extract project structure, build metadata (Spring Boot version, starters, plugins),
  Java source inventory (packages, annotations, entry points), configuration files, and
  test inventory.
- Write structured metadata files under `<source>/migration-metadata/`. These files are
  reusable across migration runs against the same source project.
- Reduce LLM context size: downstream modules consume the extracted metadata instead of
  raw source files.

Tool usage: external tools (e.g., OpenRewrite scanners, TreeSitter, CLDK parsers) may be referenced
for deterministic extraction. When used, they must be pinned to a specific version.
Tools can be enabled or disabled via `migration-spec.yaml`:

If a tool is disabled or unavailable the module falls back to direct code scanning.
The scope of external tool integration will be discussed in a separate issue.

> **Gap:** how pinned tool versions are declared, the full schema of `migration-spec.yaml`,
> and where the file resides (preferably in the target directory root) are not yet defined.
> These details will be specified in [issue #79](https://github.com/quarkusio/skills/issues/79).

Gate: **ALWAYS** — runs after `prerequisite`, before `planning`.

### 3. `planning` (`modules/planning/`)

Formulates the end-to-end migration blueprint and produces a binding specification that
all transformation modules consume as their single source of truth.

Responsibilities:
- Map source frameworks to Quarkus/Jakarta equivalents based on the chosen strategy.
- In **interactive mode**: present technology decisions in structured stages (target
  Quarkus version, Java version, migration strategy, persistence/messaging patterns),
  wait for explicit user approval before proceeding.
- In **autonomous mode**: evaluate project characteristics against selection policies,
  apply built-in defaults or any overrides declared in `.quarkus-migration.yml` without
  pausing for user input. `.quarkus-migration.yml` is user-provided configuration —
  written once before the migration starts and never modified by the agent. All
  decisions and the rationale behind them are recorded in `migration-spec.yaml` for
  auditability — the difference from interactive mode is that no confirmation is
  requested, not that decisions go unrecorded.
- Populate `migration-spec.yaml` with strategy decisions and technology mappings
  (see [migration-spec.yaml](#migration-specyaml) below). `migration-spec.yaml` is
  agent-generated runtime state — created and updated during the migration; it is
  distinct from `.quarkus-migration.yml` and must not be conflated with it.

Gate: **ALWAYS** — runs after `discovery`, before the transformation modules (Build,
Code, Frontend, Testing, Cleanup).

### 4. `reporting` (`modules/reporting/`)

Aggregates execution metrics, per-module change summaries, migration status, and
unresolved issues into a consolidated, human-readable report.

The reporting instructions currently inlined at the end of `SKILL.md` are moved into
this module. This separation means the report content can grow independently — new
fields, new modules, or new validation checks — without requiring changes to `SKILL.md`
itself. 

Responsibilities:
- Produce `migration-summary.md` in the target directory.
- Report must include at minimum: migration strategy, agent name and model, modules
  completed, checks passed, token usage, estimated cost, changes by module, validation
  results, unmigrated code (TODOs), removed code, and skill improvement suggestions.
- `migration-summary.md` accumulates a timestamped entry per module per run, enabling
  **cross-run comparison** (e.g. how rules passed, token usage, cost, and TODO count
  evolved across successive runs against the same source).
- The detailed requirements for cross-run comparison — format, required fields, and
  tooling — will be discussed in a separate issue.

Gate: **ALWAYS** — runs as the last module, after `cleanup`.

## `migration-spec.yaml`

`migration-spec.yaml` is a shared artefact written and read by multiple modules — no
single module owns it. It serves as a running lookup document that accumulates insights
across the migration lifecycle:

- **`discovery`** writes source-app metadata (detected features, entities, services,
  tool configuration).
- **`planning`** appends strategy decisions and technology mappings.
- **Transformation modules** (`build`, `code`, `frontend`, `testing`, `cleanup`) append
  per-phase ledger entries and verification results as they execute.
- **`reporting`** reads the full file to produce the final summary.

The file preferably resides in the root of the target directory so it travels with the
migrated project. The exact placement, schema, and versioning are deferred to
[issue #79](https://github.com/quarkusio/skills/issues/79).

### Gate evaluation: from live code scan to spec lookup

Today the Decision Gate Table in `SKILL.md` instructs the agent to **inspect the
project** at each module boundary to decide PASS or SKIP (e.g. "scan Java sources for
Spring annotations"). This works but requires the agent to re-read source files for
every gate check, consuming tokens and producing non-deterministic results.

Once `discovery` has run, `migration-spec.yaml` holds a `detected_features` map — a
flat set of boolean flags written once by the `discovery` module. Each flag represents
a detected capability in the source application. The transformation modules read their
gate condition directly from this map instead of re-scanning source files.

This shift makes gating **deterministic** (same spec → same gate result),
**cheaper** (no re-scan at each module boundary), and **traceable** (the flag value
is recorded in the spec alongside the evidence that produced it).

#### `detected_features` flags and their gate bindings

The `detected_features` section of `migration-spec.yaml` uses the following flags.
`discovery` writes them; transformation modules read them.

The table below is representative, not exhaustive. As new `code/` sub-modules are added
(e.g. for service layer, web layer, persistence, database, security, scheduling etc.) additional flags are introduced alongside them. Every flag follows the same
convention: one boolean per detectable Spring capability, named after the Spring concern
it represents.

| Flag | Type | Set to `true` when… |
|---|---|---|
| `spring_web` | bool | Source contains `@RestController` / `@Controller` or Spring MVC / WebFlux starters |
| `spring_data_jpa` | bool | Source contains Spring Data JPA repositories or `@Entity` classes |
| `spring_security` | bool | Source contains Spring Security configuration or `@EnableWebSecurity` |
| `spring_kafka` | bool | Source contains Kafka listeners, producers, or `spring-kafka` dependency |
| `spring_rabbitmq` | bool | Source contains AMQP listeners, producers, or `spring-rabbitmq` dependency |
| `spring_jms` | bool | Source contains JMS listeners, producers, or `spring-jms` dependency |
| `spring_actuator` | bool | Source contains Spring Actuator dependency or health/info endpoints |
| `spring_cloud` | bool | Source contains Spring Cloud starters or config-client dependency |
| `spring_async` | bool | Source contains `@Async` methods or `@EnableAsync` |
| `spring_scheduled` | bool | Source contains `@Scheduled` methods or `@EnableScheduling` |
| `spring_cache` | bool | Source contains `@Cacheable` / `@CacheEvict` or a cache manager bean |
| `spring_validation` | bool | Source contains `@Valid` / `@Validated` or JSR-380 constraint annotations |
| `spring_webflux` | bool | Source contains reactive types (`Mono`, `Flux`) or WebFlux starters |
| *(more flags…)* | bool | Added as new `code/` sub-modules are introduced |

**Fallback:** if `migration-spec.yaml` is absent or a flag is missing, the agent falls
back to a live code scan for that specific gate check and logs a warning. The spec
schema will be versioned to allow safe fallback detection; exact versioning rules are
deferred to [issue #79](https://github.com/quarkusio/skills/issues/79).

#### How each transformation module maps to flags

| Module | `detected_features` expression | Gate result |
|---|---|---|
| `build` | *(unconditional — build scaffolding is always needed)* | **ALWAYS** |
| `code` | *(sub-flow — see below)* | each sub-module evaluated independently |
| `frontend` | *(no dedicated flag — `discovery` inspects `templates/` and `static/` directly and records result in a `has_frontend` flag)* | **PASS** if `has_frontend: true`; **SKIP** otherwise |
| `testing` | *(no dedicated flag — `discovery` scans test sources for `@SpringBootTest` / `@WebMvcTest` / `@MockBean` and records result in a `has_spring_tests` flag)* | **PASS** if `has_spring_tests: true`; **SKIP** otherwise |

> **Note on `build`:** the current SKILL.md gates `build` on Spring Boot build markers.
> With `migration-spec.yaml` in place, `build` runs unconditionally — if `discovery`
> confirmed this is a Spring Boot project, the build scaffold always needs migration.
> The existing PASS/SKIP logic becomes redundant.

> **Note on `code` sub-modules:** `modules/code/` is not a single file — it contains
> one file per migration concern (e.g. `weblayer-migration.md`, `service-migration.md`,
> `persistence-migration.md`, `database-migration.md`, `messaging-migration.md`, and
> others). Each sub-module file declares its own gate condition, mapped to the specific
> `detected_features` flag(s) that signal its concern is present in the source app. The
> agent iterates over all `code/` sub-modules in order, evaluating and executing (or
> skipping) each one independently. New sub-modules may be added without modifying
> anything outside `modules/code/` and the `detected_features` flag set.

> **Note on `frontend` and `testing`:** these two modules depend on path-presence
> checks rather than Spring annotation flags. `discovery` will write two additional
> boolean flags — `has_frontend` and `has_spring_tests` — to cover them. These field
> names are proposed here and subject to finalisation in
> [issue #79](https://github.com/quarkusio/skills/issues/79).

#### Execution protocol change

##### Current protocol (SKILL.md today)

```
FOR module IN [build, code, frontend, testing, cleanup]:

  1. EVALUATE — inspect the project for the gate condition
  2. DECIDE
     IF gate == ALWAYS → proceed to step 3
     IF gate == PASS   → proceed to step 3
     IF gate == SKIP   → log "Module {name}: SKIPPED — {reason}", mark checkbox, continue
  3. LOAD — read the module file and relevant reference files
  4. EXECUTE — follow the module instructions, adapting to the chosen strategy
  5. COMPILE — run the project's compile command
     Fails → diagnose and fix before proceeding
  6. LOG — mark checkbox as done
```

##### Proposed protocol (after this ADR)

Three changes are made:

1. The module list expands to include the four new modules at the correct positions.
2. A new **LOAD SPEC** step runs once before the loop, reading `migration-spec.yaml`
   into the agent's working context.
3. The **EVALUATE** step reads gate conditions from `detected_features` in the spec
   rather than scanning source files. A per-module fallback rule applies when the spec
   is absent or a flag is missing.

```
LOAD SPEC — read migration-spec.yaml from the target directory root into working context.
            If the file is absent, set spec = null and proceed; each module's EVALUATE
            step will fall back to live code scan and log a warning.

FOR module IN [prerequisite, discovery, planning,
               build, code, frontend, testing, cleanup,
               reporting]:

  1. EVALUATE — determine the gate result using the rule for this module:

       prerequisite → ALWAYS
                      (hard-stop: abort migration if any required tool is missing)

       discovery    → ALWAYS

       planning     → ALWAYS

       build        → ALWAYS
                      (discovery confirmed this is a Spring Boot project;
                       build scaffolding always needs migration)

       code         → sub-flow: for each sub-module file in modules/code/ (e.g.
                        weblayer, service, persistence, database, messaging, …):
                          if spec != null:
                            PASS if the detected_features flag(s) declared by that
                                 sub-module are true
                            SKIP otherwise
                          if spec == null:
                            fall back to live scan for that sub-module's concern;
                            log "WARN: migration-spec.yaml absent, used live scan"
                      each sub-module is evaluated, executed, and logged independently

       frontend     → if spec != null:
                        PASS if detected_features.has_frontend == true
                        SKIP otherwise
                      if spec == null:
                        fall back to live scan — inspect templates/ and static/;
                        log "WARN: migration-spec.yaml absent, used live scan"

       testing      → if spec != null:
                        PASS if detected_features.has_spring_tests == true
                        SKIP otherwise
                      if spec == null:
                        fall back to live scan — inspect test sources for
                        @SpringBootTest / @WebMvcTest / @MockBean;
                        log "WARN: migration-spec.yaml absent, used live scan"

       cleanup      → ALWAYS

       reporting    → ALWAYS

  2. DECIDE
     IF gate == ALWAYS → proceed to step 3
     IF gate == PASS   → proceed to step 3
     IF gate == SKIP   → log "Module {name}: SKIPPED — {reason}", mark checkbox, continue

  3. LOAD — read the module file and relevant reference files

  4. EXECUTE — follow the module instructions, adapting to the chosen strategy

  5. COMPILE — run the project's compile command (skip for prerequisite, discovery,
               planning, and reporting — these modules do not produce compilable output)
               Fails → diagnose and fix before proceeding

  6. LOG — mark checkbox as done; append phase entry to migration-spec.yaml
           intermediate.history
```

##### Summary of line-level changes to SKILL.md

| Location in SKILL.md | Current text | Replacement |
|---|---|---|
| Module list in FOR loop | `[build, code, frontend, testing, cleanup]` | `[prerequisite, discovery, planning, build, code, frontend, testing, cleanup, reporting]` |
| Step 1 label | `EVALUATE — inspect the project for the gate condition` | `EVALUATE — determine the gate result using the rule for this module (see gate table)` |
| Step 1 body | *(implicit: re-read source files)* | *(explicit: read from `detected_features`; fall back to live scan if spec absent)* |
| Before the FOR loop | *(nothing)* | Add `LOAD SPEC` preamble |
| Step 5 condition | *(always compile)* | Skip compile for `prerequisite`, `discovery`, `planning`, `reporting` |
| Step 6 body | `mark checkbox as done` | `mark checkbox as done; append phase entry to migration-spec.yaml intermediate.history` |

## Updated Decision Gate Table

| Module | Gate Check | Gate Result |
|---|---|---|
| `prerequisite` | Environment and toolchain requirements | **ALWAYS** — abort migration if a hard requirement fails |
| `discovery` | Source-app metadata extraction | **ALWAYS** |
| `planning` | Strategy and spec generation, optional user confirmation | **ALWAYS** |
| `build` | Project confirmed as Spring Boot by `discovery` | **ALWAYS** |
| `code` → `weblayer` | `detected_features` flag(s) for web layer presence | **PASS** / **SKIP** per sub-module gate |
| `code` → `service` | `detected_features` flag(s) for service layer presence | **PASS** / **SKIP** per sub-module gate |
| `code` → `persistence` | `detected_features` flag(s) for persistence layer presence | **PASS** / **SKIP** per sub-module gate |
| `code` → `database` | `detected_features` flag(s) for direct database access presence | **PASS** / **SKIP** per sub-module gate |
| `code` → `messaging` | `detected_features` flag(s) for messaging presence | **PASS** / **SKIP** per sub-module gate |
| `code` → *(further sub-modules)* | `detected_features` flag(s) declared by each new sub-module | **PASS** / **SKIP** per sub-module gate |
| `frontend` | `detected_features.has_frontend` | **PASS** if `true`; **SKIP** otherwise |
| `testing` | `detected_features.has_spring_tests` | **PASS** if `true`; **SKIP** otherwise |
| `cleanup` | Leftover Spring artifacts after all other modules | **ALWAYS** |
| `reporting` | End-of-run metrics and summary | **ALWAYS** |

## Consequences

Positives:

- Environment issues are caught before a single token is spent on transformation.
- Structured metadata produced by `discovery` can be reused across multiple runs
  against the same source, reducing token consumption and execution time.
- `planning` gives users a clear, auditable record of every migration decision before
  transformation begins; in autonomous mode it provides the same record without blocking.
- `reporting` becomes an independently testable module; the existing tests framework
  already captures tokens and cost per run, enabling coverage comparisons across runs.

Negatives:

- `SKILL.md` and the Decision Gate Table grow by four rows; the execution protocol must
  be updated accordingly.
- The standalone `jdk` module is superseded by `prerequisite`; the content of
  `modules/jdk/jdk.md` is moved into `modules/prerequisite/` and all references to
  `modules/jdk/jdk.md` in `SKILL.md` and the Decision Gate Table are updated to point
  to the new location.
- The migration report currently inlined at the end of `SKILL.md` must be migrated into
  `modules/reporting/` without losing any existing fields.