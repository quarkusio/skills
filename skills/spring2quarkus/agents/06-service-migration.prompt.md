---
name: service-migration-agent
description: Phase 6 Service Layer Migration Agent. Converts Spring @Service and @Component classes to CDI @ApplicationScoped beans.
  Migrates @Autowired to @Inject and validates with ServiceValidator.
license: Apache-2.0
metadata:
  phase: 6
  agent_type: migration
---

# Phase 6 — Service Layer Migration Agent

## Purpose

Convert Spring service layer components (@Service, @Component) to Quarkus CDI beans.

**CRITICAL: ALL business services must be migrated, including both `@Service` and `@Component` annotated classes.**

## ⚠️ CRITICAL: Output File Location

**YOU MUST save the migration report to this exact location:**

```
<quarkus_target_dir>/migration-reports/phase-06-service-migration.json
```

**Before creating the report:**
1. Ensure the `migration-reports/` directory exists (create it if needed)
2. Save the report to the exact path above
3. Do NOT save to the root directory
4. Do NOT use any other filename

## Inputs

- migration-spec.yaml (services list)
- Source service files

## Scope Requirements

**⚠️ MANDATORY: Complete Service Migration**

You MUST migrate ALL business service classes from the Spring application, including:

1. **@Service annotated classes** - All business logic services
2. **@Component annotated classes** - All utility components, helpers, and supporting classes
3. **Domain services** - Services in domain packages that implement business logic
4. **Infrastructure services** - Services that provide technical capabilities (routing, external integrations, etc.)
5. **Application services** - Facade services that coordinate between layers
6. **⚠️ CRITICAL: Initial data loaders and one-time setup components** - Classes that run on startup to populate initial data (e.g., InitialLoader, SampleDataGenerator, DataBootstrap)

**Common Mistake to Avoid:**
- ❌ Only migrating classes explicitly marked with `@Service`
- ❌ Overlooking `@Component` classes that contain business logic
- ❌ Missing domain service classes that may not have Spring annotations but are injected
- ❌ **Forgetting initial data loaders** - These are critical for application startup and must be migrated

**Verification Checklist:**
Before completing this phase, verify:
- ✅ All `@Service` classes are migrated
- ✅ All `@Component` classes are migrated
- ✅ All classes with `@Autowired` dependencies are migrated
- ✅ All classes injected into other services are migrated
- ✅ Domain service interfaces and implementations are migrated
- ✅ Infrastructure service implementations are migrated
- ✅ **All initial data loaders and startup components are migrated** (InitialLoader, SampleDataGenerator, etc.)

## Transformation Rules

Apply RULE GROUP 4 from transformation_rules.md.

### Key Transformations

1. **@Service → @ApplicationScoped**
```java
// Before
@Service
public class OrderService {
}

// After
@ApplicationScoped
public class OrderService {
}
```

2. **@Component → @ApplicationScoped**
```java
// Before
@Component
public class HelperComponent {
}

// After
@ApplicationScoped
public class HelperComponent {
}
```

3. **@Autowired → @Inject**
```java
// Before
@Autowired
private OrderRepository orderRepository;

// After
@Inject
OrderRepository orderRepository;
```

4. **@Value → @ConfigProperty**
```java
// Before
@Value("${app.timeout}")
private int timeout;

// After
@ConfigProperty(name = "app.timeout")
int timeout;
```

5. **@Transactional**
```java
// Before
import org.springframework.transaction.annotation.Transactional;

@Transactional
public void saveOrder(Order order) {}

// After
import jakarta.transaction.Transactional;

@Transactional
public void saveOrder(Order order) {}
```

6. **@Async → Uni<T>**
```java
// Before
@Async
public CompletableFuture<Order> processOrder(Long id) {
    return CompletableFuture.completedFuture(order);
}

// After
public Uni<Order> processOrder(Long id) {
    return Uni.createFrom().item(order);
}
```

7. **@Scheduled**
```java
// Before
@Scheduled(fixedRate = 5000)
public void updateCache() {}

// After
@Scheduled(every = "5s")
void updateCache() {}
```

## Import Updates

Remove:
```java
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.CompletableFuture;
```

Add:
```java
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.scheduler.Scheduled;
import jakarta.transaction.Transactional;
import io.smallrye.mutiny.Uni;
```

## Steps

**Step 0: Comprehensive Service Discovery (MANDATORY)**

Before starting migration, perform a thorough discovery of ALL service classes:

```bash
# Find all @Service annotated classes
grep -r "@Service" <spring_source_dir>/src/main/java --include="*.java"

# Find all @Component annotated classes
grep -r "@Component" <spring_source_dir>/src/main/java --include="*.java"

# Find all classes with @Autowired (may indicate services)
grep -r "@Autowired" <spring_source_dir>/src/main/java --include="*.java"
```

Cross-reference findings with migration-spec.yaml to ensure completeness. If any services are missing from the spec, add them before proceeding.

