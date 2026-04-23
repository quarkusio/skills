. 

Migrate all Java source code from Spring patterns to Quarkus equivalents.

Load [references/annotation-map.md](../references/annotation-map.md) before starting. It contains the complete annotation mapping tables for DI, REST, Data, Security, Cache, Scheduling, and Lifecycle.

## What to do

- [ ] Migrate entities (JPA → Panache if native strategy)
- [ ] Migrate repositories (Spring Data → Panache if native strategy)
- [ ] Simplify service layer (remove unnecessary interface+impl)
- [ ] Migrate controllers/resources (Spring MVC → JAX-RS if native strategy)
- [ ] Migrate DI annotations (`@Autowired` → `@Inject`, `@Component` → `@ApplicationScoped`, etc.)
- [ ] Migrate `Model.addAttribute()` → Qute `Template.data()` or `@CheckedTemplate`
- [ ] Migrate `return "redirect:..."` → `Response.seeOther()`
- [ ] Remove `@SpringBootApplication` main class
- [ ] Compile: `mvn clean compile -DskipTests`

Use the annotation-map.md reference for the full mapping. Below are the key patterns with before/after examples.

## Entity Layer (Native strategy)

```java
// BEFORE: Spring Data JPA
@Entity
public class Todo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private boolean completed;
    // getters + setters
}

// AFTER: Panache Active Record
@Entity
public class Todo extends PanacheEntity {
    public String title;
    public boolean completed;
    // id provided by PanacheEntity — remove @Id and @GeneratedValue
    // public fields — remove getters/setters
}
```

## Repository Layer (Native strategy)

Two patterns available — choose based on project conventions:

**Active Record** (queries on the entity):
```java
// Static methods on the entity
public static List<Todo> findByCompleted(boolean completed) {
    return list("completed", completed);
}
// Usage: Todo.listAll(), Todo.findById(id), Todo.findByCompleted(true)
```

**Repository class** (separate from entity):
```java
@ApplicationScoped
public class TodoRepository implements PanacheRepository<Todo> {
    public List<Todo> findByCompleted(boolean completed) {
        return list("completed", completed);
    }
}
```

**Pagination** (replaces Spring's `Page<T>` + `Pageable`):
```java
PanacheQuery<Todo> query = find("completed", completed);
query.page(Page.of(page, size));
long totalCount = query.count();
List<Todo> items = query.list();
```

**Spring compat strategy**: Keep `JpaRepository`/`CrudRepository` — they work with `quarkus-spring-data-jpa`.

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
- Service only delegates to repository → eliminate it, inject repository directly in the resource
- Service has real business logic → keep as `@ApplicationScoped`, remove the interface
- Interface used for testing/mocking → not needed, `@InjectMock` works on concrete classes

**Spring compat strategy**: `@Service` is supported by `quarkus-spring-di` — no changes needed.

## Controller → Resource (Native strategy)

```java
// BEFORE: Spring MVC
@Controller
public class TodoController {
    @GetMapping("/todos")
    public String list(Model model) {
        model.addAttribute("todos", todoService.findAll());
        return "todos";
    }

    @PostMapping("/todos")
    public String create(@ModelAttribute Todo todo) {
        todoService.save(todo);
        return "redirect:/todos";
    }
}

// AFTER: Quarkus + JAX-RS + Qute
@Path("/todos")
@ApplicationScoped
public class TodoResource {
    @Inject TodoService todoService;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance todos(List<Todo> todos);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.todos(todoService.findAll());
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response create(@BeanParam Todo todo) {
        todoService.save(todo);
        return Response.seeOther(URI.create("/todos")).build();
    }
}
```

**Qute strict data map** — unlike Thymeleaf, Qute throws `TemplateException` if a key referenced in the template is missing from the data map. Every `.data()` call site must provide the **same complete set of keys**, including on empty-result paths.

**Spring compat strategy**: `@RestController` works with `quarkus-spring-web` (but NOT plain `@Controller`).

## Main Class Removal

Quarkus auto-generates a main class — delete the `@SpringBootApplication` file. But first, migrate anything it contains:

- `@Bean` methods → move to an `@ApplicationScoped` class with `@Produces`
- `CommandLineRunner` / `ApplicationRunner` → `void onStart(@Observes StartupEvent event)`

## Watch out

- **Missing `@Transactional`**: Quarkus uses `jakarta.transaction.Transactional`, not Spring's
- **Bean discovery**: Quarkus uses build-time CDI; beans must have a scope annotation
- **No OSIV**: Quarkus doesn't have Open Session in View; lazy loading outside transactions will fail
- **No component scanning**: Beans in external JARs need a Jandex index or `quarkus.index-dependency`
- **JAX-RS path conflicts**: Spring allows overlapping `@RequestMapping` paths — JAX-RS does not. Check for duplicate `@Path` values

## Spring Compat Extension Limitations

When using the compatibility strategy (`quarkus-spring-*` extensions), be aware of these **verified limitations from the Quarkus source code**:

| Extension | What does NOT work |
|---|---|
| `quarkus-spring-di` | `@Primary`, `@Conditional*`, `@Profile`, `@Lazy` not processed. SpEL `#{...}` in `@Value` throws error. `@Bean` must be inside `@Configuration` class. |
| `quarkus-spring-web` | Only `@RestController` — plain `@Controller` not supported. Only one `@RestControllerAdvice` per app. `@CrossOrigin`, `@InitBinder`, `@ModelAttribute` not supported. No reactive types (`Mono`, `Flux`). |
| `quarkus-spring-security` | Limited SpEL in `@PreAuthorize`: only `hasRole`, `hasAnyRole`, `permitAll`, `denyAll`, `isAuthenticated`, `@bean.method()`, param comparison. Cannot mix `and`/`or` operators. Cannot combine `@Secured` with `@PreAuthorize`. |
| `quarkus-spring-data-jpa` | SpEL `#{...}` in `@Query` not supported. No `Distinct` queries. Limited custom repository fragment support. |
| `quarkus-spring-cache` | Single cache name only (no arrays). `key`, `condition`, `unless`, `keyGenerator`, `cacheManager` parameters NOT supported. No `@Caching` or `@CacheConfig`. |
| `quarkus-spring-scheduled` | `fixedDelay` NOT supported (only `fixedRate`). Cannot combine `initialDelay` with `cron`. |