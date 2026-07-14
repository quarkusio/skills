---
name: migrate-simple-without-mtool
description: Migrates Spring Boot applications to Quarkus by reading source files directly.
  Analyze the code source by reading Java files. Use when the user wants to migrate, convert, or port a Spring Boot app to Quarkus, mentions "spring to quarkus",
  "quarkus migration", "replace spring", or asks about migrating "pom.xml", "build.gradle", "Spring MVC", "Spring Data JPA", "@SpringBootApplication".
license: Apache-2.0
metadata:
  author: Quarkus Community
---

# Spring Boot to Quarkus — Code Migration (without mtool)

Migrate Spring Boot Java source code to Quarkus by reading source files directly.

## Critical Rules

- **Scope: code migration only.** Do NOT migrate tests, frontend templates, static assets, or build plugins. Only migrate Java source files under `src/main/java/` and the build file (`pom.xml` or `build.gradle`). Leave everything else untouched.
- **Never delete code you cannot migrate.** Leave the original in place with a `// TODO: Migration required — <reason>` comment.
- **Don't break the build.** Compile after all changes are done.

## Step 1: Analyze the Project

### 1a. Identify the build tool and dependencies

Read `pom.xml` (Maven) or `build.gradle(.kts)` (Gradle) to identify the build tool and Spring dependencies.

### 1b. Discover all Java source files

Find all Java files in the project:

```bash
find src -name "*.java" -type f
```

### 1c. Read and catalog source files

Read each Java file under `src/main/java/`. For each file, note:
- The class name and package
- Which Spring annotations it uses (`@Component`, `@Service`, `@RestController`, `@Autowired`, `@Entity`, `@SpringBootApplication`, etc.)
- Which Spring imports it has (`org.springframework.*`)

### 1d. Identify what to migrate

From the source files, identify:
- Which files contain Spring annotations that need migration
- Whether there is a `@SpringBootApplication` main class to remove

Present a brief summary of what was found and what needs to change.

Load and follow [../shared/migration-steps.md](../shared/migration-steps.md) for Steps 2–3 (migrate code, report).
