# Speaker Notes — AI-Powered Spring to Quarkus Migration

**Event**: Tech Sales Enablement Session
**Date**: August 13, 2026
**Duration**: ~25 min presentation + Q&A (1 hour session total)
**Audience**: Tech Sales, Sales, customer-facing technical people (internal IBM)
**Recording**: Yes (Laura records for people who can't attend)

---

## Slide 1: Title

> **What to say**: "Hi everyone, thanks Laura for the invitation. Today I'm going to talk about something that keeps coming up in customer conversations — migrating from Spring Boot to Quarkus — and how we're using AI combined with deterministic tooling to solve it at scale. I'll show you a working tool, the collaboration with IBM Research, and at the end I have a specific ask."

---

## Slide 2: About Us

> **What to say**: "Quick intro. I'm Aurea Munoz and I work with Charles Moulliard on the Quarkus team. What's relevant here is our background — both of us come from productizing Spring Boot. We know Spring inside-out, and we know what it costs to migrate. Migration is the #1 friction point for Quarkus adoption. That's what motivated us to build this tool."

**Key points**:
- Aurea Munoz + Charles Moulliard, Quarkus team at IBM
- Both previously worked on productizing Spring Boot
- Deep expertise on both Spring Boot and Quarkus — understand the migration pain from experience
- This project is the result of that dual expertise

---

## Slide 3: The Problem

> **What to say**: "Let's start with the reality. Spring Boot is the most popular Java framework — 60%+ of enterprise Java apps use it. When customers want to move to Quarkus for the performance benefits, the cloud-native features, the faster startup, they hit a wall. Migration is manual, error-prone, and expensive. A typical medium app takes 2-4 weeks of developer time. That's a blocker to adoption."

**Key points**:
- Migration is the #1 friction point for Quarkus adoption
- Customers ask about it constantly — Laura mentioned you've had questions about this
- The manual process is: read Spring code, understand patterns, rewrite to Quarkus equivalents, fix build, fix tests, verify — per file, per annotation, per config property
- Enterprise apps have hundreds of files — it's not a weekend project

---

## Slide 4: What We Built — The Migration Skill

> **What to say**: "So we built an AI-powered migration skill. It's a set of instructions — a 'skill' — that tells an AI coding agent exactly how to migrate a Spring Boot application to Quarkus. Think of it as encoding the expertise of a Quarkus migration consultant into a reusable tool."

**Key points**:
- It's open source: `github.com/quarkusio/quarkus-skills`
- Works with Claude Code, and we're testing with other agents (opencode, pi)
- Not a one-shot prompt — it's a structured, modular, gate-driven process
- Two migration strategies (next slide)

---

## Slide 5: Two Migration Strategies

> **What to say**: "This is important for customer conversations. We offer two paths. Spring Compatibility mode uses Quarkus's own Spring compatibility extensions — quarkus-spring-web, quarkus-spring-data-jpa, and so on. The app keeps its Spring annotations but runs on Quarkus. Minimal code changes, lower risk. Full Quarkus mode replaces everything with native Quarkus APIs — JAX-RS, CDI, Panache. More work, but you get the full Quarkus experience and best performance. Most enterprise customers will start with compatibility mode and migrate to full Quarkus incrementally."

**Key points**:
- **Spring Compatibility** (`spring-compat`): uses `quarkus-spring-web`, `quarkus-spring-di`, `quarkus-spring-data-jpa`, etc. Keeps Spring annotations. Minimal risk.
- **Full Quarkus** (`full-quarkus`): replaces all Spring with JAX-RS, CDI, Panache. Full performance benefits.
- The tool asks the customer to choose, or it can be pre-configured for batch runs
- This is a differentiator vs other migration tools — we support both paths

---

## Slide 6: How It Works — Modular Gate-Driven Process

> **What to say**: "The skill doesn't just throw everything at the AI and hope for the best. It's modular — there are six modules: JDK verification, build migration, code migration, frontend/template migration, testing, and cleanup. Each module has a gate: a condition that's evaluated automatically. If there are no Thymeleaf templates, the frontend module is skipped. If there are no Spring tests, the testing module is skipped. After each module, it compiles the project to make sure nothing is broken before moving to the next one."

**Key points**:
- 6 modules: JDK check, Build, Code, Frontend, Testing, Cleanup
- Automatic gate evaluation — no manual intervention needed between modules
- Compile check after each module — never moves forward with a broken build
- Reference files with detailed mappings: dependency-map, annotation-map, config-map
- Git workflow: creates a branch per migration run, original code stays on main

**If asked about the reference files**:
- `dependency-map.md`: maps every Spring starter to its Quarkus equivalent
- `annotation-map.md`: maps Spring annotations to Quarkus/CDI/JAX-RS equivalents
- `config-map.md`: maps `application.properties` keys from Spring to Quarkus

---

## Slide 7: Real Results

> **What to say**: "Let me show you what this looks like in practice. Here's a migration of a Spring Boot TODO app — REST controllers, Thymeleaf templates, JPA with MySQL, Spring Data. The tool migrated everything in under 10 minutes, for a few dollars in AI cost. It replaced the Spring Boot parent with the Quarkus BOM, swapped all starters for Quarkus extensions, converted Thymeleaf templates to Qute, rewrote the controller from Spring MVC to JAX-RS, migrated the tests from SpringBootTest to QuarkusTest, and all checks pass — it compiles, tests pass, it starts up and serves requests. These are early results — we're still measuring systematically, but the direction is clear."

**Key data points from the actual run report**:
- Project: `spring-boot-todo-app` (REST + Thymeleaf + JPA + MySQL)
- Strategy: Spring Compatibility
- Duration: under 10 minutes
- Cost: a few dollars in AI tokens
- 5/5 modules completed
- All checks pass: builds, no Spring deps, has Quarkus, tests pass
- Even caught and removed an unused `jjwt` dependency
- **Note**: these are early results from individual runs, not systematic benchmarks

**If asked about more complex apps**:
- We've tested with PetClinic (Thymeleaf, JPA, caching) — it works
- IBM Research tested with CargoTracker, DayTrader, RealWorld — see collaboration slide

---

## Slide 8: Validated Against Real Migration

> **What to say**: "We didn't just test against hello-world apps. We ran the skill against the spring-quarkus-perf-comparison repository — that's a real app maintained by our performance team, with an existing bash script that does the same migration manually. The result? The skill produced functionally identical code, plus two improvements: it migrated the tests instead of deleting them, and it used the correct Quarkus naming convention for SQL files. The Java source was byte-for-byte identical."

**Key points**:
- Compared against Holly's `spring-conversion.sh` script
- Java source: identical output
- Skill improvements: preserved tests (script deletes them), correct SQL file naming
- One gap: container image extension not added (repo-specific, not migration-related)
- This is a real validation that the tool produces production-quality output

---

## Slide 9: IBM Research Collaboration

> **What to say**: "Now, here's where it gets really interesting. IBM Research — George Safta's team — has contributed a significant enhancement in PR 37. They bring three key innovations. First, deterministic validators: Java CLI tools that verify each migration phase without relying on the AI. Did the persistence layer migrate correctly? The validator checks programmatically. Second, a layer-by-layer approach: instead of migrating everything at once, they break it into 12 phases — database, persistence, services, messaging, REST, views, config — each validated independently. Third, they use IBM's CodeAnalyzer for static analysis of the source code before migration."

**Key points**:
- PR #37 by George Safta (IBM Research): +33,686 lines across 93 files
- 8 Java validators: Config, Database, Messaging, Persistence, ProjectSetup, REST, Service, UI
- Uses IBM's `codeanalyzer-java` for static code analysis
- Layer-by-layer migration with validation after each layer
- Resumable state — if a session dies, you can pick up where you left off

---

## Slide 10: Benchmark Results (IBM Research)

> **What to say**: "And here are the numbers. IBM Research benchmarked their approach against the scarfbench dataset — five real-world applications of varying complexity. The layer-by-layer approach with validators achieves 33 out of 33 on CargoTracker, where the plain AI approach without the skill only gets 24 out of 33. DayTrader, which was failing to deploy entirely, now deploys and passes 2 out of 30 functional tests — work in progress but the hardest part is deployment, and that's solved."

**Benchmark data** (from PR #37 description):

| Application | Plain AI (Opus 4.7) | AI + UltraCode (4.8) | Layer-wise + Validators (PR 37) |
|---|---|---|---|
| DayTrader | FAIL Deploy | PASS Deploy, 0/30 | PASS Deploy, 2/30 |
| CargoTracker | PASS, 24/33 | PASS, 0/33 | **PASS, 33/33** |
| Coffee-Shop | PASS, 5/9 | PASS, 5/9 | PASS, 5/9 |
| PetClinic | PASS, 12/13 | PASS, 3/13 | PASS, 11/13 |
| RealWorld | PASS, 33/62 | PASS, 33/62 | PASS, 33/62 |

> "The CargoTracker result is the headline — going from 24/33 to 33/33 means a complete, fully functional migration of a complex enterprise app."

---

## Slide 11: The Vision — mtool + AI = Hybrid Migration

> **What to say**: "Now let me talk about where we're heading. Pure AI migration works, as you've seen, but we want to make it cheaper and more deterministic. That's where mtool comes in. mtool is a deterministic tool — no AI involved — that analyzes a Spring Boot project and applies automated transformations: dependency mapping, import renaming, annotation swapping. The things that have a clear, mechanical rule. Then the AI skill handles what mtool can't: understanding controller logic, converting templates, making contextual decisions about migration strategy. We've done early testing of this combination and the results are promising — with mtool doing the analysis and heavy lifting upfront, the AI has better context and makes fewer mistakes. And the deterministic parts are reproducible — same input, same output, every time."

**Key points**:
- **mtool**: external deterministic tool that analyzes Spring code and applies automated transformations (deps, imports, annotations)
- **AI skill**: handles what requires understanding — logic, templates, contextual decisions
- **Hybrid = cheaper**: mtool does the mechanical work, AI focuses on the hard parts
- **Hybrid = more deterministic**: mechanical transformations produce the same result every time, no LLM variability
- Early testing shows: with mtool the AI gets a richer analysis upfront (scanner report) and more reliable transformations
- **Roadmap**: mtool as pre-migration step + IBM Research validators as post-migration step = deterministic sandwich around the AI

**If asked about mtool details**:
- It's a separate tool, not part of the AI skill
- Scans the project, generates a report (what needs to migrate), then applies what it can transform deterministically
- The skill reads the mtool report to know exactly what it needs to handle
- Think of it like OpenRewrite but purpose-built for Spring-to-Quarkus

---

## Slide 12: Quality at Scale — The Test Harness

> **What to say**: "How do we know this keeps working? We have a test harness. It's a JUnit 5 suite that takes a Spring Boot project, runs the migration skill against it using any AI agent, then runs six automated checks: does it compile, do tests pass, are Spring dependencies gone, is Quarkus present, does it start up, are templates migrated. Results are tracked over time in a dashboard. We can run this with different models, different strategies, with or without mtool — and compare results."

**Key points**:
- JUnit 5 test suite in `tests/`
- Works with multiple AI agents: opencode, pi (and Claude Code)
- Works with multiple models: Claude Opus, Claude Sonnet, Gemini, GPT-4o
- 6 automated checks per project
- Results tracked in `history.jsonl` with cost, tokens, duration
- HTML dashboard for comparison
- Test projects: spring-rest-api, spring-jpa-crud, spring-boot-todo-app, spring-petclinic

---

## Slide 13: The Ask — Pilot Customers

> **What to say**: "Here's where you come in. We need pilot customers. If you have a customer who's considering moving from Spring Boot to Quarkus — or who's stuck because the migration effort is too large — this tool could be exactly what they need. What we're looking for is a real enterprise application where we can run the migration, measure the results, and get feedback. In return, the customer gets a free, AI-assisted migration with hands-on support from our team."

**What we're looking for**:
- Customers with Spring Boot applications they want to migrate to Quarkus
- Ideally medium complexity: REST APIs, JPA, some templates, standard patterns
- Not looking for: massive monoliths as a first test (we'll get there)
- The customer would need to share the source code (or we work on their environment)

**What the customer gets**:
- Free AI-assisted migration with expert support
- Two strategy options: keep Spring annotations (low risk) or go full Quarkus
- Detailed migration report documenting every change
- Direct access to the team building the tool

**What we get**:
- Real-world validation on enterprise code
- Feedback to improve the skill and mtool
- A case study for future sales conversations
- Data on cost, time, and quality for different app profiles

> "So if you have a customer in mind, talk to me after this session or send me an email. Even if the app seems too complex for a first pilot, I'd love to hear about it — it helps us prioritize what to build next."

---

## Slide 14: Summary / Q&A

> **What to say**: "To summarize: we have a working, open-source AI tool that migrates Spring Boot to Quarkus in minutes instead of weeks. It supports both gradual and full migration paths. IBM Research is adding deterministic validation. And with mtool, we're working towards making the whole process cheaper and more reproducible — combining the best of deterministic tooling with the flexibility of AI. Early results are promising, and the next step is real customer applications to validate at scale — and that's where your help is invaluable. Questions?"

---

## Appendix: Anticipated Questions

### "How much does it cost to run?"
Early runs suggest it's in the range of a few dollars per migration, depending on app complexity and model used. We're still measuring systematically — that's one of the things we want to nail down with pilot customers. With mtool handling the mechanical parts, we expect the AI cost to drop further.

### "Does it work with Gradle?"
Yes, the skill supports Maven (`pom.xml`), Gradle Groovy (`build.gradle`), and Gradle Kotlin DSL (`build.gradle.kts`).

### "What about microservices / multi-module projects?"
Currently the skill works on single-module projects. Multi-module is on the roadmap. For microservices, you'd run it per service.

### "What AI models does it work with?"
Tested with Claude Opus 4.6, Claude Sonnet 4.5, and we're testing with Gemini and GPT-4o. Best results are with Claude Opus.

### "Can it run in CI/CD?"
Yes, the test harness already does this. You need AI API credentials configured. We're working on the CI integration story for customer environments.

### "What about Spring Security?"
Supported in both strategies. Compatibility mode uses `quarkus-spring-security`. Full mode migrates to `quarkus-security` + `quarkus-oidc`.

### "Does it handle database migration?"
The tool migrates the configuration and ORM layer. It doesn't modify the database schema or data — those typically don't need changes since JPA/Hibernate is the same underneath.

### "What about reactive Spring (WebFlux)?"
Not fully covered yet. This is an area where we'd welcome customer use cases to prioritize.

### "How does this compare to OpenRewrite?"
Complementary, and similar in spirit to mtool. OpenRewrite handles deterministic transformations (rename imports, update dependencies). mtool does the same but is purpose-built for Spring-to-Quarkus. Our AI skill handles the cases that need understanding: controller rewriting, template conversion, config migration with context. The vision is all three working together.

### "What if the migration fails?"
The git workflow creates a branch — main is always intact. The customer can review the migration report, see exactly what changed, and decide whether to accept, modify, or discard. It's non-destructive.

### "What's the difference between mtool and the IBM validators?"
mtool works **before** migration: it scans the Spring project and applies deterministic transformations. The IBM validators work **after** each migration phase: they verify the result is correct. Together they form a deterministic sandwich around the AI — mtool prepares, AI migrates, validators verify.

### General Quarkus questions (Laura mentioned these come up):
- Be ready for questions about Quarkus vs Spring Boot performance, native compilation, dev experience
- Point to quarkus.io for general Quarkus questions
- For migration-specific questions, this tool is the answer