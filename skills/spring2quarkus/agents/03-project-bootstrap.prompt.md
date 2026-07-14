---
name: project-bootstrap-agent
description: Phase 3 Project Bootstrap Agent. Creates Quarkus project structure with required extensions.
  Generates pom.xml, directory structure, and validates setup with ProjectSetupValidator.
license: Apache-2.0
metadata:
  phase: 3
  agent_type: bootstrap
---

# Phase 3 — Project Bootstrap Agent

## Purpose

Create the Quarkus project structure with appropriate extensions based on migration-spec.yaml.

## ⚠️ CRITICAL: Output File Location

**YOU MUST save the migration report to this exact location:**

```
<quarkus_target_dir>/migration-reports/phase-03-project-bootstrap.json
```

**Before creating the report:**
1. Ensure the `migration-reports/` directory exists (create it if needed)
2. Save the report to the exact path above
3. Do NOT save to the root directory
4. Do NOT use any other filename

## Inputs

- migration-spec.yaml

## Steps

1. Read migration-spec.yaml
2. Create target directory structure
3. Generate pom.xml with:
   - Quarkus BOM
   - Quarkus extensions from migration-spec.yaml
   - Java version from user decision
4. Create application.properties skeleton
5. Create directory structure:
   - src/main/java/<base-package>/
   - src/main/resources/
   - src/test/java/
   - src/test/resources/
6. Generate project-bootstrap-report.json
7. Run `mvn clean package -DskipTests` to verify setup and ensure compilation is successful
8. **Run validator and fix errors iteratively (CRITICAL):**
   ```bash
   # Build validator if needed
   cd validators/java
   mvn clean package -DskipTests -q
   
   # Run validator
   java -jar target/migration-validator-1.0.0.jar validate project-setup \
     <target_project_root> \
     <target_project_root>/migration-spec.yaml
   ```
   
   **VALIDATION LOOP (MANDATORY - DO NOT SKIP):**
   - If validator shows failures (exit code 1):
     1. Read error messages and identify issues
     2. Fix the problems in code/configuration
     3. Rerun validator
     4. Repeat until exit code = 0 and Status = SUCCESS
   - Only proceed to next phase when: `Rules: X total | X passed | 0 failed`
   - Validator checks: POM structure, Quarkus BOM, extensions, no Spring deps, directories, Maven compile

## pom.xml Template

```xml
<?xml version="1.0"?>
<project xsi:schemaLocation="http://maven.org/POM/4.0.0 https://maven.org/xsd/maven-4.0.0.xsd"
         xmlns="http://maven.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  
  <groupId><!-- from migration-spec --></groupId>
  <artifactId><!-- from migration-spec --></artifactId>
  <version><!-- from migration-spec --></version>
  
  <properties>
    <quarkus.version>3.15.1</quarkus.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
  </properties>
  
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>io.quarkus.platform</groupId>
        <artifactId>quarkus-bom</artifactId>
        <version>${quarkus.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  
  <dependencies>
    <!-- Quarkus extensions from migration-spec.yaml -->
  </dependencies>
  
  <build>
    <plugins>
      <plugin>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-maven-plugin</artifactId>
        <version>${quarkus.version}</version>
      </plugin>
    </plugins>
  </build>
</project>
```

## application.properties Skeleton

```properties
# Application
quarkus.application.name=<!-- from migration-spec -->

# HTTP
quarkus.http.port=8080

# Database (placeholder - will be populated in Phase 5)
# quarkus.datasource.db-kind=postgresql
# quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydb
# quarkus.datasource.username=user
# quarkus.datasource.password=password

# Hibernate ORM (placeholder - will be populated in Phase 5)
# quarkus.hibernate-orm.database.generation=update

# Messaging (placeholder - will be populated in Phase 7)
# kafka.bootstrap.servers=localhost:9092

# Logging
quarkus.log.console.enable=true
quarkus.log.console.level=INFO
```

## Output

**Directory Setup:**
```bash
mkdir -p migration-reports
```

**File Location:** `migration-reports/phase-03-project-bootstrap.json`

This report should be created in the target Quarkus project at `<quarkus_target_dir>/migration-reports/phase-03-project-bootstrap.json`.

Example:

```json
{
  "phase": "project-bootstrap",
  "status": "completed",
  "target_directory": "/path/to/target",
  "files_created": [
    "pom.xml",
    "src/main/resources/application.properties",
    "src/main/java/com/example/"
  ],
  "quarkus_version": "3.15.1",
  "java_version": "17",
  "extensions_added": [
    "quarkus-arc",
    "quarkus-rest-jackson",
    "quarkus-hibernate-orm-panache"
  ],
  "package_status": "PASS"
}