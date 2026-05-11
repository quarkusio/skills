# Red Hat Build of Quarkus (RHBQ) Reference

Use this guidance when:
- User mentions Red Hat support, RHBQ, or Red Hat subscription
- User is deploying to Red Hat OpenShift in production and wants supported runtimes
- User needs a 3-year support lifecycle instead of community 12-month LTS

---

## RHBQ vs Community Quarkus

| Aspect | Community Quarkus | RHBQ |
|---|---|---|
| Latest version cadence | ~4–6 weeks | Aligned to community LTS |
| Support lifecycle | 12 months (LTS) | 3 years |
| Requires Red Hat subscription | No | Yes (for production) |
| Extension ecosystem | Full platform + Quarkiverse | Certified subset |
| Current stable | 3.32.x | 3.27.x |

---

## RHBQ Maven BOM

When using RHBQ, replace the community `io.quarkus.platform:quarkus-bom` with the Red Hat BOM:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.redhat.quarkus.platform</groupId>
      <artifactId>quarkus-bom</artifactId>
      <version>3.27.0.redhat-00001</version>  <!-- check for latest RHBQ version -->
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

---

## RHBQ Maven Repository

RHBQ artifacts are in the Red Hat Maven repository. Add to `settings.xml` or `pom.xml`:

```xml
<repositories>
  <repository>
    <id>redhat</id>
    <url>https://maven.repository.redhat.com/ga/</url>
    <releases><enabled>true</enabled></releases>
    <snapshots><enabled>false</enabled></snapshots>
  </repository>
</repositories>
```

---

## RHBQ Extension Support Scope

When listing extensions with RHBQ, you can filter by support scope:

```bash
quarkus ext ls --support-scope
```

Extensions are marked as:
- **Supported** — Red Hat-supported, backed by subscription
- **Tech Preview** — available but not officially supported
- **Community** — from Quarkiverse, not covered by RHBQ subscription

---

## Resources

- RHBQ product page: https://access.redhat.com/products/quarkus
- RHBQ release notes: https://access.redhat.com/documentation/en-us/red_hat_build_of_quarkus
- RHBQ lifecycle: https://access.redhat.com/support/policy/updates/jboss_notes/#p_quarkus
