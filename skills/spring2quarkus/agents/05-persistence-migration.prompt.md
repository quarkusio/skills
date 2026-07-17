---
name: persistence-migration-agent
description: Phase 5 Persistence Migration Agent. Migrates JPA entities and Spring Data repositories to Quarkus Hibernate ORM with Panache.
  Validates with PersistenceValidator and verifies runtime database initialization.
license: Apache-2.0
metadata:
  phase: 5
  agent_type: migration
---

# Phase 5 — Persistence Migration Agent

## Purpose

Migrate JPA entities and Spring Data repositories to Quarkus Hibernate ORM (with or without Panache).

## ⚠️ CRITICAL: Output File Location

**YOU MUST save the migration report to this exact location:**

```
<quarkus_target_dir>/migration-reports/phase-05-persistence-migration.json
```

**Before creating the report:**
1. Ensure the `migration-reports/` directory exists (create it if needed)
2. Save the report to the exact path above
3. Do NOT save to the root directory
4. Do NOT use any other filename

## Inputs

- migration-spec.yaml (entities and repositories lists)
- Source entity and repository files

## Transformation Rules

Apply RULE GROUP 3 from transformation_rules.md.

### Entity Migration

Entities require minimal changes:
1. Update imports: `javax.persistence.*` → `jakarta.persistence.*`
2. Keep all JPA annotations unchanged
3. Update `@PersistenceContext` → `@Inject` for EntityManager
4. **CRITICAL: Ensure proper table and column name mappings**

### Database Schema Mapping (CRITICAL)

**⚠️ MANDATORY: All entities MUST have explicit table and column mappings to match import.sql**

When migrating entities, you MUST ensure that JPA annotations match the database schema used in `import.sql`:

#### Table Name Mapping

**Rule:** If the table name in `import.sql` uses snake_case or differs from the entity class name, you MUST add `@Table` annotation.

```java
// import.sql uses: INSERT INTO application_settings ...
@Entity
@Table(name = "application_settings")  // ✓ REQUIRED
public class ApplicationSettings {
    // ...
}

// import.sql uses: INSERT INTO carrier_movement ...
@Entity
@Table(name = "carrier_movement")  // ✓ REQUIRED
public class CarrierMovement {
    // ...
}
```

**Without `@Table` annotation:** Hibernate will use default naming strategy which may differ from your SQL schema, causing "Table not found" errors at runtime.

#### Column Name Mapping

**Rule:** Column names in `@Column` annotations MUST EXACTLY match the column names used in `import.sql` - including case sensitivity.

**⚠️ CRITICAL: The column name in @Column MUST be character-for-character identical to import.sql**

```java
// import.sql uses: INSERT INTO application_settings (id, sample_loaded) VALUES ...
@Entity
@Table(name = "application_settings")
public class ApplicationSettings {
    @Id
    private Long id;
    
    @Column(name = "sample_loaded")  // ✓ CORRECT - matches import.sql exactly
    private boolean sampleLoaded;
}

// ❌ WRONG EXAMPLES:
// @Column(name = "SAMPLE_LOADED")  // Wrong - import.sql uses lowercase
// @Column(name = "sampleLoaded")   // Wrong - import.sql uses snake_case
// No @Column annotation            // Wrong - Hibernate will use "sampleLoaded"
```

**Verification Process:**
1. Open `import.sql` and identify the EXACT column name (e.g., `sample_loaded`)
2. Copy the column name character-for-character into `@Column(name = "...")`
3. Do NOT change case, do NOT convert between snake_case/camelCase
4. The annotation value must be a perfect string match to the SQL column name

**Without exact `@Column` annotation:** Hibernate will use the field name as-is, causing "Column not found" errors at runtime.

#### Validation Process

After migrating each entity:

1. **Read import.sql** and identify all table names and column names
2. **For each entity:**
   - Check if table name matches class name (case-insensitive)
   - If not, verify `@Table(name = "...")` annotation exists
   - Check each field against corresponding column in import.sql
   - If column uses snake_case, verify `@Column(name = "...")` annotation exists

