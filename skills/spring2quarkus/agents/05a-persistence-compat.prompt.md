---
name: persistence-compat-agent
description: Phase 5A Persistence Migration — Compat Mode. Keeps Spring Data JPA repository interfaces intact using quarkus-spring-data-jpa bridge.
  Called from 05-persistence-migration.prompt.md when repository_layer = spring-data-compat.
license: Apache-2.0
metadata:
  phase: 5
  agent_type: migration
---

# Phase 5A — Persistence Migration: Compat Mode (spring-data-compat)

> **Entry point:** This file is invoked by `05-persistence-migration.prompt.md` when
> `migration_strategy.repository_layer = spring-data-compat`.
> Entity migration rules and database schema mapping are in the entry-point file — read those first.

## Overview

`quarkus-spring-data-jpa` bridges Spring Data JPA interfaces at runtime. Repository interfaces keep
their `JpaRepository<E, ID>` declaration — no rewrite to Panache is needed.

> **Minimum Quarkus version:** `quarkus-spring-data-jpa` is available since **0.23.0**.
> `quarkus-spring-tx` (optional, for keeping Spring `@Transactional` imports) requires **3.37.0**.
> If the target version in migration-spec.yaml is older than these minimums, flag the conflict to the
> user and refer to `references/spring-compat-mode-support.md` for resolution options.

---

## Step 1 — Migrate entities

Read the `entities` list from `migration-spec.yaml`. For each entity file:

1. Update all `javax.persistence.*` imports to `jakarta.persistence.*`.
   This is **always required** — `quarkus-spring-data-jpa` does **not** bridge old `javax` imports.

   ```java
   // Before
   import javax.persistence.Entity;
   import javax.persistence.Table;

   // After
   import jakarta.persistence.Entity;
   import jakarta.persistence.Table;
   ```

2. Replace any `@PersistenceContext EntityManager` with `@Inject EntityManager` (JPA-4 rule):

   ```java
   // Before
   @PersistenceContext
   private EntityManager em;

   // After
   @Inject
   EntityManager em;
   ```

3. Apply all `@Table` / `@Column` name-mapping rules from the **Database Schema Mapping** section
   in `05-persistence-migration.prompt.md`.

4. Record the entity in the transformation ledger.

---

## Step 2 — Process repository files

Read the `repositories` list from `migration-spec.yaml`. For each repository file:

1. **Check whether a corresponding `*RepositoryImpl` class exists** alongside the interface.
   - If yes → convert to the repository fragments pattern (see Step 6 below).
   - If no → the `JpaRepository` interface requires no changes; note it in the ledger as unchanged.

2. Record the repository in the transformation ledger.

---

## Step 3 — Fix `@Transactional` imports in entity and repository files

Spring's `org.springframework.transaction.annotation.Transactional` is **not** bridged by
`quarkus-spring-data-jpa`. Choose one option:

**Option A — Migrate import (recommended for clean code):**

```java
// Before
import org.springframework.transaction.annotation.Transactional;

// After
import jakarta.transaction.Transactional;
```

**Option B — Add `quarkus-spring-tx` extension (keep Spring import as-is):**

Add to `pom.xml`:
```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-spring-tx</artifactId>
</dependency>
```

If you choose Option B, also set `compat_mode.spring_tx: true` in `migration-spec.yaml` so
the validator knows to check for `quarkus-spring-tx` instead of the Jakarta import.

> **Which option to use?** If the project has many `@Transactional` usages spread across services
> and repositories, Option B saves time now; Option A is the correct end state.

---

## Step 4 — Verify `quarkus-spring-data-jpa` is in `pom.xml`

```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-spring-data-jpa</artifactId>
</dependency>
```

Add it if missing.

---

## Step 5 — Configure datasource in `application.properties`

Ensure the Quarkus datasource properties are present:

```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydb
quarkus.datasource.username=user
quarkus.datasource.password=password
quarkus.hibernate-orm.database.generation=update
quarkus.hibernate-orm.log.sql=true
```

---

## Step 6 — Handle `*RepositoryImpl` classes (repository fragments)

> Applies only to repositories flagged in Step 2 as having a `*RepositoryImpl` class.

`quarkus-spring-data-jpa` has **limited** support for the classic Spring pattern of a
`*RepositoryImpl` class alongside a `JpaRepository` interface.

### Pattern NOT supported directly — must use repository fragments

The classic Spring pattern:
```java
// Interface (Spring Data)
public interface OrderRepository extends JpaRepository<Order, Long> { }

// Custom impl (separate class — NOT reliably bridged in Quarkus)
public class OrderRepositoryImpl implements OrderRepository {
    @PersistenceContext EntityManager em;
    public List<Order> findComplexOrders() { ... }
}
```

**The supported Quarkus compat pattern — repository fragments:**

Define the custom behaviour in a separate *fragment interface* and its implementation, then
compose them into the repository interface:

