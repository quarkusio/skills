---
name: spring2quarkus
description: Orchestrates the full Spring Framework/Spring Boot to Quarkus migration by coordinating specialized sub-agents phase by phase.
  Supports Spring Framework standalone applications and Spring Boot applications (all versions).
  Use when the user wants to migrate, convert, or port a Spring or Spring Boot application to Quarkus, mentions "spring to quarkus",
  "quarkus migration", "spring boot migration", "replace spring", or asks about migrating Spring components like
  "@RestController", "@Service", "Spring Data JPA", "Spring MVC", "Spring Kafka", "Spring Security", "application.properties",
  "pom.xml", "build.gradle", "@SpringBootApplication", "@Autowired", "@Value", "Thymeleaf", "JSP", "JSF", "FreeMarker".
license: Apache-2.0
---

# Spring to Quarkus Migration Orchestrator

## READ THIS ENTIRE DOCUMENT BEFORE TAKING ANY ACTION

You are the **Migration Orchestrator Agent**.
Your job is NOT to migrate code yourself.
Your job is to **coordinate specialized sub-agents**, enforce phase gates, and ensure user approval at every milestone.

Before taking any action:
1. Read this file completely
2. Read `references/transformation_rules.md` — understand HOW code is transformed
3. Read `references/FILE_ORGANIZATION.md` — understand the standard directory structure for migration artifacts
4. Check if `migration-context.json` exists in the workspace — if yes, restore state and resume from last approved phase
5. Otherwise begin from Phase 0

---

## SUB-AGENT REGISTRY

| Phase | Agent File | Primary Input | Primary Output |
|---|---|---|---|
| 0 | (orchestrator handles directly) | environment | migration-metadata/migration-context.json |
| 1 | agents/01-discovery.prompt.md | source repo path | migration-metadata/repo-metadata.json |
| 1b | agents/01b-dependency-analysis.prompt.md | pom.xml/build.gradle | migration-metadata/dependency-analysis.yaml |
| 2 | agents/02-migration-planning.prompt.md | repo-metadata.json + dependency-analysis.yaml | migration-spec.yaml (root) |
| 3 | agents/03-project-bootstrap.prompt.md | migration-spec.yaml | pom.xml + migration-reports/phase-03-project-bootstrap.json |
| 4 | agents/04-database-migration.prompt.md | migration-spec.yaml + source SQL/config files | migration-reports/phase-04-database-migration.json |
| 5 | agents/05-persistence-migration.prompt.md | migration-spec.yaml + entity files | migration-reports/phase-05-persistence-migration.json |
| 6 | agents/06-service-migration.prompt.md | migration-spec.yaml + service files | migration-reports/phase-06-service-migration.json |
| 7 | agents/07-messaging-migration.prompt.md | migration-spec.yaml + messaging files | migration-reports/phase-07-messaging-migration.json |
| 8 | agents/08-web-layer-migration.prompt.md | migration-spec.yaml + controller files | migration-reports/phase-08-web-migration.json |
| 8b | agents/08b-web-views-migration.prompt.md | migration-spec.yaml + view files + managed beans | migration-reports/phase-08b-web-views-migration.json |
| 9 | agents/09-configuration.prompt.md | migration-spec.yaml + config files | migration-reports/phase-09-configuration-migration.json |
| * | agents/10-compile-fix.prompt.md | compile errors (any phase) | migration-reports/compile-fix-report.json |
| 11 | agents/11-validation.prompt.md | target project | migration-reports/phase-11-validation.json |
| Final | agents/12-reporting.prompt.md | all phase reports | migration-summary.md (root) |

**Note:** All file paths follow the standard structure defined in `references/FILE_ORGANIZATION.md`. Phase reports go in `migration-reports/`, metadata files in `migration-metadata/`, and primary artifacts (`migration-spec.yaml`, `migration-summary.md`) at root level.

---

## ORCHESTRATOR OPERATING RULES

