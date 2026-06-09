---
name: quarkus-update
description: Use when working in a Quarkus project and the user wants to check if their build files are up-to-date, compare project structure against a reference, or upgrade their Quarkus version. Triggers on "check project", "update quarkus", "is my project up to date", "compare build", "quarkus upgrade".
---

# Quarkus Project Check

Check if a Quarkus project is up-to-date by comparing its version against the latest release from the Quarkus registry, then running the built-in update tooling.

## Step 1: Detect Build Tool and Version

Identify which build file exists in the project root:

| File | Build Tool |
|------|-----------|
| `pom.xml` | Maven |
| `build.gradle` | Gradle |
| `build.gradle.kts` | Gradle Kotlin DSL |

Extract the Quarkus version:

- **Maven (`pom.xml`):** Look for the `<quarkus.platform.version>` property, or the version of `quarkus-bom` in `<dependencyManagement>`
- **Gradle (`build.gradle`):** Look for `quarkusPlatformVersion` in the `ext` block or `gradle.properties`
- **Gradle KTS (`build.gradle.kts`):** Look for `val quarkusPlatformVersion` or the BOM declaration

## Step 2: Check for Newer Quarkus Version

Fetch the latest stable release from GitHub:

```
https://api.github.com/repos/quarkusio/quarkus/releases/latest
```

Extract the `tag_name` field — this is the latest stable version (pre-releases are automatically excluded).

Compare the user's version against the latest release. If the user is already on the latest version, report that and stop here.

## Step 3: Upgrade Analysis (if outdated)

When a newer version is available, run the update dry-run using whichever tool is available:

| Tool | Command |
|------|---------|
| Quarkus CLI | `quarkus update --dry-run` |
| Maven wrapper | `./mvnw quarkus:update -DrewriteDryRun` |
| Maven | `mvn quarkus:update -DrewriteDryRun` |
| Gradle wrapper | `./gradlew quarkusUpdate --rewriteDryRun` |
| Gradle | `gradle quarkusUpdate --rewriteDryRun` |

This produces (without modifying any files):
- **Console output:** BOM update suggestions, extension sync status, matched migration recipes
- **`target/rewrite/rewrite.patch`** — the actual diff of changes the update would apply
- **`target/rewrite/rewrite.yaml`** — the full OpenRewrite recipe (version bumps + migration rules)
- **`target/rewrite/rewrite.log`** — detailed execution log

Read `target/rewrite/rewrite.patch` to understand what code-level changes the update covers.

## Step 4: Report

Present a combined report:

1. **Current status:** Build tool, current Quarkus version, latest available version
2. **What `quarkus update` would handle:** Summarize the patch (version bumps, dependency renames, config key migrations)
3. **Recommended actions:**
   - Apply the automated migrations using whichever tool is available: `quarkus update --yes`, `./mvnw quarkus:update -Drewrite`, or `./gradlew quarkusUpdate --rewrite`
