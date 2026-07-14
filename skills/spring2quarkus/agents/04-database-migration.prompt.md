---
name: database-migration-agent
description: Phase 4 Database Migration Agent. Migrates database initialization files (import.sql, schema.sql) and configures datasource.
  Validates with DatabaseMigrationValidator to ensure proper database setup.
license: Apache-2.0
metadata:
  phase: 4
  agent_type: migration
---

# Phase 4 — Database Migration Agent

## Purpose

Migrate database initialization files (schema.sql, data.sql) from Spring Boot to Quarkus and configure the datasource based on the database type chosen in Phase 2 (Migration Planning).

## ⚠️ CRITICAL: Output File Location

**YOU MUST save the migration report to this exact location:**

```
<quarkus_target_dir>/migration-reports/phase-04-database-migration.json
```

**Before creating the report:**
1. Ensure the `migration-reports/` directory exists (create it if needed)
2. Save the report to the exact path above
3. Do NOT save to the root directory
4. Do NOT use any other filename

## Inputs

- migration-spec.yaml (database type from Phase 2)
- Source database files (schema.sql, data.sql, schema-{db}.sql, etc.)
- Source application.yml/properties (datasource configuration)

## Outputs

- import.sql in src/main/resources (Quarkus standard)
- Quarkus datasource configuration in application.properties
- Database migration report (JSON) at `migration-reports/phase-04-database-migration.json`

## Migration Approach

Quarkus uses a simple, standard approach for database initialization:
- **import.sql** - Placed in `src/main/resources/`, automatically executed on startup
- Works for all databases (H2, PostgreSQL, MySQL, MariaDB, etc.)
- Combines schema and data in one file

## Step 1: Locate Source Database Files

Common locations in Spring Boot projects:
```
src/main/resources/schema.sql
src/main/resources/data.sql
src/main/resources/schema-h2.sql
src/main/resources/data-h2.sql
src/main/resources/schema-postgresql.sql
src/main/resources/import.sql (if already exists)
```

## Step 2: Read Database Type from Migration Spec

```yaml
# migration-spec.yaml
database:
  type: "h2"  # or "postgresql", "mysql", "mariadb"
  source_files:
    - "src/main/resources/schema-h2.sql"
    - "src/main/resources/data.sql"
```

## Step 3: Combine SQL Files into import.sql

Merge all schema and data files into a single `import.sql`:

```sql
-- import.sql
-- Database initialization for Quarkus
-- Migrated from Spring Boot: schema-h2.sql, data.sql
-- Database type: H2

-- Schema definitions
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    total DECIMAL(10,2),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Initial data
INSERT INTO users (id, username, email) VALUES (1, 'admin', 'admin@example.com');
INSERT INTO users (id, username, email) VALUES (2, 'user', 'user@example.com');
```

**Important Notes:**
- Use `CREATE TABLE IF NOT EXISTS` for idempotency
- Include all schema definitions first, then data
- Preserve comments from source files
- Ensure foreign key constraints are in correct order

## Step 4: Migrate Datasource Configuration

### Spring Boot → Quarkus Property Mapping

| Spring Property | Quarkus Property | Notes |
|----------------|------------------|-------|
| `spring.datasource.url` | `quarkus.datasource.jdbc.url` | Direct mapping |
| `spring.datasource.username` | `quarkus.datasource.username` | Direct mapping |
| `spring.datasource.password` | `quarkus.datasource.password` | Direct mapping |
| `spring.datasource.driver-class-name` | `quarkus.datasource.jdbc.driver` | Usually auto-detected |
| `spring.datasource.hikari.maximum-pool-size` | `quarkus.datasource.jdbc.max-size` | Connection pool |
| `spring.datasource.hikari.minimum-idle` | `quarkus.datasource.jdbc.min-size` | Connection pool |
| `spring.jpa.hibernate.ddl-auto` | (remove) | Use import.sql instead |
| `spring.sql.init.mode` | (remove) | import.sql auto-runs |
| `spring.sql.init.schema-locations` | (remove) | import.sql auto-runs |
### CRITICAL: Hibernate ORM Configuration for import.sql Execution

