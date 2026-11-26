# Module: Service

Migrate Spring services, dependency injection, configuration injection, transactions, scheduling and application bootstrap to Quarkus equivalents.

Scope: `@Service`, `@Component`, `@Autowired`, `@Value`, `@Bean`, `@Configuration`, `@Transactional`, `@Scheduled`, `@Async`, the `@SpringBootApplication` main class, and `CommandLineRunner` / `ApplicationRunner`. Controllers belong to the [rest module](rest.md).

All files to transform are in `<target>` (already copied there by the build module). Do not modify `<source>`.

Read `<target>/migration-spec.yaml` at module start:
- `decisions.strategy`: `full-quarkus` vs `spring-compat`

Load [references/annotation-map.md](../../references/annotation-map.md) before starting. Use its Dependency Injection, Scheduling, Security, Cache, Configuration Properties and Application Lifecycle sections, plus the `@Transactional` rows and notes in its Data / JPA section.

## What to do

- [ ] Simplify service layer (remove unnecessary interface+impl)
- [ ] Migrate DI annotations (`@Autowired` → `@Inject`, `@Component` → `@ApplicationScoped`, etc.)
- [ ] Migrate `@Transactional` according to the strategy (see [Transactions](#transactions))
- [ ] Remove `@SpringBootApplication` main class
- [ ] Compile: `cd <target> && ./mvnw clean compile -DskipTests` (Maven) or `cd <target> && ./gradlew clean compileJava -x test` (Gradle)

Use the annotation-map.md reference for the full mapping. Below are the key patterns with before/after examples.

## Service Layer

Spring services often use interface + implementation unnecessarily. Simplify:

```java
// BEFORE: Spring — interface + impl
public interface TodoService { List<Todo> findAll(); }

@Service
public class TodoServiceImpl implements TodoService {
    @Autowired private TodoRepository repository;
    @Override public List<Todo> findAll() { return repository.findAll(); }
}

// AFTER: Quarkus — single class
@ApplicationScoped
public class TodoService {
    @Inject TodoRepository repository;
    public List<Todo> findAll() { return repository.listAll(); }
}
```

**Decision guide:**
- Service only delegates to repository → eliminate it, inject repository directly in the resource. Writes then happen in the resource method, so annotate it with `jakarta.transaction.Transactional` (see [Transactions](#transactions))
- Service has real business logic → keep as `@ApplicationScoped`, remove the interface
- Interface used for testing/mocking → not needed, `@InjectMock` works on concrete classes

**Spring compat strategy**: `@Service` is supported by `quarkus-spring-di` — no changes needed.

## Main Class Removal

If the main class in `<target>` **only** contains `SpringApplication.run(...)`, delete it from `<target>` -- Quarkus auto-generates a main class. Do not modify `<source>`.

If it contains additional logic, migrate before deleting:

- `@Bean` methods → move to an `@ApplicationScoped` class with `@Produces`
- `CommandLineRunner` → `@QuarkusMain` with `QuarkusApplication`, or `@TopCommand` (Picocli) if it parses CLI arguments
- `ApplicationRunner` → `@QuarkusMain` implementing `QuarkusApplication`
- `@EnableScheduling`, `@EnableCaching`, etc. → not needed, Quarkus enables these via extensions

### @Bean methods

```java
// BEFORE: Spring — @Bean in main class
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) { SpringApplication.run(MyApp.class, args); }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}

// AFTER: Quarkus — @Produces in a dedicated class
@ApplicationScoped
public class AppConfig {
    @Produces
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
```

### CommandLineRunner → @QuarkusMain

Use `@QuarkusMain` when the runner executes startup logic without parsing CLI arguments.

```java
// BEFORE: Spring — CommandLineRunner
@SpringBootApplication
public class MyApp implements CommandLineRunner {
    @Autowired DataLoader dataLoader;

    public static void main(String[] args) { SpringApplication.run(MyApp.class, args); }

    @Override
    public void run(String... args) {
        dataLoader.seed();
    }
}

// AFTER: Quarkus — @QuarkusMain
@QuarkusMain
public class MyApp implements QuarkusApplication {
    @Inject DataLoader dataLoader;

    @Override
    public int run(String... args) {
        dataLoader.seed();
        Quarkus.waitForExit();
        return 0;
    }
}
```

### CommandLineRunner with CLI arguments → @TopCommand (Picocli)

Use `@TopCommand` when the runner parses command-line arguments.

```java
// BEFORE: Spring — CommandLineRunner parsing args
@SpringBootApplication
public class MyCli implements CommandLineRunner {
    public static void main(String[] args) { SpringApplication.run(MyCli.class, args); }

    @Override
    public void run(String... args) {
        String file = args[0];
        process(file);
    }
}

// AFTER: Quarkus — Picocli @TopCommand
@TopCommand
@CommandLine.Command(name = "mycli", mixinStandardHelpOptions = true)
public class MyCli implements Runnable {
    @CommandLine.Parameters(index = "0", description = "File to process")
    String file;

    @Override
    public void run() {
        process(file);
    }
}
// Requires: quarkus-picocli extension
```

### ApplicationRunner → @QuarkusMain

```java
// BEFORE: Spring — ApplicationRunner
@SpringBootApplication
public class MyApp implements ApplicationRunner {
    @Autowired MigrationService migrations;

    public static void main(String[] args) { SpringApplication.run(MyApp.class, args); }

    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption("migrate")) {
            migrations.execute();
        }
    }
}

// AFTER: Quarkus — @QuarkusMain
@QuarkusMain
public class MyApp implements QuarkusApplication {
    @Inject MigrationService migrations;

    @Override
    public int run(String... args) {
        List<String> argList = List.of(args);
        if (argList.contains("--migrate")) {
            migrations.execute();
        }
        Quarkus.waitForExit();
        return 0;
    }
}
```


## Transactions

Look for `org.springframework.transaction.annotation.Transactional` (class or method level) and check which attributes are used.

**Full Quarkus strategy** — replace with `jakarta.transaction.Transactional` and adapt the attributes. Make sure `quarkus-narayana-jta` is present (normally pulled in by Hibernate ORM).

```java
// BEFORE: Spring
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = IOException.class, timeout = 30)
public void archive(Todo todo) { ... }

// AFTER: Quarkus
import io.quarkus.narayana.jta.runtime.TransactionConfiguration;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@Transactional(value = TxType.REQUIRES_NEW, rollbackOn = IOException.class)
@TransactionConfiguration(timeout = 30)
public void archive(Todo todo) { ... }
```

| Spring attribute | Quarkus equivalent |
|---|---|
| `propagation` | `value = TxType.*` (`REQUIRED`, `REQUIRES_NEW`, `SUPPORTS`, `MANDATORY`, `NOT_SUPPORTED`, `NEVER`) |
| `rollbackFor` / `noRollbackFor` | `rollbackOn` / `dontRollbackOn` |
| `timeout` | `@TransactionConfiguration(timeout = <seconds>)` — only on the method that starts the transaction; on a method called inside an existing transaction it throws |
| `readOnly` | No equivalent — drop it |
| `propagation = NESTED` | No equivalent — redesign (e.g. `REQUIRES_NEW` if the inner work may commit independently) and note it in the migration summary |

**Spring compat strategy** — add `quarkus-spring-tx` and keep Spring's `@Transactional` unchanged. Check the attributes against its limitations (see the table below):
- `propagation = NESTED` fails the build — replace it as above
- `readOnly` and `timeout` compile but are ignored at runtime (Quarkus only logs a warning) — flag each use in the migration summary as a behavioral difference
- `isolation` (other than `DEFAULT`), `value` / `transactionManager` and `label` are also unsupported and ignored with a warning

## Watch out

- **Missing `@Transactional`**: Quarkus uses `jakarta.transaction.Transactional`, not Spring's — see [Transactions](#transactions)
- **Bean discovery**: Quarkus uses build-time CDI; beans must have a scope annotation
- **No component scanning**: Beans in external JARs need a Jandex index or `quarkus.index-dependency`

## Spring Compat Extension Limitations

When using the compatibility strategy (`quarkus-spring-*` extensions), be aware of these **verified limitations from the Quarkus source code**:

| Extension | What does NOT work |
|---|---|
| `quarkus-spring-di` | `@Primary`, `@Conditional*`, `@Profile`, `@Lazy` not processed. SpEL `#{...}` in `@Value` throws error. `@Bean` must be inside `@Configuration` class. |
| `quarkus-spring-security` | Limited SpEL in `@PreAuthorize`: only `hasRole`, `hasAnyRole`, `permitAll`, `denyAll`, `isAuthenticated`, `@bean.method()`, param comparison. Cannot mix `and`/`or` operators. Cannot combine `@Secured` with `@PreAuthorize`. |
| `quarkus-spring-cache` | Single cache name only (no arrays). `key`, `condition`, `unless`, `keyGenerator`, `cacheManager` parameters NOT supported. No `@Caching` or `@CacheConfig`. |
| `quarkus-spring-scheduled` | `fixedDelay` NOT supported (only `fixedRate`). Cannot combine `initialDelay` with `cron`. |
| `quarkus-spring-tx` | `propagation = NESTED` causes a build-time error. `readOnly`, `timeout`, non-default `isolation`, `value`/`transactionManager` and `label` are ignored with a warning. |
