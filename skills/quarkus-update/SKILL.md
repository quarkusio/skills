---
name: quarkus-update
description: Use when working in a Quarkus project and the user wants to check if their build files are up-to-date, compare project structure against a reference, or upgrade their Quarkus version. Triggers on "check project", "update quarkus", "is my project up to date", "compare build", "quarkus upgrade".
---

# Quarkus Project Check

Check if a Quarkus project's build files are up-to-date by comparing against reference generated projects from [code-with-quarkus-compare](https://github.com/quarkusio/code-with-quarkus-compare).

## Step 1: Detect Build Tool and Version

Identify which build file exists in the project root:

| File | Build Tool | Tag prefix |
|------|-----------|------------|
| `pom.xml` | Maven | `maven-` |
| `build.gradle` | Gradle | `gradle-` |
| `build.gradle.kts` | Gradle Kotlin DSL | `gradle-kotlin-dsl-` |

Extract the Quarkus version:

- **Maven (`pom.xml`):** Look for the `<quarkus.platform.version>` property, or the version of `quarkus-bom` in `<dependencyManagement>`
- **Gradle (`build.gradle`):** Look for `quarkusPlatformVersion` in the `ext` block or `gradle.properties`
- **Gradle KTS (`build.gradle.kts`):** Look for `val quarkusPlatformVersion` or the BOM declaration

Construct the reference tag: `{tag_prefix}{version}` (e.g., `maven-3.15.7`).

## Step 2: Compare Build Files Against Reference

Fetch the reference build file:

```
https://raw.githubusercontent.com/quarkusio/code-with-quarkus-compare/{tag}/{build_file}
```

For example: `https://raw.githubusercontent.com/quarkusio/code-with-quarkus-compare/maven-3.32.4/pom.xml`

Compare the user's build file against the reference, focusing on:

- **Plugin versions and configurations** (compiler plugin, surefire, failsafe, quarkus-maven-plugin)
- **BOM setup** (dependency management structure)
- **Build properties** (Java version, encoding, surefire-plugin.version)
- **Wrapper scripts** (presence of `.mvnw`/`gradlew`, wrapper version)

**Ignore user-specific content** (do not flag these as differences):
- Custom dependencies not present in the reference
- groupId, artifactId, project name, version
- Custom profiles, modules, or build customizations
- Application-specific configuration

Report each meaningful difference with an explanation of what the reference project has and why it matters.

If the reference tag does not exist in the repository (404), inform the user that no reference is available for their specific version and suggest checking the [available tags](https://github.com/quarkusio/code-with-quarkus-compare/tags).

## Step 3: Check for Newer Quarkus Version

Fetch the list of tags for the user's build tool to find the latest available version:

```
https://github.com/quarkusio/code-with-quarkus-compare/tags
```

Or use git: `git ls-remote --tags https://github.com/quarkusio/code-with-quarkus-compare.git '{tag_prefix}*'`

Compare the user's version against the latest tag. If the user is already on the latest version, report that and stop here.

## Step 4: Upgrade Analysis (if outdated)

When a newer version is available, combine two sources of information:

### 4a: Run update dry-run

Run the dry-run using whichever tool is available:

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

### 4b: Fetch generator diff

Get the structural diff between the user's current version and the latest version using the GitHub compare view:

```
https://github.com/quarkusio/code-with-quarkus-compare/compare/{current_tag}...{latest_tag}
```

For example: `https://github.com/quarkusio/code-with-quarkus-compare/compare/maven-3.15.7...maven-3.32.4`

This reveals structural changes that `quarkus update` may not cover, such as:
- New plugin configurations (e.g., `<argLine>` additions)
- Surefire/failsafe version bumps
- Wrapper script updates
- Dockerfile changes
- New or removed boilerplate files

### 4c: Unified report

Present a combined report:

1. **Current status:** Build tool, current Quarkus version, latest available version
2. **What `quarkus update` would handle:** Summarize the patch (version bumps, dependency renames, config key migrations)
3. **What `quarkus update` does NOT cover:** Structural differences from the generator diff that are absent from the patch — these require manual attention
4. **Recommended actions:**
   - Apply the automated migrations using whichever tool is available: `quarkus update --yes`, `./mvnw quarkus:update -Drewrite`, or `./gradlew quarkusUpdate --rewrite`
   - Manually apply remaining structural changes identified from the generator diff
   - Link to the full comparison: `https://github.com/quarkusio/code-with-quarkus-compare/compare/{current_tag}...{latest_tag}`
