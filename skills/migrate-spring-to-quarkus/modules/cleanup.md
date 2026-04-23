# Module: Cleanup

Remove leftover Spring artifacts that survived the per-module migration: orphaned imports, unused dependencies, stale configuration, and the Spring Boot main class (if not already removed).

## What to do

- [ ] Remove the `@SpringBootApplication` main class (if still present)
- [ ] Remove leftover Spring imports from all Java files
- [ ] Remove unused Spring dependencies from `pom.xml`
- [ ] Remove stale Spring configuration properties
- [ ] Remove orphaned Spring config files (`application-*.properties/yml` that have no Quarkus equivalent)
- [ ] Compile: `mvn clean compile -DskipTests`

## Main class removal

Quarkus auto-generates a main class — delete the `@SpringBootApplication` file. But first, check if it contains anything that needs migrating:

- `@Bean` methods → move to an `@ApplicationScoped` class with `@Produces`
- `CommandLineRunner` / `ApplicationRunner` → `void onStart(@Observes StartupEvent event)`
- `@EnableScheduling`, `@EnableCaching`, etc. → not needed, Quarkus enables these via extensions

If the main class was already removed during the code module, mark this as done.

## Leftover Spring imports

Search all Java files for remaining `org.springframework.*` imports:

```bash
grep -rn "import org.springframework" src/
```

For each hit:
- If the class has a Quarkus/Jakarta equivalent → replace the import (use annotation-map.md)
- If it's an unused import → delete it
- If it's still needed (Spring compat strategy) → leave it, but verify the corresponding `quarkus-spring-*` extension is in `pom.xml`

## Unused Spring dependencies

Check `pom.xml` for Spring dependencies that are no longer referenced anywhere in the code:

- `spring-boot-devtools` → always remove (no Quarkus equivalent; use `quarkus:dev` instead)
- `spring-boot-configuration-processor` → remove (Quarkus uses build-time config)
- `spring-boot-starter-actuator` → remove if replaced by `quarkus-smallrye-health` / `quarkus-micrometer`
- Any `spring-boot-starter-*` without matching code usage → remove

## Stale configuration

Check `application.properties` / `application.yml` for properties still using `spring.*` prefix that were missed during the build module. Either migrate them using config-map.md or remove them if the feature they configure no longer exists.