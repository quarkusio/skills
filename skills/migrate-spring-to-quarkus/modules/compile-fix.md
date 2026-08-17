# Module: Compile Fix

Resolve compilation failures that arise after migrating Spring Boot code to Quarkus.

## Strategy

1. Read the compiler output.
2. Fix one file at a time.
3. Recompile after each file fix to verify the change reduced the error count.
4. Repeat up to **3 attempts per file**.
5. If a file still fails after 3 attempts → emit the `MANUAL_REVIEW_REQUIRED` block (see below), then act according to mode:
   - **`interactive`** — ask the user: *"I was unable to automatically fix `File.java`. Would you like me to continue migrating the remaining files, or stop here?"* Wait for the response before proceeding.
   - **`autonomous`** — log the failure internally and continue automatically to the next file. Do **not** pause or ask. All failures will be surfaced in the final Migration Report.

## Common Error Patterns

### Missing or wrong import (`cannot find symbol`, `package does not exist`)

```java
// BEFORE: Spring / javax
import javax.persistence.Entity;
import javax.inject.Inject;
import org.springframework.stereotype.Service;

// AFTER: Quarkus / jakarta
import jakarta.persistence.Entity;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
```

### Incorrect or unknown annotation (`annotation type not applicable`)

Spring annotations that have no Quarkus equivalent must be replaced. See [references/annotation-map.md](../references/annotation-map.md) for the full table.

Common swaps:

| Spring | Quarkus |
|---|---|
| `@Autowired` | `@Inject` |
| `@Component` / `@Service` / `@Repository` | `@ApplicationScoped` |
| `@RestController` | `@Path` + `@ApplicationScoped` (full) or keep with `quarkus-spring-web` (compat) |
| `@Transactional` (Spring) | `@Transactional` (jakarta) |

### Type mismatch (`incompatible types`, `cannot convert`)

Typically caused by return-type changes after repository migration:

```java
// BEFORE: Spring Data — returns Optional<T>
Optional<Todo> result = repository.findById(id);

// AFTER: Panache — findById returns T directly (null if not found)
Todo result = Todo.findById(id);
```

### Method signature issue (`method not found`, `wrong number of arguments`)

```java
// BEFORE: Spring Data derived query
List<Todo> findByCompleted(boolean completed);

// AFTER: Panache
List<Todo> findByCompleted(boolean completed) {
    return list("completed", completed);
}
```

## MANUAL_REVIEW_REQUIRED

When a file cannot be fixed after 3 attempts, always emit:

```
MANUAL_REVIEW_REQUIRED: <relative/path/to/File.java>
Reason: <paste the compiler error here>
Attempted fixes: <brief description of what was tried>
```

Then act according to the current mode (see **Strategy** step 5 above).

## Watch out

- **Cascading errors**: A single broken class (e.g. a base entity) can produce dozens of errors in subclasses. Fix the root class first.
- **Jandex index missing**: If CDI beans in an external JAR aren't discovered because the dependency doesn't contain a Jandex index, add `quarkus.index-dependency.<name>.group-id` and, optionally, `quarkus.index-dependency.<name>.artifact-id` to `application.properties`.
