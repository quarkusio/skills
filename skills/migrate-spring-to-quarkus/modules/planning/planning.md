# Module: Planning

Scan the source project, collect migration decisions, and generate `<target>/migration-spec.yaml` as the binding contract for all downstream modules.

## Gate Condition

**ALWAYS** — runs before all transformation modules.

---

## Instructions

Follow the steps below in sequence:

### Step 0: Resolve Directories and Copy Source

Before scanning or asking anything, establish `<source>` and `<target>`, then copy the source project:

1. **Identify `<source>`** — the Spring Boot project directory the user points to.
2. **Resolve `<target>`** — default is `<source-name>-quarkus/` as a sibling of `<source>`. Example: if source is `~/projects/petclinic`, target is `~/projects/petclinic-quarkus`.
3. **If `<target>` already exists** — ask the user whether to overwrite it or choose a different path.
4. **Log both paths** before continuing:
   ```
   Source: <source-path>
   Target: <target-path>
   ```
5. **Copy the source project into `<target>`**:
   - Create `<target>` if it does not exist.
   - Copy all files from `<source>` into `<target>`, preserving the directory structure. This includes `src/`, resources, build files, wrapper scripts, and any other project files.
   - From this point on, all transformation modules read from `<source>` and write to `<target>`. Do not modify `<source>`.

All files written by the planning module (including `migration-spec.yaml`) go into `<target>`. Never write to `<source>`.

---

### Step 1: Scan Source Project & Detect Features

1. **Build Descriptor**:
   - Maven (`pom.xml`) or Gradle (`build.gradle` / `build.gradle.kts`).
   - Identify: `spring_boot_version`, source `java_version`, `build_tool` (`Maven` or `Gradle`).
2. **Java Sources**:
   - Inspect annotations, imports, and classes to evaluate selective feature flags:

| Flag | Detected when |
|---|---|
| `spring_web` | `@RestController`, `@Controller` in Java sources |
| `spring_data_jpa` | `JpaRepository`, `@Entity` in Java sources |
| `spring_security` | `SecurityConfig`, `@EnableWebSecurity`, `@PreAuthorize`, `SecurityFilterChain` in Java sources |
| `spring_kafka` | `@KafkaListener`, `KafkaTemplate` in Java sources |
| `spring_rabbitmq` | `@RabbitListener`, `RabbitTemplate` in Java sources |
| `spring_jms` | `@JmsListener`, `JmsTemplate` in Java sources |
| `spring_scheduled` | `@Scheduled` in Java sources |
| `spring_cache` | `@Cacheable`, `@CacheEvict` in Java sources |
| `view_layer` | Thymeleaf/JSP in `templates/` or `webapp/`, static resources in `static/`, JSF XHTML in `webapp/` or `META-INF/resources/`, FreeMarker templates (`.ftl`, `.ftlh`, `.ftlx`), `freemarker.*` imports, or `FreeMarkerConfigurer` bean |

3. **Complexity Estimation**:
   - Count total components across controllers, services, repositories, and entities:
     - `low`: < 10 components
     - `medium`: 10–50 components
     - `high`: > 50 components

---

### Step 2: Present Findings Summary

Display a findings table to the user:

```markdown
### Source Project Analysis Summary
- **Build Tool**: [Maven | Gradle]
- **Spring Boot Version**: [e.g., 3.2.0]
- **Source Java Version**: [e.g., 17]
- **Estimated Complexity**: [low | medium | high]

| Area | Detected Technology / Annotations | Status |
|---|---|---|
| Web / REST | `@RestController`, `@Controller` | [Detected / Not found] |
| Data / Persistence | `@Entity`, `JpaRepository` | [Detected / Not found] |
| Security | `SecurityConfig`, `@EnableWebSecurity` | [Detected / Not found] |
| Messaging | Kafka / RabbitMQ / JMS | [Detected (type) / Not found] |
| Scheduling | `@Scheduled` | [Detected / Not found] |
| Caching | `@Cacheable` | [Detected / Not found] |
| View Layer | Thymeleaf / JSP / FreeMarker / JSF | [Detected / Not found] |
| Tests | `@SpringBootTest`, `@WebMvcTest` | [Detected / Not found] |
```

---

### Step 3: Collect Decisions

**Determining the mode:**
- **Non-interactive mode**: `mode: non-interactive` was passed as a skill argument **or** is set in `.quarkus-migration.yml`. Apply all defaults silently — do not ask the user anything. The presence of other fields (`strategy`, `quarkus_version`, `java_version`) in the config file does NOT trigger non-interactive mode on its own.
- **Interactive mode**: everything else. Ask the user for any decision not already resolved by a skill argument or `.quarkus-migration.yml`. Having some fields pre-set just means fewer questions — not no questions.