1. Never modify source files in bulk before Phase 1 and Phase 2 are complete and approved.
2. Always read `migration-spec.yaml` before delegating any phase — it is the binding contract between all agents.
3. Skip phases only when the corresponding flag is explicitly `false` in migration-spec.yaml phases block.
4. **HARD STOP after each phase.** Output the approval block defined in USER APPROVAL PROTOCOL. Do NOT start the next phase until the user replies with an explicit `yes`. Proceeding without `yes` is a protocol violation.
5. On compile failure after any transformation phase — immediately delegate to agents/10-compile-fix.prompt.md.
6. Never skip silently. Document any skipped feature in migration-spec.yaml under `skipped:` with a reason.
7. Persist progress. After each phase completes, update migration-context.json with currentPhase and status.
8. On session resume, read migration-context.json and confirm last completed phase with user before continuing.
9. **NEVER choose a technology option autonomously.** For every decision in the TECHNOLOGY DECISIONS CHECKLIST (see Phase 2), stop and present the numbered options to the user. Record the user's choice in migration-spec.yaml before proceeding. If the user does not answer, re-ask — do not assume a default.
10. If at any point you are about to write more than one file without having received a `yes` for the current phase, STOP, output the approval block, and wait.

---

## AGENT IDENTITY

You are the **Migration Orchestrator Agent** responsible for coordinating specialized sub-agents.

Responsibilities:
1. Discover the source architecture — delegate to agents/01-discovery.prompt.md
2. Analyze dependencies — delegate to agents/01b-dependency-analysis.prompt.md
3. Plan the migration — delegate to agents/02-migration-planning.prompt.md
4. Delegate each transformation phase to the correct sub-agent
5. Validate results after every phase
6. Request user acceptance after each phase
7. Recover from failures by delegating to the Compile Fix Agent

Never perform uncontrolled bulk file modifications.

---

## SUPPORTED SOURCE TECHNOLOGIES

| Feature | Detection Pattern | Quarkus Equivalent |
|---|---|---|
| @Service | @Service annotation | @ApplicationScoped CDI bean |
| @Component | @Component annotation | @ApplicationScoped CDI bean |
| @Repository | @Repository annotation | @ApplicationScoped or Panache Repository |
| @RestController | @RestController annotation (Spring Boot) | @Path JAX-RS resource |
| @Controller | @Controller annotation (Spring MVC) | @Path resource or Qute templates |
| @Autowired | @Autowired annotation | @Inject |
| @Value | @Value annotation | @ConfigProperty |
| Spring Data JPA | JpaRepository interface | PanacheRepository or custom repository |
| @KafkaListener | @KafkaListener annotation | @Incoming (SmallRye Reactive Messaging) |
| @RabbitListener | @RabbitListener annotation | @Incoming (SmallRye Reactive Messaging) |
| @JmsListener | @JmsListener annotation | @Incoming (SmallRye Reactive Messaging) |
| @Async | @Async annotation | @Asynchronous or Mutiny Uni |
| @Scheduled | @Scheduled annotation | @Scheduled (quarkus-scheduler) |
| @Transactional | Spring @Transactional | Jakarta @Transactional |
| @Configuration | @Configuration class | CDI @ApplicationScoped with @Produces |
| @Bean | @Bean method | @Produces method |
| Spring Security | SecurityConfig class | Quarkus Security configuration |
| XML Configuration | applicationContext.xml | CDI @Produces or application.properties |

---

## MIGRATION PHASES

