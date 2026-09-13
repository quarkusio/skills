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
Tools can be enabled or disabled via configruation files `.quarkus-migration.yml`:

```yaml
discovery:
  tools:
    openrewrite_scan: true
    cldk_extractor: false
```

If a tool is disabled or unavailable the module falls back to direct code scanning.
The scope of external tool integration will be discussed in a separate issue.

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
  apply defaults or `.quarkus-migration.yml` overrides, record explicit rationales
  without blocking.
- Write `migration-spec.yaml` to the target directory root as the binding contract for
  all downstream modules.

Gate: **ALWAYS** — runs after `discovery`, before the transformation modules (Build,
Code, Frontend, Testing, Cleanup).

### 4. `reporting` (`modules/reporting/`)

Aggregates execution metrics, per-module change summaries, migration status, and
unresolved issues into a consolidated, human-readable report.

Responsibilities:
- Produce `migration-summary.md` in the target directory.
- Report must include at minimum: migration strategy, agent name and model, modules
  completed, checks passed, token usage, estimated cost, changes by module, validation
  results, unmigrated code (TODOs), removed code, and skill improvement suggestions.
- The format is compatible with the existing migration report defined in `SKILL.md` so
  that cross-run comparison (tokens, cost, coverage) remains possible.

Gate: **ALWAYS** — runs as the last module, after `cleanup`.

## Updated Decision Gate Table

| Module | Gate Check | Gate Result |
|---|---|---|
| `prerequisite` | Environment and toolchain requirements | **ALWAYS** — abort migration if a hard requirement fails |
| `discovery` | Source-app metadata extraction | **ALWAYS** |
| `planning` | Strategy and spec generation, optional user confirmation | **ALWAYS** |
| `build` | Spring Boot build markers present | **PASS** if found; **SKIP** otherwise |
| `code` | Spring annotations in Java sources | **PASS** if found; **SKIP** otherwise |
| `frontend` | Thymeleaf/JSP templates or static resources | **PASS** if found; **SKIP** otherwise |
| `testing` | Spring test annotations in test sources | **PASS** if found; **SKIP** otherwise |
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