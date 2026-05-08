# Test Harness

JUnit 5 test suite that runs migration skills against real Spring Boot / Jakarta EE projects, scores the results, and generates skill improvement reviews — all tracked over time.

## Prerequisites

- **Java 21+** — `java -version`
- **Maven 3.9+** — `mvn -version`
- **git** — for cloning external test projects

> [!IMPORTANT]
> At least one **AI agent installed** and provider configured (see [AI agent section](#ai-agent-and-provider) below)

## AI agent and provider

The test harness calls an Ai `agent` to run migrations. The AI agent needs credentials for whichever combination AI provider/model you want to test. 
The following table references the agent currently supported and refers to their documentation to install the agent and configure a provider using a subscription, API key, OAuth, etc

| Agent name                       | Description                     |                                                                                                              |
|----------------------------------|---------------------------------|--------------------------------------------------------------------------------------------------------------|
| [opencode](https://opencode.ai/) | OpenSource AI coding agent      | Default agent. See the list of the LLM [providers](https://opencode.ai/docs/providers/) supported            |
| [Pi](https://pi.dev)             | Minimal terminal coding harness | See the list of the [providers](https://pi.dev/docs/latest/providers) to configure them like the credentials |

> [!IMPORTANT] 
> Before to execute a test, verify that the AI agent can access the provider and the model selected

```bash
# Quick test — should produce a response
opencode run "Say hello in 5 different languages"
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

# Use compatibility migration strategy instead of full
mvn test -Dai.strategy=compatibility

# Override timeout (seconds)
mvn test -Dai.project=spring-petclinic -Dai.timeout=900

# Combine options
mvn test -Dai.project=spring-jpa-crud -Dai.provider=anthropic -Dai.model=claude-sonnet-4-5-20250514 -Dai.timeout=600
```

### Configuration Properties

The complete list of the configurations via `-D` flags:

| Property          | Default                                 | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|-------------------|-----------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ai.provider`     | `google-vertex-anthropic`               | Provider name (e.g. `anthropic`, `google`, `openai`, `vertex-anthropic`)                                                                                                                                                                                                                                                                                                                                                                                         |
| `ai.model`        | `claude-opus-4-6@default`               | Model ID (e.g. `claude-sonnet-4-5-20250514`, `gemini-2.5-pro`)                                                                                                                                                                                                                                                                                                                                                                                                   |
| `ai.strategy`     | `full`                                  | Migration strategy: `full` or `compatibility`. The strategy will tell to AI if we would like to migrate Spring Boot to Quarkus or using the Spring compatibility later which has been developed for some spring components like [DI](https://quarkus.io/guides/spring-di#more-spring-guides), [Web](https://quarkus.io/guides/spring-web), [Data JPA](https://quarkus.io/guides/spring-data-jpa), [Data REST](https://quarkus.io/guides/spring-data-rest),  etc. |
| `ai.prompt`       | see template migration message [here]() | Override the default migration prompt message when it is needed to test a new and different skills                                                                                                                                                                                                                                                                                                                                                               |
| `ai.timeout`      | `300`                                   | Timeout per project in seconds                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `ai.cmd`          | `opencode`                              | Path to the AI binary (if not on PATH)                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `ai.project`      | *(all)*                                 | Run only this project name                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `ai.skill`        | *(from project.yaml)*                   | Skill to use: a local name (e.g. `spring-boot-to-quarkus`) or a GitHub URL                                                                                                                                                                                                                                                                                                                                                                                       |
| `ai.skill.branch` | *(parsed from URL)*                     | Explicit branch — only needed when the branch name contains `/` and the URL has a subpath                                                                                                                                                                                                                                                                                                                                                                        |
| `ai.sanitize`     | `false`                                 | When `true`, pass `--sanitize` to strip sensitive content from exported opencode sessions                                                                                                                                                                                                                                                                                                                                                                        |

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

Here are some examples that we currently use for local tests with Google Vertex AI combining the system propserties and envirionment variables

1. Dummy project

```shell
// Use gcloud auth login to use OAuth authentication and generate locally the application_default_credentials.json file
export GOOGLE_APPLICATION_CREDENTIALS=~/.config/gcloud/application_default_credentials.json
export VERTEX_LOCATION=europe-west1
export GOOGLE_CLOUD_PROJECT=itpc-gcp-cp-pe-eng-claude
rm -rf target/runs

// Dummy test to verify if the Agent works, is well configured
mvn test \
  -Dai.project=dummy \
  -Dai.prompt="Say Hello." \
  -Dai.skill=dummy
```
Verify if there is under the following path `/target/workdirs/dummy` a `HELLO.md created !
  
2. Spring Boot TODO

The following example uses the local project: `Spring Boot TODO` and the strategy: `compatibility`
```bash
export GOOGLE_APPLICATION_CREDENTIALS=~/.config/gcloud/application_default_credentials.json
export VERTEX_LOCATION=europe-west1
export GOOGLE_CLOUD_PROJECT=itpc-gcp-cp-pe-eng-claude
rm -rf target/runs

mvn test \
    -Dai.project=spring-boot-todo-app \
    -Dai.strategy=compatibility \
    -Dai.provider=google-vertex-anthropic \
    -Dai.model=claude-opus-4-6@default \
    -Dai.skill=https://github.com/aureamunoz/quarkus-skills/tree/add-new-migration-from-spring/skills/migrate-spring-to-quarkus \
    -Dai.timeout=600
```
> [!NOTE] You can remove the `-Dai.***` system properties having default values !

## What Happens During a Test Run

Each test project goes through these phases:

1. **Prepare** — copies local source or clones external repo into `target/workdirs/<project>/`
2. **Migrate** — runs `AI` agent with the migration skill against the project (output streams to console)
3. **Check** — runs verification checks (builds, tests pass, no Spring deps, has Quarkus, starts up)
4  **Record** — appends results to `results/history.jsonl`

Future iterations of this project will propose some improvements and new steps such as:
**Review** — forks the migration session and asks agent to review the skill and suggest improvements (separate session, separate cost)

## Test Output

During the run, you'll see live-streamed output:

```
┌── turn
│ 🤖 assistant:
I'll migrate this Spring Boot project to Quarkus...
│    [tokens: 5000, cost: $0.0150]
│ 🔧 read: pom.xml
│ 🔧 edit: pom.xml (4 edits)
│ 🔧 bash: ./mvnw compile
└── turn end
```

## Run Artifacts

Artifacts are stored in two locations:

**`target/runs/`** — run logs and reviews, named `<project>_<model>_<strategy>.*`:

| File | Description                                                |
|------|------------------------------------------------------------|
| `<run>.json.log` | Raw JSON streaming output (every event from AI agent)      |
| `<run>.pretty.md` | Human-readable log (what you see in the console)           |
| `<run>.session.jsonl` | AI session file |

Example filenames:
```
target/runs/
├── spring-rest-api_claude-sonnet-4-5-20250514_full.json.log
├── spring-rest-api_claude-sonnet-4-5-20250514_full.pretty.md
├── spring-rest-api_claude-sonnet-4-5-20250514_full.session.jsonl
└── spring-rest-api_claude-sonnet-4-5-20250514_full.review.md
```

**`target/workdirs/<project>/`** — the migrated project source code (pom.xml, src/, etc.)

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

| Check | What it verifies |
|-------|-----------------|
| `builds` | `./mvnw compile` succeeds |
| `tests-pass` | `./mvnw test` succeeds |
| `no-spring-deps` | No `org.springframework` in `pom.xml` |
| `has-quarkus` | `io.quarkus` present in `pom.xml` |
| `starts-up` | App starts and responds to HTTP (port 18080) |
| `no-thymeleaf` | No Thymeleaf references remain in code or pom |

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
  "usage": {"total_tokens": 321222, "total_cost": 0.3216, "api_calls": 22},
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