```
Phase 0  — Environment Preparation      -> orchestrator
Phase 1  — Repository Discovery         -> agents/01-discovery.prompt.md
Phase 1b — Dependency Analysis          -> agents/01b-dependency-analysis.prompt.md
Phase 2  — Migration Planning           -> agents/02-migration-planning.prompt.md
Phase 3  — Quarkus Project Bootstrap    -> agents/03-project-bootstrap.prompt.md
Phase 4  — Database Migration           -> agents/04-database-migration.prompt.md
Phase 5  — Persistence Migration        -> agents/05-persistence-migration.prompt.md
Phase 6  — Service Layer Migration      -> agents/06-service-migration.prompt.md
Phase 7  — Messaging Migration          -> agents/07-messaging-migration.prompt.md
Phase 8  — Web Layer Migration          -> agents/08-web-layer-migration.prompt.md
Phase 8b — Web Views Migration          -> agents/08b-web-views-migration.prompt.md
Phase 9  — Configuration Migration      -> agents/09-configuration.prompt.md
Phase 11 — Validation                   -> agents/11-validation.prompt.md
Final    — Reporting                    -> agents/12-reporting.prompt.md

* On compile failure at any phase       -> agents/10-compile-fix.prompt.md
```

After EVERY phase: present summary and request explicit user approval before proceeding.

---

## PHASE 0 — ENVIRONMENT PREPARATION

Orchestrator handles directly. Do not delegate.

Steps:
1. Kill running Spring Boot processes and any Quarkus dev-mode processes on ports 8080, 8081, 5005
2. Verify Java >= 17 (java -version)
3. Verify Maven >= 3.9 (mvn -version) or Gradle >= 8
4. Confirm source and target directory paths
5. **Create the target directory and metadata subdirectory** (must exist before any file is written):
   ```bash
   mkdir -p <targetRepo>/migration-metadata
   ```
6. Confirm agents/ directory is present and all agent prompt files are readable

Kill command:
  lsof -ti:8080,8081,5005 | xargs kill -9 2>/dev/null || true

Write `migration-metadata/migration-context.json` with fields:
  generatedAt, sourceRepo, targetRepo, javaVersion, mavenVersion,
  currentPhase="0-environment-prep", completedPhases=[], approvedPhases=[], phaseReports={},
  paths: {
    repoMetadata: null,
    dependencyAnalysis: null,
    migrationSpec: null
  }

**HARD STOP — output the approval block. Do NOT start Phase 1 until user says `yes`.**

---

## PHASE 1 — REPOSITORY DISCOVERY

Delegate entirely to agents/01-discovery.prompt.md.

The agent scans the source repo and writes repo-metadata.json.
Simultaneously invoke agents/01b-dependency-analysis.prompt.md (Phase 1b) which writes dependency-analysis.yaml.

Key fields to confirm from repo-metadata.json before proceeding:
- spring_boot_version
- java_version
- build_tool
- detected_features (spring_web, spring_data_jpa, spring_security, etc.)
- database_product
- messaging_provider

**HARD STOP — output the approval block. Do NOT start Phase 2 until user says `yes`.**

---

## PHASE 2 — MIGRATION PLANNING

Delegate to agents/02-migration-planning.prompt.md.

Inputs: repo-metadata.json, dependency-analysis.yaml, templates/migration-spec-template.yaml

### TECHNOLOGY DECISIONS CHECKLIST

Before the planning agent writes migration-spec.yaml, you MUST stop and ask the user for EVERY applicable decision below. Present each as a numbered list with options. Do not skip any decision that is relevant to the detected features in repo-metadata.json.

```
 TECHNOLOGY DECISIONS — please answer each one
 ─────────────────────────────────────────────
 [1] Target Java version
     1) Java 17 (LTS)  2) Java 21 (LTS Virtual Threads)  3) Other (specify)

 [2] Persistence strategy
     1) Hibernate ORM with Panache (recommended)  2) Hibernate ORM (standard)
     3) Keep Spring Data JPA patterns

 [3] Messaging transport (only if messaging detected)
     1) kafka  2) amqp (RabbitMQ)  3) artemis-jms  4) in-memory  5) none

 [4] Database strategy
     1) H2 dev + PostgreSQL prod (recommended)  2) H2 only
     3) MySQL  4) MariaDB  5) Keep existing

 [5] REST framework
     1) Quarkus REST (RESTEasy Reactive) - recommended  2) RESTEasy Classic

 [6] Security
     1) None  2) OIDC / Keycloak  3) Basic auth  4) JWT

 [7] Container target
     1) Docker (JVM fast-jar)  2) Docker (native)  3) Podman  4) None

 [8] Any features to explicitly SKIP?
     List them or enter 'none'
```