**⚠️ IMPORTANT QUARKUS BEHAVIOR:**
Quarkus will **completely disable Hibernate ORM** if no JPA entities are present in the project. When Hibernate ORM is disabled, `import.sql` will **NOT execute**, even if the file exists and datasource is configured correctly.

**Why This Matters:**
- Phase 4 (Database Migration) happens BEFORE Phase 5 (Persistence Migration)
- At this stage, no entities have been migrated yet
- Therefore, `import.sql` will not execute until Phase 5 completes
- This is expected Quarkus behavior, not a configuration error

**Required Configuration:**
You MUST add the following Hibernate ORM configuration to `application.properties` to prepare for entity migration in Phase 5:

```properties
# Hibernate ORM configuration (required for import.sql execution)
# Note: import.sql will only execute after entities are migrated in Phase 5

# Development mode: Drop and recreate schema from entities, then run import.sql
%dev.quarkus.hibernate-orm.database.generation=drop-and-create

# Production mode: No schema generation (use Flyway/Liquibase for production)
%prod.quarkus.hibernate-orm.database.generation=none

# Enable SQL logging to verify import.sql execution
quarkus.hibernate-orm.log.sql=true
```

**Configuration Options for `quarkus.hibernate-orm.database.generation`:**
- `none` - No schema generation (default for production)
- `create` - Create schema on startup, don't drop existing
- `drop-and-create` - Drop existing schema and recreate (recommended for dev with import.sql)
- `update` - Update schema to match entities (use with caution)
- `validate` - Validate schema matches entities, fail if not

**Runtime Verification:**
- Static validation in Phase 4 can only verify file existence and configuration
- **Actual import.sql execution verification happens in Phase 5** after entities are migrated
- Phase 5 agent will start the application and verify SQL statements in logs


### Database-Specific Configuration

**H2 Database:**
```properties
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:file:./data/testdb;DB_CLOSE_DELAY=-1
quarkus.datasource.username=sa
quarkus.datasource.password=

# Optional: H2 console for development
quarkus.datasource.jdbc.url=jdbc:h2:mem:testdb
%dev.quarkus.hibernate-orm.database.generation=drop-and-create
```

**PostgreSQL:**
```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydb
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres

# Connection pool
quarkus.datasource.jdbc.max-size=20
quarkus.datasource.jdbc.min-size=5
```

**MySQL:**
```properties
quarkus.datasource.db-kind=mysql
quarkus.datasource.jdbc.url=jdbc:mysql://localhost:3306/mydb
quarkus.datasource.username=root
quarkus.datasource.password=root

# MySQL specific
quarkus.datasource.jdbc.url=jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=UTC
```

**MariaDB:**
```properties
quarkus.datasource.db-kind=mariadb
quarkus.datasource.jdbc.url=jdbc:mariadb://localhost:3306/mydb
quarkus.datasource.username=root
quarkus.datasource.password=root
```

## Step 5: SQL Syntax Adjustments

### Common Adjustments by Database Type

**H2:**
- `AUTO_INCREMENT` → `GENERATED BY DEFAULT AS IDENTITY`
- `DATETIME` → `TIMESTAMP`
- `MERGE INTO` statements work as-is

**PostgreSQL:**
- `AUTO_INCREMENT` → `SERIAL` or `BIGSERIAL`
- Use `BIGSERIAL` for `BIGINT` primary keys
- `DATETIME` → `TIMESTAMP`

**MySQL/MariaDB:**
- `AUTO_INCREMENT` works as-is
- `DATETIME` works as-is
- Ensure `ENGINE=InnoDB` if specified

### Example Conversion

**Before (Spring Boot - schema-h2.sql):**
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME,
    username VARCHAR(255)
);
```

**After (Quarkus - import.sql for H2):**
```sql
CREATE TABLE IF NOT EXISTS users (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    created_at TIMESTAMP,
    username VARCHAR(255)
);
```

**After (Quarkus - import.sql for PostgreSQL):**
```sql
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    username VARCHAR(255)
);
```

## Step 6: Add Required Dependencies

Ensure correct JDBC driver in pom.xml based on database type:

**H2:**
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-h2</artifactId>
</dependency>
```

**PostgreSQL:**
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-postgresql</artifactId>
</dependency>
```

**MySQL:**
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-mysql</artifactId>
</dependency>
```

