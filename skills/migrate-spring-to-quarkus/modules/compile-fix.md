# Compile Fix

Structured retry loop for compilation errors during migration. Loaded on demand when a module's compile step fails.

## Procedure

```
FOR attempt IN 1..3:

  1. READ the compiler output and identify the failing file(s)
  2. CLASSIFY each error (see Common Error Patterns below)
  3. FIX one file at a time, applying the most specific fix for the error type
  4. COMPILE again
     IF pass → done, return to the module
     IF fail → continue to next attempt
```

If compilation still fails after 3 attempts, emit for each unresolved file:

```
MANUAL_REVIEW_REQUIRED: <relative/path/to/File.java>
Reason: <compiler error message>
Attempted fixes: <brief description of what was tried>
```

Then return control to SKILL.md (the Execution Protocol handles what to do next).

## Common Error Patterns

### Missing or wrong imports (`cannot find symbol`, `package does not exist`)

The most frequent error after migration. Spring/javax packages become Jakarta/Quarkus equivalents.

```java
// javax → jakarta
import javax.persistence.Entity;    → import jakarta.persistence.Entity;
import javax.inject.Inject;         → import jakarta.inject.Inject;

// Spring → CDI/JAX-RS
import org.springframework.stereotype.Service;  → import jakarta.enterprise.context.ApplicationScoped;
import org.springframework.web.bind.annotation.*; → import jakarta.ws.rs.*;
```

Load [references/annotation-map.md](../references/annotation-map.md) for the full mapping.

### Wrong annotations (`annotation type not applicable`)

Spring annotations replaced with incorrect Quarkus equivalents.

```java
@PathVariable("id")  → @PathParam("id")
@RequestParam("name") → @QueryParam("name")
@RequestBody         → no annotation needed (JAX-RS binds body by default)
```

### Type mismatches (`incompatible types`, `cannot convert`)

Usually caused by repository return-type changes.

```java
// Spring Data returns Optional<T>
Optional<Todo> result = repository.findById(id);

// Panache findById returns T (null if not found)
Todo result = Todo.findById(id);
```

### Method signature issues (`method not found`, `wrong number of arguments`)

Spring Data derived queries don't exist in Panache.

```java
// Spring Data — derived query method
List<Todo> findByCompleted(boolean completed);

// Panache — explicit query
public static List<Todo> findByCompleted(boolean completed) {
    return list("completed", completed);
}
```

## Report

When the procedure finishes (whether all errors were fixed or not), write `migration-reports/compile-fix-report.json`:

```json
{
  "phase": "compile-fix",
  "attempts": 2,
  "files_fixed": 5,
  "manual_review": ["src/main/java/com/example/SecurityConfig.java"],
  "fixes": [
    {
      "file": "src/main/java/com/example/TodoController.java",
      "error": "package org.springfranymework.http does not exist",
      "category": "missing-import",
      "fix": "Corrected org.springfranymework.http.ResponseEntity to org.springframework.http.ResponseEntity",
      "attempt": 1
    }
  ],
  "status": "PASS"
}
```

- `attempts`: total compile retries used
- `files_fixed`: number of files successfully fixed
- `manual_review`: list of files that still fail (empty if all fixed)
- `fixes`: array with one entry per fix applied (file, error message, category, description of the fix, and which attempt resolved it)
- `status`: `PASS` if compilation succeeds, `FAIL` if manual review items remain

## Guidelines

- Fix one file at a time, recompile after each fix.
- Don't delete code you cannot fix. Leave the original with a `// TODO: Migration required` comment.
- If an error doesn't match any known pattern, read the relevant reference file before attempting a fix.