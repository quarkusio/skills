# Quarkus Platform Offerings Reference

Use this guidance when:
- User mentions Quarkus platform offerings, such as Red Hat Build of Quarkus (RHBQ), IBM Enterprise Build of Quarkus (IBQ), Red Hat Build of Camel for Quarkus (RHCQ), or other platform build of Quarkus.

---

## Platform Summary

| Platform | Root Group ID | Root Artifact ID | Maven Repository URL | Quarkus Registry URL | Offering name |
|---|---|---|---|---|---|
| Quarkus Community | `io.quarkus.platform` | `quarkus-bom` | (maven central) | `https://registry.quarkus.io/` | (default/no-offering) |
| Red Hat Build of Quarkus (RHBQ) | `com.redhat.quarkus.platform` | `quarkus-bom` | `https://maven.repository.redhat.com/ga/` | `registry.quarkus.redhat.com` | redhat |
| Red Hat Build of Camel for Quarkus (RHCQ) | `com.redhat.quarkus.platform` | `camel-quarkus-bom` | `https://maven.repository.redhat.com/ga/` | `registry.quarkus.redhat.com` | camel |
| IBM Enterprise Build of Quarkus (IBQ) | `com.redhat.quarkus.platform` | `quarkus-bom` | `https://maven.repository.ibm.com/releases/` | `registry.quarkus.redhat.com` | ibm |

---

## Pre-Generation Setup

**You MUST complete this before running `quarkus create`.** The CLI uses the registry and offering config to
resolve the correct platform extensions and BOM.

### Create `.quarkus/config.yaml`

Create this file in the directory where you will run `quarkus create` (the CLI reads it from the current
working directory's `.quarkus/` folder):

```yaml
quarkus:
  registry:
    url: {quarkus-registry-url}
  offering:
    name: {offering-name}
```

Look up `{quarkus-registry-url}` and `{offering-name}` from the Platform Summary table above.

#### Examples

**RHBQ** — `.quarkus/config.yaml`:
```yaml
quarkus:
  registry:
    url: registry.quarkus.redhat.com
  offering:
    name: redhat
```

**IBQ** — `.quarkus/config.yaml`:
```yaml
quarkus:
  registry:
    url: registry.quarkus.redhat.com
  offering:
    name: ibm
```

**RHCQ (Camel)** — `.quarkus/config.yaml`:
```yaml
quarkus:
  registry:
    url: registry.quarkus.redhat.com
  offering:
    name: camel
```

Alternatively, set the config globally at `~/.config/quarkus/config.yaml` so all Quarkus CLI commands
default to the platform — useful if the user primarily works with one offering.

After creating the config, proceed with `quarkus create` as normal — the CLI will automatically use the
correct platform registry.

### Maven Plugin Fallback

When using the Maven plugin (Option B in Step 2), the `.quarkus/config.yaml` is also honored. However,
you must also use the platform's group ID in the plugin coordinates:

```bash
mvn {root-group-id}:quarkus-maven-plugin:{platform-version}:create \
  -DprojectGroupId={groupId} \
  -DprojectArtifactId={artifactId} \
  -DprojectVersion={version} \
  -Dextensions='{extensions}' \
  -DnoCode
```

Look up `{root-group-id}` from the Platform Summary table. Do **not** use `io.quarkus.platform` for
non-community platforms — it will resolve the wrong BOM.

You also need to configure the Maven repository (see Post-Generation Setup below, or globally
in `~/.m2/settings.xml`) so Maven can resolve the platform artifacts.

---

## Post-Generation Setup

After `quarkus create` completes, verify the generated project has the correct platform configuration.
If it doesn't, apply these fixes:

### 1. Verify BOM coordinates

Check that the generated `pom.xml` (or `gradle.properties`) uses the correct platform group ID and artifact ID.

**Maven** — verify these properties in `pom.xml`:
```xml
<properties>
  <quarkus.platform.artifact-id>{root-artifact-id}</quarkus.platform.artifact-id>
  <quarkus.platform.group-id>{root-group-id}</quarkus.platform.group-id>
  <quarkus.platform.version>{platform-version}</quarkus.platform.version>
</properties>
```

**Gradle** — verify these in `gradle.properties`:
```properties
quarkusPlatformGroupId={root-group-id}
quarkusPlatformArtifactId={root-artifact-id}
quarkusPlatformVersion={platform-version}
```

Look up `{root-group-id}` and `{root-artifact-id}` from the Platform Summary table. If they still show
community values (`io.quarkus.platform`), update them to the correct platform values.

### 2. Add Maven repository

Non-community platforms require their own Maven repository. This can be configured per-project or globally:

**Option A: Per-project** — add to `pom.xml` if not already present:

```xml
<repositories>
  <repository>
    <id>{platform-id}</id>
    <url>{maven-repository-url}</url>
    <releases>
      <enabled>true</enabled>
    </releases>
  </repository>
</repositories>
<pluginRepositories>
  <pluginRepository>
    <id>{platform-id}-plugins</id>
    <url>{maven-repository-url}</url>
    <releases>
      <enabled>true</enabled>
    </releases>
  </pluginRepository>
</pluginRepositories>
```

Look up `{maven-repository-url}` from the Platform Summary table. Use a descriptive `{platform-id}`
(e.g., `redhat-ga`, `ibm-releases`).

For **Gradle**, add to `settings.gradle` or `build.gradle`:
```groovy
repositories {
    mavenCentral()
    maven { url '{maven-repository-url}' }
}
```

**Option B: Global** — add to `~/.m2/settings.xml` instead, which applies to all Maven projects.
This is useful if the user primarily works with one platform and doesn't want to repeat the repository
config in every project. Ask the user which approach they prefer.

### 3. Ensure config.yaml is in the project

If the generated project does not already contain `.quarkus/config.yaml`, move or copy the one you
created in Pre-Generation Setup into the project directory:

```bash
mv .quarkus/config.yaml {artifactId}/.quarkus/config.yaml
```

This ensures future `quarkus` CLI commands (adding extensions, updating, dev mode) use the correct
platform registry. This step is unnecessary if the user has set the config globally at
`~/.config/quarkus/config.yaml`.

---

## Extension Support Scope

When listing extensions in a platform they might have different support scopes — different from the
community status state.

You can filter by support scope:

```bash
quarkus ext ls --support-scope
```

Extensions are marked depending on the platform, following are examples but not exhaustive:
- **Supported** — Supported by the vendor, backed by subscription
- **Tech Preview** — available but not officially supported
- **Community** — from Quarkiverse, not covered by the platform subscription
