# Test Harness

JUnit 5 test suite that runs migration skills against real projects, scores the results, and generates skill improvement reviews — all tracked over time.

## Prerequisites

- **Java 21+** — `java -version`
- **Maven 3.9+** — `mvn -version`
- **git** — for cloning external test projects
- **acp client** - to [install](https://github.com/smallrye/smallrye-acp-client#acp-cli) locally an AI acp agent and check available models
- **AI Provider KEY** - an AI api key to access a LLM provider: Google Vertex, Anthropic, IBM Bob, etc

> [!IMPORTANT]
> Before to run the test, install at least one **AI ACP agent** and set the appropriate environment variables according to your AI LLM provider ! 

## Set up the AI ACP agent and identify the model to be used

The test harness calls an AI [ACP](https://agentclientprotocol.com/get-started/introduction) `agent` to execute a headless conversation using prompt message and SKILL. 

To simplify your life, you can use the [Smallrye ACP client](https://github.com/smallrye/smallrye-acp-client#acp-cli) able to install locally (under ~/.acp/agents) the agent you would like to use from the registry of the agents: 

```shell
acp registry list -r
ACP Registry v1.0.0 - 41 agents available
Current platform: darwin-aarch64

ID                        VERSION      DISTRIBUTION   DESCRIPTION
------------------------------------------------------------------------------------------
...
claude-acp                0.79.0       npx            ACP wrapper for Anthropic's Claude
...
codex-acp                 1.12.0       npx            ACP adapter for OpenAI's coding ass... [installed]
...
```

Execute then this command to install it using the `ID` (see the first column of the registry table) passed to the command:
```shell
acp registry install claude-acp
```

> [!IMPORTANT]
> If the AI agent is not available from the ACP registry, you can install it using an ACP Registry file that you specify to the command using the option: `--registry-file`.

To select the model to be used from AI LLM provider (Google Vertex, Anthropic, IBM Bob, etc), set the corresponding environment variables

```shell
export GOOGLE_APPLICATION_CREDENTIALS=~/.config/gcloud/application_default_credentials.json
export VERTEX_LOCATION=<YOUR_GOOGLE_CLOUD_LOCATION>
export GOOGLE_CLOUD_PROJECT=<YOUR_GOOGLE_CLOUD_PROJECt_ID>
export BOBSHELL_API_KEY=<BOBSHELL_API_KEY>
...
```
and execute next the following command:
```shell
acp model list -a <ACP_AGENT_ID>

Example:
acp model list -a opencode
acp model list -a claude-acp
acp model list -a pi-acp
```

## Running Tests

The process to execute the tests is pretty straightforward and just require to:
- open a terminal and move under the `tests` folder, 
- define different system properties `-Dxxxx`
as described hereafter. 

```bash
cd tests/

# Run all in-repo test projects (uses agent's default model)
mvn test -Pintegration
```

The maven profile has been configured to use by default as agent: `claude`, the default model.

You can, of course, change different parameters as listed at the section: [Configuration Properties](#configuration-properties)

Examples 
```shell
# Select the agent to be used. Default is: claude-acp
mvn test -Pintegration -Dai.agent=claude-acp
mvn test -Pintegration -Dai.agent=pi-acp

# Run a specific sample project
mvn test -Pintegration -Dai.projects=spring-rest-api

# Set model
mvn test -Pintegration -Dai.model=anthropic/claude-opus-4-6
mvn test -Pintegration -Dai.model=vertex-anthropic/claude-opus-4-6
mvn test -Pintegration -Dai.model=claude-sonnet-4-5-20250514

# Claude Code agent (uses Anthropic API directly)
mvn test -Pintegration -Dai.agent=claude-acp -Dai.model=claude-opus-4-6

# Use compatibility migration strategy instead of full
mvn test -Pintegration -Dai.strategy=compatibility

# Override timeout (seconds)
mvn test -Pintegration -Dai.projects=spring-petclinic -Dai.timeout=900

# Combine options
mvn test -Pintegration -Dai.projects=spring-jpa-crud -Dai.model=anthropic/claude-sonnet-4-5-20250514 -Dai.timeout=600
```

### Configuration Properties

The complete list of the configurations via `-D` flags:

| Property          | Default                                                                                                                                                                                             | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|-------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ai.model`        | *(empty)*                                                                                                                                                                                           | Model, optionally prefixed with provider (e.g. `anthropic/claude-opus-4-6`, `opus`, `claude-sonnet-4-5-20250514`)                                                                                                                                                                                                                                                                                                                                                |
| `ai.strategy`     | `full`                                                                                                                                                                                              | Migration strategy: `full` or `compatibility`. The strategy will tell to AI if we would like to migrate Spring Boot to Quarkus or using the Spring compatibility later which has been developed for some spring components like [DI](https://quarkus.io/guides/spring-di#more-spring-guides), [Web](https://quarkus.io/guides/spring-web), [Data JPA](https://quarkus.io/guides/spring-data-jpa), [Data REST](https://quarkus.io/guides/spring-data-rest),  etc. |
| `ai.prompt`       | Migration prompt message declared [here](https://github.com/quarkusio/skills/blob/bec909505664bf3405c39542a402c4ee8e5c5cf1/tests/src/test/java/io/quarkus/migration/runner/OpenCodeRunner.java#L55) | Override the default migration prompt message when it is needed to test a new and different skills                                                                                                                                                                                                                                                                                                                                                               |
| `ai.timeout`      | `300`                                                                                                                                                                                               | Timeout per project in seconds                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `ai.agent`          | `claude-acp`                                                                                                                                                                                            | Path to the AI binary (if not on PATH)                                                                                                                                                                                                                                                                                                                                                                                                                           | |
| `ai.projects`     | *(all)*                                                                                                                                                                                             | Comma-separated list of projects to test (e.g. `dummy,spring-rest-api`).                                                                                                                                                                                                                                                                                                                                                                                         |
| `ai.skills`       | *(from project.yaml)*                                                                                                                                                                               | Comma-separated list of skills (max 2). Accepts local names or GitHub URLs. Use `#branch/subpath` for URLs with branch disambiguation (see [Selecting a skill](#selecting-a-skill))                                                                                                                                                                                                                                                                               |
| `ai.args`         | *(empty)*                                                                                                                                                                                           | Space-separated skill arguments substituted into `SKILL.md` placeholders (see [Skill arguments](#skill-arguments))                                                                                                                                                                                                                                                                                                                                               |
| `runs`            | `1`                                                                                                                                                                                                 | Number of times to repeat the migration. Each run gets a fresh workdir, its own report, and a separate entry in `history.jsonl`. Useful for collecting data across multiple runs                                                                                                                                                                                                                                                                                 |
| `runChecks`       | `true`                                                                                                                                                                                              | When `false`, skip verification checks after migration. Also skipped when the project has no checks defined                                                                                                                                                                                                                                                                                                                                                      |
| `ai.review`       | `true`                                                                                                                                                                                              | When `false`, skip the skill review step after migration. Also skipped when checks are disabled or none defined                                                                                                                                                                                                                                                                                                                                                  |

### Selecting a skill

`ai.skills` accepts a skill name (resolved at the root of this project under `./skills` folder) or using a GitHub URL:

```bash
# Local skill by name (looked up in skills/)
mvn test -Pintegration -Dai.skills=migrate-spring-to-quarkus
mvn test -Pintegration -Dai.skills="../tests/skills/dummy"

# Remote skill — paste the GitHub URL as-is
mvn test -Pintegration -Dai.skills=https://github.com/org/repo/tree/main/skills/custom-skill

# Remote skill on a feature branch (branch name has no slashes — URL is unambiguous)
mvn test -Pintegration -Dai.skills=https://github.com/org/repo/tree/new-feature-branch/skills/custom-skill

# Remote skill when branch name contains '/' — use '#' to separate the URL from branch/subpath
mvn test -Pintegration -Dai.skills=https://github.com/org/repo#branch/with/slashes/skills/custom-skill
```

Remote clones are cached in `target/skills/` (or within the AI agent recommended folder) and cleaned with `mvn clean`.

### Examples

Here are some examples that we currently use for local tests using `Google Vertex AI` combining the system properties and environment variables

1. Dummy project

```shell
// Use gcloud auth login to use OAuth authentication and generate locally the application_default_credentials.json file
export GOOGLE_APPLICATION_CREDENTIALS=~/.config/gcloud/application_default_credentials.json
export VERTEX_LOCATION=your-google-cloud-location
export GOOGLE_CLOUD_PROJECT=your-google-cloud-project-id
rm -rf target/runs

// Dummy test to verify if the Agent works, is well configured
mvn test -Pintegration \
  -Dai.projects=dummy \
  -Dai.skills=../tests/skills/dummy \
  -Dai.prompt="Say Hello."
  
// or using project.yaml definition
mvn test -P integration -Dai.projects=dummy -Dai.prompt="Say Hello."  
```
Verify if there is under the following path `target/workdirs/dummy-quarkus/` a `HELLO.md` created!
  
2. Spring Boot TODO

The following example uses the local project: `Spring Boot TODO` and the strategy: `compatibility`
```bash
export GOOGLE_APPLICATION_CREDENTIALS=~/.config/gcloud/application_default_credentials.json
export VERTEX_LOCATION=your-google-cloud-location
export GOOGLE_CLOUD_PROJECT=your-google-cloud-project-id
rm -rf target/runs

mvn test -Pintegration \
    -Dai.projects=spring-boot-todo-app \
    -Dai.strategy=compatibility \
    -Dai.agent=claude-acp \
    -Dai.skills=migrate-spring-to-quarkus \
    -Dai.timeout=600
```

## Running the ACP Agent using Main class

Instead of `mvn test -Pintegration`, you can launch the agent directly using the `Main` class (`io.quarkus.ai.harness.launcher.Main`). This is useful for running from an IDE (IntelliJ, VS Code) or from the terminal without the JUnit overhead.

### Using `mvn exec:exec`

The `exec-maven-plugin` is pre-configured with a `default-cli` execution. All `-D` system properties are forwarded automatically:

```bash
cd tests/

# Run the dummy project
mvn exec:exec -Dai.projects=dummy -Dai.prompt="Say Hello." -Dai.agent=ibm-bob

mvn exec:exec -Dai.projects=spring-rest-api -Dai.skills=migrate-spring-to-quarkus
```

### Running from an IDE

In IntelliJ or VS Code, create a **Run Configuration** with:

- **Main class:** `io.quarkus.ai.harness.launcher.Main`
- **Working directory:** `tests/`
- **VM options:** `-Dai.projects=dummy -Dai.prompt="Say Hello." -Dai.agent=claude-acp`

The `Main` class also accepts `-D` flags as program arguments (e.g. `-Dai.projects=dummy`), so you can pass them either way.

> [!TIP]
> This approach gives you IDE debugging support — you can set breakpoints in `AgentSkillExecutor`, `SmallryeAcpRunner`, or any harness class.

## Benchmark: comparing skills

Use `-Dai.skills` to benchmark up to 2 skills against one or more projects. Each skill is run independently with its own set of runs, and a benchmark summary report with delta comparison is generated at the end. Multiple values for `-Dai.skills` or `-Dai.projects` must be comma-separated.

### Single project, 2 skills

```bash
# Compare two skills on the same project with 2 runs each
mvn test -Pintegration \
  -Dai.projects=spring-rest-api \
  -Dai.skills=migrate-spring-to-quarkus,migrate-spring-to-quarkus-mtool \
  -Dai.agent=claude-acp \
  -Druns=2
```

### Multiple projects, 2 skills

```bash
# Benchmark across multiple projects
mvn test -Pintegration \
  -Dai.projects=spring-rest-api,spring-jpa-crud \
  -Dai.skills=migrate-spring-to-quarkus,migrate-spring-to-quarkus-mtool \
  -Dai.agent=claude-acp \
  -Druns=3
```

### Generated reports

When benchmarking with 2 skills, the harness generates:

- **Per-skill summary** (`target/runs/<project>_<skill>_*.summary.md`) — averages across runs for each skill+project combination
- **Benchmark summary** (`target/runs/benchmark-report.md`) — side-by-side comparison with a delta row showing the percentage difference between the two skills for tokens, tools, etc

Example of benchmark output:

```
| Skill | mtool? | Runs | Avg Duration | Avg Input | Avg Output | Avg Cache Read | Avg Total Tokens | Avg Cost |
|---|---|---|---|---|---|---|---|---|
| migrate-spring-to-quarkus | No | 15 | 1m 5s (+/- 15s) | 78 (+/- 11) | 3,669 (+/- 984) | ... | 165,582 (+/- 50,343) | $0.00 |
| migrate-spring-to-quarkus-mtool | Yes | 15 | 0m 40s (+/- 8s) | 6 (+/- 0) | 1,484 (+/- 240) | ... | 116,672 (+/- 14,660) | $0.00 |
| **Delta** | — | — | -38.5% | -92.3% | -59.5% | ... | -29.5% | — |
```

A negative delta means the second skill used fewer resources or ran faster.

> [!NOTE]
> Run artifacts in `target/runs/` are **not deleted** between projects or skills within the same `mvn test` invocation. Each run produces its own distinct report file.

## Eval Tests

Eval tests assess the quality of a skill without requiring a full migration run (except `eval-all`, which includes one). 
They live alongside the migration tests but run under separate Maven profiles.

### Trigger Eval (`-Peval`)

Uses an LLM-as-judge approach to test whether the skill description correctly activates for matching prompts and rejects non-matching ones. 
Sends 20 classification prompts (10 positive, 10 negative) to an ACP agent and measures accuracy.

```bash
# Run trigger eval (requires an ACP agent)
mvn test -Peval -Dai.agent=claude-acp

# Use a specific model as judge
mvn test -Peval -Dai.agent=claude-acp -Dai.model=claude-sonnet-4-5-20250514
```

Test cases are defined in `src/test/resources/evals/trigger-tests.json`. 
Each case has an `id`, `prompt`, `should_trigger` (expected result), and `category` (for diagnosing failure patterns). 
Results are written to `target/eval-results/trigger-eval-<timestamp>.json`.

The test fails if overall accuracy drops below 80% (configurable via `-Deval.min.accuracy=0.90`).

### Execution Fidelity (`-Peval-fidelity`)

Analyzes session log files from prior migration runs to verify that the agent attempted expected tool calls. 
This test does **not** launch a migration -- it parses the JSONL session logs in `target/runs/`.

```bash
# Analyze all session logs in target/runs/
mvn test -Peval-fidelity

# Point to a specific directory
mvn test -Peval-fidelity -Deval.session.dir=path/to/runs

# Check a single session log
mvn test -Peval-fidelity -Deval.session.file=target/runs/spring-jpa-crud_default_full.json
```

Checks are defined in `src/test/resources/evals/execution-fidelity-tests.json`. 
Each check specifies:
- `session_log_pattern` -- glob pattern to match log files (e.g. `*_full.json`)
- `expected_tools` -- tool names that must appear in the log
- `min_calls` -- minimum number of times each tool should be called
- `required` -- when `false`, failures are reported as `[ADVISORY]` without failing the build

### Full Pipeline (`-Peval-all`)

Runs all three test types in sequence: trigger eval, migration, then fidelity analysis on the session logs just produced.

```bash
# Full pipeline for a specific project
mvn test -Peval-all \
  -Dai.agent=claude-acp \
  -Dai.projects=spring-jpa-crud \
  -Dai.timeout=600
```

The three phases execute in order within the Maven `test` phase:
1. **Trigger Eval** -- skill activates correctly for matching/non-matching prompts?
2. **Migration** -- runs the full migration against the specified project(s)
3. **Fidelity Eval** -- analyzes the session logs from step 2

If any phase fails, Maven stops (unless `-Dmaven.test.failure.ignore=true`).

## Two-directory migration model

The harness uses a **two-directory model** where the source project is read-only and the agent writes the migrated project into a separate target directory:

```
target/workdirs/
  spring-rest-api/          # source (read-only copy of the original project)
  spring-rest-api-quarkus/  # target (agent writes the migrated project here)
```

This separation enables:
- **Safe comparison** between source and target after migration
- **Deterministic verification**: checks always run against the target directory
- **Auditability**: the source is never modified, so you can diff source vs. target

The agent receives both paths in its prompt and is instructed not to modify the source. The build module copies the source into the target as its first step, then all subsequent modules transform files in the target.

### Standalone checks

You can re-run verification checks against an existing target directory without re-running the agent:

```bash
mvn exec:exec@checks -Dai.projects=spring-rest-api
```

This looks for `target/workdirs/<project>-quarkus/` and runs the checks defined in `project.yaml`.

## What Happens During a Test Run

Each test project goes through these phases:

1. **Prepare** -- copies local source or clones external repo into `target/workdirs/<project>/` (source) and creates an empty `target/workdirs/<project>-quarkus/` (target)
2. **Migrate** -- runs the AI agent with the migration skill, passing both source and target paths (output streams to console)
3. **Check** -- runs verification checks against the target directory (builds, tests pass, no Spring deps, has Quarkus, starts up)
4. **Review** -- resume a previous session and asks the agent to review the skill and suggest improvements (separate session, separate cost)
5. **Record** -- appends results to `target/runs/history.jsonl`

## Test Output

During a migration run, you'll see live-streamed output. The stream uses the AI agent JSON messages and shows
the messages, the tool executed, tokens and cost.

```
  ┌── step
  │ 
I'll start by loading the migration skill and exploring the project structure.
  │ 🔧 skill: migrate-spring-to-quarkus
  └── step end (tool-calls)  [tokens: 13511, cost: $0.0858]
  ┌── step
  │ Now let me analyze the project structure and load the reference files I'll need:
  │ 🔧 read: /Users/cmoullia/code/quarkus/rewrite-mtool/fork-quarkus-skills/tests/.opencode/skills/references/dependency-map.md
  │ 🔧 read: /Users/cmoullia/code/quarkus/rewrite-mtool/fork-quarkus-skills/tests/.opencode/skills/references/annotation-map.md
  │ 🔧 read: /Users/cmoullia/code/quarkus/rewrite-mtool/fork-quarkus-skills/tests/.opencode/skills/references/config-map.md
  │ 🔧 task: Explore Spring Boot project
  └── step end (tool-calls)  [tokens: 17154, cost: $0.0390]
```

## Run Artifacts

Each run generates artifacts which are stored in two locations:

**`target/runs/`** — run logs, named `<project>_<skill>_<model>_<strategy>.*`:

| File | Description                                           |
|------|-------------------------------------------------------|
| `<run>.json` | Raw JSON streaming output (every event from AI agent) |
| `<run>.pretty.md` | Human-readable log (what you see in the console)      |
| `<run>.report.md` | Report of the SKILL execution: info, usage, checks      |

Example filenames:
```
target/runs/
├── spring-boot-todo-app_google-vertex-anthropic_claude-opus-4-6-default_compatibility.json.log
├── spring-boot-todo-app_google-vertex-anthropic_claude-opus-4-6-default_compatibility.pretty.md
└── ses_<opencode-session-id>.session.jsonl
```

**`target/workdirs/<project>/`** -- read-only source copy; **`target/workdirs/<project>-quarkus/`** -- the migrated project (pom.xml, src/, etc.)

## Running Checks independently

The `CheckRunner` class (`io.quarkus.ai.harness.checks.CheckRunner`) runs the project verification checks (builds, tests-pass, no-spring-deps, etc.) **without re-running the AI agent**. This is useful when you want to re-verify a previous migration run after manual fixes.

It expects a prior agent run to have produced a work directory under `target/workdirs/<project-name>`.

### Using `mvn exec:exec@checks`

```bash
cd tests/

# Run checks on a specific project
mvn exec:exec@checks -Dai.projects=spring-rest-api

# Run checks on multiple projects
mvn exec:exec@checks -Dai.projects=spring-rest-api,spring-jpa-crud

# Run checks on all projects
mvn exec:exec@checks -Dai.enabled=all
```

### Running from an IDE

Create a **Run Configuration** with:

- **Main class:** `io.quarkus.ai.harness.checks.CheckRunner`
- **Working directory:** `tests/`
- **VM options:** `-Dai.projects=spring-rest-api`

If no work directory exists for a project (`target/workdirs/<project-name>`), CheckRunner reports an error and asks you to run the agent first.

## Test Projects

Each project has a `project.yaml` that can include a `test.enabled` flag. Projects with `enabled: false` (or without the flag set to `true`) are **skipped by default** when running `mvn test` — they are available for on-demand use but won't run in a blanket test execution.

### Enabled by default

These projects run when you execute `mvn test` (or `mvn clean test`) without any project filter:

| Project                | Description                                                                                             | Complexity | Checks |
|------------------------|---------------------------------------------------------------------------------------------------------|------------|--------|
| `spring-rest-api`      | REST controller + service + validation, no DB                                                           | Trivial    | builds, tests-pass, no-spring-deps, has-quarkus, starts-up |
| `spring-jpa-crud`      | CRUD with JPA, H2, Spring Data, custom queries                                                          | Low        | builds, tests-pass, no-spring-deps, has-quarkus, starts-up |
| `spring-boot-todo-app` | TODO application designed using REST Controller + Thymeleaf Web + Data REST and JPA, MySQL, Spring Data | Middle     | builds, tests-pass, no-spring-deps, has-quarkus, starts-up |
| `spring-petclinic`     | Classic PetClinic with Thymeleaf, JPA, caching (cloned at runtime)                                      | Medium     | builds, tests-pass, no-spring-deps, has-quarkus, starts-up, no-thymeleaf |
| `spring-petclinic-rest`| REST-only PetClinic, no templates (cloned at runtime)                                                   | Medium     | builds, tests-pass, no-spring-deps, has-quarkus, starts-up |

### Disabled by default (`test.enabled: false`)

These projects exist in `projects/` but are skipped unless explicitly requested. They are typically larger, experimental, or used for targeted testing only:

| Project                | Description                                                                                             |
|------------------------|---------------------------------------------------------------------------------------------------------|
| `dummy`                | Empty project for verifying agent setup                                                                 |
| `cargotracker`         | Eclipse Cargo Tracker — Jakarta EE / Spring Boot DDD application                                       |
| `coffee-shop`          | Simple coffeeshop Spring application                                                                    |
| `daytrader`            | DayTrader — benchmark application for stock trading                                                     |
| `jhipster-spring-boot` | JHipster Spring Boot sample application                                                                 |
| `realworld`            | RealWorld.io backend using Spring Security, Spring Data JPA                                             |

### Running disabled projects

There are three ways to include disabled projects:

```bash
# 1. Select a specific disabled project by name — bypasses the enabled flag
mvn test -Pintegration -Dai.projects=dummy

# 2. Select multiple projects (enabled or disabled) — bypasses the enabled flag
mvn test -Pintegration -Dai.projects=dummy,cargotracker,spring-rest-api

# 3. Include ALL projects regardless of enabled flag
mvn test -Pintegration -Dai.enabled=all
```

> [!NOTE]
> When using `-Dai.projects`, the `test.enabled` flag is ignored — the harness runs exactly the projects you asked for. The `-Dai.enabled=all` flag is useful when you want to run every project in the repository without listing them.

### Enabling a project permanently

To make a disabled project run by default, edit its `project.yaml` and either remove the `test` block or set `enabled: true`:

```yaml
# Before — disabled
test:
  enabled: false

# After — enabled (either form works)
test:
  enabled: true

# Or simply remove the test block entirely (defaults to enabled)
```

## Checks

After the AI agent completes the migration, the harness runs a series of verification checks to score the result. Each project declares which checks apply in its `project.yaml` file (see [Adding a Test Project](#adding-a-test-project)).

### Available checks

| Check | What it verifies |
|-------|-----------------|
| `builds` | `./mvnw compile` succeeds |
| `tests-pass` | `./mvnw test` succeeds |
| `no-spring-deps` | No `org.springframework` in `pom.xml` |
| `has-quarkus` | `io.quarkus` present in `pom.xml` |
| `starts-up` | App starts and responds to HTTP (port 18080) |
| `no-thymeleaf` | No Thymeleaf references remain in code or pom |

### Enabling / disabling checks

Checks are **enabled by default**. Use the `-DrunChecks` flag to control them:

```bash
# Run with checks (default)
mvn test -Pintegration -Dai.projects=spring-rest-api

# Skip checks — useful for quick smoke tests or when iterating on skills
mvn test -Pintegration -Dai.projects=spring-rest-api -DrunChecks=false

# Checks are also auto-disabled when the project has none defined (e.g. dummy)
mvn test -Pintegration -Dai.projects=dummy -Dai.prompt="Say Hello." -Dai.agent=claude
```

When checks are disabled, the console output shows the reason:

```
  checks:   disabled (runChecks=false)    ← user disabled via -DrunChecks=false
  checks:   disabled (none defined)       ← project has no checks in project.yaml
```

> [!NOTE]
> Disabling checks also skips the **skill review** step, since the review uses check results to evaluate the migration.

## Results Tracking

Results are appended to `target/runs/history.jsonl` — one JSON line per run:

```json
{
  "project": "spring-rest-api",
  "date": "2026-04-11T08:30:00Z",
  "model": "vertex-anthropic/claude-sonnet-4-5@20250929",
  "strategy": "full",
  "skill": "spring-boot-to-quarkus",
  "duration_seconds": 196,
  "usage": {"total_tokens": 321222, "total_cost": 0.3216, "api_calls": 22, "tool_calls": 78},
  "checks": {"builds": true, "tests-pass": true, "no-spring-deps": true, "has-quarkus": true, "starts-up": true},
  "score": "5/5",
  "review": {"tokens": 376929, "cost": 0.466, "summary": "The skill performed well..."}
}
```

## HTML Report

Generate a dashboard from all recorded runs:

```bash
# Generate report from default location
./scripts/report.sh

# Opens at target/runs/report.html
open target/runs/report.html
```

The report shows:

- **Summary stats** — total runs, perfect scores, tokens, cost, time
- **Score trends** — per project/model/strategy with visual score progression (3/5 → 4/5 → 5/5)
- **All runs detail** — expandable migration log and skill review for each run
- **Check pass rates** — bar chart showing how often each check passes across all runs
- **Cost comparison** — bar chart comparing costs across configurations

Re-run `./report.sh` after each test to update. The report is a single self-contained HTML file with no external dependencies.

## Adding a Test Project

### In-repo project (checked in, self-contained)

1. Create `tests/projects/<name>/source/` with the full Maven project
2. Make sure it builds and tests pass as a Spring Boot / Jakarta EE app
3. Create `tests/projects/<name>/project.yaml`:

```yaml
name: my-project
description: What migration patterns this tests
type: spring-boot
skill: spring-boot-to-quarkus
source: local
timeout: 300
checks:
  - builds
  - tests-pass
  - no-spring-deps
  - has-quarkus
  - starts-up
```

### External project (cloned from git)

```yaml
name: my-external-project
description: What migration patterns this tests
type: spring-boot
skill: spring-boot-to-quarkus
source: https://github.com/org/repo
ref: main
timeout: 600
checks:
  - builds
  - tests-pass
  - no-spring-deps
  - has-quarkus
```

## Troubleshooting

### Tests timeout

Increase the timeout: `-Dpi.timeout=900`. Complex projects like petclinic may need 10-15 minutes.

### Maven wrapper not found

Some test projects don't ship `mvnw`. The migration agent usually creates it, but if checks fail with "mvnw not found", the agent didn't get to that step (likely timed out).

### Port conflict on starts-up check

The `starts-up` check uses port 18080. If another process is using it, the check will fail. Kill any stale Quarkus dev processes:

```bash
lsof -i :18080 | grep LISTEN
kill <pid>
```