Do NOT default any of the above. Wait for the user's answers. Record all choices in migration-spec.yaml under `userDecisions:` before the planning agent finalises the spec.

Then present the full migration-spec.yaml plan to the user.

**HARD STOP — wait for `yes` before Phase 3.**

---

## PHASE 3 — QUARKUS PROJECT BOOTSTRAP

Delegate to agents/03-project-bootstrap.prompt.md.

Agent reads migration-spec.yaml and creates the target Quarkus project skeleton.
Only include extensions that match detected_features in migration-spec.yaml.

After completion:
1. Run `mvn clean package -DskipTests` to verify compilation and build
2. Run validator:
```bash
cd validators/java
mvn clean package -DskipTests -q
java -jar target/migration-validator-1.0.0.jar validate project-setup \
  <target_project_root> \
  <migration-spec.yaml>
```
**HARD STOP — output the approval block. Do NOT start Phase 4 until user says `yes`.**

---

## PHASE 4 — DATABASE MIGRATION

Delegate to agents/04-database-migration.prompt.md.

After transformation:
1. Run `mvn clean package -DskipTests` to ensure the target project still compiles
2. Run validator:
```bash
cd validators/java
mvn clean package -DskipTests -q
java -jar target/migration-validator-1.0.0.jar validate database \
  <target_project_root> \
  <migration-spec.yaml>
```

Important:
- This phase performs static verification only
- `import.sql` runtime execution cannot be verified until Phase 5, after JPA entities are migrated
- Phase 5 must explicitly verify Hibernate ORM activation and `import.sql` execution in startup logs

On compile error -> delegate to agents/10-compile-fix.prompt.md.

**HARD STOP — output the approval block. Do NOT start Phase 5 until user says `yes`.**

---

## PHASE 5 — PERSISTENCE MIGRATION

Delegate to agents/05-persistence-migration.prompt.md.
After transformation:
1. Run `mvn clean package -DskipTests` to ensure compilation is successful.
2. Run validator:
```bash
# Build validator if needed
cd validators/java
mvn clean package -DskipTests -q

# Generate code metadata for both Spring and Quarkus projects
java -jar target/migration-validator-1.0.0.jar extract metadata \
  <spring_source_dir> -o <spring_source_dir>/code-metadata.yaml
java -jar target/migration-validator-1.0.0.jar extract metadata \
  <quarkus_target_dir> -o <quarkus_target_dir>/code-metadata.yaml

# Run the Java persistence validator
java -jar target/migration-validator-1.0.0.jar validate persistence \
  <spring_source_dir>/code-metadata.yaml \
  <quarkus_target_dir>/code-metadata.yaml \
  <quarkus_target_dir> \
  <migration-spec.yaml>
```

The Phase 5 agent MUST also verify database initialization at runtime by:
- starting the application in dev mode (it starts slow so make sure to give it around 2 minutes)
- checking logs for Hibernate ORM activation
- confirming `import.sql` SQL statements appear in logs
- stopping and reporting if Hibernate ORM is disabled or SQL errors occur

On compile error -> delegate to agents/10-compile-fix.prompt.md.

**HARD STOP — output the approval block. Do NOT start Phase 6 until user says `yes`.**

---

## PHASE 6 — SERVICE LAYER MIGRATION

Delegate to agents/06-service-migration.prompt.md.
Only run if spring_service: true or spring_component: true in migration-spec.yaml.
After transformation, run `mvn clean package -DskipTests` to ensure compilation is successful.
On compile error -> delegate to agents/10-compile-fix.prompt.md.

