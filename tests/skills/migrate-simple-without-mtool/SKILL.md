---
name: migrate-simple-without-mtool
description: Migrates a Spring Boot application to Quarkus.
  Analyze the code source. Use when the user wants to migrate, convert, or port a Spring Boot app to Quarkus, mentions "spring to quarkus", "quarkus migration", "replace spring", or asks about migrating "pom.xml", "build.gradle", "Spring MVC", "Spring Data JPA", "@SpringBootApplication".
---

# Spring Boot to Quarkus — Code Migration (without mtool)

Simulate to migrate Spring Boot Java source code to Quarkus.

## Critical Rules

- **Non-interactive.** Do NOT ask the user any questions. Do NOT ask for strategy choices. Just run the steps below and report the results.
- **Scope: code migration only.** Do NOT migrate tests, frontend templates, static assets, or build plugins. Only migrate Java source files under `src/main/java/` and the build file (`pom.xml` or `build.gradle`). Leave everything else untouched.
- **Never delete code you cannot migrate.** Leave the original in place with a `// TODO: Migration required — <reason>` comment.
- **Don't compile the code** using maven or gradle.

## Step 1: Analyze the Project

### 1. Discover only the Java source files

Find all Java source files in the "src/main/java" project:

```bash
find src -name "src/main/java/*.java" -type f
```

For each Java file, note:
- The class name and package
- Which Spring annotations it uses (`@Component`, `@Service`, `@RestController`, `@Autowired`, `@Entity`, `@SpringBootApplication`, etc.)
- Which Spring imports it has (`org.springframework.*`)

### 2. Identify what to migrate

For each unique annotation discovered:
1. Look it up in `references/annotation-map.md` — find the matching row in the **Spring** column
2. Check the **Compat** column to find the Quarkus Spring compatibility annotation replacing it
3. Note the file path(s) from the report — these are the files where the change must be applied

Annotations that appear in annotation-map.md with "Same" or "Not needed" in the Quarkus column require no code change. Focus on annotations where the Quarkus equivalent differs (e.g. `@Autowired` → `@Inject`, `@Controller` → `@Path`, `@Service` → `@ApplicationScoped`).

Also check:
- Whether there is a `@SpringBootApplication` main class to remove
- Whether any annotation is marked **NOT supported** in the compat column — these require manual migration

## Step 2: Report

- Present a brief summary of what was found and what needs to change.

## Step 3. Stop

Stop the migration process as we are simulating and wanted to get the token usage