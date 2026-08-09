---
name: persistence-migration-agent
description: Phase 5 Persistence Migration Agent. Migrates JPA entities and, depending on strategy, rewrites
  Spring Data repositories to Quarkus Hibernate ORM (Panache or standard) or bridges them via quarkus-spring-data-jpa.
  Validates with PersistenceValidator and verifies runtime database initialization.
license: Apache-2.0
metadata:
  phase: 5
  agent_type: migration
---

# Phase 5 — Persistence Migration Agent

## Purpose

Migrate JPA entities to Quarkus Hibernate ORM. Repository migration depends on the chosen strategy:

- **`spring-data-compat`** — repository interfaces are kept as-is (`JpaRepository<E, ID>`) and bridged at runtime by `quarkus-spring-data-jpa`; no rewrite to Panache
- **`panache-repository`** — repositories are rewritten as `PanacheRepository<E>` beans
- **`hibernate-orm-standard`** — repositories are rewritten as `@ApplicationScoped` beans with an injected `EntityManager`

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

- `migration-spec.yaml` (entities and repositories lists, `migration_strategy.repository_layer`, `compat_mode.spring_data_jpa`)
- Source entity and repository files

## Transformation Rules

Apply RULE GROUP 3 from `transformation_rules.md`.

---

## Shared: Entity Migration Rules

These rules apply to **both** migration paths.

### Entity Migration

Entities require minimal changes:
1. Update imports: `javax.persistence.*` → `jakarta.persistence.*`
2. Keep all JPA annotations unchanged
3. Update `@PersistenceContext` → `@Inject` for EntityManager
4. **CRITICAL: Ensure proper table and column name mappings**

### Database Schema Mapping (CRITICAL)

**⚠️ MANDATORY: All entities MUST have explicit table and column mappings to match `import.sql`**

When migrating entities, you MUST ensure that JPA annotations match the database schema used in `import.sql`.

#### Table Name Mapping

**Rule:** If the table name in `import.sql` uses snake_case or differs from the entity class name, you MUST add `@Table` annotation.

```java
// import.sql uses: INSERT INTO application_settings ...
@Entity
@Table(name = "application_settings")  // ✓ REQUIRED
public class ApplicationSettings { }

// import.sql uses: INSERT INTO carrier_movement ...
@Entity
@Table(name = "carrier_movement")  // ✓ REQUIRED
public class CarrierMovement { }
```

**Without `@Table` annotation:** Hibernate will use its default naming strategy, which may differ from your SQL schema, causing "Table not found" errors at runtime.

#### Column Name Mapping

**Rule:** Column names in `@Column` annotations MUST EXACTLY match the column names used in `import.sql` — including case.

**⚠️ CRITICAL: The column name in `@Column` MUST be character-for-character identical to `import.sql`**

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

1. **Read `import.sql`** and identify all table names and column names
2. **For each entity:**
   - Check if table name matches class name (case-insensitive)
   - If not, verify `@Table(name = "...")` annotation exists
   - Check each field against the corresponding column in `import.sql`
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
- **Runtime fails** when Hibernate tries to execute `import.sql`
- Errors only appear during application startup
- Common errors: `Table "TABLE_NAME" not found` or `Column "column_name" not found`

**Example of what happens without proper mapping:**

```java
// ❌ WRONG - Missing @Table annotation
@Entity
public class ApplicationSettings {
    private boolean sampleLoaded;  // ❌ Missing @Column
}
// Runtime error: Table "APPLICATIONSETTINGS" not found

// ✓ CORRECT - With proper annotations
@Entity
@Table(name = "application_settings")
public class ApplicationSettings {
    @Column(name = "sample_loaded")
    private boolean sampleLoaded;
}
```

---

## Step 0 — Read `migration-spec.yaml` and route to the correct path

Read `migration_strategy.repository_layer` from the spec, then **continue in the appropriate file**:

| Value | Continue with |
|-------|---------------|
| `spring-data-compat` | [`05a-persistence-compat.prompt.md`](05a-persistence-compat.prompt.md) |
| `panache-repository` or `hibernate-orm-standard` | [`05b-persistence-full-migration.prompt.md`](05b-persistence-full-migration.prompt.md) |

The entity migration rules and database schema mapping above apply in **both** paths.
Each sub-file is self-contained from Step 1 onward.
