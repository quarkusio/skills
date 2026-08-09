---
name: persistence-full-migration-agent
description: Phase 5B Persistence Migration — Full Migration. Converts Spring Data JPA repositories to Quarkus Hibernate ORM with Panache
  (panache-repository) or standard EntityManager (hibernate-orm-standard).
  Called from 05-persistence-migration.prompt.md for any strategy other than spring-data-compat.
license: Apache-2.0
metadata:
  phase: 5
  agent_type: migration
---

# Phase 5B — Persistence Migration: Full Migration

> **Entry point:** This file is invoked by `05-persistence-migration.prompt.md` when
> `migration_strategy.repository_layer` is `panache-repository` or `hibernate-orm-standard`.
> Entity migration rules and database schema mapping are in the entry-point file — read those first.

## Overview

Repositories are fully rewritten from Spring Data JPA interfaces to either:
- **Panache Repository** (`PanacheRepository<E>`) — recommended, idiomatic Quarkus
- **Standard Hibernate ORM** — `@ApplicationScoped` bean with injected `EntityManager`

The choice is determined by `migration_strategy.repository_layer` in `migration-spec.yaml`.

---

## Step 1 — Migrate entities

Apply entity migration rules from `05-persistence-migration.prompt.md`:
- Update `javax.persistence.*` → `jakarta.persistence.*`
- Replace `@PersistenceContext` with `@Inject` on `EntityManager` fields
- Apply all `@Table` / `@Column` name-mapping rules
- Record each entity in the transformation ledger

---

## Step 2 — Migrate repositories

For each repository in the `migration-spec.yaml` repositories list, apply the pattern that matches
`migration_strategy.repository_layer`.

### Handling Custom Repository Implementations (`*RepositoryImpl`)

Spring applications often pair a `JpaRepository` interface with a `*RepositoryImpl` class for
custom queries. When migrating to Panache you MUST **merge** both into a single class.

**Pattern in Spring (two files):**
```java
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
```

Steps when encountering this pattern:
1. Identify both the repository interface AND its `*RepositoryImpl` class
2. Merge ALL methods from both files into a single Panache repository
3. Convert Spring Data method names to Panache queries (see translation table below)
4. Migrate custom `EntityManager`-based queries to Panache, or keep `EntityManager` if complex
5. Delete the original `*RepositoryImpl` file

### Option A: Panache Repository (Recommended)

Use when `repository_layer = panache-repository`.

```java
// After (Panache) — ONE merged file replacing both Spring Data files
@ApplicationScoped
public class OrderRepository implements PanacheRepository<Order> {

    // Converted from Spring Data derived query
    public List<Order> findByCustomerId(Long customerId) {
        return find("customerId", customerId).list();
    }

    // Converted from Spring Data derived query
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return find("orderNumber", orderNumber).firstResultOptional();
    }

    // Migrated from custom impl — Panache query
    public List<Order> findComplexOrders() {
        return find("SELECT o FROM Order o WHERE ...").list();
    }

    // OR for very complex queries, keep EntityManager:
    // @Inject EntityManager em;
    // return em.createQuery("...", Order.class).getResultList();
}
```

**Key points:**
- ✅ Always check for `*RepositoryImpl` classes and merge them
- ✅ Convert `@PersistenceContext` to `@Inject` if keeping `EntityManager`
- ✅ Use Panache query methods; `EntityManager` is still available for complex queries
- ✅ Delete the separate implementation class after merging

### Option B: Standard Hibernate ORM

Use when `repository_layer = hibernate-orm-standard`.

```java
@ApplicationScoped
public class OrderRepository {

    @Inject
    EntityManager em;

    public List<Order> findByCustomerId(Long customerId) {
        return em.createQuery(
                "SELECT o FROM Order o WHERE o.customerId = :id", Order.class)
            .setParameter("id", customerId)
            .getResultList();
    }

    public Optional<Order> findByOrderNumber(String orderNumber) {
        return em.createQuery(
                "SELECT o FROM Order o WHERE o.orderNumber = :num", Order.class)
            .setParameter("num", orderNumber)
            .getResultStream()
            .findFirst();
    }
}
```

### Spring Data Method Name Translation

| Spring Data Pattern | Panache Query |
|---------------------|---------------|
| `findByField(value)` | `find("field", value).list()` |
| `findByFieldAndOther(v1, v2)` | `find("field = ?1 and other = ?2", v1, v2).list()` |
| `findByFieldOrderByOther(value)` | `find("field", Sort.by("other"), value).list()` |
| `countByField(value)` | `count("field", value)` |
| `deleteByField(value)` | `delete("field", value)` |
| `existsByField(value)` | `count("field", value) > 0` |