**MariaDB:**
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-mariadb</artifactId>
</dependency>
```

## Migration Workflow

1. **Read migration-spec.yaml** to get database type
2. **Locate source SQL files** (schema.sql, data.sql, etc.)
3. **Combine into import.sql** with appropriate syntax for database type
4. **Extract datasource config** from Spring application.yml/properties
5. **Convert to Quarkus properties** in application.properties
6. **Add JDBC driver dependency** if not already present
7. **Generate migration report**

## Example Migration

**Source (Spring Boot):**
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:h2:file:./data/testdb
    username: sa
    password:
  sql:
    init:
      mode: always
      schema-locations: classpath:schema-h2.sql
```

**Target (Quarkus):**
```properties
# application.properties
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:file:./data/testdb
quarkus.datasource.username=sa
quarkus.datasource.password=

# import.sql automatically detected and executed
```

## Validation Points

After migration, verify:
1. ✅ import.sql exists in src/main/resources
2. ✅ import.sql contains both schema and data
3. ✅ SQL syntax is compatible with target database
4. ✅ Datasource configuration is complete
5. ✅ Correct JDBC driver dependency is present
6. ✅ No Spring datasource properties remain

## Report Generation

**Directory Setup:**
```bash
mkdir -p migration-reports
```

**File Location:** `migration-reports/phase-04-database-migration.json`

Generate the report in the target Quarkus project at `<quarkus_target_dir>/migration-reports/phase-04-database-migration.json`:

```json
{
  "phase": "database-migration",
  "status": "success",
  "database_type": "h2",
  "files_migrated": [
    {
      "source": ["schema-h2.sql", "data.sql"],
      "target": "import.sql",
      "lines": 150,
      "tables_created": 5
    }
  ],
  "datasource_config": {
    "db_kind": "h2",
    "url": "jdbc:h2:file:./data/testdb",
    "pool_max_size": 20
  },
  "dependencies_added": ["quarkus-jdbc-h2"],
  "sql_adjustments": [
    "Converted AUTO_INCREMENT to GENERATED BY DEFAULT AS IDENTITY",
    "Converted DATETIME to TIMESTAMP"
  ],
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Error Handling

### Common Issues

1. **Multiple schema files for different databases:**
   - Use the one matching the chosen database type
   - Example: If PostgreSQL chosen, use schema-postgresql.sql

2. **Large data files:**
   - If data.sql > 10MB, warn user
   - Consider loading data programmatically instead

3. **Database-specific SQL:**
   - Flag syntax that may not be portable
   - Provide conversion suggestions

4. **Missing datasource config:**
   - Use sensible defaults based on database type
   - Flag for user review

## Validation

**Run validator after completing database migration:**

```bash
# Build validator if needed
cd validators/java
mvn clean package -DskipTests -q

# Run validator
java -jar target/migration-validator-1.0.0.jar validate database \
  <target_project_root> \
  <target_project_root>/migration-spec.yaml
```

**VALIDATION LOOP (MANDATORY - DO NOT SKIP):**
- If validator shows failures (exit code 1):
  1. Read error messages and identify issues
  2. Fix the problems in SQL files/configuration
  3. Rerun validator
  4. Repeat until exit code = 0 and Status = SUCCESS
- Only proceed to next phase when: `Rules: X total | X passed | 0 failed`

**Validator checks:** import.sql exists, datasource config complete, no Spring properties, JDBC driver present, Hibernate ORM config, Maven compile

**⚠️ Note:** Static validation only - runtime import.sql execution verified in Phase 5 after entities are migrated
- Detailed evidence for each check provided in output

## Success Criteria

- [ ] import.sql created with combined schema and data
- [ ] Datasource configuration migrated to application.properties
- [ ] Correct JDBC driver dependency added
- [ ] SQL syntax adjusted for target database
- [ ] No Spring datasource properties remain
- [ ] Database migration report generated
- [ ] **Validator passes all checks (exit code 0)**

## Next Phase

After successful database migration and validation, proceed to:
- **Phase 5: Persistence Migration** - Migrate JPA entities and repositories

The database foundation is now ready for entity and repository migration, and can be tested immediately.