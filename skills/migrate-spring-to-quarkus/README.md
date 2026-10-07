# Spring Boot to Quarkus Migration Skill

Modular, gate-driven migration of Spring Boot applications to Quarkus. Supports both Spring compatibility extensions (`quarkus-spring-*`) and full Quarkus migration paths as documented on the migration [page](https://quarkus.io/spring/migrate/).

## Quick Start

Open a terminal and from your Spring Boot project directory, launch an AI agent using the following prompt message:

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

If you run the skill without any configuration, it will ask you to choose a migration strategy and confirm the Quarkus and Java versions before starting.

### Autonomous mode

Add a `.quarkus-migration.yml` file to your project root to pre-configure the migration:

```yaml
# .quarkus-migration.yml

# Migration strategy (required if you want to skip the strategy prompt)
strategy: spring-compat       # or full-quarkus

# Target Quarkus version (optional — defaults to latest stable from code.quarkus.io/api/streams)
quarkus_version: "3.15.1"

# Target Java version (optional — defaults to minimum JDK required by the resolved Quarkus version)
java_version: "17"

# Run mode (optional — omit for interactive)
mode: non-interactive         # skips all prompts and applies defaults for any unset fields
```

Any field present in this file is used directly without prompting the user. Fields not present are still asked interactively (unless `mode: non-interactive` is set, in which case defaults are applied for everything).

This is useful for:
- CI/CD pipelines or automated test runs where no human is present
- Teams that have already decided on a strategy and don't want to be asked every time
- Reproducing migrations with consistent settings

### Skill argument

You can also pass the strategy directly when invoking the skill:

```
Migrate this project to Quarkus using the compatibility migration strategy
```

### Priority order

If multiple sources provide a value for the same decision, the first match wins:

1. Skill argument (highest priority)
2. `.quarkus-migration.yml` config file
3. Interactive prompt (fallback)

## How It Works

The skill follows a 5-step process:

1. **Analyze** — scans your project (build files, Java code, config, templates, tests)
2. **Choose strategy** — resolved from config or asked interactively
3. **Execute modules** — runs each migration module through an automatic gate system
4. **Verify** — 6 post-migration checks (builds, no Spring deps, tests pass, app starts and smoke-tests.)
5. **Review** — self-reflection report with what migrated, what didn't, and why

### Gate System

Each module has a gate condition that determines whether it runs:

| Module | Runs when |
|---|---|
| **prerequisite** | Always — stops migration if JDK or build tool hard check fails |
| **planning** | Always — runs after prerequisite; generates `migration-spec.yaml` |
| **build** | Spring Boot starters/plugins found in build file |
| **code** | Spring annotations found in Java sources |
| **frontend** | Thymeleaf, JSP, FreeMarker, JSF views or static resources found |
| **testing** | Spring test annotations found in test sources |
| **cleanup** | Always — runs after all other modules |

Modules that don't apply are automatically skipped. After each module, the project is compiled to catch errors before moving on.

## Skill Structure

```
skills/migrate-spring-to-quarkus/
├── SKILL.md                          # Main skill instructions (read by the AI agent)
├── README.md                         # This file (for humans)
├── modules/                          # Migration modules
│   ├── planning/
│   │   └── planning.md               #   Scan project, collect decisions, write migration-spec.yaml
│   ├── prerequisite/
│   │   └── prerequisite.md           #   JDK version and build tool check
│   ├── build/
│   │   ├── build.md                  #   Build file migration (dispatches to Maven or Gradle)
│   │   ├── maven.md                  #   Maven-specific: pom.xml, dependencies, plugins
│   │   └── gradle.md                 #   Gradle-specific: build.gradle(.kts), plugins
│   ├── code/
│   │   ├── code.md                   #   Java code: annotations, DI, REST, Data, Security
│   │   └── messaging.md              #   Messaging migration: Kafka, RabbitMQ, JMS -> Quarkus
│   ├── frontend/
│   │   ├── frontend.md               #   View layer dispatcher & static resources
│   │   ├── thymeleaf.md              #   Thymeleaf -> Qute
│   │   ├── freemarker.md             #   FreeMarker router (Qute vs quarkus-freemarker)
│   │   ├── freemarker-qute.md        #   FreeMarker -> Qute
│   │   ├── freemarker-quarkus.md     #   FreeMarker -> quarkus-freemarker extension
│   │   ├── jsp.md                    #   JSP -> Qute
│   │   ├── jsf.md                    #   JSF router (Qute vs MyFaces)
│   │   ├── jsf-qute.md               #   JSF -> Qute
│   │   └── jsf-myfaces.md            #   JSF -> MyFaces
│   ├── testing/
│   │   └── testing.md                #   Test migration: @SpringBootTest -> @QuarkusTest
│   ├── cleanup/
│   │   └── cleanup.md                #   Remove leftover Spring artifacts
│   └── compile-fix.md                #   Retry procedure for compilation errors
└── references/                       # Mapping tables loaded during migration
    ├── dependency-map.md             #   Spring -> Quarkus dependency mapping
    ├── annotation-map.md             #   Spring -> Quarkus annotation mapping
    └── config-map.md                 #   Spring -> Quarkus config property mapping
```

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

## Related

- [Quarkus Spring compatibility guides](https://quarkus.io/guides/#spring)
- [quarkus-update skill](../quarkus-update/) — check and update your Quarkus version