Record each repository in the transformation ledger.

---

## Step 3 — Update `application.properties` with datasource config

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

---

## Step 4 — Compile the project

```bash
cd <quarkus_target_dir>
mvn clean package -DskipTests
```

Fix any compilation errors before proceeding.

---

## Step 5 — Verify database initialization (MANDATORY)

After migrating entities and repositories, you MUST verify that `import.sql` executes correctly
at runtime.

### Why this step is critical

In Phase 4, `import.sql` was created but could not execute because:
- Quarkus disables Hibernate ORM when no JPA entities are present
- Phase 4 validator can only perform static checks (file exists, config correct)
- Runtime verification requires entities to be present (Phase 5)

Now that entities are migrated, Hibernate ORM will activate and `import.sql` should execute.

### Step 5.1 — Start the application

```bash
cd <quarkus_target_dir>
mvn quarkus:dev
```

### Step 5.2 — Check logs for Hibernate ORM activation

Look for this message in the startup logs:
```
Hibernate ORM core version X.X.X.Final
```

If you see "Hibernate ORM is disabled" instead, STOP and investigate:
- Verify entities have `@Entity` annotation
- Check entities are in correct package structure
- Ensure entities are compiled (check `target/classes`)

### Step 5.3 — Verify `import.sql` execution

With `quarkus.hibernate-orm.log.sql=true` enabled, you should see SQL statements in logs:
```
Hibernate: CREATE TABLE IF NOT EXISTS owners (...)
Hibernate: INSERT INTO vets VALUES (1, 'James', 'Carter')
Hibernate: INSERT INTO vets VALUES (2, 'Helen', 'Leary')
...
```

### Step 5.4 — Verify database content (optional)

If using H2 console or database client, connect and verify:
```sql
SELECT COUNT(*) FROM owners;  -- Should return > 0
SELECT COUNT(*) FROM vets;    -- Should return > 0
SELECT * FROM pets LIMIT 5;   -- Should show sample data
```

### Success criteria

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
- Solution: Ensure file is in `src/main/resources/import.sql` (not in a subdirectory)

**Problem: SQL syntax errors**
- Cause: SQL not compatible with database type
- Solution: Review `import.sql` syntax for target database (H2, PostgreSQL, etc.)

**Problem: Foreign key constraint violations**
- Cause: INSERT statements in wrong order
- Solution: Reorder `import.sql` to insert parent tables before child tables

### Automated verification (optional)

Create this class to check database content programmatically at startup:

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

Place this in `src/main/java/org/example/verification/DatabaseVerifier.java`.

---

## Step 6 — Generate migration report

Write `<quarkus_target_dir>/migration-reports/phase-05-persistence-migration.json`:

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

Set `"strategy"` to `"panache"` or `"hibernate-orm-standard"` to match the actual path taken.

Then update the `transformations.persistence-migration` section in `migration-spec.yaml`.

---

## Validation Gate

```bash
# Build validator if needed
cd validators/java
mvn clean package -DskipTests -q

# Generate metadata (regenerate each time code changes)
java -jar target/migration-validator-1.0.0.jar extract metadata \
  <spring_source_dir> -o <spring_source_dir>/migration-metadata/code-metadata.yaml
java -jar target/migration-validator-1.0.0.jar extract metadata \
  <quarkus_target_dir> -o <quarkus_target_dir>/migration-metadata/code-metadata.yaml

# Run full-migration validation
java -jar target/migration-validator-1.0.0.jar validate persistence \
  <spring_source_dir>/migration-metadata/code-metadata.yaml \
  <quarkus_target_dir>/migration-metadata/code-metadata.yaml \
  <quarkus_target_dir> \
  <migration-spec.yaml>
```

**VALIDATION LOOP (MANDATORY — DO NOT SKIP):**
- If validator shows failures (exit code 1):
  1. Read error messages and identify issues
  2. Fix the problems in entities/repositories
  3. Regenerate metadata and rerun validator
  4. Repeat until exit code = 0 and Status = SUCCESS
- Only proceed to next phase when: `Rules: X total | X passed | 0 failed`

**Validator checks:** Entity coverage, repository coverage, `@ApplicationScoped` on repos,
Panache patterns (if used), `import.sql` runtime execution

**⚠️ Do not proceed to Phase 6 until validation passes!**
