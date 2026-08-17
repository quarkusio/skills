# Spring Boot to Quarkus Migration Skill

Modular, gate-driven migration of Spring Boot applications to Quarkus. Supports both Spring compatibility extensions (`quarkus-spring-*`) and full Quarkus migration paths.

## Quick Start

From your Spring Boot project directory:

```
Migrate this Spring Boot project to Quarkus
```

The skill will analyze your project, ask you to choose a strategy, and execute the migration module by module.

## Migration Strategies

| Strategy | What it does | Best for |
|---|---|---|
| **Spring compatibility** (`spring-compat`) | Uses `quarkus-spring-web`, `quarkus-spring-data-jpa`, `quarkus-spring-di`, etc. Minimal code changes. | Teams wanting a low-risk first step, reusing their existing Spring code or large codebases where a full migration isn't practical yet |
| **Full Quarkus** (`full-quarkus`) | Replaces all Spring annotations with JAX-RS, CDI, Hibernate ORM, and Panache. Full Quarkus experience. | New projects, small-to-medium apps, or teams ready to fully adopt Quarkus |

## Configuration

### Interactive (default)

If you run the skill without any configuration, it will ask you to choose a strategy interactively.

### Project config file

Add a `.quarkus-migration.yml` file to your project root to prepare the migration:

```yaml
# .quarkus-migration.yml
strategy: spring-compat   # spring-compat | full-quarkus
mode: interactive         # interactive (default) | autonomous
```

When this file is present, the skill skips the strategy prompt and uses the configured values. This is useful for:
- CI/CD pipelines or automated test runs where no human is present
- Teams that have already decided on a strategy and don't want to be asked every time
- Reproducing migrations with consistent settings

#### `mode` values

| Value | Behaviour |
|---|---|
| `interactive` (default) | Pauses at decision points and asks the user. On an unfixable file, the agent stops and waits for confirmation before continuing. |
| `autonomous` | Never pauses. Unfixable files are logged and skipped automatically. All failures are collected and surfaced in the final Migration Report. |

### Skill argument

You can also pass strategy or mode directly when invoking the skill:

```
Migrate this project to Quarkus using the compatibility migration strategy
Migrate this project to Quarkus autonomously
```

### Priority order

If multiple sources provide a value, the first match wins (applies to both `strategy` and `mode`):

1. Skill argument (highest priority)
2. `.quarkus-migration.yml` config file
3. Default / interactive prompt (fallback)

## How It Works

The skill follows a 6-step process:

1. **Analyze** — scans your project (build files, Java code, config, templates, tests)
2. **Choose strategy** — resolved from config or asked interactively
3. **Execute modules** — runs each migration module through an automatic gate system
4. **Verify** — 6 post-migration checks (builds, no Spring deps, tests pass, app starts, etc.)
5. **Review** — self-reflection report with what migrated, what didn't, and why
6. **Commit** — optional git branch + draft PR workflow

### Gate System

Each module has a gate condition that determines whether it runs:

| Module | Runs when |
|---|---|
| **jdk** | Always — stops migration if JDK < 21 |
| **build** | Spring Boot starters/plugins found in build file |
| **code** | Spring annotations found in Java sources |
| **frontend** | Thymeleaf/JSP templates or static resources found |
| **testing** | Spring test annotations found in test sources |
| **cleanup** | Always — runs after all other modules |

Modules that don't apply are automatically skipped. After each module, the project is compiled to catch errors before moving on.

## Skill Structure

```
skills/migrate-spring-to-quarkus/
├── SKILL.md                          # Main skill instructions (read by the AI agent)
├── README.md                         # This file (for humans)
├── modules/                          # Migration modules
│   ├── jdk.md                        #   JDK version check
│   ├── build.md                      #   Build file migration (dispatches to Maven or Gradle)
│   ├── build-maven.md                #   Maven-specific: pom.xml, dependencies, plugins
│   ├── build-gradle.md               #   Gradle-specific: build.gradle(.kts), plugins
│   ├── code.md                       #   Java code: annotations, DI, REST, Data, Security
│   ├── frontend.md                   #   Thymeleaf/JSP templates, static resources
│   ├── testing.md                    #   Test migration: @SpringBootTest → @QuarkusTest
│   ├── cleanup.md                    #   Remove leftover Spring artifacts
│   └── git.md                        #   Git branch and PR workflow
└── references/                       # Mapping tables loaded during migration
    ├── dependency-map.md             #   Spring → Quarkus dependency mapping
    ├── annotation-map.md             #   Spring → Quarkus annotation mapping
    └── config-map.md                 #   Spring → Quarkus config property mapping
```

## Running Individual Modules

You can run a single module without executing the full migration:

```
Run only the build module
```
```
Re-run the frontend module
```

The module will use the current project state and the chosen strategy (if already decided).

## Post-Migration Checks

After all modules complete, the skill runs 6 verification checks:

| # | Check | Pass criteria |
|---|---|---|
| 1 | Builds | `mvn clean package -DskipTests` exits 0 |
| 2 | No Spring deps | No `org.springframework` in build file (except compat extensions) |
| 3 | Has Quarkus | Quarkus BOM and at least one extension present |
| 4 | Tests pass | All tests pass with `@QuarkusTest` |
| 5 | Starts up | `mvn quarkus:dev` starts, health endpoint returns UP |
| 6 | No leftover templates | No remaining Thymeleaf/JSP references |

## Git Workflow (optional)

If your project is a git repo, the skill can isolate each migration in its own branch:

- Branch: `migration/run-01`, `migration/run-02`, ...
- Single commit with all changes + migration report
- Draft PR against `main` for review (never merged — serves as a permanent diff record)

## Related

- [Quarkus Spring compatibility guides](https://quarkus.io/guides/#spring)
- [quarkus-update skill](../quarkus-update/) — check and update your Quarkus version