```java
// 1. Fragment interface (pure Java interface, no Spring Data dependency)
public interface OrderRepositoryFragment {
    List<Order> findComplexOrders();
}

// 2. Fragment implementation — use JpaOperations for Panache-style queries
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;

public class OrderRepositoryFragmentImpl implements OrderRepositoryFragment {
    @Override
    public List<Order> findComplexOrders() {
        // Use JpaOperations for Panache-style access, or inject EntityManager directly
        return (List<Order>) JpaOperations.find(Order.class, "status = 'PENDING'").list();
    }
}

// 3. Repository interface — compose JpaRepository + fragment
public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryFragment {
    // Spring Data derived queries still work here
    List<Order> findByCustomerId(Long customerId);
}
```

**Key rules for fragment implementations:**
- ✅ The fragment interface must be a plain Java interface (no Spring Data supertype)
- ✅ The impl class name must follow the pattern `<FragmentInterface>Impl` exactly
- ✅ Both must be on the classpath — Quarkus picks them up automatically
- ✅ `@Inject EntityManager em` works inside fragment impls (use `@Inject`, not `@PersistenceContext`)
- ✅ `io.quarkus.hibernate.orm.panache.runtime.JpaOperations` is available for Panache-style queries
- ❌ Do NOT have the impl class implement the repository interface itself (only implement the fragment interface)

### Checklist for each `*RepositoryImpl` found

For every `OrderRepositoryImpl` (or similar) in the source:

1. Identify the methods it provides beyond the standard `JpaRepository` operations
2. Create a `OrderRepositoryFragment` interface with those method signatures
3. Rename the impl class to `OrderRepositoryFragmentImpl implements OrderRepositoryFragment`
4. Fix `@PersistenceContext` → `@Inject` inside the impl
5. Add `OrderRepositoryFragment` to the `extends` clause of `OrderRepository`
6. Delete the original `OrderRepositoryImpl` file

---

## Step 7 — Compile the project

```bash
cd <quarkus_target_dir>
mvn clean package -DskipTests
```

Fix any compilation errors before proceeding. Common issues:
- Missing `jakarta.persistence` imports (entity not updated in Step 1)
- Fragment impl not found (class naming mismatch — must end in `Impl`)
- `@Inject` on `EntityManager` without a datasource configured

---

## Step 8 — Verify database initialization (MANDATORY)

Start the application with `mvn quarkus:dev` and confirm:
- Hibernate ORM activates (log: `Hibernate ORM core version X.X.X.Final`)
- `import.sql` statements appear in logs
- No SQL errors at startup

See the **Step 5 — Verify database initialization** section in `05b-persistence-full-migration.prompt.md`
for the full verification procedure, success criteria, troubleshooting steps, and the optional
`DatabaseVerifier` class — the process is identical in both paths.

---

## Step 9 — Generate migration report

Write `<quarkus_target_dir>/migration-reports/phase-05-persistence-migration.json`:

```json
{
  "phase": "persistence-migration",
  "status": "completed",
  "entities_migrated": 15,
  "repositories_migrated": 12,
  "strategy": "spring-data-compat",
  "files": [],
  "package_status": "PASS"
}
```

Then update the `transformations.persistence-migration` section in `migration-spec.yaml`.

---

## Validation Gate

```bash
# Build validator if needed
cd validators/java
mvn clean package -DskipTests -q

# Regenerate metadata after code changes
java -jar target/migration-validator-1.0.0.jar extract metadata \
  <spring_source_dir> -o <spring_source_dir>/migration-metadata/code-metadata.yaml
java -jar target/migration-validator-1.0.0.jar extract metadata \
  <quarkus_target_dir> -o <quarkus_target_dir>/migration-metadata/code-metadata.yaml

# Run compat-mode validation
java -jar target/migration-validator-1.0.0.jar validate persistence \
  <spring_source_dir>/migration-metadata/code-metadata.yaml \
  <quarkus_target_dir>/migration-metadata/code-metadata.yaml \
  <quarkus_target_dir> \
  <migration-spec.yaml> \
  --compat-mode
```

**VALIDATION LOOP (MANDATORY):**
- If validator shows failures (exit code 1): fix issues → regenerate metadata → rerun
- Only proceed to Phase 6 when: `Rules: X total | X passed | 0 failed`

**Compat mode validator checks:**
- Entity count matches Spring source
- Entity structure (fields, relationships, table/column mappings)
- `quarkus-spring-data-jpa` present in `pom.xml`
- `quarkus-spring-tx` present in `pom.xml` (only when `compat_mode.spring_tx: true` in spec)
- `*RepositoryImpl` classes detected and warned (must be converted to fragments — see Step 6)
- Datasource configuration present
- `mvn compile` succeeds

**⚠️ Do not proceed to Phase 6 until validation passes!**