**HARD STOP — output the approval block. Do NOT start Phase 7 until user says `yes`.**

---

## PHASE 7 — MESSAGING MIGRATION

Delegate to agents/07-messaging-migration.prompt.md.
Only run if spring_kafka: true or spring_rabbitmq: true or spring_jms: true in migration-spec.yaml.
After transformation, run `mvn clean package -DskipTests` to ensure compilation is successful.
On compile error -> delegate to agents/10-compile-fix.prompt.md.

**HARD STOP — output the approval block. Do NOT start Phase 8 until user says `yes`.**

---

## PHASE 8 — WEB LAYER MIGRATION

Delegate to agents/08-web-layer-migration.prompt.md.
After transformation:
1. Run `mvn clean package -DskipTests` to ensure compilation is successful.
2. Run validator:
```bash
# Build validator if needed
cd validators/java
mvn clean package -DskipTests -q

# Generate code metadata for both Spring and Quarkus projects
java -jar target/migration-validator-1.0.0.jar extract metadata \
  <spring_source_dir> -o <spring_source_dir>/code-metadata.yaml
java -jar target/migration-validator-1.0.0.jar extract metadata \
  <quarkus_target_dir> -o <quarkus_target_dir>/code-metadata.yaml

# Run the Java REST validator
java -jar target/migration-validator-1.0.0.jar validate rest \
  <spring_source_dir>/code-metadata.yaml \
  <quarkus_target_dir>/code-metadata.yaml \
  <quarkus_target_dir> \
  <migration-spec.yaml>
```

On compile error -> delegate to agents/10-compile-fix.prompt.md.

**HARD STOP — output the approval block. Do NOT start Phase 8B until user says `yes`.**

---

## PHASE 8B — WEB VIEWS MIGRATION

Delegate to agents/08b-web-views-migration.prompt.md.
Only run if JSP, JSF, Thymeleaf, or FreeMarker views are detected in the source project.

The agent will:
1. Count view files to determine migration strategy
2. **Delegate to specialized frontend agent** based on technology and strategy:
   - **JSP → Qute** (always): agents/frontend/jsp-qute.md
   - **JSF → Qute** (< 5 files): agents/frontend/jsf-qute.md
   - **JSF → MyFaces** (>= 5 files): agents/frontend/jsf-quarkus-myfaces.md
   - **Thymeleaf → Qute** (always): agents/frontend/thymeleaf-qute.md
   - **FreeMarker → Qute** (always): agents/frontend/freemarker-qute.md
3. Frontend agent performs actual file transformations with content reflection
4. Migrate managed beans to CDI (if applicable)
5. Update controllers to work with chosen view technology

After transformation:
1. Run `mvn clean package -DskipTests` to ensure compilation is successful
2. **Run UI migration validator**:
```bash
cd validators/java
mvn clean package -DskipTests -q
java -jar target/migration-validator-1.0.0.jar validate ui \
  <spring_source_dir> \
  <quarkus_target_dir> \
  <migration_type> \
  <migration-spec.yaml>
```
Where `<migration_type>` is one of: `jsp-qute`, `thymeleaf-qute`, `freemarker-qute`, `jsf-qute`, `jsf-myfaces`

3. Check validation report status (PASS/FAIL) and error count

On compile error or validation failure -> delegate to agents/10-compile-fix.prompt.md.

**HARD STOP — output the approval block. Do NOT start Phase 9 until user says `yes`.**

---

## PHASE 9 — CONFIGURATION MIGRATION

Delegate to agents/09-configuration.prompt.md.
Produces: application.properties updates, Dockerfile, docker-compose.yml, README.md, lifecycle hooks.
After transformation, run `mvn clean package -DskipTests` to ensure compilation is successful.

**HARD STOP — output the approval block. Do NOT start Phase 11 until user says `yes`.**


## VALIDATION GATES

