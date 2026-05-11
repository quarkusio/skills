---
name: quarkus-app
description: >
  Use this skill whenever a user wants to create, scaffold, bootstrap, initialize, generate, or set up a new
  Quarkus application or microservice. Also trigger when a user asks how to add extensions to an existing Quarkus
  project, upgrade a Quarkus version, or maintain a Quarkus app's dependencies. This skill ensures all projects
  are generated using the latest official Quarkus platform version, proper tooling (CLI preferred, Maven plugin as
  fallback), and official platform or Quarkiverse extensions only. Trigger even for casual phrasing like "spin up
  a Quarkus app", "start a new Quarkus service", "create a Quarkus REST API", or "add a Quarkus extension".
---

# Quarkus App Skill

This skill governs how to create and maintain Quarkus applications. Always follow the decision tree and
conventions below.

---

## Step 0 — Check the Latest Quarkus Version

Before generating any project, always verify the current latest stable Quarkus release. The version baked into
this skill may be stale. Use web search or the Maven Central / GitHub releases to confirm.

- Community latest stable: check https://github.com/quarkusio/quarkus/releases (e.g. currently `3.32.x`)
- LTS stream: currently `3.27.x` and `3.20.x` (use LTS only if user explicitly requests long-term stability)
- Red Hat build of Quarkus (RHBQ): use when the user mentions Red Hat support, OpenShift production, or RHBQ

Use the latest stable community version unless told otherwise.

---

## Step 1 — Gather Project Parameters

Before generating, collect:

| Parameter | Default | Notes |
|---|---|---|
| `groupId` | `io.arrogantprogrammer` | Always ask if not provided |
| `artifactId` | *(required)* | The app name / Maven artifact ID |
| `version` | `1.0.0-SNAPSHOT` | Standard Maven SNAPSHOT for new apps |
| `extensions` | *(required)* | See extension guidance below |
| `build tool` | Maven | Ask if user prefers Gradle |
| `Java version` | 25 | Always ask — suggest 25 as the default |

If the user hasn't specified a Java version, ask which version they'd like to use and suggest 25 as the default.

If the user hasn't specified extensions, ask or propose sensible defaults based on their described use case.

---

## Step 2 — Choose the Tooling

**Always prefer the Quarkus CLI.** Fall back to the Maven plugin only if:
- The user says `quarkus` CLI is not installed, OR
- You're generating commands in a context where CLI availability cannot be assumed (e.g., CI pipelines, Docker)

### Option A: Quarkus CLI (preferred)

```bash
quarkus create app {groupId}:{artifactId}:{version} \
  --no-code \
  -x={extension1},{extension2},{extension3}
```

**Key CLI flags:**
- `-x` / `--extensions` — comma-separated extension list (short names, no spaces after commas)
- `--no-code` — skip generated example code (omit if user wants starter code)
- `--gradle` — switch to Gradle build (default is Maven)
- `-S io.quarkus.platform:3.32` — pin to a specific stream if needed

**Installation note** (include if user might not have CLI):
```bash
# Via SDKMAN (recommended)
sdk install quarkus

# Via JBang
curl -Ls https://sh.jbang.dev | bash -s - trust add https://repo1.maven.org/maven2/io/quarkus/quarkus-cli/
curl -Ls https://sh.jbang.dev | bash -s - app install --fresh --force quarkus@quarkusio

# Via Homebrew (macOS/Linux)
brew install quarkusio/tap/quarkus
```

---

### Option B: Maven Plugin (fallback)

Use the `io.quarkus.platform` group (NOT `io.quarkus`) — this ensures the full Quarkus Platform BOM is used,
which includes Quarkiverse extensions and correct dependency alignment.

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:{QUARKUS_VERSION}:create \
  -DprojectGroupId={groupId} \
  -DprojectArtifactId={artifactId} \
  -DprojectVersion={version} \
  -Dextensions='{extension1},{extension2},{extension3}' \
  -DnoCode
