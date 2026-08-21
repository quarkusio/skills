# Test Harness

JUnit 5 test suite that runs migration skills against real projects, scores the results, and generates skill improvement reviews — all tracked over time.

## Prerequisites

- **Java 21+** — `java -version`
- **Maven 3.9+** — `mvn -version`
- **git** — for cloning external test projects

> [!IMPORTANT]
> At least one **AI agent installed** and provider configured (see [AI agent section](#ai-agent-and-provider) below)

## AI agent and provider

The test harness calls an Ai `agent` to run migrations. The AI agent needs credentials for whichever combination AI provider/model you want to test. 
The following table references the agent currently supported and refers to their documentation to install the agent and configure a provider using a subscription, API key, OAuth, etc

| Agent name | `ai.cmd` | Default provider | Default model | Description |
|---|---|---|---|---|
| [opencode](https://opencode.ai/) | `opencode` | `google-vertex-anthropic` | `claude-opus-4-6@default` | Default agent. See the list of the LLM [providers](https://opencode.ai/docs/providers/) supported |
| [Claude Code](https://docs.anthropic.com/en/docs/claude-code) | `claude` | _(n/a)_ | `claude-opus-4-6` | Uses Anthropic API directly. Set up via `claude login` or `ANTHROPIC_API_KEY` env var |
| [Pi](https://pi.dev) | `pi` | `vertex-anthropic` | `claude-opus-4-6` | See the list of the [providers](https://pi.dev/docs/latest/providers) to configure them like the credentials |

> [!IMPORTANT] 
> Before to execute a test, verify that the AI agent can access the provider and the model selected

```bash
# Quick test — should produce a response
opencode run "Say hello in 5 different languages"
claude -p "Say hello in 5 different languages"
pi -p "Say hello in 5 different languages"
```

## Running Tests

The process to execute the tests is pretty straightforward and just require to move under the `tests` folder, to set different system properties
and environment variables as described hereafter. 

> [!NOTE]
> The prompt message to perform migrated is defined part of the project's code and don't need to be changed except if you want to test new SKILLS or adapt the 
text to pass to LLM !

```bash
cd tests/

# Run all in-repo test projects with default model
mvn test

# Select the agent to be used. Default is: opencode
mvn test -Dai.cmd=claude
mvn test -Dai.cmd=pi

# Run a specific sample project
mvn test -Dai.project=spring-rest-api

# Set provider only (uses provider's default model)
mvn test -Dai.provider=google-vertex-anthropic // opencode ai agent & Google Vertex AI
mvn test -Dai.provider=vertex-anthropic        // pi ai agent & Google Vertex

# Set model only
mvn test -Dai.model=claude-opus-4-6@default // opencode ai agent & Google Vertex AI
mvn test -Dai.model=claude-opus-4-6         // pi ai agent & Google Vertex AI

# Set both provider and model explicitly (recommended for CI)
mvn test -Dai.provider=google-vertex-anthropic -Dai.model=claude-opus-4-6@default // opencode
mvn test -Dai.provider=vertex-anthropic -Dai.model=claude-opus-4-6 // pi

mvn test -Dai.provider=anthropic -Dai.model=claude-sonnet-4-5-20250514
mvn test -Dai.provider=openai -Dai.model=gpt-4o

# Claude Code agent (uses Anthropic API directly, provider is implicit)
mvn test -Dai.cmd=claude -Dai.model=claude-opus-4-6
mvn test -Dai.cmd=claude -Dai.model=claude-sonnet-4-5-20250514

# Use compatibility migration strategy instead of full
mvn test -Dai.strategy=compatibility

# Override timeout (seconds)
mvn test -Dai.project=spring-petclinic -Dai.timeout=900

# Combine options
mvn test -Dai.project=spring-jpa-crud -Dai.provider=anthropic -Dai.model=claude-sonnet-4-5-20250514 -Dai.timeout=600
```

### Configuration Properties

The complete list of the configurations via `-D` flags:

| Property          | Default                                                                                                                                                                                             | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|-------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ai.provider`     | `google-vertex-anthropic`                                                                                                                                                                           | Provider name (e.g. `anthropic`, `google`, `openai`, `vertex-anthropic`)                                                                                                                                                                                                                                                                                                                                                                                         |
| `ai.model`        | `claude-opus-4-6@default`                                                                                                                                                                           | Model ID (e.g. `claude-sonnet-4-5-20250514`, `gemini-2.5-pro`)                                                                                                                                                                                                                                                                                                                                                                                                   |
| `ai.strategy`     | `full`                                                                                                                                                                                              | Migration strategy: `full` or `compatibility`. The strategy will tell to AI if we would like to migrate Spring Boot to Quarkus or using the Spring compatibility later which has been developed for some spring components like [DI](https://quarkus.io/guides/spring-di#more-spring-guides), [Web](https://quarkus.io/guides/spring-web), [Data JPA](https://quarkus.io/guides/spring-data-jpa), [Data REST](https://quarkus.io/guides/spring-data-rest),  etc. |
| `ai.prompt`       | Migration prompt message declared [here](https://github.com/quarkusio/skills/blob/bec909505664bf3405c39542a402c4ee8e5c5cf1/tests/src/test/java/io/quarkus/migration/runner/OpenCodeRunner.java#L55) | Override the default migration prompt message when it is needed to test a new and different skills                                                                                                                                                                                                                                                                                                                                                               |
| `ai.timeout`      | `300`                                                                                                                                                                                               | Timeout per project in seconds                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `ai.cmd`          | `opencode`                                                                                                                                                                                          | Path to the AI binary (if not on PATH)                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `ai.project`      | *(all)*                                                                                                                                                                                             | Run only this project name                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `ai.projects`     | *(all)*                                                                                                                                                                                             | Comma-separated list of projects to test (e.g. `dummy,spring-rest-api`). Overrides `ai.project`                                                                                                                                                                                                                                                                                                                                                                  |
| `ai.skill`        | *(from project.yaml)*                                                                                                                                                                               | Skill to use: a local name (e.g. `spring-boot-to-quarkus`) or a GitHub URL                                                                                                                                                                                                                                                                                                                                                                                       |
| `ai.skills`       | *(from project.yaml)*                                                                                                                                                                               | Comma-separated list of skills for benchmark comparison (max 2). Overrides `ai.skill`                                                                                                                                                                                                                                                                                                                                                                            |
| `ai.skill.branch` | *(parsed from URL)*                                                                                                                                                                                 | Explicit branch — only needed when the branch name contains `/` and the URL has a subpath                                                                                                                                                                                                                                                                                                                                                                        |
| `runs`            | `1`                                                                                                                                                                                                 | Number of times to repeat the migration. Each run gets a fresh workdir, its own report, and a separate entry in `history.jsonl`. Useful for collecting data across multiple runs                                                                                                                                                                                                                                                                                 |
| `runChecks`       | `true`                                                                                                                                                                                              | When `false`, skip verification checks after migration. Also skipped when the project has no checks defined                                                                                                                                                                                                                                                                                                                                                      |
| `ai.review`       | `true`                                                                                                                                                                                              | When `false`, skip the skill review step after migration. Also skipped when checks are disabled or none defined                                                                                                                                                                                                                                                                                                                                                  |
| `ai.sanitize`     | `false`                                                                                                                                                                                             | When `true`, pass `--sanitize` to strip sensitive content from exported opencode sessions                                                                                                                                                                                                                                                                                                                                                                        |

### Selecting a skill

`ai.skill` accepts a local skill name or a GitHub URL pasted directly from the browser:

```bash
# Local skill by name (looked up in skills/)
mvn test -Dai.skill=jakarta-ee-to-quarkus

# Remote skill — paste the GitHub URL as-is
mvn test -Dai.skill=https://github.com/org/repo/tree/main/skills/custom-skill

# Remote skill on a feature branch (branch name has no slashes — URL is unambiguous)
mvn test -Dai.skill=https://github.com/org/repo/tree/new-feature-branch/skills/custom-skill

# Remote skill when branch name contains '/' — add ai.skill.branch to resolve ambiguity
mvn test -Dai.skill=https://github.com/org/repo/tree/branch/with/slashes/new-feature-skill \
         -Dai.skill.branch=branch/with/slashes
```

Remote clones are cached in `target/skills/` (or within the AI agent recommended folder) and cleaned with `mvn clean`.

### Examples

Here are some examples that we currently use for local tests with Google Vertex AI combining the system properties and environment variables

1. Dummy project

```shell
// Use gcloud auth login to use OAuth authentication and generate locally the application_default_credentials.json file
export GOOGLE_APPLICATION_CREDENTIALS=~/.config/gcloud/application_default_credentials.json
export VERTEX_LOCATION=your-google-cloud-location
export GOOGLE_CLOUD_PROJECT=your-google-cloud-project-id
rm -rf target/runs

// Dummy test to verify if the Agent works, is well configured
mvn test \
  -Dai.project=dummy \
  -Dai.skill=../tests/skills/dummy \
  -Dai.prompt="Say Hello."
  
// or using project.yaml definition
mvn test -Dai.project=dummy -Dai.prompt="Say Hello."  
```
Verify if there is under the following path `/target/workdirs/dummy` a `HELLO.md created !
  
2. Spring Boot TODO

The following example uses the local project: `Spring Boot TODO` and the strategy: `compatibility`
```bash
export GOOGLE_APPLICATION_CREDENTIALS=~/.config/gcloud/application_default_credentials.json
export VERTEX_LOCATION=your-google-cloud-location
export GOOGLE_CLOUD_PROJECT=your-google-cloud-project-id
rm -rf target/runs

mvn test \
    -Dai.project=spring-boot-todo-app \
    -Dai.strategy=compatibility \
    -Dai.provider=google-vertex-anthropic \
    -Dai.model=claude-opus-4-6@default \
    -Dai.skill=migrate-spring-to-quarkus \
    -Dai.timeout=600
```
> [!NOTE] You can remove the `-Dai.***` system properties having default values !

## Benchmark: comparing skills

Use `-Dai.skills` to benchmark up to 2 skills against one or more projects. Each skill is run independently with its own set of runs, and a global summary report with delta comparison is generated at the end.

### Single project, 2 skills

```bash
# Compare two skills on the same project with 5 runs each
mvn test \
  -Dai.project=spring-rest-api \
  -Dai.skills=migrate-spring-to-quarkus,migrate-spring-to-quarkus-mtool \
  -Dai.cmd=claude \
  -Druns=5
```

### Multiple projects, 2 skills

```bash
# Benchmark across multiple projects
mvn test \
  -Dai.projects=spring-rest-api,spring-jpa-crud \
  -Dai.skills=migrate-spring-to-quarkus,migrate-spring-to-quarkus-mtool \
  -Dai.cmd=claude \
  -Druns=3
```

### Multiple projects, single skill

```bash
# Test a single skill across several projects
mvn test \
  -Dai.projects=spring-rest-api,spring-jpa-crud,spring-boot-todo-app \
  -Dai.skill=migrate-spring-to-quarkus \
  -Druns=3
```

### Generated reports

When benchmarking with 2 skills, the harness generates:

- **Per-skill summary** (`target/runs/<project>_<skill>_*.summary.md`) — averages across runs for each skill+project combination
- **Global summary** (`target/runs/global.summary.md`) — side-by-side comparison with a delta row showing the percentage difference between the two skills

Example global summary output:

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

## What Happens During a Test Run

Each test project goes through these phases:

1. **Prepare** — copies local source or clones external repo into `target/workdirs/<project>/`
2. **Migrate** — runs `AI` agent with the migration skill against the project (output streams to console)
3. **Check** — runs verification checks (builds, tests pass, no Spring deps, has Quarkus, starts up)
4  **Record** — appends results to `results/history.jsonl`

Future iterations of this project will propose some improvements and new steps such as:
**Review** — forks the migration session and asks agent to review the skill and suggest improvements (separate session, separate cost)

## Test Output

During a migration run, you'll see live-streamed output. The stream uses the AI agent json messages and shows
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

**`target/runs/`** — run logs, named `<project>_<provider>_<model>_<strategy>.*`:

| File | Description                                           |
|------|-------------------------------------------------------|
| `<run>.json.log` | Raw JSON streaming output (every event from AI agent) |
| `<run>.pretty.md` | Human-readable log (what you see in the console)      |
| `<run>.session.jsonl` | AI agent session file                                 |

Example filenames:
```
target/runs/
├── spring-boot-todo-app_google-vertex-anthropic_claude-opus-4-6-default_compatibility.json.log
├── spring-boot-todo-app_google-vertex-anthropic_claude-opus-4-6-default_compatibility.pretty.md
└── ses_<opencode-session-id>.session.jsonl
```

**`target/workdirs/<project>/`** — the project source code (pom.xml, src/, etc.)

You can resume a migration session to inspect or continue using AI agent command:

```bash
// Opencode
opencode run -c // To continue the last session

// get the ids of the session and pick up the last or the one to be used
opencode session list --format json | jq '.[].id'
opencode run -s <ID> // The id of session to continue. You can get them using

// Pi AI agent 
pi --session target/runs/spring-rest-api_claude-sonnet-4-5-20250514_full.session.jsonl
```

## Test Projects

### In-Repo (self-contained, no external dependencies)

| Project                | Description                                                                                             | Complexity | Checks |
|------------------------|---------------------------------------------------------------------------------------------------------|------------|--------|
| `spring-rest-api`      | REST controller + service + validation, no DB                                                           | Trivial    | builds, tests-pass, no-spring-deps, has-quarkus, starts-up |
| `spring-jpa-crud`      | CRUD with JPA, H2, Spring Data, custom queries                                                          | Low        | builds, tests-pass, no-spring-deps, has-quarkus, starts-up |
| `spring-boot-todo-app` | TODO application designed using REST Controller + Thymeleaf Web + Data REST and JPA, MySQL, Spring Data | Middle     | builds, tests-pass, no-spring-deps, has-quarkus, starts-up |

### External (cloned at runtime)

| Project | Description | Complexity | Checks |
|---------|-------------|-----------|--------|
| `spring-petclinic` | Classic PetClinic with Thymeleaf, JPA, caching | Medium | builds, tests-pass, no-spring-deps, has-quarkus, starts-up, no-thymeleaf |
| `spring-petclinic-rest` | REST-only PetClinic, no templates | Medium | builds, tests-pass, no-spring-deps, has-quarkus, starts-up |

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
mvn test -Dai.project=spring-rest-api

# Skip checks — useful for quick smoke tests or when iterating on skills
mvn test -Dai.project=spring-rest-api -DrunChecks=false

# Checks are also auto-disabled when the project has none defined (e.g. dummy)
mvn test -Dai.project=dummy -Dai.prompt="Say Hello." -Dai.cmd=claude
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

Compare runs across models by grepping the history:

```bash
# See all runs
cat target/runs/history.jsonl | python3 -m json.tool --json-lines

# Compare scores across models
grep '"score"' target/runs/history.jsonl
```

All run artifacts live under `target/` and are cleaned with `mvn clean`.

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

### "No API key found" or authentication errors

Make sure your provider is configured. Run `pi --list-models` — if it shows models for your provider, credentials are working.

### pi hangs with no output

Pi requires a pseudo-TTY. The test harness handles this via `script -q /dev/null` on macOS/Linux. If you see hangs, check that the `script` command is available.

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
