# Resumable migration state

- Status: proposed
- Date: 2026-10-13
- Issue: [#40](https://github.com/quarkusio/skills/issues/40)

## Context and Problem Statement

A migration runs many modules in sequence. If it is interrupted (agent crash, token limit, closed
session, compile failure the user wants to fix by hand), the only option today is to start over,
which repeats paid LLM work and loses decisions made during planning.

[ADR-0003](0003-file-schemas-for-new-migration-modules.md) §5 defined `migration-context.json`
with `currentModule` and `completedModules` and named resumability as a driver, but left open
**who writes it, when, and how a later run reads it**. In practice the file was updated by
prose instructions inside individual module `.md` files, and those instructions had already
diverged (`completedPhases` / `phaseReports` in `messaging-migration.md` versus
`completedModules` / `moduleReports` in the schema).

## Decision Drivers

- **Reliability.** State that is wrong is worse than no state. Whole-file JSON rewrites,
  timestamps, hashing, and set arithmetic are error-prone when done by an LLM.
- **Agent portability.** The skill runs under several agents with different hook mechanisms.
  Nothing can depend on an automatic trigger.
- **No new dependency.** This is a Java project: the JDK is already a hard prerequisite, so
  the tooling must run on it and on nothing else.
- **Human judgment stays with the agent.** Asking the user, retry policy, and compile-fix are
  not scriptable.

## Considered Options

### Hybrid: a deterministic Java program owns state, `SKILL.md` owns the protocol

Mechanical work is done by a program, and judgment calls stay with the agent.

- **Program (`scripts/Checkpoint.java`)** reads and writes `migration-context.json`, computes
  the source fingerprint, validates state, and calculates the pending-module list. These tasks
  are exact and repeatable, and they are the ones an LLM gets wrong silently.
- **Protocol (`SKILL.md`)** tells the agent when to call the program (around every module in
  the execution loop) and how to act on its verdicts: resume or start fresh, handle source
  drift, and run compile-fix.

`Checkpoint.java` is a Java single-file source program (JEP 330), run as
`java scripts/Checkpoint.java <command> ...`:

- It needs only the JDK that the prerequisite gate already requires. There is no build step,
  no `pom.xml`, no installed tooling, and no third-party library.
- It requires JDK 17 or later, which is the floor for every supported Quarkus version, so
  the existing JDK check already covers it and no additional prerequisite check is needed.

## Decision

### Ownership

| Concern | Owner |
|---|---|
| Read/write `migration-context.json`, atomic replace | `scripts/Checkpoint.java` |
| Source fingerprint, drift detection | `scripts/Checkpoint.java` |
| Pending-module computation, state validation | `scripts/Checkpoint.java` (`status` verdict) |
| Resume vs. fresh, drift decision, compile-fix | Agent, per "Resumable Migration" in `SKILL.md` |

Modules **must not** edit `migration-context.json` by hand. The execution protocol in
`SKILL.md` calls the program around every module, so individual modules do not carry
checkpoint logic.

### Schema additions to `migration-context.json`

Extends ADR-0003 §5. All additions are optional-with-default, so this is not a breaking change.

```jsonc
{
  "schema_version":     "1.0",
  "lastCheckpointAt":  "<ISO-8601>",
  "resumeCount":       0,                // incremented by `resume`
  "maxResumes":        5,                // `resume` refuses once resumeCount reaches this
  "sourceFingerprint": "<16 hex chars>", // sha256 prefix of <source> (top-level exclusions only:
                                         // .git, target, build, .gradle, migration-metadata)
  "strategy":          "<string | null>",       // decisions.strategy from migration-spec.yaml
  "strategySource":    "<string | null>",       // decisions.strategy_source from migration-spec.yaml
  "skippedModules":    ["<string>"],     // gate evaluated to SKIP

  // Per-module records — written by start and complete
  "modules": {
    "<module-name>": {
      "status":          "in_progress | completed | skipped",
      "gate":            "PASS | SKIP | ALWAYS",    // set by complete
      "gateReason":      "<string>",                 // human-readable reason, set by complete
      "startedAt":       "<ISO-8601>",               // set by start
      "finishedAt":      "<ISO-8601 | null>",        // set by complete
      "compile":         "PASS | FAIL | NOT_RUN",    // set by complete
      "compileAttempts": 0,                          // number of compile/fix retries; set by complete
      "statusDescription": "<string>"                // optional; why the status is what it is, e.g. why a
                                                     // module was skipped. Set by `complete` via
                                                     // --description; cleared by `start` and by `complete`
                                                     // when no description is given
    }
  }
}
```

`completedModules`, `currentModule`, and `moduleReports` keep the ADR-0003 meaning.
`moduleReports.<module>` is populated by `complete ... --report <relpath>`.

Tracked modules (`MODULE_ORDER`): `planning, build, code, messaging, frontend, testing, cleanup`.
`prerequisite`, verification, and `reporting` are not tracked because they re-run every time.

### Command interface

`java <skill>/scripts/Checkpoint.java <command> ...`


| Command | Effect |
|---|---|
| `init <target> <source>` | Create the context file. Reads `strategy` and `strategySource` from `migration-spec.yaml` if it already exists. Errors if the context file already exists. |
| `status <target>` | Verdict `NOT_FOUND`, `CORRUPT`, `COMPLETE`, or `RESUMABLE`, plus `strategy`, `pendingModules`, `modules`, `drift`, `specWarning`, `missingReports`, `resumeCount`, `maxResumes`, `resumeLimitReached`. |
| `start <target> <module>` | Set `currentModule`; write `modules.<module>.startedAt` and `status = in_progress`. |
| `complete <target> <module> <completed\|skipped> [--gate G] [--reason R] [--compile C] [--compile-attempts N] [--description D] [--report rel]` | Record outcome; update `modules.<module>` with gate, reason, compile status, finish time; idempotent. |
| `reset <target> <module>` | Clear the module and all later modules from all lists and from `modules`; null out their `moduleReports` entries. |
| `resume <target> [--accept-drift] [--max-resumes N]` | If `resumeCount >= maxResumes`: change nothing, print verdict `RESUME_LIMIT_REACHED`, exit 1. Otherwise increment `resumeCount`, clear `currentModule` (an interrupted module is re-run from its start), optionally refresh the fingerprint. `--max-resumes N` (1-9999) raises the limit. |

All commands print JSON.
### Semantics

- **Granularity is the module.** A module interrupted mid-run restarts from its beginning. This
  is safe because modules read `<source>` and write `<target>`.
- **A module is `completed` once the agent has run it and written its report.**
- **`<target>` decisions are final.** On resume, strategy, versions, and `decisions.*` come from
  `migration-spec.yaml` and are not re-asked.
- **Drift.** If `<source>` changed since the fingerprint was taken, completed modules may be stale.
  Interactive: ask. Non-interactive: start fresh.
- **Resume limit.** A migration that keeps dying (for example a module that always exhausts the
  token budget) would otherwise resume forever and burn LLM spend on each pass. `resume` allows
  `maxResumes` resumes (default 5). At the limit it refuses without touching state and returns
  `RESUME_LIMIT_REACHED`; the agent must stop, report to the user, and not run any further module.
  Only an explicit user approval to run `resume --max-resumes <n>` (n above the current count)
  lets the migration continue. The check is in the program rather than the protocol, so it holds
  even if the agent would otherwise keep retrying.
- **Corrupt state is never repaired by guessing.** The user chooses fresh or abort.
- **Destructive action.** `<target>` is deleted only after explicit confirmation or `resume: false`.
- **Control.** Skill argument `resume: true|false` or `.quarkus-migration.yml`; default is to ask
  (interactive) or resume (non-interactive).

### Fingerprint scope

Top-level directories named `.git`, `target`, `build`, `.gradle`, and `migration-metadata` are excluded. Subdirectories with those names (e.g. a Java package `com/example/build/`) are **included**, so drift in deep source code is detected.

## Consequences

### Positive

- An interrupted migration restarts at the first unfinished module without repeating LLM work or
  user questions.
- State handling is deterministic and unit-testable, independent of the model.
- One writer for the context file removes the `completedPhases` / `completedModules` divergence.

### Negative / Trade-offs

- `java Checkpoint.java` compiles the source in memory on every invocation, adding a few
  tenths of a second per call (about 0.4 s measured), and the protocol makes several calls
  per module.
- The agent must still call the program. The protocol makes the calls explicit steps in the module
  loop, but cannot force them; a skipped `complete` call makes that module re-run on resume
  (safe, only wasteful).
- The fingerprint hashes the whole source tree, which is O(source size) on every `status` and
  `init`.
- `reset` clears later modules conservatively, which can redo work the user wanted to keep.

### Neutral

- No change to `migration-spec.yaml` or per-module report schemas.