```

**Key parameters:**
- `-DnoCode` — omit generated example classes
- `-DclassName` — optional REST resource class name
- `-Dpath` — optional REST path (used with `-DclassName`)

---

## Step 3 — Extension Selection Rules

### Always use official extensions — never raw Maven dependencies for Quarkus functionality

| Source | When to use | How to reference |
|---|---|---|
| **Quarkus Platform** | Core Quarkus features | Short name (e.g., `rest-jackson`) |
| **Quarkiverse** | Community/ecosystem features | Short name or `io.quarkiverse.*:quarkus-*` |
| **Never raw** | Don't add `io.quarkus:quarkus-*` manually | Use CLI/Maven plugin to manage |

### Resolving extension names

Short names work in both CLI (`-x=rest-jackson`) and Maven plugin (`-Dextensions='rest-jackson'`).
For Quarkiverse extensions (not in core platform), use the full artifact coordinates:

```bash
# CLI — Quarkiverse extension
quarkus ext add io.quarkiverse.langchain4j:quarkus-langchain4j-openai

# Maven plugin — Quarkiverse extension  
-Dextensions='io.quarkiverse.langchain4j:quarkus-langchain4j-openai'
```

### Common extension reference table

See `references/extensions.md` for a curated list of commonly used extensions by category.

---

## Step 4 — Post-Generation Guidance

After generating, always remind the user:

1. **Dev mode**: `./mvnw quarkus:dev` (Maven) or `quarkus dev` (CLI)
2. **Dev UI**: available at `http://localhost:8080/q/dev` — useful for extension management
3. **Adding extensions later**:
   ```bash
   quarkus ext add {extension}          # CLI
   ./mvnw quarkus:add-extension -Dextensions='{extension}'  # Maven
   ```
4. **DevServices**: Quarkus auto-starts containers (PostgreSQL, Kafka, etc.) in dev/test mode — no manual setup needed
5. **application.properties** location: `src/main/resources/application.properties`

---

## Step 5 — Maintaining Existing Apps

### Upgrade Quarkus version

```bash
# CLI (preferred)
quarkus update

# Maven plugin
./mvnw quarkus:update
```

Always check the migration guide at `https://quarkus.io/guides/update-quarkus` before upgrading.

### Add extensions to existing project (run from project root)

```bash
quarkus ext add rest-jackson hibernate-orm-panache jdbc-postgresql
```

### List installed extensions

```bash
quarkus ext ls
```

### List available extensions

```bash
quarkus ext ls -i       # installable from current platform
quarkus ext ls -i -s=ai # search by keyword
```

---

## Canonical Examples

### Minimal REST + PostgreSQL service (CLI)

```bash
quarkus create app io.arrogantprogrammer:my-service:1.0.0-SNAPSHOT \
  --no-code \
  -x=rest-jackson,hibernate-orm-panache,jdbc-postgresql
```

### Same, with Maven plugin (pinned to specific version)

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.32.3:create \
  -DprojectGroupId=io.arrogantprogrammer \
  -DprojectArtifactId=my-service \
  -DprojectVersion=1.0.0-SNAPSHOT \
  -Dextensions='rest-jackson,hibernate-orm-panache,jdbc-postgresql' \
  -DnoCode
```

### AI/LangChain4j service

```bash
quarkus create app io.arrogantprogrammer:ai-service:1.0.0-SNAPSHOT \
  --no-code \
  -x=rest-jackson,io.quarkiverse.langchain4j:quarkus-langchain4j-openai
```

### Kafka event-driven service

```bash
quarkus create app io.arrogantprogrammer:event-service:1.0.0-SNAPSHOT \
  --no-code \
  -x=messaging-kafka,rest-jackson
```

---

## Important Rules

- **NEVER** manually add `<dependency>` blocks for `io.quarkus:quarkus-*` artifacts — always go through the
  extension management tooling to ensure BOM alignment
- **NEVER** use `io.quarkus:quarkus-maven-plugin` in the create command — always use
  `io.quarkus.platform:quarkus-maven-plugin` (the platform plugin resolves the full BOM)
- Always verify the Quarkus version at Step 0 before filling in commands
- If the user is on Red Hat OpenShift or wants RHBQ, see `references/rhbq.md` for RHBQ-specific guidance
