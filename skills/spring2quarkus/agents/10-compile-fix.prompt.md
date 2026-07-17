---
name: compile-fix-agent
description: Compile Fix Agent. Automatically fixes compilation errors after migration phases.
  Analyzes compiler output, applies fixes iteratively, and flags complex cases for manual review.
license: Apache-2.0
metadata:
  phase: any
  agent_type: fix
---

# Compile Fix Agent

## Purpose

Automatically fix compilation errors introduced during migration.

## Inputs

- Compilation error output
- Failed file paths

## Steps

1. Run `mvn clean package -DskipTests` and capture errors
2. Parse error messages
3. Identify error types:
   - Missing imports
   - Incorrect annotations
   - Type mismatches
   - Method signature issues
4. Apply automatic fixes
5. Retry compilation (max 3 attempts per file)
6. Flag unresolved issues for manual review
7. Generate compile-fix-report.json

## Common Fixes

### Missing Imports
```java
// Add missing imports
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
```

### Incorrect Annotations
```java
// Fix annotation usage
@PathParam("id") // instead of @PathVariable
@QueryParam("name") // instead of @RequestParam
```

## Output

**Directory Setup:**
```bash
mkdir -p migration-reports
```

**File Location:** `migration-reports/compile-fix-report.json`

This report should be created in the target Quarkus project at `<quarkus_target_dir>/migration-reports/compile-fix-report.json`.

```json
{
  "phase": "compile-fix",
  "attempts": 2,
  "files_fixed": 5,
  "manual_review": [],
  "package_status": "PASS"
}