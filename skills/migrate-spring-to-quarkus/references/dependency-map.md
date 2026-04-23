# Spring Boot to Quarkus Dependency Map

## Core

| Spring Boot | Quarkus (Full Migration) | Quarkus (Compatibility) |
|---|---|---|
| `spring-boot-starter-web` | `quarkus-rest` | `quarkus-spring-web` |
| `spring-boot-starter-webflux` | `quarkus-rest` with reactive | `quarkus-spring-web` |
| `spring-boot-starter-data-jpa` | `quarkus-hibernate-orm-panache` | `quarkus-spring-data-jpa` |
| `spring-boot-starter-data-rest` | `quarkus-rest` + `quarkus-hibernate-orm-panache` | `quarkus-spring-data-rest` |
| `spring-boot-starter-validation` | `quarkus-hibernate-validator` | `quarkus-hibernate-validator` |
| `spring-boot-starter-security` | `quarkus-security` + `quarkus-oidc` (or `quarkus-elytron-security-properties-file`) | `quarkus-spring-security` |
| `spring-boot-starter-actuator` | `quarkus-smallrye-health` + `quarkus-micrometer` | `quarkus-spring-boot-properties` |
| `spring-boot-starter-cache` | `quarkus-cache` | `quarkus-spring-cache` |
| `spring-boot-starter-test` | `quarkus-junit` + `io.rest-assured:rest-assured` | `quarkus-junit` |

## Data / Persistence

| Spring Boot | Quarkus (Full Migration) | Quarkus (Compatibility) |
|---|---|---|
| `spring-boot-starter-data-mongodb` | `quarkus-mongodb-panache` | `quarkus-spring-data-api` (limited) |
| `spring-boot-starter-data-redis` | `quarkus-redis-client` | -- |
| `spring-boot-starter-jdbc` | `quarkus-agroal` + `quarkus-jdbc-*` | `quarkus-spring-data-jpa` |
| `h2` (test DB) | `quarkus-jdbc-h2` | `quarkus-jdbc-h2` |
| `postgresql` | `quarkus-jdbc-postgresql` | `quarkus-jdbc-postgresql` |
| `mysql-connector-j` | `quarkus-jdbc-mysql` | `quarkus-jdbc-mysql` |
| `flyway` | `quarkus-flyway` | `quarkus-flyway` |
| `liquibase` | `quarkus-liquibase` | `quarkus-liquibase` |

## Messaging

| Spring Boot | Quarkus |
|---|---|
| `spring-boot-starter-amqp` | `quarkus-smallrye-reactive-messaging-amqp` |
| `spring-kafka` | `quarkus-smallrye-reactive-messaging-kafka` |

## Templating

| Spring Boot | Quarkus |
|---|---|
| `spring-boot-starter-thymeleaf` | `quarkus-rest-qute` (when controllers return `TemplateInstance`) or `quarkus-qute-web` (auto-serve templates without REST resources). **Never** use `quarkus-qute` alone — it lacks REST integration. |
| `spring-boot-starter-freemarker` | `quarkus-freemarker` |

## Scheduling / DI / Config

| Spring Boot | Quarkus (Full) | Quarkus (Compat) |
|---|---|---|
| `spring-boot-starter` (DI) | `quarkus-arc` (included by default) | `quarkus-spring-di` |
| `spring-boot-configuration-processor` | `quarkus-arc` (built-in config mapping) | `quarkus-spring-boot-properties` |
| `spring-boot-starter-quartz` | `quarkus-quartz` or `quarkus-scheduler` | `quarkus-spring-scheduled` |

## Data REST / Cloud / Testing

| Spring Boot | Quarkus (Full) | Quarkus (Compat) |
|---|---|---|
| `spring-boot-starter-data-rest` | `quarkus-rest` + `quarkus-hibernate-orm-panache` | `quarkus-spring-data-rest` |
| `spring-cloud-starter-config` | -- | `quarkus-spring-cloud-config-client` |
| `spring-boot-starter-test` (with `@SpringBootTest`) | `quarkus-junit` | `quarkus-spring-boot-test` |

## Observability

| Spring Boot | Quarkus |
|---|---|
| `micrometer-registry-prometheus` | `quarkus-micrometer-registry-prometheus` |
| `spring-boot-starter-logging` | `quarkus-logging-json` (JSON) or built-in JBoss Logging |
| `opentelemetry` | `quarkus-opentelemetry` |

## Build Plugin

| Spring Boot | Quarkus |
|---|---|
| `spring-boot-maven-plugin` | `quarkus-maven-plugin` |
| `spring-boot-gradle-plugin` | `io.quarkus` Gradle plugin |