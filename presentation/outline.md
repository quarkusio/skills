# Outline — AI-Powered Spring to Quarkus Migration

**Tech Sales Enablement** · August 13, 2026 · 25-30 min presentation + Q&A

---

## 1. About Us (2 min)
- **Aurea Munoz** and **Charles Moulliard**, Quarkus team at IBM.
- Background: both of us previously productized Spring Boot. We know Spring inside-out — we understand the migration pain because we've lived both sides.

## 2. The Problem (3 min)
- 60%+ of enterprise Java runs on Spring Boot. Migration to Quarkus is manual, expensive, and error-prone.
- A typical medium app takes 2-4 weeks of developer time.
- Migration is the #1 friction point for Quarkus adoption.

## 3. What We Built (5 min)
- Open source AI skill (`quarkusio/quarkus-skills`) that automates the migration.
- Two strategies: **Spring Compatibility** (low risk, uses `quarkus-spring-*`) and **Full Quarkus** (JAX-RS/CDI/Panache). The customer chooses.
- Modular process with automatic gates: JDK → Build → Code → Frontend → Testing → Cleanup.
- Compiles after every module — never moves forward with a broken build.

## 4. Real Results (5 min)
- TODO app (REST + Thymeleaf + JPA + MySQL): **migrated in under 10 min, a few dollars in AI cost**, all checks pass. Early results — still measuring systematically.
- Validated against the perf team's bash script (`spring-quarkus-perf-comparison`): Java source identical, skill improves on tests and naming conventions.

## 5. IBM Research — PR #37 (5 min)
- George Safta's team: +33K lines, 93 files.
- **Deterministic validators**: 8 Java CLIs that verify each phase without relying on AI.
- **Layer-by-layer migration** with a specialized agent per phase.
- **Benchmarks (scarfbench)**: CargoTracker goes from 24/33 → **33/33**. DayTrader goes from FAIL → deploys.

## 6. The Vision: mtool + AI = Hybrid (5 min)
- **mtool** is a deterministic tool that analyzes Spring code and applies automated transformations (dependencies, imports, annotations).
- What mtool can't handle (complex logic, templates, contextual decisions) is done by the AI.
- **Goal**: migrations that are cheaper (fewer AI tokens) and more deterministic (reproducible results).
- Early testing shows mtool provides richer analysis and more reliable transformations. We expect the total cost to drop on larger apps because mtool handles the heavy lifting.
- **Roadmap**: integrate mtool as a pre-migration step, combine with IBM Research validators as a post-migration step.

## 7. The Ask — Pilot Customers (3 min)
- Looking for customers with Spring Boot apps they want to migrate to Quarkus.
- The customer gets: free AI-assisted migration with hands-on support from our team.
- We get: real-world validation, feedback, and a case study.

## 8. Q&A (remaining time)

---

**Notes**: HTML slides at `presentation/slides.html`, detailed speaker notes at `presentation/speaker-notes.md`.