---
name: reporting-agent
description: Final Reporting Agent. Generates comprehensive migration summary (migration-summary.md).
  Aggregates all phase reports, documents manual review items, and provides next steps.
license: Apache-2.0
metadata:
  phase: final
  agent_type: reporting
---

# Final — Reporting Agent

## Purpose

Generate comprehensive migration summary report.

## Inputs

- All phase reports from `migration-reports/` directory
- `migration-spec.yaml` (root level)
- `migration-metadata/migration-context.json`

## Steps

1. Read all phase reports
2. Aggregate statistics
3. Identify manual review items
4. Calculate migration metrics
5. Generate migration-summary.md

## Output

**File Location:** `migration-summary.md`

This summary report should be created in the target Quarkus project at `<quarkus_target_dir>/migration-summary.md` (root level).

The report should aggregate information from:
- `migration-reports/phase-03-project-bootstrap.json`
- `migration-reports/phase-04-database-migration.json`
- `migration-reports/phase-05-persistence-migration.json`
- `migration-reports/phase-06-service-migration.json`
- `migration-reports/phase-07-messaging-migration.json`
- `migration-reports/phase-08-web-migration.json`
- `migration-reports/phase-08b-web-views-migration.json` (if view layer migration was performed)
- `migration-reports/phase-09-configuration-migration.json`
- `migration-reports/phase-11-validation.json`
- `migration-metadata/migration-context.json`

Example:

```markdown
# Spring Boot to Quarkus Migration Summary

## Overview
- **Project**: myapp-quarkus
- **Migration Date**: 2024-01-15
- **Status**: ✅ COMPLETED

## Statistics
- **Files Modified**: 87
- **Files Created**: 24
- **Controllers Migrated**: 8
- **Services Migrated**: 15
- **Repositories Migrated**: 12
- **Entities Migrated**: 42
- **Messaging Listeners**: 5

## Build Status
- **Compilation**: ✅ PASS
- **Tests**: ✅ PASS
- **Package**: ✅ PASS

## Performance Improvements
- **Startup Time**: 2.5s → 0.8s (68% faster)
- **Memory Usage**: 512MB → 256MB (50% reduction)
- **Build Time**: 45s → 12s (73% faster)

## Manual Review Items
- None

## Next Steps
1. Run full test suite
2. Performance testing
3. Deploy to staging
4. Update documentation