3. **Common patterns to check:**
   ```java
   // Pattern 1: snake_case table name
   @Table(name = "table_name")
   
   // Pattern 2: snake_case column name
   @Column(name = "column_name")
   
   // Pattern 3: Both
   @Entity
   @Table(name = "user_profile")
   public class UserProfile {
       @Column(name = "first_name")
       private String firstName;
       
       @Column(name = "last_name")
       private String lastName;
   }
   ```

#### Why This Matters

- **Compilation succeeds** even without proper mappings
- **Runtime fails** when Hibernate tries to execute import.sql
- Errors only appear during application startup
- Common error: `Table "TABLE_NAME" not found` or `Column "column_name" not found`

**Example of what happens without proper mapping:**

```java
// ❌ WRONG - Missing @Table annotation
@Entity
public class ApplicationSettings {
    private boolean sampleLoaded;  // ❌ Missing @Column
}

// Runtime error:
// Table "APPLICATIONSETTINGS" not found
// (Hibernate generated wrong table name)

// ✓ CORRECT - With proper annotations
@Entity
@Table(name = "application_settings")
public class ApplicationSettings {
    @Column(name = "sample_loaded")
    private boolean sampleLoaded;
}
```

### Repository Migration

**IMPORTANT: Handling Custom Repository Implementations**

Spring applications often use custom repository implementations alongside the standard Spring Data JPA interface. When migrating to Panache, you must **merge** the repository interface and its custom implementation into a single Panache repository class.

**Pattern in Spring:**
```java
// Spring Data interface
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);
    Optional<Order> findByOrderNumber(String orderNumber);
}

// Custom implementation (separate class)
public class OrderRepositoryImpl implements OrderRepository {
    @PersistenceContext
    private EntityManager em;
    
    public List<Order> findComplexOrders() {
        // Custom query logic
        return em.createQuery("...", Order.class).getResultList();
    }
}
```

**Migration to Panache:**
When you encounter this pattern, you must:
1. Identify both the repository interface AND its custom implementation class (typically named `*RepositoryImpl`)
2. Merge ALL methods from both files into a single Panache repository
3. Convert Spring Data method names to Panache queries
4. Migrate custom EntityManager-based queries to Panache or keep using EntityManager if needed

**Option A: Panache Repository (Recommended)**

```java
// Before (Spring Data JPA) - TWO separate files
// File 1: OrderRepository.java
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);
    Optional<Order> findByOrderNumber(String orderNumber);
}

// File 2: OrderRepositoryImpl.java
public class OrderRepositoryImpl implements OrderRepository {
    @PersistenceContext
    private EntityManager em;
    
    public List<Order> findComplexOrders() {
        return em.createQuery("SELECT o FROM Order o WHERE ...", Order.class)
                 .getResultList();
    }
}

// After (Panache) - ONE merged file
@ApplicationScoped
public class OrderRepository implements PanacheRepository<Order> {
    // Methods from Spring Data interface (converted to Panache)
    public List<Order> findByCustomerId(Long customerId) {
        return find("customerId", customerId).list();
    }
    
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return find("orderNumber", orderNumber).firstResultOptional();
    }
    
    // Custom methods from implementation class (migrated)
    public List<Order> findComplexOrders() {
        return find("SELECT o FROM Order o WHERE ...").list();
        // OR if complex, inject EntityManager:
        // @Inject EntityManager em;
        // return em.createQuery("...", Order.class).getResultList();
    }
}
```

**Key Points for Custom Repository Migration:**
- ✅ Always check for `*RepositoryImpl` classes alongside repository interfaces
- ✅ Merge both interface methods AND implementation methods into one Panache class
- ✅ Convert `@PersistenceContext` to `@Inject` if keeping EntityManager
- ✅ Prefer Panache query methods, but EntityManager is still available for complex queries
- ✅ Remove the separate implementation class after merging

**Option B: Standard Hibernate ORM**