Decisions are collected in two stages.

**Resolution Priority for any decision** (first match wins):

Every decision in the `decisions` block has a corresponding `<field>_source` field (e.g. `strategy_source`, `persistence_source`) that records how its value was determined. Each decision is tracked independently — some may come from a config file while others are asked interactively.

| `*_source` value | When it is set | Definition |
|---|---|---|
| `argument` | A value was passed directly as a skill invocation argument | Highest priority. The value is used as-is without prompting the user. |
| `config-file` | `.quarkus-migration.yml` exists in `<source>` root and contains the field | Pre-existing project config. Takes precedence over interactive prompt but not over an explicit argument. |
| `user` | Neither argument nor config file provided a value, and the user was asked interactively | The user explicitly chose this value during the planning conversation. |
| `default` | Neither argument nor config file provided a value, and the session is non-interactive | The planning module auto-selected the documented default. No user was asked. |

#### Stage 1: Core Decisions (Always Collect)

**Before asking the user**, do the following in order:

1. **Call the [`code.quarkus.io/api/streams`](https://code.quarkus.io/api/streams) API** to resolve the latest stable Quarkus version and the minimum JDK it requires.

   From the API response, extract:
   - `<api_quarkus_version>` — the latest recommended stable stream (e.g. `3.20.1`)
   - `<api_min_jdk>` — the minimum JDK required by that stream (e.g. `17` for Quarkus 3.x, `21` for Quarkus 4.x)

   **Pass 2 JDK check** — run immediately after the API call, before asking the user anything:
   - Reuse the installed JDK version already captured by the prerequisite module.
   - **Determine the target Quarkus version** for the check (first match wins):
     - Skill argument `quarkus_version` if provided
     - `quarkus_version` from `.quarkus-migration.yml` if present
     - Otherwise `<api_quarkus_version>` (the recommended stream)
   - **Match the target version to its stream**: find the entry in the API response whose `quarkusCoreVersion` major.minor matches the target (e.g. `4.1.x` matches the `4.1` stream). Extract `javaCompatibility.versions[0]` from that stream as `<api_min_jdk>`. If the API is unreachable, use the static fallback table (3.x → 17, 4.x → 21).
   - If the installed JDK is **< `<api_min_jdk>`**:
     - Warn the user: "The target Quarkus version `<api_quarkus_version>` requires JDK `<api_min_jdk>` or later. Your installed JDK (`<detected version>`) is too old. Please install JDK `<api_min_jdk>` or choose an older Quarkus stream that matches your JDK."
     - **Stop the migration** — do not proceed to decision collection or spec writing.
   - If the installed JDK meets the requirement, continue.

2. **Check for skill arguments** — if the skill was invoked with `strategy`, `quarkus_version`, `java_version`, or `mode` as arguments, use those values directly. Set the corresponding `*_source` field to `argument`. Mark these fields as resolved — skip them in the next step.

3. **Check for `.quarkus-migration.yml`** in `<source>` root. If it exists, read it and extract any of the following fields **not already resolved by step 2**:
   - `mode` — `non-interactive` (if set, apply all defaults silently for this entire run)
   - `strategy` — `full-quarkus` or `spring-compat`
   - `quarkus_version` — target Quarkus version (written to `target_technology.quarkus_version`; set `quarkus_version_source: config-file`)
   - `java_version` — target Java version (written to `target_technology.java_version`; set `java_version_source: config-file`)

   For each field found, use its value directly and set the corresponding `*_source` field to `config-file`. Skip asking the user for that decision.

   > **Do NOT validate or override user-provided values.** When `quarkus_version` or any other field comes from a skill argument or config file, use it exactly as provided. Do not check it against the API response or substitute a different value.

Only proceed to ask the user (or apply defaults) for decisions not already resolved by steps 2–3.

**In interactive mode**, present all unresolved Stage 1 questions together in a single message:

```
Based on the source project analysis above, I need a few decisions before starting the migration:

1. **Target Quarkus version** — Latest stable is <API-resolved version>. Use this or specify another?
2. **Target Java version** — Your source project uses JDK <source_java_version>. The minimum for Quarkus <version> is <minimum>. Use <source_java_version> or specify another?
3. **Migration strategy**:
   - `full-quarkus` (recommended): Replace Spring with JAX-RS/CDI/Panache — full Quarkus experience
   - `spring-compat`: Keep Spring annotations via Quarkus compatibility extensions — minimal code changes

Please reply with your choices before I continue.
```

**Stop here and wait for the user's response before proceeding to Stage 2 or writing the spec.**

In **non-interactive mode**, apply defaults without asking:

| # | Decision | Non-interactive default |
|---|---|---|
| 1 | Target Quarkus version | API-resolved latest stable from `code.quarkus.io/api/streams` |
| 2 | Target Java version | Minimum JDK required by the resolved Quarkus version (today: JDK 17 for Quarkus 3.x, JDK 21 for Quarkus 4.x) |
| 3 | Migration strategy | `full-quarkus` |

#### Stage 2: Conditional Decisions (Based on Stage 1 and Detected Features)

Evaluate ALL Stage 2 conditions against `detected_features` before presenting questions. Log each condition and its result (met/not met) so skipped decisions are traceable.

**In interactive mode**, collect all applicable Stage 2 questions in a single message:

```
A few more decisions based on what I found in the source project:

[Include only the applicable questions below]

- **Persistence strategy** (JPA detected, full-quarkus only): panache-active-record / panache-repository / hibernate-orm?
- **REST framework** (web layer detected, full-quarkus only): quarkus-rest (RESTEasy Reactive, recommended) / resteasy-classic?
- **Messaging transport** (messaging detected): kafka / amqp / artemis-jms?
- **View technology** (view layer detected): qute (recommended) / myfaces (maintain JSF, if JSF detected) / freemarker (maintain FreeMarker, if FreeMarker detected)?
- **Security approach** (Spring Security detected, full-quarkus only): oidc / basic / jwt / none?

Please reply with your choices before I continue.
```

**Stop here and wait for the user's response before writing the spec.**

In **non-interactive mode**, apply defaults without asking:

| # | Decision | Condition | Non-interactive default |
|---|---|---|---|
| 4 | Persistence strategy | `full-quarkus` + `spring_data_jpa` detected | `panache-active-record` |
| 5 | REST framework | `full-quarkus` + `spring_web` detected | `quarkus-rest` |
| 6 | Messaging transport | Messaging detected (`spring_kafka`, `spring_rabbitmq`, `spring_jms`) | Matching detected transport |
| 7 | View technology | `full-quarkus` + `view_layer` detected (any technology) | `qute` |
| 7 | View technology | `spring-compat` + JSF detected | `myfaces` |
| 7 | View technology | `spring-compat` + FreeMarker detected | `freemarker` |
| 7 | View technology | `spring-compat` + Thymeleaf or JSP detected | `qute` |
| 8 | Security approach | `full-quarkus` + `spring_security` detected | `none` |

If a Stage 2 condition is not met, skip the question and set the field to `none`.

---

### Step 4: Write `migration-spec.yaml`

Before writing the file, run the following command to get the actual current timestamp:

```bash
date -u +"%Y-%m-%dT%H:%M:%SZ"
```

Use the output as the value of `metadata.generatedAt`. Do not hardcode or guess the timestamp.

Write the specification to `<target>/migration-spec.yaml`.

```yaml
project:
  name: <source-name>-quarkus
  source_path: <source_directory_path>
  target_path: <target_directory_path>

source_technology:
  spring_boot_version: "<detected-version>"
  java_version: "<detected-java-version>"
  build_tool: "<Maven|Gradle>"

target_technology:
  quarkus_version: "<resolved-quarkus-version>"
  java_version: "<target-java-version>"
  build_tool: "<Maven|Gradle>"

detected_features:
  spring_web: true|false
  spring_data_jpa: true|false
  spring_security: true|false
  spring_kafka: true|false
  spring_rabbitmq: true|false
  spring_jms: true|false
  spring_scheduled: true|false
  spring_cache: true|false
  view_layer: true|false

decisions:
  strategy: "full-quarkus|spring-compat"
  strategy_source: "argument|config-file|user|default"
  quarkus_version_source: "argument|config-file|user|default"  # resolved value written to target_technology.quarkus_version
  java_version_source: "argument|config-file|user|default"     # resolved value written to target_technology.java_version
  persistence: "panache-active-record|panache-repository|hibernate-orm|none"
  persistence_source: "argument|config-file|user|default"
  rest_framework: "quarkus-rest|resteasy-classic|none"
  rest_framework_source: "argument|config-file|user|default"
  messaging_transport: "kafka|amqp|artemis-jms|none"
  messaging_transport_source: "argument|config-file|user|default"
  view_layer: "qute|myfaces|freemarker|none"
  view_layer_source: "argument|config-file|user|default"
  security_approach: "oidc|basic|jwt|none"
  security_approach_source: "argument|config-file|user|default"

metadata:
  complexity: "low|medium|high"
  generatedAt: "<actual current timestamp in ISO-8601 format, e.g. 2026-09-28T11:00:00Z>"
```
