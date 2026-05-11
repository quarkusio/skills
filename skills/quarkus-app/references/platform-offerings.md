# Quarkus Platform Offerings Reference

Use this guidance when:
- User mentions Quarkus platform offerings, such as Red Hat Build of Quarkus (RHBQ) or IBM Enterprise Build of Quarkus (IBQ) or other platform build of Quarkus.

---

Quarkus platform manage a set of extensions and dependencies.

They are grouped in a BOM (Bill of Materials) that is used to manage the dependencies of the platform.

The default platform is the community platform: `io.quarkus.platform:quarkus-bom`, each platform has its own root group id and artifact id.

| Platform | Root Group ID | Root Artifact ID | Maven Repository URL | Quarkus Registry URL | Offering name |
|---|---|---|---|---|---|  
| Quarkus Community | `io.quarkus.platform` | `quarkus-bom` |(maven central) | `https://registry.quarkus.io/` | (default/no-offering) |
| Red Hat Build of Quarkus (RHBQ)| `com.redhat.quarkus.platform` | `quarkus-bom` | `https://maven.repository.redhat.com/ga/` | `registry.quarkus.redhat.com` | redhat |
| Red Hat Build of Camel for Quarkus (RHCQ)| `com.redhat.quarkus.platform` | `camel-quarkus-bom` | `https://maven.repository.redhat.com/ga/` | `registry.quarkus.redhat.com` | camel |
| IBM Enterprise Build of Quarkus (IBQ)| `com.redhat.quarkus.platform` | `quarkus-bom` | `https://maven.repository.ibm.com/releases/` | `registry.quarkus.redhat.com` | ibm|


## Maven Repository 

Platforms most likely have their own Maven repository.

```xml
<repositories>
  <repository>
    <id>{platform-id}</id>
    <url>{platform-repository-url}</url>
  </repository>
</repositories>
```

Use the url from the table above.

## Quarkus Registry + Offering name

A registry is used to find the platform BOM and extensions and offering is used to identify which extensions are supported by that platform for a specific product offering. 

You specify this the simplest by putting the following in `.quarkus/config.yaml` (specific project) or `~/.config/quarkus/config.yaml` (globally):

```yaml
quarkus:
  registry:
    url: {quarkus-registry-url}
  offering:
    name: {offering-name}
```

## Platform BOM

When using a platform, you should use the root group id and artifact id of the platform in the managed dependencies.

By default, Quarkus projects uses properties to manage the platform:

Maven:

```xml
<properties>
  <quarkus.platform.artifact-id>{root-artifact-id}</quarkus.platform.artifact-id>
  <quarkus.platform.group-id>{root-group-id}</quarkus.platform.group-id>
  <quarkus.platform.version>{platform-version}</quarkus.platform.version>
</properties>
```

Gradle:

```properties
quarkusPluginVersion={quarkus-plugin-version}
quarkusPlatformGroupId={root-group-id}
quarkusPlatformArtifactId={root-artifact-id}
quarkusPlatformVersion={platform-version}
```

---

## Extension Support Scope

When listing extensions in a platform they might have different support scopes - differnt from the community status state.

Uou can filter by support scope:

```bash
quarkus ext ls --support-scope
```

Extensions are marked depending on the platform, following are examples but not exhaustive:
- **Supported** — Supported by the vendor, backed by subscription
- **Tech Preview** — available but not officially supported
- **Community** — from Quarkiverse, not covered by RHBQ subscription


