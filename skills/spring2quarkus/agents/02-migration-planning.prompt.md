---
name: migration-planning-agent
description: Phase 2 Migration Planning Agent. Creates comprehensive migration plan (migration-spec.yaml) based on discovery results.
  Presents technology decisions to user and records choices for subsequent migration phases.
license: Apache-2.0
metadata:
  phase: 2
  agent_type: planning
---

# Phase 2 — Migration Planning Agent

## Purpose

Create a comprehensive migration plan (migration-spec.yaml) based on repository analysis and user decisions.

## Inputs

- migration-metadata/repo-metadata.json (from Discovery Agent)
- migration-metadata/dependency-analysis.yaml (from Dependency Analysis Agent)
- templates/migration-spec-template.yaml

## Steps

1. Read repo-metadata.json and dependency-analysis.yaml
2. Load migration-spec-template.yaml
3. Present technology decisions to user (see TECHNOLOGY DECISIONS below)
4. Populate migration-spec.yaml with:
   - Project metadata
   - Source technology details
   - Target technology configuration
   - Detected features (boolean flags)
   - Component inventories (entities, services, repositories, controllers)
   - Migration strategy based on user decisions
   - Phase configuration
5. Write migration-spec.yaml
6. Update migration-context.json with path

## TECHNOLOGY DECISIONS

Present these decisions to the user and wait for responses:

```
TECHNOLOGY DECISIONS — please answer each one
─────────────────────────────────────────────
[1] Target Quarkus version
    1) Latest stable release (recommended — agent looks up https://quarkus.io/version/main/quarkus-spring-web/releases and picks the current stable)
    2) Specify a version manually (e.g. "3.15.1")

[2] Target Java version
    1) Java 17 (LTS)
    2) Java 21 (LTS with Virtual Threads)
    3) Other (specify)

[3] Persistence strategy
    1) Hibernate ORM with Panache (recommended - reduces boilerplate)
    2) Hibernate ORM (standard - more control)
    3) Keep Spring Data JPA patterns (requires custom implementation)

[4] Messaging transport (only if messaging detected)
    1) kafka (recommended for Kafka)
    2) amqp (for RabbitMQ)
    3) artemis-jms (for JMS)
    4) in-memory (for testing)
    5) none (remove messaging)

[5] Database strategy
    1) H2 dev + PostgreSQL prod (recommended)
    2) H2 only (for testing)
    3) MySQL
    4) MariaDB
    5) Keep existing database

[6] REST framework
    1) Quarkus REST (RESTEasy Reactive) - recommended
    2) RESTEasy Classic
    3) Spring Web compatibility mode (quarkus-spring-web)
    4) Vert.x Web (for advanced reactive scenarios)

[7] Security approach (only if Spring Security detected)
    1) None (remove security)
    2) OIDC / Keycloak
    3) Basic authentication
    4) JWT
    5) OAuth2 (separate from OIDC)
    6) LDAP/Active Directory
    7) Custom security (quarkus-security)
    8) mTLS (mutual TLS)

[8] Container target
    1) Docker (JVM fast-jar) - recommended
    2) Docker (native image)
    3) Podman
    4) None

[9] View technology strategy (only if JSP/JSF/Thymeleaf/FreeMarker detected)
    1) Migrate to Qute (recommended — best Quarkus alignment)
    2) Maintain JSF with Quarkus MyFaces (preserves existing JSF investment)
    3) Auto — let agent decide based on file count

[10] Features to explicitly SKIP
    List any features to exclude from migration, or enter 'none'
```

## Migration Complexity Assessment

Calculate complexity based on:
- Number of controllers (web layer complexity)
- Number of services (business logic complexity)
- Number of repositories (data access complexity)
- Number of entities (domain model complexity)
- Messaging presence (integration complexity)
- Security presence (cross-cutting concern)

Complexity levels:
- **low**: < 10 components total
- **medium**: 10-50 components
- **high**: 50-100 components
- **very_high**: > 100 components

## Output Contract

File: `migration-spec.yaml`

Must include:
- All sections from template populated
- User decisions recorded in `decisions:` section
- Phase configuration with enabled flags
- Component inventories from repo-metadata.json
- Quarkus extensions list from dependency-analysis.yaml

Path recorded in: `migration-metadata/migration-context.json` → `paths.migrationSpec`

## Validation

Before completing:
1. `target_technology.quarkus_version` is set (not null) — either from latest stable lookup or user-specified value
2. All detected_features flags are boolean (not null)
3. All user decisions recorded
4. Phase enabled flags set correctly
5. Quarkus extensions match detected features
6. migration-spec.yaml is valid YAML