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

Tool usage: external tools (e.g., OpenRewrite scanners, TreeSitter, CLDK parsers) may be referenced for deterministic extraction. The tools to use — along with their pinned versions — can be declared in a migration configuration file, allowing different projects or environments to specify their own
tooling without modifying the module logic. Tools can also be enabled or disabled through this configuration.

The tools to be used to scan the code source to be migrated will be discussed in a separate ADR or issue.

Gate: **ALWAYS** — runs after `prerequisite`, before `planning`.

### 3. `planning` (`modules/planning/`)

Formulates the end-to-end migration blueprint and produces a binding specification that
all transformation modules consume as their single source of truth.

Responsibilities:
- Map source frameworks to Quarkus/Jakarta equivalents based on the chosen strategy.

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

#### Feature flags and their gate bindings

The `detected_features` section of `migration-spec.yaml` is written by
`discovery` and transformation modules read them.

The table below is representative, not exhaustive. As new `code/` sub-modules are added
(e.g. for service layer, web layer, persistence, database, security, scheduling etc.)
additional flags are introduced alongside them. Every flag follows the same convention:
one boolean per detectable Spring capability, named after the Spring concern it represents.
Individual modules reference these flags both to decide whether to run (gate check) and
to guide and validate the transformations they apply.

| Flag | Type | Set to `true` when… |
|---|---|---|
| `spring_web` | bool | Source contains `@RestController` / `@Controller` or Spring MVC / WebFlux starters |
| `spring_data_jpa` | bool | Source contains Spring Data JPA repositories or `@Entity` classes |
| *(more flags…)* | bool | Added as new `code/` sub-modules are introduced |

**Fallback:** if `migration-spec.yaml` is absent or a flag is missing, the agent falls
back to a live code scan for that specific gate check and logs a warning. The spec
schema will be versioned to allow safe fallback detection; exact versioning rules are
deferred to [issue #79](https://github.com/quarkusio/skills/issues/79).


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

- `SKILL.md` and the Decision Gate Table grow by four rows; the execution protocol must be updated accordingly.
- The standalone `jdk` module is superseded by `prerequisite`; the content of
  `modules/jdk/jdk.md` is moved into `modules/prerequisite/` and all references to
  `modules/jdk/jdk.md` in `SKILL.md` and the Decision Gate Table are updated to point
  to the new location.
- The migration report currently inlined at the end of `SKILL.md` must be migrated into
  `modules/reporting/` without losing any existing fields.