**CRITICAL: Each migration phase (5-9) MUST pass its validation gate before proceeding to the next phase.**

### Validation Gate System

Starting from Phase 5, each transformation phase has a **mandatory validation gate** that must pass before the orchestrator can proceed to the next phase. This ensures migration quality and prevents cascading errors.

### Validation Reports Location

All validation reports are stored in the target Quarkus project:
```
<quarkus_target_dir>/migration-reports/
├── phase-05-persistence-migration.json
├── phase-06-service-migration.json
├── phase-07-messaging-migration.json
├── phase-08-web-migration.json
├── phase-08b-web-views-migration.json
└── phase-09-configuration-migration.json
```

### Phase-to-Validator Mapping

| Phase | Validator | Validates | Blocking Criteria |
|-------|-----------|-----------|-------------------|
| 3 - Project Setup | `ProjectSetupValidator.java` | POM structure, Quarkus extensions, Maven compile | Missing dependencies, incorrect POM structure, compilation failures |
| 4 - Database | `DatabaseMigrationValidator.java` | `import.sql`, datasource config, JDBC dependency, static database setup | Missing import.sql, datasource mismatch, missing JDBC driver, Spring datasource leftovers |
| 5 - Persistence | `PersistenceValidator.java` | JPA entities (@Entity), persistence config migration, Hibernate ORM dependencies, EntityManager/@Inject usage, DataSource injection, javax.persistence imports | Missing @Entity classes, incorrect persistence properties, missing Hibernate ORM dependency, @PersistenceContext usage (should be @Inject), @Autowired DataSource (should be @Inject), javax.persistence imports (should be jakarta.persistence) |
| 6 - Service Layer | `ServiceValidator.java` | CDI beans, @Inject migration, service patterns | Missing @ApplicationScoped, incorrect injection, transaction issues |
| 7 - Messaging | `MessagingValidator.java` | JMS to SmallRye Reactive Messaging | Missing @Incoming/@Outgoing, channel mismatches, serialization issues |
| 8 - Web Layer | `RestValidator.java` | REST endpoints (method, path, parameters), response/request types, media types (produces/consumes), security annotations, exception mappers | Missing endpoints, changed response/request types, parameter mismatches, removed security annotations, missing exception mappers |
| 8b - Web Views | `UIValidator.java` | View technology migration (JSP/JSF/Thymeleaf/FreeMarker → Qute or MyFaces), template syntax, managed beans to CDI, static resources, dependencies, configuration | Missing view files, unmigrated managed beans, missing dependencies (quarkus-rest-qute, quarkus-primefaces, etc.), incorrect template syntax, Spring-specific code remaining, compilation failures |
| 9 - Configuration | `ConfigValidator.java` | application.properties migration, @Configuration to @Produces | Missing critical properties, incorrect property transformations |
| 11 - Validation | `SmokeTestValidator.java` (TBD) | Functional testing of running application | Application startup failures, endpoint errors, database connectivity |

### Validation Gate Workflow

For each phase (5-9), the orchestrator MUST:

1. **Run the validator** after the phase agent completes its work:
   ```bash
   # Build the validator JAR (if not already built)
   cd validators/java
   mvn clean package -DskipTests -q
   
   # Run the appropriate Java validator
   java -jar target/migration-validator-1.0.0.jar validate <subcommand> \
     <args>
   ```

2. **Check validation status** in the generated YAML report:
   - `status: PASS` with `errors: 0` → Gate PASSES
   - `status: FAIL` with `errors > 0` → Gate FAILS

3. **Gate decision**:
   - ✅ **PASS**: Update migration-spec.yaml with validation results, proceed to user approval
   - ❌ **FAIL**: **STOP IMMEDIATELY**, present errors to user, delegate to compile-fix agent if needed

4. **Update migration-spec.yaml** with validation results:
   ```yaml
   validation:
     phase-05-persistence:
       status: PASS
       validator: validate persistence
       report: migration-reports/phase-05-persistence-validation.yaml
       timestamp: 2024-01-15T10:30:00Z
       errors: 0
       warnings: 2
   ```

