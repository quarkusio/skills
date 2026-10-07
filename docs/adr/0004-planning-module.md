# Planning module and migration-spec.yaml 

- Status: proposed
- Date: 2026-09-22
- Issue: [#53](https://github.com/quarkusio/skills/issues/53)

## Context and Problem Statement

As proposed in [ADR-0002](https://github.com/quarkusio/skills/pull/77/) (not yet merged),
the `planning` module is one of the new modules with its responsibilities defined at a
high level. As proposed in [ADR-0003](https://github.com/quarkusio/skills/pull/81/) (not yet merged),
the full schema of `migration-spec.yaml` is defined there.

This ADR specifies how the planning module collects user decisions, the selective
feature flag approach, and the two-pass JDK check split between `jdk.md` and the
planning module (resolving [#71](https://github.com/quarkusio/skills/issues/71)).

The `migrate-spring-to-quarkus` skill currently has no module that captures critical
features requiring frequent lookup and user transformation preferences in interactive
mode. Strategy, Quarkus version, and Java version are resolved ad-hoc inline inside
`SKILL.md` Step 1, then the skill immediately begins executing modules. This means:

- Decisions are not machine-readable and can get lost on session interruption.
- Every module either re-scans the source independently (wasting tokens) or relies on
  implicit prompt context.
- There is no durable artifact in the target directory recording what was found and
  what was decided before transformation began.

## Decision Drivers

- **Single decision point.** Strategy, Quarkus version, Java version, persistence
  choice, messaging transport, and view technology should be asked once and recorded
  once. Downstream modules read the record rather than re-asking or re-inferring.
- **Token efficiency.** A structured scan written at the start of the run avoids each
  module independently re-reading the source project to detect the same features.
- **Traceability.** Every decision and its source (`argument | config-file | user | default`)
  must be recoverable from the target directory after the run.

## Considered Options

### Keep decisions inline in `SKILL.md` (status quo)

Strategy and version are asked as part of Step 1 and carried implicitly in prompt
context. No file is written.

Simple, no new files. But decisions are not machine-readable, every new module must
re-derive them, and there is no persistent record in the target directory.

### Dedicated `modules/planning/planning.md` module writing `migration-spec.yaml`

A new module runs after the JDK check and before the transformation modules. It scans
the source project, collects decisions from the user (or auto-selects in non-interactive
mode), and writes `migration-spec.yaml` into the target directory. All downstream
modules read from this file.

More structure up front, but produces a durable artifact that serves as the shared
contract for all current and future modules (discovery #55, reporting #59, resumable
state #40).

## Decision

Introduce `modules/planning/planning.md` that runs **ALWAYS**, after the JDK Pass 1
check passes, and writes `migration-spec.yaml` into `<target>/migration-spec.yaml`.

### JDK check split (resolves #71)

The JDK version check is split across two modules to handle the chicken-and-egg
problem: the version-specific minimum depends on the target Quarkus version, which is
only known after the planning module calls `code.quarkus.io/api/streams`.

- **Pass 1 — `modules/jdk/jdk.md`** (runs before planning, no spec yet): checks the
  absolute minimum JDK for any supported Quarkus version (**17**). Resolves the floor
  from skill argument → `.quarkus-migration.yml` → default (17). Stops the migration
  immediately if not met.

- **Pass 2 — `modules/planning/planning.md` Stage 1 Step 1** (runs inside planning,
  after the streams API call): derives the version-specific minimum JDK from the
  `javaCompatibility.versions[0]` field of the resolved Quarkus stream. Stops the
  migration if the installed JDK is below that minimum, with a suggestion to either
  upgrade the JDK or choose an older Quarkus stream. Falls back to a static table
  (3.x=17, 4.x=21) if the API is unreachable.

When `#55` introduces the prerequisite module, `jdk.md` moves there as a simple gate
alongside `maven.md`/`gradle.md`. At that point Pass 1 and Pass 2 can be consolidated.

### Gate condition

**ALWAYS** — provided `jdk.md` Pass 1 has passed. If Pass 1 fails, the migration is
aborted before planning runs.

### `migration-spec.yaml`

The planning module writes `migration-spec.yaml` into `<target>/migration-spec.yaml`.

**Downstream modules always read from `migration-spec.yaml` only.** There is always a
single place to look for the current run's decisions, regardless of how they were
originally provided.

### User decisions

The planning module collects decisions in two stages. Stage 1 is always collected
before Stage 2 questions are shown. In non-interactive mode, defaults are applied
without asking; all chosen values and their source are recorded in the `decisions`
block of `migration-spec.yaml` via per-field `*_source` fields.

**Stage 1 — always collect:**

### Decision resolution chain

Decisions are resolved in priority order:

> **`argument` → `.quarkus-migration.yml` → interactive prompt**

- **`argument`**: a value passed directly as a skill invocation argument takes highest priority.
- **`.quarkus-migration.yml`**: a pre-existing config file in the source directory is read next.
- **interactive prompt**: the user is asked only if neither of the above provides a value.

In non-interactive mode the interactive prompt step is skipped and the non-interactive default is used instead. All resolved values and their source are written to the `decisions` block in `migration-spec.yaml`, each with its own `*_source` field.

| # | Decision | Interactive | Non-interactive default |
|---|---|---|---|
| 1 | Target Quarkus version | Ask: offer API-resolved latest stable or let user specify | Resolved from `code.quarkus.io/api/streams` |
| 2 | Target Java version | Ask: offer minimum JDK required by the resolved Quarkus version or let user specify | Minimum JDK required by the resolved Quarkus version |
| 3 | Migration strategy | Ask: `full-quarkus` or `spring-compat` | `full-quarkus` |


Stage 2 collects conditional decisions (persistence strategy, REST framework, messaging transport, view technology, security approach) based on Stage 1 answers and detected features. If a condition is not met the question is skipped. The full Stage 2 decision table and detection rules are defined in [`modules/planning/planning.md`](../../skills/migrate-spring-to-quarkus/modules/planning/planning.md).

### `migration-spec.yaml` as shared contract

`migration-spec.yaml` is written by `planning` and read by all downstream modules. The full schema is as proposed in [ADR-0003](https://github.com/quarkusio/skills/pull/81/) (not yet merged). The concrete field names, enum values, and YAML structure are defined at implementation time in [`modules/planning/planning.md`](../../skills/migrate-spring-to-quarkus/modules/planning/planning.md).


### Changes to `SKILL.md`

- Step 1 is renamed **JDK Check & Planning** — it first delegates to `modules/jdk/jdk.md`
  (Pass 1), then to `modules/planning/planning.md`.
- The Decision Gate Table gains `jdk` and `planning` **ALWAYS** rows before all
  transformation modules.
- The Execution Protocol `FOR module IN [...]` list is updated to include `jdk` and
  `planning` as the first two entries.

### Planned `migration-spec.yaml` consumers

The table below documents the intended contract between the planning module and each consumer.

| Module | Fields consumed | Module status |
|---|---|---|
| `modules/jdk/jdk.md` | Pass 1: resolves absolute minimum JDK from argument / `.quarkus-migration.yml` / default (17); runs before spec exists | Existing |
| `modules/build/` | `target_technology.quarkus_version`, `target_technology.java_version` — writes to `pom.xml` / `build.gradle` | Existing |
| `modules/code/code.md` | `decisions.strategy`, `decisions.persistence` — branches between strategies | Existing |
| `modules/code/messaging.md` | `decisions.messaging_transport` — targets the correct Quarkus connector | Existing |
| `modules/frontend/frontend.md` | `decisions.view_layer` — chooses Qute vs. MyFaces vs. FreeMarker path | Existing |
| `modules/testing/testing.md` | `decisions.strategy` — adjusts test annotation replacement approach | Existing |
| `modules/cleanup/cleanup.md` | `decisions.strategy` — determines which Spring imports are intentional | Existing |
| Discovery module (#55) | Writes richer `detected_features` flags into the spec | Planned |
| Reporting module (#59) | `decisions.*`, `metadata.complexity`, feature flags — final report | Planned |

## Consequences

### Positive

- Decisions are asked once, recorded durably, and available to every module without
  re-scanning. Reduces both token consumption and non-determinism.
- The target directory contains a complete, human-readable record of every migration
  run.
- New modules can be added without touching `SKILL.md`'s decision logic — they declare the
  `detected_features` flags they depend on and read their decisions from the spec.
  Note: the Execution Protocol's hardcoded module list (`FOR module IN [...]`) in `SKILL.md`
  still requires a manual update until that list is made dynamic.
- The planning module's scan is a stepping stone: once `discovery` (#55) is
  introduced, its richer structured output replaces the planning-time scan, and
  planning becomes purely a decision-collection and spec-writing module.

### Negative

- Adds a mandatory module before all transformation work, even for trivial projects.
- The planning gate must be **ALWAYS** with no skip condition — if it is skipped or
  fails, all downstream modules lose their source of truth.
- `modules/build/build.md`, `maven.md`, and `gradle.md` must be updated to read
  version values from the spec rather than resolving them independently.
- The existing inline scan and strategy question in `SKILL.md` Step 1 must be removed
  to avoid duplication.