```java
// After (Standard)
@ApplicationScoped
public class OrderRepository {
    @Inject
    EntityManager em;
    
    public List<Order> findByCustomerId(Long customerId) {
        return em.createQuery("SELECT o FROM Order o WHERE o.customerId = :id", Order.class)
                 .setParameter("id", customerId)
                 .getResultList();
    }
}
```

### Configuration Migration

Convert Spring datasource properties to Quarkus:

```properties
# Before (Spring)
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=user
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# After (Quarkus)
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydb
quarkus.datasource.username=user
quarkus.datasource.password=password
quarkus.hibernate-orm.database.generation=update
quarkus.hibernate-orm.log.sql=true
```

## Steps

1. Read migration-spec.yaml entities list
2. For each entity:
   - Copy to target with import updates
   - Update @PersistenceContext to @Inject
   - Record in transformation ledger
3. Read migration-spec.yaml repositories list
4. For each repository:
   - Convert based on strategy (Panache or Standard)
   - Translate Spring Data method names to queries
   - Record in transformation ledger
5. Update application.properties with datasource config
6. Run `mvn clean package -DskipTests` to ensure compilation is successful
7. Verify Database Initialization (MANDATORY)
   - Start the application with `mvn quarkus:dev`
   - Check logs for Hibernate ORM activation
   - Verify `import.sql` SQL statements appear in logs
   - Confirm no SQL or database startup errors occur
8. Generate persistence-migration-report.json

## Step 7: Verify Database Initialization (MANDATORY)

After migrating entities and repositories, you MUST verify that `import.sql` executes correctly at runtime.

### Why This Step is Critical

In Phase 4, `import.sql` was created but could not execute because:
- Quarkus disables Hibernate ORM when no JPA entities are present
- Phase 4 validator can only perform static checks (file exists, config correct)
- Runtime verification requires entities to be present (Phase 5)

Now that entities are migrated, Hibernate ORM will activate and `import.sql` should execute.

### Verification Process

**Step 7.1: Start the Application**
```bash
cd <quarkus_target_dir>
mvn quarkus:dev
```

**Step 7.2: Check Logs for Hibernate ORM Activation**
Look for this message in the startup logs:
```
Hibernate ORM core version X.X.X.Final
```

If you see "Hibernate ORM is disabled" instead, STOP and investigate:
- Verify entities have `@Entity` annotation
- Check entities are in correct package structure
- Ensure entities are compiled (check `target/classes`)

**Step 7.3: Verify `import.sql` Execution**
With `quarkus.hibernate-orm.log.sql=true` enabled, you should see SQL statements in logs:
```
Hibernate: CREATE TABLE IF NOT EXISTS owners (...)
Hibernate: INSERT INTO vets VALUES (1, 'James', 'Carter')
Hibernate: INSERT INTO vets VALUES (2, 'Helen', 'Leary')
...
```

**Step 7.4: Verify Database Content (Optional)**
If using H2 console or database client, connect and verify:
```sql
SELECT COUNT(*) FROM owners;  -- Should return > 0
SELECT COUNT(*) FROM vets;    -- Should return > 0
SELECT * FROM pets LIMIT 5;   -- Should show sample data
```

### Success Criteria

✅ **PASS** if ALL of the following are true:
- Hibernate ORM activation message appears in logs
- `import.sql` SQL statements appear in logs (CREATE TABLE, INSERT)
- No SQL errors in logs
- Application starts successfully without database errors

❌ **FAIL** if ANY of the following occur:
- "Hibernate ORM is disabled" message appears
- No SQL statements in logs
- SQL syntax errors in logs
- Application fails to start with database errors

### Troubleshooting

**Problem: "Hibernate ORM is disabled"**
- Cause: No entities found or entities not compiled
- Solution: Verify `@Entity` annotations, check package structure, run `mvn clean compile`

**Problem: "`import.sql` not found"**
- Cause: File in wrong location
- Solution: Ensure file is in `src/main/resources/import.sql` (not in subdirectory)

**Problem: SQL syntax errors**
- Cause: SQL not compatible with database type
- Solution: Review `import.sql` syntax for target database (H2, PostgreSQL, etc.)