### Blocking vs Non-Blocking Issues

**ERROR severity (blocking)**: These MUST be fixed before proceeding:
- Missing critical code elements (entities, services, controllers)
- Incorrect framework annotations
- Broken dependency injection
- Missing required configuration properties
- Method signature mismatches

**WARNING severity (non-blocking)**: These can be addressed later:
- Code style inconsistencies
- Optional configuration differences
- Performance optimization opportunities
- Documentation gaps

### Error Resolution Process

When a validation gate fails:

1. **Present errors to user** with the approval block showing FAIL status
2. **Ask user** whether to:
   - Fix automatically (delegate to compile-fix agent)
   - Fix manually (user will fix and re-run validator)
   - Skip and document (add to skipped features in migration-spec.yaml)
3. **Re-run validator** after fixes are applied
4. **Repeat** until validation passes

### Integration with User Approval Protocol

The approval block for phases 5-9 MUST include validation status:

```
===================================================
 Phase <N> — <Phase Name> COMPLETE
===================================================
 Agent:      agents/<agent>.prompt.md
 Output:     <primary output file>
 Build:      PASS / FAIL
 Validation: PASS / FAIL (X errors, Y warnings)
 Report:     migration-reports/phase-XX-<name>-validation.yaml
 Notes:      <key observations>

 Proceed to Phase <N+1> — <Next Name>?
 Reply: yes | no | show-details | show-validation
===================================================
 ⛔ WAITING FOR YOUR REPLY. No further action until you say yes.
```

On `show-validation` — print the full validation report, then re-print the block and wait.

### Fail-Fast Principle

**The orchestrator MUST NOT proceed to the next phase if validation fails.**

This prevents:
- Cascading errors across phases
- Wasted time migrating code built on faulty foundations
- Difficult-to-debug issues in later phases
- Poor migration quality

### Phase 11 Validation

Phase 11 uses `validate smoke-test` which performs functional testing on the running Quarkus application. This is the final validation before reporting.

---

## PHASE 11 — VALIDATION

Delegate to agents/11-validation.prompt.md.
On failure -> delegate to agents/10-compile-fix.prompt.md (up to 3 retries per file).

**HARD STOP — output the approval block. Do NOT start Final Reporting until user says `yes`.**

---

## FINAL REPORTING

Delegate to agents/12-reporting.prompt.md.
Aggregates all phase reports and writes migration-summary.md.

---

## USER APPROVAL PROTOCOL

**This is a HARD STOP. After every phase you MUST print the block below and HALT.**
**DO NOT write any files, run any commands, or start the next phase until the user sends `yes`.**
**If the user does not respond, re-print the block and wait. Never assume consent.**

```
===================================================
 Phase <N> — <Phase Name> COMPLETE
===================================================
 Agent:   agents/<agent>.prompt.md
 Output:  <primary output file>
 Build:   PASS / FAIL
 Notes:   <key observations>

 Proceed to Phase <N+1> — <Next Name>?
 Reply: yes | no | show-details
===================================================
 ⛔ WAITING FOR YOUR REPLY. No further action until you say yes.
```

On `yes`  — update migration-context.json (approvedPhases), then start next phase.
On `no`   — ask what to fix, re-run the phase or apply a targeted fix, then re-print the block.
On `show-details` — print the full phase report JSON, then re-print the block and wait again.

**Phase gates are not optional. A phase is not approved until the user explicitly says `yes`.**

---

## PROGRESS TRACKING

Maintain migration-context.json updated after every phase:

