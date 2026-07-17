---
name: configuration-migration-agent
description: Phase 9 Configuration Migration Agent. Migrates Spring application.properties to Quarkus configuration.
  Converts @Configuration classes to CDI @Produces and validates with ConfigValidator.
license: Apache-2.0
metadata:
  phase: 9
  agent_type: migration
---

# Phase 9 — Configuration Migration Agent

## Purpose

Migrate Spring Boot configuration to Quarkus configuration.

## ⚠️ CRITICAL: Output File Location

**YOU MUST save the migration report to this exact location:**

```
<quarkus_target_dir>/migration-reports/phase-09-configuration-migration.json
```

**Before creating the report:**
1. Ensure the `migration-reports/` directory exists (create it if needed)
2. Save the report to the exact path above
3. Do NOT save to the root directory
4. Do NOT use any other filename

## Inputs

- migration-spec.yaml
- Source application.properties/application.yml
- Source @Configuration classes

## Steps

1. Convert application.properties/yml
2. Migrate @Configuration classes to CDI @Produces
3. Create lifecycle hooks (StartupEvent/ShutdownEvent)
4. Run `mvn clean package -DskipTests` to ensure compilation is successful
5. Create Dockerfile
6. Create docker-compose.yml
7. Update README.md
8. Generate configuration-migration-report.json

## Property Migration

```properties
# Spring → Quarkus
server.port → quarkus.http.port
spring.application.name → quarkus.application.name
spring.datasource.url → quarkus.datasource.jdbc.url
spring.jpa.hibernate.ddl-auto → quarkus.hibernate-orm.database.generation
spring.kafka.bootstrap-servers → kafka.bootstrap.servers
```

## @Configuration Migration

```java
// Before (Spring)
@Configuration
public class AppConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

// After (Quarkus)
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

@ApplicationScoped
public class AppConfig {
    @Produces
    public Client restClient() {
        return ClientBuilder.newClient();
    }
}
```

## Lifecycle Hooks

```java
@ApplicationScoped
public class AppLifecycle {
    void onStart(@Observes StartupEvent event) {
        // startup logic
    }
    
    void onStop(@Observes ShutdownEvent event) {
        // shutdown logic
    }
}
```


## Validation Gate

**Run validator after completing configuration migration:**

```bash
# Build validator if needed
cd validators/java
mvn clean package -DskipTests -q

# Extract config metadata (optional for Spring Boot migrations - validator can read application.properties directly)
java -jar target/migration-validator-1.0.0.jar extract server-config \
  <spring_source_dir> <spring_source_dir>/migration-metadata/spring-config-metadata.json
java -jar target/migration-validator-1.0.0.jar extract server-config \
  <quarkus_target_dir> <quarkus_target_dir>/migration-metadata/quarkus-config-metadata.json

# Run validator
java -jar target/migration-validator-1.0.0.jar validate config \
  <spring_source_dir> \
  <quarkus_target_dir> \
  <quarkus_target_dir>/migration-spec.yaml
```

**VALIDATION LOOP (MANDATORY - DO NOT SKIP):**
- If validator shows failures (exit code 1):
  1. Read error messages and identify issues
  2. Fix the problems in application.properties/@Configuration classes
  3. Rerun validator
  4. Repeat until exit code = 0 and Status = SUCCESS
- Only proceed to next phase when: `Rules: X total | X passed | 0 failed`

**Validator checks:** Property mappings (server.port→quarkus.http.port, spring.datasource.*→quarkus.datasource.*), @Configuration→@Produces, lifecycle hooks, no Spring properties remain
```

### Blocking Criteria
The following issues will block progression to Phase 10:
- Critical Spring properties not migrated to Quarkus equivalents
- @Configuration classes not migrated to @Produces
- Missing required Quarkus configuration properties
- Incorrect property value transformations

### Non-Blocking Warnings
These can be addressed later but should be documented:
- Unmapped Spring properties (may not be needed in Quarkus)
- Property value differences (verify intentional)
- Missing optional configuration

**⚠️ IMPORTANT: Do not proceed to Phase 10 until validation gate passes!**

## Output

**Directory Setup:**
```bash
mkdir -p migration-reports
```

**File Location:** `migration-reports/phase-09-configuration-migration.json`

This report should be created in the target Quarkus project at `<quarkus_target_dir>/migration-reports/phase-09-configuration-migration.json`.