**Problem: Foreign key constraint violations**
- Cause: INSERT statements in wrong order
- Solution: Reorder `import.sql` to insert parent tables before child tables

### Automated Verification (Optional)

You can create a simple verification class to check database content programmatically:

```java
package org.example.verification;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DatabaseVerifier {
    
    private static final Logger LOG = Logger.getLogger(DatabaseVerifier.class);
    
    @Inject
    EntityManager em;
    
    void onStart(@Observes StartupEvent ev) {
        try {
            Long ownerCount = em.createQuery("SELECT COUNT(o) FROM Owner o", Long.class)
                .getSingleResult();
            Long vetCount = em.createQuery("SELECT COUNT(v) FROM Vet v", Long.class)
                .getSingleResult();
            Long petCount = em.createQuery("SELECT COUNT(p) FROM Pet p", Long.class)
                .getSingleResult();
            
            LOG.infof("Database verification: %d owners, %d vets, %d pets",
                ownerCount, vetCount, petCount);
            
            if (ownerCount == 0 || vetCount == 0) {
                LOG.error("Database appears empty! import.sql may not have executed.");
            } else {
                LOG.info("Database initialization verified successfully!");
            }
        } catch (Exception e) {
            LOG.error("Database verification failed", e);
        }
    }
}
```

Place this file in `src/main/java/org/example/verification/DatabaseVerifier.java` and it will automatically run on startup.

## Spring Data Method Name Translation

| Spring Data Pattern | Panache Query |
|---------------------|---------------|
| `findByField(value)` | `find("field", value).list()` |
| `findByFieldAndOther(v1, v2)` | `find("field = ?1 and other = ?2", v1, v2).list()` |
| `findByFieldOrderByOther(value)` | `find("field", Sort.by("other"), value).list()` |
| `countByField(value)` | `count("field", value)` |
| `deleteByField(value)` | `delete("field", value)` |
| `existsByField(value)` | `count("field", value) > 0` |

## Output

**Directory Setup:**
```bash
mkdir -p migration-reports migration-metadata
```

**File Location:** `migration-reports/phase-05-persistence-migration.json`

This report should be created in the target Quarkus project at `<quarkus_target_dir>/migration-reports/phase-05-persistence-migration.json`.

## Validation Gate

**Run validator after completing persistence migration:**

```bash
# Build validator if needed
cd validators/java
mvn clean package -DskipTests -q

# Generate metadata (regenerate each time code changes)
# Store in migration-metadata/ directory
java -jar target/migration-validator-1.0.0.jar extract metadata \
  <spring_source_dir> -o <spring_source_dir>/migration-metadata/code-metadata.yaml
java -jar target/migration-validator-1.0.0.jar extract metadata \
  <quarkus_target_dir> -o <quarkus_target_dir>/migration-metadata/code-metadata.yaml

# Run validator (using metadata from migration-metadata/)
java -jar target/migration-validator-1.0.0.jar validate persistence \
  <spring_source_dir>/migration-metadata/code-metadata.yaml \
  <quarkus_target_dir>/migration-metadata/code-metadata.yaml \
  <quarkus_target_dir> \
  <migration-spec.yaml>
```

**VALIDATION LOOP (MANDATORY - DO NOT SKIP):**
- If validator shows failures (exit code 1):
  1. Read error messages and identify issues
  2. Fix the problems in entities/repositories
  3. Regenerate metadata and rerun validator
  4. Repeat until exit code = 0 and Status = SUCCESS
- Only proceed to next phase when: `Rules: X total | X passed | 0 failed`

**Validator checks:** Entity coverage, repository coverage, @ApplicationScoped on repos, Panache patterns (if used), import.sql runtime execution

**⚠️ Do not proceed to Phase 6 until validation passes!**


**Report Example** (`migration-reports/phase-05-persistence-migration.json`):

```json
{
  "phase": "persistence-migration",
  "status": "completed",
  "entities_migrated": 15,
  "repositories_migrated": 12,
  "strategy": "panache",
  "files": [],
  "package_status": "PASS"
}
```

Update migration-spec.yaml transformations.persistence-migration section.