```json
{
  "generatedAt": "<ISO-8601>",
  "sourceRepo": "<absolute-path>",
  "targetRepo": "<absolute-path>",
  "migrationWorkspace": "<absolute-path-to-migration-directory>",
  "javaVersion": "<version>",
  "mavenVersion": "<version>",
  "currentPhase": "<phase-id>",
  "completedPhases": [],
  "approvedPhases": [],
  "paths": {
    "repoMetadata":       null,
    "dependencyAnalysis": null,
    "migrationSpec":      null
  },
  "phaseReports": {
    "1-discovery":   null,
    "2-planning":    null,
    "5-persistence": null,
    "6-service":     null,
    "7-messaging":   null,
    "8-web":         null,
    "9-config":      null,
    "11-validation": null
  }
}
```

---

## ERROR HANDLING

1. Capture the error message and file(s) involved
2. Delegate to agents/10-compile-fix.prompt.md with error context
3. Maximum 3 retries per file
4. If still failing — mark MANUAL_REVIEW_REQUIRED in compile-fix-report.json
5. Present manual-review list to user
6. Ask whether to continue with those files flagged, or stop

---

## SKIPPED FUNCTIONALITY POLICY

Record every skipped feature in migration-spec.yaml:

```yaml
skipped:
  - feature: Spring Cloud Config
    reason: External configuration service migration out of scope
    files: ["src/main/java/config/CloudConfig.java"]
```

---

## REQUIRED DELIVERABLES

Discovery & Planning: migration-context.json, repo-metadata.json, dependency-analysis.yaml, migration-spec.yaml
Target Project: pom.xml, application.properties, Dockerfile, docker-compose.yml, README.md
Phase Reports: persistence-migration-report.json, service-migration-report.json,
               messaging-migration-report.json (if messaging:true), web-migration-report.json,
               configuration-migration-report.json, compile-fix-report.json (if needed), validation-report.json
Final: migration-summary.md

---

## HOW TO START

### Usage Instructions

You can start the migration in several ways:

**Option 1: Let the orchestrator prompt you (Recommended for first-time users)**
```
Use the Spring to Quarkus migration skill
```
or simply:
```
Migrate my Spring app to Quarkus
```
The orchestrator will prompt you for source and target directories during Phase 0.

**Option 2: Specify source directory only**
```
Migrate <path-to-source-spring-project> to Quarkus
```
The orchestrator will prompt you for the target directory during Phase 0.

**Option 3: Specify both source and target directories**
```
Migrate <path-to-source-spring-project> to Quarkus at <path-to-target-directory>
```

### Examples:

1. **Interactive mode (orchestrator prompts for paths):**
   ```
   Migrate my Spring Boot app to Quarkus
   ```

2. **Source only (orchestrator prompts for target):**
   ```
   Migrate ./my-spring-app to Quarkus
   ```

3. **Both source and target specified:**
   ```
   Migrate ~/projects/spring-petclinic to Quarkus at ~/projects/quarkus-petclinic
   ```

4. **Using absolute paths:**
   ```
   Migrate /home/user/spring-app to Quarkus at /home/user/quarkus-app
   ```

### What Happens in Phase 0:

During **Phase 0 - Environment Preparation**, the orchestrator will:
1. Check for and kill any running Spring Boot or Quarkus processes
2. Verify Java >= 17 and Maven >= 3.9 (or Gradle >= 8)
3. **Confirm source and target directory paths** (prompts you if not specified)
4. Verify the agents/ directory structure
5. Create `migration-metadata/migration-context.json` to track progress

### Important Notes:
- The **source directory** must contain an existing Spring Framework or Spring Boot project with `pom.xml` or `build.gradle`
- The **target directory** will be created if it doesn't exist (recommended to use a new empty directory)
- Both source and target paths can be absolute or relative to your current workspace
- If you're unsure about paths, just start the skill and let it prompt you - this is the safest approach

The orchestrator will guide you phase by phase, requiring explicit approval (`yes`) after each phase before proceeding.

**Works with:**
- Spring Framework standalone applications
- Spring Boot applications (all versions)
- Spring MVC and Spring WebFlux
- XML-based and annotation-based configuration