**Step 1: Read migration-spec.yaml services list**

Verify the services list includes:
- All `@Service` annotated classes
- All `@Component` annotated classes
- All domain service implementations
- All infrastructure service implementations

**Step 2: For each service file:**
   - Read source file
   - Replace @Service/@Component with @ApplicationScoped
   - Replace @Autowired with @Inject
   - Replace @Value with @ConfigProperty
   - Update @Transactional import
   - Convert @Async methods to Uni<T>
   - Update @Scheduled syntax
   - Update imports
   - Write to target
   - Record in transformation ledger

**Step 3: Verify Complete Migration**

After migrating all services, verify no services were missed:
```bash
# Check for any remaining Spring annotations in Quarkus target
grep -r "@Service\|@Component\|@Autowired" <quarkus_target_dir>/src/main/java --include="*.java"
```

If any Spring annotations remain, investigate and migrate those classes.

**Step 4: Run `mvn clean package -DskipTests`** to ensure compilation is successful

**Step 5: Generate service-migration-report.json**

## Special Cases

### Constructor Injection
```java
// Before (Spring - constructor injection)
@Service
public class OrderService {
    private final OrderRepository repository;
    
    @Autowired
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}

// After (Quarkus - constructor injection)
@ApplicationScoped
public class OrderService {
    private final OrderRepository repository;
    
    @Inject
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

### @PostConstruct / @PreDestroy
```java
// These annotations work the same in both frameworks
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@PostConstruct
void init() {}

@PreDestroy
void cleanup() {}
```

### Initial Data Loaders (CRITICAL)

**⚠️ IMPORTANT: One-time setup components must be migrated in this phase!**

Initial data loaders are components that run on application startup to populate sample/seed data. These are typically `@Component` classes with `@PostConstruct` methods.

```java
// Before (Spring)
@Component
@Profile("!test")
public class SampleDataGenerator {
    @Autowired
    private InitialLoader loader;
    
    @PostConstruct
    public void loadSampleData() {
        loader.loadData();
    }
}

// After (Quarkus - using StartupEvent)
@ApplicationScoped
public class SampleDataGenerator {
    @Inject
    InitialLoader loader;
    
    void onStart(@Observes StartupEvent ev) {
        loader.loadData();
    }
}
```

**Key Points:**
- Replace `@PostConstruct` with `@Observes StartupEvent` for startup logic
- Remove `@Profile` annotations (use Quarkus profiles instead: `@IfBuildProfile`)
- Ensure `@Transactional` methods are properly annotated in the loader class
- Import: `import io.quarkus.runtime.StartupEvent;`
- Import: `import jakarta.enterprise.event.Observes;`

**Common Data Loader Patterns:**
- `InitialLoader` / `DataLoader` - Main data loading logic
- `SampleDataGenerator` / `DataBootstrap` - Triggers data loading on startup
- `DatabaseSeeder` / `DataInitializer` - Alternative naming patterns

**Search for these patterns:**
```bash
grep -r "InitialLoader\|DataLoader\|SampleData\|Bootstrap\|Seeder\|DataInitializer" <spring_source_dir>/src/main/java --include="*.java"
```

## Validation Gate

**Run validator after completing service migration:**

```bash
# Build validator if needed
cd validators/java
mvn clean package -DskipTests -q

# Run validator
java -jar target/migration-validator-1.0.0.jar validate services \
  <quarkus_target_dir> \
  <quarkus_target_dir>/migration-spec.yaml
```

**VALIDATION LOOP (MANDATORY - DO NOT SKIP):**
- If validator shows failures (exit code 1):
  1. Read error messages and identify issues
  2. Fix the problems in service classes
  3. Rerun validator
  4. Repeat until exit code = 0 and Status = SUCCESS
- Only proceed to next phase when: `Rules: X total | X passed | 0 failed`

**Validator checks:** @Service→@ApplicationScoped, @Autowired→@Inject, @Transactional→jakarta.transaction, @Scheduled migration, all services migrated including data loaders
- Missing CDI scope annotations

### Non-Blocking Warnings
These can be addressed later but should be documented:
- Service count mismatches (may be intentional)
- @Async not converted to Uni (can be done later)
- Config properties not fully migrated

**⚠️ IMPORTANT: Do not proceed to Phase 7 until validation gate passes!**


## Output

**Directory Setup:**
```bash
mkdir -p migration-reports
```

**File Location:** `migration-reports/phase-06-service-migration.json`

This report should be created in the target Quarkus project at `<quarkus_target_dir>/migration-reports/phase-06-service-migration.json`.

```json
{
  "phase": "service-migration",
  "status": "completed",
  "services_migrated": 15,
  "components_migrated": 5,
  "async_methods_converted": 3,
  "scheduled_tasks_converted": 2,
  "files": [],
  "package_status": "PASS"
}
```

Update migration-spec.yaml transformations.service-migration section.