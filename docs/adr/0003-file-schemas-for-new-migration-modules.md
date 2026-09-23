# File schemas for new migration modules

- Status: proposed
- Date: 2026-07-25
- Issue: [#79](https://github.com/quarkusio/skills/issues/79)

## Context and Problem Statement

[ADR-0002](0002-expand-modules-prerequisite-discovery-planning-reporting.md) introduced
four new modules — `prerequisite`, `discovery`, `planning`, and `reporting` — and
explicitly deferred the question of *what files each module reads and writes* to this
issue:

> "The exact placement, schema, and versioning are deferred to issue #79."
> — ADR-0002, `migration-spec.yaml` section

Before agents can be authored or updated for those modules, three questions need a
precise, reviewable answer:

1. **Which files** does each module produce or consume?
2. **Where** do those files live relative to the source and target project roots?
3. **What is the full schema** of every new or extended file?

### Directory naming convention

Two sibling directories are used throughout this document:

- **`<source-name>/`** — the original Spring project root
- **`<source-name>-quarkus/`** — the Quarkus migration target directory

The target directory name is always the source directory name with `-quarkus` appended.

---

## Decision Drivers

- **Agent interoperability.** Multiple modules share files; a shared schema prevents
  one module from writing a field that another cannot read.
- **Gate determinism.** ADR-0002 noted that gate checks currently re-scan source files
  at every module boundary. A fully-specified `migration-spec.yaml` schema is the
  prerequisite for moving gate evaluation from live code scan to spec lookup.
- **Token efficiency.** The `discovery` module produces compact metadata files so
  downstream modules consume structured data rather than raw source files.
- **Resumability.** `migration-context.json` lets an interrupted migration restart at
  the correct module; it must have a stable schema.
- **Observability.** Per-module reports and the consolidated `migration-summary.md`
  need agreed formats so tooling can aggregate results across runs.
- **Extensibility.** Modules may write additional metadata into `migration-metadata/`
  as they execute. The `migration-metadata/` directory is a shared workspace; any
  module can place structured files there provided the filename does not collide with
  an existing schema'd file.

---

## Considered Options


### 1. Annotated prose schemas in one ADR

This ADR includes annotated YAML/JSON examples that serve as the schema for each file. All contracts are defined here, no external schema files or validation tools required.

---

## Decision

### File ownership overview

| File | Written by | Read by |
|---|---|---|
| `<source-name>/migration-metadata/repo-metadata.json` | `discovery` | `planning`, validators |
| `<source-name>/migration-metadata/dependency-analysis.yaml` | `discovery` | `planning` |
| `<source-name>/migration-metadata/code-metadata.yaml` | `discovery` / validator CLI | persistence, service, web validators |
| `<source-name>-quarkus/migration-spec.yaml` | `planning`; appended by all modules | all transformation modules, `reporting` |
| `<source-name>-quarkus/migration-metadata/migration-context.json` | orchestrator; updated every module | all modules |
| `<source-name>-quarkus/migration-metadata/code-metadata.yaml` | validator CLI (target copy) | persistence, service, web validators |
| `<source-name>-quarkus/migration-reports/<module-name>-report.json` | each transformation module | `reporting` |
| `<source-name>-quarkus/migration-metadata/testlogs.txt` | `testing` / `validation` (append) | `reporting` |
| `<source-name>-quarkus/migration-summary.md` | `reporting` | human reviewer |

> Any module may also write additional structured files under
> `<source-name>/migration-metadata/` or `<source-name>-quarkus/migration-metadata/`
> as part of its work. Filenames for such files must be documented in the module's own
> `.md` file and must not collide with the schema'd files listed above.

> **Source of truth for overlapping data**: `repo-metadata.json`, `dependency-analysis.yaml`,
> and `code-metadata.yaml` are intermediate artifacts written by `discovery` and consumed
> by `planning`. Once `planning` has written `migration-spec.yaml`, that file becomes the
> authoritative source of truth for all subsequent modules. If the same field (e.g.
> `detected_features`, `database`, `messaging`) appears in both `repo-metadata.json` and
> `migration-spec.yaml` and they diverge, `migration-spec.yaml` governs.

---

### 1. `<source-name>/migration-metadata/repo-metadata.json`

**Written by**: `discovery` module
**Read by**: `planning` module, validators

Summarises the source project's components so that downstream modules do not need to
rescan source code.

```jsonc
{
  "schema_version": "1.0",            // increment on breaking field changes
  "project": {
    "name":                "<string>",  // pom.xml / build.gradle artifactId
    "type":                "Spring Boot",
    "spring_boot_version": "<string>",  // e.g. "3.3.0"
    "java_version":        "<string>",  // source compiler level, e.g. "17"
    "build_tool":          "maven | gradle",
    "config_type":         "annotation | xml | mixed"
  },

  // Boolean feature flags — drive gate checks and extension selection.
  // All flags MUST be true or false; never null.
  // This is not an exhaustive list; new flags are added as more apps and
  // technologies are encountered. All unknown flags default to false when absent.
  // If this set grows too large, consider migrating to a flat string list instead.
  "detected_features": {
    "spring_web":        "<boolean>",
    "spring_data_jpa":   "<boolean>",
    "spring_security":   "<boolean>",
    "spring_kafka":      "<boolean>",
    "spring_rabbitmq":   "<boolean>",
    "spring_jms":        "<boolean>",
    "spring_actuator":   "<boolean>",
    "spring_cloud":      "<boolean>",
    "spring_async":      "<boolean>",
    "spring_scheduled":  "<boolean>",
    "spring_cache":      "<boolean>",
    "spring_validation": "<boolean>",
    "spring_thymeleaf":  "<boolean>",
    "spring_webflux":    "<boolean>",
    "has_frontend":      "<boolean>",
    "has_spring_tests":  "<boolean>"
  },

  "components": {
    "controllers": [
      {
        "name":      "<string>",                     // simple class name
        "path":      "<string>",                     // relative to <source-name>/
        "type":      "Controller | RestController",
        "endpoints": "<integer>"
      }
    ],
    "services": [
      { "name": "<string>", "path": "<string>", "type": "@Service | @Component" }
    ],
    "repositories": [
      {
        "name":   "<string>",
        "path":   "<string>",
        "type":   "JpaRepository | CrudRepository | Repository | interface",
        "entity": "<string>"   // managed entity simple name
      }
    ],
    "entities": [
      { "name": "<string>", "path": "<string>", "table": "<string>" }
    ],
    "configurations": [
      { "name": "<string>", "path": "<string>", "type": "Configuration | ..." }
    ],
    "components": [
      { "name": "<string>", "path": "<string>", "type": "Component | Formatter | Validator | ..." }
    ]
  },

  "database": {
    "type":              "<string>",    // common values; not exhaustive: h2 | postgresql | mysql | mariadb | oracle | mssql | ...
    "driver":            "<string>",
    "supports_multiple": "<boolean>",
    "alternatives":      ["<string>"], // profile-activated alternatives
    "init_mode":         "script | always | never | embedded | ...",  // common values; not exhaustive
    "schema_location":   "<string | null>",
    "data_location":     "<string | null>"
  },

  "messaging": { "provider": "kafka | rabbitmq | jms | null" },  // common values; not exhaustive

  "view_technology": {
    "type":              "thymeleaf | jsp | jsf | freemarker | none",  // common values; not exhaustive
    "template_count":    "<integer>",
    "template_location": "<string | null>"
  },

  // Aggregate counts — must match the lengths of the component arrays above
  "statistics": {
    "total_java_files":    "<integer>",
    "controllers":         "<integer>",
    "services":            "<integer>",
    "repositories":        "<integer>",
    "entities":            "<integer>",
    "configurations":      "<integer>",
    "components":          "<integer>",
    "thymeleaf_templates": "<integer>",  // 0 when spring_thymeleaf = false
    "test_files":          "<integer>"
  }
}
```

**Constraints**: Written once by `discovery`; transformation modules are read-only consumers.

---

### 2. `<source-name>/migration-metadata/dependency-analysis.yaml`

**Written by**: `discovery` module
**Read by**: `planning` module

Compact dependency mapping that lets `planning` choose Quarkus extensions without
re-parsing `pom.xml` or `build.gradle`.

```yaml
build_tool: maven | gradle
java_version: "<string>"           # source compiler level
spring_boot_version: "<string>"

# Spring starters and libs detected in the source build file
spring_dependencies:
  - <string>                       # e.g. spring-boot-starter-web

# Recommended Quarkus extensions — left empty ([]) by discovery; filled by planning
quarkus_extensions:
  - <string>                       # e.g. quarkus-rest-jackson

database:
  primary: h2 | postgresql | mysql | mariadb | oracle | mssql  # common values; not exhaustive
  drivers:
    - <string>                     # JDBC driver class
  products:
    - <string>                     # human-readable product name

messaging:
  provider: kafka | rabbitmq | jms | null  # common values; not exhaustive

view_technology:
  spring:             thymeleaf | jsp | jsf | freemarker | none  # common values; not exhaustive
  quarkus:            qute | myfaces | none                      # common values; not exhaustive
  migration_strategy: migrate_to_qute | maintain_jsf_myfaces | none  # common values; not exhaustive
  reason: "<string | null>"        # e.g. "Thymeleaf not supported in Quarkus"

# Non-starter runtime dependencies that need explicit mapping
additional_dependencies:
  - <string>

notes:
  - <string>
```

**Constraints**: `quarkus_extensions` must be `[]` when written by `discovery`;
`planning` fills it in. `view_technology.migration_strategy` must be `none` when
`view_technology.spring` is `none`.

---

### 3. `<source-name>/migration-metadata/code-metadata.yaml` (and target copy)

**Written by**: `discovery` module or the Java validator CLI
(`migration-validator.jar extract metadata <project-root> -o code-metadata.yaml`)
**Read by**: persistence, service, web, and UI validators
(validators as defined in [#39](https://github.com/quarkusio/skills/issues/39)

Deep structural extract of all Java source files. An identical copy is generated for
the target at `<source-name>-quarkus/migration-metadata/code-metadata.yaml`;
validators diff the two copies to verify migration completeness.

```yaml
# Schema version — increment on breaking structural changes
schema_version: "1.0"
# Single-element YAML sequence — envelope allows future multi-module support.
- entities:
    - original_file:        "<string>"    # relative to project root
      package_name:         "<string>"
      class_name:           "<string>"
      extends:              ["<string>"]  # simple class names
      implements:           ["<string>"]
      table_name:           "<string | null>"
      indexes:              []            # reserved; empty when none
      id_field:             "<string | null>"
      id_generation:        "<string | null>"   # AUTO | SEQUENCE | TABLE | IDENTITY
      composite_id:         null                # reserved
      inheritance_strategy: "<string | null>"   # SINGLE_TABLE | JOINED | TABLE_PER_CLASS
      version_field:        "<string | null>"
      named_queries:        []
      soft_delete_flag:     "<boolean>"
      relationships:
        - type:            OneToOne | OneToMany | ManyToOne | ManyToMany
          collection_type: List | Set | null
          target_entity:   "<string>"    # fully-qualified class name
          mapped_by:       "<string | null>"
          fetch:           EAGER | LAZY | null
          cascade:         ["<string>"]  # PERSIST | MERGE | REMOVE | ALL | ...
          column:          "<string | null>"
      fields:
        - name:        "<string>"
          type:        "<string>"        # Java type, e.g. "String", "LocalDate"
          nullable:    "<boolean>"
          unique:      "<boolean>"
          column:      "<string | null>"
          annotations: ["<string>"]     # e.g. ["@NotEmpty", "@Size(max=30)"]
          transient:   "<boolean>"
      annotations: ["<string>"]         # class-level annotations

  repositories:
    - original_file:           "<string>"
      package_name:            "<string>"
      class_name:              "<string>"
      uses_entity_manager:     "<boolean>"
      autowired_dependencies:  ["<string>"]
      transaction_management:
        type:                     "<string>"   # SPRING | JPA | NONE
        user_transaction:         null         # reserved
        has_manual_transactions:  "<boolean>"
        manual_transaction_calls: ["<string>"]
        transaction_attributes:   ["<string>"]
      managed_entities:  ["<string>"]          # simple class names
      entity_operations: {}                    # reserved; empty object
      operations:        []                    # reserved

  rest:
    root_path: "<string | null>"
    apis:
      - path:         "<string>"
        file:         "<string>"
        class_name:   "<string>"
        annotations:  ["<string>"]
        injected_dependencies: ["<string>"]
        context_injections:    ["<string>"]
        consumes: ["<string>"]   # MIME types; empty list when not declared
        produces: ["<string>"]
        operations:
          - http_method: GET | POST | PUT | DELETE | PATCH
            path:        "<string>"
            method_name: "<string>"
            parameters:  ["<string>"]
            return_type: "<string>"

  servlets:          []   # reserved; populated for servlet-based apps only
  exception_mappers: []

  project_config:
    dependencies:
      - group_id:    "<string>"
        artifact_id: "<string>"
        version:     "<string | null>"
    persistence_config:
      persistence_xml_exists: "<boolean>"
      properties:
        - key:   "<string>"
          value: "<string>"
      datasource_url:      "<string | null>"
      datasource_driver:   "<string | null>"
      datasource_username: "<string | null>"
      hibernate_dialect:   "<string | null>"
      hibernate_ddl_auto:  "<string>"    # create-drop | validate | update | none
      hibernate_show_sql:  "<string | null>"
      jta_data_source:     "<string | null>"
      transaction_type:    "<string | null>"   # RESOURCE_LOCAL | JTA
    javax_persistence_import_files: ["<string>"]
    server_datasources: []

  datasource_usages:     []   # reserved
  entity_manager_usages: []   # reserved
```

---

### 4. `<source-name>-quarkus/migration-spec.yaml`

**Written by**: `planning` (creates and populates); appended by every transformation module
**Read by**: all transformation modules, `reporting`

The binding contract between all modules. `planning` writes the initial structure after
`discovery` completes; transformation modules append their ledger entries and
verification results under `transformations` and `intermediate.history` as they
execute. `reporting` reads the full file to produce `migration-summary.md`.

```yaml
project:
  name:        "<string>"
  description: "<string>"
  source_path: "<string>"
  target_path: "<string>"

metadata:
  application:    "<string>"
  version:        "<string>"
  complexity:     "low | medium | high | very_high"
  generatedAt:    "<ISO-8601>"
  schema_version: "1.0"

execution:
  mode:            "interactive | autonomous"
  mode_source:     "argument | config-file | default"
  strategy_source: "argument | config-file | ask | agent-selected"

source_technology:
  framework:           "Spring Boot"
  spring_boot_version: "<string>"
  java_version:        "<string>"
  build_tool:          "maven | gradle"
  base_package:        "<string>"
  database:            "<string | null>"
  messaging:           "<string | null>"
  web_framework:       "<string | null>"
  persistence:         "<string | null>"
  security:            "<string | null>"
  source_repo:         "<absolute-path>"

target_technology:
  runtime:            "Quarkus"
  quarkus_version:    "<string>"
  java_version:       "<string>"
  build_tool:         "maven | gradle"
  database:           "<string | null>"
  messaging:          "<string | null>"
  web_framework:      "<string | null>"
  persistence:        "<string | null>"
  target_repo:        "<absolute-path>"
  quarkus_extensions: ["<string>"]

# Not exhaustive — new flags are added as more apps and technologies are encountered.
# All unknown flags default to false when absent.
# If this set grows too large, consider migrating to a flat string list instead.
detected_features:
  spring_web:        "<boolean>"
  spring_data_jpa:   "<boolean>"
  spring_security:   "<boolean>"
  spring_kafka:      "<boolean>"
  spring_rabbitmq:   "<boolean>"
  spring_jms:        "<boolean>"
  spring_actuator:   "<boolean>"
  spring_cloud:      "<boolean>"
  spring_async:      "<boolean>"
  spring_scheduled:  "<boolean>"
  spring_cache:      "<boolean>"
  spring_validation: "<boolean>"
  spring_thymeleaf:  "<boolean>"
  spring_webflux:    "<boolean>"
  has_frontend:      "<boolean>"
  has_spring_tests:  "<boolean>"

database:
  product:     "postgresql | mysql | h2 | mariadb | oracle"  # common values; not exhaustive
  driver:      "<string>"
  url_pattern: "<string | null>"

entities:
  - name:          "<string>"
    source:        "<string>"
    table:         "<string>"
    pk_type:       "Long | String | UUID"
    relationships: ["<string>"]
    notes:         "<string | null>"

services:
  - name:         "<string>"
    source:       "<string>"
    type:         "@Service | @Component"
    target:       "<string>"
    target_scope: "@ApplicationScoped"
    notes:        "<string | null>"

repositories:
  - name:           "<string>"
    source:         "<string>"
    type:           "JpaRepository | CrudRepository | ..."
    entity:         "<string>"
    target_type:    "PanacheRepository | PanacheEntity"
    custom_methods: ["<string>"]
    notes:          "<string | null>"

messaging_listeners:
  - name:              "<string>"
    source:            "<string>"
    type:              "@KafkaListener | @RabbitListener | @JmsListener"
    topic:             "<string>"
    target_annotation: "@Incoming(\"<channel>\")"
    channel:           "<string>"

messaging_producers:
  - name:        "<string>"
    source:      "<string>"
    type:        "KafkaTemplate | RabbitTemplate | JmsTemplate"
    topic:       "<string>"
    target_type: "@Channel Emitter"

rest_controllers:
  - name:      "<string>"
    source:    "<string>"
    path:      "<string>"
    endpoints: "<integer>"
    target:    "<string>"
    notes:     "<string | null>"

configurations:
  - name:            "<string>"
    source:          "<string>"
    beans:           ["<string>"]
    target_strategy: "<string>"
    notes:           "<string | null>"

migration_strategy:
  migration_mode:      "full-migration | spring-compatibility"                                          # common values; not exhaustive
  service_layer:       "application-scoped-cdi | spring-di-compat"                                     # common values; not exhaustive
  repository_layer:    "panache-repository | hibernate-orm-standard | spring-data-compat"              # common values; not exhaustive
  rest_framework:      "quarkus-rest | resteasy-classic | vertx-web | spring-web-compat"               # common values; not exhaustive
  messaging_transport: "kafka | amqp | artemis-jms | in-memory | none"                                 # common values; not exhaustive
  security_approach:   "none | oidc | basic | jwt | oauth2 | ldap | custom | mtls | spring-security-compat"  # common values; not exhaustive
  async_strategy:      "mutiny-uni | cdi-asynchronous"                                                 # common values; not exhaustive
  persistence:         "hibernate-orm-panache | hibernate-orm"                                         # common values; not exhaustive
  view_layer:          "qute | myfaces | auto | none"                                                   # common values; not exhaustive

compat_mode:
  spring_di:              "<boolean>"
  spring_web:             "<boolean>"
  spring_data_jpa:        "<boolean>"
  spring_scheduled:       "<boolean>"
  spring_cache:           "<boolean>"
  spring_boot_properties: "<boolean>"
  spring_tx:              "<boolean>"
  spring_security:        "<boolean>"

decisions:
  - decision: "<string>"
    reason:   "<string>"

skip:
  - path:   "<string>"
    reason: "<string>"

unresolved_issues:
  - module:          "<string>"
    issue:           "<string>"
    files:           ["<string>"]
    attempted_fixes: "<integer>"
    severity:        "ERROR | WARNING"

approval_policy:
  require_user_acceptance_after_each_module: "<boolean>"
  max_compile_fix_retries_per_file:          3
  on_manual_review_required:                 "pause-and-ask | document-and-continue"

verification_rules:
  <module-name>:
    - "<string>"

transformations:
  <module-name>: []

intermediate:
  history:
    - module:         "<string>"
      run_at:         "<ISO-8601>"
      status:         "success | partial | failed"
      rules_total:    "<integer>"
      rules_passed:   "<integer>"
      rules_failed:   "<integer>"
      verification_evidence:
        - rule:     "<string>"
          passed:   "<boolean>"
          evidence: "<string>"
```

**Constraints**:
- This file is authoritative for all fields it shares with `repo-metadata.json` and
  `dependency-analysis.yaml`; if they diverge, values here govern.
- `detected_features` flags are written by `discovery` and are read-only for all subsequent modules.
- `execution.mode` and `execution.mode_source` are set by the orchestrator before any module runs and must not be changed.
- `compat_mode` flags are auto-derived by `planning` from `migration_strategy.migration_mode`; modules must not set them directly.
- `intermediate.history` is append-only — existing entries must never be modified or deleted.
- `unresolved_issues` is populated only in autonomous mode; in interactive mode the orchestrator pauses instead.

---

### 5. `<source-name>-quarkus/migration-metadata/migration-context.json`

**Written by**: orchestrator; updated by every module
**Read by**: all modules

```jsonc
{
  "generatedAt":        "<ISO-8601>",
  "sourceRepo":         "<absolute-path>",   // <source-name>/
  "targetRepo":         "<absolute-path>",   // <source-name>-quarkus/
  "migrationWorkspace": "<absolute-path>",   // same as targetRepo unless overridden
  "javaVersion":        "<string>",          // from `java -version` at startup
  "mavenVersion":       "<string>",          // from `mvn -version` at startup

  "currentModule":    "<string>",    // e.g. "persistence" — set BEFORE module starts
  "completedModules": ["<string>"],  // modules that reached SUCCESS
  "approvedModules":  ["<string>"],  // user-approved modules; always [] in autonomous mode

  // Canonical file locations — absolute paths
  "paths": {
    "repoMetadata":       "<absolute-path>",
    "dependencyAnalysis": "<absolute-path>",
    "migrationSpec":      "<absolute-path>",
    "codeMetadata":       "<absolute-path | null>"
  },

  // Per-module report paths relative to targetRepo; null until the module writes its report
  "moduleReports": {
    "discovery":     "<relative-path | null>",
    "planning":      "<relative-path | null>",
    "build":         "<relative-path | null>",
    "database":      "<relative-path | null>",
    "persistence":   "<relative-path | null>",
    "service":       "<relative-path | null>",
    "messaging":     "<relative-path | null>",
    "web":           "<relative-path | null>",
    "frontend":      "<relative-path | null>",
    "configuration": "<relative-path | null>",
    "testing":       "<relative-path | null>",
    "validation":    "<relative-path | null>"
  }
}
```

**Constraints**:
- `currentModule` is set to the next module name *before* that module's agent begins.
- A module name is added to `completedModules` only after its report JSON is written
  and the validation gate passes.
- `approvedModules` is always `[]` in autonomous mode.

---

### 6. `<source-name>-quarkus/migration-reports/<module-name>-report.json`

**Written by**: each transformation module
**Read by**: `reporting`, human reviewer

Filename convention: `<xyz-module-name>-report.json`.
Examples: `build-report.json`, `persistence-report.json`, `web-views-report.json`.

```jsonc
{
  "module":    "<string>",   // e.g. "build" or "persistence"
  "status":    "SUCCESS | PARTIAL | FAILURE | SKIPPED",
  "timestamp": "<ISO-8601>",
  "summary":   "<string>",   // one-sentence result

  // One entry per discrete action performed
  "actions_performed": [
    {
      "action":      "<string>",   // machine-readable id, e.g. "create_pom_xml"
      "description": "<string>",
      "details":     {}            // module-specific payload; may be omitted
    }
  ],

  // Omit entirely when the module does not invoke a validator
  "validation_results": {
    "validator":    "<string>",    // e.g. "ProjectSetupValidator"
    "status":       "SUCCESS | PARTIAL | FAILURE",
    "rules_total":  "<integer>",
    "rules_passed": "<integer>",
    "rules_failed": "<integer>",
    "checks":       ["<string>"]   // human-readable descriptions of passing rules
  },

  // Paths relative to <source-name>-quarkus/
  "files_created":  ["<string>"],
  "files_modified": ["<string>"],  // may be omitted when empty

  "next_module": "<string | null>", // next enabled module name; null when this is last

  "notes": ["<string>"]
}
```

**Constraints**:
- `status` must be `SKIPPED` (not `FAILURE`) when a module is disabled by its gate.
- `validation_results` may be omitted for modules with no validator.

---

### 7. `<source-name>-quarkus/migration-metadata/testlogs.txt`

**Written by**: `testing` and `validation` modules, in append mode
**Read by**: `reporting`

Plain text. Each module appends a timestamped block preceded by a separator line:

```
=== Module <name> — <ISO-8601> ===
<raw stdout / stderr from mvn or pytest>
```

The `reporting` module includes the full contents verbatim as an appendix in
`migration-summary.md`.

---

### 8. `<source-name>-quarkus/migration-summary.md`

**Written by**: `reporting` module
**Read by**: human reviewer

Markdown document. The `reporting` module **must** produce sections in the following
order. Section headings are normative; body content is generated from migration data.

#### §1 — Header

```markdown
# <Application Name> → Quarkus Migration Summary

**Migration Date**: <date>
**Source**: <Framework> <version> (Java <version>)
**Target**: Quarkus <version> (Java <version>)
**Overall Status**: <emoji> **<N>% Complete** (Backend: <N>%, Frontend: <N>%)
```

Status emoji convention: `✅` when 100%, `🚧` when partial, `❌` when failed.

#### §2 — Executive Summary

Narrative paragraph covering what is complete and what is pending, followed by two
bullet lists:

- **Key Achievements** — one bullet per completed milestone, prefixed `✅`
- **Remaining Work** — one bullet per pending item, prefixed `🚧`

#### §3 — Migration Modules

Markdown table, one row per module:

| Module | Name | Status | Details |
|--------|------|--------|---------|

Status values: `✅ Complete`, `⏭️ Skipped`, `🚧 Pending`, `❌ Failed`.

#### §4 — Component Migration Status

One sub-section per component category, using the headings below. Each heading
includes the completion ratio:

```markdown
### ✅ JPA Entities (6/6 - 100%)
### 🚧 Controllers (0/6 - 0%)
### 🚧 Templates (0/12 - 0%)   <!-- only when a view technology is detected -->
### ✅ Configuration (2/2 - 100%)
```

Each sub-section contains a Markdown table with the following columns:

| Sub-section | Table columns |
|---|---|
| Entities | Entity, Table, Status, Notes |
| Repositories | Repository, Entity, Status, Notes |
| Services | Service, Status, Notes |
| Controllers | Controller, Endpoints, Status, Notes |
| Templates | bulleted file list — no table |
| Configuration | Configuration, Status, Notes |

Each sub-section ends with a **Migration Pattern** line in bold describing the
transformation approach used, e.g.:

```markdown
**Migration Pattern**: `javax.persistence.*` → `jakarta.persistence.*`
```

#### §5 — Technology Mapping

`##` section with one `###` sub-section per technology axis. Mandatory sub-sections:
Framework, Persistence, Web Layer, Dependency Injection, Configuration. Optional
sub-sections (include only when detected in source): Caching, Messaging, Security.

Each sub-section lists `before → after` pairs as a bullet list.

#### §6 — Build & Deployment

Fenced code blocks showing build command outcomes, artifacts created under `target/`,
and running instructions (dev mode, production, Docker).

#### §7 — Database

Schema table count, data row count, JDBC URL, and the relevant
`application.properties` snippet in a fenced code block.

#### §8 — Migration Reports

Numbered list of all `migration-reports/*-report.json` files with a one-line validator
result summary for each (rules passed / total).

#### §9 — Next Steps

Three sub-sections, each containing a numbered list:

```markdown
### Immediate
### Short Term
### Long Term
```

#### §10 — Key Migration Patterns

Before / after Java code block pairs for the most important transformation patterns.
Include at minimum: entity migration, repository migration. Add caching, messaging,
and web layer patterns when those components were migrated.

#### §11 — Lessons Learned

Two sub-sections, each containing a numbered list:

```markdown
### What Went Well
### Challenges
```

#### §12 — Resources

Links to relevant Quarkus guides and migration references.

#### §13 — Conclusion

One-paragraph summary of overall migration status and readiness.

#### Footer (last three lines of file)

```markdown
**Generated by**: <agent-name>
**Date**: <date>
**Version**: 1.0
```

#### Cross-run behaviour

When `reporting` runs against a `<source-name>-quarkus/` that already has a
`migration-summary.md` (partial or re-run), the module **overwrites** the file. The
module table (§3) accurately reflects the current run state.

---

## File layout summary

```
<source-name>/
└── migration-metadata/
    ├── repo-metadata.json              §1  written by discovery
    ├── dependency-analysis.yaml        §2  written by discovery
    └── code-metadata.yaml              §3  written by discovery / validator CLI
        (modules may add further files here; names must not collide with the above)

<source-name>-quarkus/
├── migration-spec.yaml                 §4  written by planning; appended by all modules
├── migration-summary.md                §8  written by reporting
├── migration-metadata/
│   ├── migration-context.json          §5  written by orchestrator; updated by all
│   ├── code-metadata.yaml              §3  target copy written by validator CLI
│   └── testlogs.txt                    §7  appended by testing / validation
│   
│    
└── migration-reports/
    ├── build-report.json
    ├── database-report.json
    ├── persistence-report.json
    ├── service-report.json
    ├── messaging-report.json
    ├── web-report.json
    ├── web-views-report.json
    ├── configuration-report.json
    ├── testing-report.json             only when gate PASSES
    ├── compile-fix-report.json         on demand, any module
    └── validation-report.json          (all reports share the §6 schema)
```

---

## Consequences

### Positive

- Every file an agent author needs to write or consume has an explicit schema with
  field-level types, constraints, and a designated owner.
- Gate checks in `SKILL.md` and module `.md` files can be updated to read from
  `migration-spec.yaml` rather than rescanning source code, closing the gap
  identified in ADR-0002.
- The `migration-summary.md` section contract gives the `reporting` module a precise
  checklist to validate against before declaring a run complete.
- The `<source-name>-quarkus/` naming convention makes the source/target relationship
  self-evident in any directory listing.
- The open extension point in `migration-metadata/` allows modules to deposit
  additional structured data without requiring an ADR update, as long as they document
  the file in their own `.md` file and avoid name collisions.

### Negative / Trade-offs

- The schemas add surface area that must be kept in sync. Any **breaking** change
  (removing a required field, renaming a key) requires a `schema_version` bump in
  `migration-spec.yaml` and an update to this ADR.
- `code-metadata.yaml` is more verbose than the other files. Contributors adding new
  source-code patterns must extend this schema accordingly.

### Neutral

- This ADR does not dictate *how* files are generated (LLM agent vs. Java validator
  CLI vs. deterministic script). Generation mechanism is a module-level concern.
- Adding optional fields (absent in existing artefacts, defaulting to `null` or
  `false`) is **not** a breaking change and does not require an ADR update.
