# Deterministic validators for migration correctness

- Status: proposed
- Date: 2026-09-16

## Context and Problem Statement

The `migrate-spring-to-quarkus` skill verifies migration results through its gate system: the project compiles, tests pass, the app starts, and endpoints respond. These checks confirm the result *works*, but they cannot detect partial migrations. A project that compiles and starts may still be missing migrated entities, REST endpoints, messaging channels, or configuration properties.

A set of 8 Java validators has been contributed as a CLI jar (`migration-validator-1.0.0.jar`) that performs deterministic, non-LLM verification of migration *completeness*. They extract metadata from both the source (Spring) and target (Quarkus) projects, then compare them against hardcoded rules to verify that every Spring component has a Quarkus counterpart.

The validators are complementary to the existing gate checks: the gates verify "does it work," the validators verify "is it complete." Both are needed.

The jar is a self-contained CLI with two main commands:
- `extract metadata`: parses Java source code and generates structured metadata files (currently depends on CodeAnalyzer/CLDK)
- `validate <subcommand>`: compares extracted metadata from source vs target against validation rules

The 8 validators cover: project-setup, database, persistence, services, messaging, REST, UI, and configuration.

This ADR decides how to adopt these validators and establishes the maintenance and contribution model for them.

## Decision Drivers

- **Partial migrations go undetected.** A project can compile and start while entire domains (entities, endpoints, messaging channels) remain unmigrated. The gate system has no way to catch this today.
- **Determinism matters.** Letting the LLM judge completeness is non-repeatable and unreliable for regression testing. The validators use pure Java with hardcoded rules, producing the same result every time.
- **Build once, use many times.** The jar is compiled once (`mvn clean package`) and then invoked during migration runs. It is not rebuilt per migration. This makes the build step a one-time cost for contributors, not a per-run overhead.
- **Runtime network dependency.** The validators' `extract metadata` command currently depends on CodeAnalyzer (CLDK, from IBM's `codellm-devkit/codeanalyzer-java`), a jar downloaded from GitHub at runtime and stored in `~/.migration-validator/tools/`. This fails in offline and CI environments and the version is hardcoded. The implications of this dependency need to be understood before the validators can be considered production-ready.
- **Alignment with two-directory model.** The validators were designed for source-to-target comparison, which matches the adopted two-directory migration model (ADR 0001).
- **Maintenance burden.** Java code living inside a skill repository requires a build step, dependency management, and test infrastructure that the rest of the repo (pure Markdown) does not need.

## Decision

Adopt the deterministic validators. The validator source code lives in this repository under `validators/java/` and is maintained alongside the skill.

### Runtime dependency on CodeAnalyzer (CLDK)

The `extract metadata` command currently downloads CodeAnalyzer (CLDK) from GitHub at runtime. This means the validators that depend on metadata extraction (persistence, REST, messaging) will not work in environments without internet access.

This is an open concern. CodeAnalyzer is an IBM tool that uses JavaParser internally for Java source analysis. We need to understand whether this runtime dependency is acceptable, whether it can be replaced by a compile-time dependency (e.g., using JavaParser directly as a Maven dependency), or whether there are other approaches to avoid the network requirement. This should be discussed with the validator maintainers before considering the validators production-ready.

Validators that do not depend on `extract metadata` (project-setup, database, services, config, UI) work fully offline.

### Integration into the Execution Protocol

A new **VALIDATE** step is added between COMPILE and LOG in the Execution Protocol. Each module invokes its relevant validators after completing its transformation:

| Module | Validators |
|---|---|
| build | `validate project-setup` |
| code (database) | `validate database` |
| code (persistence) | `extract metadata` (source + target), then `validate persistence` |
| code (services) | `validate services` |
| code (messaging) | `extract metadata` (source + target), then `validate messaging` |
| code (REST) | `extract metadata` (source + target), then `validate rest` |
| frontend | `validate ui` |
| cleanup (config) | `validate config` |

The gate table in `SKILL.md` remains the single source for skip decisions. Validators run only for modules that the gate table activates.

### Maintenance model

- **Code ownership.** The validator Java code lives in this repository and is maintained by the same contributors who maintain the skill modules. Changes to a validator and its corresponding skill instructions should be submitted together in the same PR.
- **Build and test.** The validator jar is built once with `mvn clean package` from `validators/java/`. It does not need to be rebuilt per migration run. Test classes should be added for each validator, covering at least one positive case (migration complete) and one negative case (migration incomplete).
- **Discoverability.** The validator README (`validators/java/README.md`) documents each validator: its subcommand name, what it checks, which module invokes it, and what inputs it requires. The `--help` command on the jar lists available subcommands at runtime.

## Consequences

Positives:

- Partial migrations are caught deterministically, closing a gap the gate system cannot cover.
- The two-directory model provides the source and target directories the validators need.
- Clear ownership and contribution model for validator code.
- One-time build cost, reusable across migration runs.

Negatives:

- The skill repository now contains Java code that requires a build step, adding complexity for contributors who only work on Markdown skill files.
- The CodeAnalyzer (CLDK) runtime dependency limits offline and CI usage until it is resolved.
- Coupling between jar and skill instructions means changes to validation logic often span two file types (Java and Markdown).